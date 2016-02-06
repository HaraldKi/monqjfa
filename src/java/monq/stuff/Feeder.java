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

package monq.stuff;


/**
 * <p>Implementations of <code>Feeder</code> start feeding data
 * to the {@link java.io.OutputStream} passed to {@link #setOut
 * setOut()} as soon as their {@link #run} method is
 * called. The contract for use of a <code>Feeder</code> is as
 * follows:</p>
 * 
 * <ol>
 * <li><code>setOut()</code> is called first to specify where the
 * <code>Feeder</code> must deliver the data,</li>
 *
 * <li>then <code>run()</code> (according the inherited interface
 * {@link monq.net.Service}) is called for the the <code>Feeder</code>
 * to actually deliver the data,</li>
 *
 * <li>the <code>Feeder</code> closes the <code>OutputStream</code>
 * set with <code>setOut</code> depening on the second parameter of
 * <code>setOut</code> before <code>run()</code> terminates.</li>
 * </ol>
 *
 * <p><b>Hint:</b> A <code>Feeder</code> can be easily implemented by
 * subclassing {@link AbstractPipe}.</p>
 *
 * @see Drainer
 * @author &copy; 2005 Harald Kirsch
 */
public interface Feeder extends monq.net.Service {

  /**
   * <p>specifies the <code>OutputStream</code> where to deliver the
   * data in the <code>run()</code> method.</p>
   *
   * @param closeOnExit if true, instructs the <code>Feeder</code> to close
   * <code>out</code> before exiting <code>run()</code>.
   */
  void setOut(java.io.OutputStream out, boolean closeOnExit);
}
