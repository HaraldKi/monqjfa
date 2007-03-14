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

import java.io.PrintStream;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;

/**
 * contains a static function to output a finite automaton as a graph
 * to be printed by the fine <a
 * href="http://www.research.att.com/sw/tools/graphviz/">dot
 * utility</a>. The static {@link #print print} below could as well
 * have gone into {@link Nfa}, but I did not want to clutter that
 * class anymore.
 *
 * @author &copy; 2003,2004 Harald Kirsch
 */
public class FaToDot {

  // this class cannot be instantiated
  private FaToDot() {}

  /**********************************************************************/
  private static String getID(Object s) {
    return 
      Integer.toHexString(System.identityHashCode(s))
      ;
  }
  /**********************************************************************/
  public static String printable(char ch) {
    int v = (int)ch;
    if( v<=32 || v>=128 ) {
      if( v==9 )   { return "\\\\t"; }
      if( v==10 )  { return "\\\\n"; }
      if( v==13 )  { return "\\\\r"; }
      return "\\\\u" + Integer.toHexString(v);
    } else if( v=='"' ) { 
      return "\\\"";
    } else {
      return ""+ch;
    }    
  }
  /**********************************************************************/
  private static void print(FaState s, 
			    PrintStream out, 
			    FaState start, FaState last, 
			    HashSet known) {
    known.add(s);

    String id = getID(s);

    // visual appearence of the node depending on its features
    if( s==start ) out.println("  n"+id+" [shape=\"box\"];");
    if( s==last ) out.println("  n"+id+" [fontcolor=\"blue\" "
			      +"label=\"lastState\"];");
    if( s.getAction()!=null ) {
      out.println("  n"+id+" [color=\"red\"];");

      // the action is shown too
      FaAction a = s.getAction();
      String aid = getID(a);
      out.println("  n"+aid+"[label=\""+a+"\" shape=\"plaintext\"];");
      out.println("  n"+id+" -> n"+aid+" [style=\"dotted\"];");
    }

    // visualize the subautomaton info
    Map subs = s.getSubinfos();
    Iterator keys;
    if( subs==null ) keys = new HashSet().iterator();
    else keys = subs.keySet().iterator();
    while( keys.hasNext() ) {
      Object o = keys.next();
      //System.err.println(">>>>"+o.getClass().getName());
      String aid = getID(o);
      FaSubinfo[] ary = (FaSubinfo[])subs.get(o);
      for(int ii=0; ii<ary.length; ii++) {
	FaSubinfo sfi = ary[ii];
	String type = sfi.typeString();
	out.println("  n"+id+" -> n"+
		    aid+" [style=\"dotted\", color=\"green\", taillabel=\""+
		    type+","+sfi.id()+"\"];");
      }
    }
    // use the memory address as a label for the node
    if( s!=last ) out.println("  n"+id+" [label=\""+id+"\"];");

    
    // edges with characer annotation
    CharTrans trans = s.getTrans();
    if( trans!=null ) {
      for(int i=0; i<trans.size(); i++) {
	char first = trans.getFirstAt(i);
	char lastChar = trans.getLastAt(i);
	FaState other = (FaState)trans.getAt(i);
	String otherId = getID(other);
	if( first==lastChar ) {
	  out.println("  n"+id+" -> n"+otherId+
		      " [label=\""+printable(first)+"\"];");
	} else {
	  out.println("  n"+id+" -> n"+otherId+
		      " [label=\""+printable(first)+".."
		      +printable(lastChar)+"\"];");
	}
      }
    }

    // dashed edges for epsilon transitions
    FaState[] eps = s.getEps();
    if( eps!=null ) {
      for(int ii=0; ii<eps.length; ii++) {
	String otherId = getID(eps[ii]);
	out.println("  n"+id+" -> n" + otherId + "[style=\"dashed\"];");
      }
    }

    // go recursive
    for(Iterator i=s.getChildIterator(); i.hasNext(); /**/) {
      FaState child = (FaState)i.next();
      if( known.contains(child) ) continue;
      print(child, out, start, last, known);
    }

  }
  /**********************************************************************/
  /**
   * prints a finite automaton in a format suitable for
   * <code>dot</code>. This function is not really for public
   * consumption as it is not easy to get hold of the start state of
   * an Nfa or Dfa. Rather use {@link Nfa#toDot} and {@link Dfa#toDot}
   * to call this function.
   */
  public static void print(PrintStream out, FaState start, FaState last) {
    out.println("digraph hallo {");
    print(start, out, start, last, new HashSet());
    out.println("}");
  }
  /**********************************************************************/
  /**
   * each command line argument is taken to be a regular expression
   * which is added to a {@link Nfa} with a unique action. The
   * resulting Nfa as well as the compiled Dfa are printed to
   * standard output in the format suitable for <code>dot</code>.
   */
  public static void main(String[] argv)
    throws java.io.IOException, ReSyntaxException, 
    CompileDfaException
  {

    Nfa nfa = new Nfa(Nfa.NOTHING);
    for(int i=0; i<argv.length; i++) {
      nfa.or(argv[i], 
	     new monq.jfa.actions.Replace(new Integer(i).toString()));
    }
    nfa.toDot(System.out);
    System.out.println("#1");
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    dfa.toDot(System.out);
  }
}

 
