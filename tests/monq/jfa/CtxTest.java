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

import monq.jfa.ctx.*;
import monq.jfa.actions.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;

/**
 * exercises monq.jfa.ctx.
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class CtxTest extends TestCase {

  public static void test_first() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);

    // convert everything within <a>...</a> to lowercase and drop any
    // enclosed xml tags.
    Context root = mgr.addXml("a");
    nfa.or("[A-Z]+", new IfContext(root, LowerCase.LOWERCASE))
      .or(Xml.STag(), new IfContext(root, new Drop(-1)))
      .or(Xml.ETag(), new IfContext(root, new Drop(-1)))
      ;
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY));
    r.clientData = ContextManager.createStackProvider();

    String s = r.filter("ABC <x> </x><a>Bla Harald</y> Kirsch<x></a>");
    assertEquals("ABC <x> </x><a>bla harald kirsch</a>", s);
			
  }
  /**********************************************************************/
  public static void test_impossibleMerge() {
    IfContext if1 = new IfContext().elsedo(new Replace("x"));
    IfContext if2 = new IfContext().elsedo(new Replace("y"));
    assertEquals(null, if1.mergeWith(if2));

    // just to silent emma on the toString(), which I do not really
    // intend to test:
    if1.toString();
  }
  /**********************************************************************/
  public static void test_noNullAction() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);
    Context c = mgr.addXml();
    Exception e = null;
    try {
      new IfContext(c, null);
    } catch( NullPointerException _e ) {
      e = _e;
    }
    assertTrue(e instanceof NullPointerException);
  }
  /**********************************************************************/
  public static void test_noReRegister() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);
    Context c = mgr.addXml();
    Exception e = null;
    try {
      new IfContext(c, Drop.DROP).ifthen(c, Drop.DROP);
    } catch( IllegalStateException _e ) {
      e = _e;
    }
    assertTrue(e instanceof IllegalStateException);
  }
  /**********************************************************************/
  public static void test_messedStackInIf() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);
    Context ctx = mgr.addXml("x");
    nfa.or("Z", new IfContext(ctx, new AbstractFaAction() {
	public void invoke(StringBuffer yytext, int start, DfaRun r) {
	  List stack = ((ContextStackProvider)r.clientData).getStack();
	  // just mess it up somehow
	  stack.add(this);
	}
      }));
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP));
    r.clientData = ContextManager.createStackProvider();
    Exception e = null;
    try {
      r.filter("<x>...Z</x>");
    } catch( CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof CallbackException);
    assertTrue(e.getMessage().startsWith("the context stack"));
  }
  /**********************************************************************/
  public static void test_messedStackInPop() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);
    final Context ctx = mgr.addXml("x");
    nfa.or("Z", new IfContext(ctx, new AbstractFaAction() {
	public void invoke(StringBuffer yytext, int start, DfaRun r) {
	  java.util.List stack 
	    = ((ContextStackProvider)r.clientData).getStack();
	  // just mess it up somehow
	  stack.add(ctx);
	}
      }));
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP));
    r.clientData = ContextManager.createStackProvider();
    Exception e = null;
    try {
      r.filter("<x>...Z</x>");
    } catch( CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof CallbackException);
    assertTrue(e.getMessage().startsWith("the context stack"));
  }
  /**********************************************************************/
  public static void test_defaultDropInIf() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);
    Context ctx = mgr.addXml("x").setFMB(DfaRun.UNMATCHED_COPY);

    nfa.or("[a-z]+", new IfContext(ctx, Copy.COPY));
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP));
    r.clientData =  ContextManager.createStackProvider();

    // for the abc inside the <x> IfContext knows what to do because
    // it was defined. However for the leading and trailing it does
    // not know off-hand what to do, but has to fetch the behaviour
    // from the calling DfaRun. And the behaviour define above is
    // DROP. So outside of <x>, the "abc" is dropped *after* being
    // matched, while the ABC is dropped because the DfaRun is
    // configured like that.
    String s = r.filter("abcABC<x>abcABC</x>abc");
    assertEquals("<x>abcABC</x>", s);
  }
  /**********************************************************************/
  // same test as above, but with UNMATCHED_THROW
  public static void test_defaultFailInIf() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);
    Context ctx = mgr.addXml("x").setFMB(DfaRun.UNMATCHED_COPY);

    nfa.or("[a-z]+", new IfContext(ctx, Copy.COPY));
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_THROW));
    r.clientData =  ContextManager.createStackProvider();

    // see test_defaultDropInIf for explanation
    Exception e = null;
    try {
      String s = r.filter("abcABC<x>abcABC</x>abc");
    } catch( CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof CallbackException);
    assertTrue(e.getMessage().startsWith("match `abc' invalid"));
  }
  /**********************************************************************/
  public static void test_startEndAction() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);
    Context ctx = mgr.addXml("x")
      .setStartAction(new Replace("["))
      .setEndAction(new Replace("]"))
      .setFMB(DfaRun.UNMATCHED_COPY)
      ;
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP));
    r.clientData =  ContextManager.createStackProvider();
    String s = r.filter("blabla<x>123456789abc</x>yadaydada");
    assertEquals("[123456789abc]", s);
  }
  /**********************************************************************/
  public static void test_defaultSetters() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa)
      .setDefaultFMB(DfaRun.UNMATCHED_DROP)
      .setDefaultStartAction(new Replace("{"))
      .setDefaultEndAction(new Replace("}"))
      ;

    Context ctx = mgr.addXml("x");
    Context cin = mgr.addXml(ctx, "a")
      .setFMB(DfaRun.UNMATCHED_COPY)
      .setStartAction(new Replace("["))
      .setEndAction(new Replace("]"))
      ;
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY));
    r.clientData =  ContextManager.createStackProvider();
    String s = r.filter("outer <x>deleted <a>ok ok</a> futsch </x> hier");
    assertEquals("outer {[ok ok]} hier", s);
  }
  /**********************************************************************/
  public static void test_hierarchy() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa)
      .setDefaultFMB(DfaRun.UNMATCHED_DROP)
      .setDefaultAction(new Replace("|"))
      ;

    String[] tags = {"x", "y", "a"};
    Context[] ctx = mgr.addXml(null, tags);
    ctx[2]
      .setFMB(DfaRun.UNMATCHED_COPY)
      .setStartAction(new Replace("["))
      .setEndAction(new Replace("]"))
      ;
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY));
    r.clientData =  ContextManager.createStackProvider();
    String s = 
      r.filter("outer <x>del<y>eted <a>ok ok</a> fu</y>tsch </x> hier");
    assertEquals("outer ||[ok ok]|| hier", s);
  }
  /**********************************************************************/
  // It took me a while to notice how to do this:-) Accept only
  // balanced braces.
  public static void test_PumpingLemma() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);

    // This is the basic rule to define a pair of braces. There is no
    // need to restrict the opening brace to any context, because the
    // outermost brace does not need a context.
    mgr.add("{", "}")
      .setStartAction(Drop.DROP)
      .setEndAction(Drop.DROP);

    // However, we define a default rule for the closing brace, i.e. an
    // action which gets called when '}' is not found in a any
    // context. We replace it with an x, so if the output string
    // contains one or more 'x', the parentheses were not balanced.
    nfa.or("}", new Replace("x"));

    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP));
    r.clientData =  ContextManager.createStackProvider();

    // this is balanced
    String s = r.filter("{{}{}{}}");
    assertEquals("", s);

    // another balanced one
    s = r.filter("{{{{{{{{{{}}}}}}}}}}");
    assertEquals("", s);

    // this is not balanced
    s = r.filter("{}}");
    assertEquals("x", s);
  }
  /**********************************************************************/
  public static void test_staticPop() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    ContextManager mgr = new ContextManager(nfa);
    
    // we look at brace-pairs like "{123 ...}" and move the number to
    // the end of the braces
    mgr.add("{[0-9]*", "}")
      .setStartAction(new AbstractFaAction() {
	  public void invoke(StringBuffer yytext, int start, DfaRun r) {
	    List stack = ((ContextStackProvider)r.clientData).getStack();
	    String s = yytext.substring(start+1);
	    // a length of zero allows us to test the EmptyStackException
	    if( s.length()>0 ) stack.add(s);
	    yytext.setLength(start+1);
	  }
	})
      .setEndAction(new AbstractFaAction() {
	  public void invoke(StringBuffer yytext, int start, DfaRun r) {
	    List stack = ((ContextStackProvider)r.clientData).getStack();
	    String s = (String)(ContextManager.pop(stack));
	    yytext.insert(start, s);
	  }
	})
      ;
    
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_COPY));
    r.clientData =  ContextManager.createStackProvider();
    
    String s = r.filter("bla {111 hallo }");
    assertEquals("bla { hallo 111}", s);

    Exception e = null;
    try {
      r.filter("{ }");
    } catch( EmptyStackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof EmptyStackException );
  }
  /**********************************************************************/
  public static void main(String[] argv)   {
    junit.textui.TestRunner.run(new TestSuite(CtxTest.class));
  }

}
