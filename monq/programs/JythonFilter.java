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

package monq.programs;

import monq.stuff.ArrayUtil;
import monq.net.*;
import monq.jfa.*;
import monq.clifj.*;

import java.io.*;
import java.nio.charset.*;
 
/**
 * <p>A manager for a sequence of {@link monq.jfa.JyFA} objects that
 * allows to set them up as a {@link monq.net.Service} and also
 * runs on the command line.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class JythonFilter implements ServiceFactory {
  private JyFA[] jyfas;
  private int ff = 0;		// first free in jyfas

  private String inputEncoding = "UTF-8";
  private String outputEncoding = "UTF-8";
  
  /**********************************************************************/
  /**
   * <p>creates a <code>JythonFilter</code> for the given number of
   * <code>JyFA</code> objects to be managed. More objects may be
   * added, but under normal conditions it is expected that a fairly
   * good estimate can be given to the constructor.</p>
   *
   * <p>The input encoding of the first filter and the output encoding
   * of the last are initially set to <q>UTF-8</q>.</p>
   */
  public JythonFilter(int initialCapacity) {
    jyfas = new JyFA[initialCapacity];
    ff = 0;
  }
  /**********************************************************************/
  /**
   * <p>adds a {@link JyFA} object to the list of filters to be
   * managed.</p>
   * @return <code>this</code>
   */
  public JythonFilter add(JyFA jyfa) {
    if( ff>=jyfas.length ) jyfas = (JyFA[])ArrayUtil.resize(jyfas, ff+2);
    jyfas[ff++] = jyfa;
    return this;
  }
  /**********************************************************************/
  /**
   * <p>sets the input encoding to be used for the first filter in the
   * sequence of filters created by {@link #createService}.</p>
   */
  public void setInputEncoding(String charsetName) 
    throws UnsupportedEncodingException
  {
    inputEncoding = charsetName;
    // trigger exception early
    Charset.forName(charsetName);
  }
  /**********************************************************************/
  /**
   * <p>sets the output encoding to be used for the last filter
   * in the sequence of filters created by {@link #createService}.</p>
   */
  public void setOutputEncoding(String charsetName) 
    throws UnsupportedEncodingException
  {
    outputEncoding = charsetName;
    // trigger exception early
    Charset.forName(charsetName);
  }
  /**********************************************************************/
  /**
   * <p>creates a cascaded filter from the managed sequence of {@link
   * monq.jfa.JyFA} objects. The {@link monq.jfa.JyFA#createRun}
   * method of each <code>JyFA</code> is called in turn and the
   * resulting {@link monq.jfa.DfaRun} objects are set up such that
   * number <em>i+1</em> has number <em>i</em> as input, while number
   * 0 reads from the given <code>in</code>. The last
   * <code>DfaRun</code> is returned.
   */
  public DfaRun createRun(CharSource in) {
    DfaRun r = null;
    for(int i=0; i<ff; i++) {
      r = jyfas[i].createRun();
      r.setIn(in);
      in = r;
    }
    return r;
  }
  /**********************************************************************/
  /**
   * sets up a cascaded filter as obtained from {@link #createRun
   * createRun()} as a {@link monq.net.Service}. The given
   * <code>InputStream</code> will be wrapped into a
   * <code>Reader</code> with an input encoding as specified by {@link
   * #setInputEncoding setInputEncoding()}. The encoding used
   * to write to the given <code>OutputStream</code> is the one which
   * was specified with {@link #setOutputEncoding
   * setOutputEncoding()}. Both encodings default to <q>UTF-8</q>.</p>
   */
  public Service createService(InputStream in, OutputStream out, Object p) {
    try {
      CharSource cs = 
	new ReaderCharSource(new InputStreamReader(in, inputEncoding));
      PrintStream ps = new PrintStream(out, true, outputEncoding);
      DfaRun r = createRun(cs);
      return new DfaRunService(r, ps);
    } catch( UnsupportedEncodingException e ) {
      throw new Error("bug", e);
    }
  }
  /**********************************************************************/
  /**
   * <p>run on the commandline with <code>-h</code> to get a
   * description.</p>
   */
  public static void main(String[] argv) throws Exception {
    String prog = System.getProperty("argv0", "JythonFilter");
    Commandline cmd = new Commandline
      (prog, 
       "set up a sequence of FA filters by means of python modules.\n"+
       "Read the documentation of monq.jfa.JyFA to find out how to write\n"+
       "a module.", "py", ".py files", 1, Integer.MAX_VALUE);

    cmd.addOption(new Option("-ie", "inEnc",
                             "encoding used for input stream, guessed "
                             +"from input if not specified and then "
                             +"defaults to UTF-8",
                             1, 1, null));

    cmd.addOption(new Option("-oe", "outEnc",
                             "encoding used for output stream, "
                             +"defaults to UTF-8",
                             1, 1, null));

    cmd.addOption(new LongOption
                  ("-p", "port", 
                   "run as a filter server on the given port",
                   1, 1, 0, 65535, null));
 
    try {
      cmd.parse(argv);
    } catch( CommandlineException e ) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    String[] py = cmd.getStringValues("--");
    JythonFilter jf = new JythonFilter(py.length);

    if( cmd.available("-ie") ) {
      jf.setInputEncoding(cmd.getStringValue("-ie"));
    }
    if( cmd.available("-oe") ) {
      jf.setInputEncoding(cmd.getStringValue("-oe"));
    }
    
    for(int i=0; i<py.length; i++) { 
      jf.add(new JyFA(py[i])); 
    }

    if( cmd.available("-p") ) {
      //go into server mood
      FilterServiceFactory fsf = new FilterServiceFactory(jf);
      int port = (int)cmd.getLongValue("-p");
      new TcpServer(port, fsf, 20).setLogging(System.out).serve();
    } else {
      // prefer to use a plain OutputStream from the FileDescriptor
      // over System.out, because otherwise a broken output pipe will not
      // recognized
      OutputStream out = new FileOutputStream(FileDescriptor.out);
      Service svc = jf.createService(System.in, out, null);
      svc.run();
      Exception e = svc.getException();
      if( e!=null ) throw e;      
    }
  }
}
