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
import java.io.UnsupportedEncodingException;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;

import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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

  private DfaState startState;

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
      @Override
      public void add(DfaState s) {}
    };

  /**********************************************************************/
  DfaState getStart() {return startState;}

  Dfa(DfaState start, DfaRun.FailedMatchBehaviour fmb, FaAction eofA) {
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
  public Nfa toNfa() {
    return toNfa(1.0);
  }
  /**********************************************************************/
  /**
   * create an {@link Nfa} from this {@code Dfa} which has the same
   * transitions but no actions.
   *
   * @return an {@link Nfa} containing all <code>Dfa</code> nodes plus a
   *         common stop state required by the <code>Nfa</code> data
   *         structure.
   */
  public Nfa toNfa(double memoryForSpeedTradeFactor) {
    AbstractFaState newLast = new AbstractFaState();
    Map<DfaState, AbstractFaState> dfaToNfa = new IdentityHashMap<>();
    LinkedList<DfaState> work = new LinkedList<>();
    Intervals<AbstractFaState> ivals = new Intervals<>();
    work.add(startState);
    dfaToNfa.put(startState, new AbstractFaState());

    while (!work.isEmpty()) {
      DfaState current = work.removeLast();
      ivals.reset();
      CharTrans<DfaState> tr = current.getTrans();
      for (int i=0; i<tr.size(); i++) {
        char first = tr.getFirstAt(i);
        char last = tr.getLastAt(i);
        DfaState child = tr.getAt(i);
        AbstractFaState nfaChild = dfaToNfa.get(child);
        if (nfaChild==null) {
          nfaChild = new AbstractFaState(child.getAction());
          dfaToNfa.put(child, nfaChild);
          work.add(child);
        }
        ivals.overwrite(first, last, nfaChild);
      }
      AbstractFaState nfaState = dfaToNfa.get(current);
      nfaState.setTrans(ivals.toCharTrans(memoryForSpeedTradeFactor));
      if (current.getAction()!=null) {
        nfaState.addEps(newLast);
      }
    }
    // assert that the start state has no incoming transitions
    AbstractFaState newStart = new AbstractFaState();
    newStart.addEps(dfaToNfa.get(startState));
    return new Nfa(newStart, newLast);
  }
  /**********************************************************************/
  /**
   * prints a graph representation of the Dfa in the
   * <code>graphviz</code> format.
   */
  public void toDot(PrintStream out) {
    FaToDot.print(out, startState, null);
  }

  public void toDot(String filename) {
    try (PrintStream out = new PrintStream(filename, "UTF-8")) {
      toDot(out);
    } catch( FileNotFoundException|UnsupportedEncodingException e ) {
      e.printStackTrace();
      return;
    }
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
  FaAction match(CharSource in, StringBuilder out, SubmatchData smd)
    throws java.io.IOException
  {
    int startPos = out.length();
    int lastStopPos = startPos;
    DfaState lastStopState = null;
    long rest = matchMax;

    DfaState current = startState;
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
        // TODO: we are not able to return an action for a match of the empty
        // string at the end of input, a corner case, but a problem for
        // Regexp.matches()
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
  /**
   * <p>determine a matching prefix of <code>in</code> and deliver
   * respective data. If <code>in</code> can be matched by the
   * <code>Dfa</code> the matching text is appended to
   * <code>out</code> and the associated action is returned. If
   * <code>subMatches</code> is not <code>null</code>, the submatches,
   * if any, are analyzed and provided in <code>subMatches</code>
   * after first clearing it.</p>
   *
   * @param subMatches may be null, if no submatch analysis is
   * requested.
   *
   * @return either the action associated with the match found or
   * <code>null</code> if no match was found.
   */
  public FaAction match(CharSource in, StringBuilder out,
			TextStore subMatches) throws IOException {
    if( subMatches==null ) {
      return match(in, out, dummySmd);
    }

    SubmatchData smd = new SubmatchData();
    int start = out.length();
    FaAction a = match(in, out, smd);
    subMatches.clear();
    subMatches.appendPart(out, start, out.length());
    smd.analyze(subMatches, a);

    return a;
  }
  /**********************************************************************/}


