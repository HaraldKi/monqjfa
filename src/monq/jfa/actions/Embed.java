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

package monq.jfa.actions;

import monq.jfa.*;

/**
 * implements an {@link FaAction} which embeds the matched input
 * into two fixed strings.
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.5 $, $Date: 2005-02-16 14:03:34 $
 */
public class Embed extends AbstractFaAction {
  private String pre, post;
  /**
   * <p>creates an {@link FaAction} with the given priority which
   * embeds the matched text into the given strings. For example
   * <code>new Embed("&lt;x&gt;", "&lt;/x&gt;", 0)</code> arranges
   * for the match to be XML tagged with <code>&lt;x&gt;</code>.
   * The parameters may not be <code>null</code>, but may be the
   * empty string.</p>
   *
   * <p>Although the functionality of this action is covered by the
   * more general {@link Printf}, it is believed
   * that this specific implementation is slightly more efficient.</p>
   */
  public Embed(String pre, String post, int prio) {
    this.pre = pre;
    this.post = post;
    this.priority = prio;
  }
  /**
   * <p>calls the 3 parameter constructor with
   * <code>priority==0</code>. 
   */
  public Embed(String pre, String post) {
    this(pre, post, 0);
  }
  public void invoke(StringBuffer out, int start, DfaRun runner) {
    out.insert(start, pre);
    out.append(post);
  }
  public boolean equals(Object _o) {
    if( !(_o instanceof Embed) ) return false;
    Embed o = (Embed)_o;
    return pre.equals(o.pre) && post.equals(o.post) && priority==o.priority;
  }
  public String toString() {
    StringBuffer sb = new StringBuffer(30);
    sb.append(super.toString())
      .append("[\"").append(pre).append("\", \"")
      .append(post).append("\"]")
      ;
    return sb.toString();
  }
}
