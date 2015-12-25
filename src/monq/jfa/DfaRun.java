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

/**
 * <p>A <code>DfaRun</code> is used to apply a {@link Dfa} to a
 * stream of characters. After creation of a <code>DfaRun</code>
 * object, invoke one of its {@link #read()} or {@link #filter()}
 * methods to filter the input data according to the patterns encoded
 * in the <code>Dfa</code> and the {@link FaAction} callback objects
 * attached to them.</p>
 *
 * <p>The default behaviour of the machine on non-matching input is
 * initialized from whatever was specified when the <code>Dfa</code>
 * {@link Nfa#compile was compiled}. Initialization happens in the
 * constructor as well as every time one of the {@link #setIn setIn()}
 * methods is called. The method {@link #setOnFailedMatch
 * setOnFailedMatch()} should normally only be used in {@link
 * FaAction} callbacks.</p>
 *
 * <p>Use field {@link #clientData} to store data to communicate
 * between different action callbacks. Don't let your action callbacks
 * communicate via a common object allocated alongside the
 * <code>Dfa</code>, because this does not allow to share
 * <code>Dfa</code>s between threads.</p>
 *
 * <p>Set field {@link #collect} to <code>true</code> in an action
 * callback to prevent the <code>read()</code> methods from
 * returning. Thereby data already filtered is kept from shipping and
 * can be changed by further action callbacks. Eventually, however, an
 * action callback should set <code>collect</code> to
 * <code>false</code> again to allow the <code>read()</code> method to
 * finally ship the filtered data.</p>
 *
 * <p>A <code>Dfa</code> that matches the empty string should not be
 * used in a <code>DfaRun</code>, because this is usually a bug in the
 * regular expressions used. As soon as only the empty string
 * matches, methods like {@link #filter filter()} enter an infinitie
 * loop because they keep matching without reading input. Use {@link
 * Dfa#matchesEmpty} if unsure whether your <code>Dfa</code> is safe
 * to use.
 *
 * <p>It is safe to change the <code>Dfa</code> with {@link #setDfa
 * setDfa()} at any time within an action callback. This is
 * particularly useful to parse different parts of input with
 * different automata.</p>
 *
 * <p><b>Note:</b> This class is not synchronized. Objects of this
 * class should only be used within one thread at a time. However, the
 * {@link Dfa} operated may be shared between threads, given that the
 * {@link FaAction} callbacks in the <code>Dfa</code> contain no
 * internal state. For the callbacks to communicate, use {@link
 * #clientData}.</p>
 *
 * <p><b>Hint</b> For maximum speed try to complete your set of
 * regular expressions such that every piece of input is
 * matched. Don't rely on <code>DfaRun</code>'s feature to handle
 * unmatched input. Handling unmatched input is less efficient than
 * handling matches.</p>
 *
 * @author (C) 2003 Harald Kirsch
 * @version $Revision: 1.48 $, $Date: 2006-09-03 17:28:50 $
 */
public class DfaRun extends EmptyCharSource implements Serializable {

  /**
   * defines typed enumerated values which describe
   * what a <code>DfaRun</code> shall do in its read() and filter()
   * functions, if 
   * no match can be found.
   */
  public static final class FailedMatchBehaviour implements Serializable {
    static FailedMatchBehaviour[] all = new FailedMatchBehaviour[3];
    static {
      all[0] = new FailedMatchBehaviour(0);
      all[1] = new FailedMatchBehaviour(1);
      all[2] = new FailedMatchBehaviour(2);
    }
    int i;
    FailedMatchBehaviour(int i) {this.i = i;}
    private Object readResolve() throws java.io.ObjectStreamException {
      return all[i];
    }
  }

  /**
   * <p>requests the <code>DfaRun</code> object to copy input not
   * matched by the DFA to the output.</p>
   *
   * @see #setOnFailedMatch
   */
  public static final FailedMatchBehaviour UNMATCHED_COPY 
    = new FailedMatchBehaviour(0);
  /**
   * requests the <code>DfaRun</code> object to drop (delete) input
   * not matched by the DFA.
   *
   * @see #setOnFailedMatch
   */
  public static final FailedMatchBehaviour UNMATCHED_DROP
    = new FailedMatchBehaviour(1);

