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


import monq.jfa.actions.*;
import monq.jfa.xml.*;

import java.util.Map;
import java.util.Arrays;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class XmlTest extends TestCase {

  private FaAction h = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun r) {
	Map m = Xml.splitElement(yytext, start);
	yytext.setLength(start);
	String[] keys = (String[])m.keySet().toArray(new String[0]);
	Arrays.sort(keys);
	yytext.append(keys.length);
	for(int i=0; i<keys.length; i++) {
	  yytext.append('.')
	      .append(keys[i])
	    .append('.')
	    .append(m.get(keys[i]))
	      ;
	}
      }
    };

  public void test_STag_1() throws Exception {
    DfaRun r = 
      new Nfa(Xml.STag(), h)
      .compile(DfaRun.UNMATCHED_DROP)
      .createRun()
      ;
    String s = r.filter("<abc x='1' y=\"2\">");
    assertEquals("3.<.abc.x.1.y.2", s);
  }
  public void test_EmptyElemTag_1() throws Exception {
    DfaRun r = 
      new Nfa(Xml.EmptyElemTag(), h)
      .compile(DfaRun.UNMATCHED_DROP)
      .createRun()
      ;
    String s = r.filter("<abc x='1' y=\"2\"/>");
    assertEquals("3.<.abc.x.1.y.2", s);
  }
  public void test_GoofedElement_1()
    throws CompileDfaException, java.io.IOException, ReSyntaxException
  {
    DfaRun r = 
      new Nfa(Xml.GoofedElement("abc"), h)
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    String s = r.filter("0123456789|<abc x='1' y=\"2\">[jock]</abc>");
    assertEquals("0123456789|4.<.abc.>.[jock].x.1.y.2", s);
  }

  // try to force a bug once found where ElementSplitter.split did not
  // add start to stag.splitX()
  public void test_GoofedElement_2() 
    throws CompileDfaException, java.io.IOException, ReSyntaxException
  {
    DfaRun r = new Nfa(Xml.GoofedElement("abc"), h)
      .compile(DfaRun.UNMATCHED_DROP)
      .createRun()
      ;

    // the following triggered an exception before I fixed the bug
    String s = r.filter("<abc>b</abc> <abc>w</abc>");
    assertEquals("2.<.abc.>.b2.<.abc.>.w", s);
  }
	
  public void test_ETag_1() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    DfaRun r =
      new Nfa(Xml.ETag("abc"), Drop.DROP)
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    assertEquals("55", r.filter("5</abc>5"));
    assertEquals("55", r.filter("5</abc >5"));
    assertEquals("55", r.filter("5</abc\n>5"));
  }
  public void test_ETag_2() 
    throws ReSyntaxException, CompileDfaException, java.io.IOException
  {
    DfaRun r =
      new Nfa(Xml.ETag(), Drop.DROP)
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    assertEquals("55", r.filter("5</abc>5"));
    assertEquals("55", r.filter("5</abc >5"));
    assertEquals("55", r.filter("5</abc\n>5"));
    assertEquals("55", r.filter("5</_bc>5"));
    assertEquals("55", r.filter("5</a:abc>5"));
    assertEquals("55", r.filter("5</abc-d>5"));
    assertEquals("55", r.filter("5</özgüräß>5"));

    // lets pick some unicode more or less randomly
    // BaseChar introducing the tag
    assertEquals("55", r.filter("5</\u03de>5"));
    // Ideographic 
    assertEquals("55", r.filter("5</\u3022>5"));
    // Digit within tagname
    assertEquals("55", r.filter("5</A\u0D66b>5"));
    // CombiningChar within tagname
    assertEquals("55", r.filter("5</A\u0ac8b>5"));
    // Extender within tagname
    assertEquals("55", r.filter("5</A\u3033X>5"));

    // a tiny little cross check with a character which should not
    // appear in a tag name
    assertEquals("5</A\u309bX>5", r.filter("5</A\u309bX>5"));

  }
  /**********************************************************************/
  public void test_StdCharEntities() throws Exception {
    String s = 
      StdCharEntities.toChar(" &amp; &quot; &apos; &lt; &gt; ");
    assertEquals(" & \" ' < > ", s);

    s = StdCharEntities.toEntities(s);
    assertEquals(" &amp; &quot; &apos; &lt; &gt; ", s);
  }
  /**********************************************************************/
