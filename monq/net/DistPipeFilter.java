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

import monq.stuff.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.*;

/**
 * <p>is a communication endpoint for a <em>distributed pipe</em> of
 * (text) filters. Given there are several filters set up with a
 * {@link FilterServiceFactory} running on the network, this class
 * allows to contact them and run data through them in a given
 * sequence. 
 *
 * <p style="font-size:smaller; margin-left:1em;
 * margin-right:1em"><b>Implementation Note:</b> Rather then
 * contacting each server, one after another, the last server in the
 * sequence is contacted and instructed to fetch the data from the
 * second to last and so on. Thereby an arbitrary large sequence of
 * filters is set up. Finally, the first filter in the sequence
 * contacts back to the initiating <code>DistPipeFilter</code> to read
 * the input.</p>
 *
 * <p>Objects of this class can be used by several threads in
 * parallel. The maximum number that will run in parallel is defined
 * by the <code>slot</code> parameter of the constructor. If this
 * number is exceeded, threads are blocked until running threads
 * finish.</p>
 *
 * <p>The following steps are necessary to use objects of this
 * class:</p>
 *
 * <ol>
 *
 * <li>call {@link #start} (necessary in only one thread) to start the
 * internal server,</li>
 *
 * <li>call pairs of {@link #open open()} and {@link #close close()} to
 * filter data,</li>
 *
 * <li>eventually call {@link #shutdown} to stop the internal
 * server.</li> 
 *
 * </ol>
 *
 * <p>Filter servers to contact can be easily set up by running a
 * {@link TcpServer} with a {@link FilterServiceFactory}.<p>
 *
 * @author &copy; 2004, 2005 Harald Kirsch
 */

public class DistPipeFilter {
  private String host;
  private java.net.ServerSocket serverSocket;

  private monq.stuff.Semaphore sem;

  private Feeder[] pending;
  private TcpServer serv = null;
  private boolean running = false;
  private Map connections = new HashMap();
  
