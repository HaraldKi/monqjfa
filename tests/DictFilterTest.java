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

import monq.jfa.*;
import monq.programs.DictFilter;
import monq.net.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;

/**
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class DictFilterTest extends TestCase {

  private static final String EX1 = 
    "<?xml version='1.0' encoding='iso-8859-1'?>"+
    "<mwt>\n"+
    "<template>[%0](%1)</template>\n"+
    "<t p1='17'>hallo</t>\n"+
    "</mwt>";

  public static void test_1() throws Exception {
    InputStream in = new ByteArrayInputStream(EX1.getBytes("iso-8859-1"));
    DictFilter df = new DictFilter(in, "raw", null, false);
    df.setInputEncoding("UTF-8");
    df.setOutputEncoding("UTF-8");
    in = new ByteArrayInputStream
      ("blurb hallos äöüß".getBytes("UTF-8"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Service svc = df.createService(in, out, null);
    svc.run();
    assertEquals(null, svc.getException());
    String s = out.toString("UTF-8");
    //System.out.println("+++"+s);
    assertEquals("blurb [hallos](17) äöüß", s);
  }

  public static void test_2() throws Exception {
    InputStream in = new ByteArrayInputStream(EX1.getBytes("iso-8859-1"));
    DictFilter df = new DictFilter(in, "xml", null, false);
    df.setInputEncoding("UTF-8");
    df.setOutputEncoding("UTF-8");
    in = new ByteArrayInputStream
      ("blurb <hallo>hallos</hallo> äöüß".getBytes("UTF-8"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Service svc = df.createService(in, out, null);
    svc.run();
    assertEquals(null, svc.getException());
    String s = out.toString("UTF-8");
    //System.out.println("+++"+s);
    assertEquals("blurb <hallo>[hallos](17)</hallo> äöüß", s);
  }

  public static void test_3() throws Exception {
    InputStream in = new ByteArrayInputStream(EX1.getBytes("iso-8859-1"));
    DictFilter df = new DictFilter(in, "elem", "x", false);
    df.setInputEncoding("UTF-8");
    df.setOutputEncoding("UTF-8");
    in = new ByteArrayInputStream
      ("blurb <hallo><x><hallo>hallos</hallo></x><hallo> äöüß"
       .getBytes("UTF-8"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Service svc = df.createService(in, out, null);
    svc.run();
    assertEquals(null, svc.getException());
    String s = out.toString("UTF-8");
    //System.out.println("+++"+s);
    assertEquals("blurb <hallo><x><[hallo](17)>[hallos](17)"+
		 "</[hallo](17)></x><hallo> äöüß", s);
  }
  public static void test_IncompleteMwt() throws Exception {
    Reader rin = new StringReader("<mwt>");
    Exception e = null;
    try {
       new DictFilter(rin, "raw", null, false);
    } catch( monq.jfa.CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof monq.jfa.CallbackException);
    assertTrue(e.getMessage().startsWith("open context `mwt'"));
  }

  public static void test_InputWithWrongEncoding() throws Exception {
    Reader rin = new StringReader(EX1);
    DictFilter df = new DictFilter(rin, "raw", null, false);
    InputStream in = new ByteArrayInputStream
      ("<?xml version='1.0' encoding='blooog'?><x>a</x>"
       .getBytes());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Exception e = null;
    try {
      df.createService(in, out, null);
    } catch( ServiceUnavailException _e ) {
      e = _e;
    }
    assertTrue(e instanceof ServiceUnavailException);
    assertTrue(e.getCause() instanceof UnsupportedEncodingException);
  }

  public static void test_WrongInputEncSpecified() throws Exception {
    Reader rin = new StringReader(EX1);
    DictFilter df = new DictFilter(rin, "raw", null, false);
    df.setInputEncoding("yuck");
    InputStream in = new ByteArrayInputStream
      ("<?xml version='1.0'?><x>a</x>"
       .getBytes());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Exception e = null;
    try {
      df.createService(in, out, null);
    } catch( ServiceCreateException _e ) {
      e = _e;
    }
    assertTrue(e instanceof ServiceCreateException);
    Throwable cause = e.getCause();
    //System.out.println(e.getMessage());
    assertTrue(e.getMessage().startsWith("non-existant input enc"));
    assertTrue(cause instanceof UnsupportedEncodingException);    
  }

  public static void test_WrongOutputEncSpecified() throws Exception {
    Reader rin = new StringReader(EX1);
    DictFilter df = new DictFilter(rin, "raw", null, false);
    df.setOutputEncoding("yuck");
    InputStream in = new ByteArrayInputStream
      ("<?xml version='1.0'?><x>a</x>"
       .getBytes());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Exception e = null;
    try {
      df.createService(in, out, null);
    } catch( ServiceCreateException _e ) {
      e = _e;
    }
    assertTrue(e instanceof ServiceCreateException);
    Throwable cause = e.getCause();
    //System.out.println(e.getMessage());
    assertTrue(cause instanceof UnsupportedEncodingException);
    assertTrue(e.getMessage().startsWith("non-existant output enc"));
  }

  public static void test_IllegArgToConstructor() throws Exception {
    InputStream in = new ByteArrayInputStream(EX1.getBytes("iso-8859-1"));
    Exception e = null;
    try {
      new DictFilter(in, "yuck", "x", false);
    } catch(IllegalArgumentException _e ) {
      e = _e;
    }
    assertTrue(e instanceof IllegalArgumentException);
    assertTrue(e.getMessage().startsWith("`yuck'"));
  }

  public static void test_MissingTemplate() throws Exception {
    StringReader in = new StringReader("<mwt><t>blod</t></mwt>");
    Exception e = null;
    try {
      new DictFilter(in, "raw", null, false);
    } catch(monq.jfa.CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof monq.jfa.CallbackException);
    assertTrue(e.getMessage().startsWith("no <template>"));
    assertTrue(e.getMessage().endsWith("[[<t>blod</t>]]"));
  }

  public static void test_WrongAttrs() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template>%0</template>\n"+
       "<t p1='aaa' blubl='s'>hix</t></mwt>");
    Exception e = null;
    try {
      new DictFilter(in, "raw", null, false);
    } catch(monq.jfa.CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof monq.jfa.CallbackException);
    //e.printStackTrace();
    String s = e.getMessage();
    assertTrue(s.startsWith("superfluous attributes: blubl=s"));
    assertTrue(s.endsWith("[[<t p1='aaa' blubl='s'>hix</t>]]"));
  }

  public static void test_BadRe() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template>xx</template><r>p[</r></mwt>");
    Exception e = null;
    try {
      new DictFilter(in, "raw", null, false);
    } catch(monq.jfa.CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof monq.jfa.CallbackException);
    //e.printStackTrace();
    String s = e.getMessage();
    assertTrue(s.startsWith("regular expression syntax error (see cause)"));
    assertTrue(s.endsWith("[[<r>p[</r>]]"));
  }

  public static void test_TCnotNumber() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template>xx</template><r tc='1a'>p</r></mwt>");
    Exception e = null;
    try {
      new DictFilter(in, "raw", null, false);
    } catch(monq.jfa.CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof monq.jfa.CallbackException);
    //e.printStackTrace();
    String s = e.getMessage();
    assertTrue(s.startsWith("found tc attribute which"));
    assertTrue(s.endsWith("[[<r tc='1a'>p</r>]]"));
  }

  public static void test_TCnegative() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template>xx</template><r tc='-18'>p</r></mwt>");
    Exception e = null;
    try {
      new DictFilter(in, "raw", null, false);
    } catch(monq.jfa.CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof monq.jfa.CallbackException);
    //e.printStackTrace();
    String s = e.getMessage();
    assertTrue(s.startsWith("found negative tc attribute"));
    assertTrue(s.endsWith("[[<r tc='-18'>p</r>]]"));
  }

  public static void test_templateWithAttr() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template x='1'>xx</template><r>p</r></mwt>");
    Exception e = null;
    try {
      new DictFilter(in, "raw", null, false);
    } catch(monq.jfa.CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof monq.jfa.CallbackException);
    //e.printStackTrace();
    String s = e.getMessage();
    assertTrue(s.startsWith("malformed template, attributes not allowed"));
    assertTrue(s.endsWith("[[<template x='1'>xx</template>]]"));
  }

  public static void test_templateBadPrintf() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template>xx%(blorg</template><r>p</r></mwt>");
    Exception e = null;
    try {
      new DictFilter(in, "raw", null, false);
    } catch(monq.jfa.CallbackException _e ) {
      e = _e;
    }
    assertTrue(e instanceof monq.jfa.CallbackException);
    //e.printStackTrace();
    String s = e.getMessage();
    assertTrue(s.startsWith("malformed template content (see cause)"));
    assertTrue(s.endsWith("[[<template>xx%(blorg</template>]]"));
  }

  public static void test_noDefaultWord() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template>[%0]</template>"
       +"<r>harald</r></mwt>");
    DictFilter df = new DictFilter(in, "raw", null, false, false, false);
    DfaRun r = df.createRun();
    String s = r.filter("bla blaharald bla harald bla");
    assertEquals("bla bla[harald] bla [harald] bla", s);
  }

  public static void test_withDefaultWord() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template>[%0]</template>"
       +"<r>harald</r></mwt>");
    DictFilter df = new DictFilter(in, "raw", null, false);
    DfaRun r = df.createRun();
    String s = r.filter("bla blaharald bla harald bla");
    assertEquals("bla blaharald bla [harald] bla", s);
  }

  // After converting character entities  in the template string, it
  // was used with the wrong size which led to very bad results.
  public static void test_templateEntityBug() throws Exception {
    StringReader in = new StringReader
      ("<mwt><template>&lt;%0(1,-1)&gt;</template>"
       +"<r>-harald-</r></mwt>");
    DictFilter df = new DictFilter(in, "raw", null, false);
    DfaRun r = df.createRun();
    String s = r.filter("bla bla-harald- bla -harald- bla");
    //System.err.println("[[["+s+"]]]");
    assertEquals("bla bla<harald> bla <harald> bla", s);
  }

  // to be able to run on the command line
  public static void main(String[] argv) {
    junit.textui.TestRunner.run(new TestSuite(DictFilterTest.class));
  }

}
