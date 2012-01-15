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
 * <p>convenience class for matching regular expressions.</p>
 *
 * <blockquote><b>Important Note:</b> If you find yourself using
 * <code>Regexp</code> objects in a loop, exercising a lengthy input,
 * you are on the wrong track. Consider using a <code>DfaRun</code> to
 * do the looping for you and concentrate on the work to be done for
 * each match.</blockquote>
 *
 * <p>When you don't need high speed filtering of large amounts of
 * input data, using <code>Nfa/Dfa/FaAction/DfaRun</code> is a bit
 * tedious for <em>just</em> checking whether a string matches a
 * regular expression. This class provides a simpler interface.</p>
 *
 * <p><b>Note that this implementation is not synchronized.</b> Objects
 * of this class should never be shared between threads without
 * explicit synchronization by other means.</p>
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class Regexp {
  private Dfa dfa;
  private CharSequenceCharSource in = new CharSequenceCharSource();
  private StringBuffer out = new StringBuffer();
  private SubmatchData smd = new SubmatchData();
  private TextStore ts = new TextStore();
  boolean analyzed;
  FaAction a;

  /**********************************************************************/
  /**
   * <p>create a <code>Regexp</code> object for the given regular
   * expression.</p>
   *
   * @throws IllegalArgumentException in case
   * <code>re</code> contains syntax errors.
   *
   * @see <a href="doc-files/resyntax.html">regular expression syntax</a>
   */
  public Regexp(CharSequence re) {
    try {
      dfa = new Nfa(re, Copy.COPY).compile(DfaRun.UNMATCHED_COPY);
    } catch( ReSyntaxException e ) {
      throw makeEx("regexp syntax error", e);
    } catch( CompileDfaException e ) {
      throw new Error("impossible", e);
    }
  }
  /**********************************************************************/
  private IllegalArgumentException makeEx(String msg, Throwable cause) {
    IllegalArgumentException e = new IllegalArgumentException(msg);
    e.initCause(cause);
    return e;
  }
  /**********************************************************************/
  /**
   * <p>checks if the whole input sequence starting at position
   * <code>pos</code> can be matched.</p>
   *
   * @return <code>true</code> iff the whole input sequence starting
   * at position <code>pos</code> and ending at
   * <code>s.length()</code> can be matched.
   */
  public boolean matches(CharSequence s, int pos) {
    return atStartOf(s, pos)!=-1 && out.length()==s.length()-pos;
  }
  /**********************************************************************/
  /**
   * <p>checks if the whole input sequence can be matched. This method
   * is equivalent to a call to <code>matches(s, 0)</code>.</p>
   */
  public boolean matches(CharSequence s) { return matches(s, 0); }
  /**********************************************************************/
  /**
   * <p>checks if the complete tail of <code>s</code> starting at
   * <code>start</code> matches <code>regexp</code>.</p>
   *
   * <p>For anything but
   * casual uses consider to create a <code>Regexp</code> object and
   * reuse it.</p>
   */
  public static boolean matches(CharSequence regexp, 
				CharSequence s, int start) {
    return new Regexp(regexp).matches(s, start);
  }
  /**********************************************************************/
  /**
   * <p>tries to find <code>this</code> in <code>s</code> starting at
   * position <code>start</code>.</p>
   *
   * @return the position of the match within <code>s</code>,
   * i.e. <b>not</b> relative to <code>start</code>.
   * If not match can be found, -1 is returned.
   */
  public int find(CharSequence s, int start) {
    analyzed = false;
    in.setSource(s, start);
    out.setLength(0);
    int l = s.length();
    while( start<l ) {
      try {
	a = dfa.match(in, out, smd);
      } catch( java.io.IOException e ) {
	throw new Error("impossible", e);
      }
      if( a!=null && a!=DfaRun.EOF )  return start;
      start += 1;
      in.read();
    }
    return -1;
  }
  /**********************************************************************/
  /**
   * <p>tries to find <code>this</code> in <code>s</code>. This method
   * is equivalent to a call to <code>find(s, 0)</code>.</p>
   */
  public int find(CharSequence s) { return find(s, 0); }
  /**********************************************************************/
  /**
   * <p>tries to find <code>regexp</code> in <code>s</code> starting at
   * position <code>start</code>.</p>
   *
   * <p>For anything but casual uses consider to
   * create a <code>Regexp</code> object and reuse it.</p>
   *
   * @return the position of the match within <code>s</code>,
   * i.e. <b>not</b> relative to <code>start</code>.
   * If no match can be found, -1 is returned.
   */  
  public static int find(CharSequence regexp, CharSequence s, int start) {
    return new Regexp(regexp).find(s, start);
  }
  /**********************************************************************/
  /**
   * <p>tests whether <code>this</code> matches a prefix of
   * <code>s</code> starting at position <code>pos</code>.</p>
   *
   * @return the length of the match or -1.
   */
  public int atStartOf(CharSequence s, int pos) {
    analyzed = false;
    in.setSource(s, pos);
    out.setLength(0);
    try {
     a = dfa.match(in, out, smd);
    } catch( java.io.IOException e ) {
      throw new Error("impossible", e);
    }
    return (a!=null && a!=DfaRun.EOF) ? out.length() : -1;
  }
  /**********************************************************************/
  /**
   * <p>tests whether <code>this</code> matches a prefix of
   * <code>s</code>. This method
   * is equivalent to a call to <code>atStartOf(s, 0)</code>.</p>
   */
  public int atStartOf(CharSequence s) { return atStartOf(s, 0); }
  /**********************************************************************/
  /**
   * <p>tests whether <code>regexp</code> matches within
   * <code>s</code> at position <code>start</code>.</p>
   *
   * <p>For anything but
   * casual uses consider to create a <code>Regexp</code> object and
   * reuse it.</p>
   * 
   * @return the length of the match or -1.
   */
  public static int atStartOf(CharSequence regexp, 
			      CharSequence s, int start) {
    return new Regexp(regexp).atStartOf(s, start);
  }
  /**********************************************************************/
  /**
   * <p>returns the number of characters matched by the most recent
   * match call to any of <code>matches</code>,
   * <code>atStartOf</code> or <code>find</code>.</p> 
   *
   * @throws IllegalStateException if the most recent application of
   * <code>this</code> did not yield a match.
   */
  public int length() {
    if( a==null || a==DfaRun.EOF ) {
      throw new IllegalStateException("no recent match available");
    }
    return out.length();
  }
  /**********************************************************************/
  /**
   * <p>returns a <code>TextStore</code> which contains submatches, if any,
   * pertaining to the most recent match. The object returned should
   * be treated read-only. Its contents are only valid until the next
   * match operation.</p>
   *
   * @throws IllegalStateException if the most recent application of
   * <code>this</code> did not yield a match.
   */
  public TextStore submatches() {
    if( a==null || a==DfaRun.EOF ) {
      throw new IllegalStateException("no recent match available");
    }
    if( !analyzed ) {
      analyzed = true;
      ts.clear();
      ts.appendPart(out, 0, out.length());
      smd.analyze(ts, a);
    }
    return ts;
  }
  /**********************************************************************/

}
 
