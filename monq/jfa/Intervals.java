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

/**
 * <p>Used to construct {@link CharTrans} objects of different
 * implementations.</p>
 *
 * @author &copy; 2004, 2005 Harald Kirsch
 */
class Intervals {

  // Every element of the array denotes a range starting at this
  // character and reaching up to just before the next character or
  // Character.MAX_VALUE.
  private StringBuffer ivals = new StringBuffer();
  private ArrayList values = new ArrayList();

  // used during conversion to CharTrans
  private StringBuffer ranges = new StringBuffer();
  private ArrayList vtmp = new ArrayList();

  public static long[] stats = new long[4];
  /**********************************************************************/
  public Intervals() {
    init();
  }
  /**********************************************************************/
  public Intervals(CharTrans t) {
    init();
    setFrom(t);
  }
  /**********************************************************************/
  public Intervals setFrom(CharTrans t) {
    if( t==null ) return this;

    int L = t.size();
    char previousBorder = 0;
    for(int i=0; i<L; i++) {
      char first = t.getFirstAt(i);
      if( first==previousBorder ) {
	// have to replace the null already assigned to previous
	// border 
	values.set(values.size()-1, t.getAt(i));
      } else {
	ivals.append(first);
	values.add(t.getAt(i));
      }
      char last = t.getLastAt(i);
      if( last<Character.MAX_VALUE ) {
	ivals.append((char)(last+1));
	values.add(null);
	previousBorder = (char)(last+1);
      }
      //System.out.println("++"+this);
    }
    return this;
  }
  /**********************************************************************/
  /**
   * <p>all intervals which are currently mapped to <code>null</code>
   * will be mapped to the given object.</p>
   */
  public Intervals complete(Object o) {
    int L = values.size();
    for(int i=0; i<L; i++) {
      if( values.get(i)!=null ) continue;
      values.set(i, o);
    }
    return this;
  }
  /**********************************************************************/
  private void init() {
    ivals.append((char)0);
    values.add(null);
  }

  /**
   * reset to the state of a freshly constructed object. In particular
   * there will be one interval spanning the whole range of
   * <code>char</code> values being mapped to <code>null</code>.
   */
  public Intervals reset() {
    ivals.setLength(0);
    values.clear();
    ranges.setLength(0);
    vtmp.clear();
    init();
    return this;
  }
  /**********************************************************************/
  public int size() {return values.size();}
  public Object getAt(int i) { return values.get(i); }
  public void setAt(int i, Object o) { values.set(i, o); }
  public char getFirstAt(int i) {return ivals.charAt(i);}
  public char getLastAt(int i) {
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
  public int split(char ch) {
    int pos = getPos(ch);
    char first = ivals.charAt(pos);
    if( first==ch ) return -pos-1;
    Object v = values.get(pos);

    pos += 1;
    ivals.insert(pos, ch);
    values.add(pos, v);
    return pos;
  }
  /**********************************************************************/
  public void overwrite(char first, char last, Object o) {
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
  public void invert(Object o) {
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
  public CharTrans toCharTrans() {
    // We have to do the following:
    // 1) convert right open intervals to ranges [a,b]
    // 2) drop intervals which map to null
    // 3) collapse consecutive ranges pointing to identical objects
    
    //System.out.println("toTrans: "+toString());

    int dst = 0;
    int combined = 0;
    int L = values.size();
    for(int i=0; i<L; i++) {
      Object o = values.get(i);
      if( o==null ) continue;
      char ch = ivals.charAt(i);
      if( dst>0 && ranges.charAt(2*(dst-1)+1)+1==ch 
	  && vtmp.get(dst-1)==o ) {
	// just extend the interval at dst-1
	ranges.setCharAt(2*(dst-1)+1, getLastAt(i));
	combined += 1;
	continue;
      }
      vtmp.add(o);
      ranges.append(ch);
      ranges.append(getLastAt(i));
      dst += 1;
    }
    //if( combined>0 ) System.err.println("combined "+combined+" intervals");
    if( vtmp.size()==0 ) {
      reset();
      return null;
    }
    assert vtmp.size()*2 == ranges.length();

    CharTrans t;
    if( vtmp.size()==1 ) {
      char first = ranges.charAt(0);
      char last = ranges.charAt(1);
      if( first==last ) {
	t = new SingletonCharTrans(first, vtmp.get(0));
	stats[0] += 1;
      } else {
	t = new RangeCharTrans(first, last, vtmp.get(0));
	stats[1] += 1;
      }
    } else {
      // estimate the size of an ArrayCharTrans
      int s1 = ArrayCharTrans.estimateSize(vtmp.size());
      int s2 = TableCharTrans.estimateSize
	(ranges.charAt(2*vtmp.size()-1)-ranges.charAt(0)+1);
      //System.out.println("+++"+this);
      //System.out.println("Array:"+s1);
      //System.out.println("Table:"+s2);

      if( s1<s2 ) {
	t = new ArrayCharTrans(ranges, vtmp);
	stats[2] += 1;
      } else {
	t = new TableCharTrans(ranges, vtmp);
	//System.out.println(">>>"+t);
	stats[3] += 1;
      }

    }
    reset();
    return t;
  }
  /**********************************************************************/
  ///CLOVER:OFF
  public String toString() {
    StringBuffer sb = new StringBuffer();
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
  ///CLOVER:ON
}
      
    
