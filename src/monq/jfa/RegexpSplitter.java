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

/**
 *  <p>implements a {@link TextSplitter} to split text according to a
 *  regular expression.</p>
 *
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.11 $, $Date: 2005-07-08 13:01:26 $
 */
public class RegexpSplitter implements TextSplitter {
  
  Dfa dfa;
  
  /** 
   * constructor parameter to split text according to a regular
   * expression.
   */
  public static final int SPLIT = 1;

  /** 
   * constructor parameter to fetch parts defined by a regular
   * expression.
   */
  public static final int FETCH = 2;

  /** 
   * constructor parameter to separate text according to a regular
   * expression into matching and non-matching parts.
   */
  public static final int SEP = SPLIT|FETCH;

  // if true, also non-matching stretches form a part with the result
  // that all generated parts together comprise exactly the full
  // split string. 
  private boolean split;

  // if true, the matching parts are dropped. This only makes sense,
  // if at the same time separate==true;
  private boolean fetch;
  /**
   * <p>creates a {@link TextSplitter} to split text according the
   * given regular expression <code>re</code>. When {@link #split
   * split()} is invoked, it will first partition the input string
   * into matching and non-matching parts. According to parameter
   * <code>what</code> these parts are then entered into the {@link
   * TextStore} given to <code>split</code>:
   * <ul>
   * <li>{@link #SPLIT} &mdash; all non-matching parts are entered.</li>
   * <li>{@link #FETCH} &mdash; all matching parts are entered.</li>
   * <li>{@link #SEP} &mdash; all parts are entered i.e. the input is
   * <em>SEP</em>arated into parts</li>
   * </ul>
   * </p>
   *
   *
   * @param re a regular expression suitable for {@link monq.jfa.Nfa}. The
   * regular expression must not match the empty string.
   *
   * @param what should be one of {@link #SPLIT}, {@link #FETCH} or
   * {@link #SEP}.
   *
   * @exception ReSyntaxException if the given string is not a well
   * formed regular expression
   * @exception IllegalArgumentException if the given regular expression
   * matches the empty string.
   */
  public RegexpSplitter(CharSequence re, int what) 
    throws ReSyntaxException
  {
    ///CLOVER:OFF
    if( what<SPLIT || what>SEP ) {
      throw new IllegalArgumentException("parameter 'what' out of range");
    }
    ///CLOVER:ON
    split = (what&SPLIT)!=0;
    fetch = (what&FETCH)!=0;
    try {
      dfa = new Nfa(re, monq.jfa.actions.Copy.COPY)
	.compile(DfaRun.UNMATCHED_COPY);
    } catch( CompileDfaException e ) {
      ///CLOVER:OFF
      throw new Error("impossible", e);
      ///CLOVER:ON
    }
    if( dfa.matchesEmpty() ) {
      throw new IllegalArgumentException("dfa matches the empty string");
    }
  }
  
  public void split(TextStore dst, StringBuffer s, int start) {

    // We will use the tail of s also as a scratch
    // area. CharSequenceCharSource allows this by storing the end of
    // the CharSequence locally.
    CharSource in = new CharSequenceCharSource(s, start);
    DfaRun r = new DfaRun(dfa, in);

    // secure part 0
    int l = s.length();
    dst.appendPart(s, start, s.length());
    
    int recent = l;

    while( true ) {
      FaAction a;

      try {
	a = r.next(s);
      } catch( java.io.IOException e ) {
	throw new Error("impossible", e);
      }

      int ms = r.matchStart();
      if( ms>recent && split ) dst.addPart(recent-l, ms-l);
      
      if( a==DfaRun.EOF ) break;

      recent = s.length();
      if( fetch ) dst.addPart(ms-l, recent-l);
    }
    
    s.setLength(start);
  }
  /**********************************************************************/  
}
 
