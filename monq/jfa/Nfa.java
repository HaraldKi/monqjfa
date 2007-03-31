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

import java.util.Vector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.Stack;
import java.util.HashMap;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.io.PrintStream;

/**
 * <p>models a non-deterministic finite automaton.</p>
 *
 * <p>The most frequent use involves several calls to {@link
 * #or(CharSequence, FaAction)} to bind regular expressions to
 * actions. After having added all necessary <code>re/action</code>
 * pairs, {@link #compile compile()} into a {@link Dfa} and operate it
 * by a {@link DfaRun} to filter text. <em>Filtering</em> means that
 * all regular expressions which were bound to actions compete for
 * matches. The regular expression with the longest match is selected
 * and its associated action will be invoked.
 *
 * @see <a href="doc-files/resyntax.html">Regular expression
 * syntax</a>
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.48 $, $Date: 2006-01-25 15:35:38 $
*/

public class Nfa  {
  private FaState start;

  // is the single unique state used for operations on and with the
  // Nfa like those necessary for Thompson's construction. This state
  // is always treated as if it were a stop state, except in
  // compile(). To make it a into a stop state which survives
  // compilation, addAction() must be called just before compilation.
  private AbstractFaState.EpsState lastState;

  // our private parser for regular expressions
  private ReParser rep = null;

  /**********************************************************************/
  /**
   * <p>is a string filled with those characters that have a special
   * meaning in regular expressions.</p>
   * @see #escape(String)
   */
  public static final char[] specialChars 
    = ReParser.specialChars.toCharArray();
  static { java.util.Arrays.sort(specialChars); }

  /**
   * <p>transforms the input string into a regular expression which
   * matches exactly only the input string. In particular, characters
   * with a special meaning in regular expressions are escaped. For
   * example <code>"1+2"</code> is transformed into
   * <code>"1\+2"</code>.</p>
   * @see #specialChars
   */
  public static String escape(String s) {
    StringBuffer sb = new StringBuffer();
    escape(sb, s, 0);
    return sb.toString();
  }
  /**
   * @see #escape(String)
   */
  public static void escape(StringBuffer out, 
			    CharSequence in, int startAt) {
    int l = in.length();
    for(int i=startAt; i<l; i++) {
      char ch = in.charAt(i);
      int pos = java.util.Arrays.binarySearch(specialChars, ch);
      if( pos>=0 ) out.append('\\');
      out.append(ch);
    }
  }
  /**********************************************************************/
  
  /**
   * <p>is used for an enumeration type which determines which type of
   * empty <code>Nfa</code> is generated when calling
   * {@link monq.jfa.Nfa#Nfa(Nfa.EmptyNfaType)}. The possible values of this class are
   * defined as static fields of <code>Nfa</code>.</p>
   */
  private static final class EmptyNfaType {
    private EmptyNfaType() {}
  }
  /**
   * value of an enumeration type to be passed to {@link
   * monq.jfa.Nfa#Nfa(Nfa.EmptyNfaType)}.
   */
  public static final EmptyNfaType NOTHING = new EmptyNfaType();
  /**
   * value of an enumeration type to be passed to {@link
   * monq.jfa.Nfa#Nfa(Nfa.EmptyNfaType)}.
   */
  public static final EmptyNfaType EPSILON = new EmptyNfaType();

  /**
   * <p>creates an <code>Nfa</code> which does either not match
   * anything or only the empty string. Most of the time you will use
   * {@link #NOTHING} as the parameter and later add regular
   * expressions with {@link #or or()}. If, however, you intend to
   * start adding to the automaton with {@link #seq seq()}, call this
   * constructor with {@link #EPSILON}.</p>
   */
  public Nfa(EmptyNfaType type) {
    start = new AbstractFaState.EpsState();
    lastState = new AbstractFaState.EpsState();
    if( type==EPSILON ) {
      // automaton suitable to be extended by seq, i.e. one
      // recognizing the empty set
      start.addEps(lastState);
    }      
  }

  /**
   * <p>creates an<code>Nfa</code> from the given 
   * <a href="doc-files/resyntax.html">regular expression</a> and
   * assigns the given action to the stop state. More regex/action
   * pairs are then normally added with {@link
   * #or(CharSequence,FaAction)}. </p>
   */
  public Nfa(CharSequence s, FaAction a) throws ReSyntaxException {
    setRegex(s);
    addAction(a);
  }

  /**
   * creates an <code>Nfa</code> from the given 
   * <a href="doc-files/resyntax.html">regular expression</a>.
   */
//   public Nfa(CharSequence s) throws ReSyntaxException {
//     setRegex(s);
//   }
  /**********************************************************************/
  // to be able to test different Set implementations
//   private static Set newSet() { return new LeanSet(); }
//   private static Set newSet(int s) { return new LeanSet(s); }
//   private static Set newSet(Collection c) { return new LeanSet(c); }

  private static Set newSet() { return new PlainSet(); }
  private static Set newSet(int s) { return new PlainSet(s); }
  private static Set newSet(Collection c) { return new PlainSet(c); }

//   private static Set newSet() { return new HashSet(16, 1.0F); }
//   private static Set newSet(int s) { return new HashSet(s, 1.0F); }
//   private static Set newSet(Collection c) { 
//     HashSet h = new HashSet(c.size()+1, 1.0F);
//     h.addAll(c);
//     return h;
//   }
  
  /**********************************************************************/
  /**
   * used in high volume Nfa creation to conserve space. To be able to
   * parse a regular expression, an Nfa creates an internal parser
   * object. To prevent the parser object to be created and thereby
   * save quite some space, use this method of an already available
   * Nfa to create new Nfas instead of creating them with the
   * constructor. 
   * @deprecated will disappear soon.
   */
  public Nfa parse(CharSequence s, FaAction a) throws ReSyntaxException {
    // the parsing will mess with start and lastState, so we have to
    // keep them on the side.
    FaState keepStart = start;
    AbstractFaState.EpsState keepLast = lastState;

    setRegex(s);
    addAction(a);

    Nfa result = new Nfa(start, lastState);
    start = keepStart;
    lastState = keepLast;
    return result;
  }

  /**
   * <p>expert only.</p>
   * @deprecated will disappear soon.
   */
  public Nfa parse(CharSequence s) throws ReSyntaxException {
    return parse(s, null);
  }
  /**********************************************************************/
  FaState getStart() {return start;}
  //private AbstractFaState.EpsState getLastState() {return lastState;}

  /**
   * <p>adds an action to the Nfa (expert only).  This is done by
   * adding an epsilon transition to the last state of this Nfa to a
   * new last state which will carry the action.</p>
   *
   * <p>If the automaton has free subgraphs, these will be bound to
   * this action.</p>
   *
   * <p><b>Note:</b>If the automaton has already one or more actions
   * (i.e. stop states), this operation may easily make the automaton
   * uncompilable due to conflicting actions.
   */
  public Nfa addAction(FaAction a) {
    if( a==null ) return this;

    AbstractFaState.EpsStopState newLast;
    newLast = new AbstractFaState.EpsStopState(a);
    lastState.addEps(newLast);
    lastState = newLast;
    addSubAction(start, a, newSet());
    
    // All unbound subgraphs are now bound to an action. Since a
    // subgraph is identified by a pair (FaAction a, byte id), now is
    // the time to reset the id counter in our ReParser.
    rep.resetSubGraphCounter();
    return this;
  }
  private void addSubAction(FaState s, FaAction a, Set known) {
    s.reassignSub(null, a);
    known.add(s);
    Iterator children = s.getChildIterator();
    while( children.hasNext() ) {
      Object o = children.next();
      if( known.contains(o) ) continue;
      addSubAction((FaState)o, a, known);
    }
    
  }

