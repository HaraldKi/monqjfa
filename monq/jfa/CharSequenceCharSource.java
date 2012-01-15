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

import java.io.Serializable; 

/**
 * <p>is a {@link CharSource} wrapper around a {@link
 * java.lang.CharSequence} such that it can be used as an input source
 * of a {@link DfaRun}.</p>
 *
 * <p><b>Note:</b> The length of the sequence provided is stored
 * locally in objects of this class and is not updated, if the length
 * of the sequence changes. This allows to use the tail of a
 * <code>StringBuffer</code>, for example, for other purposes, while
 * the head of it is read by an object of this class.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class CharSequenceCharSource 
  extends EmptyCharSource implements CharSource, Serializable {

  private int next;
  private int end;
  private CharSequence s;

  public CharSequenceCharSource() {
    s = "";
    next = 0;
  }
  public CharSequenceCharSource(CharSequence s) {
    setSource(s);
  }

  public CharSequenceCharSource(CharSequence s, int startAt) {
    setSource(s, startAt);
  }

  public void setSource(CharSequence s) {
    setSource(s, 0, s.length());
  }

  public void setSource(CharSequence s, int startAt) {
    setSource(s, startAt, s.length());
  }

  public void setSource(CharSequence s, int startAt, int end) {
    super.clear();
    this.s = s;
    this.next = startAt;
    this.end = end;
  }

  public int read() {
    int ch = super.readOne();
    if( ch>=0 ) return ch;
    if( next>=end ) return -1;
    return s.charAt(next++);
  }
}