//   public void test_EmptyTagName() {
//     Exception e = null;
//     try {
//       Xml.STag("");
//     } catch( IllegalArgumentException _e ) {
//       e = _e;
//     }
//     //System.out.println(e);
//     assertTrue(e instanceof IllegalArgumentException);
//   }
  /********************************************************************/
  public void test_splitToHash1() throws Exception {
    String s = new 
      Nfa(Xml.STag()+Xml.S+"?", new AbstractFaAction() {
	  public void invoke(StringBuffer yytext, int start, DfaRun r) {
	    Map m = Xml.splitElement(yytext, start);
	    yytext.setLength(start);
	    yytext.append("tagname is `"+m.get("<")+"', ");
	    yytext.append("ößgür is `"+m.get("ößgür")+"', ");
	    yytext.append("x is `"+m.get("x")+"'");
	  }
	})
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      .filter("<böller    \n ößgür='hallo' x = '11' >   ")
      ;
    //System.out.println(s+"<<");
    assertEquals("tagname is `böller', ößgür is `hallo', x is `11'", s);
  }
  /**********************************************************************/
  public void test_splitToHash2() throws Exception {
    DfaRun r = new
      Nfa(Xml.GoofedElement("bla")+"|"+Xml.EmptyElemTag("bla"), new AbstractFaAction() {
	  public void invoke(StringBuffer yytext, int start, DfaRun r) {
	    Map m = Xml.splitElement(yytext, start);
	    yytext.setLength(start);
	    yytext.append("tagname is `"+m.get("<")+"', ");
	    yytext.append("ößgür is `"+m.get("ößgür")+"', ");
	    yytext.append("content is `"+m.get(">")+"'");
	  }
	})
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    String s = 
      r.filter("<bla    \n ößgür='hallo'  >  <x>ÄÖÜ</x></bla  >")
      ;
    assertEquals("tagname is `bla', ößgür is `hallo', "+
		 "content is `  <x>ÄÖÜ</x>'", s);

    s = r.filter("<bla />");
    assertEquals("tagname is `bla', ößgür is `null', content is `null'",
		 s);
    
  }
  /**********************************************************************/
  public static void test_XMLDecl() throws Exception {
    DfaRun r = new 
      Nfa(Xml.XMLDecl, new AbstractFaAction() {
	  public void invoke(StringBuffer yytext, int start, DfaRun r) {
	    Map m = Xml.splitElement(yytext, start);
	    yytext.setLength(start);
	    yytext.append("tag=").append(m.get(Xml.TAGNAME))
	      .append(" version=").append(m.get("version"))
	      .append(" encoding=").append(m.get("encoding"))
	      .append(" standalone=").append(m.get("standalone"))
	      ;
	  }
	})
      .compile(DfaRun.UNMATCHED_COPY)
      .createRun()
      ;
    String s =
      r.filter("<?xml version='1.0' ?>");
    assertEquals("tag=xml version=1.0 encoding=null standalone=null", s);
    
    s = r.filter("<?xml version=\"1.0\" encoding='gorbo744'?>");
    assertEquals("tag=xml version=1.0 encoding=gorbo744 standalone=null", s);
    s = r.filter("<?xml version=\"1.0\" encoding='gorbo744' "
		 +"standalone='yes'?>");
    assertEquals("tag=xml version=1.0 encoding=gorbo744 "
		 +"standalone=yes", s);

    // this should not be matched, because xstandalone is wrong
    s = r.filter("<?xml version=\"1.0\" encoding='gorbo744' "
		 +"xstandalone='yes'?>");
    assertEquals("<?xml version=\"1.0\" encoding='gorbo744' "
		 +"xstandalone='yes'?>", s);
  }
  /**********************************************************************/
  public static void main(String[] argv)   {
    // Fa fa = new Fa();
    junit.textui.TestRunner.run(new TestSuite(XmlTest.class));
  }
}
