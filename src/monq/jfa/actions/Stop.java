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

package monq.jfa.actions;

import monq.jfa.*;
/**
 * <p>implements an {@link FaAction} which resets the input of the
 * calling {@link DfaRun} to the empty string by calling
 * <code>DfaRun.setIn(null)</code>. If the <code>DfaRun</code>'s
 * <code>Dfa</code> has not defined an action to invoke at EOF, input
 * processing will stop immediatly after returning from this
 * action.</p>
 *
 * <p>This action leaves the matching text untouched. To do something
 * with it, consider using a {@link Run} action.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 * @version $Revision: 1.3 $, $Date: 2005-11-03 08:50:25 $
 */
public class Stop extends AbstractFaAction {
  /**
   * <p>a predefined {@link FaAction} with priority 0 which 
   * sets the input of the calling {@link DfaRun} to the empty
   * string. Only if priority 0 is not
   * what you want, create a new {@link Stop} object.
   */
  public static final FaAction STOP = new Stop();

  private Stop() {}
  /** 
   * use only if {@link Stop#STOP} is not what you want, because you
   * have to specify a priority.
   */
  public Stop(int priority) {this.priority = priority;}
  public void invoke(StringBuffer out, int start, DfaRun runner) {
    runner.setIn(new EmptyCharSource());
  }
  public String toString() {return "Stop";}
}
