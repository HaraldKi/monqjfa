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

package monq.clifj;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;
/**
 * Test a bunch of monq.jfa.actions.*.
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class CommandlineTest extends TestCase {

  private static final Exception parse(Commandline cmd, String[] argv) {
    try {
      cmd.parse(argv);
    } catch( CommandlineException e) {
      return e;
    }
    return null;
  }
  /*+********************************************************************/
  public void test_Basic() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    Exception e = parse(cmd, new String[0]);
    assertNull(e);
  }
  /*+********************************************************************/
  public void test_BasicException() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    String[] argv = {"bla"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().startsWith("TestProg: non-option"));
  }
  /*+********************************************************************/
  public void test_minmaxWrong() {
    Exception ex = null;
    try {
      new Commandline("TestProg", "do the test", "all", "the rest", 22, 2);
    } catch( Exception e) {
      ex = e;
    }
    assertNotNull(ex);
    assertTrue(ex instanceof IllegalArgumentException);
  }
  /*+********************************************************************/
  public void test_BasicFullException() {
    Commandline cmd = 
      new Commandline("TestProg", "do the test", "rest", "the rest", 1, 2);
    String[] argv = {"furp1", "glurp2", "blarg3", "murf4"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    //e.printStackTrace();
    assertTrue(e.getMessage().contains("want no more than 2"));
    assertTrue(e.getMessage().contains("found 4"));
  }

  /*+********************************************************************/
  public void test_BasicFullException2() {
    Commandline cmd = 
      new Commandline("TestProg", "do the test", "rest", "the rest", 1, 2);
    String[] argv = {};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    //e.printStackTrace();
    assertTrue(e.getMessage().contains("but want 1"));
    assertTrue(e.getMessage().contains("found 0"));
  }

  /*+********************************************************************/
  public void test_explicitRest() {
    Commandline cmd = 
      new Commandline("TestProg", "do the test", "rest", "the rest", 1, 3);
    String[] argv = {"furp1", "--", "glurp2", "blarg3"};
    Exception e = parse(cmd, argv);
    assertNull(e);
    String[] rest = cmd.getStringValues("--");
    assertEquals(argv[0], rest[0]);
    assertEquals(argv[2], rest[1]);
    assertEquals(argv[3], rest[2]);
  }
  /*+********************************************************************/
  public void test_unknownOption() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    String[] argv = {"-x", "hallo"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().contains("unknown option `-x'"));
  }
  /* +******************************************************************* */
  public void test_required() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    cmd.addOption(new Option("-a", "anopt", 
        "test an option", 0, 19999).required());
    String[] argv = { "-a", "blarg", "varg" };
    Exception e = parse(cmd, argv);
    assertNull(e);
    String[] anopt = cmd.getStringValues("-a");
    assertEquals(argv[1], anopt[0]);
    assertEquals(argv[2], anopt[1]);
  }
  /*+********************************************************************/
  public void test_requiredFail() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    cmd.addOption(new Option("-a", "anopt", 
        "test an option", 0, 19999).required());
    String[] argv = {};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().contains("required option(s)"));
    assertTrue(e.getMessage().contains("are missing"));
    assertTrue(e.getMessage().contains("`-a'"));
  }
  /*+********************************************************************/
  public void test_requiredFail2() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    cmd.addOption(new Option("-a", "anopt", 
        "test an option", 0, 19999).required());
    cmd.addOption(new Option("-b", "anopt", 
        "test an option", 0, 19999).required());
    String[] argv = {};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().contains("required option(s)"));
    assertTrue(e.getMessage().contains("are missing"));
    assertTrue(e.getMessage().contains("`-a'"));
    assertTrue(e.getMessage().contains("`-b'"));
  }
  /*+********************************************************************/
  public void testAvailable() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    cmd.addOption(new BooleanOption("-v", "verbose"));
    cmd.addOption(new BooleanOption("-w", "wwwww"));
    cmd.addOption(new Option("-x", "xxxxopt", "do the x", 0, 1));
    String[] argv = {"-v", "-x", "jocko"};
    Exception e = parse(cmd, argv);
    assertNull(e);
    assertTrue(cmd.available("-v"));
    assertTrue(cmd.available("-x"));
    assertFalse(cmd.available("-w"));
  }
  /*+********************************************************************/
  public void testAvailOnUndefined() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    String[] argv = {};
    Exception e = parse(cmd, argv);
    assertNull(e);
    try {
      cmd.available("-x");
    } catch( NullPointerException ex) {
      e = ex;
    }
    assertNotNull(e);
    assertTrue( e instanceof NullPointerException );
  }
  /*+********************************************************************/
  public void testDoubleDefined() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    cmd.addOption(new BooleanOption("-v", "verbose"));
    Exception ex = null;
    try {
      cmd.addOption(new BooleanOption("-v", "verbose"));
    } catch( Exception e ) {
      ex = e;
    }
    assertNotNull(ex);
    assertTrue(ex instanceof IllegalArgumentException);
    assertTrue(ex.getMessage().contains("-v already specified"));
  }
  /* +******************************************************************* */
  public void testGetValueMethods() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    cmd.addOption(new LongOption("-l", "longstuff", "do the long trick", 1, 5,
                                 10, 20));
    String[] argv = {"-l", "12", "13", "18"};
    Exception e = parse(cmd, argv);
    assertNull(e);
    assertEquals(new Long(12), (Long)cmd.getValue("-l"));
    assertEquals(12L, cmd.getLongValue("-l"));

    Vector values = cmd.getValues("-l");
    assertEquals(3, values.size());
    for(int i=0; i<3; i++) {
      Object o = values.get(i);
      assertEquals(new Long(argv[i+1]), o);
    }

    long[] vl = cmd.getLongValues("-l");
    assertEquals(3, vl.length);
    for(int i=0; i<3; i++) {
      assertEquals(Long.parseLong(argv[i+1]), vl[i]);
    }
  }
  /*+********************************************************************/
  public void testGetValueMethods2() {
    Commandline cmd = new Commandline("TestProg", "do the test");
    cmd.addOption(new Option("-s", "stringstuff", "do the long trick", 1, 5));
    String[] argv = {"-s", "bla", "ri", "lulatsch"};
    Exception e = parse(cmd, argv);
    assertNull(e);
    assertEquals(argv[1], cmd.getStringValue("-s"));
  }
  /* +******************************************************************* */
  public void testUsage() throws Exception {
    Commandline cmd = new Commandline("TestProg", "do the test", "rest",
        "all non-option params", 3, 99);
    cmd.addOption(new BooleanOption("-v", "verbose"));
    cmd.addOption(new Option("-f", "input", "all input files", 1,
                             Integer.MAX_VALUE));
    Long dflt[] = {-1L, 12L};
    cmd.addOption(new LongOption("-l", "weight", 
                                 "weight vector to cast the blaselfaster "+ 
                                 "into the muckujacker and obtain "+ 
                                 "the shnooodoodelbom", 0, 4, -10, 20, dflt));
    String[] argv = {"-h"};
    Exception e = parse(cmd, argv);
    String s = e.getMessage();
    assertTrue(s.contains("[-v] [-f input ...] [-l [weight ...]] [--] rest ..."));
    assertTrue(s.contains("muckujacker and\n"));
    assertTrue(s.contains("0 to 4"));
    assertTrue(s.contains("in the range [-10, 20]"));
    assertTrue(s.contains("3 to 99"));
    assertTrue(s.contains("`-1 12'"));
    //System.err.println(e);
  }
  /* +******************************************************************* */
  public void testBreakLongWord() {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    
    cmd.addOption(new Option("-o", "option", 
                             "we test this option with the word "
                             + "voodooomasterblasterwonderwuzzelsuperdazzeledgargleblaster, "
                             + "which is just the right stuff", 0, 1));
    String[] argv = {"-h"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    String s = e.getMessage();
    assertTrue(s.contains(" voodooomasterblasterwo\n"));
    assertTrue(s.contains(" nderwuzzelsuperdazzeledgargleblaster,"));
    //System.err.println(e);
  }
  /* +******************************************************************* */
  public void testUseDouble() {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    cmd.addOption(new BooleanOption("-v", "verbose"));
    String[] argv = {"-v", "-v"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertEquals("option `-v' used more than once", e.getMessage());
  }
  /* +******************************************************************* */
  public void testFewArgs() {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    cmd.addOption(new Option("-s", "pair", "a pair of values", 2, 2));
    String[] argv = {"-s", "1"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    String s = e.getMessage();
    assertTrue(s.contains("not enough `pair' arguments"));
    assertTrue(s.contains("found 1"));
    assertTrue(s.contains("but want 2"));
  }
  /* +******************************************************************* */
  public void testOverflowTorest() {
    Commandline cmd =  new Commandline("TestProg", "do the test", "rest",
                                       "all other args", 0, Integer.MAX_VALUE);
    cmd.addOption(new Option("-s", "pair", "a pair of values", 2, 2));
    cmd.addOption(new Option("-t", "values", "some values", 0, 100));
    String[] argv = {"-t", "hicks", "clicks", "-s", "1", "1", "doo"};
    Exception e = parse(cmd, argv);
    assertNull(e);
    assertEquals("doo", cmd.getStringValue("--"));
  }
  /* +******************************************************************* */
  public void testForceMinuxValue() {
    Commandline cmd =  new Commandline("TestProg", "do the test", "rest",
                                       "all other args", 0, Integer.MAX_VALUE);
    cmd.addOption(new Option("-s", "pair", "a pair of values", 2, 2));
    String[] argv = {"-s", "-1", "-2", "hallo"};
    Exception e = parse(cmd, argv);
    assertNull(e);
    assertEquals("-1", cmd.getStringValue("-s"));
    assertEquals("hallo", cmd.getStringValue("--"));
  }
  /* +******************************************************************* */
  public void testDefault() throws Exception {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    String[] dflt = {"boo", "duu"};
    cmd.addOption(new Option("-s", "pair", "a pair of values", 2, 2, dflt));
    String[] argv = {};
    Exception e = parse(cmd, argv);
    assertNull(e);
    assertEquals("boo", cmd.getValue("-s"));
  }
  /* +******************************************************************* */
  public void testLongWrong() {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    cmd.addOption(new LongOption("-l", "longs", "some long values",
                                 1, Integer.MAX_VALUE, 0, 10));
    String[] argv = {"-l", "kaput"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().contains("expects long value but found `kaput'"));
  }
  /* +******************************************************************* */
  public void testLongSmall() {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    cmd.addOption(new LongOption("-l", "longs", "some long values",
                                 1, Integer.MAX_VALUE, 0, 10));
    String[] argv = {"-l", "-134"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().contains("-134"));
    assertTrue(e.getMessage().contains("smaller than allowed 0"));
  }
  /* +******************************************************************* */
  public void testLongLarge() {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    cmd.addOption(new LongOption("-l", "longs", "some long values",
                                 1, Integer.MAX_VALUE, 0, 10));
    String[] argv = {"-l", "134"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().contains("134"));
    assertTrue(e.getMessage().contains("larger than allowed 10"));
  }
  /* +******************************************************************* */
  public void testEnum() {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    cmd.addOption(new EnumOption("-e", "anenum", "do the enum",
                                 1, 5, "|eins|zwei|drei|vier"));
    String[] argv = {"-e", "zwei", "vier"};
    Exception e = parse(cmd, argv);
    assertNull(e);
    assertEquals("zwei", cmd.getValue("-e"));
    assertEquals("vier", cmd.getStringValues("-e")[1]);
  }
  /* +******************************************************************* */
   public void testEnumException() {
    Commandline cmd =  new Commandline("TestProg", "do the test");
    cmd.addOption(new EnumOption("-e", "anenum", "do the enum",
                                 1, 5, "|eins|zwei|drei|vier"));
    String[] argv = {"-e", "zwei", "xvier"};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().contains("not accept the value `xvier'"));
  }
   /* +******************************************************************* */
   public void testWrongDefault1() {
     Option o = new Option("-e", "erg", "do the thing", 2, 2);
     String[] dflt = {"do"};
     Exception e = null;
     try {
       o.setDefault(dflt);
     } catch( Exception ex ) {
       e = ex;
     }
     assertNotNull(e);
     //e.printStackTrace();
     assertTrue(e.getMessage().contains("found 1 but want 2"));
   }
   /* +******************************************************************* */
   public void testWrongDefault2() {
     Option o = new Option("-e", "erg", "do the thing", 2, 2);
     String[] dflt = {"do", "di", "da"};
     Exception e = null;
     try {
       o.setDefault(dflt);
     } catch( Exception ex ) {
       e = ex;
     }
     assertNotNull(e);
     //e.printStackTrace();
     assertTrue(e.getMessage().contains("only 2 are allowed"));
   }
   /* +******************************************************************* */
   /* +******************************************************************* */
  public static void main(String[] argv) {
    junit.textui.TestRunner.run(new TestSuite(CommandlineTest.class));
  }
  
}