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

package monq.stuff;

import java.lang.reflect.Array;
/**
 * <p>is a collection of static methods to manipulate arrays. The idea
 * is that the currently used area in the array is stored somewhere
 * else. The functions in here, except {@link #resize resize()},
 * pretend the whole array is in use.</p>
 *
 * <p>This
 * will hopefully be mostly obsoleted as soon as we have generics. The
 * set of methods is not in any way complete, because I only add what
 * I just needed.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public final class ArrayUtil {

  // no constructor needed
  private ArrayUtil() {}
  /**********************************************************************/
  /**
   * <p>allocates a new array with the given <code>newSize</code> and
   * copies the content of <code>old</code> into the new array up to
   * the maximum of <code>newSize</code> or
   * <code>old.length</code>.</p>
   * @return the newly allocate array
   */
  public static int[] resize(int[] old, int newSize) {
    int copySize = newSize>old.length ? old.length : newSize;
    int[] tmp = new int[newSize];
    System.arraycopy(old, 0, tmp, 0, copySize);
    return tmp;
  }
  /**
   * inserts <code>elem</code> at index <code>pos</code> in
   * <code>ary</code> by shifting all elements at indices greater
   * <code>pos</code> to a one larger index position. The very last
   * element of the array is lost.</p>
   */
  public static void insert(int[] ary, int pos, int elem) {
    System.arraycopy(ary, pos, ary, pos+1, ary.length-pos-1);
    ary[pos] = elem;
  }
  /**
   * <p>deletes <code>count</code> elements from <code>ary</code>
   * starting at index <code>pos</code> by copying the elements
   * at indices greater than or equal to <code>pos+count</code> to
   * index <code>pos</code>. The elements which become "free" at the
   * end of the array are not touched and the array is not
   * resized. Call {@link #resize(int[], int) resize()} afterwards to
   * reallocate the array.
   */
  public static void delete(int[] ary, int pos, int count) {
    System.arraycopy(ary, pos+count, ary, pos, ary.length-pos-count);
  }

  /**
   *<p>deletes <code>count</code> elements from <code>ary</code>
   * starting at index <code>pos</code> by copying the elements
   * at indices greater than or equal to <code>pos+count</code> to
   * index <code>pos</code>.
   * @see #delete(int[],int,int)
   */
  public static void delete(byte[] ary, int pos, int count) {
    System.arraycopy(ary, pos+count, ary, pos, ary.length-pos-count);
  }
  /**********************************************************************/
  /**
   * @see #resize(int[],int)
   */
  public static char[] resize(char[] old, int newSize) {
    int copySize = newSize>old.length ? old.length : newSize;
    char[] tmp = new char[newSize];
    System.arraycopy(old, 0, tmp, 0, copySize);
    return tmp;
  }
  /**
   * @see #insert(int[],int,int)
   */
  public static void insert(char[] ary, int pos, char elem) {
    System.arraycopy(ary, pos, ary, pos+1, ary.length-pos-1);
    ary[pos] = elem;
  }
  /**********************************************************************/
  /**
   * @see #resize(int[],int)
   */
  public static byte[] resize(byte[] old, int newSize) {
    int copySize = newSize>old.length ? old.length : newSize;
    byte[] tmp = new byte[newSize];
    System.arraycopy(old, 0, tmp, 0, copySize);
    return tmp;
  }
  /**
   * @see #insert(int[],int,int)
   */
  public static void insert(byte[] ary, int pos, byte elem) {
    System.arraycopy(ary, pos, ary, pos+1, ary.length-pos-1);
    ary[pos] = elem;
  }
  /**********************************************************************/
  /**
   * @see #resize(int[],int)
   */
  public static Object[] resize(Object[] old, int newSize) {
    int copySize = newSize>old.length ? old.length : newSize;
    Class elemType = old.getClass().getComponentType();
    Object[] tmp = (Object[])Array.newInstance(elemType, newSize);
    System.arraycopy(old, 0, tmp, 0, copySize);
    return tmp;
  }
  /**
   * @see #insert(int[],int,int)
   */
  public static void insert(Object[] ary, int pos, Object o) {
    System.arraycopy(ary, pos, ary, pos+1, ary.length-pos-1);
    ary[pos] = o;
  }
  /**
   * @see #delete(int[],int,int)
   */
  public static void delete(Object[] ary, int pos, int count) {
    System.arraycopy(ary, pos+count, ary, pos, ary.length-pos-count);
  }
}
