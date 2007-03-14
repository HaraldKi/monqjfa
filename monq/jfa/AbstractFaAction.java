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

import java.io.Serializable;

/**
 * <p>implements the prototype of an {@link FaAction} with initial
 * priority zero so that actions can be easily written as anonymous
 * classes. For a bunch of useful actions have a
 * look at the subpackage {@link monq.jfa.actions}.
 *
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.37 $, $Date: 2005-02-20 21:10:53 $
 */
public abstract class AbstractFaAction implements Serializable, FaAction {

  /**
   * used to score against competing actions during compilation of an
   * NFA into a DFA.
   */
  protected int priority = 0;
  /*
   * is used to mark a dfa state as a stop state the associated action
   * of which shall do nothing.
   */
  //public int getPriority() {return priority;}
  public AbstractFaAction setPriority(int p) { 
    this.priority = p; 
    return this;
  }

  public FaAction mergeWith(FaAction _other) {
    if( this.equals(_other) ) return this;

    // Instead of doing an 'instanceof' ourselves, we leave this test to
    // the JVM, which does it anyway, and do a 'catch' instead of an
    // 'else' 
    AbstractFaAction other;
    try {
      other = (AbstractFaAction)_other;
    } catch( ClassCastException e ) {
      return null;
    }

    if( other.priority>priority ) return other;
    if( other.priority<priority ) return this;
    return null;
  }

  /**
   * <p>although this class is abstract, the constructor is needed to create
   * anonymous subclasses.</p>
   */
  public AbstractFaAction() {}
  /**********************************************************************/
}
