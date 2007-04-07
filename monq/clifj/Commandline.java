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

import java.util.*;
import java.util.Vector;

/**
 * describes the structure of the command line with options. Use
 * {@link #addOption addOption} to add options to the command
 * line. Then call {@link #parse parse} to parse a command line. The
 * retrieve command line paramters with {@link #getValues getValues}.
 * @author (C) 2003 Harald Kirsch
 * @version $Revision: 1.8 $, $Date: 2005-02-14 10:23:37 $
 */
public class Commandline {

  private Map<String,Option> options = new HashMap<String,Option>();
  Vector<String> optOrder = new Vector<String>();
  public String ProgramName;
  String usage;

  /**********************************************************************/
  /**
   * creates a <code>Commandline</code> object which allows non option
   * arguments.
   * @param ProgramName is used as the name of the program in the
   * usage message generated when unknown options are used. Typically
   * the string <code>System.getProperty("argv0")</code> is passed here.
   * @param usage a one-liner summarizing the function of the program.
   * @param restName name used to denote non option arguments in the
   * usage message
   * @param restInfo one liner describing the meaning of non option
   * arguments 
   * @param cmin minimum number of non option arguments required
   * @param cmax maximum number of non option arguments allowed. Use
   * <code>Integer.MAX_VALUE</code> to define that (almost) any number of
   * arguments is allowed.
   */
  public Commandline(String ProgramName, String usage,
		     String restName,
		     String restInfo,
		     int cmin, int cmax) {
    this.ProgramName = ProgramName;
    this.usage = usage;

    // The rest handled in a special way all over. But some things can
    // be handled automatically by adding it as if it were an
    // option. In particular we block "--" to be the name of an
    // option.
    if( cmax>0 ) {
      Option rest = new Option("--", restName, restInfo, cmin, cmax);
      if( cmin>0 ) rest.required();
      addOption(rest);
    }
  }
  /**********************************************************************/
  /**
   * creates a <code>Commandline</code> object which does not allow
   * non option arguments. This is a convenience constructor which
   * calls the 6 parameter constructor with suitable parameters.
   */
  public Commandline(String ProgramName, String usage) {
    this(ProgramName, usage, null, null, 0, 0);
  }
  /**********************************************************************/
  private static void breakLine(StringBuffer s, int startLine, int indent) {
    int M = 77;
    while( s.length()-startLine>M ) {
      int lastBlank = -1;
      int end = startLine+M;
      int i=startLine;
      //System.out.println("rest: "+s.substring(startLine));
      while( i<end && s.charAt(i)!='\n' ) {
	if( s.charAt(i)==' ' ) lastBlank = i;
	i+=1;
      }
      if( s.charAt(i)=='\n' ) {
	// have a nice breakpoint already, use it
	startLine = i+1;
      } else if( i-lastBlank>15 ) {
	// need to break in the middle of a word, too bad
	s.insert(i-15, '\n');
	startLine = i-14;
      } else {
	s.setCharAt(lastBlank, '\n');
	startLine = lastBlank+1;
      }
      for(i=0; i<indent; i++) s.insert(startLine, ' ');
    }
    // Even in the rest, there may be \n which need indenting
    while( startLine<s.length() ) {
      if( s.charAt(startLine)=='\n' ) {
	startLine+=1;
	for(int i=0; i<indent; i++) s.insert(startLine, ' ');
      } else {
	startLine+=1;
      }
    }
	
  }
  /**********************************************************************/
  private String usage() {
    StringBuffer s = new StringBuffer();
    s.append("usage: ").append(ProgramName);

    Vector<String> lines = new Vector<String>();

    for(int i=0; i<optOrder.size(); i++) {
      String opt = (String)optOrder.get(i);
      s.append(' ').append(((Option)options.get(opt)).shortUsage());
      lines.add(((Option)options.get(opt)).usage());
    }
    s.append("\n  ").append(usage).append('\n');

    // figure out indentation
    int maxPos = 0;
    for(int i=0; i<lines.size(); i++) {
      int p = ((String)lines.get(i)).indexOf(':');
      if( p>maxPos ) maxPos = p;
    }
    if( maxPos>20 ) maxPos=20;

    // append to s with good indentation

    for(int i=0; i<lines.size(); i++) {
      int startLine = s.length();
      int p = ((String)lines.get(i)).indexOf(':');
      for(int j=0; j<maxPos-p+2; j++) s.append(' ');
      s.append((String)lines.get(i));
      breakLine(s, startLine, maxPos+4);
      s.append('\n');
      //s = s + blank.substring(0, maxPos-p+2) + (String)lines.get(i) + '\n';
    }
    return s.toString();
  }
  /**********************************************************************/
  public Commandline addOption(Option option) {
    String opt = option.getOpt();
    if( options.containsKey(opt) ) {
      throw new 
	IllegalArgumentException("option "+opt+" already specified");
    }
    options.put(opt, option);
    optOrder.add(opt);
    return this;
  }
  /**********************************************************************/
  /**
   * <p>returns the arguments found on the command line or set as a
   * default for this option. May be <code>null</code>. The elements
   * of the resulting vector are of type <code>String</code> or
   * whatever was passed in as the <code>defalt</code> parameter of
   * the constructor. For subclasses of <code>Option</code>, different
   * types may be returned. To access any arguments which were not
   * assigned to an option, call this function with
   * <code>"--"</code>.</p>
   * 
   */
  public Vector<Object> getValues(String opt) {
    return options.get(opt).getValues();
  }
  /**********************************************************************/
  public String[] getStringValues(String opt) {
    Vector<Object> v = getValues(opt);
    String[] res = new String[v.size()];
    return (String[])v.toArray(res);
  }
  /**********************************************************************/
  public long[] getLongValues(String opt) {
    Vector v = getValues(opt);
    long[] res = new long[v.size()];
    for(int i=0; i<v.size(); i++) {
      res[i] = ((Long)v.get(i)).longValue();
    }
    return res;
  }
  /**********************************************************************/
  /**
   * returns the first element of the vector returned by
   * <code>getValues</code>. This is a convenience method normally to
   * be used only for options with exactly one parameter. If the
   * option is indeed "optional", a call to {@link #available
   * available()} should preceed this call.
   */
  public Object getValue(String opt) {
    return ((Option)options.get(opt)).getValue();
  }
  /**********************************************************************/
  /**
   * <p>
   * returns the first element of the vector returned by
   * {@link #getValues(String)} and casted to <code>String</code>. Use this
   * method only if you are sure that the requested value indeed exists (by
   * definition and/or checking with {@link #available(String)}.
   * </p>
   * 
   * @return first element of the options casted to String.
   */
  public String getStringValue(String opt) {
    return (String)getValue(opt);
  }
  /**********************************************************************/
  /**
   * <p>
   * returns the first element of the vector returned by
   * {@link #getValues(String)} and casted and converted to <code>long</code>.
   * Use this method only if you are sure that the requested value indeed exists
   * (by definition and/or checking with {@link #available(String)}.
   * </p>
   * 
   * @return first element of the options casted to String.
   */
  public long getLongValue(String opt) {
    return ((Long)getValue(opt)).longValue();
  }
  /**********************************************************************/
  /**
   * @deprecated defaults can be provided in the declaration of an option. If a
   *             dynamic default is needed, test first with
   *             {@link #available(String)}.
   */ 
  public String getStringValue(String opt, String defalt) {
    if( !available(opt) ) return defalt;
    return (String)getValue(opt);
  }
  /**********************************************************************/
  /**
   * @deprecated defaults can be provided in the declaration of an option. If a
   *             dynamic default is needed, test first with
   *             {@link #available(String)}.
   */ 
  public long getLongValue(String opt, long defalt) {
    if( !available(opt) ) return defalt;
    return ((Long)getValue(opt)).longValue();
  }
  /**********************************************************************/
  /**
   * <p>
   * returns true if the option was found on the command line or has default
   * values.
   * </p>
   * 
   * @throws NullPointerException
   *           if <code>opt</code> does not denote a defined option
   */
  public boolean available(String opt) {
    return ((Option)options.get(opt)).available();
  }
  /**********************************************************************/
  /**
   * <p>parses the given command line. Results are stored internally
   * and can be retrieved with {@link #getValues getValues()}. If an
   * error occurs during parsing, the message of the resulting {@link
   * CommandlineException} makes a good usage message. A typical
   * application looks like<pre>
   *   try {
   *     cmd.parse(argv);
   *   } catch( CommandlineException e ) {
   *     System.err.println(e.getMessage());
   *     System.exit(1);
   *   }</pre></p>
   */

