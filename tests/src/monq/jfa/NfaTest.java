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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Method;

import org.junit.Test;

import monq.jfa.actions.Copy;
import monq.jfa.actions.Drop;
import monq.jfa.actions.Embed;
import monq.jfa.actions.Printf;
import monq.jfa.actions.Replace;

/**
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class NfaTest {

  @Test
  public void testSeqToNFA() throws ReSyntaxException {
    String s = "abcdedcba";
    Nfa fa = new Nfa(s, Copy.COPY);
    //assertEquals(s.length()+1,fa.size());
    AbstractFaState state = fa.getStart();
    AbstractFaState other = null;
    // the last char of a sequence is added via an epsilon, therefore
    // L-1 below.
    for(int i=0, L=s.length(); i<L-1; i++) {
      other = state.follow(s.charAt(i));
      assertNotNull(other);
      state = other;
    }
    //assertTrue(other.isStop());
  }


  @Test
  public void testDotToNFA() throws ReSyntaxException {
    String s = ".";
    Nfa fa = new Nfa(s, Copy.COPY);
    //assertEquals(2, fa.size());
    AbstractFaState state = fa.getStart();
    assertNotNull(state);

    AbstractFaState other = state.follow('\0');
    assertNotNull(other);

    for(char ch=Character.MIN_VALUE+1; ch<Character.MAX_VALUE; ch++) {
      assertSame(other, state.follow(ch));
    }
    //assertTrue(other.isStop());
  }

  @Test
  public void testRangeToNFA() throws ReSyntaxException {
    //System.out.println("testRangeToNFA");
    String[] re = {"[abc]", "[0-9]", "[]\\-]", "[a0b1c2d3e]"};
    String[] s = {"abc", "012346789", "-]", "abcde0123"};

    for(int i=0; i<re.length; i++) {
      Nfa fa = new Nfa(re[i], Copy.COPY);
      //assertEquals(2, fa.size());
      AbstractFaState start = fa.getStart();
      AbstractFaState stop = start.follow(s[i].charAt(0));
      for(int j=1; j<s[i].length(); j++) {
	AbstractFaState other = start.follow(s[i].charAt(j));
	assertSame(stop, other);
      }
      //assertTrue(stop.isStop());
    }
  }
  @Test
  public void testInvertRange() throws ReSyntaxException {
    Nfa nfa = new Nfa("[^acf]+", Drop.DROP);
    assertEquals(2, nfa.findPath("  a"));
    assertEquals(3, nfa.findPath("   c"));
    assertEquals(5, nfa.findPath("     f"));
  }

  @Test
  public void testOptional() throws ReSyntaxException {
    Nfa nfa = new Nfa("[a-bx0]?", Drop.DROP);
    assertEquals(1, nfa.findPath("a"));
    assertEquals(0, nfa.findPath(""));
    assertEquals(0, nfa.findPath("X"));
  }

  @Test
  public void testStar() throws ReSyntaxException {
    Nfa nfa = new Nfa("[ab]*", Drop.DROP);
    assertEquals(0, nfa.findPath("X"));
    assertEquals(1, nfa.findPath("a"));
    assertEquals(8, nfa.findPath("abababab"));
  }

  @Test
  public void testPlus() throws ReSyntaxException {
    Nfa nfa = new Nfa("[ab]+", Drop.DROP);
    assertEquals(-1, nfa.findPath("X"));
    assertEquals(1, nfa.findPath("a"));
    assertEquals(7, nfa.findPath("abababa"));
  }
  @Test
  public void testOr() throws ReSyntaxException {
    Nfa nfa = new Nfa("ab|rx|hallo", Drop.DROP);
    assertEquals(2, nfa.findPath("ab"));
    assertEquals(2, nfa.findPath("rx"));
    assertEquals(5, nfa.findPath("hallo"));
  }
  @Test
   public void testDoubleOr() throws ReSyntaxException {
    // try to enforce the first branch in Nfa.or()
    Nfa nfa = new Nfa("a|b*", Drop.DROP);
    assertEquals(1, nfa.findPath("a"));
    assertEquals(1, nfa.findPath("b"));
    assertEquals(3, nfa.findPath("bbbxxy"));
    assertEquals(0, nfa.findPath("y"));
  }


  @Test
  public void testSeq() throws ReSyntaxException {
    Nfa nfa = new Nfa("a[0-9](xx|yy)", Drop.DROP);
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
  @Test
  public void testSeqToStartWithEps() throws ReSyntaxException {
    // the parsing went wrong already
    Nfa nfa = new Nfa("b[a]*", Drop.DROP);

    // nevertheless a basic test
    assertEquals(1, nfa.findPath("b"));
    assertEquals(2, nfa.findPath("ba"));
    assertEquals(4, nfa.findPath("baaa"));
  }

  @Test
  public void testCompileBasic()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = new Nfa("a", Drop.DROP).compile(DfaRun.UNMATCHED_COPY);
    String s = "aaa";
    CharSource cs = new ReaderCharSource(new StringReader(s));
    StringBuilder sb = new StringBuilder();
    DfaRun r = new DfaRun(dfa);
    r.setIn(cs);

    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals(DfaRun.EOF, r.next(sb));
    assertEquals(s, sb.toString());
  }
  @Test
  public void testCompileOr()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = new Nfa("(ab)|(cd)", Drop.DROP).compile(DfaRun.UNMATCHED_COPY);
    String s =  "abcdcdab";
    CharSource cs = new ReaderCharSource(new StringReader(s));
    StringBuilder sb = new StringBuilder();
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
  @Test
  public void testCompile2Actions() throws Exception {
    Nfa nfa = new Nfa("a", Drop.DROP);
    nfa.or("b", Copy.COPY);
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    CharSource source = new CharSequenceCharSource("ab");
    DfaRun r = new DfaRun(dfa);
    r.setIn(source);
    StringBuilder sb = new StringBuilder(3);
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals("a", sb.toString());
    assertEquals(Copy.COPY, r.next(sb));
    assertEquals("ab", sb.toString());
  }

  @Test
  public void testBugInvertRange() throws Exception {
    Nfa nfa = new Nfa("a", Drop.DROP);
    nfa.or("[^a]", Copy.COPY);

    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    CharSource source = new CharSequenceCharSource("ab");
    DfaRun r = new DfaRun(dfa);
    r.setIn(source);
    StringBuilder sb = new StringBuilder(3);
    assertEquals(Drop.DROP, r.next(sb));
    assertEquals("a", sb.toString());
    assertEquals(Copy.COPY, r.next(sb));
    assertEquals("ab", sb.toString());
  }

  @Test
  public void testSameActionTwice()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    FaAction a = new Printf("xx");
    Dfa dfa =
      new Nfa("a|b", a)
      .or("b|a", a)
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "abba";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("xxxxxxxx", sb.toString());
  }

  @Test
  public void testTwoActionsPrio1()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    FaAction a = new Printf(false, "xx").setPriority(1);
    FaAction b = new Printf(false, "yy").setPriority(2);
    Dfa dfa =
      new Nfa("a|b", a)
      .or("b|a", b)
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "abba";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("yyyyyyyy", sb.toString());
  }

  // same test as before, but the priority should select the other
  // action this time.
  @Test
  public void testTwoActionsPrio2()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    //System.out.println("testTwoActionsPrio2");
    FaAction a = new Printf(false, "xx").setPriority(2);
    FaAction b = new Printf(false, "yy").setPriority(1);
    Dfa dfa =
      new Nfa("a|b", a)
      .or("b|a", b)
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "abba";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("xxxxxxxx", sb.toString());
  }

  @Test
  public void testTwoActionsClash()
    throws ReSyntaxException
  {
    FaAction a = new Printf(false, "xx").setPriority(2);
    FaAction b = new Printf(false, "yy").setPriority(2);
    Exception e = null;
    try {
      Dfa dfa =
	new Nfa("a|b", a)
	.or("b|a", b)
	.compile(DfaRun.UNMATCHED_COPY);
      assertNotNull(dfa);
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
  @Test
  public void testCompileToEmpty() throws Exception {
    Dfa dfa = new Nfa("abc").compile(DfaRun.UNMATCHED_COPY);
    Nfa nfa = dfa.toNfa();
    dfa = nfa.or("xyz", Copy.COPY).compile(DfaRun.UNMATCHED_DROP);
    DfaRun r = new DfaRun(dfa);
    String s = r.filter("abcxyzabc");
    assertEquals("xyz", s);
  }

  @Test
  public void testDfaRunWithEpsRecognizer()
      throws ReSyntaxException, CompileDfaException,
      java.io.IOException
  {
    //System.out.println("testDfaRunWithEpsRecognizer");
    FaAction a = new AbstractFaAction() {
      @Override
      public void invoke(StringBuilder out, int start, DfaRun r) {
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
    Dfa dfa = new Nfa("a*", a).compile(DfaRun.UNMATCHED_COPY);

    // we have to do tricks to get an epsmatcher into a DfaRun
    Dfa dummy = new Nfa("x").compile(DfaRun.UNMATCHED_THROW);
    DfaRun r = new DfaRun(dummy);
    r.setDfa(dfa);
    String s = r.filter("yyaayy");
    assertEquals("bbaabb", s);
  }

  @Test
  public void testNfaAddTransitions1()
    throws ReSyntaxException,  CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      new Nfa("[abc]", Drop.DROP)
      .or("[b-z]", Drop.DROP)
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "abcdefghijklmnopqrstuvwxyz";
    CharSource cs = new ReaderCharSource(new StringReader(s));
    StringBuilder sb = new StringBuilder(32);
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
  @Test
  public void testNfaAddTransitions2()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    //System.out.println("testNfaAddTransitions2");
    Dfa dfa =
      new Nfa("[b-er-v]", Drop.DROP)
      .or("[abcs-z]", Drop.DROP)
      .compile(DfaRun.UNMATCHED_COPY);
    String s = "abcderstuvwxyz";
    CharSource cs = new ReaderCharSource(new StringReader(s));
    StringBuilder sb = new StringBuilder(32);
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
  @Test
  public void testIsImportant() throws ReSyntaxException, CompileDfaException  {
    // it suffices if this does not throw
    new Nfa("a|b", Drop.DROP).compile(DfaRun.UNMATCHED_COPY);
  }

  @Test
  public void testOrWithAction()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    // it is important, that the two actions used don't end up in the
    // same DFA state, which happend in an early implementation.
    // The following should simply not throw anything.
    Dfa dfa =
      new Nfa("a", Copy.COPY)
      .or("b", Drop.DROP)
      //.or("b", new Replace(""))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "abasdfj asdfj asdfj as;ldfj asdjf ";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    while(r.read(sb)) /**/;
    assertEquals("aaaaaa", sb.toString());
  }

  @Test
  public void testUNMATCHED_COPY()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    FaAction a = new Printf(".");
    Dfa dfa = new Nfa("[a-c]+", a).compile(DfaRun.UNMATCHED_COPY);
    String s = "cccHabcabaAaaaaRbbbbAabcLaDcc";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    for(int i=0; i<7; i++) {
      assertTrue(r.read(sb));
    }
    assertFalse(r.read(sb));
    assertEquals(".H.A.R.A.L.D.", sb.toString());
  }
  @Test
  public void testUNMATCHED_DROP()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      new Nfa("[A-Z]+", Copy.COPY).compile(DfaRun.UNMATCHED_DROP);
    String s = "KaIxRaSbCcH";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    for(int i=0; i<6; i++) {
      assertTrue(r.read(sb));
    }
    assertFalse(r.read(sb));
    assertEquals("KIRSCH", sb.toString());
  }
  @Test
  public void testUNMATCHED_THROW()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      new Nfa("[A-Z]+", Copy.COPY).compile(DfaRun.UNMATCHED_THROW);
    String s = "xKaIxRaSbCcH";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    for(int i=0; i<6; i++) {
      Exception e = null;
      try {
	sb.length();
	r.read(sb);
      } catch( NomatchException _e) { e = _e; }
      assertTrue(e instanceof NomatchException );
      assertTrue(r.skip()>=0);
      assertTrue(r.read(sb));
    }
    assertEquals("KIRSCH", sb.toString());
  }
  @Test
  public void testDfaRunPiped()
    throws ReSyntaxException,  CompileDfaException,
    java.io.IOException
  {
    // The first step replaces blank-lowercase like ' a' by '# a#'
    FaAction mark = new Printf("#%0#");
    Dfa dfa1 = new Nfa(" [a-z]", mark).compile(DfaRun.UNMATCHED_COPY);

    // The 2nd step will used the marked stuff to upcase the marked
    // character
    FaAction upCase = new AbstractFaAction() {
	@Override

	public void invoke(StringBuilder out, int start, DfaRun runner) {
	  char ch = out.charAt(start+2);
	  out.setLength(start);
	  out.append(' ').append(Character.toUpperCase(ch));
	}
      };
    Dfa dfa2 = new Nfa("# .#", upCase).compile(DfaRun.UNMATCHED_COPY);

    String s = " alle meine entchen schwimmen auf dem see";

    // now connect the two automata
    DfaRun r = new DfaRun(dfa1, new CharSequenceCharSource(s));
    r = new DfaRun(dfa2, r);

    StringBuilder sb = new StringBuilder(50);
    assertTrue(r.read(sb, 100));
    assertFalse(r.read(sb));
    assertEquals(" Alle Meine Entchen Schwimmen Auf Dem See",
		 sb.toString());
  }
  @Test
  public void testReaderPushBack()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      new Nfa("[a-z]+(0000)?", new Printf(false, "FULL"))
      .or("[a-z]+", new Printf(false, "PART").setPriority(1))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "abc000def0000xyz0r0000";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    for(int i=0; i<4; i++) {
      assertTrue(r.read(sb));
    }
    assertFalse(r.read(sb));
    assertEquals("PARTFULLPARTFULL", sb.toString());
  }
  @Test
  public void testSeqString()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      new Nfa("abc|def", null)
      .seq("xxx", new Printf("1"))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "abcxxxdefxxx";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    assertTrue(r.read(sb));
    assertTrue(r.read(sb));
    assertFalse(r.read(sb));
    assertEquals("11", sb.toString());
  }
  @Test
  public void testOrString()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      new Nfa("abc|def", null)
      .or("xxx")
      .addAction(new Printf("1"))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "abcxxxdefxxx";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
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
  @Test
  public void testStartCanBeStop()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dummy = new Nfa("x", Copy.COPY).compile(DfaRun.UNMATCHED_COPY);

    Dfa dfa = new Nfa("a*", Copy.COPY).compile(DfaRun.UNMATCHED_COPY);
    StringBuilder sb = new StringBuilder();
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
  @Test
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
      new Nfa("a", new Printf("b"))
      .or("(a*x)!", Drop.DROP)
      .compile(DfaRun.UNMATCHED_THROW);
    StringBuilder in = new StringBuilder(L);
    StringBuilder out = new StringBuilder(L);
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
  @Test
  public void testShortestAllowEpsilonInStartState() throws ReSyntaxException {
    // basically the following should not throw an exception
    Nfa nfa = new Nfa("(a+)!|b", Drop.DROP);

    // Nevertheless we check some basic functionality, in particular
    // the shortest operator put this thing into a "a|b" machine.
    assertEquals(1, nfa.findPath("aa"));
    assertEquals(1, nfa.findPath("bb"));
    assertEquals(-1, nfa.findPath("x"));
  }

  // try shortest *after* an action was assigned
  @Test
  public void testShortestAfterActionAssigned()
    throws ReSyntaxException, CompileDfaException
  {
    Nfa nfa = new Nfa("[abc]+", Drop.DROP).shortest();
    assertEquals(-1, nfa.findPath("xx"));
    assertEquals(1, nfa.findPath("a"));
    assertEquals(1, nfa.findPath("bx"));
    assertEquals(1, nfa.findPath("cc"));
  }

  // force some coverage in Nfa.hasAction(Set)
  @Test
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
  @Test
  public void testNfaConstructorTwoArgs()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa =
      new Nfa("[a-z][a-z][0-9]", new Printf("x"))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "......ab1....xy2....rr4....rr...ab5";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    while(r.read(sb)) /**/;
    assertEquals("xxxx", sb.toString());
  }
  @Test
  public void testNfaSeqWithoutAction()
    throws ReSyntaxException, CompileDfaException,
    java.io.IOException
  {
    Dfa dfa = new Nfa(Nfa.EPSILON).seq("[a-z][a-z]").seq("[0-9]")
      .addAction(new Printf("x"))
      .compile(DfaRun.UNMATCHED_DROP);
    String s = "......ab1....xy2....rr4....rr...ab5";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    StringBuilder sb = new StringBuilder();
    while(r.read(sb)) /**/;
    assertEquals("xxxx", sb.toString());
  }

  /**
   * tries to evoke a bug where
   * <code>new Nfa("a", Drop.DROP).or("r")</code>
   * allows to reach the stop state via 'r'
   */
  @Test
  public void testSymmetricOr() throws ReSyntaxException {
    Nfa nfa = new Nfa("a", Drop.DROP).or("r");
    assertEquals(1, nfa.findPath("a"));
    assertEquals(-1, nfa.findPath("r"));

    nfa = new Nfa(Nfa.NOTHING).or("a").or("r", Drop.DROP);
    assertEquals(-1, nfa.findPath("a"));
    assertEquals(1, nfa.findPath("r"));
  }

  @Test
  public void testInvert1() throws ReSyntaxException {
    Nfa nfa = new Nfa("a~", Drop.DROP);
    assertEquals(0, nfa.findPath(""));
    assertEquals(1, nfa.findPath("b"));
    assertEquals(0, nfa.findPath("a"));
    assertEquals(2, nfa.findPath("ab"));
  }
  @Test
  public void testInvert2() throws ReSyntaxException {
    Nfa nfa = new Nfa("x(...)~", Drop.DROP);
    assertEquals(-1, nfa.findPath("r"));
    assertEquals(1, nfa.findPath("x"));
    assertEquals(2, nfa.findPath("xa"));
    assertEquals(3, nfa.findPath("xab"));
    assertEquals(3, nfa.findPath("xabc"));
    assertEquals(5, nfa.findPath("xabcd"));
  }
  @Test
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
  @Test
  public void testInvert4() throws ReSyntaxException {
    Nfa nfa = new Nfa("((a.*)?)~", Drop.DROP);
    assertEquals(-1, nfa.findPath("a"));
    assertEquals(-1, nfa.findPath("aa"));
    assertEquals(-1, nfa.findPath("aab"));
    assertEquals(2, nfa.findPath("ba"));
  }
  // try inverting an automaton AFTER adding an action.
  @Test
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
  @Test
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
      @Override
      public int read() {
        count +=1;
        assertTrue(count<3);
        return 'a';
      }
      @Override
      public void pushBack(StringBuilder unused, int i) {}
    };
    DfaRun r =
        new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP), s);
    StringBuilder sb = new StringBuilder();
    r.read(sb);
  }

  /**
   * the ChildIterator of a state is a bit tricky because it has to
   * iterate over two other iterators.
   */
  @Test
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
  @Test
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
    StringBuilder sb = new StringBuilder();
    while( r.read(sb) ) ;
    assertEquals("Harald%Kirsch%hat%serf%nden", sb.toString());
  }

  /**
   * this threw an exception in a buggy pre-version where the single
   * char read did not expect that a call to read() may result in
   * nothing.
   */
  @Test
  public void test_readCharWithDroppingAction()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    Dfa dfa = new Nfa("a+", Drop.DROP).compile(DfaRun.UNMATCHED_COPY);
    String s = "aaabbbb";
    DfaRun r = new DfaRun(dfa, new CharSequenceCharSource(s));
    assertEquals('b', r.read());
  }

  @Test
  public void test_unskip()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    FaAction a = new AbstractFaAction() {
	@Override
  public void invoke(StringBuilder out, int start, DfaRun runner) {
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

  @Test
  public void testPriorityWithDrop()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    Nfa nfa =
      new Nfa("[ab]+", new Printf("(%0)"))
      .or("[ab]", new Drop(1))
      ;
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY),
			  new CharSequenceCharSource("xxaxxbxxabxxba"));
    StringBuilder sb = new StringBuilder();
    while( r.read(sb) ) /*just run*/;
    assertEquals("xxxxxx(ab)xx(ba)", sb.toString());
  }

  // for completeness, do a dummy test of toDot
  @Test
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
  @Test
  public void testDfaRun1()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    FaAction switchToDrop = new AbstractFaAction() {
      @Override
      public void invoke(StringBuilder out, int start, DfaRun r) {
        r.setOnFailedMatch(DfaRun.UNMATCHED_DROP);
      }
    };

    FaAction unskipXX = new AbstractFaAction() {
      @Override
      public void invoke(StringBuilder out, int start, DfaRun r) {
        out.setLength(start);
        r.unskip("XX");
      }
    };

    // use the filter version which copies to StringBuilder to have
    // this tested as well somewhere
    StringBuilder sb = new StringBuilder();
    Nfa nfa = new Nfa("aa", unskipXX).or("XX", switchToDrop);
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY));
    r.setIn(new CharSequenceCharSource("1234aaxxxx"));
    r.filter(sb);
    //System.out.println("\n>>"+s+"<<");
    assertEquals("1234XX", sb.toString());
  }


  // test the collect-behaviour of a DfaRun
  @Test
  public void testDfaRun2()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    FaAction switchToCollect = new AbstractFaAction() {
	@Override
  public void invoke(StringBuilder out, int start, DfaRun r) {
	  if( r.collect ) return; // go for longest match
	  r.collect = true;
	  r.clientData = new Integer(start);
	}
      };

    FaAction dropCollected = new AbstractFaAction() {
	@Override
  public void invoke(StringBuilder out, int start, DfaRun r) {
	  int startPos = ((Integer)r.clientData).intValue();
	  out.setLength(startPos);
	  r.collect = false;
	  //System.out.println(""+r);
	}
      };
    FaAction release = new AbstractFaAction() {
	@Override
  public void invoke(StringBuilder out, int start, DfaRun r) {
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
    StringBuilder result = new StringBuilder();
    StringBuilder scratch = new StringBuilder();
    while( run.read(scratch) ) {
      result.append(scratch);
      scratch.setLength(0);
    }
    //System.out.println("\n>>"+result+"<<");
    assertEquals("......a..[b]..xxx", result.toString());
  }

  // check that an EOF in collect throws
  @Test
  public void testDfaRun3()
    throws ReSyntaxException, CompileDfaException
  {
    FaAction switchToCollect = new AbstractFaAction() {
	@Override
  public void invoke(StringBuilder out, int start, DfaRun r) {
	  r.collect = true;
	}
      };
    Exception e = null;
    Nfa nfa =
      new Nfa("a", switchToCollect)
      ;
    DfaRun run = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY),
			    new CharSequenceCharSource("aaa"));
    StringBuilder result = new StringBuilder();
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
  @Test
  public void testDfaRun4()
    throws ReSyntaxException, CompileDfaException
  {
    Dfa dfa = new Nfa("a", Copy.COPY).compile(DfaRun.UNMATCHED_COPY);
    DfaRun run = new DfaRun(dfa);
    assertEquals(dfa, run.getDfa());
  }

  // check that overreading with DfaRun.read(buf,count) works
  @Test
  public void testDfaRun5()
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    Nfa nfa = new Nfa("a+", Copy.COPY);
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP),
			  new CharSequenceCharSource("aaaaab"));
    StringBuilder sb = new StringBuilder();
    int l = 0;
    while( r.read(sb, 1) ) {
      assertEquals(1, sb.length()-l);
      l+=1;
    }
    assertEquals("aaaaa", sb.toString());
  }

  // check that overreading with DfaRun.read() works
  @Test
  public void testDfaRun6()
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    Nfa nfa = new Nfa("a+", Copy.COPY);
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP),
			  new CharSequenceCharSource("ababab"));
    StringBuilder sb = new StringBuilder();
    int ch;
    while( -1!=(ch=r.read()) ) {sb.append((char)ch);}
    assertEquals("aaa", sb.toString());
  }
  @Test
  public void testDfaRun_Eofaction() throws Exception {
    DfaRun r = new
      Nfa("[0-9]+", new Replace("x"))
      .compile(DfaRun.UNMATCHED_COPY, new Replace("</EOF>"))
      .createRun()
      ;
    String s = r.filter("bla99bla");
    assertEquals("blaxbla</EOF>", s);
  }

  @Test
  public void testDfaRun_filter1() throws Exception {
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

  @Test
  public void testDfaRun_filter2() throws Exception {
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

  @Test
  public void testDfaRun_Clash1() throws Exception {
    Nfa nfa = new
      Nfa("<[a-z]+>", new Drop(0))
      .or("<[rst]>", new Copy(0))
      ;
    Exception e = null;
    try {
      nfa.compile(DfaRun.UNMATCHED_COPY);
    } catch( CompileDfaException _e ) {
      e = _e;
    }
    assertTrue(e instanceof CompileDfaException );
    assertTrue(e.getMessage().indexOf("<[r-t]>")>0);
  }

  // A certain branch in Nfa.findAction is only executed if the clash
  // happens on the empty string. This tries to exercise it.
  @Test
  public void testDfaRun_Clash2() throws Exception {
    Nfa nfa = new
      Nfa("[a-z]*", new Drop(0))
      .or("[rst]*", new Copy(0))
      ;
    Exception e = null;
    try {
      nfa.compile(DfaRun.UNMATCHED_COPY);
    } catch( CompileDfaException _e ) {
      e = _e;
    }
    assertTrue(e instanceof CompileDfaException );
    //System.out.println(e.getMessage());
    assertTrue(e.getMessage().indexOf("path `':")>0);
  }

  @Test
  public void test_or_withOther()
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

    // the start state of other should lead nowwhere now because the
    // .or() should have initialized it
    AbstractFaState state = other.getStart();
    for(char ch=Character.MIN_VALUE; ch<Character.MAX_VALUE; ch++) {
      assertNull(state.follow(ch));
    }
  }

  @Test
  public void test_seq_withOther()
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

    // the start state of other should lead nowwhere now because the
    // .seq() should have initialized it
    AbstractFaState state = other.getStart();
    for(char ch=Character.MIN_VALUE; ch<Character.MAX_VALUE; ch++) {
      assertNull(state.follow(ch));
    }
  }

  // Exercise Dfa connected to InputStream
  @Test
  public void test_Dfa_withInputStream()
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    java.io.ByteArrayInputStream in
      = new java.io.ByteArrayInputStream("a b c".getBytes());
    Dfa dfa = new Nfa("a|b", new Printf("[%0]"))
      .compile(DfaRun.UNMATCHED_COPY);
    DfaRun r = new DfaRun(dfa, new ReaderCharSource(in));
    StringBuilder sb = new StringBuilder();
    while( r.read(sb) ) /**/;
    assertEquals("[a] [b] c", sb.toString());
  }

  @Test
  public void test_not()
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
  @Test
  public void test_SetPrio() throws Exception {
    String s = new
      Nfa("[a-z]+", new Embed("[", "]"))
      .or("[rst]+[0-9]*", new Embed("<", ">").setPriority(-1))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("abc-rrr111");
    assertEquals("[abc]-<rrr111>", s);
  }
  /**********************************************************************/
  // make an effort to ensure that the escaped special characters
  // match only the string of special characters
  @Test
  public void test_Escape1() {
    ReParserFactory rpfs[] = {ReClassicParser.factory};
    for(ReParserFactory rpf : rpfs ) {
      Nfa.setDefaultParserFactory(rpf);
      Nfa nfa = new Nfa();
      String spch = nfa.specialChars();
      Regexp re = new Regexp(nfa.escape(spch));
      StringBuilder sb = new StringBuilder(spch);
      assertEquals(0, re.find(sb, 0));
      assertEquals(spch.length(), re.length());

      for(int i=0; i<spch.length(); i++) {
	sb.setCharAt(i, 'X');
	assertEquals(-1, re.find(sb, 0));
      }
    }
  }
  /**********************************************************************/
