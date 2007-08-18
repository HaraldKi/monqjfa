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

import java.io.Serializable;
import java.io.PrintStream;
import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * implements a deterministic finite automaton. In contrast to class
 * <code>Nfa</code>, objects of this class do not allow operations
 * which change the automaton structure. But only a <code>Dfa</code>
 * can be operated by a <code>DfaRun</code> to filter text. This makes
 * sure that the automaton structure is not changed while a
 * <code>DfaRun</code> is working with it.
 *
 * <p>A <code>Dfa</code> can only be created by calling one of the
 * {@link Nfa#compile(DfaRun.FailedMatchBehaviour,FaAction)
 * Nfa.compile()} methods. It can only be used for matching by
 * operating it in a {@link DfaRun} object. To apply
 * automata-operations again, it is necessary to copy it into a
 * <code>Nfa</code> again with {@link #toNfa()}.</p>
 *
 * <p><b style="color:red">Warning:</b> The intention of separating
 * the data structure encapsulated in a <code>Dfa</code> from the
 * machinery to run it, i.e. <code>DfaRun</code>, is that
 * <code>Dfa</code> objects shall be shareable between threads. For
 * this to work, however, make sure that each {@link FaAction} used in
 * the <code>Dfa</code> has either no internal state or is
 * synchronized. To pass data between actions, use {@link
 * DfaRun#clientData}.</p>
 *
 * @author &copy; 2003, 2004, 2005 Harald Kirsch
 */
public class Dfa implements Serializable {

  /**********************************************************************/
  /**
   * <p>maximum length a match can have. If the value is greater zero,
   * no match found by this object will be longer. If
   * <code>matchMax</code> is left at its default -1, the longest
   * match found will be no longer than 2<sup
   * style="font-size:80%">64</sup> characters, which is probably long
   * after you ran out of memory.</p>
   */
  public long matchMax = -1;
  /**********************************************************************/
  
  private FaState startState;

  // The action to be used by DfaRun when eof is hit
  final FaAction eofAction;

  // The FailedMatchBehaviour to be used initially by a DfaRun
  final DfaRun.FailedMatchBehaviour fmb;

  /**
   * the main match()-function shall be fast. If it is called in a
   * context where submatches are not asked for, I provide a dummy
   * which can be called but does nothing.
   */
  static SubmatchData dummySmd = new SubmatchData() {
      public void add(FaState s) {}
    };

  /**********************************************************************/
  FaState getStart() {return startState;}

  Dfa(FaState start, DfaRun.FailedMatchBehaviour fmb, FaAction eofA) {
    this.fmb = fmb;
    this.eofAction = eofA;
    this.startState = start;
  }
  /**********************************************************************/
  /**
   * <p>returns <code>true</code> if this automaton can match the empty
   * string. If <code>this</code> can match the empty string, it
   * should better not be used with a {@link DfaRun} because many
   * methods of <code>DfaRun</code> will be likely to enter an
   * infinite loop.</p>
   */
  public boolean matchesEmpty() {
    return null!=startState.getAction();
  }

  /**********************************************************************/
  /**
   * <p>convenience function to get a <code>DfaRun</code> for this
   * automaton.
   */
  public DfaRun createRun() {
    return new DfaRun(this);
  }
  /**********************************************************************/
  /*  *
   * <p>convenience function to get a <code>DfaRun</code> for this
   * automaton.
   *
   * @deprecated The way how non-matching input shall be handled is
   * now defined when the <code>Dfa</code> is created. Overriding it
   * here is a bad idea.
   */
//   public DfaRun createRun(DfaRun.FailedMatchBehaviour fmb) {
//     return new DfaRun(this, fmb);
//   }
  /**********************************************************************/
  /**
   * <p>converts the <code>Dfa</code> back into an <code>Nfa</code> in
   * a way that <code>this</code> is totally useless afterwards. Make
   * sure it is not used somewhere else meanwhile. The approach was
   * taken for efficiency reasons. A deep copy operation is not yet
   * implemented.</p>
   */
  public Nfa toNfa() {
    AbstractFaState.EpsState newLast = new AbstractFaState.EpsState();
    addLastState(startState, newLast, new IdentityHashMap<FaState, Object>());
    Nfa nfa = new Nfa(startState, newLast);
    startState = null;
    return nfa;
  }
  private void addLastState(FaState state, 
			    FaState newLast,
			    IdentityHashMap<FaState,Object> known) {
    // NOTE: We only need the key part of the map, too bad there is no
    // IdentitySet.
    known.put(state, null);

    Iterator i = state.getChildIterator();
    while( i.hasNext() ) {
      FaState child = (FaState)i.next();
      if( known.containsKey(child) ) continue;
      if( child.getAction()!=null ) child.addEps(newLast);
      addLastState(child, newLast, known);
    }
  }
  /**********************************************************************/
  /**
   * prints a graph representation of the Dfa in the
   * <code>graphviz</code> format.
   */
  public void toDot(PrintStream out) {
    FaToDot.print(out, startState, null);
    //new FaToDot(startState).print(out);
  }
  /**********************************************************************/

  /**
   * used internally by DfaRun.next() to get not only the match, but
   * also data about submatches.
   *
   * @return action associated with the match or <code>null</code>, if
   * no match can be found.
   *
   * @throws java.io.IOException only if <code>in.read()</code>
   * throws. 
   */
  FaAction match(CharSource in, StringBuffer out, SubmatchData smd) 
    throws java.io.IOException 
  {
    int startPos = out.length();
    int lastStopPos = startPos;
    FaState lastStopState = null;
    long rest = matchMax;

    FaState current = startState;
    smd.reset();
    while( current!=null && rest!=0 ) {
      smd.add(current);

      if( null!=current.getAction() ) {
	lastStopState = current;
	lastStopPos = out.length();
      }
      rest -= 1;
      int ch = in.read();
      if( ch<0 ) {
	// if we did not move yet, we hit EOF
	if( out.length()==startPos ) return DfaRun.EOF;
	break;
      }
      out.append((char)ch);
      current = current.follow((char)ch);
    }
   
    // we might have read a few characters after the last stop
    // state. Those characters must be pushed back into the input
    // source. 
    //System.out.println("LLL("+out+")"+endPos+" "+out.length());
    in.pushBack(out, lastStopPos);

    // If we never crossed a stop state, null indicates that we found
    // no match.
    if( lastStopState==null ) return null;

    // The smd must be trimmed in the same way as out is drained above
    // because too many characters were read
    smd.size = lastStopPos-startPos+1;

    FaAction a = lastStopState.getAction();
    return a;
  }
  /**********************************************************************/

}
 
