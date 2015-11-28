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
import monq.jfa.actions.Copy;

/**
 * will copy any test in here for debugging
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class IntervalsTest extends TestCase {


  // exploit a bug once found in Intervals.setFrom()
  public static void test_Bug1() throws Exception {
    FaState inner = AbstractFaState.createDfaState(new Copy(0),  false);
    FaState outer = AbstractFaState.createDfaState(new Copy(0), false);
    IntervalsFaState ivals = new IntervalsFaState();
    ivals.invert(outer);
    ivals.overwrite('e', 'e', inner);
    CharTrans t = ivals.toCharTrans(1.0);
    ivals.setFrom(t);
    assertEquals(3, ivals.size());
    assertEquals(outer, ivals.getAt(0));
    assertEquals(inner, ivals.getAt(1));
    assertEquals(outer, ivals.getAt(2));
  }
  /**********************************************************************/
  // this is mainly here to get the test coverage. Normally it should
  // be possible to switch the affected lines off for TC
  public static void test_toString() throws Exception {
    Intervals<Object> ivals = new Intervals<Object>();
    String s = ivals.toString();
    //System.out.println(s);
    assertEquals("Intervals[0x0->null...]", s);
  }
  /**********************************************************************/
  // similar for TableCharTrans
  public static void test_TCTtoString() throws Exception {
    IntervalsFaState ivals = new IntervalsFaState();
    for(int i=0; i<10; i++) {
      FaState someState = AbstractFaState.createDfaState(new Copy(i), false);
      ivals.overwrite((char)('a'+i), (char)('a'+i), someState);
    }
    CharTrans t = ivals.toCharTrans(1.0);
    String s = t.toString();
    //System.out.println(s);
    assertEquals("[a,j ..........]", s);
  }
  /**********************************************************************/
  public static void main(String[] argv)   {
    // Fa fa = new Fa();
    junit.textui.TestRunner.run(new TestSuite(IntervalsTest.class));
  }
}
