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


/**
 * <p>run one or another {@link monq.jfa.FaAction} depending on the outcome
 * of asking a {@link Verifier}. A typical use case normally employs
 * a <code>Verifier</code> and a {@link Hold}:<pre>
 * Hold hold = new Hold();
 * Count c = new Count("yada");
 * Nfa nfa = new Nfa("start", new Run(hold, c.reset()))
 *               .or("cute pattern", c)
 *               .or("end", new If(c.ge(2), hold.ship(), hold.drop());
 * </pre>
 * In the example, a {@link Count} object is used to count the
 * occurrences of the string <code>"cute pattern"</code>. The counter
 * is reset on seeing <code>"start"</code>. When seeing
 * <code>"end"</code>, <code>If</code> calls the <code>Verifier</code>
 * returned by <code>c.ge(2)</code> which will return
 * <code>true</code> or <code>false</code> depending on whether more
 * than 1 pattern was seen. Then it calls one of the actions
 * <code>hold.ship()</code> or <code>hold.drop()</code> accordingly.</p>
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class If extends AbstractFaAction {
  private Verifier v;
  private FaAction thendo, elsedo;
  /**
   *
   */
  public If(Verifier v, FaAction thendo, FaAction elsedo) {
    this.v = v;
    this.thendo = thendo;
    this.elsedo = elsedo;
  }
  public void invoke(StringBuffer yytext, int start, DfaRun r) 
    throws CallbackException
  {
    if( v.ok(r) ) thendo.invoke(yytext, start, r);
    else elsedo.invoke(yytext, start, r);
  }
}



 
