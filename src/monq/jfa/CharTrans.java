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
  <p>defines the interface to a data structure which associates
  non-overlapping character ranges like 'a'-'f' to objects. Since
  the character ranges are naturally sorted, a <code>CharTrans</code>
  object acts like an array for the stored object with a {@link #size}
  and access method {@link #getAt getAt()} to retrieve an object
  stored at a given index.</p>
  <p><code>CharTrans</code> objects should be immutable. To assemble
  them, use {@link Intervals}.</p>
  *
  * @author &copy; 2005 Harald Kirsch
*****/

interface CharTrans {
  /**
   <p>returns the object stored in the transition table map for
   character <code>ch</code>. If <code>ch</code> is not mapped,
   <code>null</code> is returned.</p>
  *****/
  Object get(char ch);

  /**
   * returns the number of character ranges (and objects) stored.
   */
  int size();

  /**
   * returns the object stored at index position <code>pos</code>.
   */
  Object getAt(int pos);

  /**
   * returns the left boundary of the range stored at index
   * <code>pos</code> 
   */
  char getFirstAt(int pos);

  /**
   * returns the right boundary of the range stored at index
   * <code>pos</code> 
   */
  char getLastAt(int pos);

}
