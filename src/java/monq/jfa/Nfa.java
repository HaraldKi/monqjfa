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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import monq.jfa.FaState.IterType;
import monq.jfa.actions.DefaultAction;

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
 * <p>The regular expression syntax depends on the {@link ReParser}
 * implementation in use, which can be inspeced and changed with
 * {@link #getDefaultParserFactory getDefaultParserFactory()} and
 * {@link #setDefaultParserFactory setDefaultParserFactory()}. At the
 * time of this writing, the default parser is a {@link
 * ReClassicParser}.</p>
 *
 * @author &copy; 2003&ndash;2007 Harald Kirsch
*/

public class Nfa  {
  private AbstractFaState start;

  // is the single unique state used for operations on and with the
  // Nfa like those necessary for Thompson's construction. This state
  // is always treated as if it were a stop state, except in
  // compile(). To make it a into a stop state which survives
  // compilation, addAction() must be called just before compilation.
  private AbstractFaState lastState;

  // our private parser for regular expressions
  private ReParser reParser = null;

  // used to get running IDs for marked subgraphs (see
  // markAsSub). Must not exceed 256 before addAction is called.
  private int subgraphID = 0;

  private final ParserView pView = new ParserView();
  private double memoryForSpeedTradeFactor = 1.0;

  /**
   * <p>is used by each <code>Nfa</code> to obtain a default regular
   * expression parser.</p>
   *
   * @see #setReParser
   */
  private static ReParserFactory defaultParserFactory
    = ReClassicParser.factory;
  /**********************************************************************/
  /**
   * <p>sets the default parser factory used by <code>Nfa</code>s to
   * obtain a default <code>ReParser</code>.</p>
   * @see #setReParser
   */
  public static synchronized void
    setDefaultParserFactory(ReParserFactory rpf) {
      defaultParserFactory = rpf;
    }

  /**
   * <p>returns the default parser factory used by <code>Nfa</code>s
   * to obtain a default {@link ReParser}.</p>
   * @see #setReParser
   */
  public static synchronized ReParserFactory
    getDefaultParserFactory() {
      return defaultParserFactory;
    }

  /**
   * <p>escapes special characters in <code>s</code> according to the
   * rules of the current <code>ReParser</code> set for
   * <code>this</code>.</p>
   *
   * @see  ReParser#escape(StringBuilder, CharSequence, int)
   */
  public String escape(String s) {
    StringBuilder sb = new StringBuilder();
    getReParser().escape(sb, s, 0);
    return sb.toString();
  }
  /**
   * <p>is only a pass-through to
   * <code>getReParser().escape(...)</code>.</p>
   *
   * @see ReParser#escape(StringBuilder, CharSequence, int)
   */
  public void escape(StringBuilder out, CharSequence in, int startAt) {
    getReParser().escape(out, in, startAt);
  }
  /**
   * <p>is a shortcut for <code>.getReParser().specialChars()</code>
   */
  public String specialChars() {
    return getReParser().specialChars();
  }
  /**********************************************************************/

  /**
   * <p>is used for an enumeration type which determines which type of
   * empty <code>Nfa</code> is generated when calling {@link
   * monq.jfa.Nfa#Nfa(Nfa.EmptyNfaType)}. The possible values of this
   * class are defined as static fields of <code>Nfa</code>.</p>
   */
  private static enum EmptyNfaType {
    T_NOTHING, T_EPSILON;
  }
  /**
   * value of an enumeration type to be passed to {@link
   * monq.jfa.Nfa#Nfa(Nfa.EmptyNfaType)}.
   */
  public static final EmptyNfaType NOTHING = EmptyNfaType.T_NOTHING;
  /**
   * value of an enumeration type to be passed to {@link
   * monq.jfa.Nfa#Nfa(Nfa.EmptyNfaType)}.
   */
  public static final EmptyNfaType EPSILON = EmptyNfaType.T_EPSILON;

  /**
   * <p>
   * creates an automaton that recognizes nothing but is suitable to be filled
   * with calls to any of the {@link #or(CharSequence, FaAction) or()} methods.
   * </p>
   */
  public Nfa() {
    initialize();
  }
  /**
   * <p>creates an <code>Nfa</code> which does either not match
   * anything or only the empty string. Most of the time you will use
   * {@link #NOTHING} as the parameter and later add regular
   * expressions with {@link #or or()}. If, however, you intend to
   * start adding to the automaton with {@link #seq seq()}, call this
   * constructor with {@link #EPSILON}.</p>
   */
  public Nfa(EmptyNfaType type) {
    this();
    if( type==EPSILON ) {
      // automaton recognizing the empty string
      optional();
    }
  }
  /**
   * <p>creates an <code>Nfa</code> from the given
   * <a href="doc-files/resyntax.html">regular expression</a> and
   * assigns the given action to the stop state. More regex/action
   * pairs are then normally added with {@link
   * #or(CharSequence,FaAction)}. </p>
   */
  public Nfa(CharSequence regex, FaAction a) throws ReSyntaxException {
    getReParser().parse(pView, regex);
    pView.clear();     // pView has two null states pushed still
    addAction(a);
  }
  /**
   * <p>calls {@link #Nfa(CharSequence,FaAction)} with the 2nd
   * parameter set to <code>null</code>.</p>
   */
  public Nfa(CharSequence regex) throws ReSyntaxException {
    this(regex, null);
  }

  /**
   * <p>
   * when creating DFA transitions during NFA to DFA compilation, the
   * transition tables use different implementations, depending on how dense
   * the transition table is. The fastest transition table uses an array that
   * is indexed by the transition character. For sparse tables, this is a
   * significant memory overhead, therefore a denser table can be used which
   * uses a binary search for lookup.
   * </p>
   *
   * <p>
   * The factor given defines how much larger the fast table may be, before
   * the denser table is used. If the factor is 1.0, the implementation with
   * the smaller estimated memory footprint is used. The larger the factor
   * is, the more likely is it that even sparse tables still use the faster
   * implementation. With a factor of several thousand, nearly all transition
   * tables will be fast, possibly using up a lot of heap space.
   * </p>
   *
   * The default is 1.0.
   */
  public void setMemoryForSpeedTradeFactor(float f) {
    memoryForSpeedTradeFactor = f;
  }
  public double getMemoryForSpeedTradeFactor() {
    return memoryForSpeedTradeFactor;
  }
  //-*******************************************************************
  /**
   * <p>initializes this automaton to recogize nothing.<p>
   */
  private void initialize() {
    start = new AbstractFaState();
    lastState = new AbstractFaState();
  }
  //-*******************************************************************
  private void initialize(final CharSequence pairs, boolean invert) {
    start = new AbstractFaState();
    lastState = new AbstractFaState();

    Intervals<AbstractFaState> ivals = new Intervals<>();

    if( pairs!=null ) {
      if( pairs.length()%2!=0 ) {
	throw new IllegalArgumentException
	  ("pairs must be null or have even length");
      }
      for(int i=0; i<pairs.length(); i+=2) {
	char from = pairs.charAt(i);
	char to = pairs.charAt(i+1);
	if( from>to ) { char tmp=from; from=to; to=tmp; }
	ivals.overwrite(from, to, lastState);
      }
    }
    if( invert ) {
      ivals.invert(lastState);
    }
    start.setTrans(ivals.toCharTrans(memoryForSpeedTradeFactor));
  }
  //-*******************************************************************
  // initialize to a sequence of states that recognize exactly the
  // given string. (no regex parsing!)
  private void initialize(CharSequence s) {
    //System.out.println("+++"+s.toString());
    start = new AbstractFaState();

    Intervals<AbstractFaState> assemble = new Intervals<>();
    AbstractFaState current = start;
    AbstractFaState other;
    char ch;
    int i, L;
    for(i=0, L=s.length(); i<L-1; i++) {
      other = new AbstractFaState();
      ch = s.charAt(i);
      assemble.overwrite(ch, ch, other);
      current.setTrans(assemble.toCharTrans(memoryForSpeedTradeFactor));
      current = other;
    }
    lastState = new AbstractFaState();
    ch = s.charAt(i);
    assemble.overwrite(ch, ch, lastState);
    current.setTrans(assemble.toCharTrans(memoryForSpeedTradeFactor));
  }
  private void initializeAsSequence(AbstractFaState start1,
                                    AbstractFaState last1,
				    AbstractFaState start2,
				    AbstractFaState last2) {
    // FIXME:
    // Since we are sure that start2 has no incoming transitions, we
    // can drop that state if we transfer all its transitions to
    // last1. Because last1 does not have any outgoing transitions,
    // the transfer does not require any merging.
    // BUT: This requires changing the type of lastState throughout to
    // a class which accepts a CharTrans, which EpsState currently
    // does not. Therefore the slightly less efficient solution below:
    //last1.addEps(start2.getEps());
      //last1.setTrans(start2.getTrans());
    if( !start2.isImportant() ) {
      last1.addEps(start2.getEps());
    } else {
      last1.addEps(start2);
    }
    start = start1;
    lastState = last2;
  }
  //-*******************************************************************
  private void initializeAsOr(AbstractFaState start1,
			      AbstractFaState last1,
			      AbstractFaState start2,
			      AbstractFaState last2) {
    // To conserve objects, we want to use one of the last states as
    // the new lastState. However, a state with an action is not a
    // good last state as it would assign that action to the other
    // branch of the or.
    if( null!=last1.getAction() ) {
      // this is not a good new lastState because it would add this
      // action to the automaton ending in last2
      if( null!=last2.getAction() ) {
	// again no good lastState, so we need a fresh one
	lastState = new AbstractFaState();
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
      AbstractFaState tmp = start1; start1 = start2; start2 = tmp;
    }
    if( !start2.isImportant() ) {
      start1.addEps(start2.getEps());
    } else {
      start1.addEps(start2);
    }
    start = start1;
  }
  //-*******************************************************************
  // to be able to test different Set implementations
  private static <E> Set<E> newSet() { return new PlainSet<>(); }
  private static <E> Set<E> newSet(int s) { return new PlainSet<>(s); }
  private static <E> Set<E> newSet(Collection<E> c) { return new PlainSet<>(c); }

//   private static <E> Set<E> newSet() { return new HashSet<E>(16, 1.0F); }
//   private static <E> Set<E> newSet(int s) { return new HashSet<E>(s, 1.0F); }
//   private static <E> Set<E> newSet(Collection<E> c) {
//     Set<E> h = new HashSet<E>(c.size()+1, 1.0F);
//     h.addAll(c);
//     return h;
//   }

  //-*******************************************************************/
  /**
   * <p>returns the regular expression parser currently used by this
   * <code>Nfa</code>. If none was explicitely set, {@link
   * #getDefaultParserFactory} is used to set the parser, before
   * it is returned.</p>
   */
  public ReParser getReParser() {
    if( reParser==null ) reParser = defaultParserFactory.newReParser();
    return reParser;
  }
  /**
   * <p>sets the parser to be used by this <code>Nfa</code>.</p>
   * @return <code>this</code>
   */
  public Nfa setReParser(ReParser reParser) {
    this.reParser = reParser;
    return this;
  }
  //-********************************************************************/
  AbstractFaState getStart() {return start;}
  //private AbstractFaState.EpsState getLastState() {return lastState;}

  /**
   * <p>adds an action to the Nfa (expert only).  This is done by
   * adding an epsilon transition of the last state of this Nfa to a
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

    AbstractFaState newLast = new AbstractFaState(a);
    lastState.addEps(newLast);
    lastState = newLast;

    FaStateTraverser<AbstractFaState, FaAction> fasVis = new FaStateTraverser<>(IterType.ALL, a);
    fasVis.traverse(start,
                    new FaStateTraverser.StateVisitor<AbstractFaState, FaAction>() {
      @Override
      public void visit(AbstractFaState state, FaAction action) {
        state.reassignSub(null, action);
      }
    });

    // All unbound subgraphs are now bound to an action. Since a
    // subgraph is identified by a pair (FaAction a, byte id), now is
    // the time to reset the id counter in our ReParser.
    subgraphID = 0;
    return this;
  }

  /*+******************************************************************/
  public void toDot(PrintStream out) {
    FaToDot.print(out, start, lastState);
  }
  /*+******************************************************************/
  public void toDot(String filename) {
    try {
      PrintStream out = new PrintStream(filename, "UTF-8");
      toDot(out);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  /**********************************************************************/
  /**
   * for internal use by {@link Dfa#toNfa()} only.
   */
  Nfa(AbstractFaState start, AbstractFaState lastState) {
    this.start = start;
    this.lastState = lastState;
  }

  /**********************************************************************/
  /**
   * <p>marks this automaton as a reporting subautomaton. It will of
   * course only become a <em>sub</em>automaton, if further operators
   * are applied, or if this automaton is combined with another
   * <code>Nfa</code>.</p>
   * <p>Marking subgraphs for reporting matching substrings is
   * expensive, therefore the number of subgraphs per action is
   * limited to a single byte. This method returns <code>null</code>
   * if 256 subgraph markings are exceeded before an action is added
   * to this <code>Nfa</code>. In this case, the subgraph marking
   * requested was not performed. The <code>Nfa</code> is not touched
   * in any way in this case.</p>
   *
   * @return <code>this</code> if the subgraph marking could be
   * performed or <code>null</code> if the marking could not be
   * performed, because the number of possible subgraphs was exceeded.
   */
  public boolean markAsSub() {
    if( subgraphID>Byte.MAX_VALUE ) return false;

    byte id = (byte)subgraphID++;
    Set<AbstractFaState> known = newSet();

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
    return true;
  }

  // Every state that can be reached via a character transition is
  // marked as an inner state. States that a merely reachable via an
  // epsilon transition, are not marked. The parent is not touched
  // here.
  // TODO: this is still recursive, it will bomb out on moderately large Nfas
  private void _markAsSub(AbstractFaState parent,
			  FaSubinfo innerInfo, boolean innerValid,
			  Set<AbstractFaState> known) {
    known.add(parent);

    // iterate of character transitions
    CharTrans<AbstractFaState> trans = parent.getTrans();
    if( trans!=null ) {
      int N = trans.size();
      for(int i=0; i<N; i++) {
	AbstractFaState child = trans.getAt(i);
	child.addUnassignedSub(innerInfo);
	if( known.contains(child) ) continue;
	_markAsSub(child, innerInfo, true, known);
      }
    }

    AbstractFaState[] children = parent.getEps();
    if( children!=null ) {
      for(int i=0; i<children.length; i++) {
	AbstractFaState child = children[i];
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
    AbstractFaState newStart = new AbstractFaState();
    AbstractFaState newLast = new AbstractFaState();
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
  /*+******************************************************************/
  /**
   * <p>extends the automaton with an additional automaton that matches
   * everything except all non-empty prefixes of this automaton.</p>
   *
   * <p>For example, if this automaton matches "abc", the addional automaton
   * matches every string that does not contain "a", "ab" or "abc". The
   * additional automaton will have the given action attached.</p>
   *
   * <p>The resulting, combined automaton will nearly never hit a no-match
   * situation when used as a filter. The only exception is when the input
   * ends in a true prefix of the original automaton.
   *
   * Informally, the operation performed is as follows:</p>
   *
   * <pre>this.or(this.copy().allPrefixes().not())</pre>
   *
   * @param noMatchAction the action to call when the additional automaton
   *        runs into a match, i.e. when the {@code this} does not match.
   *
   * @throws CompileDfaException if {@code this} automaton does not compile.
   */
  public Nfa completeToSkip(FaAction noMatchAction) throws CompileDfaException {
    Dfa dfa = compile(DfaRun.UNMATCHED_THROW);
    allPrefixes();
    not();

    addAction(noMatchAction);
    or(dfa.toNfa(memoryForSpeedTradeFactor));
    return this;
  }
  /*+******************************************************************/
  /**
   * converts the automaton into one which matches all non-empty prefixes of
   * the given automaton.
   *
   * Any actions already specified will be deleted.
   *
   * @throws CompileDfaException if the internally called compile operation
   *         throws this exception
   */
  public void allPrefixes() throws CompileDfaException {
    if (null==lastState.getAction()) {
      addAction(DefaultAction.nullInstance());
    }
    // need an automaton were all states can carry eps-transitions
    AbstractFaState dfaStart = compile_p(FaStateFactory.forNfa);

    start = new AbstractFaState();
    start.addEps(dfaStart);
    lastState = new AbstractFaState();

    // everything shall become a stop state, but not useless states
    removeUseless();

    FaStateTraverser<AbstractFaState, AbstractFaState> ft =
        new FaStateTraverser<>(IterType.ALL, dfaStart);

    ft.traverse(dfaStart,
                new FaStateTraverser.StateVisitor<AbstractFaState, AbstractFaState>() {
      @Override
      public void visit(AbstractFaState state, AbstractFaState exclude) {
        if (state==exclude) {
          return;
        }
        state.clearAction();
        state.addEps(lastState);
      }
    });
  }
  /*+******************************************************************/
  private void removeUseless() {
    // TODO: refactor, at least by extracting the loops into methods
    Map<AbstractFaState,Boolean> stateUseful = new IdentityHashMap<>();
    ArrayList<AbstractFaState> states = new ArrayList<>();

    states.add(start);
    int nextState = 0;
    while (nextState<states.size()) {
      AbstractFaState state = states.get(nextState++);
      stateUseful.put(state, state.getAction()!=null || state==lastState);
      Iterator<AbstractFaState> children = state.getChildIterator(IterType.ALL);
      while (children.hasNext()) {
        AbstractFaState child = children.next();
        if (stateUseful.containsKey(child)) {
          continue;
        }
        states.add(child);
      }
    }
    // repeatedly iterate over all states and see if one of their children is
    // useful, which makes the state itself useful. Repeat until no new
    // useful state is found.
    boolean foundOne;
    do {
      foundOne = false;
      // reverse iteration tends to hit children earlier, allowing to mark as
      // useful many states in one sweep
      for (int i=states.size()-1; i>=0; i--) {
        AbstractFaState state = states.get(i);
        if (stateUseful.get(state)) {
          continue;
        }
        Iterator<AbstractFaState> children = state.getChildIterator(IterType.ALL);
        while (children.hasNext()) {
          if (stateUseful.get(children.next())) {
            stateUseful.put(state, Boolean.TRUE);
            foundOne = true;
            break;
          }
        }
      }
    } while (foundOne);

    // TODO: refactor this mess
    // remove all useless states
    if (!stateUseful.get(start)) {
      initialize();
      return;
    }

    Intervals<AbstractFaState> worker = new Intervals<>();
    for (int i=0; i<states.size(); i++) {
      AbstractFaState state = states.get(i);
      Boolean b = stateUseful.get(state);
      if (!b) {
        continue;
      }
      removeUselessEps(state, stateUseful);
      removeUselessCharTrans(state, stateUseful, worker);
      worker.reset();
    }
  }
  /*+******************************************************************/
  private void removeUselessCharTrans(AbstractFaState state,
                                      Map<AbstractFaState,Boolean> useful,
                                      Intervals<AbstractFaState> ivalsWorker) {
    CharTrans<AbstractFaState> t = state.getTrans();
    if( t==null ) return;

    // Since CharTrans is immutable, we move things into an Intervals
    // object. Only if we remove at least one transition will be
    // create a new CharTrans from the Intervals.

    boolean removedOne = false;
    ivalsWorker.setFrom(t);
    int L = ivalsWorker.size();
    for(int i=0; i<L; i++) {
      AbstractFaState child = ivalsWorker.getAt(i);
      if (child==null) {
        continue;
      }
      Boolean b = useful.get(child);
      if (!b) {
        ivalsWorker.setAt(i, null);
        removedOne = true;
      }
    }
    if (removedOne) {
      state.setTrans(ivalsWorker.toCharTrans(memoryForSpeedTradeFactor));
    }
  }
  /*+******************************************************************/
  private static void
  removeUselessEps(AbstractFaState state, Map<AbstractFaState,Boolean> useful) {
    AbstractFaState[] eps = state.getEps();
    if (eps==null) {
      return;
    }
    int dst = 0;
    for (int i=0; i<eps.length; i++) {
      if (useful.get(eps[i])) {
        eps[dst++] = eps[i];
      }
    }
    if (dst==0) {
      state.setEps(null);
    } else {
      if (dst<eps.length) {
        eps = Arrays.copyOf(eps, dst);
      }
      state.setEps(eps);
    }
  }
  /*+******************************************************************/
  /**
   * recursively completes all transition tables, removes actions and adds
   * epsilon transitions to a new last state. Only character transitions, not
   * epsilon transitions, are traversed because the automaton was compiled
   * just before.
   */
  private void invertStates(AbstractFaState initialState,
			   final AbstractFaState sink,
			   final AbstractFaState last,
			   final Intervals<AbstractFaState> worker) {
    FaStateTraverser<AbstractFaState, Void> fasTrav =
        new FaStateTraverser<>(IterType.CHAR, null);

    fasTrav.traverse(initialState,
                     new FaStateTraverser.StateVisitor<AbstractFaState, Void>() {
      @Override public void visit(AbstractFaState state, Void xd) {
        if (state.getAction()==null) {
          state.addEps(last);
        } else {
          state.clearAction();
        }
        worker.setFrom(state.getTrans());
        worker.complete(sink);
        state.setTrans(worker.toCharTrans(memoryForSpeedTradeFactor));
      }});
  }
  /*+******************************************************************/
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
      addAction(DefaultAction.nullInstance());
    }

    // need to manufacture some CharTrans, so we need this.
    Intervals<AbstractFaState> worker = new Intervals<>();

    // This automaton needs to be compiled; we lose the lastState by
    // this, but will add a new one later on
    AbstractFaState dfaStart = compile_p(FaStateFactory.forNfa);

    AbstractFaState newLast = new AbstractFaState();
    AbstractFaState sinkState = new AbstractFaState(null);
    worker.complete(sinkState);
    sinkState.setTrans(worker.toCharTrans(memoryForSpeedTradeFactor));

    sinkState.addEps(newLast);

    invertStates(dfaStart, sinkState, newLast, worker);

    start = new AbstractFaState();
    start.addEps(dfaStart);
    lastState = newLast;

    // This may have produced useless states. We don't want to keep them.
    removeUseless();

    return this;
  }
  /*+******************************************************************/
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
   * <p>In particular <code>"xHallo"</code> or <code>"Hallo"</code> will
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
    // If this is equivalent to re, we do invert("(.*re.*)?")

    pView.pushDot();
    pView.star();

    pView.swap();
    pView.seq();

    pView.pushDot();
    pView.star();
    pView.seq();
    pView.optional();
    pView.invert();

    return this;
  }
  /**********************************************************************/
  /**
   * used by <code>shortest</code> to recursively prune outgoing
   * transitions of stop states.
   *
   * TODO: get rid of recursion, use FaStateTraverser instead.
   */
  private void trimStopState(AbstractFaState s, Set<AbstractFaState> known,
			     AbstractFaState newLast, FaAction mark) {
    known.add(s);
    FaAction a = s.getAction();

    if( a!=null ) {
      // character transitions are cleared before we go recursive
      // because any children becoming disconnected are not interesting
      // anyway. The 'if' is necessary because some states don't
      // have a CharTrans
      if( s.getTrans()!=null ) s.setTrans(null);
      if( a==mark ) s.clearAction();
    }

    // The action 'mark' which was added before compilation to mark
    // the last state as a stop state will have been entered into
    // subgraph information. We have to remove it again.
    s.reassignSub(mark, null);

    Iterator<AbstractFaState> i = s.getChildIterator(IterType.ALL);
    while( i.hasNext() ) {
      AbstractFaState child = i.next();
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
    AbstractFaState newStart = new AbstractFaState();
    AbstractFaState compiledStart = compile_p(FaStateFactory.forNfa);
    newStart.addEps(compiledStart);

    // the following may result in totally disconnected states which
    // we leave for the garbage collector (in C this was a real
    // pain). However, we cannot live with dead states, i.e. states
    // from which we cannot reach any stop state
    AbstractFaState newLast = new AbstractFaState();
    trimStopState(newStart, Nfa.<AbstractFaState>newSet(), newLast, mark);

    start = newStart;
    lastState = newLast;

    return this;
  }
  /*+******************************************************************/
  /**
   * creates a copy of this Nfa where all the states are duplicated, but the
   * actions are kept such that both Nfas reference the same actions.
   */
  public Nfa copy() {
    // this.toDot("/home/harald/tmp/bla.dot");
    final Map<AbstractFaState,AbstractFaState> visited = new IdentityHashMap<>();
    final Queue<AbstractFaState> work = new LinkedList<>();
    work.add(start);
    Nfa result = new Nfa(Nfa.NOTHING);
    result.start = new AbstractFaState();
    visited.put(start, result.start);
    Intervals<AbstractFaState> ivals = new Intervals<AbstractFaState>();

    while (!work.isEmpty()) {
      AbstractFaState current = work.remove();
      AbstractFaState newState = visited.get(current);
      if (current==lastState) {
        result.lastState = newState;
      }
      AbstractFaState[] eps = current.getEps();
      if (eps!=null) {
        for (AbstractFaState oldTarget : eps) {
          AbstractFaState newTarget = visited.get(oldTarget);
          if (newTarget==null) {
            newTarget = new AbstractFaState();
            work.add(oldTarget);
            visited.put(oldTarget, newTarget);
          }
          newState.addEps(newTarget);
        }
      }
      CharTrans<AbstractFaState> trans = current.getTrans();
      if (trans!=null) {
        int l = trans.size();
        ivals.reset();
        for (int i=0; i<l; i++) {
          char chFirst = trans.getFirstAt(i);
          char chLast = trans.getLastAt(i);
          AbstractFaState oldTarget = trans.getAt(i);
          if (oldTarget==null) {
            continue;
          }
          AbstractFaState newTarget = visited.get(oldTarget);
           if (newTarget==null) {
             newTarget = new AbstractFaState();
             work.add(oldTarget);
             visited.put(oldTarget, newTarget);
           }
           ivals.overwrite(chFirst, chLast, newTarget);
        }
        newState.setTrans(ivals.toCharTrans(memoryForSpeedTradeFactor));
      }
    }
    result.toDot("/home/harald/tmp/bli.dot");
    return result;
  }
  /*+******************************************************************/
  /**
   * <p>extend the current automaton to recognize <code>regex</code>
   * as a suffix of the strings already recognized and arrange for the
   * given action to be called if the suffix was recognized. If the
   * action is <code>null</code>, no action will be added.</p>
   */
  public Nfa seq(CharSequence regex, FaAction a)
    throws ReSyntaxException
  {
    getReParser().parse(pView, regex);
    addAction(a);
    pView.seq();
    return this;
  }
  /**
   * <p>calls {@link #seq(CharSequence,FaAction)} with
   * <code>null</code> as the 2nd parameter.</p>
   */
  public Nfa seq(CharSequence s) throws ReSyntaxException {
    return seq(s, null);
  }
  /**
   * <p> Concatenates <code>other</code> to <code>this</code> such that a
   * sequence of characters <em>vw</em> is matched where <em>v</em> is
   * matched by <code>this</code> and <em>w</em> is matched by
   * <code>other</code>.  The <code>other</code> <code>Nfa</code> is
   * initialized to the same state as if just constructed with {@link
   * #Nfa()}.</p>
   *
   * <p>FIX ME: unassigned reporting subexpressions in other may
   * create a mess</p>
   */
  public Nfa seq(Nfa other) {
    initializeAsSequence(start, lastState, other.start, other.lastState);
    other.initialize();
    return this;
  }
  /**********************************************************************/

  /**
   * <p>adds the given regular expression <code>re</code> to the
   * automaton and associates it with the given action. </p>
   */
  public Nfa or(CharSequence regex, FaAction action)
    throws ReSyntaxException
  {
    getReParser().parse(pView, regex);
    addAction(action);
    pView.or();
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
   * keeping all stop states and assigned actions. The
   * <code>other</code> <code>Nfa</code> is initialized to the same
   * state as if just created constructed with {@link #Nfa()}.</p>
   *
   * <p>FIX ME: unassigned reporting subexpressions in other may
   * create a mess</p>
   */
  public Nfa or(Nfa other) {
    initializeAsOr(other.start, other.lastState, start, lastState);
    other.initialize();
    return this;
  }
  /**********************************************************************/
  // is a convenience wrapper around findAction for use in findPath
  private static boolean hasAction(Set<AbstractFaState> nfaStates) {
    List<Clash> clashes = new LinkedList<>();
    Set<FaAction> actions = new HashSet<FaAction>(3);
    StringBuilder sb = new StringBuilder();
    return null!=findAction(sb, 'a', 'a', clashes, actions, nfaStates);
  }
  /**********************************************************************/
  /**
   * is a very inefficient way to use an Nfa for matching, mainly
   * intended for crosschecks in unit-testing.
   */
  int findPath(String s) {
    int lastMatch;
    Set<AbstractFaState> current = newSet(100);
    Set<AbstractFaState> other = newSet(100);

    current.add(start);
    eclosure(current);
    if( hasAction(current) ) lastMatch = 0;
    else lastMatch = -1;

    for(int i=0, L=s.length(); i<L && current.size()>0; i++) {
      char ch = s.charAt(i);
      for(Iterator<AbstractFaState> j=current.iterator(); j.hasNext(); ) {
	AbstractFaState state = j.next();
	state = state.follow(ch);
	if( state!=null ) {
	  //System.out.println("followed "+ch);
	  other.add(state);
	}
      }
      Set<AbstractFaState> tmp = current; current = other; other = tmp;
      other.clear();
      eclosure(current);
      if( hasAction(current) ) lastMatch = i+1;
    }
    return lastMatch;
  }
  /**********************************************************************/
  // generate a human readable exception from the clash information
  // collected during compilation.
  private static String clashToString(List<Clash> clashes) {

    StringBuilder s = new StringBuilder(200);
    s.append(CompileDfaException.EAMBIGUOUS)
      .append(".\nThe following set(s) of clashes exist:\n");

    int i = 1;
    for(Clash clash: clashes) {
      s.append(i++)
	.append(") path `")
	.append(clash.message)
	.append("':\n");
      for(FaAction action : clash.actions) {
	s.append("    ").append(action).append('\n');
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
  private static <T extends FaState<T>> void eclosure(Set<T> states) {
    LinkedList<T> stack = new LinkedList<T>();
    Set<T> closure = Nfa.<T>newSet(states.size()+20);

    stack.addAll(states);
    states.clear();
    while( stack.size()>0 ) {
      T ns = stack.removeLast();
      closure.add(ns);
      T[] eps = ns.getEps();
      if( eps==null ) continue;
      for(int i=0; i<eps.length; i++) {
	if( closure.contains(eps[i]) ) continue;
	stack.add(eps[i]);
      }
    }

    // Delete unimportant states from tmpResult while copying back into states.
    for(T s : closure) {
      if (s.isImportant()) {
        states.add(s);
      }
    }
  }
  /*+******************************************************************/
  /**
   * finds the action to be associated with a set of nfaStates. This function
   * is a mess, because it collects error information in clashes to allow a
   * complete and helpful error report later.
   *
   * @param dfaPath character ranges denoting a shortest path through the Dfa
   *        to the parent state of the state we want to find an action for
   *
   * @param first, with last -- contain the last step not yet reflected in
   *        dfaPath. It is not yet in there for efficiency reasons because it
   *        is only needed in case of error. The order of first, last may be
   *        inverted to show that it is not really a step
   *
   * @param clashes will receive an additional Clash if a clash happens
   *
   * @param actions is preallocated reusable space to keep track of the
   *        actions found (reminds me a bit about old FORTRAN:-)
   *
   * @param nfaStates -- is the set of NFA states within which we look for
   *        the highest priority action. A clash is defined by having two or
   *        more non mergable actions.
   */
  private static FaAction findAction(StringBuilder dfaPath, char first,
                                     char last, List<Clash> clashes,
                                     Set<FaAction> actions,
                                     Set<AbstractFaState> nfaStates)
  {
    FaAction actionFound = null;
    actions.clear();

    for(FaState<?> ns : nfaStates) {
      FaAction a = ns.getAction();
      if( a!=null ) {
        actionFound = mergeInto(actions, ns.getAction());
      }
    }

    if( actions.size()==0 ) return null;
    if( actions.size()==1 ) return actionFound;

    // --- REST IS ERROR HANDLING ---
    // arriving here, we have a clash, i.e. more than one highest
    // priority action. We store all helpful information in an Clash
    // object.

    // add the very last step to dfaPath
    if( first<=last ) dfaPath.append(first).append(last);

    // convert dfaPath into something more readable, folding character
    // ranges with first==last into one character
    StringBuilder sb = new StringBuilder();
    int L = dfaPath.length();
    for(int i = 0; i<L; i += 2) {
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
    Clash c = new Clash(sb.toString(), actions);
    clashes.add(c);

    // correct dfaPath again, otherwise clashing siblings will show
    // the wrong path
    if( first<=last ) dfaPath.setLength(dfaPath.length()-2);

    // return an arbitrarily chosen action
    return actionFound;
  }
  /********************************************************************/
  static FaAction mergeInto(Set<FaAction> actions, FaAction other) {
    for(Iterator<FaAction> ia=actions.iterator(); ia.hasNext(); /**/) {
      FaAction oldAction = ia.next();

      // Try to merge the two actions either way. This is the
      // closest to symmetric operation I can think of. Is there a
      // good way to enforce real symmetry?
      FaAction merged = other.mergeWith(oldAction);
      if( merged==null ) {
        merged = oldAction.mergeWith(other);
      }
      if( merged!=null ) {
        other = merged;
        ia.remove();
      }
    }
    actions.add(other);
    return other;
  }
  /********************************************************************/
  void addTransition(Intervals<Set<AbstractFaState>> v,
                     char first, char last, AbstractFaState dst) {
    int from = v.split(first);
    if( from<0 ) {
      // first falls just on an interval border
      from = -(from+1);
    } else {
      // Need a fresh Set for the new interval pos, which starts with ch
      Set<AbstractFaState> states = v.getAt(from);
      if( states!=null ) v.setAt(from, newSet(states));
    }

    int to = v.size();
    if( last<Character.MAX_VALUE ) {
      to = v.split((char)(last+1));
      if( to<0 ) {
	to = -(to+1);
      } else {
	Set<AbstractFaState> states = v.getAt(to);
	if( states!=null ) v.setAt(to, newSet(states));
      }
    }

    // now we are sure that interval borders nicely fit with first and
    // last
    for(int i=from; i<to; i++) {
      Set<AbstractFaState> s = v.getAt(i);
      if( s==null ) s = newSet();
      s.add(dst);
      v.setAt(i, s);
    }
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
    DfaState tmpStart = compile_p(FaStateFactory.forDfa);
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
   * compile into an automaton without epsilon transitions. The result can be
   * different implementations of automata states, either optimized for
   * matching or prepared for more automata operations.
   */
  <STATE extends FaState<STATE>> STATE
  compile_p(FaStateFactory<STATE> stateFac) throws CompileDfaException
  {
    // If we find multiple actions on some stop states, these are
    // registered as clashes here and will finally result in an
    // exception.
    List<Clash>clashes = new LinkedList<Clash>();

    // reusable container for findAction()
    Set<FaAction> actions = newSet(3);

    // If no stop state shows up during compilation, an exception is
    // thrown. This guards against an automaton which recognizes
    // nothing. The construction guarantees that if a stop state is
    // generated, it is reachable from the start state.
    boolean haveStopState = false;

    // Generate the representative set of nfa states for the start
    // state of the dfa
    Set<AbstractFaState> starters = newSet(100);
    starters.add(start);
    eclosure(starters);

    // in order to generate a meaningful error message, we keep a
    // stack of character ranges stored as characters here. The
    // sequence of ranges denotes a shortest path through the Dfa going
    // to the state the children of which are currently being constructed.
    StringBuilder dfaPath = new StringBuilder();

    // Even the start state can have an action associated and thereby
    // be a stop state. This is important for subsequent applications
    // of Nfa-operations like shortest().
    // The start state will always have the possibility of epsilon
    // transitions going out in case the automaton will be subject to
    // later operations like 'or'.
    FaAction startAction = findAction(dfaPath, '1', '0',
				      clashes, actions, starters);

    STATE dfaStart = stateFac.create(startAction);
    dfaStart.mergeSubinfos(starters);
    haveStopState |= startAction!=null;

    // The map 'known' stores unique sets of NFA states as keys and
    // maps them to their assigned DFA state
    Map<Set<AbstractFaState>, STATE> known = new HashMap<>();
    known.put(starters, dfaStart);


    // The stack keeps track of states the children of which still
    // need to be constructed. The CompileTask stores the number of steps
    // that lead to a the stored dfaState, the character interval of the last
    // transition as well as the set of Nfa states that represent the dfa state.

    LinkedList<CompileTask<STATE>> stack = new LinkedList<>();
    stack.add(new CompileTask<STATE>(dfaStart, 0, (char)0, (char)0, starters));

    // this is the transition table we will use over and over again so
    // that it can grow to a typical required size internally. The
    // transition tables used in generated states are then copied from
    // it.
    Intervals<Set<AbstractFaState>> currentTrans = new Intervals<>();
    Intervals<STATE> dfaTrans = new Intervals<>();

    while( stack.size()>0 ) {
      CompileTask<STATE> currentTask = stack.removeLast();
      if( currentTask.steps>0 ) {
	dfaPath.setLength(2*currentTask.steps-2);
	dfaPath.append(currentTask.chLeft);
	dfaPath.append(currentTask.chRight);
      }

      // loop over nfa-states which define current and over all of
      // their transitions and add those transitions to
      // currentTrans. Note that during this operation, the objects
      // stored in currentTrans are not destination states but
      // Sets of destination states.
      currentTrans.reset();
      for(AbstractFaState nfaState : currentTask.nfaStates) {
        CharTrans<AbstractFaState> trans = nfaState.getTrans();
	if( trans==null ) continue;
	int L = trans.size();
	for(int j=0; j<L; j++) {
	  char first = trans.getFirstAt(j);
	  char last = trans.getLastAt(j);
	  AbstractFaState st = trans.getAt(j);
	  addTransition(currentTrans, first, last, st);
	}
      }

      // Convert the generated sets of NFA states which are stored in
      // currentTrans to unique ones and replace the transition
      // destination by the respective DFA state. The latter either
      // exists already or will be created right heree.
      int L = currentTrans.size();
      dfaTrans.reset();
      for(int i=0; i<L; i++) {
        Set<AbstractFaState> stateSet = currentTrans.getAt(i);
	if( stateSet==null ) {
	  continue;
	}

	eclosure(stateSet);
	STATE dst = known.get(stateSet);

	char first = currentTrans.getFirstAt(i);
	char last = currentTrans.getLastAt(i);
	if( dst==null ) {
	  FaAction a = findAction(dfaPath, first, last,
				  clashes, actions, stateSet);
	  haveStopState |= a!=null;
	  dst = stateFac.create(a);
	  dst.mergeSubinfos(stateSet);

	  CompileTask<STATE> t =
	      new CompileTask<>(dst, currentTask.steps+1, first, last, stateSet);
	  stack.add(t);
	  known.put(stateSet, dst);
	}
	dfaTrans.overwrite(first, last, dst);
      }

      // make a (space minimal) copy of dfaTrans and stick it into
      // the current state finally.
      CharTrans<STATE> ct = dfaTrans.toCharTrans(memoryForSpeedTradeFactor);
      CharTrans<?> tmp = ct;
      @SuppressWarnings("unchecked")
      CharTrans<STATE> tmp2 = (CharTrans<STATE>)tmp;
      currentTask.dfaState.setTrans(tmp2);
    }
    if( clashes.size()>0 ) {
      throw new CompileDfaException(clashToString(clashes));
    }

    if( !haveStopState ) {
      // The resulting automaton recognizes nothing, not even the
      // empty string. Should there be now a huge network of states,
      // they are useless. We return a much more concise
      // representation of an automaton which matches nothing.
      return stateFac.create();
    }

    return dfaStart;
  }
  /*+******************************************************************/
  private static class CompileTask<STATE extends FaState<STATE>> {
    final char chLeft;
    final int steps;
    final char chRight;
    final Set<AbstractFaState> nfaStates;
    final STATE dfaState;

    CompileTask(STATE dfaState, int steps, char chLeft, char chRight,
                Set<AbstractFaState> nfaStates) {
      this.chLeft = chLeft;
      this.chRight = chRight;
      this.steps = steps;
      this.nfaStates = nfaStates;
      this.dfaState = dfaState;
    }
  }
  //-*****************************************************************
  private static final class Clash {
    final String message;
    final List<FaAction> actions;
    public Clash(String message, Set<FaAction> actions) {
      this.message = message;
      this.actions = new LinkedList<>(actions);
    }
  }
  //-*****************************************************************
  private class ParserView implements NfaParserView {
    private List<AbstractFaState> startStack = new ArrayList<>();
    private List<AbstractFaState> lastStack = new ArrayList<>();

    private <T> T pop(List<T> stack) {
      return stack.remove(stack.size()-1);
    }
    private <T> T swap(List<T> stack, T newTop) {
      T value = pop(stack);
      stack.add(newTop);
      return value;
    }
    private void clear() {
      startStack.clear();
      lastStack.clear();
    }
    @Override
    public void pushCharSet(CharSequence pairs, boolean invert) {
      startStack.add(start);
      lastStack.add(lastState);
      initialize(pairs, invert);
    }
    @Override
    public void pushDot() {
      pushCharSet("\u0000\uFFFF", false);
    }
    @Override
    public void pushString(CharSequence str) {
      startStack.add(start);
      lastStack.add(lastState);
      initialize(str);
    }
    @Override
    public void swap() {
      start = swap(startStack, start);
      lastState = swap(lastStack, lastState);
    }
    @Override
    public void or() {
      AbstractFaState oldStart = pop(startStack);
      AbstractFaState oldLast = pop(lastStack);
      initializeAsOr(oldStart, oldLast, start, lastState);
    }
    @Override
    public void seq() {
      AbstractFaState oldStart = pop(startStack);
      AbstractFaState oldLast = pop(lastStack);
      initializeAsSequence(oldStart, oldLast, start, lastState);
    }

    @Override
    public void star() { Nfa.this.star(); }
    @Override
    public void plus() { Nfa.this.plus(); }
    @Override
    public void optional() { Nfa.this.optional(); }
    @Override
    public void dup() {
      Nfa copy = Nfa.this.copy();
      startStack.add(copy.start);
      lastStack.add(copy.lastState);
    }
    @Override
    public void allPrefixes() throws CompileDfaException {
      Nfa.this.allPrefixes();
    }

    @Override
    public void not() throws CompileDfaException { Nfa.this.not(); }
    @Override
    public void invert() throws CompileDfaException { Nfa.this.invert(); }
    @Override
    public void shortest() throws CompileDfaException { Nfa.this.shortest(); }
    @Override
    public boolean markAsSub() { return Nfa.this.markAsSub(); }
  }
  //-*****************************************************************
}

