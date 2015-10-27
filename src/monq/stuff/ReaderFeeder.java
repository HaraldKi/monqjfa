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

import java.io.*;
import java.nio.charset.*;

/**
 * <p>implements a {@link Feeder} that feeds data read from a {@link
 * java.io.Reader} to the output specified by {@link #setOut
 * setOut()}.</p>
 *
 * <p>Objects of this class can be reused within a <em>single</em>
 * thread.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class ReaderFeeder extends AbstractPipe {
  private Reader in;
  private Charset cs;
  private char[] buffer;

  /**
   * <p>when {@link #setOut setOut()} is called, it wraps the given
   * <code>OutputStream</code> into a <code>Writer</code> it stores it
   * here. This <code>Writer</code> is used by {@link #pipe pipe()} as
   * the output sink. Subclasses wrapping <code>pipe()</code> may use
   * <code>wout</code> to prefix or postfix the data send.</p>
   */
  protected Writer wout;

  /**
   * <p>creates the <code>Feeder</code> to feed <code>in</code> to an
   * <code>OutputStream</code> while encoding characters with the
   * character set given by <code>csname</code>.</p>
   *
   * @param source is the <code>Reader</code> from which to read input
   * data. May be <code>null</code>, but then it must be set
   * with {@link #setSource setSource()} before using this object.
   *
   * @param bsize specifies the size of the internal character buffer and
   * must be greater zero
   */
  public ReaderFeeder(Reader source, String csname, int bsize) 
    throws UnsupportedCharsetException
  { 
    if( bsize<1 ) 
      throw new IllegalArgumentException("bsize must be >0, but is "+bsize);
    this.in = source;
    this.cs = Charset.forName(csname);
    this.buffer = new char[bsize];
  }
  /**********************************************************************/
  /**
   * <p>specifies the source <code>java.lang.Reader</code> from which to
   * fetch the data to feed.</p>
   *
   * <p><b>Hint:</b> Do not use the inherited method
   * <code>setIn()</code>. If you need a <code>Feeder</code> with an
   * <code>InputStream</code> as the source, use {@link Pipe}.</p>
   */
  public void setSource(Reader in) {
    this.in = in;
  }
  /**********************************************************************/  
  protected void pipe() throws IOException {
    int l;
    while( -1!=(l=in.read(buffer)) ) wout.write(buffer, 0, l);
    wout.flush();
  }
  /**********************************************************************/
  public void setOut(OutputStream out, boolean closeOnExit) {
    super.setOut(out, closeOnExit);
    wout = new OutputStreamWriter(out, cs);
  }
}
/**********************************************************************/
