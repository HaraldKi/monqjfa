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
 * <p>implements an {@link FaAction} which applies 
 * <code>Character.toLowerCase</code> to every character of the
 * match.</p>
 * <p><b>Hint:</b> Instead of attaching this callback to the regular
 * expression <code>"[A-Z]"</code>, rather use <code>"[A-Z]+"</code>
 * to save some callbacks.</p>
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.6 $, $Date: 2005-02-20 21:12:17 $
 */
public class LowerCase extends AbstractFaAction {
  /**
   * a predefined {@link FaAction} with priority 0 which applies
   * <code>Character.toLowerCase()</code> to every character of the
   * match. Only if priority 0 is not what you want, create a new
   * {@link LowerCase} object.
   */
  public static final FaAction LOWERCASE = new LowerCase();

  public LowerCase() {}
  public LowerCase(int prio) {this.priority = prio;}
  public void invoke(StringBuffer out, int start, DfaRun runner) {
    int L = out.length();
    while( start<L ) {
      out.setCharAt(start, Character.toLowerCase(out.charAt(start)));
      start += 1;
    }
  }
}
