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
 * <p>contains actions to switch the default behaviour of a {@link
 * monq.jfa.DfaRun} object.</p>
 *
 * <p>The static {@link FaAction} objects supplied here do not allow
 * to change their priority. If it is really necessary to have such an
 * object with a different priority than zero, wrap it into a {@link
 * Run} object and call {@link Run#setPriority} for
 * it.</p>
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class Unmatched {

  // nothing to instantiate
  private Unmatched() {}

  /**
   * <p>change the default behaviour of the calling {@link
   * monq.jfa.DfaRun} to {@link monq.jfa.DfaRun#UNMATCHED_DROP}.</p>
   */
  // REMINDER: because the type is FaAction, setPriority cannot
  // immediately be called. If someone casts and then messes it up, it
  // is not my fault, right?
  public static final FaAction DROP = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun r) {
	r.setOnFailedMatch(DfaRun.UNMATCHED_DROP);
      }
    };

  /**
   * <p>change the default behaviour of the calling {@link
   * monq.jfa.DfaRun} to {@link monq.jfa.DfaRun#UNMATCHED_COPY}.</p>
   */
  public static final FaAction COPY = new AbstractFaAction() {
      //public int getPriority() {return 0;}
      public void invoke(StringBuffer yytext, int start, DfaRun r) {
	r.setOnFailedMatch(DfaRun.UNMATCHED_COPY);
      }
    };

  /**
   * <p>change the default behaviour of the calling {@link
   * monq.jfa.DfaRun} to {@link monq.jfa.DfaRun#UNMATCHED_THROW}.</p>
   */
  public static final FaAction THROW = new AbstractFaAction() {
      //public int getPriority() {return 0;}
      public void invoke(StringBuffer yytext, int start, DfaRun r) {
	r.setOnFailedMatch(DfaRun.UNMATCHED_THROW);
      }
    };

}