  public void toDot(PrintStream out) {
    FaToDot.print(out, start, lastState);
  }

  /**
   * is used to clear an automaton to be an empty shell without
   * states. This is used in {@link or(Nfa)} and {@link
   * seq(Nfa)} because these functions don't make a deep copy of their
   * parameter, but rather integrate the given automata into their own
   * structure.
   */
  private void invalidate() {start=null; lastState=null;}


  //public int size() {return all.size();}
  /**********************************************************************/
  /**
   * makes sure this.rep is not null and then calls it to parse the
   * regex.
   */
  private void setRegex(CharSequence s) throws ReSyntaxException {
    if( rep==null ) rep = new ReParser();
    rep.parse(s);
  }
  /**
   * for internal use by {@link Dfa#toNfa()} only.
   */
  Nfa(FaState start, AbstractFaState.EpsState lastState) {
    this.start = start;
    this.lastState = lastState;
  }
  private void initialize(Intervals ivals) {
    lastState = new AbstractFaState.EpsState();
    int L = ivals.size();
    for(int i=0; i<L; i++) {
      if( ivals.getAt(i)==null ) continue;
      ivals.setAt(i, lastState);
    }
    start = new AbstractFaState.NfaState();
    start.setTrans(ivals.toCharTrans());
  }
  private void initialize(StringBuffer s) {
    //System.out.println("+++"+s.toString());
    start = new AbstractFaState.NfaState();

    Intervals assemble = new Intervals();
    FaState current = start;
    FaState other;
    char ch;
    int i, L;
    for(i=0, L=s.length(); i<L-1; i++) {
      other = new AbstractFaState.DfaState();
      ch = s.charAt(i);
      assemble.overwrite(ch, ch, other);
      current.setTrans(assemble.toCharTrans());
      current = other;
    }
    lastState = new AbstractFaState.EpsState();
    ch = s.charAt(i);
    assemble.overwrite(ch, ch, lastState);
    current.setTrans(assemble.toCharTrans());
  }    
  private void initializeAsSequence(FaState start1, FaState last1,
				    FaState start2, 
				    AbstractFaState.EpsState last2) {
    if( false) {
      // FIX ME: 
      // Since we are sure that start2 has no incoming transitions, we
      // can drop that state if we transfer all its transitions to
      // last1. Because last1 does not have any outgoing transitions,
      // the transfer does not require any merging.
      // BUT: This requires changing the type of lastState throughout to
      // a class which accepts a CharTrans, which EpsState currently
      // does not. Therefore the slightly less efficient solution below:
      //last1.addEps(start2.getEps());
      //last1.setTrans(start2.getTrans());
    } else {
      if( !start2.isImportant() ) {
	last1.addEps(start2.getEps());
      } else {
	last1.addEps(start2);
      }
    }
    start = start1;
    lastState = last2;
  }
  private void initializeAsOr(FaState start1, 
			      AbstractFaState.EpsState last1,
			      FaState start2, 
			      AbstractFaState.EpsState last2) {
    // To conserve objects, we want to use one of the last states as
    // the new lastState. However, a state with an action is not a
    // good last state as it would assign that action to the other
    // branch of the or.
    if( null!=last1.getAction() ) {
      // this is not a good new lastState because it would add this
      // action to the automaton ending in last2
      if( null!=last2.getAction() ) {
	// again no good lastState, so we need a fresh one
	lastState = new AbstractFaState.EpsState();
	last1.addEps(lastState);
	last2.addEps(lastState);
      } else {
	// this one can serve as a useful new lastState
	lastState = last2;
	last1.addEps(lastState);
      } 
    } else {
      // last1 is a good new lastState
      lastState = last1;
      last2.addEps(lastState);
    }

    // now wire the start states. If one of the start states is not
    // important, we can skip it by just transfering its eps
    // transitions to the other one.
    if( start2.isImportant() ) {
      FaState tmp = start1; start1 = start2; start2 = tmp;
    }
    if( !start2.isImportant() ) {
      start1.addEps(start2.getEps());
    } else {
      start1.addEps(start2);
    }
    start = start1;
  }
  /********************************************************************/
  /**
   * creates a generally useful empty <code>CharTrans</code>. It is
   * expected, that this function will be modified in the future if
   * different kinds of CharTrans classes become available.
   *
   * @return a generally useful empty <code>CharTrans</code>.
   */
//   public static CharTrans createCharTrans() { 
//     //return new SimpleCharTrans();
//     return new ArrayCharTrans();
//   }
  /**********************************************************************/
  /**
   * <p>marks this automaton as a reporting subautomaton. It will of
   * course only become a <em>sub</em>automaton, if further operators
   * are applied. This is the underlying function called when a
   * regular expression contains an <code>@</code>-marked atom.</p>
   */
  Nfa markAsSub(byte id) {
    Set known = newSet();

    FaSubinfo startInfo = FaSubinfo.start(id);
    FaSubinfo stopInfo = FaSubinfo.stop(id);
    FaSubinfo innerInfo = FaSubinfo.inner(id);
    start.addUnassignedSub(startInfo);
    lastState.addUnassignedSub(stopInfo);

    // we do not add lastState to known. In rare cases, where the Nfa
    // has only two states, a character transition goes into
    // lastState. In this case its subinfo will be amended as "inner".
    //known.add(lastState);

    _markAsSub(start, innerInfo, false, known);
    return this;
  }

  // Every state that can be reached via a character transition is
  // marked as an inner state. States that a merely reachable via an
  // epsilon transition, are not marked. The parent is not touched
  // here.
  private void _markAsSub(FaState parent, 
			  FaSubinfo innerInfo, boolean innerValid,
			  Set known) {
    known.add(parent);

    // iterate of character transitions
    CharTrans trans = parent.getTrans();
    if( trans!=null ) {
      int N = trans.size();
      for(int i=0; i<N; i++) {
	FaState child = (FaState)(trans.getAt(i));
	child.addUnassignedSub(innerInfo);
	if( known.contains(child) ) continue;
	_markAsSub(child, innerInfo, true, known);
      }
    }

    FaState[] children = parent.getEps();
    if( children!=null ) {
      for(int i=0; i<children.length; i++) {
	FaState child = children[i];
	if( known.contains(child) ) continue;
	//if( innerValid ) child.addUnassignedSub(innerInfo);
	_markAsSub(child, innerInfo, innerValid, known);
      }
    }
  }
  /**********************************************************************/
  /**
   * creates virgin start and stop state for this automaton to make
   * sure the start state has no incoming transitions and the stop
   * state has no outgoing ones.
   */
  private void embed() {
    FaState newStart = new AbstractFaState.EpsState();
    AbstractFaState.EpsState newLast = new AbstractFaState.EpsState();
    newStart.addEps(start);
    lastState.addEps(newLast);
    start = newStart;
    lastState = newLast;
  }
  /********************************************************************/
  // -------- IMPORTANT -----------------------------------
  // | The following operators maintain the two assertion:
  // | A) the start state has no incoming transitions
  // | B) the stop state has no outgoing transitions
  // | This is required for Thompson's construction to work properly. 
  // ------------------------------------------------------
  /**
   * <p>transforms this finite automaton to match the empty string
   * too. All actions defining stop states are kept as they are. 
   * This method implements the operator <code>?</code>.<p>
   *
   * @return the return value is <code>this</code>.
   */
  public Nfa optional() {
    start.addEps(lastState);
    return this;
  }
  /**
   * <p>applies the Kleene closure (<code>*</code>) operator to the NFA.</p>
   */
  public Nfa star() {
    lastState.addEps(start);
    embed();
    start.addEps(lastState);
    return this;
  }
  /**
   * <p>applies the <code>+</code> operator to the NFA.</p>
   */
  public Nfa plus() {
    lastState.addEps(start);
    // the following is necessary to maintain a situation such that
    // the stop state has no outgoing transitions, as is necessary for
    // Thompson's construction;
    embed();
    return this;
  }
  
