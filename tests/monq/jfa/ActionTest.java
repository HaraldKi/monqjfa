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

//import monq.jfa.*;
import monq.jfa.actions.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map;
import java.util.HashMap;
/**
 * Test a bunch of monq.jfa.actions.*.
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class ActionTest extends TestCase {

  // shut up clover on Copy
  public void test_Copy() {
    assertEquals("Copy", Copy.COPY.toString());
  }
  // same for Drop
  public void test_Drop() {
    assertEquals("Drop", Drop.DROP.toString());
  }
  /**********************************************************************/
  public void test_Embed() throws Exception {
    Embed emb;
    String s = new
      Nfa("[a-z]", emb=new Embed("a", "bb"))
      .compile(DfaRun.UNMATCHED_THROW)
      .createRun()
      .filter("xy")
      ;
    assertEquals("axbbaybb", s);
    String t = emb.toString();
    assertEquals("[\"a\", \"bb\"]", t.substring(t.length()-11));
  }
  /**********************************************************************/
  public void test_Replace() throws Exception {
    Replace a;
    String s = new
      Nfa("[a-z]", a=new Replace("0"))
      .compile(DfaRun.UNMATCHED_THROW)
      .createRun()
      .filter("xy")
      ;
    assertEquals("00", s);
    String t = a.toString();
    assertEquals("[\"0\"]", t.substring(t.length()-5));
  }
  /**********************************************************************/
  public void test_Casechange() throws Exception {
    String s = new
      Nfa("[a-z‰ˆ¸ÈË]", UpperCase.UPPERCASE)
      .or("[A-Zƒ÷‹…»]", LowerCase.LOWERCASE)
      .or("[a-z][0-9]*", new UpperCase(-1))
      .or("[A-Z][0-9]*", new LowerCase(-1))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("abcdefghijklmnopqrstuvwxyz‰ˆ¸ÈË "
	      +"-- ABCDEFGHIJKLMNOPQRSTUVWXYZƒ÷‹…»"
	      +"-- a0B1")
      ;
    assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZƒ÷‹…» "
		 +"-- abcdefghijklmnopqrstuvwxyz‰ˆ¸ÈË"
		 +"-- A0b1", s);
  }
  /**********************************************************************/
  public void test_Run() throws Exception {
    Run run = null;
    String pre="", post="";
    Embed[] emb = new Embed[10];
    for(int i=0; i<10; i++) {
      emb[i] = new Embed(new String(""+(char)('a'+i)), 
			 new String(""+(char)('A'+i)));
    }
    for(int i=0; i<10; i++) {
      switch( i ) {
      case 0:
	pre="a"; 
	post="A"; 
	run = new Run(emb[0]);
	break;
      case 1:
	pre="ab"; 
	post="BA"; 
	run = new Run(emb[1], emb[0]);
	break;
      case 3:
	pre="abc"; 
	post="CBA"; 
	run = new Run(emb[2], emb[1], emb[0]);
	break;
      case 4:
	pre="abcd"; 
	post="DCBA"; 
	run = new Run(emb[3], emb[2], emb[1], emb[0]);
	break;
      default: 
	pre = "";
	post = "";
	for(int j=0; j<i; j++) {
	  if( j==0 ) run = new Run(emb[i-1]);
	  else run.add(emb[i-j-1]);		      
	  pre = pre+((char)(j+'a'));
	  post = ""+((char)(j+'A'))+post;
	}
      }
      String s = new 
	Nfa("0", run)
	.compile(DfaRun.UNMATCHED_THROW)
	.createRun()
	.filter("0")
	;
      assertEquals(pre+"0"+post, s);
    }
  }
  /**********************************************************************/
  public void test_SearchReplace1() throws Exception {
    String s = new
      Nfa(Xml.GoofedElement("(X|Y)"), 
	  new SearchReplace("<(!/?[A-Z]+)>", "[%1]"))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("<X>bla</X>, <Y>bli</Y>")
      ;
    assertEquals("[X]bla[/X], [Y]bli[/Y]", s);	  
  }
  
  // here we try to limit the number of replacements done
  public void test_SearchReplace2() throws Exception {
    String s = new
      Nfa("[0-9]+", new SearchReplace("[0-9]", "x", 3))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("0123456789 012;")
      ;
    assertEquals("xxx3456789 xxx;", s);
  }

  // here with an explicit splitter
  public void test_SearchReplace3() throws Exception {
    TextSplitter sp = new RegexpSplitter("/", RegexpSplitter.SPLIT);
    Formatter fmt = new PrintfFormatter("%3/%2/%1");
    String s = new
      Nfa("[^\n]+", new SearchReplace("[0-9]+/[0-9]+/[0-9]+", 
				      sp, fmt, -1))
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("0/1/2 1/2 1/2/3 33/44 345/456/567")
      ;
    assertEquals("2/1/0 1/2 3/2/1 33/44 567/456/345", s);
  }
  /**********************************************************************/
  public static void test_Count() throws Exception {
    // If we count three or more b between brackets, we deliver,
    // otherwise we drop
    Hold h = new Hold();
    Count c = new Count("ccc");
    DfaRun r = new 
      Nfa("\\[", new Run(h, c.reset()))
      .or("b", c)
      .or("bb", c.add(2))
      .or("\\]", new If(c.ge(3), 
			new Run(new Printf("(%(ccc))]"), h.ship()), 
			h.drop()))
      .compile(DfaRun.UNMATCHED_DROP)
      .createRun()
      ;
    MapProvider mp;
    r.clientData = mp = new MapProvider() {
	Map m = new HashMap();
	public Map getMap() { return m; }
      };
    String s = r.filter("[a] [aba] [aabb] [abbba] [abbbbbb]");
    assertEquals("[bbb(3)][bbbbbb(6)]", s);
    assertEquals(6, c.getValue(r));
  }
  /**********************************************************************/
  public static void test_HoldGetStart() throws Exception {
    final Hold h = new Hold();
    DfaRun r = new 
      Nfa("A", h)
      .or("b", new AbstractFaAction() {
	  private Hold hh = h;
	  public void invoke(StringBuffer yytext, int start, DfaRun r) {
	    yytext.insert(hh.getStart(r), "[");
	    yytext.append(']');
	  }
	})
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun();
    MapProvider mp;
    r.clientData = mp = new MapProvider() {
	Map m = new HashMap();
	public Map getMap() { return m; }
      };
    String s = r.filter("...A-----b...");
    assertEquals("...[A-----b]...", s);
  }
  /**********************************************************************/
  public static void test_throwAtEndOfHold() throws Exception {
    Hold h = new Hold();
    DfaRun r = new 
      Nfa("xxxx", h.ship())
      .compile(DfaRun.UNMATCHED_DROP)
      .createRun();
    MapProvider mp;
    r.clientData = mp = new MapProvider() {
	Map m = new HashMap();
	public Map getMap() { return m; }
      };

    Exception e = null;
    Throwable cause = null;
    try {
      r.filter("xxxx");
    } catch( CallbackException _e ) {
      e = _e;
      cause = e.getCause();
    }
    String s = e.getMessage();
    assertTrue(e instanceof CallbackException);
    assertTrue(cause instanceof IllegalStateException);
    assertTrue(s.startsWith("Hold not active"));
    assertTrue(s.endsWith("[[xxxx]]"));
	       
    //e.printStackTrace();
    //assertEquals("Hold not active when looking at `xxxx[EOF]'", s);
  }
  /**********************************************************************/
  public static void test_SwitchDfa() throws Exception {
    SwitchDfa toWork = new SwitchDfa(Drop.DROP);
    SwitchDfa toEnv = new SwitchDfa(Drop.DROP);
    SwitchDfa toDoLower = new SwitchDfa();
    Dfa work = new Nfa("[a-z]+", UpperCase.UPPERCASE)
      .or("</xxx>", toEnv)
      .or("<lower />", toDoLower)
      .compile(DfaRun.UNMATCHED_COPY);
    Dfa lower = new Nfa("[A-Z]+", LowerCase.LOWERCASE)
      .or("</xxx>", toEnv)
      .compile(DfaRun.UNMATCHED_COPY);
    Dfa env = new Nfa("<xxx>", toWork)
      .compile(DfaRun.UNMATCHED_DROP);
    toWork.setDfa(work);
    toEnv.setDfa(env);
    toDoLower.setDfa(lower);

    DfaRun r = new DfaRun(env);
    String s = "blabla<xxx>bla BLA 123 bla</xxx> bla "+
      "<xxx>aa<lower />blUrb BLA</xxx>blurb";
    r.setIn(s);
    s = r.filter(s);
    assertEquals("BLA BLA 123 BLAAA<lower />blurb bla", s);
  }
  /**********************************************************************/
  public static void test_TailContext() throws Exception {
    DfaRun r = new
      Nfa("[0-9]+abc", new TailContextN(3, new Embed("[", "]")))
      .compile(DfaRun.UNMATCHED_DROP)
      .createRun();
    String s = r.filter("...99...123abc...");
    assertEquals("[123]", s);
  }
  /**********************************************************************/
  public static void test_TailContext2() throws Exception {
    DfaRun r = new
      Nfa("[0-9]+abc", new TailContextN(3))
      .compile(DfaRun.UNMATCHED_DROP)
      .createRun();
    String s = r.filter("...99...123abc...");
    assertEquals("123", s);
  }  
  /**********************************************************************/
  public static void main(String[] argv) {
    junit.textui.TestRunner.run(new TestSuite(ActionTest.class));
  }

}
 
