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

package monq.programs;

import monq.jfa.*;
import monq.jfa.actions.*;
import monq.clifj.*;
import monq.net.*;

import java.io.*;

/**
 * <p>is a class and a command line program to copy input to output
 * depending on matching regular expressions. The following
 * paragraphs describe how <code>Grep</code> works. You need to read
 * it in order to understand the documentation of the constructor as
 * well as the output generated when passing the option
 * <code>"-h"</code> to the command line program.</p>
 *
 * <p><code>Grep</code> accepts an arbitrary number of pairs
 * <em>(re,f)</em> on the command line where</p>
 *
 * <dl>
 * <dt><em>re</em></dt><dd>is a 
 * <a href="../jfa/doc-files/resyntax.html">regular expression</a>
 * and</dd>
 * <dt><em>f</em></dt><dd>is a format as defined for {@link
 * monq.jfa.PrintfFormatter}</dd>
 * </dl>
 *
 * <p>In normal operation, whenever one of the regular expressions
 * matches, the respective format is used to print the
 * match. Non-matching text is deleted, except if option
 * <code>"-cr"</code> (parameter <code>copy</code>) is given.</p>
 *
 * <p>Example:</p>
 * <blockquote>
 *   <tt>java monq.programs.Grep 'insulin *[^ ]+' "%0(8,0)\n"</tt>
 * </blockquote>
 * <p>is a rough way to find all the words which appear just after
 * <em>insulin</em> in a text.</p>
 *
 * <p>In more interesting applications, option <code>"-r"</code> (or
 * the many-argument constructor) is
 * used with two regular expressions, <code>roiOn</code> and
 * <code>roiOff</code>, to specify a <em>Region Of Interest</em>
 * (ROI). <code>Grep</code> will then apply its regular expressions
 * only in those ranges of input text which lie between pairs of
 * matches of <code>roiOn</code> and <code>roiOff</code>. In particular
 * <code>Grep</code> starts pattern matching just after a match of
 * <code>roiOn</code> and stops as soon as a match of <code>roiOff</code> is
 * found. The generated generated output is as follows:</p>
 *
 * <ol>
 * <li>Text outside any ROI is deleted (change with
 * <code>"-co"</code>, or parameter <code>copyEnv</code>).</li>
 * <li>ROIs which do not contain a match are deleted (change with
 * <code>"-cr"</code> or parameter <code>select</code>).</li>
 * <li>Text matching <code>roiOn</code> and <code>roiOff</code> is copied
 * (change with <code>"-rf"</code> or parameters <code>rfOn</code> and
 * <code>rfOff</code>).</li>
 * <li>Non-matching text within ROIs that contain at least one match
 * is copied (change with <code>"-d"</code> or parameter
 * <code>fmRoi</code>).</li>
 * <li>Matching text is handled according to the respective format
 * <em>f</em> as described above.
 * </ol>
 *
 * <h2>Examples</h2><pre>
 *   java programs.Grep \
 *       -r '&lt;MedlineCitation' '&lt;/MedlineCitation&gt;' \
 *       -rf "%0" "%0\n\n" \
 *       insulin "[[[%0]]]"</pre>
 * <p>will fetch from a Medline file all those entries which contain
 * the word <em>insulin</em> somewhere. The output of each ROI will be
 * followed by a pair of newlines. In addition, the word
 * <em>insulin</em> will be triple bracketed in the output.</p>
 * 
 * <pre>
 *   java programs.Grep \
 *     -cr \
 *     -r '&lt;protein&gt;' '&lt;/protein&gt;'</pre>
 * <p>extracts all <code>&lt;protein&gt;</code> elements from an XML
 * file without testing them for any regular expression.</p>
 *
 * <pre>
 *   java programs.Grep \
 *   -cr \
 *   -r '&lt;protein&gt;' '&lt;/protein&gt;' \
 *   -rf '%0' '%0\n' 
 *   '[\r\n\t ]+' ' '</pre>
 *
 * <p>works almost as above. In addition whitespace, which includes
 * newlines, is normalized to one space character. As a result, each
 * whole <code>&lt;protein&gt;</code> element will be on a line on its
 * own. You may then process the output with classic <code>grep</code>
 * again if you feel more comfortable with it.</p>
 *
 * <h2>Application Notes</h2>
 * <p>Regular expressions (REs) are not trivial. In particular when working
 * with several REs competing in parallel for matches, things easily
 * start to be confusing. The notes below might help to sort some
 * things out.</p>
 *
 * <h4>Lines have no significance</h4>
 * <p><code>Grep</code> is not line oriented like the classic
 * <code>grep</code>. It does not care for line separators, except if
 * you specify them explicitly somehow in your regular
 * expressions.</p>
 *
 * <h4>Never use <code>'.*'</code> or <code>'.+'</code>.</h4>
 * <p><code>Grep</code> lumps together all given REs <b>plus</b> the
 * regular expression denoting the end of the ROI into one big RE and
 * matches everything in parallel. The longest match wins. Guess which
 * RE wins if <code>'&lt;tag&gt;'</code> competes with
 * <code>'.+'</code>? The latter always matches all the way to the
 * very end of input and wins. Consequently the
 * <code>'&lt;tag&gt;'</code> will never win and therefore the ROI
 * will never end.</p>
 *
 * <p>Instead of <code>'.+'</code> always try to use a restricted
 * character set. For XML elements which can not have child elements,
 * <code>'[^&lt;]+'</code> is your best bet.</p>
 *
 * <p>There is, however, one admissible use of <code>'.*'</code>. When
 * you use it together with a shortest match operator like in 
 * <code>'(.*&lt;/endtag&gt;)!'</code>, you are guaranteed that the
 * match will extend exactly to the first match of
 * <code>'&lt;/endtag&gt;'</code>.</p>
 *
 * <h4>Use of <code>'%1'</code>, <code>'%2'</code>, etc. in
 * formats</h4>
 * <p>Apart from <code>'%0'</code>, position parameters
 * <code>'%1'</code>, etc. can be 
 * used in a {@link monq.jfa.PrintfFormatter format string} under
 * certain circumstances. In contrast to many other regular expression
 * packages, the package employed in <code>Grep</code> does not work with
 * so called <em>capturing parentheses</em><a 
 * href="#fn1" class="footnotemark">1</a> by
 * default. Instead, a pair of parentheses is made into reporting
 * parentheses by using an exclamation mark as the first character
 * after the opening parenthesis, like in <code>"(![a-z]+)"</code>.</p>
 *
 * <p>Example:</p><pre>
 *   Grep '&lt;entry +[a-zA-Z]+="(![^"]+)"' "(%1)\n"</pre>
 * will fetch the first attribute value from an XML element called
 * <code>'&lt;entry&gt;'</code>.
 *
 * <hr />
 * <h4>Footnotes</h4>
 * <p class="footnote"><a name="fn1">1)</a> This has to do with the
 * fact that 
 * <code>Grep</code> works with deterministic finite automata which
 * normally completely preclude the use of capturing parentheses. The
 * details why this is so are too complicated to explain here. Details
 * can be found 
 * <a href="../monq/jfa/doc-files/resyntax.html#rse">here</a>.</p>
 *
 * <hr />
 * @author &copy; 2004,2005 Harald Kirsch
 */