  /**********************************************************************/
  private static class Useful {
    // This can be '?', 'x', 'y' or 'n'. 
    // Initially '?' tells us we don't know yet
    // It will be 'x' while currently trying to recursively figure out
    // Finally, 'y' for "yes" and 'n' for" "no" will be set, where 'n'
    // can only be decided on in the toplevel invocation.
    char useful = '?';
    boolean isPruned = false;
  }
  
  private char isUseful(FaState s, IdentityHashMap known, boolean top) {
    Useful u = (Useful)known.get(s);
    if( u==null ) {
      u = new Useful();
      known.put(s, u);
    } else {
      if( u.useful!='?' ) return u.useful;
    }

    // lastState and stop states are useful
    if( null!=s.getAction() || s==lastState ) {
      u.useful = 'y';
      return u.useful;
    }

    // guard against recursively checking this state again
    u.useful = 'x';

    // if we find one useful child, this state is useful too
    Iterator i = s.getChildIterator();
    while( i.hasNext() ) {
      FaState child = (FaState)i.next();
      char useful = isUseful(child, known, false);
      if( useful!='y' ) continue;
      u.useful = 'y';
      return u.useful;
    }

    // If we arrive here, we could not prove in any branch that this
    // state is useful. If we are at the top of the recursive calls,
    // this means the state is indeed not useful. However, deep down
    // in the recursive invocations, it means nothing
    u.useful = top ? 'n' : '?';
    return u.useful;
  }

  /**
   * deletes states which have no path to neither a stop state nor to
   * lastState. 
   */
  private void deleteUseless(FaState s, IdentityHashMap known) {
    //System.err.println("dU"+s);
    Useful u = (Useful)known.get(s);
    if( u==null ) {
      u = new Useful();
      known.put(s, u);
    }
    u.isPruned = true;

    // we cannot use the child iterator because we may want to delete
    // outgoing transitions.
    int L = 0;
    FaState[] eps = s.getEps();
    if( eps!=null ) L = eps.length;
    int dst = 0;    
    for(int i=0; i<L; i++) {
      FaState child = eps[i];
      Useful cu = (Useful)known.get(child);
      if( cu==null || !cu.isPruned ) deleteUseless(child, known);
      char useful = isUseful(child, known, true);
      if( 'y'==useful ) eps[dst++] = eps[i];
    }
    if( dst<L ) {
      FaState[] newEps = null;
      if( dst>0 ) {
	newEps = new FaState[dst];
	System.arraycopy(eps, 0, newEps, 0, dst);
      }
      s.setEps(newEps);
    }

    CharTrans t = s.getTrans();
    if( t==null ) return;

    // Since CharTrans is immutable, we move things into an Intervals
    // object. Onle if we remove at least one transition will be
    // create a new CharTrans from the Intervals.
    Intervals ivals = new Intervals(t);
    boolean removedOne = false;
    L = ivals.size();
    for(int i=0; i<L; i++) {
      FaState child = (FaState)ivals.getAt(i);
      if( child==null ) continue;
      Useful cu = (Useful)known.get(child);
      if( cu==null || !cu.isPruned ) deleteUseless(child, known);
      char useful = isUseful(child, known, true);
      if( 'y'==useful ) continue;
      ivals.setAt(i, null);
      removedOne = true;
    }
    if( removedOne ) s.setTrans(ivals.toCharTrans());
  }
  /**********************************************************************/
  /**
   * recursively completes all transition tables, removes actions and
   * adds epsilon transitions to a new last state. Only character
   * transitions are traversed because the automaton was compiled just
   * before.
   */
  private void invertState(FaState s, Set known, 
			   FaState sink, FaState last,
			   Intervals worker) {
    known.add(s);
    CharTrans t = s.getTrans();
    int L = t==null ? 0 : t.size();
    for(int i=0; i<L; i++) {
      FaState child = (FaState)(t.getAt(i));
      if( known.contains(child) ) continue;
      invertState(child, known, sink, last, worker);
    }
    if( s.getAction()!=null ) s.clearAction();
    else s.addEps(last);
    s.setTrans(worker.setFrom(t).complete(sink).toCharTrans());
  }
  /**
   * <p>inverts the automaton, such that the resulting automaton will
   * match the set-complement of the set of strings matched by the
   * given automaton.</p>
   *
   * <p><b>Note:</b> Except for rare circumstances you rather want to
   * use {@link #not} instead. The behaviour of this method, while
   * theoretically sound, does not implement the request <em>match
   * everthing but X</em> for some regular expression <em>X</em>.</p>
   *
   * <p>If the automaton has no actions assigned yet, its natural
   * stop state defines the set of strings matched. If it has already
   * actions assigned, those add to the set of recognized strings
   * and no difference is made between them. The resulting automaton
   * will not have any actions.</p>
   *
   * @return <code>this</code>
   * @exception CompileDfaException if the automaton can not be
   * compiled. Compilation is a necessary step to invert the automaton.
   */
  public Nfa invert() throws CompileDfaException {
    // This automaton is going to be compiled. The lastState shall be
    // treated like a stop state afterwards. Consequently we
    // temporarily make it into a stop state, if necessary.
    if( lastState.getAction()==null ) {
      FaAction mark = new monq.jfa.actions.Copy(Integer.MIN_VALUE);
      addAction(mark);
    }
    
    // need to manufacture some CharTrans, so we need this.
    Intervals worker = new Intervals();

    // This automaton needs to be compiled; we lose the lastState by
    // this, but will add a new one later on
    FaState dfaStart = compile_p(true);

    AbstractFaState.EpsState newLast = new AbstractFaState.EpsState();
    FaState sinkState = AbstractFaState.createDfaState(null, true);
    sinkState.setTrans(worker.complete(sinkState).toCharTrans());

    sinkState.addEps(newLast);
    invertState(dfaStart, newSet(), sinkState, newLast, worker);

    start = new AbstractFaState.EpsState();
    start.addEps(dfaStart);
    lastState = newLast;
    
    // all this can have produced useless state. We don't want to keep
    // them.
    deleteUseless(start, new IdentityHashMap());
    return this;
  }
  /**********************************************************************/
  /**
   * <p>implements the request <em>match everthing but X</em>. While
   * {@link #invert} implements the more theoretical set complement
   * on the set of recognized strings of an automaton, this method
   * implements what most people intuitively expect when asking to
   * match anything but the strings recognized by the given
   * automaton.</p>
   *
   * <p>As an example consider the regular expression
   * <code><em>re</em>="[A-Z]+"</code> which matches all strings
   *
   * <blockquote>which are completely uppercase.</blockquote>
   *
   * After application of <code>invert()</code>, the result, namely
   * <code>"([A-Z]+)~"</code>, will match all strings
   * 
   * <blockquote>which are <span style="color:red">not</span> completely
   * uppercase.</blockquote>
   *
   * In particular <code>"xHallo"</code> or <code>"Hallo"</code> will
   * be matched.</p>
   *
   * <p>To the contrary, <code>"([A-Z]+)^</code>,
   * i.e. <code>not()</code> applied to <code><em>re</em></code>, will not
   * match them. In fact <code>"(<em>re</em>)^"</code> is a shortcut for
   * <code>"((.*<em>re</em>.*)?)~"</code>, which matches neither the
   * empty string nor any string containing <code><em>re</em></code>.</p>
   *
   * <p>The following is an example application of
   * <code>not()</code>. It shows a way to enclose words
   * separated by white space with angle brackets:</p><pre>
   * Nfa n = new Nfa("([ \t\n\r]+)^", 
   *                 new AbstractFaAction.Printf("[%0]"))
   * </pre>
   * <p>The caret (<code>"^"</code>) is the operator applying
   * <code>not()</code>. The resulting automaton will call the
   * callback for every longest prefix of the input string which does
   * not contain a white space character. The callback will then
   * enclose the matching text in brackets.</p>
   */
  public Nfa not() throws CompileDfaException {
    Intervals worker = new Intervals();

    // If this is equivalent to re, we do invert(".*re.*)?")

    // keep our current start and lastState
    FaState keepStart = start;
    AbstractFaState.EpsState keepLast = lastState;

    // create the equivalent to '.' in this
    start = new AbstractFaState.NfaState();
    lastState = new AbstractFaState.EpsState();
    start.setTrans(worker.complete(lastState).toCharTrans());

    // apply * and append the orginal this
    star();
    initializeAsSequence(start, lastState, keepStart, keepLast);

    // keep again the Nfa built so far
    keepStart = start;
    keepLast = lastState;

    // manufacture another '.'
    start = new AbstractFaState.NfaState();
    lastState = new AbstractFaState.EpsState();
    start.setTrans(worker.complete(lastState).toCharTrans());

    // apply * again and string everything together
    star();
    initializeAsSequence(keepStart, keepLast, start, lastState);

    // finish up
    optional();
    return invert();
  }
  /**********************************************************************/
  /**
   * used below by <code>shortest</code> to recursively prune outgoing
   * transitions of stop states.
   */
  private void trimStopState(FaState s, Set known, 
			     FaState newLast, FaAction mark) {
    known.add(s);
    FaAction a = s.getAction();

    if( a!=null ) {
      // character transitions are cleared before we go recursive
      // because any children becoming disconnected are not interesting
      // anyway. The 'if' is necessary because a some states don't
      // have a CharTrans
      if( s.getTrans()!=null ) s.setTrans(null);
      if( a==mark ) s.clearAction();
    }

    // The action 'mark' which was added before compilation to mark
    // the last state as a stop state will have been entered into
    // subgraph information. We have to remove it again.
    s.reassignSub(mark, null);

    Iterator i = s.getChildIterator();
    while( i.hasNext() ) {
      FaState child = (FaState)i.next();
      if( known.contains(child) ) continue;
      trimStopState(child, known, newLast, mark);
    }

    if( a!=null ) s.addEps(newLast);
  }

