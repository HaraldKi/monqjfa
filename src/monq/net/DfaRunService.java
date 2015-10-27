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
import java.io.*;

/**
 * <p>convenience class which makes a {@link monq.jfa.DfaRun} into a
 * <em>service</em> that can be returned by a {@link
 * ServiceFactory}.</p>
 * @author &copy; Harald Kirsch
 * @version $Revision: 1.5 $, $Date: 2005-02-14 10:23:38 $
 */
public class DfaRunService implements Service {
  private final DfaRun r;
  private final PrintStream out;
  private Exception e;
  /**
   * <p>creates a <code>Runnable</code> which will run <code>r</code> as
   * a filter which sends its output to <code>out</code>.</p> 
   */
  public DfaRunService(DfaRun r, PrintStream out) {
    this.r = r;
    this.out = out;
  }
  public Exception getException() {return e;}
  public void run() {
    try {
      r.filter(out);
    } catch( java.io.IOException e ) {
      this.e = e;
    }
  }
  /**********************************************************************/
}
