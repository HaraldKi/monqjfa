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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Used to construct {@link CharTrans} objects of different implementations.
 * </p>
 *
 * @author &copy; 2004, 2005, 2015 Harald Kirsch
 * 
 * @TODO: Either make it generic or store FaState only
 * 
 * @TODO: replace with an implementation which has a complete Char.MAX_VALUE
 *        pair of arrays that is plainly set and overwritten, instead of
 *        mainting the intervals. The measure performance.
 */
public class Intervals<D> {
  // Every element of the array denotes a range starting at this
  // character and reaching up to just before the next character or
  // Character.MAX_VALUE.
  protected final StringBuilder ivals = new StringBuilder();
  protected final List<D> values = new ArrayList<D>();

  public static final long[] stats = new long[4];
  /**********************************************************************/
  public Intervals() {
    init();
  }
  /**********************************************************************/
  public static String logStats() {
    StringBuilder sb = new StringBuilder(200);
    sb.append(Intervals.class.getName()).append(" stats:");
    for (int i=0; i<stats.length; i++) {
      sb.append(' ').append(stats[i]);
    }
    return sb.toString();
  }
  /**********************************************************************/
  public static void resetStats() {
    for (int i=0; i<stats.length; i++) {
      stats[i] = 0;
    }
  }
  /**********************************************************************/
  /**
   * <p>all intervals which are currently mapped to <code>null</code>
   * will be mapped to the given object.</p>
   */
  public final void complete(D o) {
    int L = values.size();
    for(int i=0; i<L; i++) {
      if( values.get(i)!=null ) continue;
      values.set(i, o);
    }
  }
  /**********************************************************************/
  protected final void init() {
    ivals.append((char)0);
    values.add(null);
  }

  /**
   * reset to the state of a freshly constructed object. In particular
   * there will be one interval spanning the whole range of
   * <code>char</code> values being mapped to <code>null</code>.
   */
  public final void reset() {
    ivals.setLength(0);
    values.clear();
    init();
  }
  /**********************************************************************/
  public final int size() {return values.size();}
  public final D getAt(int i) { return values.get(i); }
  public final void setAt(int i, D o) {
    values.set(i, o);
  }
  public final char getFirstAt(int i) {return ivals.charAt(i);}
  public final char getLastAt(int i) {
    if( i==ivals.length()-1 ) return Character.MAX_VALUE;
    else return (char)(ivals.charAt(i+1)-1);
  }
  /**********************************************************************/
  /**
   * <p>Split whichever interval covers ch at ch and return the position
   * of the left interval. The value stored in the interval to split
   * will be put in both parts.</p>
   *
   * @return either -pos-1 if ch hits exactly the left border of the
   * interval at pos, or pos, if ch lies truly within interval pos.
   */
  public final int split(char ch) {
    int pos = getPos(ch);
    char first = ivals.charAt(pos);
    if( first==ch ) return -pos-1;
    D v = values.get(pos);

    pos += 1;
    ivals.insert(pos, ch);
    values.add(pos, v);
    return pos;
  }
  /**********************************************************************/
  public final void overwrite(char first, char last, D o) {
    int from = split(first);
    if( from<0 ) from = -(from+1);
    int to = values.size();
    if( last<Character.MAX_VALUE ) {
      to = split((char)(last+1));
      if( to<0 ) to = -(to+1);
    }
    for(int i=from; i<to; i++) values.set(i, o);
  }
  /**********************************************************************/
  /**
   * <p>inverts the mapped and unmapped intervals. Intervals which are
   * currently mapped to <code>null</code> will be mapped to
   * <code>o</code>, and 
   * those currently mapped to some object are mapped to
   * <code>null</code>. </p>
   */
  public final void invert(D o) {
    int L = values.size();
    for(int i=0; i<L; i++) {
      if( values.get(i)==null ) values.set(i, o);
      else values.set(i, null);
    }
  }
  /**********************************************************************/
  /**
   * return the index of the range which covers ch.
   */
  private int getPos(char ch) {
    int lo, hi; 
    // we start out with the correct position pos such that
    //    lo <= pos < hi
    // We maintain the above inequality throughout the loop
    for(lo=0, hi=ivals.length(); lo+1<hi; /**/) {
      int mid = (lo+hi)/2;
      if( ivals.charAt(mid)<=ch ) {
	// because hi-lo>=2, we always have mid>lo and this advances
	// the loop
	lo = mid;
      } else {
	hi = mid;
      }
    }
    
    // the loop constant lo<=pos<hi is still true
    return lo;
  }
  /**********************************************************************/
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Intervals[");
    for(int i=0; i<ivals.length(); i++) {
      if( i>0 ) sb.append(", ");
      char ch = ivals.charAt(i);
      if( ch>=' ' && ch<=126 ) sb.append('\'').append(ch).append("\'");
      else sb.append("0x").append(Integer.toString(ch, 16));
      sb.append("->").append(values.get(i)).append("...");
    }
    sb.append(']');
    return sb.toString();
  }
  /*+******************************************************************/

}
      
    
