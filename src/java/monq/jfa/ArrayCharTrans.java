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

import java.io.Serializable;
import java.util.List;

import monq.stuff.Sizeof;

/**
  <p>an implementation of interface <code>CharTrans</code> based on
  explicit arrays for ranges and the objects they are mapObjectped to.</p>
  @author &copy; 2003,2004 Harald Kirsch
*****/


class ArrayCharTrans<T> implements Serializable, CharTrans<T> {

  // stores character ranges in consecutive elements as inclusive
  // intervals, i.e. the interval 'a'..'b' contains 2 characters. 
  char[] ranges;

  // contains the value onto which the corresponding interval above is
  // mapped.
  T[] values;

  public static long stats = 0;
  /**********************************************************************/
  public static int estimateSize(int n) {
    int thisSize =
        Sizeof.roundUp(Sizeof.MEM_OBJ_OVERHEAD+2*Sizeof.MEM_PTR_SIZE);
    
    int rangesSize = Sizeof.charArrayMemEstimate(2*n);

    int valuesSize = Sizeof.objectArrayMemEstimate(n);

    return thisSize + rangesSize + valuesSize;
  }
  /**********************************************************************/
  ///CLOVER:OFF
  public String toString() {
    StringBuilder s = new StringBuilder(100);
    for(int i=0, L=values.length; i<L; i++) {
      s.append("[`").append(Misc.printable(getFirstAt(i)))
	.append("',`").append(Misc.printable(getLastAt(i)))
	.append("' -->").append(getAt(i)).append("] ");
    }
    return s.toString();
  }
  ///CLOVER:ON
  /**********************************************************************/
  public ArrayCharTrans(StringBuilder ranges2, List<T> values) {
    this.ranges = new char[ranges2.length()];
    ranges2.getChars(0, ranges2.length(), ranges, 0);
    @SuppressWarnings("unchecked")
    T[] tmp = (T[])new Object[values.size()];
    this.values = values.toArray(tmp);
  }
  /**********************************************************************/
  public int size() {return values.length; }
  /********************************************************************/
  public char getFirstAt(int pos) {
    return ranges[2*pos];
  }
  public char getLastAt(int pos) {
    return ranges[2*pos+1];
  }
  public T getAt(int pos) {
    return values[pos];
  }
  /********************************************************************/
  public T get(char ch) {
    stats += 1;

    int pos = getPos(ch);
    if( pos==values.length || ch<getFirstAt(pos) ) return null;
    return getAt(pos);
  }
  /********************************************************************/
  /**
    we cannot use java.util.Comparable for a binary search which wants
    to compare a character with a <code>RangeMap</code>.

    @return The result is the index of the element such that
    <code>ch</code> is just in front of or contained in the
    element. In addition, if the result <code>map.size()</code> then
    <code>ch</code> is larger than all elements in <code>map</code>.

    // direct access
    // 20.7%  1127  +     0    monq.jfa.ArrayCharTrans.getPos

    // access via getLastAt
    // 20.8%  1109  +     0    monq.jfa.ArrayCharTrans.getPos
    // Ist im Rahmen der Meßungenauigkeit identisch.
  *****/
  private int getPos(char ch) {
    int lo, hi; 
    // we start out with the correct position pos such that
    //    lo <= pos <= hi
    // We maintain the above inequality throughout the loop
    for(lo=0, hi=values.length; lo<hi; /**/) {
      int mid = (lo+hi)/2;
      if( getLastAt(mid)<ch ) {
	// ==> mid<pos  ==> mid+1<=pos
	// loop constant ==> pos<=hi
	// together: mid+1<=pos<=hi
	lo = mid+1;
      } else {
	// when arriving here, we have pos<=mid
	// we also have lo<=pos
	// we also know that mid<hi
	// ==> lo<=pos<=mid<hi
	// so the following maintains loops constant and advances loop
	hi = mid;
      }
    }
    
    // the loop constant lo<=pos<=hi is still true
    return lo;
  }
  /**********************************************************************/
}


