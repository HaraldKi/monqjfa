package monq.jfa.ctx;

import monq.jfa.*;

import java.util.*;

/**
 * <p>populates a {@link monq.jfa.Nfa} with regex/action pairs
 * which keep track of nested matching contexts. Objects of this class
 * are expected to be used as follows:</p>
 *
 * <ol>
 * <li>Create the object with a client <code>Nfa</code> to
 * populate.</li>
 * <li>Configure some defaults, if necessary, with the
 * <code>setDefault...</code> methods.</li>
 * <li>Add contexts to the <code>Nfa</code> with any of the
 * <code>add...()</code> methods.</li>
 * <li>Add more regex/action pairs directly to the <code>Nfa</code>,
 * typically with calls like<pre>
 * .or("regex", new IfContext().ifthen(ctx, action))</pre>
 * where <code>ctx</code> is a context returned by a one of the
 * <code>add...()</code> methods.</p>
 * </ol>
 * 
 * <p>Note that changing the defaults on this object will have effect
 * only for subsequent <code>add...()</code> calls.</p>
 *
 */
public class ContextManager {

  // the Nfa we will populate with regex/action pairs
  private Nfa nfa;

  private DfaRun.FailedMatchBehaviour fmb;
  private FaAction startAction;
  private FaAction endAction;

  /**
   * <p>creates an object to populate the given <code>Nfa</code> with
   * regex/action pairs for context matching.</p>
   */
  public ContextManager(Nfa nfa) { this.nfa = nfa; }

  /**
   * <p>defines the default behaviour into which the {@link
   * monq.jfa.DfaRun} is switched, when a context is entered. It
   * defines how unmatched stretches of input shall be handled within
   * the context. The default can be changed for individual {@link
   * Context} objects with their {@link Context#setFMB setFMB()}
   * method.</p>
   * @return this
   */
  public ContextManager setDefaultFMB(DfaRun.FailedMatchBehaviour fmb) {
    this.fmb = fmb;
    return this;
  }

  /**
   * <p>defines the default action to be applied to the text matching
   * a context start. The default can be changed for
   * individual {@link Context} objects with their {@link
   * Context#setFMB setStartAction()} method. If this method is never
   * called or is called with <code>null</code>, the context start
   * text is copied to the output unchanged.</p>
   * @return this
   * @see #setDefaultAction 
   */
  public ContextManager setDefaultStartAction(FaAction a) {
    this.startAction = a;
    return this;
  }
  /**
   * <p>defines the default action to be applied to the text matching
   * a context end. The comments to {@link #setDefaultStartAction
   * setDefaultStartAction()} apply.</p>
   * @return this
   * @see #setDefaultAction 
   */
  public ContextManager setDefaultEndAction(FaAction a) {
    //System.out.println("setting default end action:"+a);
    this.endAction = a;
    return this;
  }

  /**
   * <p>calls both, {@link #setDefaultStartAction setDefaultStartAction()}
   * and {@link #setDefaultEndAction setDefaultEndAction()} with the
   * given action.</p>
   * @return this
   */
  public ContextManager setDefaultAction(FaAction a) {
    this.startAction = a;
    this.endAction = a;
    return this;
  }
  
