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

import java.util.*;

import junit.framework.TestCase;

/**
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class ReParserTest extends TestCase {
  Nfa nfa;

  @Override
  public void setUp() throws ReSyntaxException {
    nfa = new Nfa();
  }
  public void testEBSEOF() {
    ReSyntaxException e = null;
    String s = "abcde\\";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EBSATEOF, e.emsg);
  }
  public void testEEXTRACHAR()  {
    ReSyntaxException e = null;
    String s = "a)";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EEXTRACHAR, e.emsg);
  }
  public void testEINVALUL() {
    ReSyntaxException e = null;
    String s = "[a-]";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EINVALUL, e.emsg);
  }

  public void testEINVRANGE() {
    ReSyntaxException e = null;
    String s = "[x-a]";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()-1==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EINVRANGE, e.emsg);
  }

  public void testECLOSINGP() {
    ReSyntaxException e = null;
    String s = "(abc";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.ECLOSINGP, e.emsg);
  }

  public void testECHARUNEXbracket() {
    ReSyntaxException e = null;
    String s = "[abc^]";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()-1==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.ECHARUNEX, e.emsg);
  }

  public void testEEOFUNEX() {
    ReSyntaxException e = null;
    String s = "(";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EEOFUNEX, e.emsg);
  }

  public void testEEOFUNEXinHex() {
    String s = "\\u123";
    try {
      nfa.or(s);
      fail("expected exception");
    } catch (ReSyntaxException e) {
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      assertEquals(ReSyntaxException.EEOFUNEX, e.emsg);
    }
  }
  
  
  public void testECHARUNEX() {
    ReSyntaxException e = null;
    String s = "(*";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.ECHARUNEX, e.emsg);
  }

  public void testCleanReset() {
    ReSyntaxException e = null;
    String s = "[a-";
    try {
      nfa.or("abc");
      nfa.or(s);
    } catch( ReSyntaxException _e) {
      e = _e;
      //System.out.println(e);
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
    }
    assertEquals(ReSyntaxException.EINVALUL, e.emsg);
  }
  public void testRecentWrap() {
    ReSyntaxException e = null;
    // make sure the recent buffer in the ReParser overflows.
    String s = "abcdefghijklmnopqrstuvwxyz"
      + "ABCDEFGHIJKLMNOPQRSTUVWXYZ::::::::::::::::::::::::"
      + ")0123456789";
    try {
      nfa.or(s);
    } catch( ReSyntaxException _e) {
      e = _e;
      //System.out.println(e);
      String tmp = s.substring(18);
      //System.out.println(">"+tmp+"<");
      assertEquals(tmp, e.text);
      assertTrue(tmp.length()-10==e.column);
    }
    assertEquals(ReSyntaxException.EEXTRACHAR, e.emsg);
  }
  public void testBackslash() throws ReSyntaxException {
    String s = "\\[a[x\\]]b";
    nfa.or(s, Drop.DROP);
    assertEquals(4, nfa.findPath("[a]b"));
    assertEquals(4, nfa.findPath("[axb"));
    assertEquals(-1, nfa.findPath("aaa"));
  }

  public void testMinusInBracket() throws ReSyntaxException {
    String s = "[-abc]+";
    Nfa nfas = new Nfa(s, Drop.DROP);
    assertEquals(4, nfas.findPath("a-bcd"));
    assertEquals(4, nfas.findPath("----"));
  }

  public void testEOFinBracket() {
    ReSyntaxException e = null;
    try {
      nfa.or("ab[rst");
    } catch( ReSyntaxException _e) {
      e = _e;
    }
    assertEquals(ReSyntaxException.EEOFUNEX, e.emsg);
  }

  // a very basic test of the toString function
  public void test_ReSyntaxException_toString()
  {
    ReSyntaxException e = null;
    try {
      nfa.or("x?[");
    } catch( ReSyntaxException _e) {
      e = _e;
    }
    String msg = e.toString();
    int line2 = 1+msg.indexOf("\n");
    int line3 = 1+msg.indexOf("\n", line2);
    String wrongText = msg.substring(line2, line3-1);
    String marker = msg.substring(line3);

    assertEquals("  x?[", wrongText);
    assertEquals("    ^", marker);
  }
  // ********************************************************************
  // try to make sure that all characters except the special characters match
  // themselves and check that special characters have a special meaning.
  public void testAllChars() throws Exception {
    String[] tests = {
        "[", "[a1]+", "a11ab", "a11a",
        "]", "[x]", "x", "x",
        "(", "(a)", "a", "a",
        ")", "(b)", "b", "b",
        "?", "a?b", "b", "b",
        "*", "a*", "aaab", "aaa",
        "+", "a+", "aaab", "aaa",
	"|", "(a|b)+", "aaabbbc", "aaabbb",
	".", ".+", "ai9q87b5098q", "ai9q87b5098q",
	"!", "a!", "a", "a",
	"^", "a^", "xxxxxxx", "xxxxxxx",
	"-", "[3-5]+", "345432", "34543",
	"\\", "a\\!", "a!", "a!",
	"~", "a~", "aaa", "aaa",
	// '@' should disappear soon, it is deprecated
	"@", "[@]", "@", "@",
    };
    Map<Character,Integer> m = new HashMap<Character,Integer>();
    for(int i=0; i<tests.length; i+=4) {
      m.put(new Character(tests[i].charAt(0)), new Integer(i));
    }

    // now run the test for each character
    char c = Character.MIN_VALUE;
    do {
      Character ch = new Character(c);
      String str, expected;
      Regexp re;
      if( m.containsKey(ch) ) {
	int i = m.get(ch).intValue();
	re = new Regexp(tests[i+1]);
	str = tests[i+2];
	expected = tests[i+3];
      } else {
	re = new Regexp("" + ch + "+");
	str = ""+ ch + ch + ch;
	expected = str;
      }
      assertTrue("checking `"+c+"'", re.atStartOf(str)!=-1);
      int len = re.length();
      String match = str.substring(0, len);
      assertEquals(expected, match);
      c += 1;
    } while( c!=Character.MAX_VALUE);

  }
  //********************************************************************
  public void testHexDigits() {
    for (int ch=Character.MIN_VALUE; ch<=Character.MAX_VALUE; ch++) {
      String s;
      if (ch<256) {
        s = String.format("abc\\x%02xdef", ch);
      } else {
        s = String.format("abc\\u%04xdef", ch);
      }
      Regexp re = new Regexp(s);
      assertTrue(re.matches("abc"+(char)ch+"def"));
    }
  }
  //********************************************************************
  public void testHexDigitsInRange() {
    String pattern = "abc[\\x00-\\x1f]def";
    Regexp re = new Regexp(pattern);
    for (char ch=0; ch<32; ch++) {
      assertTrue(re.matches("abc"+ch+"def"));
    }
  }
  //********************************************************************
}


