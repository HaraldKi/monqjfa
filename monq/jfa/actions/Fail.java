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
 * <p>implements an {@link FaAction} which unconditionally throws a
 * {@link CallbackException} with the message specified in the
 * constructor.</p> 
 *
 * @author &copy; 2005 Harald Kirsch
 * @version $Revision: 1.1 $, $Date: 2005-02-24 22:30:02 $
 */
public class Fail extends AbstractFaAction {
  private final String msg;

  /**
   * <p>create an action which fails with the given message.</p>
   */
  public Fail(String message) { this.msg = message; }

  public void invoke(StringBuffer out, int start, DfaRun runner) 
    throws CallbackException
  {
    throw new CallbackException(msg);
  }
  public String toString() {
    return "Fail["+msg+"]";
  }
}
