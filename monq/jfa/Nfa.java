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

import java.util.*;
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
  private FaState start;

  // is the single unique state used for operations on and with the
  // Nfa like those necessary for Thompson's construction. This state
  // is always treated as if it were a stop state, except in
  // compile(). To make it a into a stop state which survives
  // compilation, addAction() must be called just before compilation.
  private AbstractFaState.EpsState lastState;

  // our private parser for regular expressions
  private ReParser reParser = null;

  // used to get running IDs for marked subgraphs (see
  // markAsSub). Must not exceed 256 before addAction is called.
  private int subgraphID = 0;

  private ParserView pView = new ParserView();

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
    getDefaultParserFactory(ReParserFactory rpf) {
      return rpf;
    }

  /**
   * <p>escapes special characters in <code>s</code> according to the
   * rules of the current <code>ReParser</code> set for
   * <code>this</code>.</p>
   *
   * @see  ReParser#escape(StringBuffer, CharSequence, int)
   */
  public String escape(String s) {
    StringBuffer sb = new StringBuffer();
    getReParser().escape(sb, s, 0);
    return sb.toString();
  }
  /**
   * <p>is only a pass-through to
   * <code>getReParser().escape(...)</code>.</p> 
   *
   * @see ReParser#escape(StringBuffer, CharSequence, int)
   */
  public void escape(StringBuffer out, CharSequence in, int startAt) {
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
  //-*******************************************************************
  /**
   * <p>initializes this automaton to recogize nothing.<p>
   */
  private void initialize() {
    start = new AbstractFaState.EpsState();
    lastState = new AbstractFaState.EpsState();    
  }
  //-*******************************************************************
  private void initialize(final CharSequence pairs, boolean invert) {
    start = new AbstractFaState.NfaState();
    lastState = new AbstractFaState.EpsState();

    Intervals ivals = new Intervals();
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
    if( invert ) ivals.invert(lastState);
    start.setTrans(ivals.toCharTrans());
  }
  //-*******************************************************************
  // initialize to a sequence of states that recognize exactly the
  // given string. (no regex parsing!)
  private void initialize(CharSequence s) {
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
  //-*******************************************************************
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
  //-*******************************************************************
//   /**
//    * creates an <code>Nfa</code> from the given 
//    * <a href="doc-files/resyntax.html">regular expression</a>.
//    */
//   public Nfa(CharSequence s) throws ReSyntaxException {
//     setRegex(s);
//   }
  /**********************************************************************/
  // to be able to test different Set implementations
//   private static Set newSet() { return new LeanSet(); }
//   private static Set newSet(int s) { return new LeanSet(s); }
//   private static Set newSet(Collection c) { return new LeanSet(c); }

//  private static Set newSet() { return new PlainSet(); }
//  private static Set newSet(int s) { return new PlainSet(s); }
//  private static Set newSet(Collection c) { return new PlainSet(c); }

   private static <E> Set<E> newSet() { return new HashSet<E>(16, 1.0F); }
   private static <E> Set<E> newSet(int s) { return new HashSet<E>(s, 1.0F); }
   private static <E> Set<E> newSet(Collection<E> c) { 
     Set<E> h = new HashSet<E>(c.size()+1, 1.0F);
     h.addAll(c);
     return h;
   }
  
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
//   /**
//    * used in high volume Nfa creation to conserve space. To be able to
//    * parse a regular expression, an Nfa creates an internal parser
//    * object. To prevent the parser object to be created and thereby
//    * save quite some space, use this method of an already available
//    * Nfa to create new Nfas instead of creating them with the
//    * constructor. 
//    * @deprecated will disappear soon.
//    */
//   public Nfa parse(CharSequence s, FaAction a) throws ReSyntaxException {
//     // the parsing will mess with start and lastState, so we have to
//     // keep them on the side.
//     FaState keepStart = start;
//     AbstractFaState.EpsState keepLast = lastState;

//     setRegex(s);
//     addAction(a);

//     Nfa result = new Nfa(start, lastState);
//     start = keepStart;
//     lastState = keepLast;
//     return result;
//   }

//   /**
//    * <p>expert only.</p>
//    * @deprecated will disappear soon.
//    */
//   public Nfa parse(CharSequence s) throws ReSyntaxException {
//     return parse(s, null);
//   }
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
    addSubAction(start, a, Nfa.<FaState>newSet());
    
    // All unbound subgraphs are now bound to an action. Since a
    // subgraph is identified by a pair (FaAction a, byte id), now is
    // the time to reset the id counter in our ReParser.
    subgraphID = 0;
    return this;
  }
  private void addSubAction(FaState s, FaAction a, Set<FaState> known) {
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

  /**********************************************************************/
  /**
   * makes sure this.reParser is not null and then calls it to parse the
   * regex.
   */
//   private void setRegex(CharSequence s) throws ReSyntaxException {
//     ReParser rp = getReParser();
//     Nfa nfa = rp.parse(s);
//     this.start = nfa.start;
//     this.lastState = nfa.lastState;
//   }
  /**
   * for internal use by {@link Dfa#toNfa()} only.
   */
  Nfa(FaState start, AbstractFaState.EpsState lastState) {
    this.start = start;
    this.lastState = lastState;
  }
//   void initialize(Intervals ivals) {
//     lastState = new AbstractFaState.EpsState();
//     int L = ivals.size();
//     for(int i=0; i<L; i++) {
//       if( ivals.getAt(i)==null ) continue;
//       ivals.setAt(i, lastState);
//     }
//     start = new AbstractFaState.NfaState();
//     start.setTrans(ivals.toCharTrans());
//   }
//   void initialize(StringBuffer s) {
//     //System.out.println("+++"+s.toString());
//     start = new AbstractFaState.NfaState();

//     Intervals assemble = new Intervals();
//     FaState current = start;
//     FaState other;
//     char ch;
//     int i, L;
//     for(i=0, L=s.length(); i<L-1; i++) {
//       other = new AbstractFaState.DfaState();
//       ch = s.charAt(i);
//       assemble.overwrite(ch, ch, other);
//       current.setTrans(assemble.toCharTrans());
//       current = other;
//     }
//     lastState = new AbstractFaState.EpsState();
//     ch = s.charAt(i);
//     assemble.overwrite(ch, ch, lastState);
//     current.setTrans(assemble.toCharTrans());
//   }    
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
    Set<FaState> known = newSet();

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
  private void _markAsSub(FaState parent, 
			  FaSubinfo innerInfo, boolean innerValid,
			  Set<FaState> known) {
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
  
  private char isUseful(FaState s, IdentityHashMap<FaState,Useful> known,
                        boolean top) {
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
  private void deleteUseless(FaState s, IdentityHashMap<FaState,Useful> known) {
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
  private void invertState(FaState s, Set<FaState> known, 
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
    invertState(dfaStart, Nfa.<FaState>newSet(), sinkState, newLast, worker);

    start = new AbstractFaState.EpsState();
    start.addEps(dfaStart);
    lastState = newLast;
    
    // all this can have produced useless state. We don't want to keep
    // them.
    deleteUseless(start, new IdentityHashMap<FaState,Useful>());
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
   * used below by <code>shortest</code> to recursively prune outgoing
   * transitions of stop states.
   */
  private void trimStopState(FaState s, Set<FaState> known, 
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
    trimStopState(newStart, Nfa.<FaState>newSet(), newLast, mark);

    start = newStart;
    lastState = newLast;

    return this;
  }
  
  /**********************************************************************/
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
   * <code>other</code> <code>Nfa<code> is initialized to the same
   * state as if just created constructed with {@link #Nfa()}.<p>
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
  private boolean hasAction(Set nfaStates) {
    Vector<Object[]> clashes = new Vector<Object[]>(3);
    Set<FaAction> actions = new HashSet<FaAction>(3);
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
    Set<FaState> current = Nfa.<FaState>newSet(100);
    Set<FaState> other = Nfa.<FaState>newSet(100);

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
      Set<FaState> tmp = current; current = other; other = tmp;
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
  private static void eclosure(Set<FaState> states) {
    List<FaState> stack = new ArrayList<FaState>(states.size()+20);
    stack.addAll(states);
    Set<FaState> tmpResult = Nfa.<FaState>newSet(states.size()+20);
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
				     Vector<Object[]> clashes,
				     Set<FaAction> actions, 
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
      @SuppressWarnings("unchecked") 
      Set<FaState> s = (Set<FaState>)v.getAt(from);
      if( s!=null ) v.setAt(from, newSet(s));
    }

    int to = v.size();
    if( last<Character.MAX_VALUE ) {
      to = v.split((char)(last+1));
      if( to<0 ) {
	to = -(to+1);
      } else {
        @SuppressWarnings("unchecked") 
	Set<FaState> s = (Set<FaState>)v.getAt(to);
	if( s!=null ) v.setAt(to, newSet(s));
      }
    }

    // now we are sure that interval borders nicely fit with first and
    // last
    for(int i=from; i<to; i++) {
      @SuppressWarnings("unchecked") 
      Set<FaState> s = (Set<FaState>)v.getAt(i);
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

//   /**
//    * @deprecated Use {@link #compile(DfaRun.FailedMatchBehaviour)}
//    * instead. This method calls it with <code>UNMATCHED_COPY</code>.
//    */
//   public Dfa compile() throws CompileDfaException {
//     return compile(DfaRun.UNMATCHED_COPY, null);
//   }

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
    Vector<Object[]>clashes = new Vector<Object[]>(3);

    // reusable container for findAction()
    Set<FaAction> actions = newSet(3);
  
    // If no stop state shows up during compilation, an exception is
    // thrown. This guards against an automaton which recognizes
    // nothing. The construction guarantees that if a stop state is
    // generated, it is reachable from the start state.
    boolean haveStopState = false;

    // Generate the representative set of nfa states for the start
    // state of the dfa
    Set<FaState> starters = newSet(100);
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
    Map<Set<FaState>,FaState> known = new HashMap<Set<FaState>,FaState>();
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
    Stack<Object> stack = new Stack<Object>();
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
        @SuppressWarnings("unchecked")
        Set<FaState> stateSet = (Set<FaState>)currentTrans.getAt(i);
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
  //-*****************************************************************
  private class ParserView implements NfaParserView {
    private List<FaState> startStack = new ArrayList<FaState>();
    private List<AbstractFaState.EpsState> lastStack 
      = new ArrayList<AbstractFaState.EpsState>();

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

    public void pushCharSet(CharSequence pairs, boolean invert) {
      startStack.add(start);
      lastStack.add(lastState);
      initialize(pairs, invert);
    }
    public void pushDot() {
      pushCharSet("\u0000\uFFFF", false);
    }
    public void pushString(CharSequence str) {
      startStack.add(start);
      lastStack.add(lastState);
      initialize(str);      
    }
    public void swap() {
      start = swap(startStack, start);
      lastState = swap(lastStack, lastState);
    }
    public void or() {
      FaState oldStart = pop(startStack);
      AbstractFaState.EpsState oldLast = pop(lastStack);
      initializeAsOr(oldStart, oldLast, start, lastState);
    }
    public void seq() {
      FaState oldStart = pop(startStack);
      AbstractFaState.EpsState oldLast = pop(lastStack);
      initializeAsSequence(oldStart, oldLast, start, lastState);
    }
    
    public void star() { Nfa.this.star(); }
    public void plus() { Nfa.this.plus(); }
    public void optional() { Nfa.this.optional(); }
    public void not() throws CompileDfaException { Nfa.this.not(); }
    public void invert() throws CompileDfaException { Nfa.this.invert(); }
    public void shortest() throws CompileDfaException { Nfa.this.shortest(); }
    public boolean markAsSub() { return Nfa.this.markAsSub(); }
  }
  //-*****************************************************************
}
 
