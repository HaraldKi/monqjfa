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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * <p>is an {@link FaAction} that forwards the callback to its {@link
 * FaAction#invoke invoke()} method to a named method by using
 * reflection.</p>
 *
 * <p>Creating a {@link monq.jfa.Dfa} with many different callbacks
 * (<code>FaAction</code>s) 
 * can be tedious, because each action must be in its own
 * class. Although anonymous classes help to reduce the code clutter,
 * the actions often must communicate through a common state. Doing so
 * in a way that the <code>Dfa</code> can be used in parallel threads
 * requires each action to retrieve the common state from {@link
 * DfaRun#clientData} and cast it to the type it is known to be
 * &mdash; tedious again.</p>
 *
 * <p>The <code>Call</code> action helps to get rid of the tedious
 * steps described above. Instead of one class per action, just write
 * one class, say <code>Worker</code>, and implement each action as a
 * method. The name of the actions does not matter, they just have to
 * be <code>public</code> and must have the same signature as {@link
 * monq.jfa.FaAction#invoke}. To use the resulting <code>Worker</code>
 * in a <code>Dfa</code>, two things need to be done:</p>

 * <ol>
 * <li>Use <code>Call</code> objects with the action methods' names when
 * creating the {@link monq.jfa.Nfa}.</li>
 * <li>Provide a fresh instance of <code>Worker</code> in the
 * <code>DfaRun.clientData</code> field of the <code>DfaRun</code>
 * object that operates the <code>Dfa</code> compiled from the
 * <code>Nfa</code>.</li>
 * </ol>
 *
 * <p>When called, the {@link #invoke invoke()}
 * method of a <code>Call</code> object will retrieve the
 * <code>Worker</code> from the <code>clientData</code> field of its
 * third parameter. Then it uses reflection to find the action to call
 * according to the name specified in the constructor of the
 * <code>Call</code> object.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>public class Worker {
 *   public void <span style="color:red">invokeOne</span>(StringBuffer sb, int start, DfaRun r) {
 *     ...
 *   }
 *   public void <span style="color:red">invokeTwo</span>(StringBuffer sb, int start, DfaRun r) {
 *     ...
 *   }
 * }
 *
 * // somewhere else create the Dfa and the DfaRun
 * Dfa dfa = new Nfa("myRe1", new Call("<span style="color:red">invokeOne</span>"))
 *               .or("myRe2", new Call("<span style="color:red">invokeTwo</span>"))
 *               ...
 * DfaRun run = new DfaRun(dfa);
 * run.clientData = new Worker();
 * ...</pre>
 *
 * <p><b>Note:</b> I would be interested to hear opinions or
 * measurements whether this approach, using reflection, is
 * slower than using one class per action.</p>
 *
 * @author &copy; 2003-2007 Harald Kirsch
 */
public class Call extends AbstractFaAction {
  // name of the method we are going to forward the action to.
  private final String methodName;

  // describes a typical FaAction.invoke() method
  private static final Class[] invokeArgs;
  static {
    try {
      invokeArgs = new Class[3];
      invokeArgs[0] = Class.forName("java.lang.StringBuffer");
      invokeArgs[1] = java.lang.Integer.TYPE;
      invokeArgs[2] = Class.forName("monq.jfa.DfaRun");
    } catch( ClassNotFoundException e ) {
      throw new RuntimeException(e);
    }
  };
  
  //-******************************************************************
  /**
   * <p>creates a <code>Call</code> that will forward the call to a
   * method with the given <code>methodName</code>.</p>
   */
  public Call(String methodName) {
    this.methodName = methodName;
  }

  //-******************************************************************
  /**
   * <p>calls the method specified in the constructor by finding it in
   * <code>run.clientData</code> via reflection.</p>
   *
   * @exception CallbackException if the method called throws it. In
   * that case the exception is rethrown.
   *
   * @exception IllegalArgumentException if the named method cannot be
   * found in <code>run.clientData</code>, if the invocation results
   * in an <code>IllegalAccessException</code> or if the method called
   * throws a checked exception other than a
   * <code>CallbackException</code>.
   */
  public void invoke(StringBuffer out, int start, DfaRun run) 
    throws CallbackException
  {
    Method m;    
    try {
      m = run.clientData.getClass().getMethod(methodName, invokeArgs);
    } catch( NoSuchMethodException e ) {
      throw new IllegalArgumentException("see cause", e);
    }

    Object[] args = new Object[3];
    args[0] = out;
    args[1] = new Integer(start);
    args[2] = run;
    try {
      m.invoke(run.clientData, args);
    } catch( IllegalAccessException e ) {
      throw new IllegalArgumentException("see cause", e);
    } catch( InvocationTargetException e ) {
      Throwable th = e.getCause();
      if( th instanceof CallbackException ) throw (CallbackException)th;
      if( th instanceof RuntimeException ) throw (RuntimeException)th;
      throw new IllegalArgumentException("see cause", th);
    }					 
  }

  //-******************************************************************
  /**
   * <p>returns the method name specified in the constructor.</p>
   */
  public String getMethodName() { return methodName; }
  //-******************************************************************
  /**
   * <p>returns <code>true</code> if and only if <code>_o</code> is a
   * <code>Call</code> object and invokes the same method name.</p>
   */
  public boolean equals(Object _o) {
    if( !(_o instanceof Call) ) return false;
    Call o = (Call)_o;
    return o.priority==priority && methodName.equals(o.methodName);
  }
  //-******************************************************************
  public int hashCode() { return methodName.hashCode(); }
  
//   public String toString() {
//     StringBuffer sb = new StringBuffer(30);
//     sb.append(super.toString())
//       .append("[\"").append(pre).append("\", \"")
//       .append(post).append("\"]")
//       ;
//     return sb.toString();
//   }
}
