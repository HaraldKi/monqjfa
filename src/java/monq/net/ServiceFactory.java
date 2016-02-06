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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>an implementation of this interface is used by a {@link
 * TcpServer} to fetch a fresh {@link Service} for each incoming
 * connection.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public interface ServiceFactory {
  /** 
   * <p>creates a {@link Service} that reads from the given input
   * stream, processes it and writes the result to the given output
   * stream.</p>
   *
   * <p>This method should return as fast as possible, because it is
   * run in the <code>TcpServer</code>'s main thread and thereby no
   * other, parallel connections can be initiated while this method is
   * at work. In particular nothing that can block the thread, like
   * reading input, should be done in this method. Either move setup
   * code into the constructor of the <code>ServiceFactory</code> or,
   * if it is connection related, move it into the <code>run()</code>
   * method of the <code>Service</code> itself.</p>
   * <p>A similar note obviously applies to the constructor of the
   * <code>Service</code> which is most probably called by this
   * method.</p> 
   *
   * <p><b>Notice:</b> The <code>Service</code> created should <em>not
   * close</em> the streams. While it would be logical to close the
   * streams after reaching eof, this does not work well with streams
   * originating from a socket. Consequently the method which calls a
   * <code>ServiceFactory</code> has to take care to eventually close
   * the streams. It is, however, a good idea to flush the output
   * stream.</p>
   *
   * @param param is an arbitrary parameter object that may be used to
   * tweak the service created beyond its input and output
   * stream. {@link TcpServer} sets it to <code>null</code>, but when
   * stacking up service factories (like with {@link
   * FilterServiceFactory}), this is useful.
   *
   * @throws ServiceCreateException if the service is permanently
   * unavailable. To indicate that the service may be created the next
   * time this method is called, use {@link ServiceUnavailException}.
   */
  Service createService(InputStream in, OutputStream out, Object param)
    throws ServiceCreateException;
}

