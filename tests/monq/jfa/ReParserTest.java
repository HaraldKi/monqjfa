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
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class ReParserTest extends TestCase {
  Nfa rep = null;

  public void setUp() throws ReSyntaxException {
    rep = new Nfa(".", Copy.COPY);   
  }
  public void testEBSEOF() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "abcde\\";
    try {
      rep.parse(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EBSATEOF, e.emsg);
  }
  public void testEEXTRACHAR() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "a)";
    try {
      rep.parse(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EEXTRACHAR, e.emsg);
  }
  public void testEINVALUL() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "[a-]";
    try {
      rep.parse(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EINVALUL, e.emsg);
  }

  public void testEINVRANGE() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "[x-a]";
    try {
      rep.parse(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()-1==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EINVRANGE, e.emsg);
  }

  public void testECLOSINGP() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "(abc";
    try {
      rep.parse(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.ECLOSINGP, e.emsg);
  }

  public void testECHARUNEXbracket() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "[abc^]";
    try {
      rep.parse(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()-1==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.ECHARUNEX, e.emsg);
  }

  public void testEEOFUNEX() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "(";
    try {
      rep.parse(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.EEOFUNEX, e.emsg);
  }

  public void testECHARUNEX() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "(*";
    try {
      rep.parse(s);
    } catch( ReSyntaxException _e ) {
      e = _e;
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
      //System.out.println(e);
    }
    assertEquals(ReSyntaxException.ECHARUNEX, e.emsg);
  }

//   public void testEATDIGIT() throws java.io.IOException {
//     ReSyntaxException e = null;
//     String s = "a@x";
//     try {
//       rep.parse(s);
//     } catch( ReSyntaxException _e ) {
//       e = _e;
//       //System.out.println(e);
//       assertEquals(s, e.text);
//       assertEquals(3, e.column);
//     }
//     assertEquals(ReSyntaxException.EATDIGIT, e.emsg);
//   }

//   public void testEATRANGE() throws java.io.IOException {
//     ReSyntaxException e = null;
//     String s = "a@1234";
//     try {
//       rep.parse(s);
//     } catch( ReSyntaxException _e ) {
//       e = _e;
//       //System.out.println(e);
//       assertEquals(s, e.text);
//       assertEquals(6, e.column);
//     }
//     assertEquals(ReSyntaxException.EATRANGE, e.emsg);
//   }

//   public void testEATMISSAT() throws java.io.IOException {
//     ReSyntaxException e = null;
//     String s = "a@123x";
//     try {
//       rep.parse(s);
//     } catch( ReSyntaxException _e ) {
//       e = _e;
//       //System.out.println(e);
//       assertEquals(s, e.text);
//       assertEquals(6, e.column);
//     }
//     assertEquals(ReSyntaxException.EATMISSAT, e.emsg);
//   }



  public void testCleanReset() throws java.io.IOException {
    ReSyntaxException e = null;
    String s = "[a-";
    try {
      rep.parse("abc");
      rep.parse(s);
    } catch( ReSyntaxException _e) {
      e = _e;
      //System.out.println(e);
      assertEquals(s, e.text);
      assertTrue(s.length()==e.column);
    }
    assertEquals(ReSyntaxException.EINVALUL, e.emsg);
  }
  public void testRecentWrap() throws java.io.IOException {
    ReSyntaxException e = null;
    // make sure the recent buffer in the ReParser overflows. 
    String s = "abcdefghijklmnopqrstuvwxyz"
      + "ABCDEFGHIJKLMNOPQRSTUVWXYZ::::::::::::::::::::::::"
      + ")0123456789";
    try {
      rep.parse(s);
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
  public void testBackslash() 
    throws java.io.IOException, ReSyntaxException {
    String s = "\\[a[x\\]]b";
    Nfa nfa = rep.parse(s, Drop.DROP);
    assertEquals(4, nfa.findPath("[a]b"));
    assertEquals(4, nfa.findPath("[axb"));
    assertEquals(-1, nfa.findPath("aaa"));
  }

  public void testMinusInBracket() 
    throws java.io.IOException, ReSyntaxException {
    String s = "[-abc]+";
    Nfa nfa = rep.parse(s, Drop.DROP);
    assertEquals(4, nfa.findPath("a-bcd"));
    assertEquals(4, nfa.findPath("----"));
  }

  public void testEOFinBracket() 
    throws java.io.IOException 
  {
    ReSyntaxException e = null;
    try {
      Nfa nfa = rep.parse("ab[rst");
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
      Nfa nfa = rep.parse("x?[");
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
  /********************************************************************/

  public static void main(String[] argv) {
    //ReParser rep = new ReParser();
    junit.textui.TestRunner.run(new TestSuite(ReParserTest.class));
  }
}

 