public class Grep implements ServiceFactory {

  private Dfa main;

  // in some situations, callbacks must communicate and store
  // intermediate state
  private boolean needCom = false;
  /**********************************************************************/
  /**
   * used to store internal state for callbacks. Objects of this class
   * are passed around via DfaRun.clientdata.
   */
  private static class Com {
    // where did we start holding back data
    public int holdStart;

    // set to true only if the held back data was veryfied for
    // shipping 
    public boolean ship;     
  }
  /**********************************************************************/
  /**
   * <p>create a <code>Grep</code> without ROI.</p>
   *
   * @param copy if <code>true</code>, non matching text is copied. 
   * @param autoPrio if <code>true</code>, all regular expressions in
   * <code>args</code> are auto-prioritized to suppress any
   * <code>CompileDfaException</code> due to competing regular
   * expressions.
   * @param args pairs of regular expression and {@link Printf}
   * formats. 
   *
   * @throws ReSyntaxException if either a regular expression or a
   * format string has a syntax error
   * @throws CompileDfaException if any one of the regular expressions
   * matches the empty string or if <code>autoPrio==false</code> and
   * there are competing regular expressions.
   */
  public Grep(boolean copy, boolean autoPrio, String[] args) 
    throws ReSyntaxException, CompileDfaException {

    int prio = 0;
    int prioInc = autoPrio ? 1 : 0;

    Nfa nfa = new Nfa(Nfa.NOTHING);
    for(int i=0; i<args.length; i+=2) {
      nfa.or(args[i], new Printf(true, args[i+1]).setPriority(prio));
      prio += prioInc;
    }
    main = nfa.compile(copy ? 
		       DfaRun.UNMATCHED_COPY : DfaRun.UNMATCHED_DROP);
  }
  /**********************************************************************/
  /**
   * <p>create a <code>Grep</code> with ROI. With a ROI, the input text
   * is separated into 4 different parts:</p>
   * <ol>
   * <li>outside any ROI,</li>
   * <li>ROI borders,</li>
   * <li>non matching text within ROI,</li>
   * <li>matching text within ROI.</li>
   * </ol>
   *
   * <p>In addition a difference is made between ROIs containing a
   * match and those which don't. The parameters allow detailed
   * control over how to handle all the details.</p>
   *
   * @param roiOn regular expression starting ROI
   * @param roiOff regular expression ending ROI
   * @param rfOn format with which to print start of ROI,
   * <code>null</code> means use {@link monq.jfa.actions.Copy#COPY}
   * @param rfOff format with which to print end of ROI
   * <code>null</code> means use  {@link monq.jfa.actions.Copy#COPY}
   * @param fmRoi how to handle non matching input within ROI
   * @param select <code>true</code> will copy only ROIs with a match
   * @param copyEnv <code>true</code> will copy anything not in ROI
   * @param autoPrio if <code>true</code>, all regular expressions in
   * <code>args</code> are auto-prioritized to suppress any
   * <code>CompileDfaException</code> due to competing regular
   * expressions.
   * @param args pairs of regular expression and {@link Printf}
   * formats. 
   *
   * @throws ReSyntaxException if either a regular expression or a
   * format string has a syntax error
   * @throws CompileDfaException if any one of the regular expressions
   * matches the empty string or if <code>autoPrio==false</code> and
   * there are competing regular expressions.
   */
  public Grep(String roiOn, String roiOff,
	      String rfOn, String rfOff,
	      DfaRun.FailedMatchBehaviour fmRoi,
	      boolean select,
	      boolean copyEnv,
	      boolean autoPrio, 
	      String[] args) throws ReSyntaxException, CompileDfaException {

    // set up actions which define how to print the ROI separators
    FaAction toWorkAction, toEnvAction;
    if( rfOn==null) toWorkAction = Copy.COPY;
    else toWorkAction =  new Printf(true, rfOn);
    if( rfOff==null ) toEnvAction = Copy.COPY;
    else toEnvAction = new Printf(true, rfOff);
    
    // When selectively copying only those ROIs with a match, we need
    // a combination of Hold/Match/Decide callbacks to do the
    // work. Otherwise, a simple Printf suffices.
    Nfa nfa = new Nfa(Nfa.NOTHING);
    if( select ) {
      this.needCom = true;
      toWorkAction = new Hold(toWorkAction);
      toEnvAction = new Decide(toEnvAction);
      for(int i=0; i<args.length; i+=2) {
	nfa.or(args[i], new Match(args[i+1]).setPriority(autoPrio ? i : 0));
      }      
    } else {
      for(int i=0; i<args.length; i+=2) {
	Printf pf = new Printf(true, args[i+1]);
	if( autoPrio ) pf.setPriority(i);
	nfa.or(args[i], pf);
      }      
    }
    
    // add the rule to switch back to the envelope to the Nfa and
    // compile it
    DfaRun.FailedMatchBehaviour fmMain 
      = copyEnv ? DfaRun.UNMATCHED_COPY : DfaRun.UNMATCHED_DROP;
    SwitchDfa toEnv = new SwitchDfa(toEnvAction);
    toEnv.setPriority(Integer.MAX_VALUE);
    nfa.or(roiOff, toEnv);
    Dfa work = nfa.compile(fmRoi);

    // set up the envelope Nfa, we need just one action which switches
    // into the ROI
    SwitchDfa toWork = new SwitchDfa(toWorkAction);
    Nfa envelope = new Nfa(roiOn, toWork);
    main = envelope.compile(fmMain);

    // connect the SwitchDfa objects to their DFAs
    toEnv.setDfa(main);
    toWork.setDfa(work);

  }
  /**********************************************************************/
  /**
   * <p>is the way to apply the machinery created with one of the
   * constructors. Simply use one of the <code>filter()</code> methods
   * supplied by a <code>DfaRun</code>.</p>
   */
  public DfaRun createRun() {
    DfaRun r = new DfaRun(main);
    if( needCom ) r.clientData = new Com();
    return r;
  }
  /**********************************************************************/
  public Service createService(java.io.InputStream in,
			       java.io.OutputStream out,
			       Object param) {
    DfaRun r = createRun();
    r.setIn(new ReaderCharSource(in));
    return new DfaRunService(r, new PrintStream(out));
  }
  /**********************************************************************/
  // Action to run when entering the ROI. It marks the start of the
  // roi, switches to collect mode and has a boolean which will
  // finally decide whether to ship the ROI
  private static class Hold extends AbstractFaAction {
    private FaAction a;
    public Hold(FaAction a) { this.a = a; }
    public void invoke(StringBuffer yytext, int start, DfaRun r) 
      throws CallbackException {
      Com com = (Com)r.clientData;
      com.ship = false;
      com.holdStart = start;
      r.collect = true;
      a.invoke(yytext, start, r);
    }
  }
  /**********************************************************************/
  // When using a ROI to be shipped only on matches, the Printf
  // actions are wrapped into one of those
  private static class Match extends AbstractFaAction {
    private FaAction a;
    public Match(String format) throws ReSyntaxException {
      this.a = new Printf(true, format);
    }
    public void invoke(StringBuffer yytext, int start, DfaRun r) 
      throws CallbackException {
      Com com = (Com)r.clientData;
      com.ship = true;
      a.invoke(yytext, start, r);
    }
  }
  /**********************************************************************/
  // Action to be run when leaving the ROI. It calls its Hold client
  // to make the decision whether to ship or drop the ROI.
  private static class Decide extends AbstractFaAction {
    private FaAction a;
    public Decide(FaAction a) { this.a = a; }
    public void invoke(StringBuffer yytext, int start, DfaRun r) 
      throws CallbackException {
      Com com = (Com)r.clientData;
      a.invoke(yytext, start, r);
      if( !com.ship ) yytext.setLength(com.holdStart);
      r.collect = false;
    }
  }
  /**********************************************************************/
  // Do what normally the compiler does with '\r' etc.
  private static String escapeLiteral(StringBuffer scratch, String s) {
    scratch.setLength(0);
    int l = s.length();
    for(int i=0; i<l; i++) {
      char ch = s.charAt(i);
      if( ch!='\\' ) {
	scratch.append(ch);
	continue;
      }
      if( i+1==l ) break;
      i += 1;
      ch = s.charAt(i);
      if( ch=='b' ) scratch.append('\b');
      else if( ch=='t' ) scratch.append('\t');
      else if( ch=='n' ) scratch.append('\n');
      else if( ch=='f' ) scratch.append('\f');
      else if( ch=='r' ) scratch.append('\r');
      else scratch.append(ch);
    }
    return scratch.substring(0);
  }
  /**********************************************************************/
  /**
   * call with command line option <code>"-h"</code> for a short
   * summary of operation.
   */
  public static void main(String[] argv) throws Exception {
    String prog = System.getProperty("argv0", "Grep");
    Commandline cmd = new Commandline
      (prog,
       "match regular expressions and reformat them to output",
       "re format",
       "pairs of regular expression and PrintfFormatter formats",
       0, Integer.MAX_VALUE);
    cmd.addOption
      (new BooleanOption("-ap",
			 "auto-prioritize regular expressions with the "
			 +"last one having highest priority. Without this "
			 +"option you may get a CompileDfaException for "
			 +"competing regular expressions."));

    cmd.addOption
      (new Option("-r",
		  "roi",
		  "Region Of Interest defined by 2 regular expressions. "
		  +"Matches are only searched for in the roi. If no "
		  +"roi is "
		  +"given, the whole input is searched",
		  2, 2, null));
    cmd.addOption
      (new BooleanOption("-d", 
			 "delete non-matching text within roi. "
			 +"By default, non-matching text within a roi "
			 +"which contains a match is copied."));

    cmd.addOption
      (new BooleanOption("-cr", 
			 "copy every region of interest, even if they "
			 +"do not contain a match. Without roi, all "
			 +"non-matching text is copied."));

    cmd.addOption
      (new BooleanOption("-co", "copy text outside of roi."));
    cmd.addOption(new Option
		  ("-rf",
		   "roiFormat",
		   "a pair of PrintfFormatter formats which describe "
		   +"what to do with the matches defining the roi. By "
		   +"default, they are copied to the output with "
		   +"the matching roi",
		   2, 2, null));
    cmd.addOption(new LongOption
		  ("-p", "port",
		   "instead of filtering stdin to stdout, go into server "
		   +"mood and listen on the given port. The protocol used "
		   +"is implemented by class monq.jfa.DistPipeFilter. The "
		   +"program DistFilter can be used on the "
		   +"command line to make a contact",
		   1, 1, 0, 65535, null));

    try {
      cmd.parse(argv);
    } catch( CommandlineException e ) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    String[] args = cmd.getStringValues("--");
    if( args.length%2!=0 ) {
      System.err.println(prog+": must have an even number of arguments");
      System.exit(1);
    }
    
    StringBuffer scratch = new StringBuffer();
    int l = args.length;
    for(int i=0; i<l; i+=1) args[i] = escapeLiteral(scratch, args[i]);

    // set up the object to do the work
    Grep grep;

    if( cmd.available("-r") ) {
      String[] roi = cmd.getStringValues("-r");
      roi[0] = escapeLiteral(scratch, roi[0]);
      roi[1] = escapeLiteral(scratch, roi[1]);
      String[] rf = {null, null};
      if( cmd.available("-rf") ) {
	rf = cmd.getStringValues("-rf");
	rf[0] = escapeLiteral(scratch, rf[0]);
	rf[1] = escapeLiteral(scratch, rf[1]);
      }
      
      // actions needed when switching between envelope and work
      // set up whether all ROIs are to be copied or only those with a
      // match. It is only slightly nonsensical to combine -cr and -d
      // because within ROIs without match, the -d will delete
      // everything. Only the ROIs boundaries might show up, depending
      // on -rf.
      boolean select = true;
      if( cmd.available("-cr") ) {
	select = false;
      }
      DfaRun.FailedMatchBehaviour fmRoi = DfaRun.UNMATCHED_COPY;
      if( cmd.available("-d") ) fmRoi = DfaRun.UNMATCHED_DROP;

      grep = new Grep(roi[0], roi[1], rf[0], rf[1],
		      fmRoi, 
		      select, 
		      cmd.available("-co"),
		      cmd.available("-ap"), 
		      args);
    } else {
      String[] forbidden = {"-co", "-d", "-rf"};
      boolean exit = false;
      for(int i=0; i<forbidden.length; i++) {
	if( cmd.available(forbidden[i]) ) {
	  System.err.println(prog+": option "+forbidden[i]
			     +" nonsensical without -r");
	  exit = true;
	}
      }
      if( args.length==0 ) {
	System.err.println(prog+": without -r there must be at least one "
			   +"'re format' pair");
	exit = true;
      }
      if( exit ) System.exit(1);

      grep = new Grep(cmd.available("-cr"),
		      cmd.available("-ap"), 
		      args);
    }


    if( cmd.available("-p") ) {
      FilterServiceFactory fsf = new FilterServiceFactory(grep);
      int port = (int)cmd.getLongValue("-p");
      new TcpServer(port, fsf, 20).setLogging(System.out).serve();
    } else {
      DfaRun r = grep.createRun();
      r.setIn(new ReaderCharSource(System.in));
      try {
	r.filter(System.out);
      } catch( java.io.IOException e ) {
	String msg = e.getMessage();
	if( msg.startsWith("Broken pipe") 
	    || msg.startsWith("EOF hit in") ) {
	  System.exit(1);
	}
	throw e;
      }
    }
  }
}
