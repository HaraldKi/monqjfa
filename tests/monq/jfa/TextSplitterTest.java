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

import java.io.StringReader;
import java.lang.Class;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class TextSplitterTest extends TestCase {
  // a client object for which only the Splitter interface shall be
  // tested, i.e. nothing implementation specific
  TextSplitter client;
  TextStore store;

  // strings which are split into parts by the client object
  String[][] tests = {
    {"hallo;xyz", "hallo", "xyz"},
    {";;;"},
    {" abc ", "abc"},
    {"a b c d e f g h i j k l m n o p q x y z",
     "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", 
     "k", "l", "m", "n", "o", "p", "q", "x", "y", "z"}
  };

  // When preparing for another type of Splitter, set the following
  // such that these conditions hold:
  // a) the string to split starts at index 3
  // b) testAOut[0] is part 0 and must be longer than 4 chars
  // c) testAOut[1] is part 1 and must be longer than 4 chars
  String testAIn = "abcHallo;ballo";
  String[] testAOut = {"Hallo;ballo", "Hallo"}; 

  public void setUp() throws ReSyntaxException, CompileDfaException {
    client = new RegexpSplitter("[A-Za-z0-9]+", RegexpSplitter.FETCH);
    store = new TextStore();
  }
  public void testBeforeSplit1() {
    assertEquals(0, store.getNumParts());
  }
  public void testBeforeSplit2() {
    StringBuffer sb = new StringBuffer();
    Exception e = null;
    store.getPart(sb, 1);
    assertEquals("", sb.toString());
//     try {
//       store.getPart(sb, 1);
//     } catch( Exception _e ) {
//       e = _e;
//     }
    //assertTrue(e instanceof ArrayIndexOutOfBoundsException);
  }
  public void testBeforeSplit3() {
    StringBuffer sb = new StringBuffer();
    Exception e = null;
    store.getPart(sb, 0);
    assertEquals("", sb.toString());
//     try {
//       store.getPart(sb, 0);
//     } catch( ArrayIndexOutOfBoundsException _e ){
//       e = _e;
//     }
//     assertTrue(e instanceof ArrayIndexOutOfBoundsException);
  }
  public void testBeforeSplit4() {
    StringBuffer sb = new StringBuffer();
    Exception e = null;
    store.getPart(sb, 0, 0, 0);
    assertEquals("", sb.toString());
//     try {
//       store.getPart(sb, 0, 0, 0);
//     } catch( ArrayIndexOutOfBoundsException _e ){
//       e = _e;
//     }
//     assertTrue(e instanceof ArrayIndexOutOfBoundsException);
  }
  public void testBeforeSplit5() {
    StringBuffer sb = new StringBuffer();
    Exception e = null;
    store.getPart(sb, 0, 0, 1);
    assertEquals("", sb.toString());
//     try {
//       store.getPart(sb, 0, 0, 1);
//     } catch( ArrayIndexOutOfBoundsException _e) {
//       e = _e;
//     }
//     assertTrue(e instanceof ArrayIndexOutOfBoundsException);
  }

  // depending on the object in 'client' the strings in 'tests' should
  // probably be changed!! (Needs some thought if additional Splitters
  // are implemented.
  public void testGeneric1() {
    for(int i=0; i<tests.length; i++) {
      store.clear();
      client.split(store, new StringBuffer(tests[i][0]), 0);
      assertEquals(tests[i].length, store.getNumParts());
      StringBuffer sb = new StringBuffer();
      store.getPart(sb, 0);
      assertEquals(tests[i][0], sb.toString());
      for(int j=1; j<tests[i].length; j++) {
	sb.setLength(0);
	store.getPart(sb, j);
	assertEquals("loop with j="+j, tests[i][j], sb.toString());
      }
    }
  }

  public void testSubstring() {
    client.split(store, new StringBuffer(testAIn), 3);
    StringBuffer sb = new StringBuffer();
    for(int i=0; i<2; i++) {
      int L = testAOut[i].length();

      sb.setLength(0);
      store.getPart(sb, i);
      assertEquals("i="+i, testAOut[i], sb.toString());
      assertEquals("i="+i, testAOut[i].length(), store.getPartLen(i));

      sb.setLength(0);
      store.getPart(sb, i, 1, -1);
      assertEquals("i="+i, testAOut[i].substring(1, L-1), sb.toString());
      
      sb.setLength(0);
      store.getPart(sb, i, -3, 0);
      assertEquals(testAOut[i].substring(L-3, L), sb.toString());
      
      sb.setLength(0);
      store.getPart(sb, i, 2, 2);
      assertEquals("", sb.toString());
      
      sb.setLength(0);
      store.getPart(sb, i, 2, 3);
      assertEquals(testAOut[i].substring(2, 3), sb.toString());

      // test left index too small 
      sb.setLength(0);
      store.getPart(sb, i, -100, 0);
      assertEquals(testAOut[i], sb.toString());
//       Exception e = null;
//       try {
// 	store.getPart(sb, i, -100, 0);
// 	System.out.println("*************"+sb);
//       } catch( Exception _e) {
// 	e = _e;
//       }
//       assertTrue(e instanceof StringIndexOutOfBoundsException);

      // test left index much too large
      sb.setLength(0);
      store.getPart(sb, i, 10000, 0);
      assertEquals("", sb.toString());
//       e = null;
//       try {
// 	store.getPart(sb, i, 10000, 0);
//       } catch( Exception _e) {
// 	e = _e;
//       }
//       assertTrue(e instanceof StringIndexOutOfBoundsException);

      // test right index too small
      sb.setLength(0);
      store.getPart(sb, i, 0, -10000);
      assertEquals("", sb.toString());
//       e = null;
//       try {
// 	store.getPart(sb, i, 0, -10000);
//       } catch( Exception _e) {
// 	e = _e;
//       }
//       assertTrue(e instanceof StringIndexOutOfBoundsException);

      // test right index far too large
      sb.setLength(0);
      store.getPart(sb, i, 0, 10000);
      assertEquals(testAOut[i], sb.toString());
//       e = null;
//       try {
// 	store.getPart(sb, i, 0, 10000);
//       } catch( Exception _e) {
// 	e = _e;
//       }
//       assertTrue(e instanceof StringIndexOutOfBoundsException);

      // out of range part should return empty string
      sb.setLength(0);
      store.getPart(sb, 299, 0, 10000);
      assertEquals("", sb.toString());
//       e = null;
//       try {
// 	store.getPart(sb, 1000, 0, 1);
//       } catch( Exception _e) {
// 	e = _e;
//       }
//       assertTrue(e instanceof ArrayIndexOutOfBoundsException);

      // yet another one, this time in getPartLen
      assertEquals(0, store.getPartLen(1000));
//       e = null;
//       try {
// 	store.getPartLen(1000);
//       } catch( Exception _e) {
// 	e = _e;
//       }
//       assertTrue(e instanceof ArrayIndexOutOfBoundsException);
      
    }

  }

  /**********************************************************************/
  // A special test for RegexpSplitter with a re matching the empty
  // string 
  public void test_REsplitterEmptyRE() throws Exception {
    TextSplitter sp = new RegexpSplitter("[@]", RegexpSplitter.SPLIT);
    Formatter fmt = new PrintfFormatter("[%(1,0,][)]");
    StringBuffer b = new StringBuffer("abc@def@123");
    TextStore ts = new TextStore();
    sp.split(ts, b, 0);
    b.setLength(0);
    fmt.format(b, ts, null);
    // System.out.println("++"+b);
    //System.out.println(">>"+ts);
    assertEquals("[abc][def][123]", b.toString());
  }
  /********************************************************************/

  public static void main(String[] argv) {
    junit.textui.TestRunner.run(new TestSuite(TextSplitterTest.class));
  }
}

 