  /**
   * requests the <code>DfaRun</code> to throw an exception if it
   * encounters input not matched by the DFA.
   *
   * @see #setOnFailedMatch
   */
  public static final FailedMatchBehaviour UNMATCHED_THROW =
    new FailedMatchBehaviour(1);

  /**
   * returned by {@link #next next()} on EOF.
   */
  public static final FaAction EOF = new monq.jfa.actions.Drop(0);

  /**
   * is the error text used in a <code>IllegalArgumentException</code>
   * if a <code>DfaRun</code> shall be created with a
   * <code>Dfa</code> that matches the empty string.
   */
  public static final String EEPSMATCHER = "dfa matches the empty string";

  /**
   * <p>set this field to <code>true</code> from an {@link FaAction}
   * callback to prevent the machinery to ship the filtered data. It
   * allows action callbacks {@link FaAction#invoke invoked} later to
   * be sure that their first argument still contains previously
   * filtered data. Make sure this field is set to <code>false</code>
   * by some other action callback as soon as possible, because
   * otherwise filtered data will pile up unneccessarily in memory.</p>
   */
  public boolean collect = false;

  /**
   * <p>defines the maximum number of unmatched characters handled in
   * one chunk when the machinery is operating in {@link
   * #UNMATCHED_COPY} mode. When operating on a stretch of text that
   * contains no match at all, the machine runs in a tight inner loop
   * to find the next match as fast as possible. While doing so, no
   * output is delivered by the <code>filter()</code> and
   * <code>read()</code> methods because they call {@link #next
   * next()}, the method that runs the tight inner loop.</p>
   *
   * <p>To prevent against memory overflow for really long stretches
   * of non-matching text, <code>maxCopy</code> puts an upper
   * limit on the characters collected before <code>next()</code>
   * forcibly returns, even if no match is yet found. Except in very
   * special cases there should be no need to ever change this value
   * from its default of 8192. Any value&nbsp;&le;&nbsp;1 will result
   * in single character delivery by <code>next()</code>. For the
   * <code>filter()</code> methods this seems to
   * have a performance impact compared to large enough values of
   * 30%.</p>
   */
  public int maxCopy = 8192;

  // set true whenever the input is set. Flipped to false in crunch()
  // as soon as the eofAction is performed.
  private boolean eofArmed = false;

  /**
   * is the error text used in a <code>java.io.IOException</code> if
   * EOF is hit while {@link #collect} is <code>true</code>.
   */
  private static final String ECOLLECT = "EOF hit in collect mode";

  /**
   * <p>Room for an arbitrary piece of data. If the callbacks of the
   * <code>Dfa</code> want to communicate with each other &mdash; even
   * if only to count instances in the input stream &mdash; this field
   * should be used to store the data so that the <code>Dfa</code>
   * itself is kept thread safe. Storing e.g. counts in the callback
   * object itself would make the <code>Dfa</code> no longer thread
   * safe.</p>
   */
  public Object clientData = null;

  private Dfa dfa;
  private CharSource in;
  private FailedMatchBehaviour onFailedMatch;
  private int matchStart;

  // A string buffer for convenient use of some methods which don't
  // require the caller to supply one.
  private StringBuffer readBuf = new StringBuffer(1024);
  private TextStore readTs = new TextStore();

  // reusable field for calling Dfa.match() and the action returned by
  // Dfa.match(). Both are needed to assemble submatch information
  // should a callback call submatches().
  private SubmatchData smd = new SubmatchData();
  private FaAction action;
  /**********************************************************************/
  /**
   * <p>creates a <code>DfaRun</code> object to operate the given {@link
   * Dfa}. The behaviour on unmatched input and on EOF is initialized
   * from the <code>Dfa</code>.</p>
   *
   * <p>Because in nearly all cases it is a mistake to run a {@link
   * Dfa} that matches the empty string, such a <code>Dfa</code> is
   * not allowed and throws an
   * <code>IllegalArgumentException</code>. In the rare case that
   * a <code>Dfa</code> matching the empty string must be run, you
   * have to first create a <code>DfaRun</code> with a proper
   * <code>Dfa</code> and then replace it with {@link #setDfa}. It is
   * a hassle, but this is intended.</p>
   *
   * @see #setOnFailedMatch
   * @param dfa is the automaton to operate initially. Callbacks may
   * change it.
   * @param in is the initial input source. 
   *
   * @throws IllegalArgumentException if the given <code>dfa</code>
   * matches the empty string, i.e. if {@link Dfa#matchesEmpty} returns
   * <code>true</code>. 
   */
  public DfaRun(Dfa dfa, CharSource in) {
    if( dfa.matchesEmpty() ) {
      throw new java.lang.IllegalArgumentException(EEPSMATCHER);
    }
    setDfa(dfa);
    setIn(in);
  }

