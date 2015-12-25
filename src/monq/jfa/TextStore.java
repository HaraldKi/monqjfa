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

import monq.stuff.ArrayUtil;

import java.io.Serializable;

/**
 * <p>stores a text and parts of it in a (hopefully) efficient
 * manner. A <code>TextStore</code> maintains a string as well as
 * possibly overlapping substrings thereof which are called
 * <em>parts</em>. The string can be enlarged with {@link #append
 * append()} and shortened by calling {@link #setLength
 * setLength()}. The parts, defined by start and end index, are
 * created with {@link #addPart addPart()} and {@link #setPart
 * setPart()}.</p>
 *
 * <p>Furthermore, a small nonnegative integer may be associated with
 * each part. <span style="color:red;">This is a hack</span> which was
 * introduced so that a <code>TextStore</code> is exactly what is
 * needed to represent a match, its submatches as well as the small
 * identifying integers which can be associated with them.</p>
 *
 * <p>The parts can be accessed with two <code>getPart</code> methods
 * ({@link #getPart(StringBuffer, int) getPart()}, {@link
 * #getPart(StringBuffer, int, int, int) getPart()}), which append the
 * requested part, or a piece thereof to a given
 * <code>StringBuffer</code>.</p>
 *
 * <p><b>Note:</b> Many applications define part 0 to refer to the
 * whole stored string, but this is not mandatory.</p>
 *
 * <a name="indexing"><b>Part Indexing</b></a><br />
 * <p>All methods which have a part index as parameter accept negative
 * values to address parts relative to the end of the part list. In
 * particular -1 refers to the last part.</p>
 *
 * @see TextSplitter
 * @see Formatter
 *
 * @author (C) 2003 Harald Kirsch
 * @version $Revision: 1.16 $, $Date: 2005-02-14 10:23:38 $
 */
public class TextStore implements Serializable {

  private StringBuffer text = new StringBuffer();

  private int[] parts = new int[0];
  private byte[] ids = new byte[0];
  private int numParts = 0;

  /*********************************************************************/
  /**
   * returns the number of currently stored parts.
   */
  public int getNumParts() { return numParts; }

  /**
   * returns the size of the currently stored text.
   */  
  public int length() {return text.length();}

  /**
   * <p>resets <code>this</code> to contain neither text nor any
   * parts.</p>
   */
  public void clear() {
    text.setLength(0);
    numParts = 0;
  }

  public void setLength(int l) {
    text.setLength(l);
    for(int i=0; i<numParts; i+=2) {
      if( parts[i+1]>l ) {
	parts[i+1] = l;
	if( parts[i]>parts[i+1] ) parts[i] = parts[i+1];
      }
    }
  }
  /**
   * <p>appends the given text to the <code>this</code>. If the added
   * text shall become a part, call {@link #setPart setPart()}
   * or {@link #addPart addPart()} afterwards.</p>
   */
  public void append(String s) {
    text.append(s);
  }

  public void append(char ch) {
    text.append(ch);
  }
  /**
   * <p>appends the given text to the <code>this</code>. If the added
   * text shall become a part, call {@link #setPart setPart()}
   * or {@link #addPart addPart()} afterwards.</p>
   */
  public void append(StringBuffer s, int start, int end) {
    Misc.append(text, s, start, end);
  }


