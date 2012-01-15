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

import java.util.Map;

/**********************************************************************/
/**
 * <p>implements a {@link Formatter} which uses a format string to
 * print parts from a {@link TextStore} into a
 * <code>StringBuffer</code>. An additional
 * <code>java.util.Map</code> may supply additional material to
 * populate the output string.</p>
 *
 * <p>The format string contains ordinary text and control codes
 * introduced by the percent (<code>%</code>) character. The control
 * codes refer to either pieces of text from <code>TextStore</code> or
 * to objects of the internal <code>Map</code> as follows:</p>
 *
 * <ul>
 * <li><p><code>%</code><em>i</em>,
 * where <em>i</em> is an integer,
 * retrieves the <em>i</em>'th part of the <code>TextStore</code> and
 * writes it to the output string. As described <a
 * href="TextStore.html#indexing">elsewhere</a>, negative values
 * access parts relative to the last part. For example
 * <code>"%-1"</code> requests the last part.</p></li>
 *
 * <li>
 * <p><code>%</code><em>i</em><code>(</code><em>s</em>,<em>e</em><code>)</code>
 * is an extension to the the above. It refers to a substring of part
 * <em>i</em>, where <em>s</em> is the start index and <em>s</em> is
 * the end index. As usual, the end index is the index just after the
 * last character taken. If either index is negative, the value denotes the
 * position resulting from adding the value to the length of the
 * part.</p>
 * <p>For example <code>%3(1,-1)</code> denotes part three of the
 * <code>TextStore</code> with first and last character stripped
 * off. With <code>%2(-3,0)</code> the last three characters of part
 * two are returned. As you can see, <em>e</em>=0 denotes the end of
 * the string, but <em>s</em>=0 denotes the start.</p></li>
 *
 * <li><p><code>%(</code><em>i</em><code>,</code><em>j</em><code>)</code>
 * requests all parts <em>k</em> with 
 * <em>i</em>&le;<em>k</em>&lt;<em>j</em>. Like above, <em>i</em> and
 * <em>j</em> may be negative to denote an index relative to the end
 * of the list.</p></li>
 *
 * <li></p>
 * <code>%(</code><em>i</em><code>,</code><em>j</em><font color="green">,<em>sep</em></font><code>)</code><font color="green">(</code><em>s</em>,<em>e</em><code>)</code></font>
 * is a combination of the two above mentioned formats. The green
 * parts are optional. The format requests a
 * range of parts (<em>i</em> to <em>j</em>) while at the same time
 * extracting only the substring denoted by <em>s</em> and <em>e</em>
 * from each part. In addition, when printing the substrings to the
 * output, they will be separated by <em>sep</em>.</p>
 *
 * If <em>sep</em> shall contain a closing parentheses, it must be
 * escaped with a backslash. All other backslash characters are kept
 * as such. Note that in a constant string you write in source code,
 * the backslash must be doubled, because the compiler will take one
 * away.</p>
 *
 * <p>Example: <code>"&lt;%(-2,0,/&gt; &lt;)(0,-1)/&gt;"</code> applied to
 * a <code>TextStore</code> which contains "rock", "zock" and "nock"
 * will print "&lt;zoc/&gt; &lt;noc/&gt;".
 *</p></li>
 *
 * <li><p><code>%(</code><em>key</em><code>)</code> retrieves the value
 * stored for the given <em>key</em> from the
 * <code>java.lang.Map</code> passed into {@link #format format()}. If
 * the <code>Map</code> given is <code>null</code>, a
 * <code>NullPointerException</code> will be thrown. If, however, the
 * <em>key</em> does not exist in the map, the empty string is
 * silently inserted.</p></li>
 *
 * <p>Example: <code>"PMID: %(pmid)"</code> will result in the text
 * <code>"PMID: 1234"</code> provided the value <code>1234</code> was
 * stored before in this object's <code>Map</code> for the key
 * <code>"pmid"</code>.</p>
 *
 * <li><code>%l</code><em>i</em> is replaced by the length of
 * the <em>i</em>ith part of the <code>TextStore</code>. Again,
 * <em>i</em> may be negative.</li>
 *
 * <li><code>%n</code> is replaced by the number of parts
 * extracted by the <code>TextStore</code>.  </li>
 *
 * <li><code>%%</code> denotes a percent sign on its own.</li>
 * <li><code>\</code><em>c</em> denotes the character <em>c</em>.</li>
 * </ul>
 *
 * <p>Note that nowwhere in the above formats separating blank space is allowed.</p>
 *
 * @author &copy; 2003,2004 Harald Kirsch
 */
