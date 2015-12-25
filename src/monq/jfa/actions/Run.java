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
import monq.stuff.ArrayUtil;

/**
 * <p>runs several <code>FaAction</code> objects, one after another.</p>
 *
 * <p>Objects of this class contain a sequence of client
 * <code>FaAction</code>s which are run one after another when
 * <code>invoke()</code> is called. The first action in the sequence
 * will see the original <code>TextStore</code> object as prepared
 * by the calling <code>DfaRun</code>. The second and further client
 * actions will get passed a <code>TextStore</code> which contains
 * only one part, namely the text resulting from the application of
 * the previous actions. The resulting text in the output will be
 * exactly what the last action leaves there.</p>
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.5 $, $Date: 2005-02-14 10:23:38 $
 */
public class Run extends AbstractFaAction {
  FaAction[] steps;
  
  public Run(FaAction a0) {
    steps = new FaAction[1];
    steps[0] = a0;
  }
  public Run(FaAction a0, FaAction a1) {
    steps = new FaAction[2];
    steps[0] = a0;
    steps[1] = a1;
  }
  public Run(FaAction a0, FaAction a1, FaAction a2) {
    steps = new FaAction[3];
    steps[0] = a0;
    steps[1] = a1;
    steps[2] = a2;
  }
  public Run(FaAction a0, FaAction a1, FaAction a2, FaAction a3) {
    steps = new FaAction[4];
    steps[0] = a0;
    steps[1] = a1;
    steps[2] = a2;
    steps[3] = a3;
  }

  /**
   * <p>adds the given action <code>a</code> to the list of actions to
   * execute when <code>invoke</code> is called.</p>
   * @return this object itself is returned to ease repeated
   * application of this method.
   */
  public Run add(FaAction a) {
    steps = (FaAction[])ArrayUtil.resize(steps, steps.length+1);
    steps[steps.length-1] = a;
    return this;
  }
  public void invoke(StringBuffer out, int start, DfaRun r) 
    throws CallbackException
  {
    for(int i=0; i<steps.length; i++) {
      steps[i].invoke(out, start, r);
    }
  }
}

