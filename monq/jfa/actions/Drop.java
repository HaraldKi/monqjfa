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
 * implements an {@link FaAction} which drops (deletes) matched
 * input. 
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.4 $, $Date: 2005-02-14 10:23:38 $
 */
public class Drop extends AbstractFaAction {
  /**
   * a predefined {@link FaAction} with priority 0 which drops
   * (deletes) matched text such that nothing is transfered to the
   * output.  Only if priority 0 is not what you want, create a new
   * {@link Drop} object.
   */
  public static final FaAction DROP = new Drop();

  private Drop() {}
  
  /** 
   * use only if {@link Drop#DROP} is not what you want because you
   * have to specify a priority.
   */
  public Drop(int priority) {this.priority = priority;}
  public String toString() {return "Drop";}
  public void invoke(StringBuffer out, int start, DfaRun runner) {
    out.setLength(start);
  }
}