  /**********************************************************************/
  /**
   * <p>defines the given part as the stretch of characters in the stored
   * text starting at position <code>start</code> and reaching to the
   * character just before <code>end</code>.</p>
   *
   * @param part see <a href="#indexing">part indexing</a>
   * @param id is a small id to be assigned to new part
   * @throws ArrayIndexOutOfBoundsException if <code>part</code> is
   * out of range
   * @throws IllegalArgumentException if <code>start</code> and
   * <code>end</code> do not satisfy the
   * relation <code>0<=start<=end<=textSize()</code>. 
   */
  public void setPart(int part, int start, int end, byte id) {
    int idx = part2idx(part);
    if( idx<0 ) {
      throw new ArrayIndexOutOfBoundsException
	(""+part+" out of range ["+(-numParts)+","+(numParts-1)+"]");
    }
    int L = text.length();

    if( start<0 || start>L ) {
      throw new IllegalArgumentException
	("start="+start+" not in range [0,"+L+"]");
    }
    if( end<start || end>L ) {
      throw new IllegalArgumentException
	("end="+end+" not in range ["+start+","+L+"]");
    }
    parts[idx++] = start;
    parts[idx] = end;
    ids[idx/2] = id;
  }
  /**
   * <p>calls the 4 parameter {@link #setPart(int,int,int,byte)
   * setPart()} with <code>id==-1</code>.</p>
   */
  public void setPart(int part, int start, int end) {
    setPart(part, start, end, (byte)-1);
  }
  /**********************************************************************/
  /**
   * <p>deletes all parts in the inclusive range referenced by
   * <code>first</code> and <code>last</code>. This does
   * not change the underlying string. Negative part indexes are
   * allowed, but the parts referenced by <code>first</code> and
   * <code>last</code> must be in order.</p>
   */
  public void deleteParts(int first, int last) {
    int idxFirst = part2idx(first);
    if( idxFirst<0 ) {
      throw new ArrayIndexOutOfBoundsException
	(""+first+" out of range ["+(-numParts)+","+(numParts-1)+"]");
    }
    int idxLast = part2idx(last);
    if( idxLast<0 ) {
      throw new ArrayIndexOutOfBoundsException
	(""+last+" out of range ["+(-numParts)+","+(numParts-1)+"]");
    }
    int c = idxLast-idxFirst+2;
    //System.err.println(">>>"+idxFirst+", "+idxLast+", "+c);
    ArrayUtil.delete(parts, idxFirst, c);
    ArrayUtil.delete(ids, idxFirst/2, c/2);
    numParts -=c/2;
  }
  /**********************************************************************/
  /**
   * <p>like {@link #setPart setPart()} except that the given part is
   * appended to the list of parts.</p>
   */
  public void addPart(int start, int end, byte id) {
    int part = numParts;
    if( 2*part+1>=parts.length ) {
      parts = ArrayUtil.resize(parts, 2*part+4);
      ids = ArrayUtil.resize(ids, part+2);
    }
    numParts = part+1;
    setPart(part, start, end, id);
  }
  public void addPart(int start, int end) {
    addPart(start, end, (byte)-1);
  }
  /**
   * <p>combines operations {@link #append} and {@link #addPart
   * addPart()} such that the whole appended text will also define a
   * new part.</p>
   */
  public void appendPart(StringBuffer s, int start, int end, byte id) {
    int l = text.length();
    append(s, start, end);
    addPart(l, l+(end-start), id);
  }
  public void appendPart(StringBuffer s, int start, int end) {
    appendPart(s, start, end, (byte)-1);
  }
  /********************************************************************/
  /** 
   * <p>appends the requested part to the given
   * <code>StringBuffer</code>. If <code>part</code> is out of range,
   * this is silently ignored and <code>sb</code> is not changed.</p>
   *
   * @param part see <a href="#indexing">part indexing</a>.
   */
  public void getPart(StringBuffer sb, int part) {
    int idx = part2idx(part);
    if( idx<0 ) return;
    Misc.append(sb, text, parts[idx], parts[idx+1]);
  }

