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
 * <p>the dual to {@link Feeder}.</p>
 *
 * <p><b>Hint:</b> A <code>Drainer</code> can be easily implemented by
 * subclassing {@link AbstractPipe}.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public interface Drainer extends monq.net.Service {

  /**
   * <p>specifies the <code>InputStream</code> where to fetch the
   * data in the <code>run()</code> method.</p>
   *
   * @param closeOnExit if true, instructs the <code>Drainer</code> to close
   * <code>in</code> before exiting <code>run()</code>.
   */
  void setIn(java.io.InputStream out, boolean closeOnExit);
}
