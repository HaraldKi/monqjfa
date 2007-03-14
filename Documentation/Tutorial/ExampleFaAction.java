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
 * <p>This program copies System.in to System.out while embedding any text
 * which matches the regular expression given as the first command
 * line argument with the strings provided as 2nd and 3rd argument.</p>
 *
 * <p>This program is incomplete, since it does no decent checking of
 * the command line arguments. An Exception is all it eventually
 * throws.</p> 
 *
 * @author &copy; 2005, 2006 Harald Kirsch
 *
 */
public class ExampleFaAction {

  /**
   * <p>This is the action we want to use later when setting up the
   * automaton. The two parameters are strings to be used to bracket
   * the match.</p>
   */
  public static final class Bracket extends AbstractFaAction {
    private final String pre, post;
    public Bracket(String pre, String post) {
      this.pre = pre;
      this.post = post;
    }
    public void invoke(StringBuffer iotext, int start, DfaRun r) {
      iotext.insert(start, pre);
      iotext.append(post);
    }
  }
  public static void main(String[] argv) throws Exception {
    // Add one pattern action pair to an Nfa.
    Nfa nfa = new Nfa(Nfa.NOTHING)
      .or(argv[0], new Bracket(argv[1], argv[2]));

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
