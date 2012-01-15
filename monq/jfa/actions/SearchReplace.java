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

package monq.jfa.actions;

import monq.jfa.*;

/**
 * <p>implements an {@link FaAction} which scans the matched text for a
 * given regular expression and replaces those matches by applying a
 * {@link PrintfFormatter}.</p>
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.9 $, $Date: 2005-07-08 13:01:26 $
 */
public class SearchReplace extends AbstractFaAction {
  private final Dfa dfa;
  private final int count;
  private final TextSplitter splitter;
  private final Formatter f;

  /*******************************************************************/
  /**
   * <p>creates an action to search and replace within a match. Within
   * the match, <code>regexp</code> is sought consecutively up to
   * <code>count</code> times. If <code>sp</code> is not
   * <code>null</code>, it is applied to
   * separate the text matching <code>regexp</code> into
   * parts. Otherwise the reported submatches <code>regexp</code> are
   * used by <code>fmt</code> to rewrite the text matched by
   * <code>regexp</code>.</p>
   *
   * @param sp may be null in which case submatches of
   * <code>regexp</code> are used as the parts. 
   *
   * @param count is the number of replacements to make. If
   * <code>count</code> is negative, up to 2<sup><span
   * style="font:smaller">32</span></sup> matches 
   * (i.e. all) are replaced.</p>
   *
   * @see monq.jfa.PrintfFormatter format string
   */
  public SearchReplace(String regexp, TextSplitter sp, Formatter fmt,
		       int count)
    throws ReSyntaxException, CompileDfaException
  {
    this.splitter = sp;
    this.count = count;
    this.f = fmt;
    this.dfa = new Nfa(regexp, new AbstractFaAction() {
	public void invoke(StringBuffer out, int start, DfaRun r) 
	  throws CallbackException
	{
	  Info info = (Info)r.clientData;
	  if( info.count==0 ) return;
	  info.count -= 1;
	  TextStore store;
	  if( splitter!=null ) {
	    store = info.ts;
	    store.clear();
	    splitter.split(store, out, start);
	  } else {
	    store = r.submatches(out, start);
	  }
	  out.setLength(start);
	  f.format(out, store, null);
	}
      }).compile(DfaRun.UNMATCHED_COPY);
  }
  /**
   * <p>calls the four parameter constructor with <code>sp=null</code> and
   * <code>count==-1</code>.</p>
   */
  public SearchReplace(String regexp, String format) 
    throws ReSyntaxException, CompileDfaException
  {
    this(regexp, null, new PrintfFormatter(format), -1);
  }
  /**
   * calls the four parameter constructor with
   * <code>sp==null</code>.
   */
  public SearchReplace(String regexp, String format, int count) 
    throws ReSyntaxException, CompileDfaException
  {
    this(regexp, null, new PrintfFormatter(format), count);
  }
  /*******************************************************************/
  private static final class Info {
    public TextStore ts = null;
    public int count;
    public Info(int count) { this.count = count; }
  }
  /*******************************************************************/
  public void invoke(StringBuffer out, int start, DfaRun runner) {
    CharSequenceCharSource in = 
      new CharSequenceCharSource(out.substring(start));
    DfaRun r = new DfaRun(dfa, in);
    Info info = new Info(count);

    if( splitter!=null ) info.ts = new TextStore();
    r.clientData = info;
    out.setLength(start);
    try {
      r.setIn(in);
      r.filter(out);
    } catch( java.io.IOException e ) {
      ///CLOVER:OFF
      throw new Error("impossible");
      ///CLOVER:ON
    }
  }
}
 
