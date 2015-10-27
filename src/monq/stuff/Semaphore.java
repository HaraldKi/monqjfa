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


/**
 * <p>implements multi-slot semaphore. (This is somewhere available in
 * the jdk and I just missed it, right?)</p>
 *
 * @author &copy; 2004 Harald Kirsch
 * @version $Revision: 1.3 $, $Date: 2005-02-14 10:23:39 $
 */
public class Semaphore {

  private boolean[] used;
  private int hint;
  /**********************************************************************/
  /**
   * <p>creates a semaphore with the given number of slots.</p>
   */
  public Semaphore(int slots) {
    used = new boolean[slots];
  }
  /**********************************************************************/
  /**
   * <p>blocks the calling thread until a free slot is available. The
   * slot number as a value starting from zero is returned. It will
   * typically be used by the caller as and index into an array of
   * resources. Once the caller does not need the resource anymore, it
   * should call {@link #release release()} to free the slot for
   * others.</p>
   */
  public synchronized int acquire() {
    while( true ) {
      int slot = hint;
      for(int i=0; i<used.length; i++) {
	if( !used[slot] ) {
	  used[slot] = true;
	  return slot;
	}
	slot += 1;
	if( slot>=used.length ) slot=0;
      }
      try {
	this.wait();
      } catch( InterruptedException e ) {
	// someone wants us to stop waiting foreever
	return -1;
      }
    }
  }
  /**********************************************************************/
  /**
   * <p>releases the given slot and thereby allows other threads to
   * acquire it. No check is and can be made as to whether it is ok to
   * release the slot. To assure this is completely the responsibility
   * of the caller.</p>
   */
  public synchronized void release(int slot) {
    used[slot] = false;
    hint = slot;
    this.notifyAll();
  }
  /**********************************************************************/
}