  /**
   * transforms the Nfa into an Nfa which only matches the
   * <em>shortest matches</em> of the given Nfa. This operation is
   * best understood by its implementation, which compiles the Nfa
   * into a Dfa and then removes all outgoing links of any stop
   * states. 
   *
   * <p>If <code>this</code> has not stop state yet, the operation
   * is actually not well defined. Therefore and to avoid
   * counterintuitive results, the last state of the Nfa is treated
   * like a stop state, even if it is none.
   */
  public Nfa shortest() throws CompileDfaException {
    // lastState will be treated like a stop state, which means we
    // might have to mark it.
    FaAction mark = null;
    if( lastState.getAction()==null ) {
      mark = new monq.jfa.actions.Copy(Integer.MIN_VALUE);
      addAction(mark);
      //System.out.println("# setting action on last");
    }

    // We will now compile this before trimming its stop
    // states. However, the start state of the resulting Dfa cannot
    // have epsilon transitions going out. In addition, it might have
    // incoming transitions, therefore we prefix a harmless state in
    // front.
    FaState newStart = new AbstractFaState.EpsState();
    newStart.addEps(compile_p(false));

    // the following may result in totally disconnected states which
    // we leave for the garbage collector (in C this was a real
    // pain). However, we cannot live with dead states, i.e. states
    // from which we cannot reach any stop state
    AbstractFaState.EpsState newLast = new AbstractFaState.EpsState();
    trimStopState(newStart, newSet(), newLast, mark);

    start = newStart;
    lastState = newLast;

    return this;
  }
  
  /**********************************************************************/
  public Nfa seq(CharSequence s, FaAction a) throws ReSyntaxException {
    FaState keepStart = start;
    AbstractFaState.EpsState keepLast = lastState;
    setRegex(s);
    addAction(a);
    initializeAsSequence(keepStart, keepLast, start, lastState);
    return this;
  }
  public Nfa seq(CharSequence s) throws ReSyntaxException {
    return seq(s, null);
  }
  public Nfa seq(Nfa other) {
    initializeAsSequence(start, lastState, other.start, other.lastState);
    other.invalidate();
    return this;
  }
  /**********************************************************************/

