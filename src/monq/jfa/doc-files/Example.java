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
import monq.jfa.actions.*;
/**
 * This program copies System.in to System.out while enclosing words
 * which match "[A-Za-z]+" with "<id>" and "</id>" and numbers
 * matching "[0-9]+" with "<num>" and "</num>".
 *
 * In addition, if the command line contains an even number of
 * arguments, they are taken as pattern/format pairs. The patterns are
 * added to the list of regular expressions to match, and a match will
 * result in the associated format to be used to reformat the matching
 * part of the text.
 *
 * Compile and run, for example, like this:
 *
   javac Example.java
   echo hallo 123 | java -cp monq.jar Example
 * 
 * @author &copy; 2005 Harald Kirsch
 *
 */
public class Example {
  public static void main(String[] argv) throws Exception {
    String P = "Example";
    if( argv.length%2!=0 ) {
      System.err.println("usage: "+P+" [pattern format] ...");
      System.exit(1);
    }

    // We add two default pattern/action pairs to the automaton
    Nfa nfa = new Nfa(Nfa.NOTHING)
      .or("[A-Za-z]+", new Printf("<id>%0</id>"))
      .or("[0-9]+", new Printf("<num>%0</num>"))
      ;
    
    // Now we add all pattern/action pairs found on the command line. 
    for(int i=0; i<argv.length; i+=2) {
      String pattern = argv[i];
      FaAction action = new Printf(true, argv[i+1]);
      nfa.or(pattern, action);
    }

    // Compile the Nfa into a Dfa while specifying how input should be
    // treated that does not match any of the regular expressions.
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);

    // get a machinery (DfaRun) to operate the Dfa
    DfaRun r = new DfaRun(dfa);

    // Specify the input and filter it to the output
    r.setIn(new ReaderCharSource(System.in));
    r.filter(System.out);
  }
}
