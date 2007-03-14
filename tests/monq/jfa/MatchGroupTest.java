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
 * @author &copy; 2004,2005 Harald Kirsch
 */
public class MatchGroupTest extends TestCase {

//   public void test_Basic1() 
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     String s = new 
//       Nfa("a(b+)@1@c", new Printf(true, "%0-%1-", 0))
//       .compile()
//       .createRun(DfaRun.UNMATCHED_COPY)
//       .filter("abc abbbbbc");
//     assertEquals("abc-b- abbbbbc-bbbbb-", s);
//   }

  public void test_Basic1a() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    String s = new 
      Nfa("a(!b+)c", new Printf(true, "%0-%1-", 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("abc abbbbbc");
    assertEquals("abc-b- abbbbbc-bbbbb-", s);
  }
  /**********************************************************************/

  // demonstrate how match groups may overlap. Despite the overlap,
  // these two groups are uniquely separable.
//   public void test_Overlap1() 
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     String s = new 
//       Nfa("x(ab*)@1@(abc)@2@X", 
// 	  new Printf(true, "%0, [%1], [%2]", 0))
//       .compile()
//       .createRun(DfaRun.UNMATCHED_COPY)
//       .filter("xaabcX|xababcX|xabbbabcX");
//     //System.out.println(s);
//     assertEquals("xaabcX, [a], [abc]|xababcX, [ab], [abc]|xabbbabcX, [abbb], [abc]", s);
//   }
  public void test_Overlap1a() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    String s = new 
      Nfa("x(!ab*)(!abc)X", 
	  new Printf(true, "%0, [%1], [%2]", 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("xaabcX|xababcX|xabbbabcX");
    //System.out.println(s);
    assertEquals("xaabcX, [a], [abc]|xababcX, [ab], [abc]|xabbbabcX, [abbb], [abc]", s);
  }
  
  /**********************************************************************/
  // demonstrate how match groups may overlap. These two groups cannot
  // be separated. The reason is that (ab)* becomes a subgraph of
  // (abc) in the DFA, mainly because (ab)* matches the empty
  // string. In particular the loop moves into the common part of the
  // DFA and thereby allows the 2nd group to always contain the
  // first as its prefix.
//   public void test_Overlap2() 
//   throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     String s = new 
//       Nfa("x((ab)*)@1@(abc)@4@X", new Printf(true, "%0, [%1], [%2]", 9))
//       .compile()
//       .createRun(DfaRun.UNMATCHED_COPY)
//       .filter("xabcX|xababcX");
//     //System.out.println(s);
//     assertEquals("xabcX, [ab], [abc]|xababcX, [abab], [ababc]", s);
//   }
  public void test_Overlap2a() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    String s = new 
      Nfa("x(!(ab)*)(!abc)X", new Printf(true, "%0, [%1], [%2]", 9))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("xabcX|xababcX");
    //System.out.println(s);
    assertEquals("xabcX, [ab], [abc]|xababcX, [abab], [ababc]", s);
  }

  /**********************************************************************/
  // in contrast to above, we don't allow the empty string to match
  // the first group. This allows again to properly separate the
  // groups 
//   public void test_Overlap3() 
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     String s = new 
//       Nfa("x((ab)+)@2@(abc)@11@X", 
// 	  new Printf(true, "%0, [%1], [%2]", 0))
//       .compile()
//       .createRun(DfaRun.UNMATCHED_COPY)
//       .filter("xabcX|xababcX");
//     //System.out.println(s);
//     assertEquals("xabcX|xababcX, [abab], [abc]", s);
//   }
  public void test_Overlap3a() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    String s = new 
      Nfa("x(!(ab)+)(!abc)X", 
	  new Printf(true, "%0, [%1], [%2]", 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("xabcX|xababcX");
    //System.out.println(s);
    assertEquals("xabcX|xababcX, [abab], [abc]", s);
  }

  /**********************************************************************/
  // the two groups in this example perfectly merge into an automaton
  // with a linear list of states. Because the subautomaton
  // information is merged by set-union, every match always run
  // through both subgroups in full.
//   public void test_Or1() 
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     String s = new
//       Nfa("x(ab)@1@cR|x(abc)@3@S", 
// 	  new Printf(true, "%0 [%1] [%2]", 0))
//       .compile()
//       .createRun(DfaRun.UNMATCHED_COPY)
//       .filter("xabcR|xabcS");
//     //System.out.println(s);
//     assertEquals("xabcR [ab] [abc]|xabcS [ab] [abc]", s);
//   }
  public void test_Or1a() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    String s = new
      Nfa("x(!ab)cR|x(!abc)S", 
	  new Printf(true, "%0 [%1] [%2]", 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("xabcR|xabcS");
    //System.out.println(s);
    assertEquals("xabcR [ab] [abc]|xabcS [ab] [abc]", s);
  }

  /**********************************************************************/
  // see if we can separate the subgroups by means of seperate
  // explicit stop states
//   public void test_Or2() 
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
    
//     // Not that we need two incarnations of Printf because otherwise
//     // we will exactly not have the possibility to separate the two
//     // cases 
//     Dfa dfa = new
//       Nfa("x(ab)@1@cR", new Printf(true, "%0 [%1]", 0))
//       .or("x(abc)@1@S", new Printf(true, "%0 [%1]", 0))
//       .compile();
//     //dfa.toDot(new java.io.PrintStream(new java.io.FileOutputStream("bla.txt")));

//     String s = dfa
//       .createRun(DfaRun.UNMATCHED_COPY)
//       .filter("xabcR|xabcS");
//     //System.out.println(s);
//     assertEquals("xabcR [ab]|xabcS [abc]", s);
//   }
  public void test_Or2a()
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    
    // Not that we need two incarnations of Printf because otherwise
    // we will exactly not have the possibility to separate the two
    // cases 
    Dfa dfa = new
      Nfa("x(!ab)cR", new Printf(true, "%0 [%1]", 0))
      .or("x(!abc)S", new Printf(true, "%0 [%1]", 0))
      .compile(DfaRun.UNMATCHED_COPY);
    //dfa.toDot(new java.io.PrintStream(new java.io.FileOutputStream("bla.txt")));

    String s = dfa
      .createRun()
      .filter("xabcR|xabcS");
    //System.out.println(s);
    assertEquals("xabcR [ab]|xabcS [abc]", s);
  }
  /**********************************************************************/

  // exhibit a bug in or() where (c=1x)+@c=2x gets the start state
  // of the subgroup deleted 
//   public void test_Or3() 
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     Nfa nfa = new Nfa(Nfa.NOTHING);
//     nfa.or("(cat=1b)+@3@cat=2b(WRONG)@44@", new Printf(true, "%1", 0));

//     Dfa dfa = nfa.compile();
//     String s;
//     s = dfa.createRun(DfaRun.UNMATCHED_DROP).filter("..cat=1bcat=2bWRONG..");
//     assertEquals("cat=1b", s);
//   }
  public void test_Or3a() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    nfa.or("(!(cat=1b)+)cat=2b(!WRONG)", new Printf(true, "%1", 0));

    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    String s;
    s = dfa.createRun().filter("..cat=1bcat=2bWRONG..");
    assertEquals("cat=1b", s);
  }
  /**********************************************************************/

  // exhibit problems with early implementations which picked up too
  // much to easily
//   public void test_Challenge1()  
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     Nfa nfa = new Nfa(Nfa.NOTHING);
//     nfa.or("([a-z]+)@1@ *([0-9]*)@2@ *([a-z]+)@3@", 
// 	   new Printf(true, "[%1] [%2] [%3]", 0));
//     Dfa dfa = nfa.compile();
//     String s;
//     s = dfa.createRun(DfaRun.UNMATCHED_DROP).filter("a 99 c");
//     assertEquals("[a] [99] [c]", s);
//   }
  public void test_Challenge1a()  
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    nfa.or("(!([a-z]+)) *(!([0-9]*)) *(!([a-z]+))", 
	   new Printf(true, "[%1] [%2] [%3]", 0));
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    String s;
    s = dfa.createRun().filter("a 99 c");
    assertEquals("[a] [99] [c]", s);
  }