  /**
   * <p>adds the given regular expression <code>re</code> to the
   * automaton and associates it with the given action. </p>
   */
  public Nfa or(CharSequence re, FaAction action) 
    throws ReSyntaxException 
  {
    FaState keepStart = start;
    AbstractFaState.EpsState keepLast = lastState;
    setRegex(re);
    addAction(action);
    initializeAsOr(keepStart, keepLast, start, lastState);
    return this;
  }
  /**
   * <p>adds the given regular expression to the automaton (expert
   * only). To make it effective, {@link #addAction} should be called
   * before compilation into a <code>Dfa</code>.</p>
   */
  public Nfa or(CharSequence re) throws ReSyntaxException {
    return or(re, null);
  }
  /**
   * <p>joins the other <code>Nfa</code> into this automaton while
   * keeping all stop states and assigned actions.
   */
  public Nfa or(Nfa other) {
    initializeAsOr(other.start, other.lastState, start, lastState);
    other.invalidate();
    return this;
  }
  /**
   * <p>a convenience function which generates a {@link RegexpSplitter} from
   * <code>split</code>, and <code>what</code>, a {@link
   * PrintfFormatter} from 
   * <code>format</code>, and combines them both into a {@link
   * monq.jfa.actions.Printf} action. This action is then attached to
   * <code>re</code>. For the meaning of parameter
   * <code>what</code> see {@link RegexpSplitter#RegexpSplitter
   * RegexpSplitter()}.<p> 
   *
   * <p>To match a line of text, split it at blank space and then print
   * the first and second elements separated by a '|', use:<pre>
   *  .or("[^\n]*\n", " +", RegexpSplitter.SPLIT, "%1|%2\n")</pre>
   * </p>
   */
//   public Nfa or(CharSequence re, 
// 		CharSequence split, int what,
// 		String format) 
//     throws ReSyntaxException
//   {
//     FaAction a = new monq.jfa.actions.Printf(new RegexpSplitter(split, what),
// 					new PrintfFormatter(format), 0);
//     return or(re, a);
//   }
  /**********************************************************************/
  // is a convenience wrapper around findAction for use in findPath
  private boolean hasAction(Set nfaStates) {
    Vector clashes = new Vector(3);
    HashSet actions = new HashSet(3);
    StringBuffer sb = new StringBuffer();
    return null!=findAction(sb, 'a', 'a', clashes, actions, nfaStates);
  }
  /**********************************************************************/
  /**
   * is a very inefficient way to use an Nfa for matching, mainly
   * intended for crosschecks in unit-testing. 
   */
  int findPath(String s) {
    int lastMatch;
    Set current = newSet(100);
    Set other = newSet(100);

    current.add(start);
    eclosure(current);
    if( hasAction(current) ) lastMatch = 0;
    else lastMatch = -1;

    for(int i=0, L=s.length(); i<L && current.size()>0; i++) {
      char ch = s.charAt(i);      
      for(Iterator j=current.iterator(); j.hasNext(); ) {
	FaState state = (FaState)j.next();
	state = state.follow(ch);
	if( state!=null ) {
	  //System.out.println("followed "+ch);
	  other.add(state);
	}
      }
      Set tmp = current; current = other; other = tmp;
      other.clear();
      eclosure(current);
      if( hasAction(current) ) lastMatch = i+1;
    }
    return lastMatch;
  }
  /**********************************************************************/
  // generate a human readable exception from the clash information
  // collected during compilation.
  private String clashToString(Vector clashes) 
    throws CompileDfaException {

    StringBuffer s = new StringBuffer(200);
    s.append(CompileDfaException.EAMBIGUOUS)
      .append(".\nThe following set(s) of clashes exist:\n");
    for(int i=0; i<clashes.size(); i++) {
      Object[] actions = (Object[])clashes.get(i);
      s.append(i+1)
	.append(") path `")
	.append(actions[actions.length-1])
	.append("':\n");
      for(int k=0; k<actions.length-1; k++) {
	s.append("    ").append(actions[k]).append('\n');
      }
    }
    return s.toString();
  }
  /**********************************************************************/

  /**
    extends the given set of states into its epsilon closure while
    removing unimportant states, i.e. states which are no stop states
    and have no outgoing non-epsilons.
  *****/
  private static void eclosure(Set states) {
    ArrayList stack = new ArrayList(states.size()+20);
    stack.addAll(states);
    Set tmpResult = newSet(states.size()+20);
    states.clear();

    while( stack.size()>0 ) {
      FaState ns = (FaState)stack.remove(stack.size()-1);
      tmpResult.add(ns);
      FaState[] eps = ns.getEps();
      if( eps==null ) continue;
      for(int i=0; i<eps.length; i++) {
	if( tmpResult.contains(eps[i]) ) continue;
	stack.add(eps[i]);
      }
   }

    // Delete unimportant states from tmpResult while copying back into
    // states. 
    for(Iterator i=tmpResult.iterator(); i.hasNext(); /**/) {
      FaState ns = (FaState)i.next();
      if( ns.isImportant() ) states.add(ns);
    }
  }
  /********************************************************************/
  // finds the action to be associated with a set of nfaStates. This
  // function is a mess, mainly because it collects all relevant
  // information about the clash in 'classes' in order to allow a
  // complete and helpful error report later.
  // dfaPath -- character ranges denoting a shortest path through the
  // Dfa to the parent state of the state we want to find an action
  // for 

  // first, last -- contain the last step not yet reflected in
  // dfaPath. It is not yet in there for efficiency reasons because it
  // is only needed in case of error. The order of first, last may be
  // inverted to show that it is not really a step

  // clashes -- will receive an additional FaAction[] if a clash
  // happens 
  // actions -- is preallocated reusable space to keep track of the
  // actions found (reminds me a bit about old FORTRAN:-)
  // nfaStates -- is the set of NFA states within which we look for
  // the highest priority action. A clash is defined by having too or
  // more non-identical highest priority actions.
  private static FaAction findAction(StringBuffer dfaPath, 
				     char first, char last,
				     Vector clashes, Set actions, 
				     Set nfaStates) 
    //throws CompileDfaException 
  {
    FaAction action = null;
    actions.clear();

    for(Iterator i=nfaStates.iterator(); i.hasNext(); /**/) {
      FaState ns = (FaState)i.next();
      FaAction a = ns.getAction();
      if( a==null ) continue;
      for(Iterator ia=actions.iterator(); ia.hasNext(); /**/) {
	FaAction oldAction = (FaAction)ia.next();

	// XXX: try to merge the two actions either way. This is the
	// closest to symmetric operation I can think of. Is there a
	// good way to enforce real symmetry?
	FaAction tmp = a.mergeWith(oldAction);
	if( tmp==null ) tmp = oldAction.mergeWith(a);

	if( tmp!=null ) {
	  a = tmp;
	  ia.remove();
	}
      }
      actions.add(a);
      action = a;
    }

    if( actions.size()==1 ) return action;
    if( actions.size()==0 ) return null;

    // --- REST IS ERROR HANDLING ---
    // arriving here, we have a clash, i.e. more than one highes
    // priority actions. We store all helpful information in an
    // Object[]. The last element will be a string assembled from
    // dfaPath and rm. All other elements are the clashing actions.
    Object[] clash = new Object[actions.size()+1];
    clash = actions.toArray(clash);
    //clash[clash.length-2] = action; // was not yet in actions


    // add the very last step to dfaPath
    if( first<=last ) dfaPath.append(first).append(last);

    // convert dfaPath into something more readable, folding character
    // ranges with first==last into one character
    StringBuffer sb = new StringBuffer();
    int L = dfaPath.length();
    for(int i=0; i<L; i+=2) {
      char from, to;
      from = dfaPath.charAt(i);
      to = dfaPath.charAt(i+1);
      if( from==to ) {
	Misc.printable(sb, from);
      } else {
	sb.append('[');
	Misc.printable(sb, from);
	sb.append('-');
	Misc.printable(sb, to);
	sb.append(']');
      }
    }
    clash[clash.length-1] = sb.toString();
    clashes.add(clash);

    // correct dfaPath again, otherwise clashing siblings will show
    // the wrong path
    if( first<=last ) dfaPath.setLength(dfaPath.length()-2);

    // return an arbitarily choosen action
    return action;
  }
  /********************************************************************/
  void addTransition(Intervals v, char first, char last, FaState dst) {
    //System.out.println("adding: "+Misc.printable(first)+
    //		       ","+Misc.printable(last)+" to "+v);
    int from = v.split(first);
    if( from<0 ) {
      // first fell just on an interval border
      from = -(from+1);
    } else {
      // Need a fresh Set for the new interval pos, which starts with
      // ch
      Set s = (Set)v.getAt(from);
      if( s!=null ) v.setAt(from, newSet(s));
    }

    int to = v.size();
    if( last<Character.MAX_VALUE ) {
      to = v.split((char)(last+1));
      if( to<0 ) {
	to = -(to+1);
      } else {
	Set s = (Set)v.getAt(to);
	if( s!=null ) v.setAt(to, newSet(s));
      }
    }

    // now we are sure that interval borders nicely fit with first and
    // last
    for(int i=from; i<to; i++) {
      Set s = (Set)v.getAt(i);
      if( s==null ) s = newSet();
      s.add(dst);
      v.setAt(i, s);
    }

    //System.out.println("result:"+v);
  }
  /**********************************************************************/
  /**
   * <p>compiles this non-deterministic finite automaton into a
   * deterministic finite automaton. The given automaton is not
   * changed.</p>
   * <p><b>Note:</b> under most circumstances it is not a good idea to
   * compile an <code>Nfa</code> which has no actions added,
   * i.e. which has no stop state, because this automaton matches
   * nothing, not even the empty string. Nevertheless, it is not
   * forbidden to compile such an automaton into a
   * <code>Dfa</code>. It can even be used within a
   * <code>DfaRun</code> but will never produce any output.</p>
   *
   * @param fmb describes the initial behaviour of a {@link DfaRun}
   * which operates the <code>Dfa</code> created here
   * @param eofAction describes the action to run at the end of input
   * when the resulting Dfa is used with a <code>DfaRun</code>.
   * 
   * @throws CompileDfaException if stop states with differing actions
   * are found that can not be merged by their {@link
   * FaAction#mergeWith mergeWith()} methods. To get rid of the
   * exception, either change the regular expressions such that they
   * do not have matches in common, or make sure the actions involved
   * can be merged. One way of merging is to use extend {@link
   * AbstractFaAction} and prioritize the assigned actions.
   **/
  public Dfa compile(DfaRun.FailedMatchBehaviour fmb, FaAction eofAction) 
    throws CompileDfaException 
  {
    FaState tmpStart = compile_p(false);
    return new Dfa(tmpStart, fmb, eofAction);
  }

