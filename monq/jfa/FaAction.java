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

package monq.jfa;

/**
 * <p>describes the interface to objects implementing actions associated
 * with stop states of a finite automaton. By adding an
 * <code>FaAction</code> to an <code>Nfa</code> by means of {@link
 * Nfa#addAction} or when creating an <code>Nfa</code> (e.g. {@link
 * Nfa#Nfa(CharSequence, FaAction)}), the <code>Nfa</code> gets a stop
 * state having the given action associated. When the automaton is
 * compiled and run, method {@link #invoke} is called whenever the
 * automaton finds a match which stops the automaton in the associated
 * stop state. The callback then has the chance to change the matched
 * text, tag it, enter it into an internal data structure, etc.</p>
 *
 * @author <a href="mailto:pifpafpuf@gmx.de">Harald Kirsch</a>
 * @version 1.0
 */
public interface FaAction {

  /**
   * <p>returns a priority for the <code>FaAction</code> to break ties
   * with other actions. An automaton can be created such that it
   * contains many stop states or, put another way, which matches
   * several regular expressions, each associated with a different
   * <code>FaAction</code>. Sometimes the set of matched strings of
   * two of the regular expressions overlap. An example would be a
   * general regular expression to match arbitrary XML tags which
   * overlaps with the regular expression for a specific tag. Although
   * the intention seems obvious to execute the action associated with
   * the more specific regular expression, the general problem does
   * not allow an automatic decision for one of the regular
   * expressions (resp. actions). If the automaton compilation
   * recognizes such an ambiguity, it calls <code>getPriority()</code>
   * of the two actions and favors the one with the higher value.</p>
   *
   *
   * <p>Note that this function is only used during compilation of the
   * automaton. To change it dynamically while the automaton is
   * running could have some interesting applications but is
   * not implemented.</p>
   *
   * @return the priority of the action used to break ties between
   * actions if several actions could be called 
   */
  //int getPriority();

  /**
   * <p>is called by methods of {@link DfaRun} in case of a
   * match. Parameter <code>yytext</code> contains the matching text
   * from position <code>start</code> onwards. The callback may change
   * the whole of <code>yytext</code>, but the under most
   * circumstances, only the matching text should be
   * changed. Parameter <code>runner</code> is the <code>DfaRun</code>
   * object which called this function. Of interest are its fields
   * {@link DfaRun#clientData} and {@link DfaRun#collect}.</p>
   */
  void invoke(StringBuffer yytext, int start, DfaRun runner)
    throws CallbackException;

  /**
   * <p>is eventually called by {@link monq.jfa.Nfa#compile} to create a
   * unified action from this object and another <code>FaAction</code>
   * object. During compilation of an Nfa into a Dfa, two
   * regular expressions assigned to different actions may have
   * overlapping matches. In this case one of the actions'
   * <code>mergeWith</code> methods is called with the other action as
   * parameter to create a unified action object.</p>
   * <p>There are only four permissible results this method may
   * return:</p>
   * <ol>
   * <li><code>this</code> completely unchanged,</li>
   * <li><code>other</code> completely unchanged,</li>
   * <li>a <b>newly</b> created object or</li>
   * <li><code>null</code> to signal that no unified action exists.</li>
   * </ol>
   *
   * <p>Both objects, <code>this</code> and <code>other</code> must be
   * treated <b>read-only</b>. Changing them may lead to unexpected
   * behaviour which is very hard to track down. To understand why
   * this is so, consider the two regular expressions <code>ax?</code>
   * and <code>ay?</code> bound to actions <code>X</code> and
   * <code>Y</code> respectively. Both expressions match the string
   * <code>"a"</code>, which will cause a call to
   * <code>mergeWith</code> of either one of the actions. If merging
   * would integrate, say, <em>Y</em> into <em>X</em> and return
   * <em>X</em>, the changed <em>X</em> would also be run
   * for the 
   * match <code>"ax"</code> which most likely was not intended.</p>
   *
   * <p>To get a simple priority based selection of one of the actions,
   * consider extending {@link AbstractFaAction}.</p>
   *
   * <p><b>Note:</b> For two competing actions it is completely random
   * which of the <code>mergeWith()</code> methods is
   * called. Consequently, implementations of this method should be as
   * symmetric as possible.</p>
   *
   * @param other will never be <code>null</code>.
   */
  FaAction mergeWith(FaAction other);
}