  /**********************************************************************/
  /**
   * <p>adds a new context to the client <code>Nfa</code>. The context
   * starts whenever the regular expression <code>reIn</code>
   * matches while the given <code>parent</code> context is active. It
   * ends as soon as <code>reOut</code> is found. With
   * <code>parent==null</code> any match of <code>reIn</code> will
   * switch to the new context.</p>
   *
   * <p>The defaults of the context returned will be be initialized
   * from the defaults stored in <code>this</code> with any of the
   * <code>setDefault...</code> methods. They can be changed on the
   * object returned as needed.</p>
   *
   * @param parent may be null to denote any context
   * @param reIn is the regular expression denoting the start of the
   * new context being added to the client <code>Nfa</code>
   * @param reOut is the regular expression denoting the end of the
   * new context being added to the client <code>Nfa</code>
   *
   * @return the context just added to the client
   * <code>Nfa</code>. You need the returned object to change its
   * default behaviour and as a parent parameter for further calls to
   * this method. <b>Do not add the resulting context to any
   * <code>Nfa</code></b>. 
   */
  public Context add(Context parent, String reIn, String reOut)
    throws ReSyntaxException
  {
    // Prepare to switch into this context
    Context ctx = new Context();
    if( startAction!=null ) ctx.setStartAction(startAction);
    if( endAction!=null ) ctx.setEndAction(endAction);
    if( fmb!=null ) ctx.setFMB(fmb);

    // If we have a parent, we need an IfContext action in between
    FaAction a;
    if( parent==null ) a = ctx;
    else a = new IfContext(parent, ctx);
    nfa.or(reIn, a);
    
    // Prepare to switch out again
    nfa.or(reOut, new IfContext(ctx, ctx.getPop()));

    return ctx;
  }
  /**********************************************************************/
  /**
   * <p>calls {@link #add(Context,String,String)
   * add(null,reIn,reOut)}.</p>
   */
  public Context add(String reIn, String reOut)
    throws ReSyntaxException
  {
    return add(null, reIn, reOut);
  }
  /**********************************************************************/
  /**
   * <p>calls {@link #add(Context,String,String)
   * add(parent,reIn,reOut)} such that <code>reIn</code> and
   * <code>reOut</code> match the start and end of the XML element with the
   * given <code>tagname</code>.
   */
  public Context addXml(Context parent, String tagname) 
    throws ReSyntaxException
  {
    Context c = add(parent, Xml.STag(tagname), Xml.ETag(tagname));
    c.setName(tagname);
    return c;
  }
  /**********************************************************************/
  /**
   * <p>calls {@link #add(Context,String,String)
   * add(null,reIn,reOut)} such that <code>reIn</code> and
   * <code>reOut</code> match the start and end of the XML element with the
   * given <code>tagname</code>.</p>
   */
  public Context addXml(String tagname) throws ReSyntaxException
  {
    Context c = add(null, Xml.STag(tagname), Xml.ETag(tagname));
    c.setName(tagname);
    return c;
  }
  /**********************************************************************/
  /**
   * <p>creates a nested hierarchy of contexts defined by the XML
   * tagnames given. The parent context for the element with tagname
   * <code>tagnames[0]</code> will be the given
   * <code>parent</code>. The context for further elements is always
   * the previous context in the list.</p>
   *
   * @return an array of contexts for the given tagnames.
   * @param parent may be <code>null</code>.
   */
  public Context[] addXml(Context parent, String[] tagnames) 
    throws ReSyntaxException 
  {
    int l = tagnames.length;
    Context[] ctx = new Context[l];
    for(int i=0; i<l; i++) {
      ctx[i] = addXml(parent, tagnames[i]);
      parent = ctx[i];
    }
    return ctx;
  }
  /**********************************************************************/
    /**
   * <p>calls {@link #add(Context,String,String)
   * add(null,reIn,reOut)} such that <code>reIn</code> and
   * <code>reOut</code> match the start and end of any XML element.</p>
   */

  public Context addXml() throws ReSyntaxException
  {
    Context all = add(null, Xml.STag(), Xml.ETag());
    all.setPriority(-1);
    //all.setPopPriority(-1);
    all.setName("#all#");
    return all;
  }
  /**********************************************************************/
  /**
   * <p>is a convenience method to help client actions to operate the
   * {@link java.util.List} provided by the {@link ContextStackProvider}
   * via {@link DfaRun#clientData} as a stack.</p>
   *
   * <p>A push method is not provided because it would be a only an
   * alias for 
   * {@link java.util.List#add List.add()}.</p>
   */
  public static Object pop(List stack) {
    int l = stack.size();
    if( l==0 ) throw new java.util.EmptyStackException();
    return stack.remove(l-1);
  }
  /**********************************************************************/
  /**
   * <p>is a convenience method to create a sufficient implementation
   * of a {@link ContextStackProvider}.</p>
   */
  public static Object createStackProvider() {
    return new Csp();
  }
  private static class Csp implements ContextStackProvider {
    private List<Object> stack = new ArrayList<Object>();
    public List<Object> getStack() { return stack; }
  }
}
