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

import java.nio.*;
import java.nio.charset.*;

/**
 * <p>methods to encode/decode between a
 * <code>java.lang.StringBuffer</code> and a
 * <code>java.nio.ByteBuffer</code>.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class Coder {

  private CharsetEncoder enc;
  private CharsetDecoder dec;
  private CharBuffer chars;


  /**
   * <p>creates an <code>Coder</code> for the given character set
   * which uses a default internal buffer size for encoding.</p>
   */
  public Coder(Charset chs) { 
    setEncoder(chs.newEncoder(), 4096);
    setDecoder(chs.newDecoder());
  }

  /**
   * <p>sets the encoder to use and requests to use a character buffer
   * size of at least <code>chBsize</code> internally.</p>
   */
  public void setEncoder(CharsetEncoder enc, int chBsize) {
    this.enc = enc;
    if( chars==null || chars.capacity()<chBsize ) {
      chars = CharBuffer.allocate(chBsize);
    }
  }
  /**
   * <p>sets the decoder to use.</p>
   */
  public void setDecoder(CharsetDecoder dec) {
    this.dec = dec;
  }
  /**********************************************************************/
  /**
   * decode the contents of a <code>ByteBuffer</code> and append them
   * to a <code>StringBuffer</code>.
   *
   * @param in must be in flipped mode, i.e. ready to be read from
   */
  public void decode(ByteBuffer in, StringBuffer out)
    throws java.nio.charset.CharacterCodingException
  {
    chars.clear();
    while( in.remaining()>0 ) {
      CoderResult cr = dec.decode(in, chars, true);
      if( cr.isError() ) cr.throwException();
      // we could do out.append, but this would probably trigger a
      // rather inefficent version of out.append.
      char[] a = chars.array();
      out.append(a, 0, chars.position());
      chars.clear();
    }
  }
  /**********************************************************************/
  /**
   * <p>encode the contends of a <code>StringBuffer</code> into a
   * <code>ByteBuffer</code>.</p>
   *
   * @return is either the given <code>ByteBuffer</code> or a freshly
   * allocated, if the given one is too short.
   *
   */
  public ByteBuffer encode(StringBuffer in, ByteBuffer out)
    throws java.nio.charset.CharacterCodingException
  {
    int L = in.length();
    int pos = 0;
    char[] a = chars.array();
    while( pos<L || chars.position()>0 ) {

      // transfer from StringBuffer to CharBuffer, there may be chars
      // left in the CharBuffer from the previous round of this loop
      int copySize = chars.remaining();
      if( copySize>(L-pos) ) copySize = L-pos;
      in.getChars(pos, pos+copySize, a, chars.position());
      pos += copySize;
      chars.position(chars.position()+copySize);
      chars.flip();

      // encode to ByteBuffer. This may stop short because the output
      // buffer overflows.
      CoderResult cr = enc.encode(chars, out, L>=pos);

      chars.compact();
      if( cr.isError() ) cr.throwException();
      if( cr.isOverflow() ) {
	int rest = chars.position() + (L-pos);
	rest = (int)((float)rest * enc.averageBytesPerChar());
	byte[] aout = out.array();
	aout = monq.stuff.ArrayUtil.resize(aout, aout.length+rest+1);
	ByteBuffer newOut = ByteBuffer.wrap(aout);
	newOut.position(out.position());
	newOut.limit(newOut.capacity());
	out = newOut;
      }
    }
    out.flip();
    return out;
  }
}
