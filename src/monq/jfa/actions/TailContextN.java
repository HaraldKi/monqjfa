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
 * implements an {@link FaAction} which pushes a trailing context back
 * into the input and then calls a client action.
 * into two fixed strings.
 * @author &copy; 2005 Harald Kirsch
 * @version $Revision: 1.5 $, $Date: 2005-07-08 13:01:26 $
 */
public class TailContextN extends AbstractFaAction {
  private int n;
  private FaAction client;
  /**
   * <p>creates an {@link FaAction} to push back the given number of
   * characters into the input and then calls the given action.</p>
   * @param n is the number of characters to push back. This must
   * &ge;0. While <code>n==0</code> is allowed, it is obviously a
   * waste of time.
   * @param client is the action to call. It may be <code>null</code>
   * which is equivalent to {@link Copy}.
   */
  public TailContextN(int n, FaAction client) {
    if( n<0 ) throw new IllegalArgumentException("n must be >=0, but is "
						 +n);
    this.n = n;
    this.client = client;
  }
  /**
   * <p>creates an {@link FaAction} to push back the given number of
   * characters into the input and leaves the rest of the match
   * untouched. When the callback is called, there must be enough
   * characters to push back.</p>
   */
  public TailContextN(int n) { this(n, null); }
  public void invoke(StringBuffer out, int start, DfaRun r) 
    throws CallbackException 
  {
    r.unskip(out, out.length()-n);
    if( client!=null ) client.invoke(out, start, r);
  }
}
 
