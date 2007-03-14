package monq.jfa.ctx;

import monq.jfa.*;

import java.util.*;

/**
 * <p>is an {@link monq.jfa.FaAction} to execute different client actions
 * depending on the {@link Context} in which
 * <code>IfContext.invoke()</code> is called. Typical usage:<pre>
 * nfa.or("regex", new IfAction(ctx1, action1)
 *                      .ifthen(ctx2, action2)
 *                      .ifthen(ctx3, action3)
 *                      .elsedo(actionDefault))</pre>
 * The context objects <code>ctx1</code> etc. must be acquired before
 * by use of a {@link ContextManager}.</p>
 * 
 * <p>It is not necessary to add all valid contexts for a certain
 * <code>"regex"</code> with the same <code>IfAction</code>
 * object. Any number of <code>IfAction</code> objects may be bound to
 * the regular expression or to regular expressions which share
 * matches. When the <code>Nfa</code> is compiled into a
 * <code>Dfa</code>, {@link #mergeWith mergeWith()} will combine all
 * actions into one.</p>
 */
public class IfContext extends AbstractFaAction {
  private Map m = new HashMap();

  // used as a key into `m' above for the else case
  private static final Context ELSE = new Context().setName("#ELSE#");
  /**********************************************************************/
  /**
   * create an action which does only perform default actions.
   */
  public IfContext() {}
  /**********************************************************************/
  /**
   * <p>creates an {@link FaAction} to run the given client action if
   * called in the given <code>Context</code>.</p>
   * 
   * @see #ifthen
   */
  public IfContext(Context ctx, FaAction a) {
    ifthen(ctx, a);
  }
  /**********************************************************************/
  /**
   * <p>defines to call the given action when this object's invoke method
   * is called in the given context.</p>
   * <p><b>NOTE:</b>The given <code>Context</code> may be
   * <code>null</code> to indicate that the action should be run if no
   * context is (yet) on the stack.</p>
   */
  public IfContext ifthen(Context ctx, FaAction a) {
    if( a==null ) throw new NullPointerException("parameter a==null");
    if( m.containsKey(ctx) ) {
      throw new IllegalStateException("context already registered");
    }
    m.put(ctx, a);
    return this;
  }
  /**********************************************************************/
  /**
   * <p>define the action to call when this object's invoke
   * method is called in a context for which no client action is
   * registered. The default action used if this method is not called
   * mimics the behaviour of the calling {@link monq.jfa.DfaRun} for
   * unmatched text.</p>
   */
  public IfContext elsedo(FaAction a) { 
    return ifthen(ELSE, a);
  }
  /**********************************************************************/
  private IfContext copy() {
    IfContext result = new IfContext();
    result.m = (HashMap)((HashMap)m).clone();
    return result;
  }
  /**********************************************************************/
  public FaAction mergeWith(FaAction _other) {
    // first we run the priority base scheme of our superclass. 
    //System.out.println("merging: "+this+"  +  "+_other);
    FaAction result = super.mergeWith(_other);
    if( result!=null ) return result;

    // Either _other does not have a priority scheme or it has the
    // same priority than us.

    // leave the instanceof test to the JVM which has to do it anyway
    // and use a catch instead of an if
    IfContext other;
    try {
      other = (IfContext)_other;
    } catch( ClassCastException e ) {
      // pretend the other object is actually an IfContext with only
      // an else case, and then fall through
      other = new IfContext().elsedo(_other);
    }

    // arriving here, we now that other is also an IfContext
    // object. We must merge the two maps contained.
    IfContext newIf = copy();
    //System.err.println("***"+this+": merging content");

    for(Iterator it=other.m.keySet().iterator(); it.hasNext(); /**/) {
      Object key = it.next();
      FaAction aOther = (FaAction)other.m.get(key);
      FaAction a = (FaAction)newIf.m.get(key);
      if( a==null ) {
	a = aOther;
      } else {
	FaAction tmp = a.mergeWith(aOther);
	if( tmp==null ) tmp = aOther.mergeWith(a);
	if( tmp==null ) {
	  //System.out.println("null for "+a+"  and  "+aOther);
	  return null;
	}
	a = tmp;
      }

      newIf.m.put(key, a);
    }
    return newIf;
  }
    
  /**********************************************************************/
  /**
   * required by interface {@link monq.jfa.FaAction}.
   */
  public void invoke(StringBuffer yytext, int start, DfaRun r) 
    throws CallbackException 
  {
    List stack = ((ContextStackProvider)r.clientData).getStack();
    Context ctx;
    int l = stack.size();
    try {
      ctx = l>0 ? (Context)(stack.get(l-1)) : null;
    } catch( ClassCastException e ) {
      r.unskip(yytext, start);
      throw new CallbackException
	("the context stack was obviously messed up, as it should "
	 +"contain a "
	 +"Context at this point, but it has a "
	 +stack.get(l-1).getClass().getName()+" instead. This happened", e);
    }

//      System.out.println("IfContext: top=`"+cm.top()+"' on `"
//  		       +yytext.substring(start)+"' doing "+a);

    FaAction a = (FaAction)m.get(ctx);
    if( a!=null ) {
      a.invoke(yytext, start, r);
      return;
    }
    a = (FaAction)m.get(ELSE);
    if( a!=null ) {
      a.invoke(yytext, start, r);
      return;
    }

    // simulate behaviour of the DfaRun for non-matching text
    DfaRun.FailedMatchBehaviour fmb = r.getFailedMatchBehaviour();
    if( fmb==DfaRun.UNMATCHED_COPY ) return;
    if( fmb==DfaRun.UNMATCHED_DROP ) {
      yytext.setLength(start);
      return;
    }
    // give back the matching text for a proper error message
    String match = yytext.substring(start);
    r.unskip(yytext, start);
    throw new CallbackException
      ("match `"+match+"' invalid in context "+ctx);
  }
  /**********************************************************************/
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.toString());
    Iterator it = m.keySet().iterator();
    char sep = '[';
    while( it.hasNext() ) {
      Object key = it.next();
      sb.append(sep).append(key).append(':').append(m.get(key));
      sep = ',';
    }
    sb.append(']');
    return sb.toString();
  }
  /**********************************************************************/
}
