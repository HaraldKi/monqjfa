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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

/**
 * <p>implements a {@link CharSource} which reads bytes and converts
 * them to characters of a given character set while keeping track of
 * byte positions of converted characters within the input stream.</p>
 *
 * <p>Because many character encodings use a variable number of coding
 * bytes for one character, counting characters is <b>not</b>
 * sufficient to keep track of the exact position of a character or
 * string in a byte oriented input stream. Objects of this class can
 * be used as an input source to a {@link DfaRun} in cases where the
 * exact byte postions of matches are needed. Byte positions of
 * characters delivered are kept in a sliding window and can be
 * retrieved with {@link #position position()}.</p>
 *
 * <p>One particular problem is posed by the {@link #pushBack
 * pushBack()} method required by the {@link CharSource}
 * interface. Its contract does not require the pushed back
 * characters to be the same as those previously read. Using this class,
 * however, this <b>is</b> required. Since {@link Dfa#match
 * Dfa.match()} obeys to this rule, a <code>ByteCharSource</code> can
 * safely be used as the source for a {@link DfaRun} as long as the
 * callbacks employed don't violate the rule.</p>
 *
 * @author (C) 2003 Harald Kirsch
 * @version $Revision: 1.9 $, $Date: 2005-02-14 10:23:38 $
 */
public class ByteCharSource extends EmptyCharSource {

  private ReadableByteChannel source;
  private ByteBuffer inBuf = null;

  // solely for error reporting, we keep a file name, if one was
  // provided in a constructor
  private String fileName = null;

  // set by fill() depending on return value from source
  private boolean eof;


  // it is a pity, but in order to keep track of the byte positions of
  // characters we can only decode one character at a time.
  private CharBuffer outBuf = CharBuffer.allocate(1);
  private CharsetDecoder dec;

  // Denotes the byte position within the input stream of the first
  // byte of inBuf.
  private long bPos;

  // We try to keep track of the file positions of characters we
  // deliver. If and when characters are pushed back, we assume it
  // where exactly those previously delivered and move back our
  // current cursor by that many characters. The cache is operated in
  // a wrap-around fashion. Variable 'posFirst' indexes the first
  // valid entry and 'posCount' is the number of valid
  // entries. Obviously, posFirst+posCount can wrap around, but
  // posCount will never exceed posCache.length. In 'posCursor' we
  // keep track of the index position of the next character to be
  // delivered. It is incremented in read() and decremented in
  // pushBack. If a position is asked for, it should better be for
  // 0<=posCursor<=posCount.
  long[] posCache = null;
  int posFirst;
  int posCount;
  int posCursor;

