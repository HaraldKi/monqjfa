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
 * <p>represents a minimal implementation of the {@link CharSource}
 * interface mainly providing the ability to push characters back into
 * the stream for later reading. Objects of this class don't deliver
 * any data except it was pushed into them before. Subclasses exist
 * which read from input sources.</p>
 *
 * <p>Extend this class and override the <code>read()</code> methods to
 * create a character source which is not empty up front. The
 * implementation of the <code>read()</code> function should look like
 * this:<pre>
 *     public int read() throws java.io.IOException {
 *       int ch = super.readOne();
 *       if( ch>=0 ) return ch;
 *       <em>code to deliver new data</em></pre>
 * </p>
 *
 * <p><b>Hint:</b> Use objects of this class with {@link DfaRun#setIn
 * DfaRun.setIn()} from within an {@link FaAction} to force early EOF
 * and stop the <code>DfaRun</code>.</p>

 * @author &copy; 2004 Harald Kirsch
 */
public class EmptyCharSource implements CharSource {

  // The valid characters are always at the rear of this array.
  private char[] pushed;
  private int pstart;

  public EmptyCharSource() { this(16); }
  public EmptyCharSource(int initialDepth) {
    pushed = new char[initialDepth];
    pstart = initialDepth;
  }

  /**
   * enlarge <code>pushed</code> such that <code>count</code> more
   * character fit before pstart.
   */
  private void ensureRoom(int count) {
    // current load is from pstart to the end
    int len = pushed.length-pstart;

    // this is what we need at least
    int newSize = len+count;

    // below the size of 1M characters, we double the size, but
    // beyond 1M characters, we only add 1M each time
    if( 2*newSize<1024*1024 ) newSize *= 2;
    else if( newSize-pushed.length<1024*1024 ) newSize+=1024*1024;

    char[] newp = new char[newSize];
    System.arraycopy(pushed, pstart, newp, newSize-len, len);
    pushed = newp;
    pstart = pushed.length-len;
  }

  public void pushBack(StringBuffer buf, int start) {
    int L = buf.length()-start;

    if( L>pstart ) ensureRoom(L);
    pstart -= L;
    buf.getChars(start, buf.length(), pushed, pstart);
    buf.setLength(start);
  }

  /**
   * <p>For the benefit of subclasses, this discards all characters which
   * might have been pushed back. A subclass which allows to change
   * its input source on the fly should call <code>clear()</code> to
   * make sure that no <em>old</em> pushed back characters are
   * delivered after the change.</p>
   */
  protected void clear() {
    pstart = pushed.length;
  }

  public int pop(StringBuffer out) {
    int count = pushed.length-pstart;
    out.append(pushed, pstart, count);
    pstart = pushed.length;
    return count;
  }

  public int pop(StringBuffer out, int count) {
    int max = pushed.length-pstart;    
    if( count>max ) count = max;
    out.append(pushed, pstart, count);
    pstart += count;
    return count;
  }

  /**
   * <p>subclasses implementing {@link CharSource#read} should first
   * call this function to obtain possibly pushed back characters.</p>
   *
   * <p>Calling this method instead of {@link #read} in subclasses
   * avoids an unnecessary try/catch.</p>
   */
  protected int readOne() {
    if( pstart==pushed.length ) return -1;
    return pushed[pstart++];
  }

  // REMINDER: This has declare the IOException to allow subclasses
  // that really need it to do this too.
  public int read() throws java.io.IOException { return readOne(); }
}
