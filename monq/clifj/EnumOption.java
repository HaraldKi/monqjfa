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

package monq.clifj;

/**
 * <p>is a command line option with <code>String</code> valued arguments
 * from a small set of given strings.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class EnumOption extends Option {
  private String[] allowed;

  /**
   * <p>creates an option which accepts string values from small
   * enumerated set of values. Parameter <code>allowed</code> must
   * contain the enumerated values as follows: The first character in
   * <code>allowed</code> is used as a separator. Then
   * <code>allowed</code> is split at every position of where the
   * separator appears, and the resulting substrings make up the
   * enumerated values. For example <code>"|one|two|three"</code>
   * defines the enumeration <code>{"one", "two", "three"}</code>.</p>
   * 
   * <p><b>Notes:</b>
   * <ul>
   * <li>No effort is made to check whether the given strings are
   * unique.</li> 
   * <li>A trailing separator or two consecutive separators generate
   * the empty string.</li>
   * </ul>
   */
  public EnumOption(String opt, String name, String usage,
		    int cmin, int cmax, String allowed,
		    String[] deFault) {
    super(opt, name, usage, cmin, cmax, deFault);
    java.util.Vector v = new java.util.Vector();
    char sep = allowed.charAt(0);
    int end = allowed.length();
    while( end>0 ) {
      int start=end-1;
      while( allowed.charAt(start)!=sep ) start-=1;
      v.add(allowed.substring(start+1, end));
      end = start;
    }
    int L = v.size();
    this.allowed = new String[L];
    for(int i=0; i<L; i++) this.allowed[i] = (String)v.get(L-i-1);
  }
  /**********************************************************************/
  public Object check(String s) throws CommandlineException {
    for(int i=0; i<allowed.length; i++) {
      if( s.equals(allowed[i]) ) return allowed[i];
    }
    throw new CommandlineException
      ("option `"+opt+"' does not accept the value `"+s+
       "'; allowed values are `"+allowedUsage()+"'");
  }
  /**********************************************************************/
  public String addRestrictions(String s) {
    s = s + " from the list {"+allowedUsage()+"}";
    return s;
  }
  /**********************************************************************/
  private String allowedUsage() {
    StringBuffer b = new StringBuffer();
    b.append(allowed[0]);
    for(int i=1; i<allowed.length; i++) {
      b.append(", ").append(allowed[i]);
    }
    return b.toString();
  }
  /**********************************************************************/
}
