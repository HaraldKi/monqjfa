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
 * <p>defines the interface of a method which separates a string into
 * parts and stores them in a {@link TextStore}.</p>
 *
 * <p><b>Hint:</b>Typically a <code>TextSplitter</code> and a {@link
 * Formatter} are used in tandem and communicate via a
 * <code>TextStore</code>.</p>
 *
 * @author &copy; 2004 Harald Kirsch
 */
public interface TextSplitter {

  /**
   * <p>separates the stretch of text defined by the suffix of
   * <code>source</code>, starting at <code>start</code> into parts
   * and stores the parts in <code>dst</code>.</p>
   *
   * <p>The method normally does not clear the <code>TextStore</code>,
   * but implementations <em>may</em> choose to do so.</p>
   *
   * <p><b>Postcondition:</b> Implementations of this method must
   * treat <code>source</code> read-only.</p>
   */
  void split(TextStore dst, StringBuffer source, int start);

  /**
   * <p>is an instance of a <code>TextSplitter</code> which does not
   * split the incoming text at all, but appends it completely as a new
   * part onto the given <code>TextStore</code>. The
   * <code>TextStore</code> is <em>not</em> cleared before.</p>
   */
  TextSplitter NULLSPLITTER = new TextSplitter() {
      public void split(TextStore ts, StringBuffer source, int start) {
	ts.appendPart(source, start, source.length());
      }
    };
  
}