  /**********************************************************************/
  /**
   * <p>creates a <code>DistPipeFilter</code> on the given
   * port. Filters on the network will eventually contact back on this
   * port to read their input data. Don't forget to call {@link #start
   * start()} before calling one of the filter functions.</p>
   *
   * <p>If <code>DistPipeFilter</code> is no longer needed, it should be
   * {@link #shutdown shut down}.</p>
   *
   * @param port may be 0 to ask {@link java.net.ServerSocket} for an
   * arbitrary port.
   *
   * @param logging may be null to indicate that no logging is
   * required. 
   */
  public DistPipeFilter(int port, int slots, PrintStream logging) 
    throws java.io.IOException
  {
    // getting the socket ourselve instead of leaving it to TcpServer
    // allows to handle port=0 for automatic port selection properly
    serverSocket = new java.net.ServerSocket(port);

    this.host = InetAddress.getLocalHost().getHostAddress();

    this.sem = new monq.stuff.Semaphore(slots);
    this.pending = new Feeder[slots];

    serv = new TcpServer(serverSocket, new SourceFactory(), slots);
    if( logging!=null ) serv.setLogging(logging);
  }
  public DistPipeFilter(int port, int slots) 
    throws java.io.IOException
  {
    this(port, slots, null);
  }
  /**********************************************************************/
  /**
   * <p>start serving. Only after starting the internal server, {@link
   * #open open()} may be called. The internal server is run in a
   * daemon thread, i.e. it is eventually automatically terminated if
   * the JVM exits.</p>
   *
   * <p>Calling this method when the server is already running has no
   * effect.</p> 
   */
  public synchronized void start() {
    if( running ) return;
    running = true;
    Thread t = new Thread(serv);
    t.setDaemon(true);
    t.start();
  }
  /**********************************************************************/
  /**
   * <p>shuts down the {@link TcpServer} running on our behalf. This
   * does not influence any filter operations already
   * running. However, active calls to one of the filter functions
   * waiting for a slot to become free will not succeed anymore.</p>
   */
  public synchronized void shutdown() {
    if( !running ) return;
    running = false;
    serv.shutdown();
  }
  /**********************************************************************/
  /**
   * a wrapper around the Feeder to make sure that even uncaught
   * exceptions have a chance to be reported in close();
   */
  private static class CatchAllFeeder implements Feeder {
    private Exception e;
    private Feeder client;
    public CatchAllFeeder(Feeder client) { this.client = client; }
    public Exception getException() { 
      Exception ee = client.getException();
      if( ee!=null ) return ee;
      return e;
    }
    public void setOut(OutputStream out, boolean b) { 
      client.setOut(out, b);
    }
    public void run() {
      try {
	client.run();
      } catch( Exception e ) {
	this.e = e;
      }
    }
  }
  /**********************************************************************/
  /**
   * This one gets called whenever a server out there connects back
   * here to fetch its input data.
   *
   * NOTE ON Exceptions: If an exception happens here, pending[slot]
   * will not be set to null. As a result the filter() methods will
   * throw an exception telling that the data was never asked
   * for. Apart from that, the exception is logged by our TcpServer if
   * logging was requested.
   */
  private class SourceFactory implements ServiceFactory {   
    public Service createService(InputStream controlIn, 
				 OutputStream filterOut, Object param) 
      throws ServiceCreateException
    {
      FilterConnection conn;
      try {
	conn = new FilterConnection(controlIn);
	//System.err.println("SourceFactory: connection parsed");
      } catch( java.io.IOException e ) {
	throw new ServiceUnavailException("cannot read or parse request", e);
      }
      String slotString = (String)conn.getParameters().get("slot");

      // FIX ME: (SECURITY) The slot info should be
      // encrypted. Otherwise any bugger may interfere and fetch data
      // by trying slots
      int slot;
      try {
	slot = Integer.parseInt(slotString);
      } catch( NumberFormatException e ) {
	throw new ServiceUnavailException("slot received is not a number", e);
      }
      
      if( pending[slot]==null ) {
	throw new ServiceUnavailException
	  ("no data waiting to be shipped in slot "+slot);
      }
      //System.err.println("SourceFactory: starting feeder for slot "+slot);

      // we replace the Feeder with a CatchAllFeeder in the slot. The
      // CatchAllFeeder catches all exceptions and by checking the
      // class of the slot we know if we ever came here.
      Feeder r = new CatchAllFeeder(pending[slot]);
      pending[slot] = r;
      r.setOut(filterOut, false);
      return r;
    }
  }
  /**********************************************************************/
//   /**
//    * <p>filters a text through the given pipeline and appends the result
//    * to <code>out</code>.</p>
//    *
//    * @param request describes the pipeline
//    * @param in is the text to filter
//    * @param csSend is the name of the {@link
//    * java.nio.charset.Charset} to be used for encoding
//    * <code>in</code> when sending it into the pipeline
//    * @param out gets the result appended
//    * @param csReceive is the name of the <code>Charset</code> that
//    * must be used to decode the pipeline's result.
//    */
//   public void filter(PipelineRequest[] request, 
// 		     CharSequence in, String csSend,
// 		     StringBuffer out, String csReceive) 
//     throws IOException 
//   {
//     char[] buf = new char[1024];
//     Feeder fin = new CharSequenceFeeder(in, csSend);
//     InputStream is = open(request, fin);
//     InputStreamReader ir = new InputStreamReader(is, csReceive);
//     int l;
//     while( -1!=(l=ir.read(buf, 0, buf.length)) ) out.append(buf, 0, l);
//     close(is);
//   }
  /**********************************************************************/
  private static final class Job {
    public int slot;
    public FilterConnection connection;
    public Job(int slot, FilterConnection connection) {
      this.slot = slot;
      this.connection = connection;
    }
  }
  /**********************************************************************/
  /**
   * <p>sets up the pipeline as determined by <code>request</code> to be
   * fed by the given <code>Feeder</code>. The filtered data can be
   * read from the <code>InputStream</code> returned. This
   * <code>InputStream</code> <b>must</b> eventually be passed to
   * {@link #close close()} to properly terminate the connection to
   * the pipeline.</p>
   *
   * <p><b>Hint:</b> A <code>Feeder</code> can be easily implemented by
   * subclassing {@link monq.stuff.AbstractPipe}.</p>
   */
  public InputStream open(PipelineRequest[] request, Feeder in) 
    throws IOException
  {
    if( !running ) throw new IllegalStateException("server not running");

    // acquire semaphore and put the feeder into the resulting slot
    // It will be picked up by createService as soon as the last
    // upstream server connects back to us to fetch the input data
    int slot = sem.acquire();
    pending[slot] = in;


    // get the FilterConnection and append ourselves to the request
    // string
    PipelineRequest myAddress = 
      new PipelineRequest(host, serverSocket.getLocalPort());
    myAddress.put("slot", Integer.toString(slot));
    FilterConnection connection = new FilterConnection(request);
    connection.append(myAddress);

    InputStream filterIn;
    try {
      // This will eventually contact back to ourselves, starting the
      // Feeder put in pending[slot]
      filterIn = connection.connect();
    } catch( IOException e ) {
      pending[slot] = null;
      sem.release(slot);
      throw e;
    }
    connections.put(filterIn, new Job(slot, connection));
    return filterIn;
  }
  /**********************************************************************/
  /**
   * <p><b>must</b> be called with an <code>InputStream</code> as
   * returned by {@link #open open()} eventually to release the
   * connection resources used to contact the pipeline.</p>
   *
   * @throws IllegalArgumentException if <code>is</code> was either
   * closed already or was never returned by {@link #open open()}.
   *
   * @throws IOException if either closing the underlying socket
   * connection throws this exception, or if the {@link Feeder} for
   * this connection was never asked to deliver the data. The latter
   * happens if the pipeline does not connect back to this class to
   * fetch the data.
   */
  public void close(InputStream is) throws IOException {
    Job job;
    synchronized(connections) {
      job = (Job)connections.remove(is);
    }
    
    // if this is a fake or was closed before, get angry
    if( job==null ) {
      throw new IllegalArgumentException
	("connection bound to given InputStream is closed already "+
	 "or was never open");
    }

    Feeder f = pending[job.slot];
    pending[job.slot] = null;
    sem.release(job.slot);

    // now do the things that might throw an exception
    job.connection.close();
    
    if( !(f instanceof CatchAllFeeder) ) {
      // This job was never run, meaning that one of the servers
      // involved closed the connection without ever contacting upstream
      // back to us.
      throw new IOException
	("The data was never asked for. This means that the pipeline "
	 +"collapsed somewhere in the middle. More information can "
	 +"only be found in the logs of intermediate servers.");
    }
    Exception e = f.getException();
    if( e==null ) return;

    if( e instanceof IOException ) throw (IOException)e;

    IOException ioe = new IOException("feeder threw exception, see cause");
    ioe.initCause(e);
    throw ioe;
  }
  /**********************************************************************/
}