  /**********************************************************************/
//   public void test_Challenge2()  
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     Nfa nfa = new Nfa(Nfa.NOTHING);
//     nfa.or("(([a-z]*)@1@abc)!x", new Printf(true, "[%1]", 0));
//     Dfa dfa = nfa.compile();
//     String s;
//     s = dfa.createRun(DfaRun.UNMATCHED_DROP).filter("#zzzabcx--");
//     assertEquals("[zzzabc]", s);
//   }

  public void test_Challenge2a()  
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    nfa.or("((!([a-z]*))abc)!x", new Printf(true, "[%1]", 0));
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    String s;
    s = dfa.createRun().filter("#zzzabcx--");
    assertEquals("[zzzabc]", s);
  }

  /**********************************************************************/
  // exhibit a serious bug where the code ran into a throw new
  // Error("screwed") in Dfa.java
//   public void test_Screwed1()  
//     throws ReSyntaxException, CompileDfaException, java.io.IOException {
//     Nfa nfa = new Nfa("[XY](A|M)@2@", new Printf(true, "[%1]", 0));
//     DfaRun r = new DfaRun(nfa.compile(), DfaRun.UNMATCHED_DROP, "");
//     // the buggy code will throw an Error here
//     String s = r.filter("XA");
//     assertEquals("[A]", s);
//   }

  public void test_Screwed1a()
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    Nfa nfa = new Nfa("[XY](!A|M)", new Printf(true, "[%1]", 0));
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP));
    // the buggy code will throw an Error here
    String s = r.filter("XA");
    assertEquals("[A]", s);
  }
  /********************************************************************/
  public static void test_Oflow() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException {
    StringBuffer s = new StringBuffer();
    for(int i=0; i<256; i++) s.append("(!a)x");
    Exception e = null;
    try {
      Nfa nfa = new Nfa(s, Copy.COPY);
    } catch( ReSyntaxException _e ) {
      // we want this to happen
      e = _e;
    }
    assertTrue(e instanceof ReSyntaxException);
    assertTrue(e.toString().startsWith("ReSyntaxException: too many"));
  }
  /**********************************************************************/
  public static void main(String[] argv)   {
    junit.textui.TestRunner.run(new TestSuite(MatchGroupTest.class));
  }
}

 
