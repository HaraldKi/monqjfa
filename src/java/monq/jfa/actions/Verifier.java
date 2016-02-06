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

import monq.jfa.DfaRun;

/**
 * <p>used by {@link If} callbacks to get a decision.  A
 * <code>Verifier</code> is expected to collect information by
 * whatever means until finally its {@link #ok} method is called.
 * @author &copy; 2004 Harald Kirsch
 * @version $Revision: 1.5 $, $Date: 2005-07-08 13:01:26 $
 */
public interface Verifier {
  /**
   * <p>classes implementing this interface are expected to collect
   * information over time. At any time they may be asked whether the
   * information is <em>good</em> or <em>bad</em>, <em>true</em> or
   * <em>false</em>, etc. The meaning of the return value
   * <code>true</code> depends solely on the implementation.</p>
   *
   * <p>Typically a <code>Verifier</code> is called from {@link If}
   * and will rely on additional information found in {@link
   * monq.jfa.DfaRun#clientData}. See for example {@link
   * Count#ge}.</p>
   */
  boolean ok(DfaRun r);
}
 