  /**
   * <p>creates a <code>DfaRun</code> with empty initial input. This
   * method calls the 2 parameter constructur with an empty
   * <code>CharSource</code>.</p> 
   *
   * @see #DfaRun(Dfa,CharSource)
   */
  public DfaRun(Dfa dfa) {
    this(dfa, new CharSequenceCharSource(""));
  }

  /**********************************************************************/
  /** 
   * <p>changes the input source. Within a thread, this is permissable at
   * all times because a <code>DfaRun</code> object does not buffer
   * input data between calls to any of its methods.<p>
   *
   * <p>Apart from (re)initializing the input source, this method
   * initializes two other parameters:</p>
   * <ol>
   *
   * <li>It resets the way to handle non-matching input according to
   * the <code>Dfa</code> operated (see {@link #setOnFailedMatch
   * setOnFailedMatch()}).</li>
   *
   * <li>The action to take when encountering EOF is armed again so
   * that it is run exactly once when EOF is encountered on the newly
   * set input source.</li>
   * </ol>
   */
  public void setIn(CharSource in) { 
    this.in = in; 
    eofArmed = true;
    this.onFailedMatch = dfa.fmb;
  }

  /**
   * <p>returns the currently active input source.</p.
   */
  public CharSource getIn() { return in; }

//   /** changes the input source. 
//    * @deprecated Create a suitable <code>CharSource</code> object and
//    * call {@link #setIn(CharSource)}.
//    */
//   public void setIn(CharSequence in) {
//     setIn(new CharSequenceCharSource(in));
//   }
//   /** changes the input source. 
//    * @deprecated Create a suitable <code>CharSource</code> object and
//    * call {@link #setIn(CharSource)}.
//    * @see #setIn(CharSource)
//    */
//   public void setIn(CharSequence in, int startAt) {
//     setIn(new CharSequenceCharSource(in, startAt));
//   }
//   /** changes the input source.
//    * @deprecated Create a suitable <code>CharSource</code> object and
//    * call {@link #setIn(CharSource)}.
//    * @see #setIn(CharSource)
//    */
//   public void setIn(InputStream in) {
//     setIn(new ReaderCharSource(in));
//   }

//   /**
//    * <p>changes the {@link Dfa} this machinery operates on. Changing
//    * the DFA in a callback action is permissable. In particular, this
//    * allows to filter different parts of the input with different
//    * automata.</p>
//    *
//    * @throws java.lang.IllegalArgumentException if
//    * <code>dfa.matchesEmpty()</code> returns <code>true</code>.
//    * @deprecated This method does not initialize the behaviour of the
//    * DfaRun in case no match is found from the given Dfa. Use {@link
//    * #setDfa} instead.
//    */
//   public void setSaneDfa(Dfa dfa) {
//     if( dfa.matchesEmpty() ) {
//       throw new java.lang.IllegalArgumentException(EEPSMATCHER);
//     }
//     setAnyDfa(dfa);
//   }
  /**
   * <p>changes the {@link Dfa} to run. In addition the way to handle
   * unmatched input is (re)initialized from the given {@link
   * Dfa}.</p>
   *
   * <p>If the given <code>Dfa</code> matches the empty string,
   * reading and filtering methods may enter an infinite loop. Either
   * check with {@link Dfa#matchesEmpty()} or know what you are
   * doing.</p>
   *
   * @see #setOnFailedMatch
   */
  public void setDfa(Dfa dfa) {
    this.dfa = dfa; 
    this.onFailedMatch = dfa.fmb;
  }

//   /**
//    * @deprecated This method does not initialize the behaviour of the
//    * DfaRun in case no match is found from the given Dfa. Use {@link
//    * #setDfa} instead.
//    */
//   public void setAnyDfa(Dfa dfa) { this.dfa = dfa; }

