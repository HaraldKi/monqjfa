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
 * <p>implements an {@link FaAction} which changes the {@link Dfa}
 * operated by the calling {@link DfaRun}. This type of action is
 * useful whenever different parts of the input shall be handled by
 * different <code>Dfa</code>s.</p>
 *
 * <p>In a typical example, the input contains long stretches of
 * irrelevant text interspersed by parts which need closer
 * examination. Given that start and end of the relevant parts can
 * be detected by regular expressions <em>rIn</em> and
 * <em>rOut</em>, two <code>Dfa</code>s can be set up like
 * this:<pre>
 *   SwitchDfa toWork = new SwitchDfa(Drop.DROP);
 *   SwitchDfa toSkip = new SwitchDfa(Drop.DROP);
 *   Dfa skip = new Nfa(rIn, toWork)
 *                  .or(  other stuff )
 *                  .compile(DfaRun.UNMATCHED_DROP);
 *   Dfa work = new Nfa(rOut, toSkip)
 *                  .or(  other stuff )
 *                  .compile(DfaRun.UNMATCHED_COPY);
 *   // don't forget to set up the switches.
 *   toWork.setDfa(work);
 *   toSkip.setDfa(skip);
 *   DfaRun r = new DfaRun(skip, ...);</pre>
 *
 * When <code>rIn</code> is detected, <code>toWork.invoke()</code>
 * gets called which switches the calling <code>DfaRun</code> to run
 * the automaton <code>work</code>. When later the end of the
 * relevant part is detected by a match with <code>rOut</code>, the
 * a call to <code>toSkip.invoke()</code> switches back to automaton
 * <code>skip</code>. Because of the mutual reference between the
 * automata and objects of this class, the constructor does not take
 * a <code>Dfa</code> as an argument. Rather {@link #setDfa setDfa()}
 * must be used.</p>
 *
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.8 $, $Date: 2005-09-19 14:01:58 $
 */
public class SwitchDfa extends AbstractFaAction {
  Dfa dfa;

  private FaAction action;
  
  /**
   * <p>creates an {@link FaAction} to switch the calling
   * <code>DfaRun</code> object to another <code>Dfa</code>. The
   * <code>Dfa</code> must be set later with {@link #setDfa
   * setDfa()}. The parameters influence the operation of the {@link
   * #invoke invoke()} callback as follows:</p>
   *
   * <ol>
   * <li>The <code>Dfa</code>, which you have to specify by
   * calling {@link #setDfa}, will be entered into the calling
   * {@link DfaRun}.</li>
   * <li>If <code>action</code> is not
   * <code>null</code>, the action will then be invoked. Without action,
   * the triggering text <strong>will</strong> show up in the
   * output. To drop it, set <code>action</code> to {@link
   * monq.jfa.actions.Copy#COPY}.</li> 
   * </ol>
   *
   */
  public SwitchDfa(FaAction action) {
    this.action = action;
  }
  /**
   * <p>calls the 1 parameter constructor with
   * <code>action==null</code>.</p>
   */
  public SwitchDfa() {this(null);}
  /**
   * <p>sets the <code>Dfa</code> this action switches the calling
   * <code>DfaRun</code> to. This function <b>must</b> be called
   * once before the action is used.</p>
   */
  public void setDfa(Dfa dfa) { 
    if( dfa.matchesEmpty() ) {
      throw new IllegalArgumentException("dfa matches the empty string");
    }
    this.dfa = dfa; 
  }
  public void invoke(StringBuffer out, int start, DfaRun r) 
    throws CallbackException
  {
    r.setDfa(dfa);
    if( action!=null ) action.invoke(out, start, r);
  }
}
 