  /**
   * <p>compiles <code>this</code> into a {@link Dfa} with the given
   * behaviour for non-matching input.</p>
   * @see #compile(DfaRun.FailedMatchBehaviour,FaAction)
   */
  public Dfa compile(DfaRun.FailedMatchBehaviour fmb) 
    throws CompileDfaException 
  {
    return compile(fmb, null);
  }

  /**
   * @deprecated Use {@link #compile(DfaRun.FailedMatchBehaviour)}
   * instead. This method calls it with <code>UNMATCHED_COPY</code>.
   */
  public Dfa compile() throws CompileDfaException {
    return compile(DfaRun.UNMATCHED_COPY, null);
  }

  // Parameter needEps only decides for inner states if they shall be
  // able to store eps moves later. Currently this is only needed for
  // the invert() method. The start state will allow storage of eps
  // moves anyway so that the result of this compilation can be used
  // in automaton operations like or() again.
  FaState compile_p(boolean needEps)
    throws CompileDfaException
  {
    // If we find multiple actions on some stop states, these are
    // registered as clashes here and will finally result in an
    // exception. 
    Vector clashes = new Vector(3);

    // reusable container for findAction()
    Set actions = newSet(3);
  
    // If no stop state shows up during compilation, an exception is
    // thrown. This guards against an automaton which recognizes
    // nothing. The construction guarantees that if a stop state is
    // generated, it is reachable from the start state.
    boolean haveStopState = false;

    // Generate the representative set of nfa states for the start
    // state of the dfa
    Set starters = newSet(100);
    starters.add(start);
    eclosure(starters);

    // in order to generate a meaningful error message, we keep a
    // stack of character ranges stored as character here. The
    // sequence of ranges denotes a shortes path through the Dfa going
    // to the state the children are currently being constructed.
    StringBuffer dfaPath = new StringBuffer();

    // Even the start state can have an action associated and thereby
    // be a stop state. This is important for subsequent applications
    // of Nfa-operations like shortest(). 
    // The start state will always have the possibility of epsilon
    // transtions going out in case the automaton will be subject to
    // later operations like 'or'.
    FaAction startAction = findAction(dfaPath, '1', '0',
				      clashes, actions, starters);
    FaState dfaStart = 
      AbstractFaState.createDfaState(startAction, true);
    dfaStart.mergeSubinfos(starters);
    haveStopState |= startAction!=null;

    // The map 'known' stores unique sets of NFA states as keys and
    // maps them to their assigned DFA state
    HashMap known = new HashMap();
    known.put(starters, dfaStart);


    // The stack keeps track of states the children of which still
    // need to be constructed. On the stack are triples with the
    // following elements:
    // 1) an int[3], (first, last, no) tells us that the state can be
    // reached in no steps from the start state of the Dfa where the
    // last step is by a character range [first,last]
    // 2) a Set of nfa states representing the dfa state
    // 3) a dfa state which does not have its transition table
    // constructed.
    Stack stack = new Stack();
    int[] step0 = {0, 0, 0};	// a dummy
    stack.push(step0);
    stack.push(starters);
    stack.push(dfaStart);

    // this is the transition table we will use over and over again so
    // that it can grow to a typical required size internally. The
    // transition tables used in generated states are then copied from
    // it.
    Intervals currentTrans = new Intervals();
    while( stack.size()>0 ) {
      // The stack has always a dfa-state and its defining set of nfa
      // states 
      FaState currentState = (FaState)stack.pop();
      Set currentNfaSet = (Set)stack.pop();
      int[] howWeCameHere = (int[])stack.pop();
      if( howWeCameHere[2]>0 ) {
	dfaPath.setLength(howWeCameHere[2]*2-2);
	dfaPath.append((char)howWeCameHere[0])
	  .append((char)howWeCameHere[1]);
      }

      // loop over nfa-states which define current and over all of
      // their transitions and add those transitions to
      // currentTrans. Note that during this operation, the objects
      // stored in currentTrans are not destination states but
      // Sets of destination states.
      for(Iterator i=currentNfaSet.iterator(); i.hasNext(); /**/) {
	CharTrans trans = ((FaState)i.next()).getTrans();
	if( trans==null ) continue;
	int L = trans.size();
	for(int j=0; j<L; j++) {
	  char first = trans.getFirstAt(j);
	  char last = trans.getLastAt(j);
	  FaState st = (FaState)trans.getAt(j);
	  addTransition(currentTrans, first, last, st);
	}
      }
      // System.err.println("+++ "+currentTrans);
      // Convert the generated sets of NFA states which are stored in
      // currentTrans to unique ones and replace the transition
      // destination by the respective DFA state. The latter either
      // exists already or will be created right here.
      int L = currentTrans.size();
      //System.out.println(">>> "+currentTrans);
      for(int i=0; i<L; i++) {
	Set stateSet = (Set)currentTrans.getAt(i);
	if( stateSet==null ) continue;
	eclosure(stateSet);
	FaState dst = (FaState)known.get(stateSet);
	//System.out.println(""+i+"-->"+dst);
	if( dst!=null ) {
	  // A set containing exactly the same states as the one just
	  // built for rm is already known. Consequently we don't have
	  // to handle that again, but just use the value stored for
	  // it.
	} else {
	  // The set stored at rm.o is new. We create a DFA state for
	  // it, store the pair in known and push both on the stack
	  char first = currentTrans.getFirstAt(i);
	  char last = currentTrans.getLastAt(i);
	  FaAction a = findAction(dfaPath, first, last,
				  clashes, actions, stateSet);
	  haveStopState |= a!=null;
	  dst = AbstractFaState.createDfaState(a, needEps);
	  dst.mergeSubinfos(stateSet);
	  int[] step = new int[3];
	  step[0] = first; 
	  step[1] = last; 
	  step[2] = howWeCameHere[2]+1;
	  known.put(stateSet, dst);
	  stack.push(step);
	  stack.push(stateSet);
	  stack.push(dst);
	}
	currentTrans.setAt(i, dst);
      }

      // make a (space minimal) copy of currentTrans and stick it into
      // the current state finally.
      CharTrans ct = currentTrans.toCharTrans();
      //System.out.println("resulting transition:"+ct);
      currentState.setTrans(ct);
    }
    if( clashes.size()>0 ) {
      throw new CompileDfaException(clashToString(clashes));
    }

    if( !haveStopState ) {
      // The resulting automaton recognizes nothing, not even the
      // empty string. Should there be now a huge network of states,
      // they are useless. We return a much more concise
      // representation of an automaton which matching nothing:
      return AbstractFaState.createDfaState(null, true);
    }

    if( false ) {
      System.out.println("known.size()="+known.size());
      Set v = known.keySet();
      Iterator it = v.iterator();
      int count = 0;
      while( it.hasNext() ) {
	Set s = (Set)it.next();
	count += s.size();
      }
      System.err.println("all states in known:"+count);
    }
    return dfaStart;
  }
/**********************************************************************/
/**********************************************************************/
/**********************************************************************/
/**********************************************************************/
private class ReParser {
  private static final int TOK_EOF      = Character.MAX_VALUE+1;
  private static final int TOK_OBRACKET = Character.MAX_VALUE+'[';
  private static final int TOK_CBRACKET = Character.MAX_VALUE+']';
  private static final int TOK_OPAREN   = Character.MAX_VALUE+'(';
  private static final int TOK_CPAREN   = Character.MAX_VALUE+')';
  private static final int TOK_QMARK    = Character.MAX_VALUE+'?';
  private static final int TOK_STAR     = Character.MAX_VALUE+'*';
  private static final int TOK_PLUS     = Character.MAX_VALUE+'+';
  private static final int TOK_OR       = Character.MAX_VALUE+'|';
  private static final int TOK_DOT      = Character.MAX_VALUE+'.';
  private static final int TOK_EXCL     = Character.MAX_VALUE+'!';
  private static final int TOK_TILDE    = Character.MAX_VALUE+'~';
  private static final int TOK_HAT      = Character.MAX_VALUE+'^';
  private static final int TOK_MINUS    = Character.MAX_VALUE+'-';
  private static final int TOK_AT    = Character.MAX_VALUE+'@';
  
