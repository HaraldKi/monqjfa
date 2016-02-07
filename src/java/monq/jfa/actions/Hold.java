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

import java.util.*;

/**
 * <p>hold back output until a decision can be made whether to ship or
 * to drop filtered output. Associate this callback with a pattern
 * that defines the start of a lengthy region of text only at the end
 * of which a decision can be made whether to drop it or copy it to
 * the output.</p>
 *
 * <p>This callback sets {@link monq.jfa.DfaRun#collect} to
 * <code>true</code> and records the position.  To store the position,
 * the callback obtains a <code>Map</code> from a {@link MapProvider}
 * that has to be in the {@link monq.jfa.DfaRun#clientData} field of
 * the calling <code>DfaRun</code>. The <code>Hold</code> object
 * stores only one entry in the <code>Map</code> with itself as the
 * key. Consequently you may use the <code>Map</code> in other
 * callbacks to store information that may be necessary to make the
 * later decision whether to ship or drop the text.</p>
 *
 * <p>At the point where the decision can be made, use the callback
 * generated by either {@link #drop drop()} or {@link #ship ship()} to
 * delete or deliver the output held back so far. A typical use looks
 * like:
 * <pre>   Hold hold = new Hold();
 *   ...
 *   nfa.or("start", hold)
 *      .or("goodEnd", hold.ship())
 *      .or("badEnd", hold.drop())
 *      // or in a different scenario with If
 *      .or("end", new If(v, hold.ship(), hold.drop()))</pre>
 * </p>
 * <p>In the use with {@link If}, <code>v</code> would be a {@link
 * Verifier} which performs whatever test is necessary to check
 * whether the held back output shall be shipped or dropped.</p>
 *
 * <p>Objects of this class can operate in nested contexts. Only the
 * outermost <code>Hold</code> object will set {@link
 * monq.jfa.DfaRun#collect} back to false.</p>
 *
 * @author &copy; 2003,2004 Harald Kirsch
 */
public class Hold extends AbstractFaAction {


  private static class StackElem {
    // state the calling Dfa was in when calling us
    public boolean keptCollect;
    
    // start position recorded when Start is called
    public int start = -1;

    public StackElem(boolean collect, int start) {
      this.keptCollect = collect;
      this.start = start;
    }
  }
  /**********************************************************************/
  public void invoke(StringBuilder yytext, int start, DfaRun r) {
    Map<Object,Object> m = ((MapProvider)(r.clientData)).getMap();
    @SuppressWarnings("unchecked")
    List<StackElem> stack = (List<StackElem>)m.get(this);
    if( stack==null ) {
      m.put(this, stack=new ArrayList<StackElem>());
    }
    stack.add(new StackElem(r.collect, start));
    r.collect = true;
  }
  /**********************************************************************/
  /**
   * <p>returns the position this object current refers to as the start
   * position where data is being hold back.</p>
   */
  public int getStart(DfaRun r) {
    StackElem e = peek(r, false);
    return e.start;
  }
  /**********************************************************************/
  private StackElem peek(DfaRun r, boolean pop) {
    Map<Object,Object> m = ((MapProvider)(r.clientData)).getMap();
    @SuppressWarnings("unchecked")
    List<StackElem> stack = (List<StackElem>)m.get(this);
    int l = -1;
    if( stack==null || (l=stack.size())==0 ) {
      throw new 
	IllegalStateException("no current start position available");
    }
    if( pop ) {
      return stack.remove(l-1);
    }
    return stack.get(l-1);
  }
  /**********************************************************************/
  /**
   * <p>create an {@link monq.jfa.FaAction} which drops (deletes) the
   * output 
   * held back by this object. The action returned
   * will throw an <code>IllegalStateException</code> when called
   * while there is no data being held back.</p>
   */
  public AbstractFaAction drop() { return new Do(true); }
  private class Do extends AbstractFaAction {
    private final boolean drop;
    Do(boolean drop) { this.drop = drop; }
    public void invoke(StringBuilder yytext, int start, DfaRun r) 
      throws CallbackException
    {
      StackElem elem = null;
      try {
	elem = peek(r, true);
      } catch( IllegalStateException e ) {
	throw new CallbackException("Hold not active", e);
      }
      r.collect = elem.keptCollect;
      if( drop ) yytext.setLength(elem.start);
    }
  }
  /**********************************************************************/
  /**
   * <p>create an {@link monq.jfa.FaAction} which ships (delivers) the
   * output 
   * held back by this object. The action returned
   * will throw an <code>IllegalStateException</code> when called
   * while there is no data being is held back.</p>
   */
  public AbstractFaAction ship() { return new Do(false); }
  /**********************************************************************/

}

 