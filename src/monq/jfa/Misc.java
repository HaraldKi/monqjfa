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

import monq.jfa.actions.*;

/**
 * Miscellaneous static functions.
 *
 * @author &copy; 2003,2004,2005 Harald Kirsch
 */
public class Misc {

  // this class cannot be instantiated
  private Misc() {}

  /**********************************************************************/
  /**
   * <p>wraps a {@link Nfa} into another one which defines a <em>region
   * of interest</em> (ROI). The ROI is defined by two regular
   * expressions, <code>reStart</code> and <code>reEnd</code>. This
   * method creates a new <code>Nfa</code>, call it <em>envelope</em>,
   * which only contains 
   * <code>reStart</code>. If <code>reStart</code> is found, it switches
   * to the given <code>Nfa</code> <code>client</code>. The
   * <code>client</code> is augmented by <code>reEnd</code> such that
   * it switches back to the <em>envelope</em> when <code>reEnd</code>
   * is encountered.</p>
   *
   * <p><b>Note 1:</b> If <code>reEnd</code> interferes with any regular
   * expressions already in <code>client</code>, detection of the end
   * of the ROI may fail.</p>
   *
   * <p><b>Note 2:</b> The <code>client</code> <code>Nfa</code> is
   * compiled in this method and consequently cannot be used anymore
   * afterwards. 
   *
   * @param reStart is the regular expression defining the start of the
   * ROI
   * @param startAction is the action called when <code>reStart</code>
   * is found. If <code>null</code>, {@link monq.jfa.actions.Copy} is
   * assumed. 
   * @param inRoi specifies how non matching input is to be handled
   * within the ROI
   * @param reEnd is the regular expression defining the end of the ROI
   * @param endAction is the action called when <code>reEnd</code> is
   * found.  If <code>null</code>, {@link monq.jfa.actions.Copy} is
   * assumed. 
   * @param outsideRoi specifies how non matching input is to be handled
   * outside the ROI
   * @param client is the <code>Nfa</code> to be wrapped.
   *
   * @throws ReSyntaxException if <code>reStart</code> or
   * <code>reEnd</code> contain syntax errors.
   * @throws CompileDfaException if <code>client</code>, after adding
   * <code>reEnd</code> cannot be compiled.
   */
  public static Dfa wrapRoi(CharSequence reStart,
			    FaAction startAction,
			    DfaRun.FailedMatchBehaviour inRoi,
			    CharSequence reEnd,
			    FaAction endAction,
			    DfaRun.FailedMatchBehaviour outsideRoi,
			    Nfa client) 
    throws ReSyntaxException, CompileDfaException
  {
    SwitchDfa toClient = new SwitchDfa(startAction);
    SwitchDfa toEnvelope = new SwitchDfa(endAction);
    Dfa envelope = new Nfa(reStart, toClient).compile(outsideRoi);
    Dfa clientDfa = client.or(reEnd, toEnvelope).compile(inRoi);
    toClient.setDfa(clientDfa);
    toEnvelope.setDfa(envelope);
    return envelope;
  }
  /**********************************************************************/
  /**
   * converts a character into something which can be printed on the
   * console 
   */
  public static String printable(char ch) {
    StringBuffer out = new StringBuffer();
    printable(out, ch);
    return out.toString();
  }

  public static void printable(StringBuffer out, char ch) {
    int v = (int)ch;
    if( v<32 || v>=128 ) {
      if( v==9 )   { out.append("\\t"); return; }
      if( v==10 )  { out.append("\\n"); return; }
      if( v==13 )  { out.append("\\r"); return; }
      out.append("\\\\u").append(Integer.toHexString(v));
    } else if( v=='"' ) { 
      out.append("\\\""); return; 
    } else {
      out.append(ch);
    }    
  }

  public static CharSequence printable(CharSequence s) {
    StringBuffer sb = new StringBuffer(s.length()+10);
    for(int i=0; i<s.length(); i++ ) {
      sb.append(printable(s.charAt(i)));
    }
    return sb.toString();
  }

  /**
  * There is no method in StringBuffer to append a substring of
  * another StringBuffer. Consequently, a substring would have to be
  * created from the source to be appended to the destination. This
  * results in two copy operations plus a mandatory new String object
  * generated. In addition, the latter forces reallocation of the
  * source StringBuffer's internal buffer as soon as it is changed. In
  * 1.5 it will be possible to append a substring of a CharSequence to
  * a StringBuffer, but looking into the implementation, I find a
  * character-by-character copying with charAt(). Consequently, I
  * prefer this method. It copies the characters twice, but spares
  * reallocation of a new object most of the time and uses (hopefully)
  * fast array copying.
  */
  public static StringBuffer append(StringBuffer dst, 
		       StringBuffer src, int start, int end) {
    synchronized( appendBuf ) {
      int len = end-start;
      if( len>appendBuf.length ) appendBuf = new char[len];
    
      src.getChars(start, end, appendBuf, 0);
      dst.append(appendBuf, 0, len);
    }
    return dst;
  }
  private static char[] appendBuf = new char[100];
}
