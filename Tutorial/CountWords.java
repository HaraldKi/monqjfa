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
import java.util.*;
/**
 * <p>This program counts words in the input stream, sorts them by
 * frequency and then lists them with their frequency.</p>
 *
 * <p>This program is incomplete, since it does no decent checking of
 * the command line arguments. An Exception is all it eventually
 * throws.</p> 
 *
 * @author &copy; 2005 Harald Kirsch
 *
 */
public class CountWords {

  /**
   * <p>called whenenver a word is found, this action increments the
   * count for that word in a Map to be found in r.clientData.</p>
   */
  public static class DoCount extends AbstractFaAction {
    public void invoke(StringBuffer iotext, int start, DfaRun r) {
      String word = iotext.substring(start);
      iotext.setLength(start);

      Map m = (Map)r.clientData;
      Integer count = (Integer)m.get(word);
      if( count==null ) m.put(word, new Integer(1));
      else m.put(word, new Integer(1+count.intValue()));
    }
  }

  public static void main(String[] argv) throws Exception {
    // We use a trivial definition of word:
    Nfa nfa = new Nfa("[a-zA-Z]+", new DoCount());

    // Compile into the Dfa, specify that all text not matching any
    // regular expression shall deleted.
    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);

    // get a machinery (DfaRun) to operate the Dfa
    DfaRun r = new DfaRun(dfa);
    
    // IMPORTANT: the DoCount actions need a Map in r.clientData,
    // otherwise a NullpointerException will be thrown
    Map counts = new HashMap();
    r.clientData = counts;
    
    // Specify the input and filter it to the output
    r.setIn(new ReaderCharSource(System.in));
    r.filter();			// no output needed

    // sort the key value pairs according to the count
    int l = counts.size();
    Map.Entry[] pairs = new Map.Entry[l];
    pairs = (Map.Entry[])counts.entrySet().toArray(pairs);
    Arrays.sort(pairs, new Comparator() {
	public int compare(Object o1, Object o2) {
	  Map.Entry c1 = (Map.Entry)o1;
	  Map.Entry c2 = (Map.Entry)o2;
	  Integer i1 = (Integer)(c1.getValue());
	  Integer i2 = (Integer)(c2.getValue());
	  int d = i1.intValue()-i2.intValue();
	  if( d!=0 ) return d;
	  return ((String)c1.getKey()).compareTo((String)c2.getKey());
	}
      });
    for(int i=0; i<l; i++) {
      System.out.println(pairs[i].getKey()+": "+pairs[i].getValue());
    }
  }
}
