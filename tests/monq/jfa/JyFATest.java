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

import org.python.util.PythonInterpreter; 

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;
import java.io.*;
/**
 * exercises monq.jfa.JyFA.
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class JyFATest extends TestCase {
  private static final String tmpdir 
    = System.getProperty("java.io.tmpdir");

  private PythonInterpreter pi;
  private File scriptFile;
  private String pyModuleName;

  // counter for test python files. Yeah, this is static because a new
  // object is constructed for each test, why?
  private static int i=0;

  public void setUp() {
    pyModuleName = "jyfatest"+(i++);
    //System.err.println(pyModuleName);

    // create a temporary file to be used for the script. Use another
    // file numer for each invokation of setUp.
    scriptFile = new File(tmpdir, pyModuleName+".py");
    scriptFile.deleteOnExit();

    // make an interpreter with a slightly extended search path
    pi = new PythonInterpreter();
    pi.exec("import sys");
    pi.exec("sys.path.append('"+tmpdir+"')");
  }
  /**********************************************************************/
  public void test_basic() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print
      ("from monq.jfa.actions import Printf\n"+
       "from java.lang import String\n"+
       "def populate(nfa): #{\n"+
       "  nfa.or(String('(![a-z]+)=\"(![^\"]+)\"'), "+
       "         Printf(1, '[[%1]] is [[%2]]'))\n"+
       "#}\n");
    out.close();
    JyFA jy = new JyFA(pyModuleName);
    DfaRun r = jy.createRun();
    String s = r.filter("bla bla xyz=\"123\" blabla");
    assertEquals("bla bla [[xyz]] is [[123]] blabla", s);

  }
  /**********************************************************************/
  // same test as above, but this time we use the filename, not the
  // module name
  public void test_basicWithFilename() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print
      ("from monq.jfa.actions import Printf\n"+
       "from java.lang import String\n"+
       "def populate(nfa): #{\n"+
       "  nfa.or(String('(![a-z]+)=\"(![^\"]+)\"'), "+
       "         Printf(1, '[[%1]] is [[%2]]'))\n"+
       "#}\n");
    out.close();
    JyFA jy = new JyFA(scriptFile.getPath());
    DfaRun r = jy.createRun();
    String s = r.filter("bla bla xyz=\"123\" blabla");
    assertEquals("bla bla [[xyz]] is [[123]] blabla", s);

  }
  /**********************************************************************/
  public void test_drop() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print
      ("from monq.jfa.actions import Printf\n"+
       "from monq.jfa import DfaRun\n"+
       "from java.lang import String\n"+
       "onFailedMatch = DfaRun.UNMATCHED_DROP\n"+
       "def populate(nfa): #{\n"+
       "  nfa.or(String('(![a-z]+)=\"(![^\"]+)\"'),\n"+
       "         Printf(1, '[[%1]] is [[%2]]'))\n"+
       "#}\n");
    out.close();
    JyFA jy = new JyFA(pyModuleName);
    DfaRun r = jy.createRun();
    String s = r.filter("bla bla xyz=\"123\" blabla");
    assertEquals("[[xyz]] is [[123]]", s);

  }
  /**********************************************************************/
  public static final class MP implements monq.jfa.actions.MapProvider {
    public Map m = new HashMap();
    public Map getMap() { return m; }
  }
  /**********************************************************************/
  public void test_getCD() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print
      ("from monq.jfa.actions import Printf\n"+
       "from monq.jfa import DfaRun\n"+
       "from java.lang import String\n"+
       "from monq.jfa import JyFATest\n"+
       "def getClientData():\n"+
       "  mp = JyFATest.MP()\n"+
       "  mp.getMap().put('value', 'xxx')\n"+
       "  return mp\n"+
       "def populate(nfa): #{\n"+
       "  nfa.or(String('HaLLO'), Printf('[[%(value)]]'))\n"+
       "#}\n");
    out.close();
    JyFA jy = new JyFA(pyModuleName);
    DfaRun r = jy.createRun();
    String s = r.filter("bla bla HaLLO bla bla");
    assertEquals("bla bla [[xxx]] bla bla", s);

  }
  /**********************************************************************/
  public void test_eofAction() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print
      ("from monq.jfa.actions import Embed, Replace\n"+
       "from java.lang import String\n"+
       "eofAction = Replace('... und Tschüß')\n"+
       "def populate(nfa): #{\n"+
       "  nfa.or(String('[a-z]+'), Embed('<i>', '</i>'))\n"+
       "#}\n");
    out.close();
    JyFA jy = new JyFA(pyModuleName);
    DfaRun r = jy.createRun();
    String s = r.filter("aaa bbb 888");
    assertEquals("<i>aaa</i> <i>bbb</i> 888... und Tschüß", s);
  }
  /**********************************************************************/
  public void test_populateMissing() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print("# this module is just empty\n");
    out.close();
    Exception e = null;
    try {
      new JyFA(pyModuleName);
    } catch( IOException ex ) {
      e = ex;
    }
    assertTrue( e instanceof IOException );    
    assertTrue(e.getMessage().startsWith("method `populate'"));
  }
  /**********************************************************************/
  public void test_generalPyError() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print("a = 1+xaxax\n");
    out.close();
    Exception e = null;
    try {
      new JyFA(pyModuleName);
    } catch( IOException ex ) {
      e = ex;
    }
    assertTrue( e instanceof IOException );    
    assertTrue(e.getMessage().startsWith("could not load/import "));
    e = (Exception)e.getCause();
    assertTrue(e.toString().indexOf("NameError: xaxax")>0);
  }
  /**********************************************************************/
  public void test_wrongTypePopulate() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print("populate = 1\n");
    out.close();
    Exception e = null;
    try {
      new JyFA(pyModuleName);
    } catch( IOException ex ) {
      e = ex;
    }
    assertTrue( e instanceof IOException );    
    assertTrue(e.getMessage().startsWith("method `populate' is not a f"));
  }
  /**********************************************************************/
  public void test_wrongTypeFMB() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print("def populate(nfa): pass\n"+
	      "onFailedMatch = 18;  # this is wrong, of course\n");
    out.close();
    Exception e = null;
    try {
      new JyFA(pyModuleName);
    } catch( IOException ex ) {
      e = ex;
    }
    assertTrue( e instanceof IOException );
    assertTrue(e.getMessage().startsWith("onFailedMatch is not"));    
  }
  /**********************************************************************/
  public void test_wrongTypeFaAction() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print("def populate(nfa): pass\n"+
	      "eofAction = 18;  # this is wrong, of course\n");
    out.close();
    Exception e = null;
    try {
      new JyFA(pyModuleName);
    } catch( IOException ex ) {
      e = ex;
    }
    assertTrue(e instanceof IOException );
    assertTrue(e.getMessage().startsWith("eofAction is not"));    
  }
  /**********************************************************************/
  public void test_roiClashFmb() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print("from monq.jfa import Roi, DfaRun\n"+
	      "def populate(nfa): pass\n"+
	      "roi = Roi('bla', 'bla')\n"+
	      "onFailedMatch = DfaRun.UNMATCHED_DROP\n");
        out.close();
    Exception e = null;
    try {
      new JyFA(pyModuleName);
    } catch( IOException ex ) {
      e = ex;
    }
    assertTrue(e instanceof IOException );
    assertTrue(e.getMessage().startsWith("roi and onFailedMatch"));
  }
  /**********************************************************************/
  public void test_roiClashEofAction() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print("from monq.jfa import Roi, DfaRun\n"+
	      "from monq.jfa.actions import Drop\n"+
	      "def populate(nfa): pass\n"+
	      "roi = Roi('bla', 'bla')\n"+
	      "eofAction = Drop.DROP\n");
    out.close();
    Exception e = null;
    try {
      new JyFA(pyModuleName);
    } catch( IOException ex ) {
      e = ex;
    }
    assertTrue(e instanceof IOException );
    assertTrue(e.getMessage().startsWith("roi and eofAction"));
  }
  /**********************************************************************/
  public void test_WithRoi() throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(scriptFile));
    out.print("from monq.jfa import Roi, DfaRun\n"+
	      "from monq.jfa.actions import Embed\n"+
	      "from java.lang import String\n"+
	      "def populate(nfa):\n"+
	      "  nfa.or(String(\"[0-9]+\"), Embed('<', '>'))\n"+
	      "roi = Roi('<x>', '</x>')\n");
    out.close();
    JyFA jy = new JyFA(pyModuleName);
    DfaRun r = jy.createRun();
    String s = r.filter("123 <x>bla 123 bli 44</x> 9");
    assertEquals("123 <x>bla <123> bli <44></x> 9", s);
  }
  /**********************************************************************/
  public static void main(String[] argv)   {
    junit.textui.TestRunner.run(new TestSuite(JyFATest.class));
  }

}
