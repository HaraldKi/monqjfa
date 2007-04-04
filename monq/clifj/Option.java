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

import java.util.Vector;

/**
 * is a general command line option with zero or more string values.
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class Option {
  String opt;
  String usage;
  String name;
  int cmin;
  int cmax;
  Vector values = null;
  Vector defalt = null;
  boolean required = false;

  protected Option() {}
  /**********************************************************************/
  /**
   * <p>creates a command line option.</p>
   *
   * @param opt is the option string introducing this option on the
   * command line and normally starts with a dash,
   * i.e. <code>"-v"</code>.
   * @param name is a symbolic name for the <em>variable</em>
   * initialized by the option, only used in <code>usage</code>
   * output.
   * @param usage is a one liner describing the intend of this option
   * @param cmin is the minimum number of command line arguments for
   * this option
   * @param cmax is the maximum number of command line arguments for
   * this option
   * @param defalt is a list of objects which are used if the option
   * does not appear on the command line, may be null to indicate that
   * no default is given.
   */
  public Option(String opt, String name, String usage,
		int cmin, int cmax, Object[] defalt) {
    this.opt = opt;
    this.name = name;
    this.usage = usage;
    this.cmin = cmin;
    this.cmax = cmax;
    
    if( cmin>cmax ) {
      throw new IllegalArgumentException("cmin="+cmin
          +" greater than cmax="+cmax);
    }
    if( defalt!=null ) {
      this.defalt = new Vector(defalt.length);
      for(int i=0; i<defalt.length; i++) this.defalt.add(defalt[i]);
    }
  }
  /**********************************************************************/
  protected String getTypeName() {return "string";}
  /**********************************************************************/
  protected String getOpt() {return opt;}
  /**********************************************************************/
  protected boolean isRequired() {return required;}
  /**********************************************************************/
  /**
   * converts the option into a required command line argument and
   * {@link Commandline#parse Commandline.parse()} will check this.</p>
   * @return <code>this</code>
   */
  public Option required() {required = true; return this;}
  /**********************************************************************/
  /**
   * returns true if the option was found on the command line or has
   * default values.
   */
  public boolean available() {
    return values!=null || defalt!=null;
  }
  /**********************************************************************/
  /**
   * returns the arguments found on the command line or set as a
   * default for this option. May be <code>null</code>. The elements
   * of the resulting vector are of type <code>String</code> or
   * whatever was passed in as the <code>defalt</code> parameter of
   * the constructor. For subclasses of <code>Option</code>, different
   * types may be returned.
   */
  public Vector getValues() {
    return values==null ? defalt : values;
  }
  /**
   * returns the first element of the vector returned by
   * <code>getValues</code>. This is a convenience method normally to
   * be used only for options with exactly one parameter. If the
   * option is indeed "optional", a call to {@link #available
   * available()} should preceed this call.
   */
  public Object getValue() {
    Vector v = getValues();
    return v.get(0);
  }
  /**********************************************************************/
  protected void setValues(Vector v) throws CommandlineException {
    int L = v.size();
    if( L<cmin ) {
      throw new CommandlineException
	("not enough `"+name+"' arguments (option '"+opt+"'), found "
	 +L+" but need "+cmin);
    }

    if( L>cmax ) {
      throw new CommandlineException
	("to many `"+name+"' arguments (option '"+opt+"'), found "+L
	 +" but want no more than "+cmax);
    }
    this.values = v;
  }
  /**********************************************************************/
  protected Object check(String s) throws CommandlineException { 
    return s; 
  }
  /**********************************************************************/
  protected int parse(String[] argv, int startAt) throws CommandlineException {
    int i = startAt;
    if( values!=null ) {
      throw new CommandlineException
	("option `"+opt+"' used more than once");
    }
    values = new Vector();
    while( i<argv.length && values.size()<cmax 
	   && (!argv[i].startsWith("-") || values.size()<cmin) ) {
      //System.err.println(opt+i);
      String s;
      if( argv[i].startsWith("@") ) s = argv[i].substring(1);
      else s = argv[i];
      values.add(check(s));
      i += 1;
    }

    if( values.size()<cmin ) {
      throw new CommandlineException
	("not enough `"+name+"' arguments (option '"+opt+"'), found "
	 +values.size()+" but want "+cmin);
    }
    return i;
  }
  /**********************************************************************/
  protected String shortUsage() {
    String s = "";
    if( !required ) s = s+"[";
    if( opt.equals("--") ) s = s + "[--]";
    else s = s + opt;
    if( cmax>0 ) {
      if( cmin==0 ) s = s+" [";
      else s = s + ' ';
      s = s + name;
      if( cmax>1 ) s = s + " ...";
      if( cmin==0 ) s = s+"]";
    }
    if( !required ) s = s+"]";
    return s;
  }
  /**********************************************************************/
  /**
   * <p>override in subclass to describe restrictions applied to
   * values, like numerical range. The restrictions should describe
   * what {@link #check check()} checks.</p>
   */
  protected String addRestrictions(String s) {return s;}
  /**********************************************************************/
  protected String usage() {
    String s;
    // except for '--' we use the option at the start of the line. In
    // former times I always used the name, but this was confusing.
    if( "--".equals(opt) ) s = name;
    else s = opt;

    s = s + ": "+this.usage;

    if( cmax==0 ) {
      // this is a BooleanOption or perverted other option handled
      // like it
      return s;
    }

    // add something like ", usage bla, 3 to 43 blurb values"
    s = s + ",\n";
    String v = " values";
    if( cmin==cmax ) {
      s = s + cmin;
      if( cmin==1 ) v = " value";
    } else if( cmax==Integer.MAX_VALUE ) {
      s = s + cmin+" or more";
    } else {
      s = s+cmin+" to "+cmax;
    }
    s = s + " "+getTypeName() + v;

    s = addRestrictions(s);

    // add default if specified
    if( defalt!=null ) {
      s = s + ",\ndefault: `";
      for(int i=0; i<defalt.size(); i++) {
	if( i>0 ) s = s + ' ';
	s = s + defalt.get(i);
      }
      s = s + "'";
    }
    return s;
  }
}
