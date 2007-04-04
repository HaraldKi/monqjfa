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
    assertTrue(e.getMessage().contains("but need 1"));
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
        "test an option", 0, 19999, null).required());
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
        "test an option", 0, 19999, null).required());
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
        "test an option", 0, 19999, null).required());
    cmd.addOption(new Option("-b", "anopt", 
        "test an option", 0, 19999, null).required());
    String[] argv = {};
    Exception e = parse(cmd, argv);
    assertNotNull(e);
    assertTrue(e.getMessage().contains("required option(s)"));
    assertTrue(e.getMessage().contains("are missing"));
    assertTrue(e.getMessage().contains("`-a'"));
    assertTrue(e.getMessage().contains("`-b'"));
  }
  /*+********************************************************************/

  public static void main(String[] argv) {
    junit.textui.TestRunner.run(new TestSuite(CommandlineTest.class));
  }
  
}