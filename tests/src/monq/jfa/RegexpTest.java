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

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class RegexpTest extends TestCase {

  public static void test_matches() {
    assertTrue(Regexp.matches("a+", "aaaaaaa", 0));
    assertFalse(Regexp.matches("a+", "aaaab", 0));
    assertTrue(new Regexp("abc+").matches("abccc"));
    Exception e = null;
    try {
      Regexp.matches("a(*", "b", 0);
    } catch( IllegalArgumentException _e) {
      e = _e;
    }
    assertTrue(e instanceof IllegalArgumentException);

  }
  /**********************************************************************/
  public static void test_find() {
    Regexp re = new Regexp("(a|bc)+");
    assertEquals(4, re.find("0000abcbcabcbcaxyz"));
    assertEquals(11, re.length());
    assertEquals(4, re.find("0000abcbcabcbcaxyz", 2));
    assertEquals(11, re.length());

    assertEquals(-1, re.find("0000abcbcabcbcaxyz", 15));
    
    Exception e = null;
    try {
      assertEquals(11, re.length());
    } catch( IllegalStateException _e ) {
      e = _e;
    }
    assertTrue(e instanceof IllegalStateException);

    assertEquals(-1, Regexp.find("x+", "aaa", 1));
  }
  /**********************************************************************/
  public static void test_startsWith() {
    Regexp re = new Regexp("(a|bc)+");
    assertEquals(11, re.atStartOf("abcbcabcbcaxyz"));
    assertEquals(11, re.length());
    assertEquals(8, re.atStartOf("abcbcabcbcaxyz", 3));
    assertEquals(8, re.length());

    assertEquals(-1, re.atStartOf("xyz"));
    Exception e = null;
    try {
      assertEquals(11, re.length());
    } catch( IllegalStateException _e ) {
      e = _e;
    }
    assertTrue(e instanceof IllegalStateException);

    assertEquals(3, Regexp.atStartOf("abc", "xabc", 1));
  }
  /**********************************************************************/
  public static void test_submatches() {
    Regexp re = new Regexp("(!a+)(!b+)");
    String s = "xyzaaaaabbbbbZ";

    assertEquals(3, re.find(s));
    TextStore ts = re.submatches();
    assertEquals("aaaaabbbbb", ts.getPart(0));
    assertEquals("aaaaa", ts.getPart(1));
    assertEquals("bbbbb", ts.getPart(2));

    re.find("x");

    Exception e = null;
    try {
      ts = re.submatches();
    } catch( IllegalStateException _e ) {
      e = _e;
    }
    assertTrue(e instanceof IllegalStateException);

  }
  /**********************************************************************/
  public static void main(String[] argv)   {
    junit.textui.TestRunner.run(new TestSuite(RegexpTest.class));
  }

}



