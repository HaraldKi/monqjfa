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
 * is thrown whenever a syntax error is found in a regular
 * expression. 
 * @author (C) 2003 Harald Kirsch
 * @version $Revision: 1.8 $, $Date: 2005-02-14 10:23:38 $
 */
public class ReSyntaxException extends Exception {
  public static final String EBSATEOF = "backslash at end of input";
  public static final String EEXTRACHAR =
    "extra characters after regular expression";
  public static final String EINVALUL = 
    "invalid upper limit of character range";
  public static final String EINVRANGE = "inverted character range";
  public static final String ECLOSINGP = "missing closing parenthesis";
  //public static final String ECLOSINGB = "missing closing bracket";
  public static final String EEOFUNEX = "unexpected end of input";
  public static final String ECHARUNEX = "unexpected character";

  public static final String EFORMAT = "unknown format directive";
  public static final String ETOOMANYREPORTING = 
    "too many ( >255) reporting subexpressions";

  // the following three are related to subexpression tags like @12@
  public static final String EATDIGIT 
    = "there must be a small nonnegative integer after '@'";
  public static final String EATRANGE
    = "the integer after '@' must be in the range [0,127]";
  public static final String EATMISSAT 
    = "missing closing '@'";

  public String emsg;
  public int line;
  public int column;
  public String text;
  public ReSyntaxException(String emsg, 
			    String text,
			   int line, 
			   int column) {
    this.emsg = emsg;
    this.line = line;
    this.column = column;
    this.text = text;
  }
  public String toString() {
    StringBuffer b = new StringBuffer(300);
    b.append("ReSyntaxException: ").append(emsg)
      .append(".\n  ");
    int realColumn = column;

    for(int i=0; i<text.length(); i++) {
      int L = b.length();
      b.append(Misc.printable(text.charAt(i)));
      realColumn += (b.length()-L-1);
    }
    b.append("\n");
    for(int i=0; i<realColumn+1; i++) b.append(' ');
    b.append('^');
    return b.toString();
  }
}
