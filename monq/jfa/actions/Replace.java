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
 * implements an {@link FaAction} which replaces the matched input
 * with a fixed string.
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.5 $, $Date: 2005-02-20 21:13:50 $
 */
public class Replace extends AbstractFaAction {
  private String s;
  /**
   * calls the 2 parameter constructor with
   * <code>priority==0</code>.
   */
  public Replace(String s) { this(s, 0); }
  /**
   * creates an {@link FaAction} with the given priority which
   * replaces the matched text with the string given.
   */
  public Replace(String s, int prio) {
    this.priority = prio;
    this.s = s;
  }
  public void invoke(StringBuffer out, int start, DfaRun runner) {
    out.setLength(start);
    out.append(s);
  }
  public String toString() {
    StringBuffer sb = new StringBuffer(30);
    sb.append(super.toString())
      .append("[\"").append(s).append("\"]")
      ;
    return sb.toString();
  }
  public boolean equals(Object _o) {
    if( !(_o instanceof Replace) ) return false;
    Replace o = (Replace)_o;
    return s.equals(o.s) && priority==o.priority;
    
  }
}
