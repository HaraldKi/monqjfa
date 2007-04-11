package monq.jfa.ctx;

import monq.jfa.*;

import java.util.List;

/**
 * <p>manages a context when running a {@link
 * monq.jfa.DfaRun}. A context is the stretch of text between and
 * including the matches of a pair of regular expressions. To add the
 * two regular expressions to an {@link monq.jfa.Nfa} and associate
 * them with a <code>Context</code> object, use a  {@link
 * ContextManager}.</p>
 *
 * <p>The <code>Context</code> object allows for a <em>client
 * action</em> to be 
 * run just before the context is entered and immediatly after the
 * context ends. Their default derives from the defaults set up in the
 * <code>ContextManager</code>. They can be changed with {@link
 * #setStartAction setStartAction()} and {@link #setEndAction
 * setEndAction()}. Normally they are used to handle the text matching
 * the regular expressions defining the context.<p>
 *
 * <p>The client actions may push/pop elements on/from the {@link
 * java.util.List} provided via the {@link ContextStackProvider} that
 * must be available in {@link monq.jfa.DfaRun#clientData} field. Of
 * course the number of elements pushed and popped must match.</p>
 *
 * <p>To define how the <code>DfaRun</code> should handle unmatched
 * text within the context, call {@link #setFMB setFMB()}.</p>
 */  
public class Context extends AbstractFaAction {
  // This is run to work on the matched data
  private FaAction startAction;

  // this will be retrieved and run by a ContextManager.Pop
  private FaAction endAction;

  // this will be set into the DfaRun calling us. If it is null,
  // nothing is changed in the DfaRun
  private DfaRun.FailedMatchBehaviour fmb;

  private String name = null;

  // this will be changed eventually, if pop needs its own nonzero
  // priority set with setPopPriority
  private Pop pop = POP;

  private static final Pop POP = new Pop();

  private static class Pop extends AbstractFaAction {
    public void invoke(StringBuffer yytext, int start, DfaRun r)
      throws CallbackException 
    {
      List stack = ((ContextStackProvider)r.clientData).getStack();      
      Context c;
      DfaRun.FailedMatchBehaviour fmb;
      try {
	int l = stack.size()-1;
	c = (Context)stack.remove(l--);
	fmb = (DfaRun.FailedMatchBehaviour)stack.remove(l--);
      } catch( ClassCastException e ) {
	// we would not need to catch that, but a hint to the user
	// that he messed up the stack is fair enough
	throw new CallbackException
	  ("the context stack was messed up, which is why the wrong "
	   +"type of element was on the top of the stack when trying "
	   +"to pop a context.", e);
      }
//       System.out.println("popping "+c+" on `"+yytext.substring(start)+
// 			 "', running "+c.endAction+
// 			 ", setting to "+fmb);
      

      r.setOnFailedMatch(fmb);
      if( c.endAction!=null ) c.endAction.invoke(yytext, start, r);
    }
  }

  /**
   * <p>no public constructor, only a {@link IfContext} shall be
   * able to create a <code>Context</code>.</p>
   */
  Context() {}
  /**********************************************************************/
  /**
   * <p>sets the name of this object, which is currently only used to
   * have {@link #toString()} return something useful.</p>
   */
  public Context setName(String s) { 
    this.name = s; 
    return this;
  }
  /**
   * <p>returns the name of this <code>Context</code>. If the name was
   * never set, this is <code>null</code>.</p>
   *
   * @return the name which was set with {@link #setName}. 
   */
  public String getName() { return name; }
  /**********************************************************************/
  /**
   * <p>specifies the action which shall take 
   * care of the text marking the start of the context. The default
   * for this action is set up with {@link
   * ContextManager#setDefaultStartAction
   * ContextManager.setDefaultStartAction()}.</p> 
   */
  public Context setStartAction(FaAction a) { 
    this.startAction = a; 
    return this;
  }
  /**
   * <p>specifies the action which takes
   * care of the text marking the end of the context. The default
   * for this action is set up with {@link
   * ContextManager#setDefaultEndAction
   * ContextManager.setDefaultEndAction()}.</p>
   */
  public Context setEndAction(FaAction a) {
    this.endAction = a;
    return this;
  }
  /**
   * <p>specifies how the {@link monq.jfa.DfaRun} should handle
   * non-matching text within the context.
   */
  public Context setFMB(DfaRun.FailedMatchBehaviour fmb) {
    this.fmb = fmb;
    return this;
  }

  /**
   * method used by a {@link ContextManager} to obtain a suitable
   * action object pop this context off the stack. Currently this
   * action is independent of the context.
   */
  FaAction getPop() { return pop; }

  /**
   * <p>this method is not implemented and will always throw an
   * exception. It is only kept as a refence for the rare cases
   * when someone thinks it is necessary to set the priority of
   * the context pop action. This, however, can only be necessary if
   * there is a conflict between matches within the context and the
   * match terminating the context. While this is probably wrong
   * anyway, it is still possible to give the action handling the
   * in-context match a priority different from zero to make it
   * different from the priority of the pop action.
   *
   * @throws UnsupportedOperationException whenever called.
   */
  public Context setPopPriority(int p) {
    // Why this does not work anyway: ContextManager.add() creates a
    // Context object, say ctx, and passes ctx.getPop() as the action
    // to an IfContext. Only then add() returns the
    // Context. Consequently setting the popPriority later would not
    // help, because the priority has to be set on the IfContext
    // actually, not on the Pop. Any solution to get around this is
    // non-trivial. 
    throw new UnsupportedOperationException("read the docs, please");
  }
  /**********************************************************************/
  public void invoke(StringBuffer yytext, int start, DfaRun r) 
    throws CallbackException {
//      System.out.println("pushing "+this+" on `"
//  		       +yytext.substring(start)+"'");

    // run the client action *before* we push the context. This allows
    // the client action to push itself something on the stack while
    // nevertheless making sure that the active context is on the top
    // of the stack when we are done here.
    if( startAction!=null ) startAction.invoke(yytext, start, r);

    List<Object> stack = ((ContextStackProvider)r.clientData).getStack(); 
    stack.add(r.getFailedMatchBehaviour());
    stack.add(this);

    if( fmb!=null ) r.setOnFailedMatch(fmb);
  }

  /**********************************************************************/
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.toString())
      .append('[')
      .append(name)
      .append(',')
      .append(startAction)
      .append(',')
      .append(endAction)
      .append(']')
      ;
    return sb.toString();
  }
  /**********************************************************************/
}
