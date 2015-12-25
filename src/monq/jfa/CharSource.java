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
 * defines the type from which a DfaRun reads the data. In particular
 * every DfaRun implements this interface to allow cascading.
 *
 * @author &copy; 2005 Harald Kirsch
 */
public interface CharSource {
  /**
   * returns a single character or -1 to indicate end of file.
   */
  int read() throws java.io.IOException;

  /**
   * <p>moves the tail of <code>buf</code> starting with character
   * index <code>startAt</code> back onto the character source while
   * deleting them from the given <code>StringBuffer</code>. The data
   * is pushed from back to front such that a sequence of
   * read/pushBack/read will read two times the same string.
   */
  void pushBack(StringBuffer from, int startAt);
}
 