//   @Test
//   public static void test_Escape2() {
//     StringBuilder in = new StringBuilder().append(Nfa.specialChars);
//     StringBuilder out = new StringBuilder();
//     Nfa.escape(out, in, 0);

//     StringBuilder b = new StringBuilder();
//     for(int i=0; i<Nfa.specialChars.length; i++) {
//       b.append('\\').append(Nfa.specialChars[i]);
//     }
//     assertEquals(b.toString(), out.toString());
//   }
  /**********************************************************************/
  @Test
  public void test_InitWithEpsilon() throws Exception {
    String s = new Nfa(Nfa.EPSILON).seq("[a-z]+", new Replace("+"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("abc0def0x0y0z0")
      ;
    assertEquals("+0+0+0+0+0", s);
  }
  /********************************************************************/
  @Test
  public void test_CallbackException1() throws Exception {
    Exception e = null;
    try {
      new Nfa("a", new AbstractFaAction() {
	  @Override
    public void invoke(StringBuilder out, int start, DfaRun r)
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
  @Test
  public void test_CallbackException2() throws Exception {
    Exception e = null;
    try {
      new Nfa("a", new AbstractFaAction() {
	  @Override
    public void invoke(StringBuilder out, int start, DfaRun r)
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
  @Test
  public void test_DfatoNfa() throws Exception {
    Dfa dfa = new
      Nfa("a", new Replace("*"))
      .or("b", new Replace("+"))
      .compile(DfaRun.UNMATCHED_COPY);
    dfa = dfa.toNfa().or("c", new Replace("@"))
      .compile(DfaRun.UNMATCHED_THROW);
    String s = dfa.createRun().filter("abccba");
    assertEquals("*+@@+*", s);
  }
  /********************************************************************/
  // at one point the regexp ".*(b+).*" was not correctly compiled
  @Test
  public void test_Bug1() throws Exception {
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
  @Test
  public void test_Bug2() throws Exception {
    Nfa nfa = new Nfa("((ee)+.*)~", new Embed("[", "]"));

    nfa.addAction(null); // prevent compiler warning about unused allocation

    // just count this test as done if we arrive here
    assertTrue(true);
  }
  /**********************************************************************/
  // This exploits a problem once found in dead state removal, where
  // FaToDot showed obvious dead states in the graph. The regexp was
  // "(ee)+^". Looking at the graph easily reveals that "ee" leads to
  // a dead state.
  @Test
  public void test_Bug3() throws Exception {
    Nfa nfa = new Nfa("(ee)^", new Embed("[", "]"));


    // This CharSource will fail with an assertion on the
    // third character read. We have to wait for the third character,
    // because the 2nd is needed by the Dfa as a lookahead.
    CharSource s = new CharSource() {
      int count = 0;
      @Override
      public int read() {
        count +=1;
        assertTrue(count<3);
        return 'e';
      }
      @Override
      public void pushBack(StringBuilder unused, int i) {}
    };
    DfaRun r =
        new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP), s);
    StringBuilder sb = new StringBuilder();
    r.read(sb);
  }
  /**********************************************************************/
  // Try an UNMATCHED_THROW after a really long sequence of unmatched
  // characters. This exercises a certain branch in UNMATCHED_THROW
  // which contained a bug in the first implementation.
  @Test
  public void test_Bug4() throws Exception {
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
  @Test
  public void test_TableCharTrans1() throws Exception {
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
      .compile(DfaRun.UNMATCHED_COPY)
      .toNfa().compile(DfaRun.UNMATCHED_COPY);

    String s = dfa.createRun()
      .filter("abcdefghijklmnopqrstuvwxyz");
    //System.out.println(">>"+s);
    assertEquals("11c22f33i4456789@#stuvwxyz", s);
  }
  /********************************************************************/
  @Test
  public void test_SaneDfa() throws Exception {
    Dfa dfa = new Nfa(".*", Copy.COPY).compile(DfaRun.UNMATCHED_COPY);

    try {
      DfaRun r = new DfaRun(dfa);
      fail("expected IllegalArgumentException when creating "+r);
    } catch( Throwable e ) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }
  /********************************************************************/
  // after some tiny but important changes to DfaRun,
  // DfaRun.read(StringBuilder) should be a useful tokenizer.
  @Test
  public void test_Toknize() throws Exception {
    Nfa nfa = new
      Nfa("[a-zA-Z]+", Copy.COPY)
      .or("[0-9:]+", Copy.COPY)
      ;
    DfaRun r =
      nfa.compile(DfaRun.UNMATCHED_DROP)
      .createRun()
      ;
    r.setIn(new CharSequenceCharSource("  hello, it is 7:30."));
    StringBuilder sb = new StringBuilder();
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
    public StringBuilder value = new StringBuilder();
    public int rlen;
  }

  @Test
  public void test_filter_with_collect() throws Exception {
    final
    Dfa dfa = new
      Nfa("[(]", new AbstractFaAction() {
	  @Override
    public void invoke(StringBuilder sb, int start, DfaRun r) {
	    I i = (I)r.clientData;
	    i.i = start;
	    r.collect = true;
	  }
	})
      .or("[)]", new AbstractFaAction() {
	  @Override
    public void invoke(StringBuilder sb, int start, DfaRun r) {
	    I i = (I)r.clientData;
	    i.value.append(sb.substring(i.i));
	    r.collect = false;
	  }
	})
      .or("END", new AbstractFaAction() {
	  @Override
    public void invoke(StringBuilder sb, int start, DfaRun r) {
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
  @Test
  public void test_DfaRun_natural_read() throws Exception {
    DfaRun r = new Nfa("x", new Replace("0123456789"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun();
    StringBuilder sb = new StringBuilder("bla ri lu");
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
  @Test
  public void test_DfaRun_maxCopy() throws Exception {
    DfaRun r = new Nfa("x", Copy.COPY)
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun();
    String s = "x0000x00000x000000x";
    r.setIn(new CharSequenceCharSource(s));
    r.maxCopy = 5;
    StringBuilder sb = new StringBuilder();
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
  @Test
  public void test_DfaRun_maxCopy2() throws Exception {
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
  @Test
  public void test_CallbackException() throws Exception {
    Dfa dfa = new Nfa("xxx", new AbstractFaAction() {
      @Override
      public void invoke(StringBuilder yytext, int start, DfaRun r)
	  throws CallbackException
	{
	  yytext.setLength(0);
	  throw new CallbackException("boom");
	}
      })
      .or("yyy", new AbstractFaAction() {
	  @Override
    public void invoke(StringBuilder yytext, int start, DfaRun r)
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
  /*+******************************************************************/

  @Test
  public void test_LongInvertBombsStack() throws Exception {
    final int NCHARS = 10_000;
    StringBuilder sb = new StringBuilder(NCHARS+10);
    sb.append('(');
    for (int i=0; i<NCHARS; i++) {
      sb.append('c');
    }
    sb.append(")~X");
    Nfa nfa = new Nfa(sb, new Copy(0));
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);

    sb.delete(0, 1);
    sb.setLength(NCHARS);
    sb.append('X');
    CharSource text = new CharSequenceCharSource(sb);
    assertNull(dfa.match(text, new StringBuilder(), (TextStore)null));

    sb.delete(0, 1);
    text = new CharSequenceCharSource(sb);
    assertNotNull(dfa.match(text, new StringBuilder(), (TextStore)null));

    text = new CharSequenceCharSource("blacccblaX");
    assertNotNull(dfa.match(text, new StringBuilder(), (TextStore)null));

  }

  @Test
  public void test_deleteUseless() throws Exception {
    AbstractFaState[] children = new AbstractFaState[3];
    AbstractFaState start = new AbstractFaState();
    start.setEps(children);
    AbstractFaState stop = new AbstractFaState(new Copy(0));
    AbstractFaState useless = new AbstractFaState();
    AbstractFaState useless2 = new AbstractFaState();
    useless.setEps(new AbstractFaState[]{useless2});
    useless2.setEps(new AbstractFaState[]{useless});
    AbstractFaState loop = new AbstractFaState();
    loop.setEps(new AbstractFaState[]{start});

    children[0] = useless;
    children[1] = loop;
    children[2] = stop;
    Nfa nfa = new Nfa(start, stop);

    Method deleteUseless = Nfa.class.getDeclaredMethod("removeUseless");

    deleteUseless.setAccessible(true);
    deleteUseless.invoke(nfa);
    assertEquals(2, start.getEps().length);

  }

  @Test
  public void allPrefixes() throws Exception {
    Nfa nfa = new Nfa("(abc1+|abXY)@", Drop.DROP);

    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);

    StringBuilder out = new StringBuilder();
    for (String text : new String[]{"a", "ab", "abc", "abc1", "abc11", "abX"}) {
      FaAction a =
          dfa.match(new CharSequenceCharSource(text), out, (TextStore)null);
      assertEquals(Drop.DROP, a);
    }

    for (String text : new String[]{"x", "rst"}) {
      FaAction a =
          dfa.match(new CharSequenceCharSource(text), out, (TextStore)null);
      assertNull(a);
    }
  }

  @Test
  public void bug_implementationNot() throws Exception {
    // a bug in the implementation of not(). Internally it uses a specific
    // action to mark stop states, but this clashed with normal use.
    Nfa nfa = new Nfa("rst", Copy.COPY);
    nfa.not();
    nfa.addAction(Drop.DROP);
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    StringBuilder out = new StringBuilder();
    TextStore t = null;
    FaAction a = dfa.match(new CharSequenceCharSource("haselda"), out, t);
    assertEquals(a, Drop.DROP);
    assertEquals("haselda", out.toString());

    out.setLength(0);
    a = dfa.match(new CharSequenceCharSource("rst"), out, t);
    assertEquals(Drop.DROP, a);
    assertEquals("rs", out.toString());
  }
  /*+******************************************************************/
  @Test
  public void test_completeToSkip() throws Exception {
    tryCompleter("[^/]*XX", "XdadaXX--/---XX...X", "[XdadaXX]_[---XX]...X");
    tryCompleter("b", "b b1234b...bb", "[b]_[b]_[b]_[b][b]");
    tryCompleter("[a-z]+", "max123braz...", "[max]_[braz]_");
    tryCompleter("de.*", ".....deZZZ", "_[deZZZ]");
    tryCompleter(".*aaa", "...a...aa...aaa...", "[...a...aa...aaa]...");
    tryCompleter("123abc", "123123abc", "_[123abc]");
  }

  private static void
  tryCompleter(String re, String in, String out) throws Exception
  {
    FaAction repB = new Replace("_");
    //repB = new Printf("((%0))");
    Nfa nfa = new Nfa(re, new Printf("[%0]"));
    nfa.completeToSkip(repB);
    nfa.toDot("/home/harald/tmp/bla.dot");

    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    dfa.toDot("/home/harald/tmp/bli.dot");

    DfaRun r = new DfaRun(new Nfa("a").compile(DfaRun.UNMATCHED_COPY));
    r.setDfa(dfa);
    String result = r.filter(in);
    assertEquals(out, result);
  }
  @Test
  public void ggg() throws Exception {
    Nfa nfa = new Nfa(".*bcd", new Replace("X"));
    nfa.allPrefixes();
    //nfa.not();
    nfa.addAction(Copy.COPY);
    
//    Nfa other = new Nfa(".*").seq(nfa.copy()).seq(".*").invert();
//    other.addAction(Copy.COPY);
//    nfa.or(other);
//    nfa.allPrefixes();
//    nfa.not();
//    nfa.addAction(Copy.COPY);
    //nfa.completeToSkip(Copy.COPY);
    nfa.toDot("/home/harald/tmp/bla.dot");
    nfa.compile(DfaRun.UNMATCHED_COPY).toDot("/home/harald/tmp/bli.dot");
  }
}


