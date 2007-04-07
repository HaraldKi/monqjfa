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

package monq.jfa.actions;

import monq.jfa.*;

import java.util.Map;

/**
 * <p>counts matches and can then be used with {@link If} to perform
 * conditional actions depending on whether a threshold count is
 * reached. To retrieve and store the current count,
 * the callback obtains a <code>Map</code> from a {@link MapProvider}
 * that has to be in the {@link monq.jfa.DfaRun#clientData} field of
 * the calling <code>DfaRun</code>. The value is stored in the
 * <code>Map</code> with the key specified in the constructor. The
 * same key may be used in a {@link Printf} callback's format like
 * <code>"%(key)</code> to insert the value into the output stream.</p>
 *
 * <p>Whenever <code>invoke</code> is called for a
 * successfull match, the counter is incremented by one. 
 * The counter can be tested against a threshold with the {@link
 * Verifier} returned by {@link #ge ge()}. Typically this verifier
 * will be passed to an instance of {@link If}. An example is
 * <pre>   Count cnt = new Count("no_of_fishes");
 *   ...
 *   nfa.or("start", cnt.reset())
 *      .or("fish", cnt)
 *      .or("end", new If(cnt.ge(2), a1, a2))</pre> 
 * The callback of <code>If</code> will call <code>ok()</code> on the
 * <code>Verifier</code> returned by <code>cnt.ge(2)</code> and
 * perform either action <code>a1</code> or <code>a2</code>.</p>
 * 
 * <p>To add another increment then 1 to the counter, use the action
 * created by {@link #add}.</p>
 *
 * @author &copy; 2004 Harald Kirsch
 * @version $Revision: 1.6 $, $Date: 2005-07-08 13:01:26 $
 */
public class Count extends AbstractFaAction {
  private final String key;

  private static final class Int { 
    int value = 0;
    public String toString() { return Integer.toString(value); }
  }
  /**********************************************************************/
  /** 
   * <p>create a counter callback which records the its value with
   * <code>key</code>.</p>
   */
  public Count(String key) { this.key = key; }
  /**********************************************************************/
  private Int getCounter(DfaRun r) {
    MapProvider mp = (MapProvider)r.clientData;
    Map<Object,Object> m = mp.getMap();
    Int i = (Int)m.get(key);
    if( i==null ) m.put(key, i=new Int());
    return i;
  }
  /**********************************************************************/
  public void invoke(StringBuffer yytext, int start, DfaRun r) {
    getCounter(r).value += 1;
  }
  /**********************************************************************/
  /**
   * <p>given the <code>DfaRun</code> which has just the right
   * <code>MapProvider</code> in its <code>clientData</code> field,
   * this returns the current counter value.</p>
   */
  public int getValue(DfaRun r) { return getCounter(r).value; }
  /**********************************************************************/
  /**
   * <p>returns an {@link monq.jfa.FaAction} which resets the counter to
   * zero.</p> 
   */
  public AbstractFaAction reset() { return new Reset(); }
  private class Reset extends AbstractFaAction {
    public void invoke(StringBuffer yytext, int start, DfaRun r) {
      Int i = getCounter(r);
      i.value = 0;
    }
  }
  /**********************************************************************/

  /**
   * returns an {@link monq.jfa.FaAction} which changes the counter
   * according to the 
   * given increment <code>incr</code>. Example use:<pre>
   *   Count cnt = new Count(7);
   *   ...
   *   nfa.or("pattern with weight 2", cnt.add(2));</pre>
   * </p>
   */
  public AbstractFaAction add(int incr) { return new Add(incr); }
  private class Add extends AbstractFaAction {
    private final int incr;
    public Add(int incr) { this.incr = incr; }
    public void invoke(StringBuffer yytext, int start, DfaRun r) {
      Int i = getCounter(r);
      i.value += incr;
    }
  }
  /**********************************************************************/
  /**
   * <p>returns a {@link Verifier} which returns <code>true</code> if
   * the counter is greater than or equal to the given
   * threshold. Example use:<pre> 
   *   Count cnt = new Count();
   *   ...
   *   nfa.or("decision pattern", new If(cnt.ge(2), ..., ...));</pre>
   * </p>
   */
  public Verifier ge(int threshold) { return new Ge(threshold); }
  private class Ge implements Verifier {
    private final int threshold;
    public Ge(int threshold) { this.threshold = threshold; }
    public boolean ok(DfaRun r) {
      Int i = getCounter(r);
      return i.value>=threshold;
    }
  }
  /**********************************************************************/
}
 
