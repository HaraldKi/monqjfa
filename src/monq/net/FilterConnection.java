/*+********************************************************************* 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation
Foundation, Inc., 59 Temple Place - Suite 330, Boston MA 02111-1307, USA.
************************************************************************/

package monq.net;

import monq.jfa.*;
import monq.jfa.actions.*;

import java.net.Socket;
import java.io.*;
import java.util.*;

/**
 * <p>This class is used by {@link FilterServiceFactory} to read the
 * control string and connect to the upstream server, if any. With
 * {@link #getParameters} the parameters for the server to be provided
 * by the <code>FilterServiceFactory</code> can be retrieved.</p>
 *
 * <p>Objects of this class can only be created with a call to {@link
 * #connect connect()}.</p>
 
 * @author &copy; 2004 Harald Kirsch
 * @version $Revision: 1.3 $, $Date: 2005-11-17 09:40:14 $
 */

class FilterConnection {
  private Map<String,String> m = new HashMap<String,String>();

  private String host;
  private int port = -1;
  private StringBuffer tail = new StringBuffer();

  // the upstream connection
  private Socket socket = null;

  /**********************************************************************/
  // we create one instance of a Dfa to parse connection request read
  // from an input stream
  private static final Dfa requestParser;

  // the next two are used to limit parsing a request to make sure no
  // DOS is possible by just feeding endless param lists
  private static final long MATCHMAX=1000;
  private static final int MAXPARAMS=50;

  static {
    // FIX ME: this should probably become XML some day
    try {
      requestParser = new
	Nfa("[.]?"+PipelineRequest.KEYRE+"=([^;\n]|\\\\;|\\\\\n)*",
	    new AbstractFaAction() {
	      public void invoke(StringBuffer yytext, 
				 int start, DfaRun r) {
		//System.out.println(">>>"+ts);
		@SuppressWarnings("unchecked")
		  Map<String,String> h = (Map)r.clientData;

		if( h.size()>=MAXPARAMS ) {
		  r.setIn(new CharSequenceCharSource
			  (">>to many params<<"));
		  return;
		}

		// find the equal sign which we are sure to find
		int eqpos = start;
		while( yytext.charAt(eqpos)!='=' ) eqpos+=1;

		String key = yytext.substring(start, eqpos);

		// The characters '\n', ';' and '|' only appear in the
		// match when preceeded by backslash. We have to
		// delete this backslash
		for(int i=yytext.length()-1; i>eqpos; i--) {
		  char ch = yytext.charAt(i);
		  if( ch!=';' && ch!='\n' && ch!='|') continue;
		  i -= 1;
		  yytext.deleteCharAt(i);
		}
		String value = yytext.substring(eqpos+1);
		//System.out.println("FilterConnection param `"+key+"' = `"+value+"'");
		h.put(key, value);
		yytext.setLength(0);
	      }
	    })
	.or(";+", Drop.DROP)
	.or("\n.*", new AbstractFaAction() {
	    public void invoke(StringBuffer yytext, int start, DfaRun r) {
	      yytext.deleteCharAt(0);
	      r.setIn(new EmptyCharSource());
	    }
	  })
	.compile(DfaRun.UNMATCHED_THROW)
	;
    } catch( CompileDfaException e ) {
      throw new Error("impossible", e);
    } catch( ReSyntaxException e ) {
      throw new Error("impossible", e);
    }
    requestParser.matchMax = MATCHMAX;
  }
  /**********************************************************************/
  /**
   * <p>return the full <code>Map</code> of parameters.</p>
   */
  public Map getParameters() { return m; }
  /**********************************************************************/
  public FilterConnection(InputStream ctrlIn) throws IOException {
    CharSource source = new ReaderCharSource(ctrlIn, "UTF-8");
    DfaRun r = new DfaRun(requestParser, source);
    r.clientData = this.m;
    // if parameters cannot be correctly parsed or if the request is
    // far too long, the next line throws IOException.
    r.filter(this.tail);

    host = (String)m.remove(".host");
    String portString = (String)m.remove(".port");
//     System.err.println("FilterConnection just got [[[host="+host
// 		       +", port="+portString+", map="+m+"]]]");
    if( host==null ) return;

    try {
      port = Integer.parseInt(portString);
    } catch( NumberFormatException e ) {
      host = null;
      throw new ServiceUnavailException
	("port parameter `"+portString+"', as read from downstream, "
	 +"is not a number");
    }
  }
  /**********************************************************************/
  /**
   * <p>creates a new <code>FilterConnection</code> from the list of
   * <code>PipelineRequest</code>, which must contain the requests in
   * the order in which data flows through them.</p>
   */
  public FilterConnection(PipelineRequest[] reqs) {
    int last = reqs.length-1;
    this.host = reqs[last].getHost();
    // note: PipelineRequest guarantees a parsable int in the right range
    this.port = Integer.parseInt(reqs[last].getPort());
    reqs[last--].encode(tail, false);

    while(last>=0 ) reqs[last--].encode(tail, true);

//     System.err.println("FilterConnection construtor: port="+port
// 		       +", tail="+this.tail);
  }
  /**
   * <p>appends the request to the connection control string of this
   * connection. The appended request thereby becomes the <em>most
   * up-stream</em> request of <code>this</code>.</p> 
   */
  void append(PipelineRequest req) {
    req.encode(tail, true);
  }
  /**********************************************************************/
  /**
   * <p>reads parameters from the given <code>InputStream</code>,
   * possibly connects to an upstream server and provides the
   * collected information.</p>
   *
   * <p>The <code>InputStream</code> must have been written with a
   * {@link #XXX} object and therefore can contain key/value pairs,
   * upstream host and port information and control data to be send to
   * the upstream server. These are handled as follows:</p>
   *
   * <dl>
   * <dt>key/value pairs</dt><dd>are stored in the
   * <code>FilterConnection</code> returned,</dd>
   * <dt>host and port</dt><dd>if available, are used to connect to an
   * upstream server,</dd>
   * <dt>further control data</dt><dd>is send to the upstream server.</dd>
   * </dl>
   *
   * <p>If a connection is made to an upstream server, the
   * <code>InputStream</code> of this connection can be retrieved from
   * the result with {@link #getInputStream}. Once work is finished,
   * {@link #close} must be called.</p>
   *
   * @throws ServiceUnavailException if host or port for an upstream
   * server are found in the request but there are problems to connect.
   */
  public InputStream connect() throws IOException {
    if( host==null || port==-1 ) {
      throw new IllegalStateException
	("no connection possible to host="+host+", port="+port);
    }
    //
    // connect and send the tail to the upstream server
    //
    //System.out.println("pipeConnect: connecting to "+host+":"+port);
    socket = null;
    try {
      socket = new Socket(host, port);
      PrintStream controlOut = 
	new PrintStream(socket.getOutputStream(), true, "UTF-8");
      controlOut.print(tail);
      //System.err.println("Just send `"+tail+"' to "+port);
      controlOut.flush();
      socket.shutdownOutput();
    } catch( java.io.IOException e ) {
      try { socket.close(); } catch( Throwable ee ) { /** OOoooompf */ }
      throw new ServiceUnavailException("cannot contact upstream server "
					+host+":"+port, e);
    }
    return socket.getInputStream();
  }
  /**********************************************************************/
  /**
   * <p>close the upstream socket, if any.</p>
   * @throws IOException if an I/O error occurs when closing this socket.
   */
  public void close() throws IOException {
    if( socket==null ) return;	// be generous
    socket.close();
    socket = null;
  }
  /**********************************************************************/
}