  /********************************************************************/
  /**
   * creates a <code>ByteCharSource</code> to read from the given
   * channel. The decoder will be taken from the system property
   * <code>"file.encoding"</code> or default to
   * <code>"UTF-8"</code> (see {@link #setDecoder setDecoder()}). The
   * input buffer size is set to 4096 (see {@link #setInputBufferSize
   * setInputBufferSize()}) and the window keeping track of file
   * positions of characters will have a size of 1000 (see {@link
   * #setWindowSize setWindowSize()}).
   */
  public ByteCharSource(ReadableByteChannel source) {
    setSource(source);
  }
  /********************************************************************/
  /**
   * creates the same setup as {@link
   * #ByteCharSource(ReadableByteChannel)}. 
   */
  public ByteCharSource(InputStream in) {
    this(Channels.newChannel(in));
  }
  /**
   * creates the same setup as {@link
   * #ByteCharSource(ReadableByteChannel)}. 
   */
  public ByteCharSource(RandomAccessFile in) {
    this(in.getChannel());
  }
  /**
   * creates the same setup as {@link
   * #ByteCharSource(ReadableByteChannel)}. 
   */
  public ByteCharSource(String filename) throws FileNotFoundException {
    this(Channels.newChannel(new FileInputStream(filename)));
    this.fileName = filename;
  }
  /********************************************************************/
  /**
   * <p>sets the source to be read by <code>this</code>.</p>
   *
   * <p><b>Hint:</b> A <code>ReadableByteChannel</code> can be created
   * for any <code>InputStream</code> with
   * <code>java.nio.channels.Channels.newChannel()</code>.</p>
   *
   * @return <code>this</code> to make it easy to call more
   * configuration functions right away.
   */
  // Note: this method is the one true initialization of this
  // object. It makes sure the object is in a consitent state to get
  // going, entering reasonable defaults where they were not set
  // before.
  public ByteCharSource setSource(ReadableByteChannel source) {
    // get rid of left-over pushed back characters
    super.clear();

    // the source with its byte counter
    this.source = source;
    bPos = 0;

    // when called by the constructor, the input buffer must be set
    if( inBuf==null ) {
      setInputBufferSize(4096);
    } else {
      inBuf.clear();
      inBuf.flip();
    }

    // when called by the constructor, initialize the decoder
    if( dec==null ) {
      String chsName = System.getProperty("file.encoding", "UTF-8");
      setDecoder(Charset.forName(chsName).newDecoder());
    } else {
      dec.reset();
    }

    if( posCache==null ) posCache = new long[1000];
    // The first character to be delivered is at position zero. We
    // fill this already into the position cache to make sure that
    // the call position(0) is meaningful.
    posFirst = 0;
    posCache[0] = 0;
    posCount = 1;
    posCursor = 0;    // points to position of next char to be delivered

    return this;
  }
  /********************************************************************/
  /**
   * <p>sets the size of the sliding window which keeps byte
   * positions for characters recently delivered by {@link
   * #read()}.</p>
   *
   * @return <code>this</code> to make it easy to call more
   * configuration functions right away.
   */
  public ByteCharSource setWindowSize(int size) {
    if( size<1 ) {
      throw new IllegalArgumentException("size must be >=1 but is"+size);
    }
    long[] tmp = new long[size];

    // posCount is initialized and treated as to never be <1
    assert posCount>0;

    if( posCount>size ) {
      // we want to copy the tail of the available window
      posFirst = (posFirst+(posCount-size))%posCache.length;
      posCount = size;
    }
    int l = 0;
    int rest = posCount;
    if( posFirst+posCount>posCache.length ) {
      l = posCache.length-posFirst;
      System.arraycopy(posCache, posFirst, tmp, 0, l);
      rest -= l;
      posFirst = 0;
    }
    System.arraycopy(posCache, posFirst, tmp, l, rest);

//     for(int i=0; i<tmp.length; i++) {
//       System.out.print(tmp[i]+" ");
//     }
//     System.out.println();
    posCache = tmp;
    posFirst = 0;
    posCursor = posCount-1;
    return this;
  }
  /********************************************************************/
  /**
   * <p>set the size of the input buffer. Requests of very small size (1,
   * 2 bytes) are adjusted upwards to make sure the buffer is able to
   * hold at least the bytes encoding one character of the currently
   * decoded character set.</p>
   *
   * @return <code>this</code> to make it easy to call more
   * configuration functions right away.
   */
  public ByteCharSource setInputBufferSize(int size) {
    inBuf = ByteBuffer.allocate(size);
    inBuf.flip();

    // The following test will be false only during object
    // initialization. Later there will always be a decoder.
    if( dec!=null ) sanitizeInputBuffer();
    return this;
  }
  /********************************************************************/
  /**
   * <p>set the decoder to be used to decode the incoming bytes into
   * <code>char</code>.</p>
   * <p><b>Hint:</b> For a given character set name a decoder can
   * always be obtained with
   * <code>Charset.forName(chsetName)</code>.</p>
   *
   * @return <code>this</code> to make it easy to call more
   * configuration functions right away.
   */
  public ByteCharSource setDecoder(CharsetDecoder dec) {
    this.dec = dec;
    sanitizeInputBuffer();
    return this;
  }
  /**********************************************************************/
  /**
   * The input buffer size might be slightly adjusted here if it is so
   * small it cannot hold the maximum number of bytes encoding a
   * character of the currently active character encoding.
   */
  private void sanitizeInputBuffer() {
    double maxBytesPerChar = dec.charset().newEncoder().maxBytesPerChar();
    int minBsize = (int)Math.ceil(maxBytesPerChar);
    
    if( inBuf.capacity()>=minBsize ) return;

    ByteBuffer tmp = ByteBuffer.allocate(minBsize);
    tmp.clear();
    tmp.put(inBuf);
    inBuf = tmp;
    inBuf.flip();
  }
  /********************************************************************/
  /**
   * <p>closes the underlying <code>ReadableByteChannel</code>. Normally
   * you want to call this if you passed in a file name to the
   * constructor.</p>
   */
  public void close() throws java.io.IOException {
    source.close();
  }
  /********************************************************************/
  private void fill() throws java.io.IOException {
    // inBuf.position() reflects how far into inBuf we have
    // read. Moving this byte to the front of the buffer means the
    // buffer's position in the file moved that much forward
    bPos = bPos+inBuf.position();

    // move remaining buffer contents to front of inBuf
    inBuf.compact();

    // The input may be in non-blocking mode. We wait until either EOF
    // is signalled or at least one byte is read.
    int res;
    while( 0==(res=source.read(inBuf)) ) Thread.yield();
    eof = res==-1;

    // prepare for inBuf for taking out the data again.
    inBuf.flip();
  }
  /********************************************************************/
  /**
   * <p>while satisfying {@link CharSource#pushBack}, this method also
   * adjusts the internal referencing the byte position of the
   * the most recently delivered character. To do so, this method only
   * looks at the number of characters pushed back, thereby assuming
   * that they are the very same characters previously delivered by
   * {@link #read()}.</p>
   */
  public void pushBack(StringBuffer buf, int start) {
    int N = buf.length()-start;
    super.pushBack(buf, start);
    // Manage the cache of character file positions. If this goes
    // below zero, it is still not a problem. Only if someone requests
    // the current or a recent position, we will be in trouble.
    posCursor -= N;
  }
  /********************************************************************/
  /**
   * <p>returns the byte position counted from the start of the input
   * stream for the character referenced by
   * <code>charNo</code>. Parameter <code>charNo</code> is interpreted
   * relative to the first character not yet delivered. In particular,
   * the character most recently delivered by {@link #read()} has
   * <code>charNo==-1</code>. In general, to find the start of a
   * recently read string of <em>N</em> characters, call 
   * <code>position(-N)</code>. Calling <code>position(0)</code> is
   * also always possible and denotes the byte position of the next
   * character to be delivered.</p>
   *
   * <p>Because only a sliding window of character positions is kept,
   * this method may fail with a {@link
   * UnavailablePositionException} when <code>charNo</code> points
   * outside this window. If the window size is <em>N</em> and {@link
   * #pushBack pushBack()} is never called, the safe range for
   * <code>charNo</code> is <em>-N+1</em> to 0. If, however, <em>m</em>
   * characters are pushed back, this must be adjusted to
   * <em>-N+1+m</em>...<em>+m</em>. Subsequent reads move forward
   * again in the window neutralizing the effect of the pushback after
   * <em>m</em> characters have been read.</p>
   */
  public long position(int charNo) throws UnavailablePositionException {
    int p = posCursor+charNo;
    if( p<0 ) {
      throw new UnavailablePositionException
	(UnavailablePositionException.EXPIRED+": "+p);
    }
    if( p>=posCount ) {
      throw new UnavailablePositionException
	(UnavailablePositionException.NOTYET+": "+p);
    }
//     for(int i=0; i<posCount; i++) {
//       System.err.print(" "+posCache[(i+posFirst)%posCache.length]);
//     }
//     System.err.println();
    return posCache[(posFirst+p)%posCache.length];
  }
  /********************************************************************/
  private void showBuf(String name, Buffer b) {
    System.err.println(name+", position="+b.position()+
		       ", limit="+b.limit()+
		       ", cap="+b.capacity());
  }
  /********************************************************************/
  // only meant to be called by read() just after one character was
  // properly decoded into outBuf. Just before returning this
  // character, we update the position cache, i.e. posCache and the
  // likes. 
  private int deliver() {
    // This is even correct, if inBuf does not have any remaining
    // bytes.
    posCache[(posFirst+posCount)%posCache.length] = bPos + inBuf.position();
    if( posCount==posCache.length ) {
      // We have overwritten the element at posFirst, so advance
      // posFirst. Don't need to change posCursor because it marks an
      // index relativ to posFirst.
      posFirst += 1;
      if( posFirst==posCache.length ) posFirst = 0;
    } else {
      posCount += 1;
      posCursor += 1;
    }
    return outBuf.get(0);
  }
  /********************************************************************/
  public int read() throws java.io.IOException {
    // do we have pushed back characters
    int ch = super.readOne();
    if( ch>=0 ) {
      posCursor += 1;
      return ch;
    }
    
    CoderResult code = null;
    outBuf.clear();
    //System.err.println("\ngoing in.");
    while( CoderResult.OVERFLOW
	   != (code=dec.decode(inBuf, outBuf, eof)) ) {
      // Because the specs don't say what should happen if overflow
      // and underflow happen at the same time, we may have an
      // underflow but nevertheless a character is ready.
      if( code==CoderResult.UNDERFLOW ) {
	if( outBuf.position()>0 ) return deliver();
	if( eof ) return -1;
	fill();
	continue;
      }

      // Things went wrong. We have neither OVERFLOW nor UNDERFLOW, so
      // we report the problem.
      String err = 
	"decoder for character set `"+dec.charset()+"' reports `"
	+code.toString()+"' at position "
	+(bPos+inBuf.position());
      if( fileName!=null ) {
	err = err + " of file `"+fileName+"'";
      } else {
	err = err + " (input source not bound to named file)";
      }
      if( inBuf.remaining()>0 ) {
	int p = inBuf.position();
	err = err + ", offending byte is 0x" 
	  +Integer.toHexString(0xff&inBuf.get(p));
      }
      throw new java.io.IOException(err);
    }

    // arriving here, outBuf (which has anyway a length of 1)
    //overflowed, i.e. we have now one character available to deliver.
    return deliver();
  }
  /********************************************************************/
  protected void finalize() throws java.io.IOException {
    source.close();
  }
  /********************************************************************/

}
