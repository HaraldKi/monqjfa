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
 * <p>This class is merely a container to wrap some very simple {@link
 * Formatter}s into one source file. The class itself cannot be
 * instantiated. Please note that you want to use the subclasses
 * normally only indirectly by means of a {@link PrintfFormatter}.</p>
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class SimpleFormatters {

  // This class cannot be instantiated.
  private SimpleFormatters() {}

  /*********************************************************************/
  /**
   * implements a {@link Formatter} which totally ignores the given
   * {@link TextStore} and instead appends a predefined string to the
   * destination buffer.
   */
  public static class FixedString implements Formatter {
    private StringBuffer text;
    /**
     * creates a {@link Formatter} to append the content of
     * the <code>other</code> when its <code>format</code> method is
     * called.
     */
    public FixedString(StringBuffer other) {
      text = new StringBuffer(other.length());
      text.append(other);
    }
    /**
     * appends the content of <code>sb</code> to the predifined string
     * to be used by <code>format</code>.
     */
    public void append(StringBuffer sb) { text.append(sb); }
    public void append(FixedString other) { other.format(text, null, null); }
    public void format(StringBuffer sb, TextStore sp, Map m) {
      sb.append(text);
    }
  }
  /********************************************************************/

  /**
   * implements a {@link Formatter} the <code>format</code> method of
   * which produces a substring of part of the given {@link
   * TextStore}.
   */
  public static class Part implements Formatter {
    private int partNo;
    private int from;
    private int to;

    /**
     * the parameters will be passed one-to-one to {@link
     * TextStore#getPart}.
     */
    public Part(int partNo, int from, int to) {
      this.partNo = partNo;
      this.from = from;
      this.to = to;
    }
    public void format(StringBuffer sb, TextStore sp, Map m) {
      if( from==0 && to==0) {
	sp.getPart(sb, partNo);
	return;
      }
      sp.getPart(sb, partNo, from, to);
    }       
  }
  /********************************************************************/
  /**
   * implements a {@link Formatter} the <code>format</code> method of
   * which concatenates several parts of the given {@link TextStore}.
   */
  public static class PartSeq implements Formatter {
    private int from;
    private int to;
    private String sep;
    private int partFrom, partTo;
    /**
     * <p>the parameters define the range of parts to be concatenated
     * by the <code>format</code> method. Similar to <a
     * href="TextStore.html#indexing">TextStore indexing</a> both
     * <code>int</code> parameters may be negative. If the
     * <code>sep</code> is not <code>null</code> it serves to separate
     * the the parts in the output.</p>
     */
    public PartSeq(int from, int to, String sep, int partFrom, int partTo) {
      this.from = from;
      this.to = to;
      this.sep = sep;
      this.partFrom = partFrom;
      this.partTo = partTo;
    }
    public PartSeq(int from, int to) {
      this(from, to, null, 0, 0);
    }
    public void format(StringBuffer sb, TextStore sp, Map m) {
      int L = sp.getNumParts();
      int start, end;
      start = (from<0) ? L+from : from;
      end = (to<=0) ? L+to : to;
      if( start>=end ) return;
      sp.getPart(sb, start, partFrom, partTo);
      for(int i=start+1; i<end; i++) {
	if( sep!=null ) sb.append(sep);
	sp.getPart(sb, i, partFrom, partTo);
      }
    }
  }
  /********************************************************************/
  /**
   * this {@link Formatter} will append the number of parts of the
   * {@link TextStore} passed in.
   */
  public static final Formatter NumParts = new Formatter() {
      public void format(StringBuffer sb, TextStore sp, Map m) { 
	sb.append(sp.getNumParts()); 
      }
    };

  /********************************************************************/
  /**
   * <p>implements a {@link Formatter} the <code>format</code> method of
   * which produces the length of a given part of the given {@link
   * TextStore}.</p>
   */
  public static class PartLen implements Formatter {
    private int partNo;
    public PartLen(int partNo) { this.partNo = partNo; }
    public void format(StringBuffer sb, TextStore sp, Map m) { 
      sb.append(sp.getPartLen(partNo)); 
    }
  }      
  /********************************************************************/
  /**
   * <p>implements a {@link Formatter} the <code>format</code> method
   * of which inserts an element from a <code>Map</code> into the
   * output.</p>
   */
  public static class GetVar implements Formatter {
    private String key;
    /**
     * <p>sets up a formatter that inserts
     * retrieves <code>key</code> from the <code>Map</code> passed to
     * <code>format()</code> and appends the result to the output. If
     * the <code>key</code> is not found, nothing is appended.</p>
     */
    public GetVar(String key) {
      this.key = key;
    }
    public void format(StringBuffer out, TextStore sp, Map m) {
      Object o = m.get(key);
      out.append(o==null ? "" : o);
    }
  }
  /**********************************************************************/

}
 
