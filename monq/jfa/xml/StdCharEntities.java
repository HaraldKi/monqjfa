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

package monq.jfa.xml;

import monq.jfa.*;
import monq.jfa.actions.*;

/**
 * <p>translates the standard character entities defined in the
 * XML standard into the characters the represent. For occasional use
 * call one of the static but synchronized methods. For heavy duty use
 * create an instance but use it only in one thread.</p>
 *
 * <p>The following replacements are made:</p>
 * <table style="font-family:monospace" border="1" align="center">
 *   <tr><th>from</th><th>to</th></tr>
 *   <tr><td>&amp;amp;</td><td>&amp;</td></tr>
 *   <tr><td>&amp;quot;</td><td>&quot;</td></tr>
 *   <tr><td>&amp;apos;</td><td>&apos;</td></tr>
 *   <tr><td>&amp;lt;</td><td>&lt;</td></tr>
 *   <tr><td>&amp;gt;</td><td>&gt;</td></tr>
 *  </table>
 * </p>
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class StdCharEntities {

  private static final Dfa toCharDfa;
  private static final Dfa toEntDfa;

  static {
    try {
      toCharDfa= new 
	Nfa("&amp;", new Replace("&"))
	.or("&quot;", new Replace("\""))
	.or("&apos;", new Replace("'"))
	.or("&lt;", new Replace("<"))
	.or("&gt;", new Replace(">"))
	// speedup
	.or("[^&]+", Copy.COPY)
	.compile(DfaRun.UNMATCHED_COPY);

      toEntDfa = new
	Nfa("&", new Replace("&amp;"))
	.or("\"", new Replace("&quot;"))
	.or("'", new Replace("&apos;"))
	.or("<", new Replace("&lt;"))
	.or(">", new Replace("&gt;"))
	// speedup
	.or("[^&\"'<>]+", Copy.COPY)
	.compile(DfaRun.UNMATCHED_COPY);
    } catch( ReSyntaxException e ) {
      ///CLOVER:OFF
      throw new Error("impossible", e);
      ///CLOVER:ON
    } catch( CompileDfaException e ) {
      ///CLOVER:OFF
      throw new Error("impossible", e);
      ///CLOVER:ON
    }
  }

  // use for the static methods
  private static final StdCharEntities instance = new StdCharEntities();


  // little helpers used 
  private CharSequenceCharSource in = new CharSequenceCharSource();
  private DfaRun toChar = new DfaRun(toCharDfa, in);
  private DfaRun toEnt = new DfaRun(toEntDfa, in);
  private StringBuffer sb = new StringBuffer();
  /**********************************************************************/
  /**
   * <p>For high throughput non-synchronized use in one thread, get one
   * instance of this class and reuse it consistently. For occasional
   * use which is not critical with regard to performance use one of
   * the static methods.</p>
   */
  public StdCharEntities() {
    // this is only here so that I can write the comment above
  }

  /**********************************************************************/
  /**
   * <p>replaces the standard character entities in place.</p>
   */
  private void convert(DfaRun r, StringBuffer text, int start) {
    in.setSource(text, start);
    sb.setLength(0);
    try {
      r.setIn(in);
      r.filter(sb);
    } catch( java.io.IOException e ) {
      throw new Error("impossible", e);
    }
    text.setLength(start);
    Misc.append(text, sb, 0, sb.length());
  }
  /**********************************************************************/
  public void decode(StringBuffer text, int start) {
    convert(toChar, text, start);
  }
  public void encode(StringBuffer text, int start) {
    convert(toEnt, text, start);
  }
  /**********************************************************************/
  /**
   * <p>replaces the standard character entities in place.</p>
   */
  public static synchronized void toChar(StringBuffer text, int start) {
    instance.decode(text, start);
  }
  public static synchronized void toEntities(StringBuffer text, int start) {
    instance.encode(text, start);
  }
  /**********************************************************************/
  /**
   * <p>replaces the standard character entities in <code>s</code> and
   * returns the result.</p>
   */
  public static synchronized String toChar(CharSequence s) {
    StringBuffer tmp = new StringBuffer(s.length());
    tmp.append(s);
    instance.decode(tmp, 0);
    return tmp.toString();
  }
  public static synchronized String toEntities(CharSequence s) {
    StringBuffer tmp = new StringBuffer(s.length());
    tmp.append(s);
    instance.encode(tmp, 0);
    return tmp.toString();
  }
}