  /**
   * returns the {@link Dfa} operated by <code>this</code>.
   */
  public Dfa getDfa() { return dfa; }
  
  /**
   * <p>changes the way how unmatched input is handled. Any of the
   * values {@link #UNMATCHED_COPY}, {@link #UNMATCHED_DROP} or {@link
   * #UNMATCHED_THROW} may be used. The behaviour is automatically
   * (re)set by {@link #setIn setIn()} and by {@link #setDfa setDfa()}
   * to the value found in the {@link Dfa} operated.</p>
   *
   * <p>This purpose of this method is rather to allow callbacks of
   * the <code>Dfa</code> to change the handling of unmatched input
   * temporarily.</p>
   */
  public void setOnFailedMatch(FailedMatchBehaviour b) {
    onFailedMatch = b;
  }

  /**
   * <p>returns the currently active behaviour for unmatched
   * input.</p>
   */
  public FailedMatchBehaviour getFailedMatchBehaviour() {
    return onFailedMatch;
  }

  /**
   * <p>is a helper function which should only be called immediately after
   * calling {@link #next next()} or {@link #read(StringBuffer)} to get
   * the position where the match starts. This is only needed when the
   * machine is in {@link #UNMATCHED_COPY} mode, because otherwise the
   * match will be the first thing appended to the
   * <code>StringBuffer</code> given to <code>next()</code> or
   * <code>read()</code>.</p>
   *
   * <p><b>Hint:</b> When using this method together with
   * <code>read(StringBuffer)</code>, be aware that the callback
   * handling the match is in principle allowed to delete characters
   * even before the value returned here, rendering the returned value
   * completely useless. &mdash; Know your callbacks!</p>
   */
  public int matchStart() { return matchStart; }

  /**********************************************************************/
  /**
   * reads one character immediately from the input source and returns
   * it without filtering. If filtered characters are already
   * available because of a previous {@link #read()}, these are not
   * touched and will be used in the next call to one of the
   * <code>read()</code> functions.
   */
  public int skip() throws java.io.IOException {
    return in.read();
  }

  /**
   * <p>shoves back characters into the input of the
   * <code>DfaRun</code> while deleting them from the given
   * <code>StringBuffer</code>. The characters will be the first to be
   * read when the machine performs the next match, e.g. when {@link
   * #read} is called.</p>
   */
  public void unskip(StringBuffer s, int startAt) {
    in.pushBack(s, startAt);
  }
  /**
   * <p>shoves back characters into the input of the
   * <code>DfaRun</code>.</p>
   * <p><b>Warning:</b> Do not use this method in time critical
   * applications. It calls the other unskip method with a freshly
   * created <code>StringBuffer</code>.</p>
   * @see #unskip(StringBuffer, int)
   */
  public void unskip(String s) {
    unskip(new StringBuffer(s), 0);
  }