  public static final String specialChars = "[]()?*+|.!^-\\~@";

  //private Reader in;
  private CharSequence in;
  private int inNext = 0;
  private StringBuffer tmp = new StringBuffer(30);
  private int token;

  // the following two are needed to efficiently collect long strings
  // into a sequential automaton. In an re like "abcd?" however, the
  // '?' applies only to the 'd', so 'd' and '?' must be seen to
  // decide that 'c' is the last character of teh sequential
  // automaton. 
  private int lookaheadToken;
  private boolean lookaheadValid = false;
  
  // recent input, kept for error messages
  private char[] recent = new char[60];
  private int recentNext = 0;			// first free in recent
  private boolean recentWrapped = false;

  // reusable assembly area for parseBracket
  private Intervals assemble = new Intervals();

  // reporting subgraphs get auto-incremented small IDs
  private byte reportingID = 0;

  /********************************************************************/
  void resetSubGraphCounter() { reportingID = 0; }
  /********************************************************************/
  private void parse(CharSequence s) 
    throws  ReSyntaxException 
  {
    in = s;
    inNext = 0;
    recentNext = 0;
    recentWrapped = false;
    nextToken(false);
    parseOr();
    if( token!=TOK_EOF ) throw error(ReSyntaxException.EEXTRACHAR);
  }
  /**********************************************************************/
  private ReSyntaxException error(String msg) 
    throws ReSyntaxException {

    tmp.setLength(0);
    getRecent(tmp);
    int column = tmp.length();

    int ch;
    for(int i=0; -1!=(ch=nextChar()) && i<10; i++) tmp.append((char)ch);

    return new ReSyntaxException(msg, tmp.toString(), 0, column);
  }

  private void getRecent(StringBuffer b) {
    int pos = recentWrapped ? recentNext+1 : 0;
    while( pos!=recentNext ) {
      b.append(recent[pos]);
      pos += 1;
      if( pos==recent.length ) pos=0;
    }
  }
  private int nextChar() {
    //int ch = in.read();
    //if( ch==-1 ) return ch;
    if( inNext==in.length() ) return -1;
    int ch = in.charAt(inNext++);

    //System.out.println("-->"+(char)ch+" "+ch);

    recent[recentNext] = (char)ch;
    recentNext += 1;
    if( recentNext==recent.length ) {
      recentWrapped = true;
      recentNext = 0;
    }
    return ch;
  }
  private void pushBack(int valueForToken) {
    lookaheadValid = true;
    lookaheadToken = token;
    token = valueForToken;
  }
  private void nextToken(boolean withinBracket) 
    throws  ReSyntaxException {

    if( lookaheadValid ) {
      token = lookaheadToken;
      lookaheadValid = false;
      return;
    }

    int ch = nextChar();

    if( ch==-1 ) {
      token = TOK_EOF;
      return;
    }

    // a backslash escapes everything, everywhere, except EOF
    if( ch=='\\' ) {
      ch = nextChar();
      if( ch==-1 ) throw error(ReSyntaxException.EBSATEOF);
      token = ch;      
      return;
    }

    if( withinBracket ) {
      switch( ch ) {
      case '-': token = TOK_MINUS; break;
      case ']': token = TOK_CBRACKET; break;
      case '^': token = TOK_HAT; break;
      default: token = ch;
      }
    } else {
      switch( ch ) {
      case '[': token = TOK_OBRACKET; break;
      case '(': token = TOK_OPAREN; break;
      case ')': token = TOK_CPAREN; break;
      case '?': token = TOK_QMARK; break;
      case '*': token = TOK_STAR; break;
      case '+': token = TOK_PLUS; break;
      case '|': token = TOK_OR; break;
      case '.': token = TOK_DOT; break;
      case '!': token = TOK_EXCL; break;
      case '~': token = TOK_TILDE; break;
      case '^': token = TOK_HAT; break;
      case '@': token = TOK_AT; break;
      default: token = ch;
      }
      //System.out.println("xx>"+(char)ch+" "+ch);
    }
  }
  /********************************************************************/
  private void parseBracket() 
    throws  ReSyntaxException
  {
    // we just saw the opening bracket

    // The object we enter into assemble is completely arbitray,
    // as long as it is not null
    String dada = "dada";

    boolean invert = false;

    // At the start of a character range, the following characters are
    // recognized specially in this order "^]-"
    if( token==TOK_HAT ) { 
      invert = true; 
      nextToken(true); 
    }
    if( token==TOK_CBRACKET ) {
      assemble.overwrite(']', ']', dada);
      nextToken(true);
    }
    if( token==TOK_MINUS ) {
      assemble.overwrite('-', '-', dada); 
      nextToken(true);
    }

    // collect single characters and character ranges
    while( token!=TOK_CBRACKET ) {
      if( token==TOK_EOF) throw error(ReSyntaxException.EEOFUNEX);
      if( token>Character.MAX_VALUE || token<Character.MIN_VALUE) {
	throw error(ReSyntaxException.ECHARUNEX);
      }
      int ch = token;
      nextToken(true);
      if( token!=TOK_MINUS ) {
	assemble.overwrite((char)ch, (char)ch, dada);
	continue;
      }
      nextToken(true);
      if( token>Character.MAX_VALUE ) {
	throw error(ReSyntaxException.EINVALUL);
      }
      if( ch>token ) {
	throw error(ReSyntaxException.EINVRANGE);
      }
      assemble.overwrite((char)ch, (char)token, dada);
      nextToken(true);
    }
    nextToken(false);

    if( invert ) assemble.invert(dada);

    initialize(assemble);
    //return new Nfa(trans);
  }
  /********************************************************************/

