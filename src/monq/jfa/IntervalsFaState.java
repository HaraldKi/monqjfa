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
 *        mainting the intervals. The meaesure performance.
 */
public class IntervalsFaState extends Intervals<FaState> {
  // used during conversion to CharTrans
  private StringBuilder ranges = new StringBuilder();
  private List<FaState> vtmp = new ArrayList<FaState>();
  
  /**********************************************************************/
  public void setFrom(CharTrans t) {
    if( t==null ) {
      return;
    }
    
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
  }
  /**********************************************************************/
  public CharTrans toCharTrans(double memoryForSpeedTradeFactor) {
    // We have to do the following:
    // 1) convert right open intervals to ranges [a,b]
    // 2) drop intervals which map to null
    // 3) collapse consecutive ranges pointing to identical objects
    
    //System.out.println("toTrans: "+toString());
    ranges.setLength(0);
    vtmp.clear();

    int dst = 0;
    int L = values.size();
    for(int i=0; i<L; i++) {
      FaState o = values.get(i);
      if( o==null ) continue;
      char ch = ivals.charAt(i);
      if( dst>0 && ranges.charAt(2*(dst-1)+1)+1==ch 
	  && vtmp.get(dst-1)==o ) {
	// just extend the interval at dst-1
	ranges.setCharAt(2*(dst-1)+1, getLastAt(i));
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
      int arrayTransSize = ArrayCharTrans.estimateSize(vtmp.size());
      int tableTransSize = TableCharTrans.estimateSize
	(ranges.charAt(2*vtmp.size()-1)-ranges.charAt(0)+1);

      //System.out.println("array: "+arrayTransSize);
      //System.out.println("table: "+tableTransSize);

      if( arrayTransSize*memoryForSpeedTradeFactor<tableTransSize ) {
	t = new ArrayCharTrans(ranges, vtmp);
	stats[2] += 1;
      } else {
	t = new TableCharTrans(ranges, vtmp);
	stats[3] += 1;
      }

    }
    reset();
    return t;
  }
  /**********************************************************************/
}
      
    
