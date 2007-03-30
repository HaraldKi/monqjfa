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

//import jfa.*;
import monq.jfa.actions.*;

import java.lang.Class;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.Vector;
import java.util.Iterator;
import java.io.StringReader;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

/**
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class NfaTest extends TestCase {
  //ReParser rep;
  Nfa rep;
  public void setUp() throws ReSyntaxException {
    //rep = new ReParser();
    rep = new Nfa(Nfa.NOTHING);
  }

  public void testSeqToNFA() throws ReSyntaxException {
    String s = "abcdedcba";
    Nfa fa = rep.parse(s, Copy.COPY);
    //assertEquals(s.length()+1,fa.size());
    FaState state = fa.getStart();
    FaState other = null;
    // the last char of a sequence is added via an epsilon, therefore
    // L-1 below.
    for(int i=0, L=s.length(); i<L-1; i++) {
      other = state.follow(s.charAt(i));
      assertNotNull(other);
      state = other;
    }
    //assertTrue(other.isStop());
  }

  public void testDotToNFA() throws ReSyntaxException {
    String s = ".";
    Nfa fa = rep.parse(s, Copy.COPY);
    //assertEquals(2, fa.size());
    FaState state = fa.getStart();
    assertNotNull(state);

    FaState other = state.follow('\0');
    assertNotNull(other);

    for(char ch=Character.MIN_VALUE+1; ch<Character.MAX_VALUE; ch++) {
      assertSame(other, state.follow(ch));
    }
    //assertTrue(other.isStop());
  }

  public void testRangeToNFA() throws ReSyntaxException {
    //System.out.println("testRangeToNFA");
    String[] re = {"[abc]", "[0-9]", "[]\\-]", "[a0b1c2d3e]"};
    String[] s = {"abc", "012346789", "-]", "abcde0123"};

    for(int i=0; i<re.length; i++) {
      Nfa fa = rep.parse(re[i], Copy.COPY);
      //assertEquals(2, fa.size());
      FaState start = fa.getStart();
      FaState stop = start.follow(s[i].charAt(0));
      for(int j=1; j<s[i].length(); j++) {
	FaState other = start.follow(s[i].charAt(j));
	assertSame(stop, other);
      }
      //assertTrue(stop.isStop());
    }
  }
  public void testInvertRange() throws ReSyntaxException {
    Nfa nfa = rep.parse("[^acf]+", Drop.DROP);
    assertEquals(2, nfa.findPath("  a"));
    assertEquals(3, nfa.findPath("   c"));
    assertEquals(5, nfa.findPath("     f"));
  }
  public void testOptional() throws ReSyntaxException {
    Nfa nfa = rep.parse("[a-bx0]?", Drop.DROP);
    assertEquals(1, nfa.findPath("a"));
    assertEquals(0, nfa.findPath(""));
    assertEquals(0, nfa.findPath("X"));
  }
  public void testStar() throws ReSyntaxException {
    Nfa nfa = rep.parse("[ab]*", Drop.DROP);
    assertEquals(0, nfa.findPath("X"));
    assertEquals(1, nfa.findPath("a"));
    assertEquals(8, nfa.findPath("abababab"));
  }
  public void testPlus() throws ReSyntaxException {
    Nfa nfa = rep.parse("[ab]+", Drop.DROP);
    assertEquals(-1, nfa.findPath("X"));
    assertEquals(1, nfa.findPath("a"));
    assertEquals(7, nfa.findPath("abababa"));
  }
  public void testOr() throws ReSyntaxException {
    Nfa nfa = rep.parse("ab|rx|hallo", Drop.DROP);
    assertEquals(2, nfa.findPath("ab"));
    assertEquals(2, nfa.findPath("rx"));
    assertEquals(5, nfa.findPath("hallo"));
  }
   public void testDoubleOr() 
     throws ReSyntaxException, CompileDfaException
  {
    // try to enforce the first branch in Nfa.or()
    Nfa nfa = rep.parse("a|b*", Drop.DROP);
    assertEquals(1, nfa.findPath("a"));
    assertEquals(1, nfa.findPath("b"));
    assertEquals(3, nfa.findPath("bbbxxy"));
    assertEquals(0, nfa.findPath("y"));
  }


  public void testSeq() throws ReSyntaxException {
    Nfa nfa = rep.parse("a[0-9](xx|yy)", Drop.DROP);
    assertEquals(4, nfa.findPath("a0xx"));
    assertEquals(4, nfa.findPath("a1xx"));
    assertEquals(4, nfa.findPath("a2xx"));
    assertEquals(4, nfa.findPath("a3xx"));
    assertEquals(4, nfa.findPath("a0yy"));
    assertEquals(4, nfa.findPath("a1yy"));
    assertEquals(4, nfa.findPath("a2yy"));
  }

  // Test a certain bug I had when the arg of Nfa.seq() had a start
  // state with only epsilon-states going out.
  public void testSeqToStartWithEps() throws ReSyntaxException {
    // the parsing went wrong already
    Nfa nfa = rep.parse("b[a]*", Drop.DROP);

    // nevertheless a basic test
    assertEquals(1, nfa.findPath("b"));
    assertEquals(2, nfa.findPath("ba"));
    assertEquals(4, nfa.findPath("baaa"));
  }
  
  public void testCompileBasic() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = rep.parse("a", Drop.DROP).compile();
    String s = "aaa";
    CharSource cs = new ReaderCharSource(new StringReader(s));
    StringBuffer sb = new StringBuffer();
    DfaRun r = new DfaRun(dfa);
    r.setIn(cs);

    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(DfaRun.EOF, r.next(sb));
    assertEquals(s, sb.toString());
  }
  public void testCompileOr() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = rep.parse("(ab)|(cd)", Drop.DROP).compile();
    String s =  "abcdcdab";
    CharSource cs = new ReaderCharSource(new StringReader(s));
    StringBuffer sb = new StringBuffer();
    DfaRun r = new DfaRun(dfa);
    r.setIn(cs);

    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(2, sb.length());
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(4, sb.length());
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(6, sb.length());
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(8, sb.length());
    assertEquals(DfaRun.EOF, r.next(sb));
    assertEquals(8, sb.length());
    assertEquals(s, sb.toString());
  }
   
  public void testSameActionTwice() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    FaAction a = new Printf("xx");
    Dfa dfa = 
      rep.parse("a|b", a)
      .or("b|a", a)
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "abba";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("xxxxxxxx", sb.toString());
  }

  public void testTwoActionsPrio1() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    FaAction a = new Printf(false, "xx", 1);
    FaAction b = new Printf(false, "yy", 2);
    Dfa dfa =
      rep.parse("a|b", a)
      .or("b|a", b)
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "abba";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("yyyyyyyy", sb.toString());
  }

  // same test as before, but the priority should select the other
  // action this time.
  public void testTwoActionsPrio2() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    //System.out.println("testTwoActionsPrio2");
    FaAction a = new Printf(false, "xx", 2);
    FaAction b = new Printf(false, "yy", 1);
    Dfa dfa = 
      rep.parse("a|b", a)
      .or("b|a", b)
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "abba";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("xxxxxxxx", sb.toString());
  }

  public void testTwoActionsClash() 
    throws ReSyntaxException
  {
    FaAction a = new Printf(false, "xx", 2);
    FaAction b = new Printf(false, "yy", 2);
    Exception e = null;
    try {
      Dfa dfa = 
	rep.parse("a|b", a)
	.or("b|a", b)
	.compile();
      dfa.toDot(System.out);
    } catch( CompileDfaException _e ) {
      e = _e;
    }
    int l = CompileDfaException.EAMBIGUOUS.length();
    assertEquals(CompileDfaException.EAMBIGUOUS, 
		 e.getMessage().substring(0, l));
  }

  /**
   * an Nfa with no actions should compile to the empty automaton
   */
  public void testCompileToEmpty() throws Exception {
    Dfa dfa = rep.parse("abc").compile();
    Nfa nfa = dfa.toNfa();
    dfa = nfa.or("xyz", Copy.COPY).compile(DfaRun.UNMATCHED_DROP);
    DfaRun r = new DfaRun(dfa);
    String s = r.filter("abcxyzabc");
    assertEquals("xyz", s);
  }

  public void testDfaRunWithEpsRecognizer() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    //System.out.println("testDfaRunWithEpsRecognizer");
    FaAction a = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun r) {
	  if( out.length()==start ) {
	    // we matched epsilon, so we make something up
	    try {
	      r.skip();
	    } catch( java.io.IOException e ) {
	      assert false;
	    }
	    out.append('b');
	  }
	}
      };
    Dfa dfa = rep.parse("a*", a).compile();

    // we have to do tricks to get an epsmatcher into a DfaRun
    Dfa dummy = rep.parse("x").compile(DfaRun.UNMATCHED_THROW);
    DfaRun r = new DfaRun(dummy);
    r.setDfa(dfa);
    String s = r.filter("yyaayy");
    assertEquals("bbaabb", s);
  }

  public void testNfaAddTransitions1() 
    throws ReSyntaxException,  CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      rep.parse("[abc]", Drop.DROP)
      .or("[b-z]", Drop.DROP)
      .compile();
    String s = "abcdefghijklmnopqrstuvwxyz";
    CharSource cs = new ReaderCharSource(new StringReader(s));
    StringBuffer sb = new StringBuffer(32);
    DfaRun r = new DfaRun(dfa);
    r.setIn(cs);

    for(int i=0; i<s.length(); i++) {
      assertEquals(Drop.DROP, r.next(sb));
      assertEquals(i+1, sb.length());
    }
    assertEquals(s, sb.toString());
  }

  // similar test as before, except that the intervals overlap in the
  // other direction
  public void testNfaAddTransitions2() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    //System.out.println("testNfaAddTransitions2");
    Dfa dfa = 
      rep.parse("[b-er-v]", Drop.DROP)
      .or("[abcs-z]", Drop.DROP)
      .compile();
    String s = "abcderstuvwxyz";
    CharSource cs = new ReaderCharSource(new StringReader(s));
    StringBuffer sb = new StringBuffer(32);
    DfaRun r = new DfaRun(dfa);
    r.setIn(cs);

    for(int i=0; i<s.length(); i++) {
      assertEquals(Drop.DROP, r.next(sb));
      assertEquals(i+1, sb.length());
    }
    assertEquals(s, sb.toString());
  }

  /**
   * dfa compilation once had a problem where a|b resulted in an
   * automaton with two distinct stop states. The reason was that
   * <code>isImportant</code> was not implemented correctly.
   */
  public void testIsImportant()  
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    //System.out.println("testIsImportant");
    Dfa dfa =
      rep.parse("a|b", Drop.DROP)
      .compile();

    //dfa.getFaToDot(System.out).go();
    // FIX ME: how can I write this test with the current interfaces?
    //assertTrue(false);

  }
  public void testOrWithAction()  
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    // it is important, that the two actions used don't end up in the
    // same DFA state, which happend in an early implementation.
    // The following should simply not throw anything.
    Dfa dfa =
      rep.parse("a", Copy.COPY)
      .or("b", Drop.DROP)
      //.or("b", new Replace(""))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "abasdfj asdfj asdfj as;ldfj asdjf ";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    while(r.read(sb)) /**/;
    assertEquals("aaaaaa", sb.toString());
  }

  public void testUNMATCHED_COPY()  
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    FaAction a = new Printf(".");
    Dfa dfa = rep.parse("[a-c]+", a).compile(DfaRun.UNMATCHED_COPY);
    String s = "cccHabcabaAaaaaRbbbbAabcLaDcc";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    for(int i=0; i<7; i++) {
      assertTrue(r.read(sb));
    }
    assertFalse(r.read(sb));
    assertEquals(".H.A.R.A.L.D.", sb.toString());
  }
  public void testUNMATCHED_DROP()  
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = 
      rep.parse("[A-Z]+", Copy.COPY).compile(DfaRun.UNMATCHED_DROP);
    String s = "KaIxRaSbCcH";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    for(int i=0; i<6; i++) {
      assertTrue(r.read(sb));
    }
    assertFalse(r.read(sb));
    assertEquals("KIRSCH", sb.toString());
  }
  public void testUNMATCHED_THROW()  
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = 
      rep.parse("[A-Z]+", Copy.COPY).compile(DfaRun.UNMATCHED_THROW);
    String s = "xKaIxRaSbCcH";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    for(int i=0; i<6; i++) {
      int start = 0;
      Exception e = null;
      try {
	start = sb.length();
	r.read(sb);
      } catch( NomatchException _e) { e = _e; }
      assertTrue(e instanceof NomatchException );
      assertTrue(r.skip()>=0);
      assertTrue(r.read(sb));
    }
    assertEquals("KIRSCH", sb.toString());
  }
  public void testDfaRunPiped() 
    throws ReSyntaxException,  CompileDfaException,
    java.io.IOException
  {
    // The first step replaces blank-lowercase like ' a' by '# a#'
    FaAction mark = new Printf("#%0#");
    Dfa dfa1 = rep.parse(" [a-z]", mark).compile(DfaRun.UNMATCHED_COPY);

    // The 2nd step will used the marked stuff to upcase the marked
    // character 
    FaAction upCase = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun runner) {
	  char ch = out.charAt(start+2);
	  out.setLength(start);
	  out.append(' ').append(Character.toUpperCase(ch));
	}
      };
    Dfa dfa2 = rep.parse("# .#", upCase).compile(DfaRun.UNMATCHED_COPY);

    String s = " alle meine entchen schwimmen auf dem see";

    // now connect the two automata
    DfaRun r = new DfaRun(dfa1, new CharSequenceCharSource(s));
    r = new DfaRun(dfa2, r);

    StringBuffer sb = new StringBuffer(50);
    assertTrue(r.read(sb, 100));
    assertFalse(r.read(sb));
    assertEquals(" Alle Meine Entchen Schwimmen Auf Dem See",
		 sb.toString());
  }
  public void testReaderPushBack() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = 
      rep.parse("[a-z]+(0000)?", new Printf(false, "FULL", 0))
      .or("[a-z]+", new Printf(false, "PART", 1))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "abc000def0000xyz0r0000";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    for(int i=0; i<4; i++) {
      assertTrue(r.read(sb));
    }
    assertFalse(r.read(sb));
    assertEquals("PARTFULLPARTFULL", sb.toString());
  }
  public void testSeqString() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      rep.parse("abc|def")
      .seq("xxx", new Printf("1"))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "abcxxxdefxxx";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("11", sb.toString());
  }
  public void testOrString() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      rep.parse("abc|def")
      .or("xxx")
      .addAction(new Printf("1"))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "abcxxxdefxxx";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("1111", sb.toString());
  }
  /**
   * I added this test to never forget again that a start state may
   * also be a stop state. I tried to artificially forbid this
   * possibility because using a DfaRun as a filter which accepts the
   * empty string may easily end up in an infinite loop. However, the
   * right place to ignore a start state as a valid stop state is
   * indeed in the DfaRun, not in Nfa. Otherwise many operations on
   * Nfas are simply either wrong or would be hard to fix.
   */
  public void testStartCanBeStop() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dummy = rep.parse("x", Copy.COPY).compile();

    Dfa dfa = rep.parse("a*", Copy.COPY).compile();
    StringBuffer sb = new StringBuffer();
    DfaRun r = new DfaRun(dummy);
    r.setDfa(dfa);

    r.setIn(new CharSequenceCharSource("x"));
    assertEquals(Copy.COPY, r.next(sb));
    assertEquals(0, sb.length());

    r.setIn(new CharSequenceCharSource("ax"));
    assertEquals(Copy.COPY, r.next(sb));
    assertEquals(1, sb.length());

    r.setIn(new CharSequenceCharSource("aax"));
    assertEquals(Copy.COPY, r.next(sb));
    assertEquals(3, sb.length());
    assertEquals("aaa", sb.toString());
  }

  /**
   * tries to enforce a huuuuge pushback such that the CharSource we
   * are reading from has to reallocate its buffer at least once.
   */
  public void testCharSourcePushBack()  
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    // assumed necessary size of pushback, see EmtpyCharSource.java
    int L = 200;
    // a pushback becomes necessary if the machine is running forward
    // in the hope it can eventually find a better match than it has
    // already. In the following automaton, after reading the 2nd 'a',
    // the machine keeps going reading 'a's in the hope to finally
    // find an 'x'. If it does not find an 'x', it has to pushback all
    // but one 'a'.
    Dfa dfa = 
      rep.parse("a", new Printf("b"))
      .or("(a*x)!", Drop.DROP)
      .compile(DfaRun.UNMATCHED_THROW);
    StringBuffer in = new StringBuffer(L);
    StringBuffer out = new StringBuffer(L);
    for(int i=0; i<L; i++) in.append('a');
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(in));
    for(int i=0; i<L; i++) {
      assertTrue(r.read(out));
    }
    assertFalse(r.read(out));
    for(int i=0; i<L; i++) assertEquals('b', out.charAt(i));
  }

  /**
   * trigger a once detected bug where the shortest() operator, which
   * has to compile the automaton, did not make sure that the start
   * state is able to get epsilon transitions added. This is an issue,
   * because compilation of course creates states for a Dfa, which
   * cannot have epsilon transitions.
   */
  public void testShortestAllowEpsilonInStartState() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    // basically the following should not throw an exception
    Nfa nfa = rep.parse("(a+)!|b", Drop.DROP);

    // Nevertheless we check some basic functionality, in particular
    // the shortest operator put this thing into a "a|b" machine.
    assertEquals(1, nfa.findPath("aa"));
    assertEquals(1, nfa.findPath("bb"));
    assertEquals(-1, nfa.findPath("x"));
  }

  // try shortest *after* an action was assigned
  public void testShortestAfterActionAssigned()
    throws ReSyntaxException, CompileDfaException
  {
    Nfa nfa = rep.parse("[abc]+", Drop.DROP).shortest();
    assertEquals(-1, nfa.findPath("xx"));
    assertEquals(1, nfa.findPath("a"));
    assertEquals(1, nfa.findPath("bx"));
    assertEquals(1, nfa.findPath("cc"));
  }

  // force some coverage in Nfa.hasAction(Set)
  public void test_hasAction() 
    throws ReSyntaxException
  {
    Nfa nfa = new Nfa("a", Copy.COPY).or("a", Drop.DROP);
    assertEquals(1, nfa.findPath("a"));
  }

  /**
   * for completeness we also call the constructor which takes an
   * ation too.
   */
  public void testNfaConstructorTwoArgs() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = 
      new Nfa("[a-z][a-z][0-9]", new Printf("x"))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "......ab1....xy2....rr4....rr...ab5";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    while(r.read(sb)) /**/;
    assertEquals("xxxx", sb.toString());
  }
  public void testNfaSeqWithoutAction() 
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = new Nfa(Nfa.EPSILON).seq("[a-z][a-z]").seq("[0-9]")
      .addAction(new Printf("x"))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "......ab1....xy2....rr4....rr...ab5";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    while(r.read(sb)) /**/;
    assertEquals("xxxx", sb.toString());
  }

  /**
   * tries to evoke a bug where 
   * <code>new Nfa("a", Drop.DROP).or("r")</code>
   * allows to reach the stop state via 'r'
   */
  public void testSymmetricOr() throws ReSyntaxException {
    Nfa nfa = new Nfa("a", Drop.DROP).or("r");
    assertEquals(1, nfa.findPath("a"));
    assertEquals(-1, nfa.findPath("r"));

    nfa = new Nfa(Nfa.NOTHING).or("a").or("r", Drop.DROP);
    assertEquals(-1, nfa.findPath("a"));
    assertEquals(1, nfa.findPath("r"));
  }
  
  public void testInvert1() throws ReSyntaxException {
    Nfa nfa = new Nfa("a~", Drop.DROP);
    assertEquals(0, nfa.findPath(""));
    assertEquals(1, nfa.findPath("b"));
    assertEquals(0, nfa.findPath("a"));
    assertEquals(2, nfa.findPath("ab"));
  }
  public void testInvert2() throws ReSyntaxException {
    Nfa nfa = new Nfa("x(...)~", Drop.DROP);
    assertEquals(-1, nfa.findPath("r"));
    assertEquals(1, nfa.findPath("x"));
    assertEquals(2, nfa.findPath("xa"));
    assertEquals(3, nfa.findPath("xab"));
    assertEquals(3, nfa.findPath("xabc"));
    assertEquals(5, nfa.findPath("xabcd"));
  }
  public void testInvert3() throws ReSyntaxException {
    Nfa nfa = new Nfa("(a*)~", Drop.DROP);
    assertEquals(-1, nfa.findPath("a"));
    assertEquals(-1, nfa.findPath("aa"));
    assertEquals(-1, nfa.findPath("aaa"));
    assertEquals(-1, nfa.findPath("aaaa"));
    assertEquals(5, nfa.findPath("aaaab"));
  }
  /**
   * for filtering, the normal invert operator is a bit useless,
   * because e.g. "(a*)~" will happily match a string which starts
   * with 'a' but continues with something else. For filtering it is
   * more interesting to have a regexp saying "does not start with
   * ..." and does not start with epsilon. An example is
   * "((a.*)?)~". This automaton in particular generates a dead state,
   * if dead states are not remove explicitly.
   */
  public void testInvert4() throws ReSyntaxException {
    Nfa nfa = new Nfa("((a.*)?)~", Drop.DROP);
    assertEquals(-1, nfa.findPath("a"));
    assertEquals(-1, nfa.findPath("aa"));
    assertEquals(-1, nfa.findPath("aab"));
    assertEquals(2, nfa.findPath("ba"));
  }
  // try inverting an automaton AFTER adding an action.
  public void testInvert5() throws ReSyntaxException, CompileDfaException {
    Nfa nfa = new Nfa("[abc]", Drop.DROP)
      .invert()
      .addAction(Drop.DROP)
      ;
    assertEquals(0, nfa.findPath("a"));
    assertEquals(0, nfa.findPath("b"));
    assertEquals(0, nfa.findPath("c"));
    assertEquals(2, nfa.findPath("aa"));
  }
  /**
   * inversion is able to create dead states which loop on every
   * character. Using such an automaton in a pipe is
   * disastrous. Consequently, here we check that dead (useless) state
   * removal works.
   */
  public void testDeadStateRemoval1() 
    throws ReSyntaxException, java.io.IOException,
	   CompileDfaException 
  {
    // The following Nfa recognizes either 'a' or "(.*)~". The latter
    // is actually the empty set. We test that this branch is suitably
    // removed.
    Nfa nfa = new Nfa("(.*)~|a", Drop.DROP);

    // This CharSource will fail with an assertion on the
    // third character read.
    CharSource s = new CharSource() {
	int count = 0;
	public int read() {
	  count +=1;
	  assertTrue(count<3);
	  return 'a';
	}
	public void pushBack(StringBuffer s, int i) {}
      };
    DfaRun r = 
      new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP), s);
    StringBuffer sb = new StringBuffer();
    r.read(sb);
  }    

  /**
   * the ChildIterator of a state is a bit tricky because it has to
   * iterate over two other iterators.
   */
  public void testChildIterator() {
    // this needs rethinking as soon as I know if CharTrans will keep
    // its iterator or not
    //    assertEquals("Have to think this through again", "");
    
//     FaState s = new AbstractFaState.NfaState();
//     FaState o = new AbstractFaState.EpsState();
//     CharTrans t = Nfa.createCharTrans();
//     t.set('a', 'c', o);
//     s.setTrans(t);
//     o = new AbstractFaState.EpsState();
//     s.getTrans().set('0', '1', o);
//     int i = 0;
//     for(Iterator iter=s.getChildIterator(); iter.hasNext(); /**/) {
//       iter.next();
//       i+=1;
//     }
//     assertEquals(2, i);
  }
  /**
   * try out the default actions COPY and DROP in a pipe.
   */
  public void testDROPandCOPY() 
    throws java.io.IOException, ReSyntaxException, CompileDfaException 
  {
    Dfa dfa =
      new Nfa("[a-zA-Z0-9]+", Copy.COPY)
      .or("[ \t\n]+", Drop.DROP)
      .or("[^a-zA-Z0-9 \t\n]", new Printf("%%"))
      .compile(DfaRun.UNMATCHED_THROW)
      ;
    String s = "Harald; Kirsch- hat's erfünden";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuffer sb = new StringBuffer();
    while( r.read(sb) ) ;
    assertEquals("Harald%Kirsch%hat%serf%nden", sb.toString());
  }

  /**
   * this threw an exception in a buggy pre-version where the single
   * char read did not expect that a call to read() may result in
   * nothing. 
   */
  public void test_readCharWithDroppingAction() 
    throws java.io.IOException, ReSyntaxException, CompileDfaException 
  {
    Dfa dfa = new Nfa("a+", Drop.DROP).compile(DfaRun.UNMATCHED_COPY);
    String s = "aaabbbb";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    assertEquals('b', r.read());
  }

  public void test_unskip() 
    throws java.io.IOException, ReSyntaxException, CompileDfaException 
  {
    FaAction a = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun runner) {
	  runner.unskip(out, out.length()-(out.length()-start)/2);
	  out.setLength(start);
	  out.append('b');
	}
      };
    Nfa nfa = new Nfa("aa|a", a);
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_THROW));
    String s = r.filter("aaaa");
    //System.out.println("@@@@"+s);
    assertEquals("bbbb", s);
  }

  public void testPriorityWithDrop()
    throws java.io.IOException, ReSyntaxException, CompileDfaException 
  {
    Nfa nfa =
      new Nfa("[ab]+", new Printf("(%0)"))
      .or("[ab]", new Drop(1))
      ;
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY), 
			  new CharSequenceCharSource("xxaxxbxxabxxba"));
    StringBuffer sb = new StringBuffer();
    while( r.read(sb) ) /*just run*/;
    assertEquals("xxxxxx(ab)xx(ba)", sb.toString());
  }

  // for completeness, do a dummy test of toDot
  public void test_toDot() 
    throws ReSyntaxException
  {
    Nfa nfa = new Nfa(Nfa.EPSILON).seq("a");
    ByteArrayOutputStream buf = new ByteArrayOutputStream(100);
    PrintStream out = new PrintStream(buf);
    nfa.toDot(out);
    String s = "digraph hallo {";
    assertEquals(s, buf.toString().substring(0, s.length()) );
  }


  // test unskip and changing behaviour on failed match of DfaRun
  public static void testDfaRun1()
    throws java.io.IOException, ReSyntaxException, CompileDfaException 
  {
    FaAction switchToDrop = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun r) {
	  r.setOnFailedMatch(DfaRun.UNMATCHED_DROP);
	}
      };

    FaAction unskipXX = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun r) {
	  out.setLength(start);
	  r.unskip("XX");
	}
      };

    // use the filter version which copies to StringBuffer to have
    // this tested as well somewhere
    StringBuffer sb = new StringBuffer();
    Nfa nfa = new Nfa("aa", unskipXX).or("XX", switchToDrop);
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY));
    r.setIn(new CharSequenceCharSource("1234aaxxxx"));
    r.filter(sb);
    //System.out.println("\n>>"+s+"<<");
    assertEquals("1234XX", sb.toString());
  }


  // test the collect-behaviour of a DfaRun
  public static void testDfaRun2()
    throws java.io.IOException, ReSyntaxException, CompileDfaException 
  {
    FaAction switchToCollect = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun r) {
	  if( r.collect ) return; // go for longest match
	  r.collect = true;
	  r.clientData = new Integer(start);
	}
      };

    FaAction dropCollected = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun r) {
	  int startPos = ((Integer)r.clientData).intValue();
	  out.setLength(startPos);
	  r.collect = false;
	  //System.out.println(""+r);
	}
      };
    FaAction release = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun r) {
	  out.setLength(start);
	  r.collect = false;
	}
      };

    Nfa nfa =
      new Nfa("a", switchToCollect)
      .or("DROP", dropCollected)
      .or("GO", release)
      .or("b+", new Printf("[%0]"))
      //.or(".", new AbstractFaAction.Copy(-1))
      ;
    String s = "...a..b..DROP...a..b..GOxxx";
    DfaRun run = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY), 
			    new CharSequenceCharSource(s));
    //System.out.println("\n"+run);
    StringBuffer result = new StringBuffer();
    StringBuffer scratch = new StringBuffer();
    while( run.read(scratch) ) {
      result.append(scratch);
      scratch.setLength(0);
    }
    //System.out.println("\n>>"+result+"<<");
    assertEquals("......a..[b]..xxx", result.toString());
  }

  // check that an EOF in collect throws
  public static void testDfaRun3()
    throws ReSyntaxException, CompileDfaException 
  {
    FaAction switchToCollect = new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun r) {
	  r.collect = true;
	}
      };
    Exception e = null;
    Nfa nfa =
      new Nfa("a", switchToCollect)
      ;
    DfaRun run = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY), 
			    new CharSequenceCharSource("aaa"));
    StringBuffer result = new StringBuffer();
    try {
      run.read(result);
    } catch( Exception _e) {
      e = _e;
    }
    assertTrue(e instanceof java.io.IOException);
    //assertEquals(DfaRun.ECOLLECT, e.getMessage());
  }

  // trivially test that getDfa does the right thing (the curse of
  // test coverage hits here since the test is too trivial.
  public static void testDfaRun4()
    throws ReSyntaxException, CompileDfaException 
  {
    Dfa dfa = new Nfa("a", Copy.COPY).compile(DfaRun.UNMATCHED_COPY);
    DfaRun run = new DfaRun(dfa);
    assertEquals(dfa, run.getDfa());
  }

  // check that overreading with DfaRun.read(buf,count) works
  public static void testDfaRun5()
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    Nfa nfa = new Nfa("a+", Copy.COPY);
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP), 
			  new CharSequenceCharSource("aaaaab"));
    StringBuffer sb = new StringBuffer();
    int l = 0;
    while( r.read(sb, 1) ) {
      assertEquals(1, sb.length()-l);
      l+=1;
    }
    assertEquals("aaaaa", sb.toString());
  }
  
  // check that overreading with DfaRun.read() works
  public static void testDfaRun6()
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    Nfa nfa = new Nfa("a+", Copy.COPY);
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP), 
			  new CharSequenceCharSource("ababab"));
    StringBuffer sb = new StringBuffer();
    int ch;
    while( -1!=(ch=r.read()) ) {sb.append((char)ch);}
    assertEquals("aaa", sb.toString());
  }
  public static void testDfaRun_Eofaction() throws Exception {
    DfaRun r = new
      Nfa("[0-9]+", new Replace("x"))
      .compile(DfaRun.UNMATCHED_COPY, new Replace("</EOF>"))
      .createRun()
      ;
    String s = r.filter("bla99bla");
    assertEquals("blaxbla</EOF>", s);
  }
  public static void testDfaRun_filter1() throws Exception {
    DfaRun r = new 
      Nfa("[0-9]+", new Replace("x"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    r.setIn(new CharSequenceCharSource("bla99bla88dong"));
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bao, true, "UTF-8");
    r.filter(out);
    out.close();
    String s = bao.toString("UTF-8");
    assertEquals("blaxblaxdong", s);
  }
  public static void testDfaRun_filter2() throws Exception {
    DfaRun r = new 
      Nfa("[0-9]+", new Replace("x"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bao, true, "UTF-8");
    r.setIn(new CharSequenceCharSource("bla99bla88dong"));
    r.filter(out);
    out.close();
    String s = bao.toString("UTF-8");
    assertEquals("blaxblaxdong", s);
  }

  public static void testDfaRun_Clash1() throws Exception {
    Nfa nfa = new
      Nfa("<[a-z]+>", new Drop(0))
      .or("<[rst]>", new Copy(0))
      ;
    Exception e = null;
    try {
      nfa.compile();
    } catch( CompileDfaException _e ) {
      e = _e;
    }
    assertTrue(e instanceof CompileDfaException );
    assertTrue(e.getMessage().indexOf("<[r-t]>")>0);
  }

  // A certain branch in Nfa.findAction is only executed if the clash
  // happens on the empty string. This tries to exercise it.
  public static void testDfaRun_Clash2() throws Exception {    
    Nfa nfa = new
      Nfa("[a-z]*", new Drop(0))
      .or("[rst]*", new Copy(0))
      ;
    Exception e = null;
    try {
      nfa.compile();
    } catch( CompileDfaException _e ) {
      e = _e;
    }
    assertTrue(e instanceof CompileDfaException );
    //System.out.println(e.getMessage());
    assertTrue(e.getMessage().indexOf("path `':")>0);
  }

  public static void test_or_withOther() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    Nfa other = new Nfa(Nfa.NOTHING).or("a");
    Nfa nfa =
      new Nfa(Nfa.EPSILON).seq("b").or(other)
      .addAction(new Printf("<%0>"))
      ;
    String s = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY))
      .filter("a b c")
      ;
    assertEquals("<a> <b> c", s);

    Exception e = null;
    try {
      // this should not work as other is invalid now
      other.addAction(Copy.COPY);
    } catch( java.lang.NullPointerException _e) {
      e = _e;
    }
    assertTrue( e instanceof java.lang.NullPointerException );
  }
  public static void test_seq_withOther() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    Nfa other = new Nfa(Nfa.EPSILON).seq("a");
    Nfa nfa = new 
      Nfa(Nfa.NOTHING).or("b").seq(other)
      .addAction(new Printf("<%0>"))
      ;
    String s = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY))
      .filter("ba c");
    assertEquals("<ba> c", s);

    Exception e = null;
    try {
      // this should not work as other is invalid now
      other.addAction(Copy.COPY);
    } catch( java.lang.NullPointerException _e) {
      e = _e;
    }
    assertTrue( e instanceof java.lang.NullPointerException );
  }

  // Exercise Dfa connected to InputStream
  public static void test_Dfa_withInputStream() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    java.io.ByteArrayInputStream in 
      = new java.io.ByteArrayInputStream("a b c".getBytes());
    Dfa dfa = new Nfa("a|b", new Printf("[%0]"))
      .compile(DfaRun.UNMATCHED_COPY);
    DfaRun r = new DfaRun(dfa, new ReaderCharSource(in));
    StringBuffer sb = new StringBuffer();
    while( r.read(sb) ) /**/;
    assertEquals("[a] [b] c", sb.toString());
  }

  public static void test_not()  
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    Nfa nfa = new Nfa("([ \t\n\r]+)^", new Printf("<%0>"));
    String s = new
      DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY))
      .filter("hallo \nx;y\t***")
      ;
    assertEquals("<hallo> \n<x;y>\t<***>", s);
  }
  /**********************************************************************/
