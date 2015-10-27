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
 * <code>Character.toUpperCase</code> to every character of the
 * match.</p>
 * <p><b>Hint:</b> Instead of attaching this callback to the regular
 * expression <code>"[a-z]"</code>, rather use <code>"[a-z]+"</code>
 * to save some callbacks.</p>
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.7 $, $Date: 2005-10-06 10:26:35 $
 */
public class UpperCase extends AbstractFaAction {
  /**
   * a predefined {@link FaAction} with priority 0 which applies
   * <code>Character.toUpperCase()</code> to every character of the
   * match. Only if priority 0 is not what you want, create a new
   * {@link UpperCase} object.
   */
  public static final FaAction UPPERCASE = new UpperCase();

  private UpperCase() {}
  public UpperCase(int prio) {this.priority = prio;}
  public void invoke(StringBuffer out, int start, DfaRun runner) {
    int L = out.length();
    while( start<L ) {
      out.setCharAt(start, Character.toUpperCase(out.charAt(start)));
      start += 1;
    }
  }
}