public class PrintfFormatter implements Formatter {
  // a dfa which is used to translate a printf-like format into an
  // internal representation. Every constructor-call makes its own
  // DfaRun from it. The formatScanner itself is initialized below in
  // an elaborate static{} block
  private static Dfa formatScanner;

  // a "compiled" form of a format string
  private Formatter[] ops = new Formatter[4];
  int numOps = 0;

  /**********************************************************************/
  void addtoFormat(Formatter op) {
    // if op is a FixedString and the last op in the list is also a
    // FixedString, we join them
    if( numOps>0 
	&& op instanceof SimpleFormatters.FixedString 
	&& ops[numOps-1] instanceof SimpleFormatters.FixedString ) {
      SimpleFormatters.FixedString previousOp = 
	(SimpleFormatters.FixedString)ops[numOps-1];
      previousOp.append((SimpleFormatters.FixedString)op);
      return;
    }
    
    if( numOps>=ops.length ) {
      Formatter[] tmp = new Formatter[numOps+4];
      System.arraycopy(ops, 0, tmp, 0, numOps);
      ops = tmp;
    }
    ops[numOps++] = op;
  }
  /********************************************************************/
  static int chewInt(StringBuffer yytext, int start) {
    int i=start;
    int L = yytext.length();
    if( i<L && '-'==yytext.charAt(i) ) i+=1;
    while( i<L && Character.isDigit(yytext.charAt(i)) ) i += 1;
    int result = Integer.parseInt(yytext.substring(start, i));
    yytext.delete(start, i);
    return result;
  }
  private static FaAction createOpSpecial = new AbstractFaAction() {
      public void invoke(StringBuffer out, int start, DfaRun runner) {
	// add a fixed string to be found starting at index 1 of the
	// match
	out.delete(0, 1);
	PrintfFormatter f = (PrintfFormatter)runner.clientData;
	f.addtoFormat(new SimpleFormatters.FixedString(out));
	out.setLength(0);
      }
    };

  private static FaAction createOpString = new AbstractFaAction() {
      public void invoke(StringBuffer out, int start, DfaRun runner) {
	PrintfFormatter f = (PrintfFormatter)runner.clientData;
	f.addtoFormat(new SimpleFormatters.FixedString(out));
	out.setLength(0);
      }
    };

  private static FaAction createOpNumParts = new AbstractFaAction() {
      public void invoke(StringBuffer out, int start, DfaRun runner) {
	((PrintfFormatter)runner.clientData)
	  .addtoFormat(SimpleFormatters.NumParts);
	out.setLength(0);
      }
    };

  private static FaAction createOpPartLen = new AbstractFaAction() {
      public void invoke(StringBuffer out, int start, DfaRun runner) {
	int partNo = Integer.parseInt(out.substring(2));
	PrintfFormatter f = (PrintfFormatter)runner.clientData;
	f.addtoFormat(new SimpleFormatters.PartLen(partNo));
	out.setLength(0);
      }	
    };
  private static FaAction createOpPart = new AbstractFaAction() {
      public void invoke(StringBuffer out, int start, DfaRun runner) {
	// we are looking at something like "%-2(5,-3)" were the
	// parentheses part is optional
	int partNo = chewInt(out, start+1);
	int from=0, to=0;
	if( out.length()>=2 ) {
	  // since chewInt deletes the int we should have now "%(5,-3)"
	  from = chewInt(out, start+2);
	  // now should be "%(,-3)"
	  to = chewInt(out, start+3);
	}
	PrintfFormatter f = (PrintfFormatter)runner.clientData;
	f.addtoFormat(new SimpleFormatters.Part(partNo, from, to));
	out.setLength(0);
      }
    };