  private void parseAtom() 
    throws  ReSyntaxException {

    // a '(' starts a full regular expression. If the very next
    // character is TOK_EXCL, this is a reporting subexpression.
    if( token==TOK_OPAREN ) {
      boolean isReporting = false;
      nextToken(false);
      if( token==TOK_EXCL ) {
	isReporting = true;
	nextToken(false);
      }
      parseOr();
      if( token!=TOK_CPAREN ) throw error(ReSyntaxException.ECLOSINGP);
      nextToken(false);

      if( isReporting ) {
	markAsSub(reportingID++);
	if( reportingID==0 ) {
	  throw error(ReSyntaxException.ETOOMANYREPORTING);
	}
      }
      return;
    }
     
    // a '[' starts a character class
    if( token==TOK_OBRACKET ) {
      nextToken(true);
      parseBracket();
      return;
    }

    // a '.' stands for every character
    if( token==TOK_DOT ) {
      // the object we write into assemble does not matter
      assemble.overwrite(Character.MIN_VALUE, Character.MAX_VALUE, "");
      initialize(assemble);
      nextToken(false);
      return;
    }

    // Normal characters are allowed here too. We collect consecutive
    // sequences of those because a string can be turned into an Fa
    // quite compact.
    if( token<=Character.MAX_VALUE ) {
      tmp.setLength(0);
      tmp.append((char)token);
      nextToken(false);
      for(/**/; token<=Character.MAX_VALUE; nextToken(false) ) {
	tmp.append((char)token);
      }
      int L = tmp.length();
      if( L>1 ) {
	// token might be a postfix operator and as such should not be
	// applied to more than one character.
	// FIX ME: in principle the pushback is only necessary if in
	// fact a postfix operator follows. However, I don't dare to
	// use this info because it is to easily forgotten the tokens
	// for postfix operators change.
	pushBack(tmp.charAt(L-1));
	tmp.setLength(L-1);
      }
      //System.out.println(":::"+tmp.toString());
      initialize(tmp);
      return;
    }

    // everything else is an error
    if( token==TOK_EOF ) throw error(ReSyntaxException.EEOFUNEX);
    throw error(ReSyntaxException.ECHARUNEX);
  }
  /********************************************************************/
  private void parsePostfixedAtom() 
    throws  ReSyntaxException {

    parseAtom();

    boolean havePostfix = true;
    while( havePostfix ) {
      switch( token ) {
      case TOK_QMARK: optional(); nextToken(false); break;
      case TOK_STAR: star(); nextToken(false); break;
      case TOK_PLUS: plus(); nextToken(false); break;
      case TOK_AT: {
	try {
	  throw new Exception("warning: use `(!...)' instead of `(...)@i@'");
	} catch( Exception e) {
	  e.printStackTrace();
	}
	nextToken(false);
	if( token<'0' || token>'9' ) throw error(ReSyntaxException.EATDIGIT);
	int id = 0;
	while( token>='0' && token<='9' ) {
	  id = 10*id+(token-'0');
	  if( id>Byte.MAX_VALUE ) throw error(ReSyntaxException.EATRANGE);
	  nextToken(false);
	}
	if( token!=TOK_AT ) throw error(ReSyntaxException.EATMISSAT);
	nextToken(false);
	markAsSub((byte)id);
	break;
      }
      case TOK_EXCL: {
	try {
	  shortest(); 
	} catch( CompileDfaException e) {
	  ///CLOVER:OFF
	  throw error
	    ("internal error, this should not happen. A call to "+
	     "shortest() results in a compilation of the Nfa constructed "+
	     "so far. Because this Nfa should not yet have any Actions "+
	     "associated, there cannot be any ambiguous.");
	  ///CLOVER:ON
	}
	nextToken(false); 
	break;
      }
      case TOK_TILDE: {
	try {
	  invert(); 
	} catch( CompileDfaException e) {
	  ///CLOVER:OFF
	  throw error
	    ("internal error, this should not happen. A call to "+
	     "invert() results in a compilation of the Nfa constructed "+
	     "so far. Because this Nfa should not yet have any Actions "+
	     "associated, there cannot be any ambiguous.");
	  ///CLOVER:ON
	}
	nextToken(false); 
	break;
      }
      case TOK_HAT: {
	try {
	  not(); 
	} catch( CompileDfaException e) {
	  ///CLOVER:OFF
	  throw error
	    ("internal error, this should not happen. A call to "+
	     "not() results in a compilation of the Nfa constructed "+
	     "so far. Because this Nfa should not yet have any Actions "+
	     "associated, there cannot be any ambiguous.");
	  ///CLOVER:ON
	}
	nextToken(false); 
	break;
      }
      default:
	havePostfix = false;
	break;
      }
    }
  }
  /********************************************************************/
  private void parseSequence() 
    throws  ReSyntaxException {
    parsePostfixedAtom();

    while( token!=TOK_EOF && token!=TOK_OR && token!=TOK_CPAREN ) {
      //System.out.println("token:"+token);
      FaState keepStart = start;
      AbstractFaState.EpsState keepLast = lastState;
      parsePostfixedAtom();
      initializeAsSequence(keepStart, keepLast, start, lastState);
    }
  }
  /********************************************************************/
  private void parseOr() 
    throws ReSyntaxException {
    parseSequence();

    while( token==TOK_OR ) {
      nextToken(false);
      FaState keepStart = start;
      AbstractFaState.EpsState keepLast = lastState;
      parseSequence();
      initializeAsOr(keepStart, keepLast, start, lastState);
    }
  }
  /********************************************************************/

}
}
 
