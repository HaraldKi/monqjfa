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
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import monq.jfa.FaState.IterType;

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
    int v = ch;
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
			    HashSet<FaState> known) {
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
      out.println("  n"+aid+"[label=\""
          + escapeLabel(a.toString())+"\" shape=\"plaintext\"];");
      out.println("  n"+id+" -> n"+aid+" [style=\"dotted\"];");
    }

    // visualize the subautomaton info
    Map<FaAction, FaSubinfo[]> subs = s.getSubinfos();
    Iterator<FaAction> keys;
    if( subs==null ) keys = new HashSet<FaAction>().iterator();
    else keys = subs.keySet().iterator();
    while( keys.hasNext() ) {
      Object o = keys.next();
      //System.err.println(">>>>"+o.getClass().getName());
      String aid = getID(o);
      FaSubinfo[] ary = subs.get(o);
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
	FaState other = trans.getAt(i);
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
    for(Iterator<FaState> i=s.getChildIterator(IterType.ALL); i.hasNext(); /**/) {
      FaState child = i.next();
      if( known.contains(child) ) continue;
      print(child, out, start, last, known);
    }

  }
  /*+******************************************************************/
  private static String escapeLabel(String label) {
    return label.replace("\"", "\\\"");
  }
  /*+******************************************************************/
  
  /**
   * prints a finite automaton in a format suitable for
   * <code>dot</code>. This function is not really for public
   * consumption as it is not easy to get hold of the start state of
   * an Nfa or Dfa. Rather use {@link Nfa#toDot} and {@link Dfa#toDot}
   * to call this function.
   */
  public static void print(PrintStream out, FaState start, FaState last) {
    out.println("digraph hallo {");
    print(start, out, start, last, new HashSet<FaState>());
    out.println("}");
  }
  /**********************************************************************/
  private static void usage() {
      System.err.println("usage: FaToDot -nfa|-dfa|-dfanfa re [re ...]");
      System.exit(1);    
  }
  /*+******************************************************************/
  
  /**
   * each command line argument is taken to be a regular expression
   * which is added to a {@link Nfa} with a unique action. The
   * resulting Nfa as well as the compiled Dfa are printed to
   * standard output in the format suitable for <code>dot</code>.
   */
  public static void main(String[] argv)
      throws ReSyntaxException, CompileDfaException
  {
    List<String> types = Arrays.asList(new String[]{"-nfa", "-dfa", "-dfanfa"});
    if (argv.length<2 || !types.contains(argv[0])) {
      usage();
    }
    Nfa nfa = new Nfa(Nfa.NOTHING);
    for(int i=1; i<argv.length; i++) {
      nfa.or(argv[i], 
	     new monq.jfa.actions.Replace(new Integer(i).toString()));
    }
    if ("-nfa".equals(argv[0])) {
      nfa.toDot(System.out);
    } else if (argv[0].startsWith("-dfa")) {
      Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
      if ("-dfanfa".equals(argv[0])) {
        nfa = dfa.toNfa();
        nfa.toDot(System.out);
      } else {
        dfa.toDot(System.out);
      }
    }
  }
}

 
