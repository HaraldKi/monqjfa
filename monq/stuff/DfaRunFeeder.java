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

import monq.jfa.*;

import java.io.*;
import java.nio.charset.*;

/**
 * <p>implements a {@link Feeder} that feeds data filtered by a {@link
 * monq.jfa.Dfa} to the output specified by {@link #setOut
 * setOut()}.</p>
 *
 * <p>Objects of this class can be reused within a <em>single</em>
 * thread.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class DfaRunFeeder extends AbstractPipe {
  private DfaRun dfa;
  private String csname;
  private DfaRun runner;

  /**
   * <p>when {@link #setOut setOut()} is called, it wraps the given
   * <code>OutputStream</code> into a <code>PrintStream</code> and stores it
   * here. This <code>PrintStream</code> is used by {@link #pipe pipe()} as
   * the output sink. Subclasses wrapping <code>pipe()</code> may use
   * <code>pout</code> to prefix or postfix the data send.</p>
   */
  protected PrintStream pout;

  /**
   * <p>creates the <code>Feeder</code> to filter <code>source</code>
   * with the given <code>dfa</code> and then feed it to an
   * <code>OutputStream</code> while encoding characters with the
   * character set given by <code>csname</code>.</p>
   *
   * @param r is the <code>DfaRun</code> from which to read input
   * data. May be <code>null</code>, but then it must be set
   * with {@link #setSource setSource()} before using this object.
   *
   * @param csname specifies the character set used to encode
   * characters when feeding them to the output stream.
   */
  public DfaRunFeeder(DfaRun r, String csname) 
    throws UnsupportedCharsetException
  { 
    this.runner = r;
    this.csname = csname;
    // trigger UnsupportedCharsetException rather now than in setOut
    Charset.forName(csname);
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
  public void setSource(DfaRun r) {
    this.runner = r;
  }
  /**********************************************************************/  
  protected void pipe() throws IOException {
    runner.filter(pout);
    pout.flush();
  }
  /**********************************************************************/
  public void setOut(OutputStream out, boolean closeOnExit) {
    super.setOut(out, closeOnExit);
    try {
      pout = new PrintStream(out, true, csname);
    } catch( UnsupportedEncodingException e ) {
      throw new Error("impossible", e);
    }
  }
}
/**********************************************************************/
