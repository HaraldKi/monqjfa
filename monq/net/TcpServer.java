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

import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;

/**
 * a simple socket server able to serve multiple client
 * connections in parallel. Using this class make sense, if the
 * service to be provided can be coded as a transformation of data
 * read from an <code>java.io.InputStream</code> and then written to a
 * <code>java.io.OutputStream</code>. To be able to serve several
 * clients in parallel, a new service object is needed for every
 * connection. Therefore the constructor has a
 * {@link ServiceFactory} as a parameter rather than a service
 * object.</p>
 *
 * <p>A typical use of this class involves the following steps:
 * <ol>
 * <li>Write a class implementing <code>java.lang.Runnable</code>.
 * Its <code>run()</code> method should read input from an
 * <code>InputStream</code>, transform it
 * and write the result to an <code>OutputStream</code>.</li>
 * <li>Write a class implementing a
 * {@link ServiceFactory} to
 * return instances of the above class.</li>
 * <li>Create a <code>TcpServer</code> with the factory.</li>
 * <li>Call the {@link #serve} method of the
 * <code>TcpServer</code>.</li>
 * </ol>
 * If you stick your <code>ServiceFactory</code> into a {@link
 * FilterServiceFactory} between steps 2 and 3 and pass the result to the
 * <code>TcpServer</code>, you get a pipe filter which can be used in
 * conjunction with other pipe filters and with {@link DistPipeFilter}
 * to built a distributed pipe on the network.
 * </p>
 *
 * <p>As soon as clients contact the server, it will call the factory
 * method to fetch a new service instance. It connects it to the input
 * and output stream from the client, wraps it up in a
 * <code>Thread</code> and starts it. By default 10 such service
 * threads can be running in parallel. When a thread finishes,
 * <code>TcpServer</code> takes care to close the streams.</p>
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class TcpServer implements Runnable {

  private ServerSocket socket;
  private ServiceFactory fac;
  private Thread[] services;
  private PrintStream log = null;
  private java.text.SimpleDateFormat date;
  private String myName;

  // will be set to true if also debug logging shall be enabled
  private boolean loggingDebug;

  /**
   * <p>creates a server to listen on the given socket. Connection
   * request are honoured with whatever service the given {@link
   * ServiceFactory} delivers. Up to <code>slots</code> services are
   * run in parallel. If all slots are filled, incoming requests are
   * put on hold by the underlying tcp machinery.</p>
   *
   * <p><b>Important:</b> The services created by <code>fac</code>
   * should <b>not</b> close the input and output stream passed to
   * them. If they close one of them, this immediately closes the
   * socket and thereby the other one too. Both streams are close by
   * the server itself after the service thread finishes.</p>
   */
  public TcpServer(ServerSocket s, ServiceFactory fac, int slots) {
    this.socket = s;
    this.fac = fac;
    this.services = new Thread[slots];
    this.myName = "TcpServer("+fac+")";

  }
  /**
   * <p>creates a server to listen on the given port. Up to 10
   * connections are serviced in parallel.</p>
   *
   * @throws IOException if no server socket can be opened for the
   * given port.
   */
  public TcpServer(int port, ServiceFactory fac) 
    throws java.io.IOException
  {
    this(new ServerSocket(port), fac, 10);
  }
  public TcpServer(int port, ServiceFactory fac, int slots) 
    throws java.io.IOException
  {
    this(new ServerSocket(port), fac, slots);
  }
  /**********************************************************************/
  private class ServiceShell extends Thread {
    private int slot;
    private Socket io;
    private Service svc;
    private InputStream in;
    private OutputStream out;

    public ServiceShell(int slot, Service svc,
			InputStream in, OutputStream out, Socket io) {
      this.slot = slot;
      this.svc = svc;
      this.in = in;
      this.out = out;
      this.io = io;
    }
    public void run() {
      Throwable e = null;

      // Catch OOME and (mostly) ignore it. Since this thread is about
      // to finish, we may recover.
      try {
	svc.run();
	e = svc.getException();
      } catch( Throwable ugly ) {
	// this is serious, but in principle we can envisage that the
	// service factory creates different services. Some may be
	// completely flawed, throwing java.lang.Errors, while others
	// work fine. Therefore we try to keep going.
	// XXX: other idea? Please let me know.
	if( log!=null ) log("error", "will try to keep going\n", ugly);
	else {
	  System.err.println("TcpServer: severe error. Cause is:");
	  ugly.printStackTrace(System.err);
	}
      }
      if( e!=null ) {
	if( e instanceof ServiceCreateException 
	    && !(e instanceof ServiceUnavailException) ) {
	  log("error", "service in slot "+slot
	      +" threw ServiceCreateException indicating an unrecoverable "
	      +"error; shutting down server.\n", e);
	  shutdown();
	} else {
	  log("error", "service in slot "+slot+" threw Exception\n", e);
	}
      }
      closeAll(slot, in, out, io);

      clearSlot(slot);
      
    }
  }  
  /**********************************************************************/
  private void closeAll(int slot, 
			InputStream in, OutputStream out, Socket io) {
    // FIX ME: yes, I know that if the first one throws already, the
    // other two may be left open, but I dunno anyway yet when this
    // happens. 
    try {
      in.close();
      out.close();
      io.close();
    } catch( java.io.IOException e ) {
      log("error", "slot "+slot+", problems closing client's i/o in ", e);
    }
  }
  /**********************************************************************/
  private void log(String what, CharSequence s, Throwable e) {
    if( log==null ) return;
    synchronized(log) {
      log.print(date.format(new Date()));
      log.print("(");
      log.print(what);
      log.print("): ");
      log.print(s);
      if( e!=null ) {
	log.println("<stack>");
	e.printStackTrace(log);
	log.println("</stack>");
      }
    }
  }
  private void log(String what, CharSequence s) {
    if( !loggingDebug ) return;
    log(what, s, null);
  }
  /**********************************************************************/
  private void clearSlot(int slot) {
    //System.err.println("clearing slot "+slot);
    log("done", "slot "+slot+" done\n");
    synchronized(services) {
      services[slot] = null;
      services.notifyAll();
    }
    //System.err.println("slot "+slot+" now cleared");    
  }
  /**********************************************************************/
  /**
   * @return this
   */
  public synchronized TcpServer setLogging(PrintStream log) {
    date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm.ss");
    this.log = log;
    return this;
  }

  /**
   * <p>given that logging was enabled with {@link #setLogging
   * setLogging()}, we now switch to really verbose logging.</p>
   * @return this
   */
  public synchronized TcpServer setDebug() {
    loggingDebug = true;
    return this;
  }
  /**********************************************************************/
  /**
   * <p>calls {@link #serve} and catches exceptions to write them to the
   * log. This method is only here so that a <code>TcpServer</code>
   * itself can be put into a <code>Thread</code>. If the server
   * activity is anyway the main operation of your program, just call
   * {@link #serve}.</p>
   */
  public void run() {
    try {
      serve();
    } catch( java.io.IOException e ) {
      if( log!=null ) {
	log("fail", "server exited due to exception ", e);
      } else {
	e.printStackTrace();
      }
    }
  }
  /**********************************************************************/
  /**
   * <p>closes the socket on which the server is accepting connections
   * with the effect, that {@link #serve} immediately returns. Any
   * currently running service threads are not affected.</p>
   */
  public void shutdown() {
    log("shutdown", "\n", null);
    try {
      if( !socket.isClosed() ) socket.close();
    } catch( java.io.IOException e ) {
      throw new Error("should not happen", e);
    }
  }
  /**********************************************************************/
  /**
   * <p>starts the server, waiting for connections and serving up to
   * <em>n</em> requests in parallel. The value <em>n</em> is the
   * number of slots defined in the constructor. This method only
   * returns if the current thread's <code>interrupt()</code> method
   * is called. Note, however, that services not yet finished are just
   * left running. They are not forcibly shut down.
   */
  public synchronized void serve() throws java.io.IOException 
  {
    log("startup", this+" port "+socket.getLocalPort()
	+" for "+services.length+" slots\n", null);
   
    while( true ) {
      // find free slot in services
      int slot;
      while( true ) {
	for(slot=0; slot<services.length && services[slot]!=null; slot++) ;
	if( slot<services.length ) break;

	synchronized(services) {
	  // There is no slot available, but as soon as a slot becomes
	  // free, we are woken up by the ServiceShell.
	  log("blocked", this+" waiting for job to finish\n");
	  try {
	    services.wait();
	  } catch( InterruptedException e ) {
	    // we got an interrupt() as opposed to a notify. We take
	    // this as a signal to shut down the server.
	    // The interrupted state will be tested right after the
	    // loop again
	    break;
	  }
	  //log("free", this+" not blocked anymore\n");
	}
      } 

      if( Thread.currentThread().interrupted() ) {
	shutdown();
	return;
      }

      //System.err.println("accepting slot "+slot);
      Socket io;
      try {
	io = socket.accept();
      } catch( java.net.SocketException e ) {
	if( socket.isClosed() ) {
	  // someone called shutdown, this is ok
	  return;
	}
	throw e;
      }

      InetAddress client = io.getInetAddress();
      log("connect", "slot "+slot+" for "+client.getHostAddress()+'\n');

      // prepare the service to serve the socket just created
      InputStream in = io.getInputStream();
      OutputStream out = io.getOutputStream();
      Service svc = null;
      try {
	svc = fac.createService(in, out, null);
      } catch( ServiceCreateException e ) {
	// this is a bit annoying as I am not really sure if it is
	// sufficient to close just io and why there could be an
	// IOException 
	closeAll(slot, in, out, io);

	log("error", "slot "+slot+
	    ", could not create service because\n", e);
	if( !(e instanceof ServiceUnavailException) ) {
	  // the service is principally out of order, there is no need
	  // to try again
	  shutdown();
	  return;
	}

	continue;
      }

      log("starting", "slot "+slot+", service "+svc+"\n");
   
      services[slot] = new ServiceShell(slot, svc, in, out, io);
      services[slot].start();
    }
  }
  /**********************************************************************/
  public String toString() {
    return myName;
  }
  /**********************************************************************/
  
}