  private static FaAction createOpPartSeq = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun runner) {
	// in the full blown case we are looking at somthing like
	// "%(3,-5, separator with '\)')(1,-2)"
	// The 'separator' part as well as the trailing parentheses
	// are optional
	int from = chewInt(yytext, start+2);
	int to = chewInt(yytext, start+3);

	// after deletion of the first two ints we have
	// "%(,, separator with '\)')(1,-2)
	String sep = "";

	// let current point either on ')' or on ','
	int current = start+3;

	if( current<yytext.length() && yytext.charAt(current)==',' ) {
	  current = yytext.indexOf(")", current+1);
	  while( yytext.charAt(current-1)=='\\' ) {
	    yytext.deleteCharAt(current-1);
	    current = yytext.indexOf(")", current);
	  }
	  sep = yytext.substring(start+4, current);
	} 

	// put current just after ')'
	current += 1;

	// with or without "separator", current now points to the
	// closing parenthesis
	int partFrom = 0;
	int partTo = 0;

	if( current<yytext.length() ) {
	  partFrom = chewInt(yytext, current+1);
	  partTo = chewInt(yytext, current+2);
	}

	PrintfFormatter f = (PrintfFormatter)runner.clientData;
	f.addtoFormat(new SimpleFormatters.PartSeq(from, to, sep,
						   partFrom, partTo));
	yytext.setLength(0);
      }
    };

  private static FaAction createOpGetVar = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun runner) {
	String key = yytext.substring(2, yytext.length()-1);
	PrintfFormatter f = (PrintfFormatter)runner.clientData;
	f.addtoFormat(new SimpleFormatters.GetVar(key));
	yytext.setLength(0);
      }
    };

  static {
    // create the automaton. Since we do the right thing, neither
    // ReSyntaxException nor CompileDfaException should happen, so we
    // catch them.
    String range = "-?[0-9]+,-?[0-9]+";
    String optRange = "([(]"+range+"[)])?";
    String optSep = "(,([^)]|[\\\\][)])+)?";
    try {
      formatScanner = 
	new Nfa("%-?[0-9]+"+optRange, createOpPart)
	.or("%[(]"+range+optSep+"[)]"+optRange, createOpPartSeq)
	.or("%n", createOpNumParts) // request number of parts
	.or("%l-?[0-9]+", createOpPartLen) // length of specified part
	.or("%%", createOpSpecial) // a simple %
	.or("%[(][A-Za-z_<>][A-Za-z0-9_<>]*[)]", createOpGetVar)
	.or("[^%\\\\]+",createOpString)	// copy as is
	.or("\\\\.", createOpSpecial) // an escaped character
	.compile(DfaRun.UNMATCHED_THROW)
	;
    } catch( ReSyntaxException e ) {
      ///CLOVER:OFF      
      throw new Error("this cannot happen", e);
    } catch( CompileDfaException e) {
      throw new Error("this cannot happen", e);
      ///CLOVER:ON
    }
    
  }
  /********************************************************************/
  //  /**
//    * @deprecated Objects of this class typically are used by {@link
//    * monq.jfa.actions.Printf} and as such end up in a {@link
//    * Dfa}. Because a {@link Dfa} shall be shareable between threads,
//    * callbacks stored within it should not have a mutable internal
//    * state.
//    */
//   public PrintfFormatter(CharSequence format, int start, Map vars) 
//     throws ReSyntaxException
//   {
//     this(format, start, null);
//   }
//   /**
//    * @deprecated see {@link #PrintfFormatter(CharSequence,int,Map)}
//    */
//   public PrintfFormatter(CharSequence format, Map vars) 
//     throws ReSyntaxException
//   {
//     this(format, 0, vars);
//   }

  public PrintfFormatter(CharSequence format) throws ReSyntaxException
  {
    this(format, 0);
  }

  public PrintfFormatter(CharSequence format, int start) 
    throws ReSyntaxException
  {
    // of course we interprete the format string with an automaton. It
    // will be this one.
    DfaRun r;

    // translating the format with scanFormat may leave an unhandled
    // tail at the end of the format. We collect it in tail and handle it
    // below.
    r = new DfaRun(formatScanner, 
		   new CharSequenceCharSource(format, start));

    // The DfaRun will load "this" with all the operations
    r.clientData = this;

    try {
      r.filter();
    } catch( NomatchException e) {
      // there was an error in the format string. We prepare a decent
      // message. 
      StringBuffer msg = new StringBuffer(100);
      int i = 24;
      for(;;) {
	int ch = 0;
	try {
	  ch = r.skip();
	} catch( java.io.IOException ioe) {
	  ///CLOVER:OFF
	  throw new Error("impossible", ioe);
	  ///CLOVER:ON
	}
	if( ch==-1 ) break;
	msg.append((char)ch);
	i -= 1;
	if( i>0 ) continue;
	msg.setLength(msg.length()-4);
	msg.append("...");
      }
      throw new ReSyntaxException(ReSyntaxException.EFORMAT,
				  msg.toString(), 0, 1);
    } catch( java.io.IOException e ) {
      ///CLOVER:OFF 
      throw new Error("impossible", e);
      ///CLOVER:ON
    }


    // we don't need to keep the automaton, let the gc have fun with it.
    r = null;
  }
  /********************************************************************/
  public void format(StringBuffer yytext, TextStore ts, Map m) 
    throws CallbackException
  {
    for(int i=0; i<numOps; i++) {
      ops[i].format(yytext, ts, m);
      //if( numOps>2 ) System.out.println(":::"+ops[i]+"  "+i+"  >"+yytext+"<");
      //System.out.println("now >"+yytext+"<");
    }
  }
  /********************************************************************/
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.toString())
      .append('[')
      .append(numOps)
      .append(" ops]")
      ;
    return sb.toString();
  }
}
 
