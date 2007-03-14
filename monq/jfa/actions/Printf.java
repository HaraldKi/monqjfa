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
import java.util.Map;

/**
 * <p>implements an <code>FaAction</code> to apply a {@link
 * PrintfFormatter} to the matched text. If a {@link MapProvider} is
 * found in the {@link monq.jfa.DfaRun#clientData} field of the
 * calling <code>DfaRun</code>, this is passed on to the
 * <code>Formatter</code>'s {@link Formatter#format format()} method
 * to allow for <q><code>%(key)</code></q> expansions. To store values
 * in the <code>Map</code>, use {@link Store}.</p>
 *
 * @see PrintfFormatter format strings
 * @author &copy; 2003, 2004 Harald Kirsch
 * @version $Revision: 1.11 $, $Date: 2005-07-31 12:57:54 $
 */
public class Printf extends AbstractFaAction {
  private Formatter f;
  private TextStore store = new TextStore();
  private TextSplitter splitter;

  /**
   * <p>creates an <code>FaAction</code> to format the matching text
   *  according to a {@link Formatter}.</p>
   *
   * @param sp is applied to the match to separate it into parts. This
   * may be <code>null</code> to indicate that submatches should be
   * used. Another useful special case is covered by {@link
   * TextSplitter#NULLSPLITTER}. 
   *
   * @param fmt is the <code>Formatter</code> to use to format the
   * match. The most common implementation is the {@link PrintfFormatter}.
   *
   * @see PrintfFormatter format strings
   */
  public Printf(TextSplitter sp, Formatter fmt, int prio) 
  {
    this.splitter = sp;
    this.f = fmt;
    this.priority = prio;
  }
  /**********************************************************************/
  /**
   * <p>create an <code>FaAction</code> to format a match according to
   * the given format string.</p>
   *
   * @param useSubmatches if <code>true</code>, the formatter will
   * have access to submatches recorded during the match. If
   * <code>false</code> only the format control "%0" is allowed and
   * refers to the whole match.
   *
   * @param format is used to create a {@link PrintfFormatter}.
   */

  public Printf(boolean useSubmatches, 
		String format) throws ReSyntaxException {
    if( useSubmatches ) {
      this.splitter = null;
    } else {
      this.splitter = TextSplitter.NULLSPLITTER;
    }
    this.f = new PrintfFormatter(format);
  }
  /**
   * @deprecated use the two parameter constructor and {@link
   * AbstractFaAction#setPriority}. 
   */
  public Printf(boolean useSubmatches, 
		String format, int prio) throws ReSyntaxException {
    this(useSubmatches, format);
    setPriority(prio);
  }
  /**
   * <p>creates an <code>FaAction</code> to format a match according to
   * the given format string. This is a shortcut for<pre>
   * Printf(false, format, 0)</pre>
   *</p>
   */
  public Printf(String format) throws ReSyntaxException {
    this(false, format, 0);
  }
  /**********************************************************************/
  public void invoke(StringBuffer out, int start, DfaRun runner) 
    throws CallbackException
  {
    TextStore ts;
    if( splitter!=null ) {
      ts = store;
      ts.clear();
      splitter.split(ts, out, start);
    } else {
      ts = runner.submatches(out, start);
    }

    Map m = null;
    MapProvider mp = null;
    try {
      mp = (MapProvider)runner.clientData;
      try {
	m = mp.getMap();
      } catch( NullPointerException e ) {
	// some comment as below
      }
    } catch( ClassCastException e ) {
      // yes, I intendly let the machine throw the exception instead
      // of checking myself, because this way it *is* faster.
    }


    out.setLength(start);
    f.format(out, ts, m);
  }
}
 