//   public static void test_NfaWithSplitFmt() throws Exception {
//     String s = new
//       Nfa(Nfa.NOTHING)
//       .or("[0-9]+[.][0-9]+", "[.]", RegexpSplitter.SPLIT, "%1,%2")
//       .compile()
//       .createRun(DfaRun.UNMATCHED_COPY)
//       .filter("3.14 2.79 99.00 456.890")
//       ;
//     assertEquals("3,14 2,79 99,00 456,890", s);
//   }
  /**********************************************************************/
  public static void test_SetPrio() throws Exception {
    String s = new 
      Nfa("[a-z]+", new Embed("[", "]"))
      .or("[rst]+[0-9]*", new Embed("<", ">").setPriority(-1))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("abc-rrr111");
    assertEquals("[abc]-<rrr111>", s);
  }
  /**********************************************************************/
  public static void test_Escape1() {
    String s = Nfa.escape(new String(Nfa.specialChars));
    StringBuffer b = new StringBuffer();
    for(int i=0; i<Nfa.specialChars.length; i++) {
      b.append('\\').append(Nfa.specialChars[i]);
    }
    assertEquals(b.toString(), s);
  }
  /**********************************************************************/
  public static void test_Escape2() {
    StringBuffer in = new StringBuffer().append(Nfa.specialChars);
    StringBuffer out = new StringBuffer();
    Nfa.escape(out, in, 0);

    StringBuffer b = new StringBuffer();
    for(int i=0; i<Nfa.specialChars.length; i++) {
      b.append('\\').append(Nfa.specialChars[i]);
    }
    assertEquals(b.toString(), out.toString());
  }
  /**********************************************************************/
  public static void test_InitWithEpsilon() throws Exception {
    String s = new Nfa(Nfa.EPSILON).seq("[a-z]+", new Replace("+"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("abc0def0x0y0z0")
      ;
    assertEquals("+0+0+0+0+0", s);
  }
  /********************************************************************/
  public static void test_CallbackException1() throws Exception {
    Exception e = null;
    try {
      String s = new Nfa("a", new AbstractFaAction() {
	  public void invoke(StringBuffer out, int start, DfaRun r) 
	    throws CallbackException
	  {
	    throw new CallbackException("shit happens");
	  }
	})
	.compile(DfaRun.UNMATCHED_COPY)
	.createRun()
	.filter("a");
    } catch( CallbackException _e) {
      e = _e;
    }
    assertTrue(e instanceof CallbackException);
  }
  /********************************************************************/
  public static void test_CallbackException2() throws Exception {
    Exception e = null;
    try {
      String s = new Nfa("a", new AbstractFaAction() {
	  public void invoke(StringBuffer out, int start, DfaRun r) 
	    throws CallbackException
	  {
	    throw new CallbackException("shit happens", new Error("arg"));
	  }
	})
	.compile(DfaRun.UNMATCHED_COPY)
	.createRun()
	.filter("a");
    } catch( CallbackException _e) {
      e = _e;
    }
    assertTrue(e instanceof CallbackException);
    assertTrue(e.getCause() instanceof Error);
  }
  /**********************************************************************/
  public static void test_DfatoNfa() throws Exception {
    Dfa dfa = new 
      Nfa("a", new Replace("*"))
      .or("b", new Replace("+"))
      .compile();
    dfa = dfa.toNfa().or("c", new Replace("@"))
      .compile(DfaRun.UNMATCHED_THROW);
    String s = dfa.createRun().filter("abccba");
    assertEquals("*+@@+*", s);
  }
  /********************************************************************/
  // at one point the regexp ".*(b+).*" was not correctly compiled
  public static void test_Bug1() throws Exception {
    Dfa dfa = new Nfa(".*(b+).*", new Replace("x"))
      .compile(DfaRun.UNMATCHED_COPY);
    String s = dfa.createRun().filter("***bbd");
    // the bug I try to hunt here comes up with "xd" instead of "x"
    assertEquals("x", s);
  }
  /**********************************************************************/
  // The regexp used here threw an Exception in TableCharTrans at one
  // point. It was due to an error in Intervals.setFrom() which now
  // has its own test, but we leave the test here anyway
  public static void test_Bug2() throws Exception {
    // the next line already threw an ArrayIndexOutOfBoundsException;
    Nfa nfa = new Nfa("((ee)+.*)~", new Embed("[", "]"));
    // just count this test as done if we arrive here
    assertTrue(true);
  }
  /**********************************************************************/
  // This exploits a problem once found in dead state removal, where
  // FaToDot showed obvious dead states in the graph. The regexp was
  // "(ee)+^". Looking at the graph easily reveals that "ee" leads to
  // a dead state. 
  public static void test_Bug3() throws Exception {
    Nfa nfa = new Nfa("(ee)^", new Embed("[", "]"));


    // This CharSource will fail with an assertion on the
    // third character read. We have to wait for the third character,
    // because the 2nd is needed by the Dfa as a lookahead.
    CharSource s = new CharSource() {
	int count = 0;
	public int read() {
	  count +=1;
	  assertTrue(count<3);
	  return 'e';
	}
	public void pushBack(StringBuffer s, int i) {}
      };
    DfaRun r = 
      new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP), s);
    StringBuffer sb = new StringBuffer();
    r.read(sb);
  }    
  /**********************************************************************/
  // Try an UNMATCHED_THROW after a really long sequence of unmatched
  // characters. This exercises a certain branch in UNMATCHED_THROW
  // which contained a bug in the first implementation.
  public static void test_Bug4() throws Exception {
    Dfa dfa = new Nfa("x", new Replace("oooooooooo"))
      .compile(DfaRun.UNMATCHED_THROW);
    DfaRun r = new DfaRun(dfa);
    Exception e = null;
    try {
      r.filter("xxxx012345678901234567890123456789...xxx"); 
    } catch( NomatchException _e ) {
      e = _e;
    }
    assertTrue(e instanceof NomatchException);
  }
  /**********************************************************************/
  // trying to route through some bits of code not yet covered by any
  // of the other tests
  public static void test_TableCharTrans1() throws Exception {
    Dfa dfa = new 
      Nfa("[ab]", new Replace("1"))
      .or("[de]", new Replace("2"))
      .or("[gh]", new Replace("3"))
      .or("[jk]", new Replace("4"))
      .or("l", new Replace("5"))
      .or("m", new Replace("6"))
      .or("n", new Replace("7"))
      .or("o", new Replace("8"))
      .or("p", new Replace("9"))
      .or("q", new Replace("@"))
      .or("r", new Replace("#"))
      // we have to run this twice through compile to get into those
      // nasty bits of code in TableCharTrans.getLastAt() and ...getPos().
      .compile().toNfa().compile(DfaRun.UNMATCHED_COPY);
     
    String s = dfa.createRun()
      .filter("abcdefghijklmnopqrstuvwxyz");
    //System.out.println(">>"+s);
    assertEquals("11c22f33i4456789@#stuvwxyz", s);
  }
  /********************************************************************/
  public static void test_SaneDfa() throws Exception {
    Dfa dfa = new Nfa(".*", Copy.COPY).compile(DfaRun.UNMATCHED_COPY);
    Exception e = null;
    try {
      new DfaRun(dfa);
    } catch( IllegalArgumentException _e ) {
      e = _e;
    }
    assertTrue(e instanceof IllegalArgumentException);
  }
  /********************************************************************/
  // after some tiny but important changes to DfaRun,
  // DfaRun.read(StringBuffer) should be a useful tokenizer.
  public static void test_Toknize() throws Exception {
    Nfa nfa = new
      Nfa("[a-zA-Z]+", Copy.COPY)
      .or("[0-9:]+", Copy.COPY)
      ;
    DfaRun r = 
      nfa.compile(DfaRun.UNMATCHED_DROP)
      .createRun()
      ;
    r.setIn(new CharSequenceCharSource("  hello, it is 7:30."));
    StringBuffer sb = new StringBuffer();
    String[] toks = {"hello", "it", "is", "7:30"};
    for(int i=0; i<toks.length; i++) {
      assertTrue(r.read(sb));
      assertEquals(toks[i], sb.toString());
      sb.setLength(0);
    }
    assertFalse(r.read(sb));
    assertEquals("", sb.toString());


    // now try the very same thing with UNMATCHED_COPY and matchStart
    r = nfa.compile(DfaRun.UNMATCHED_COPY).createRun();
    r.setIn(new CharSequenceCharSource("  hello, it is 7:30."));
    for(int i=0; i<toks.length; i++) {
      assertTrue(r.read(sb));
      assertEquals(toks[i], sb.substring(r.matchStart()));
      sb.setLength(0);
    }
    assertTrue(r.read(sb));
    assertEquals(".", sb.toString());
    sb.setLength(0);
    assertFalse(r.read(sb));
    assertEquals("", sb.toString());
  }
  /**********************************************************************/
  // an innocent looking change (improvement) in DfaRun.filter()->void
  // did not take care of .collect
  private static class I {
    public int i;
    public StringBuffer value = new StringBuffer();
    public int rlen;
  }
  public static void test_filter_with_collect() throws Exception {
    final 
    Dfa dfa = new 
      Nfa("[(]", new AbstractFaAction() {
	  public void invoke(StringBuffer sb, int start, DfaRun r) {
	    I i = (I)r.clientData;
	    i.i = start;
	    r.collect = true;
	  }
	})
      .or("[)]", new AbstractFaAction() {
	  public void invoke(StringBuffer sb, int start, DfaRun r) {
	    I i = (I)r.clientData;
	    i.value.append(sb.substring(i.i));
	    r.collect = false;
	  }
	})
      .or("END", new AbstractFaAction() {
	  public void invoke(StringBuffer sb, int start, DfaRun r) {
	    I i = (I)r.clientData;
	    i.rlen = sb.length();
	  }
	})
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "bla (a) bla (b) gaga (*)0123456789END";
    I i = new I();
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    r.clientData = i;
    r.filter();
    assertEquals("(a)(b)(*)", i.value.toString());
    assertEquals(13, i.rlen);
  }
  /**********************************************************************/
  public static void test_DfaRun_natural_read() throws Exception {
    DfaRun r = new Nfa("x", new Replace("0123456789"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun();
    StringBuffer sb = new StringBuffer("bla ri lu");
    for(int i=0; i<410; i++) sb.append('x');
    sb.append("done");
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bo, true, "UTF-8");
    r.setIn(new CharSequenceCharSource(sb));
    r.filter(out);
    out.close();
    byte[] b = bo.toByteArray();
    String s = new String(b, "UTF-8");

    assertEquals(9+4100+4, s.length());
    assertTrue(s.startsWith("bla ri lu0123456789"));
    assertTrue(s.endsWith("0123456789done"));
  }
  /**********************************************************************/
  public static void test_DfaRun_maxCopy() throws Exception {
    DfaRun r = new Nfa("x", Copy.COPY)
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun();
    String s = "x0000x00000x000000x";
    r.setIn(new CharSequenceCharSource(s));
    r.maxCopy = 5;
    StringBuffer sb = new StringBuffer();
    FaAction a;

    // fetch matching x with Copy.COPY
    a = r.next(sb);
    assertEquals(Copy.COPY, a);
    assertEquals(0, r.matchStart());
    assertEquals("x", sb.toString());
    sb.setLength(0);

    // fetch 0000 and x
    a = r.next(sb);
    assertEquals(Copy.COPY, a);
    assertEquals(4, r.matchStart());
    assertEquals("0000x", sb.toString());
    sb.setLength(0);
    
    // fetch 00000 and x
    a = r.next(sb);
    assertEquals(Copy.COPY, a);
    assertEquals(5, r.matchStart());
    assertEquals("00000x", sb.toString());
    sb.setLength(0);

    // fetch 00000 only 
    a = r.next(sb);
    assertEquals(null, a);
    assertEquals(5, r.matchStart());
    assertEquals("00000", sb.toString());
    sb.setLength(0);
    
    // fetch the final 0x
    a = r.next(sb);
    assertEquals(Copy.COPY, a);
    assertEquals(1, r.matchStart());
    assertEquals("0x", sb.toString());
    sb.setLength(0);

    // fetch EOF
    a = r.next(sb);
    assertEquals(DfaRun.EOF, a);

  }
  /**********************************************************************/
  public static void test_DfaRun_maxCopy2() throws Exception {
    DfaRun r = new Nfa("x", new Embed("[", "]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun();
    r.maxCopy = 5;

    String s = r.filter("x0000x00000x000000x");
    assertEquals("[x]0000[x]00000[x]000000[x]", s);
  }
  /**********************************************************************/
  // trigger a bug in report of an exception in case yytext was
  // completely screwed up in the callback
  public static void test_CallbackException() throws Exception {
    Dfa dfa = new Nfa("xxx", new AbstractFaAction() {
	public void invoke(StringBuffer yytext, int start, DfaRun r) 
	  throws CallbackException 
	{
	  yytext.setLength(0);
	  throw new CallbackException("boom");
	}
      })
      .or("yyy", new AbstractFaAction() {
	  public void invoke(StringBuffer yytext, int start, DfaRun r) 
	    throws CallbackException 
	  {
	    throw new CallbackException("boom");
	  }
	}).compile(DfaRun.UNMATCHED_COPY);
    DfaRun r = new DfaRun(dfa);

    Exception e = null;
    try {
      r.filter("abcxxx123");
    } catch( CallbackException ee ) {
      e = ee;
    }
    assertTrue(e instanceof CallbackException);
    assertTrue(e.getMessage().indexOf("just before the match")>0);

    // The other branch, i.e. yytext not screwed up
    e = null;
    try {
      r.filter("abcyyy123");
    } catch( CallbackException ee ) {
      e = ee;
    }
    assertTrue(e instanceof CallbackException);
    assertTrue(e.getMessage().indexOf("[[yyy]]")>0);

  }
  /**********************************************************************/

  public static void main(String[] argv)   {
    // Fa fa = new Fa();
    junit.textui.TestRunner.run(new TestSuite(NfaTest.class));
  }
}

 
