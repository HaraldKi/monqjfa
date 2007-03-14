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
 * <p>implements a {@link Feeder} that feeds a {@link
 * java.lang.CharSequence} to the output specified by {@link #setOut
 * setOut()}.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class CharSequenceFeeder extends AbstractPipe {
  private CharSequence in;
  private Charset cs;
  /**
   * <p>creates the <code>Feeder</code> to feed <code>in</code> to an
   * <code>OutputStream</code> while encoding characters with the
   * character set given by <code>csname</code>.</p>
   */
  public CharSequenceFeeder(CharSequence in, String csname) 
    throws UnsupportedCharsetException
  { 
    this.in = in; 
    this.cs = Charset.forName(csname);
  }

  protected void pipe() {
    OutputStreamWriter o = new OutputStreamWriter(out, cs);
    PrintWriter pw = new PrintWriter(o);
    pw.print(in);
    pw.flush();
  }
}
/**********************************************************************/
