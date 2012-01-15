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
public class DfaTest extends TestCase {
  private static class Xaction extends AbstractFaAction {
    private String name;
    public Xaction(String name) { this.name = name;}
    public void invoke(StringBuffer s, int start, DfaRun r) {}
    public String toString() {return name;}
  }
  /**********************************************************************/
  public static void test_publicmatch() throws Exception {
    Dfa dfa = 
      new Nfa("a+", new Xaction("xxx"))
      .compile(DfaRun.UNMATCHED_DROP);
    StringBuffer sb = new StringBuffer();

    CharSource cs = new CharSequenceCharSource("aaaaa");
    TextStore ts = null;
    assertEquals("xxx", dfa.match(cs, sb, ts).toString());
    assertEquals(5, sb.length());

    cs = new CharSequenceCharSource("baaaa");
    sb.setLength(0);
    assertNull(dfa.match(cs, sb, ts));
    assertEquals(0, sb.length());

    dfa = 
      new Nfa("a(!b+)c", new Xaction("xxx"))
      .or("a(!XX|YY)(!z+)", new Xaction("yyy"))
      .compile(DfaRun.UNMATCHED_DROP); 

    ts = new TextStore();
    cs = new CharSequenceCharSource("abbbcd");
    assertEquals("xxx", dfa.match(cs, sb, ts).toString());
    assertEquals("abbbc", ts.getPart(0));
    assertEquals("bbb", ts.getPart(1));
    
    cs = new CharSequenceCharSource("aXXzzz");
    assertEquals("yyy", dfa.match(cs, sb, ts).toString());
    assertEquals("aXXzzz", ts.getPart(0));
    assertEquals("XX", ts.getPart(1));
    assertEquals("zzz", ts.getPart(2));


    cs = new CharSequenceCharSource("aYYzzzz");
    assertEquals("yyy", dfa.match(cs, sb, ts).toString());
    assertEquals("aYYzzzz", ts.getPart(0));
    assertEquals("YY", ts.getPart(1));
    assertEquals("zzzz", ts.getPart(2));

  }
  /**********************************************************************/
  public static void main(String[] argv)   {
    // Fa fa = new Fa();
    junit.textui.TestRunner.run(new TestSuite(DfaTest.class));
  }
}
