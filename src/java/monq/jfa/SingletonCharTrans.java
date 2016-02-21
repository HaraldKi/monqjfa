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
 * exactly one character.
 *
 * @author &copy; 2004 Harald Kirsch
 */
class SingletonCharTrans<T> implements java.io.Serializable, CharTrans<T> {
  private char ch;
  private T o;

  public static long stats = 0;

  public SingletonCharTrans(char ch, T o) {
    this.ch = ch;
    this.o = o;
  }

  @Override
  public T get(char queryChar) {
    stats += 1;
    if( this.ch==queryChar ) return o;
    return null;
  }
  public int size() {return 1;}

    @Override
    public T getAt(int pos) {
    if( pos!=0 ) throw new ArrayIndexOutOfBoundsException(pos);
    return o;
  }
  public char getFirstAt(int pos) {
    if( pos!=0 ) throw new ArrayIndexOutOfBoundsException(pos);
    return ch;
  }
  public char getLastAt(int pos) {
    if( pos!=0 ) throw new ArrayIndexOutOfBoundsException(pos);
    return ch;
  }      

}
