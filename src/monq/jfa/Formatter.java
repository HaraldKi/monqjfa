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

import java.util.Map;

/**
 * defines the interface to classes which rearrange text parts stored
 * in a {@link TextStore} and/or in a <code>java.util.Map</code>
 * append them to a <code>StringBuffer</code>.
 *
 * <p><b>Hint:</b>Typically a {@link TextSplitter}> and a
 * <code>Formatter</code> are used in tandem and communicate via a
 * <code>TextStore</code>.</p>
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.7 $, $Date: 2005-07-08 13:01:26 $
 */
public interface Formatter extends java.io.Serializable {
  /**
   * <p>arranges (some of) the pieces of text found in <code>st</code>
   * or <code>m</code> in an implementation dependend manner and
   * <b>appends</b> them to <code>out</code>. Depending on the
   * implementation, both, <code>st</code> and </code>m</code> or even
   * both may be allowed to be <code>null</code>.</p>
   *
   * <p><b>Postcondition</b>: This method may not change the content of
   * <code>dest</code> other than by appending to it.</p>
   */
  void format(StringBuffer out, TextStore st, Map m) throws CallbackException;
}
 