  /**
   * <p>returns the requested part.</p>
   * @see  #getPart(StringBuffer,int)
   */
  public String getPart(int part) {
    int idx = part2idx(part);
    if( idx<0 ) return "";
    return text.substring(parts[idx], parts[idx+1]);
  }
  /**********************************************************************/
  /**
   * <p>retrieves the small integer value which can be associated with
   * a part.</p>
   *
   * @throws ArrayIndexOutOfBoundsException if <code>part</code> is
   * out of range
   */
  public byte getId(int part) {
    int idx = part2idx(part);
    if( idx<0 ) {
      throw new ArrayIndexOutOfBoundsException
	(""+part+" out of range ["+(-numParts)+","+(numParts-1)+"]");
    }
    return ids[idx/2];
  }
  /** 
   * <p>appends a substring of the requested part to the given
   * <code>StringBuffer</code>. The substring is indicated by
   * <code>from</code> and <code>to</code>, where <code>to</code>
   * refers to the character just after the last one requested. Both
   * indices are relative to the part, not the whole internally stored
   * text. Both indices can be negative in order to be taken relative
   * to <code>getPartLen(part)</code>. In addition,
   * <code>to==0</code> is also taken relative to the end of the
   * part.</p> 
   *
   * <p>If <code>part</code> is out of range,
   * this is silently ignored and <code>sb</code> is not changed.</p>
   * <p>If <code>start</code> or <code>end</code> are out of range,
   * they are truncated to the nearest fitting value.</p>
   *
   * <p><b>Examples</b>
   * <ul>
   * <li><code>from=0, to=0</code> refers to the whole part</li>
   * <li><code>from=-2, to=-1</code> returns the 2nd to last
   * character</li>
   * </ul>
   * </p>
   *
   * @param part see <a href="#indexing">part indexing</a>
   */
  public void getPart(StringBuffer sb, int part, int from, int to) {
    int idx = part2idx(part);
    if( idx<0 ) return;

    int start = from>=0 ? parts[idx]+from : parts[idx+1]+from;
    int end = to>0 ? parts[idx]+to : parts[idx+1]+to;
    if( start<parts[idx] ) start=parts[idx];
    if( end>parts[idx+1] ) end=parts[idx+1];
    if( start>end) start = end = 0;
    Misc.append(sb, text, start, end);
  }

  /**
   * <p>returns a substring of the requested part.<p>
   *
   * @see #getPart(StringBuffer,int,int,int)
   */
  public String getPart(int part, int from, int to) {
    int idx = part2idx(part);
    if( idx<0 ) return "";

    int start = from>=0 ? parts[idx]+from : parts[idx+1]+from;
    int end = to>0 ? parts[idx]+to : parts[idx+1]+to;
    if( start<parts[idx] ) start=parts[idx];
    if( end>parts[idx+1] ) end=parts[idx+1];
    if( start>end) start = end = 0;
    return text.substring(start, end);
  }
  /**********************************************************************/
  /**
   * <p>returns the length of the requested part. If <code>part</code> is
   * out of range, 0 is returned.</p>
   *
   * @param part see <a href="#indexing">part indexing</a>
   */
  public int getPartLen(int part) {
    int idx = part2idx(part);
    if( idx<0 ) return 0;
    return parts[idx+1]-parts[idx];
  }
  /**********************************************************************/
  public int getStart(int part) {
    int idx = part2idx(part);
    if( idx<0 ) {
      throw new ArrayIndexOutOfBoundsException
	(""+part+" out of range ["+(-numParts)+","+(numParts-1)+"]");
    }
    return parts[idx];
  }
  /********************************************************************/
  public int getEnd(int part) {
    int idx = part2idx(part);
    if( idx<0 ) {
      throw new ArrayIndexOutOfBoundsException
	(""+part+" out of range ["+(-numParts)+","+(numParts-1)+"]");
    }
    return parts[idx+1];
  }
  /********************************************************************/
  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append(super.toString());
    b.append("[(").append(text).append(") ");
    for(int i=0; i<numParts; i++) {
      int idx = 2*i;
      b.append('(').append(parts[idx])
	.append(",").append(parts[idx+1]).append(')');
    }
    b.append(']');
    return b.toString();
  }
  /********************************************************************/
  private int part2idx(int part) {
    if( part<0 ) {
      part += numParts;
      if( part<0 ) return -1;
    } else if( part>=numParts ) {
      return -1;
    }			       
    return 2*part;
  }
  /********************************************************************/
  /**
   * <p>calls {@link CharSource#pushBack CharSource.pushBack()} to get
   * rid of the substring of the text beginning at start. Any parts
   * pointing into the drained substring will be shrunk, possible to
   * zero length. As with other <code>TextStore</code> indices,
   * <code>start</code> may be negative to denote a start relative to
   * the length of the text stored in <code>this</code>.</p>
   */
  public void drain(CharSource dst, int start) {
    if( start<0 ) start+=text.length();
    dst.pushBack(text, start);

    // Because the start/end pairs in parts are sorted, we run the
    // loop over the end elements. Only if they need adjustment may
    // the start element need adjustment too.
    for(int i=1; i<2*numParts; i+=2) {
      if( parts[i]<=start ) continue;
      parts[i] = start;
      if( parts[i-1]>parts[i] ) parts[i-1]=parts[i];
    }
  }    
}
