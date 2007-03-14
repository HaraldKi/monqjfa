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



/**
 * implements a <code>CharTrans</code> which has a transition on
 * just one range of characters.
 *
 * @author &copy; 2004 Harald Kirsch
 */
class RangeCharTrans implements java.io.Serializable, CharTrans {
  private char first;
  private char last;
  private Object o;

  public static long stats = 0;

  public RangeCharTrans(char first, char last, Object o) {
    this.first = first;
    this.last = last;
    this.o = o;
//     System.out.println("Range("+Misc.printable(first)+".."
// 		       +Misc.printable(last)+", "+o+")");
  }

  public Object get(char ch) {
    stats += 1;
    if( ch>=first && ch<=last ) return o;
    return null;
  }
  public int size() {return 1;}
  public Object getAt(int pos) {
    if( pos!=0 ) throw new ArrayIndexOutOfBoundsException();
    return o;
  }
  public char getFirstAt(int pos) {
    if( pos!=0 ) throw new ArrayIndexOutOfBoundsException();
    return first;
  }
  public char getLastAt(int pos) {
    if( pos!=0 ) throw new ArrayIndexOutOfBoundsException();
    return last;
  }      

}