  public void parse(String[] argv) throws CommandlineException {
    Vector<String> rest = new Vector<String>();
    if( optOrder.size()>0 && "--".equals(optOrder.get(0)) ) {
      optOrder.remove(0);
      optOrder.add("--");
    }
      
    for(int i=0; i<argv.length; /**/) {
      if( argv[i].equals("--") ) {
	i += 1;
	while( i<argv.length ) rest.add(argv[i++]);
	break;
      }
      if( argv[i].length()==0 || argv[i].charAt(0)!='-' ) {
	rest.add(argv[i]);
	i += 1;
	continue;
      }
      if( !options.containsKey(argv[i]) ) {
	throw new CommandlineException
	  (ProgramName+": unknown option `"+argv[i]+"'\n\n"+usage());
      }
      i = options.get(argv[i]).parse(argv, i+1);
    }
    
    if( options.containsKey("--") ) {
      Option orest = options.get("--");
      int i = orest.parse(rest.toArray(new String[rest.size()]), 0);
      if( i<rest.size() ) {
        throw new 
        CommandlineException("to many `"+orest.name
                             +"' arguments, found "
                             +rest.size()+" but want no more than "+orest.cmax);
        
      }
    } else if( rest.size()>0 ) {
      throw new CommandlineException
	(ProgramName+": non-option arguments not allowed\n\n"+usage());
    }      

    // check if we have all options required
    int L = optOrder.size();
    StringBuffer err = new StringBuffer();
    for(int i=0; i<L; i++) {
      Option o = (Option)(options.get(optOrder.get(i)));
      if( o.isRequired() && !o.available() ) {
	if( err.length()>0 ) err.append(", ");
	err.append("`").append(optOrder.get(i)).append("'");
      }
    }
    if( err.length()>0 ) {
      err.insert(0, ProgramName+": the required option(s) ");
      err.append(" are missing");
      throw new CommandlineException(err.toString());
    }
  }
  /**********************************************************************/
  public void showParsed() {
    Iterator it = options.keySet().iterator();
    while( it.hasNext() ) {
      Object key = it.next();
      System.out.print(key+":");
      Option opt = (Option)(options.get(key));
      Vector v = opt.getValues();
      if( v==null ) {
	System.out.println(" (null)");
      } else {
	for(int i=0; i<v.size(); i++) {
	  System.out.print(" `"+v.get(i)+"'");
	}
	System.out.println();
      }
    }	
  }
  /**********************************************************************/

}
