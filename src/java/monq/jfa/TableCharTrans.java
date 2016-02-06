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

import java.util.List;

import monq.stuff.Sizeof;

/**
 * @author &copy; 2004 Harald Kirsch
 */

class TableCharTrans implements java.io.Serializable, CharTrans {
  private char first;
  private char last;
  private FaState[] targets;

  // Note: the size is *not* equal to targets.length because targets
  // may contain null entries.
  private int size;

  public static long stats = 0;
  /**********************************************************************/
  /**
   * return estimated size of a <code>TableCharTrans</code> for
   * <code>span==last-first+1</code> covered characters.
   */
  public static int estimateSize(int span) {
    int thisSize = Sizeof.roundUp(Sizeof.MEM_OBJ_OVERHEAD
                                          + 2
                                          + Sizeof.MEM_PTR_SIZE);
    int targetsSize = Sizeof.objectArrayMemEstimate(span);
    return thisSize + targetsSize;
  }
  /**********************************************************************/
  /**
   * We do not support emtpy transitions here. Consequently there
   * should be at least one range in <code>sb</code>
   */
  public TableCharTrans(StringBuilder ranges, List<FaState> values) {
    int L = ranges.length();
    first = ranges.charAt(0);
    last = ranges.charAt(L-1);

    // To the outside world we have to maintain the idea of
    // non-overlapping intervals mapped to a value. Therefore the
    // size() is exactly this:
    size = values.size();

    targets = new FaState[last-first+1];
    L = values.size();
    for(int pos=0, i=0; pos<L; pos++) {
      char from = ranges.charAt(2*pos);
      char to = ranges.charAt(2*pos+1);
      FaState o = values.get(pos);
      //System.out.println("from="+from+", to="+to+", pos="+pos);
      for(int ch=from; ch<=to; ch++) targets[i++] = o;
      if( pos+1<L ) {
	char next = ranges.charAt(2*pos+2);
	for(int ch=to+1; ch<next; ch++) targets[i++] = null;
      }
    }
  }
  /**********************************************************************/
  @Override
  public FaState get(char ch) {
    stats += 1;
    if( ch>=first && ch<=last ) {
      FaState result = targets[ch-first];
      return result;
    }
    return null;
  }
  /**********************************************************************/
  @Override
  public int size() { return size; }
  /**********************************************************************/

  @Override
  public FaState getAt(int i) {
    FaState result = targets[getPos(i)];
    return result;
  }
  @Override
  public char getFirstAt(int i) {
    int pos = getPos(i);
    return (char)(first+pos);
  }
  @Override
  public char getLastAt(int i) {
    int pos = getPos(i);
    Object here = targets[pos];
    pos += 1;
    while( pos<targets.length && targets[pos]==here ) pos += 1;
    return (char)(first+(pos-1));
  }
  /**********************************************************************/
  private int getPos(int i) {
    int pos = 0;
    int rest = i;
    try {
      while( rest>0 ) {
	Object here = targets[pos];
	pos += 1;
	while( targets[pos]==here ) pos+=1;
	while( targets[pos]==null ) pos+=1;
	rest -= 1;
      }
    } catch( ArrayIndexOutOfBoundsException e ) {
      ///CLOVER:OFF
      throw new ArrayIndexOutOfBoundsException(i);
      ///CLOVER:ON
    }
    return pos;
  }
  /**********************************************************************/
  ///CLOVER:OFF
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder(100);
    s.append('[').append(first).append(',').append(last).append(' ');
    for(int i=0; i<targets.length; i++) {
      if( targets[i]==null ) s.append('0');
      else s.append('.');
    }
    s.append(']');
    return s.toString();
  }
  ///CLOVER:ON
  /********************************************************************/
}