  /**
   * <p>shoves back characters into the input of the
   * <code>DfaRun</code>. This method simply applies {@link
   * TextStore#drain TextStore.drain()} to the input of
   * <code>this</code>. Consequently, <code>start</code> may be
   * negative to indicate a suffix of <code>ts</code> to be pushed
   * back.</p>
   */
  public void unskip(TextStore ts, int start) {
    ts.drain(in, start);
  }
  /**********************************************************************/
  /**
   * <p>may be called by a callback to
   * retrieve see <a
   * href="doc-files/resyntax.html#rse">submatches</a>. Retrieving
   * submatches must be 
   * done before the match is changed in any way. A typical call
   * within an {@link FaAction} looks like <pre>
   *   public void invoke(StringBuffer out, int start, DfaRun r) 
   *     throws CallbackException {
   *   {
   *     TextStore ts = r.submatches(out, start);
   *     ...
   *   }</pre>
   * Parameter <code>txt</code> is not changed in any way.</p>
   *
   * @param txt must contain the full match starting at position
   * <code>start</code>. It may contain more characters.
   *
   * @param start is the position where the full match starts within
   * <code>txt</code> 
   *
   * @return a <code>TextStore</code> that contains the whole match as
   * part 0 and submatches as subsequent parts. The return value is
   * private to <code>this</code> and its contents may only be used
   * locally in a callback. After returning from the callback, the
   * contents of the result may soon change.
   */
  public TextStore submatches(StringBuffer txt, int start) {
    readTs.clear();
    //System.out.println("-->"+txt+"<--, `"+txt.substring(start)
    //+"'  "+smd.size);
    readTs.appendPart(txt, start, txt.length());
    smd.analyze(readTs, action);
    return readTs;
  }
  /**********************************************************************/
  /**
   * <p>finds the next match in the current input, appends it to
   * <code>out</code> and returns the {@link FaAction} associated with
   * the match. Input is read until a match is found, {@link #maxCopy}
   * is reached or EOF is hit. Non-matching input is handled
   * according to {@link #setOnFailedMatch setOnFailedMatch()}. In
   * particular:</p>
   *
   * <dl>
   * <dt>{@link #UNMATCHED_COPY}</dt><dd> will append up to {@link
   * #maxCopy} non-matching characters in front of the match. If
   * <code>maxCopy</code> is reached before the match, <b>no matching
   * text is returned</b>, only the non-matching characters. In this
   * case the return value is <code>null</code>, and should
   * <code>maxCopy</code> be &le;&nbsp;1, then 1 character is always
   * delivered. If a match is found before <code>maxCopy</code> is
   * reached, the match is appended to <code>out</code>. To find
   * out where the match actually starts, call {@link #matchStart()}.</dd>
   *
   * <dt>{@link #UNMATCHED_DROP}</dt><dd>will drop (delete)
   * unmatched text. In this case the matching text is the only text
   * appended to <code>out</code>.</dd>
   *
   * <dt>{@link #UNMATCHED_THROW}</dt><dd>causes a
   * {@link monq.jfa.NomatchException} to be thrown. No text will
   * be appended to <code>out</code> and the offenting text will still
   * be available in the {@link CharSource} serving as input to
   * <code>this</code>.</dd>
   * </dl>
   *
   * <p><b>Hint:</b> Use this method if you are interested only in a
   * simple tokenization of the input. The actions returned may serve
   * as the token type. If you however want to apply the actions
   * returned immediately to the match, then rather use one of the
   * <code>read</code> or <code>filter</code> methods. If you find
   * yourself using <code>if</code> statements on the
   * <code>FaAction</code> returned, you are definitively doing
   * something wrong.</p>
   *
   * @return <dl>
   * <dt>eofAction</dt><dd> When EOF is hit the first time and the
   * <code>Dfa</code> operated has a action set for EOF
   * which is not <code>null</code> this is returned (see {@link
   * Nfa#compile Nfa.compile()}).</dd>
   *
   * <dt>{@link #EOF}</dt><dd>if EOF is hit and
   * <code>eofAction</code> was already delivered or is
   * <code>null</code>. The output may have non-matching input that
   * was found just before EOF.</dd>
   *
   * <dt><code>null</code></dt><dd>if <code>UNMATCHED_COPY</code> is
   * active and <code>maxCopy</code> non-matching characters where
   * found before a match was encountered.</dd>
   *
   * <dt>an action</dt><dd> found for a match.</dd>
   * </dl>
   *
   */
  public FaAction next(StringBuffer out) 
    throws java.io.IOException 
  {
    matchStart = out.length();
    FaAction a = dfa.match(in, out, smd);

    if( a==null ) {
      // There was no match, so we have to search for the first
      // match. Note: there is always at least one character available as
      // long as not Dfa.EOF is returned by dfa.match()
      if( onFailedMatch==UNMATCHED_COPY ) {
	int unmatched = 0;
	do {
	  out.append((char)(in.read()));
	  unmatched += 1;
	  a = dfa.match(in, out, smd);
	} while( a==null && unmatched<maxCopy );
	matchStart += unmatched;

      } else if( onFailedMatch==UNMATCHED_DROP ) {
	do {
	  in.read();
	  a = dfa.match(in, out, smd);
	} while( a==null );

      } else {
	// everything else is a failure
	String emsg = lookahead();
	throw new NomatchException("no matching regular expression "+
				   "when looking at `"+emsg+"'");
      }
    }
    
    // We handle EOF and eofAction as if we have found a match
    if( a==EOF && dfa.eofAction!=null && eofArmed) {
      eofArmed = false;
      return dfa.eofAction;
    }

    return a;
  }
  /**********************************************************************/
  /**
   * fetch a bit of lookahead for use in messages for
   * exceptions. The lookahead is pushed back into the input
   * afterwards. 
   */
  private String lookahead() {
    // Read up to 30 chars for a decent error message
    StringBuffer sb = new StringBuffer(30);
    int i;
    try {
      for(i=0; i<30; i++) {
	int ch = in.read();
	if( ch==-1 ) break;
	sb.append((char)ch);
      }
    } catch( java.io.IOException e ) {
      in.pushBack(sb, 0);
      return "IOException when trying to generate context info";
    }
    String result;
    if( i==30 ) result = sb.substring(0, 27)+"...";
    else result = sb.toString()+"[EOF]";
    in.pushBack(sb, 0);
    return result;
  }
  /**********************************************************************/
  /**
   * ==== IMPORTANT ====
   * This should never again be public because it does not honour the
   * chunks defined by actions+collect mode. It only honours chunks of
   * actions. This should not be made visible to the outside.
   *
   * <p>calls {@link #next} once and applies the returned {@link
   * FaAction}. This includes the special actions configured for non
   * matching input and EOF, if any.</p>
   *
   * <p>Due to the <code>FaAction<code> applied to <code>out</code>
   * after calling <code>next</code> anything can happen to
   * <code>out</code>. In particular it need not become longer.</p>
   *
   * @return <code>false</code> if <code>out</code> was not changed
   * and would not be changed &mdash; due to EOF &mdash; by any
   * subsequent calls to this method. If <code>true</code> is
   * returned, <code>out</code> is not necessarily changed, but there
   * is more input available to process.
   */
  private boolean crunch(StringBuffer out) throws java.io.IOException {
    int l = out.length();
    action = next(out);
    if( action==null ) return true;

    if( action==EOF ) {
      // there may have been some unmatched characters added to out
      // just before EOF was hit
      return l<out.length();
    }
    try {
      action.invoke(out, matchStart, this);
    } catch( CallbackException e ) {
      String msg;
      if( matchStart<=out.length() ) {
	msg = e.getMessage()+
	  ". The match, possibly changed by the complaining "+
	  "action, follows in "+
	  "double brackets:\n[["+out.substring(matchStart)+"]]";
      } else {
	msg = e.getMessage() + 
	  ". Matched and filtered data just before the "+
	  "match triggering the exception is: `"+out+"'";
      }

      CallbackException ee = new CallbackException(msg);
      ee.setStackTrace(e.getStackTrace());
      ee.initCause(e.getCause());
      throw ee;
    }
    return true;
  }
  /**********************************************************************/
  /**
   * <p>delivers filtered data in naturally occuring chunks by
   * appending to <code>out</code>. As long as {@link #collect} is
   * <code>false</code>, the naturally occuring chunk is determined by
   * one call to {@link #next next()}, and the application of the
   * returned callback. The data may be prefixed with filtered data
   * not yet delivered by a previous call to {@link
   * #read(StringBuffer,int)}. Because the callback may delete the
   * matching text, the string returned may be empty.</p>
   *
   * <p>If an {@link FaAction#invoke FaAction.invoke()} callback
   * switches to <code>collect==true</code>, this function keeps
   * filtering until <code>collect</code> is reset to
   * <code>false</code> by another action callback. This allows the
   * action callbacks to hold back data from being delivered in cases
   * where several action callbacks cooperate in the decision about
   * shipping the data. The action callbacks have access to all the
   * filtered data held back and may treat it as needed. In particular
   * the data can be deleted before <code>collect</code> is switched
   * back to <code>false</code>.</p>
   *
   *
   * <p><b>Hint:</b> This method can be used to tokenize the input. If
   * the machine is put into <code>UNMATCHED_DROP</code> mode, every
   * call to this method will return exactly one match, treated by the
   * action bound to it.</p>
   *
   * @exception java.io.EOFException if
   * EOF is hit while <code>collect==true</code>.
   * @exception CallbackException if a callback throws this exception 
   *
   * @return <code>true</code>, if some input was read and
   * filtered. It also means that this method should be called again
   * because there might be more input waiting to be processed. Only
   * if <code>false</code> is returned, all input is completely
   * processed and <code>out</code> was not changed.</p>
   */
  public boolean read(StringBuffer out) throws java.io.IOException {
    // copy stuff which might have been pushed back to the output
    int popped = pop(out);

    boolean unfinished;
    while( (unfinished=crunch(out)) && collect ) /**/;
    if( collect ) throw new java.io.EOFException(ECOLLECT);
    return unfinished || popped>0;
  }

