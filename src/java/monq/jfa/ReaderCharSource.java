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

/**
 * wraps Reader or an input stream into a CharSource.
 *
 * @author &copy; 2003,2004 Harald Kirsch
 */
public class ReaderCharSource extends EmptyCharSource {

  private Reader in = null;

  /**
   * make sure to call {@link #setSource setSource()} before using
   * this object. 
   */
  public ReaderCharSource() {
  }
  public ReaderCharSource(Reader in) {
    this.in = in;
  }

  /**
   * <p>wraps the <code>InputStream</code> into a <code>Reader</code>,
   * using an encoding for the given character set.</p>
   *
   * <p>FIX ME: The <code>Reader</code> used internally cannot be
   * closed. Anyone who thinks this is a problem, please let me
   * know. But don't forget to close the <code>InputStream</code>
   * eventually.</p>
   */
  public ReaderCharSource(InputStream in, String charsetName) 
    throws UnsupportedEncodingException
  {
    InputStreamReader isr;
    isr = new InputStreamReader(in, charsetName);
    this.in = new BufferedReader(isr);
  }
  public ReaderCharSource(InputStream in) {
    this.in = new BufferedReader(new InputStreamReader(in));
  }
  public int read() throws java.io.IOException {
    int ch = super.readOne();
    if( ch>=0 ) return ch;
    return in.read();
  }
  public void setSource(Reader in) {
    super.clear();
    this.in = in;
  }
}
