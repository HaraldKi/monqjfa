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

import java.io.StringReader;
import java.lang.Class;
import java.util.HashMap;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map;
/**
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class PrintfFormatterTest extends TestCase {

  /**********************************************************************/
  
  public void testBasic1() 
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("a", new Printf("[%0]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("abba");
    assertEquals("[a]bb[a]", s);
  }

  public void testNullSplitter_getNumParts() 
      throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("a", new Printf("[%n]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("bab");
    assertEquals("b[1]b", s);
  }

  public void testForceReallocOfops() 
      throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("a", new Printf("%n%0%n%0%n"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("bab");
    assertEquals("b1a1a1b", s);
  }

  public void testForceReallocInNullSplitter() 
      throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("[a-z]+", new Printf("<t>%0</t>"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("0123456789abcdefghijklmnopqrstuvwxyzw");
    assertEquals("0123456789<t>abcdefghijklmnopqrstuvwxyzw</t>", s);
  }

  public void test_getPartWithRange1() 
      throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("[a-z]+", new Printf("[%0(1,-1)]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("---abcd---");
    assertEquals("---[bc]---", s);
  }

  public void test_getPartWithRange2() 
      throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("[a-z]+", new Printf("[%0(-3,0)]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("---abcd---");
    assertEquals("---[bcd]---", s);
  }

  public void test_getPartWithRange3() 
      throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("[a-z]+", new Printf("[%0(0,2)]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("---abcd---");
    assertEquals("---[ab]---", s);
  }
  public void test_getPartWithRange4() 
      throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    
    String s = 
      new Nfa("[a-z]+", new Printf("[%0(0,6)]"))
      .compile(DfaRun.UNMATCHED_DROP)
      .createRun()
      .filter("---abcd---");
    assertEquals("[abcd]", s);
  }
  public void test_getPartLen()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("[a-z]+", new Printf("[%l0]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("---abcd---");
    assertEquals("---[4]---", s);
  }
  public void test_getPartLen_WrongPart()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("[a-z]+", new Printf("[%l2]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("---abcd---");
    assertEquals("---[0]---", s);
  }
  public void test_funnyChars()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = 
      new Nfa("[a-z]+", new Printf("%%-\\%-%0()-%0\\(0,1)"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("---abcd---");
    assertEquals("---%-%-abcd()-abcd(0,1)---", s);
  }

  public void test_formatError1()
    throws ReSyntaxException
  {
    ReSyntaxException e = null;
    try {
      Printf pf = 
	new Printf("%axxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    } catch( ReSyntaxException _e) {
      e = _e;
    }
    String s = "%axxxxxx";
    assertEquals(s, e.text.substring(0, s.length()));
    int L = e.text.length();
    assertEquals("x...", e.text.substring(L-4));
    

  }

  // in contrast to the above, the format is very short resulting in
  // another preparation of the error string.
  public void test_formatError2()
    throws ReSyntaxException
  {
    ReSyntaxException e = null;
    try {
      Printf pf = new Printf("%abc");
    } catch( ReSyntaxException _e) {
      e = _e;
    }
    String s = "%abc";
    assertEquals(s, e.text);
  }

  // for completeness of the coverage, do call the constructor with
  // the priority set
  public void test_ConstructorWithPriority()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = new Nfa("a+", new Printf("[%0]"))
      .or("a", new Printf(false, "<%0>", 1))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("xaxaa");
    assertEquals("x<a>x[aa]", s);
  }

  // try PrintSpAction directly with a regular expression
  public void test_ConstructorWithRe()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s 
      = new Nfa("(![-+]?[0-9]+)[.](![0-9]+)", 
		new Printf(true, "%1,%2", 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("-2.4 5.2 +9.0001");
    assertEquals("-2,4 5,2 +9,0001", s);
  }
  
  public void test_NegativePartNumber1()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = new 
      Nfa("(![A-Za-z]+)[0-9]+(![A-Za-z]+)[0-9]+(![A-Za-z]+)", 
		       new Printf(true, "%-1-%-2", 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("aaa0bbb1ccc r0s0t0")
      ;
    assertEquals("ccc-bbb t-s0", s);
  }
  public void test_NegativePartNumber2()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = new 
      Nfa("(![A-Za-z]+)[0-9]+(![A-Za-z]+)[0-9]+(![A-Za-z]+)",
		       new Printf(true, "%-1(1,0)", 9))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("aaa0bbb1ccc r0s0t0")
      ;
    assertEquals("cc 0", s);
  }

  public void test_NegativePartNumber3()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = new 
      Nfa("(![A-Za-z]+)[0-9]+(![A-Za-z]+)[0-9]+(![A-Za-z]+)",
	  new Printf(true, "%l-1", 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("aaa0bbb1ccc r0s0t0")
      ;
    assertEquals("3 10", s);
  }

  public void test_PartSequence() 
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = new Nfa("[A-Za-z0-9]+",
		       new Printf
		       (new RegexpSplitter("[A-Za-z]+", 
					   RegexpSplitter.FETCH),
			new PrintfFormatter("%(2,-1)(%n)"), 0)
		       )
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("aaa0bbb1ccc2ddd3eeee r0s0t0u9v9w")
      ;
    assertEquals("bbbcccddd(6) stuv(7)", s);
  }
  public void test_PartSeqWithSeparator()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = new Nfa("[A-Za-z0-9]+",
		       new Printf
		       (new RegexpSplitter("[A-Za-z]+", 
					   RegexpSplitter.FETCH),
			new PrintfFormatter("%(2,-1, (+\\) )"), 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("aaa0bbb1ccc2ddd3eeee r0s0t0u9v9w")
      ;
    assertEquals("bbb (+) ccc (+) ddd s (+) t (+) u (+) v", s);
  }
  public void test_PartSeqWithPiece()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = new Nfa("[A-Za-z0-9.]+", 
		       new Printf
		       (new RegexpSplitter("[A-Za-z]+",
					   RegexpSplitter.FETCH),
			new PrintfFormatter("%(2,-1)(1,-1)"), 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("aaa0bbb1ccc2ddd3eeee XYZ.RST.ABC")
      ;
    assertEquals("bcd S", s);
  }
  public void test_PartSeqWithBoth()
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String s = new Nfa("[A-Za-z0-9.]+", 
		       new Printf
		       (new RegexpSplitter("[A-Za-z]+", 
					   RegexpSplitter.FETCH),
			new PrintfFormatter("%(2,-1,++)(1,-1)"), 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("aaa0bbb1ccc2ddd3eeee XYZ.RST.ABC")
      ;
    assertEquals("b++c++d S", s);
  }

  /**********************************************************************/
  // recently, actions.Printf was rewritten to work with a lone
  // formatter. We'll check that now
  public void test_PrintfOnlyWithFormatter() 
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    // we'll try to match digits within brackets, like "[99]" and the
    // digits are a submatch such that we can replace the brackets
    // with parentheses
    Formatter f = new Formatter() {
	public void format(StringBuffer out, TextStore ts, Map m) {
	  int L = ts.getNumParts();
	  for(int i=1; i<L; i++) {
	    out.append('(');
	    ts.getPart(out, i);
	    out.append(')');
	  }
	}
      };
    String s = new 
      Nfa("(\\[(![0-9]+)\\])+", new Printf(null, f, 0))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("[88]x[99][341]bla[99]")
      ;
    //System.out.println(s);
    assertEquals("(88)x(99)(341)bla(99)", s);
  }
  /**********************************************************************/
  public void test_GetVar() 
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    Store store = new Store("id");
    DfaRun r = new
      Nfa("[0-9]+", new Run(store, Drop.DROP))
      .or("[$]id", new Printf(null, new PrintfFormatter("%(id)"), 0) )
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun();
    r.clientData = new MapProvider() {
	Map m = new HashMap();
	public Map getMap() { return m; }
      };
    String s = r.filter("999bla=$id");
    assertEquals("bla=999", s);
  }

  // Forget to set a MapProvider in the DfaRun and see a
  // NullPointerException in PrintfFormatter.format
  public void test_GetVarNullPointerException() throws Exception {
    DfaRun r = new
      Nfa("x", new Run(new Store("id"), Drop.DROP))
      .or("y", new Printf("%(id)"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    Throwable e = null;
    try {
      r.filter("x");
    } catch( NullPointerException _e ) {
      e = _e;
    }
    assertTrue(e instanceof NullPointerException);
  }

  public void test_GetVarKeyUnavailable() throws Exception {
    DfaRun r = new
      Nfa("x", new Run(new Store("id"), Drop.DROP))
      .or("y", new Printf("%(id)"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    r.clientData = new MapProvider() {
	Map m = new HashMap();
	public Map getMap() { return m; }
      };
    String s = r.filter("aya");
    assertEquals("aa", s);
  }
  /**********************************************************************/
  public static void test_toString() throws Exception {
    PrintfFormatter pf = new PrintfFormatter("bla%1bla%(2,0,,)bla%%");
    String s = pf.toString();
    int pos = s.indexOf("[");
    s = s.substring(pos);
    //System.out.println("-->"+s);
    assertEquals("[5 ops]", s);
  }
  /**********************************************************************/
  public static void main(String[] argv) {
    junit.textui.TestRunner.run(new TestSuite(PrintfFormatterTest.class));
  }
}
 