  /**
   * reads and filters input until <code>out</code> is grown by
   * <code>count</code> characters. Less characters are returned if
   * all input was processed. The field {@link #collect} is
   * hounored in the same way as by {@link #read(StringBuffer)}.
   *
   * @return <code>true</code>, if at least one character can be
   * delivered or if <code>count==0</code>. A return of
   * <code>false</code> signals that all input was processed.
   */
  public boolean read(StringBuffer out, int count) 
    throws java.io.IOException 
  {
    int l = out.length();

    // copy stuff which might have been pushed back to the output
    int popped = pop(out, count);
    boolean outChanged = false;
    boolean tmp;
    while( out.length()-l<count && (tmp=read(out)) ) {
      outChanged = outChanged || tmp;
    }
    int tooMany = out.length()-l-count;
    if( tooMany>0) pushBack(out, out.length()-tooMany);
    return outChanged || popped>0;
  }
  /**********************************************************************/
  /**
   * reads and filters input until at least one character is available
   * or EOF is hit. The field {@link #collect} is hounored in the same
   * way as by {@link #read(StringBuffer)}.
   *
   * @return the resulting character casted to <code>int</code> or -1
   * to signal EOF.
   */
  public int read() throws java.io.IOException {
    // The next line is not really necessary because the case would be
    // handled quite nicely by the read() below, but hopefully it
    // speeds up things quite a bit.
    int ch;
    if( (ch=super.readOne())>=0 ) return (char)ch;

    // We have to loop a bit because read(buf) not necessarily
    // delivers a character, even if it returns true.
    readBuf.setLength(0);

    // we cannot use the return value of the following read because
    // 'true' merely signals that some input was processed, not that
    // output was produced
    read(readBuf, 1);
    if( readBuf.length()>0 ) return readBuf.charAt(0);
    else return -1;
  }  
  /**********************************************************************/
  /**
   * <p>reads and filters input, copying it to the output
   * until EOF is hit.</p>
   */
  public void filter(StringBuffer out) throws java.io.IOException {
    // Note: we don't have to care about .collect because the caller
    // will only see the final result, while .collect is used to hold
    // back partial results from being delivered with read
    while(crunch(out)) /**/;
  }
  /**********************************************************************/
  /**
   * <p>reads and filters input, copying it to the output
   * until EOF is hit.</p>
   */
  public void filter(PrintStream out) 
    throws java.io.IOException
  {
    StringBuffer sb = new StringBuffer(4200);
    while( read(sb) ) {
      if( sb.length()<4096 ) continue;
      out.print(sb);
      sb.setLength(0);
      if( out.checkError() ) return;
    }
    out.print(sb);
  }
  /**********************************************************************/
  /**
   * <p>reads and filters the given input and returns the filtered
   * result.</p>
   */
  public synchronized String filter(String in) 
    throws java.io.IOException
  {
    readBuf.setLength(0);
    setIn(new CharSequenceCharSource(in));
    while( crunch(readBuf) ) /**/;
    return readBuf.toString();
  }    
  /**********************************************************************/
  /**
   * <p>run the machine until EOF is hit. This is useful, when the
   * callbacks don't produce output text but rather perform different
   * work.</p>
   * <b>Note:</b>This method sets up a <code>StringBuffer</code>
   * into which filtered data is dumped. The buffer is regularly
   * cleared, in particular after each match. To prevent this from
   * happening, use {@link #collect} as for the other
   * <code>filter</code> methods.
   */
  public synchronized void filter() 
      throws java.io.IOException
  {
    readBuf.setLength(0);
    while( crunch(readBuf) ) {
      if( !collect ) readBuf.setLength(0);
    }
  }
}
 
