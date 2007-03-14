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
 * <p>This program copies System.in to System.out while changing any text
 * that matches the regular expression given as the first command
 * line argument to the string (or template) given as the second
 * command line argument.</p>
 *
 * <p>This program is incomplete, since it does no decent checking of
 * the command line arguments. An Exception is all it eventually
 * throws.</p> 
 *
 * @author &copy; 2005, 2006 Harald Kirsch
 *
 */
public class Example {
  public static void main(String[] argv) throws Exception {
    // Add one pattern action pair to an Nfa.
    Nfa nfa = new Nfa(argv[0], new Printf(argv[1]));

    // Compile into the Dfa, specify that all text not matching any
    // regular expression shall be copied from input to output
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);

    // get a machinery (DfaRun) to operate the Dfa
    DfaRun r = new DfaRun(dfa);

    // Specify the input and filter it to the output
    r.setIn(new ReaderCharSource(System.in));
    r.filter(System.out);
  }
}
