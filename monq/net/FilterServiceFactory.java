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

import java.io.*;
import java.net.Socket;
import java.util.Hashtable;

/**
 * <p>sets up a filter service as returned by any {@link
 * ServiceFactory} as an element of a <em>distributed pipe</em>. A
 * filter denotes a method thats reads from an
 * <code>InputStream</code> and writes to an
 * <code>OutputStream</code>. A {@link ServiceFactory} creates such a
 * filter and is normally used by {@link TcpServer} to create a new
 * filter instance for every connection it receives. This class is a
 * filter service which fetches input from another server further
 * upstream. Details can be found in the description of {@link
 * #createService createService()}.</p> <p>A typical use involves the
 * following steps:</p>

 * <ol>
 *
 * <li>Write a class that implements a {@link Service}, i.e. it has a
 * <code>run()</code> method that reads data from an
 * <code>InputStream</code>, possibly manipulates it, and then sends
 * it to an <code>OutputStream</code>. Lets call this class
 * <code>Blorg</code>. Normally it will have a constructor like</p>
 *
 * <pre>Blorg(InputStream, OutputStream)</pre>
 *
 * </li>
 *
 * <li>Create an object of <code>FilterServiceFactory</code>:
 *
 * <pre>FilterServiceFactory fsf = 
 *   new FilterServiceFactory(new ServiceFactory() {
 *     public Service createService(InputStream in, 
 *                                  OutputStream out) {
 *       return new Blorg(in, out);
 *     }
 *   });</pre>
 * </li>
 *
 * <li>Set up the server with a <code>TcpServer</code>:<pre>
 * int port = 3344;
 * new TcpServer(port, fsf).setLogging(System.err).serve();</pre>
 * </li>
 * </ol>
 *
 * @author &copy; 2004 Harald Kirsch
 * @version $Revision: 1.12 $, $Date: 2005-11-09 12:13:45 $
 */
public class FilterServiceFactory implements ServiceFactory {

  private ServiceFactory clientFactory;
  /**********************************************************************/
  /**
   * <p>create a filter service factory to connect services created by
   * <code>clientFactory</code> to an upstream server.</p>
   */
  public FilterServiceFactory(ServiceFactory clientFactory) {
    this.clientFactory = clientFactory;
  }
  /**
   * <p>create a filter service factory to connect services created by
   * <code>clientFactory</code> to an upstream server. A sequence of
   * upstream servers can be specified in <code>preload</code>. Only
   * the last server listed in <code>preload</code> will connect to
   * the server transmitted via the control channel.</p>
   */
//   public FilterServiceFactory(ServiceFactory clientFactory, 
// 			      String preload) {
//     this.clientFactory = clientFactory;
//     this.preload = preload;
//   }
  /**********************************************************************/
  /**
   * <p>contacts an upstream server and connects a service to the
   * resulting input and the given output. The following steps are
   * performed.</p>
   *
   * <ol>
   * <li>Read a request from <code>controlIn</code>. The request must
   * originate from a {@link PipelineRequest}, i.e. it was typically
   * send with a {@link DistPipeFilter}.</li>
   * <li>The upstream server is contacted. As a result there is an input
   * stream available from the upstream server.</li>
   * <li>The client service factory specified in the constructor is
   * called to create a service. It will have its input from the
   * upstream server and its output is <code>filterOut</code> Any key
   * value pairs read from <code>controlIn</code> orgininating from a
   * {@link PipelineRequest} object will be passed as a {@link
   * java.util.Map} to the client service factory.</li>
   * </ol>
   * <p>The resulting service is returned.
   *
   * @param param is <b>ignored</b> because parameters for the client
   * service factory are read from <code>controlIn</code>.
   *
   * @throws ServiceCreateException as thrown by the client service factory
   * @throws ServiceUnavailException as thrown by the client service factory
   */
  public Service createService(InputStream controlIn, 
			       OutputStream filterOut, Object param) 
    throws ServiceCreateException
  {
    return new FilterService(controlIn, filterOut, clientFactory);
  }
  /**********************************************************************/
  private static class FilterService implements Service {
    private InputStream controlIn;
    private OutputStream filterOut;
    private ServiceFactory fac;
    private Service svc;
    private Exception e = null;

    public FilterService(InputStream controlIn, OutputStream filterOut, 
			 ServiceFactory fac) {
      this.controlIn = controlIn;
      this.filterOut = filterOut;
      this.fac = fac;
    }
    public Exception getException() {return e;}
    public void run() {

      FilterConnection conn = null;
      try {
	conn = new FilterConnection(controlIn);
      } catch( IOException e ) {
	this.e = e;
	return;
      }

      Service svc = null;
      try {
	InputStream filterIn = conn.connect();
	svc = fac.createService(filterIn, filterOut, conn.getParameters());
      } catch( IOException e ) {
	// we cannot just return here, because we have to close the
	// two streams in any case.
	this.e = e;
      }

      if( svc!=null ) {
	svc.run(); 
	e = svc.getException();
      }

      try {
	conn.close();
      } catch( java.io.IOException e ) {
	e.printStackTrace(System.err);
	throw new Error("what am I supposed to do with this?", e);
      }
    }
  }
  /**********************************************************************/
}
