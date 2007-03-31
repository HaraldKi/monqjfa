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
import monq.jfa.ctx.*;
import monq.jfa.xml.StdCharEntities;
import monq.net.*;
import monq.stuff.*;
import monq.ie.*;

import java.io.*;
import java.util.*;

/**
 * <p><b>(Command Line Program)</b> is a class to create a text markup
 * filter
 * from a long list of term/ID or regex/ID mappings.
 * A canonical use, implemented in {@link #main}, looks like:<pre>
 *   InputStream mwtFile = new FileInputStream(mwtFileName);
 *   DictFilter dict = new DictFilter(mwtFile, inputType, 
 *                                    elemName, verbose);
 *   mwtFile.close();
 *   DfaRun r = dict.createRun();
 *   r.setIn(System.in);
 *   r.filter(System.out);</pre>
 * An example input file looks like:<pre>
 * &lt;?xml version='1.0'?>
 * 
 * &lt;mwt>
 *   &lt;template>&lt;protein id="%1">%0&lt;/protein>&lt;/template>
 *   &lt;t p1="ipi4355">casein&lt;/t>
 *   &lt;t p1="X33355">p53&lt;/t>
 * 
 *   &lt;template>&lt;disease id="%1">%0&lt;/disease>&lt;/template>
 *   &lt;t p1="UMLS4711" p2="bla">alzheimer&lt;/t>
 *
 *   &lt;template>&lt;special>%0&lt;/special>&lt;/template>
 *   &lt;r>[ \r\n\t;.,:?!]&lt;/r>
 * &lt;/mwt></pre>
 * </p>
 * <h5>The <code>&lt;template&gt;</code> element</h5>
 * <p>describes the output to be produced on a
 * match. The whole content of the element is taken as-is and
 * interpreted as a format for {@link monq.jfa.PrintfFormatter}. It
 * applies to all the <code>&lt;r&gt;</code> and
 * <code>&lt;t&gt;</code> elements that follow before another
 * <code>&lt;template&gt;</code> element or before the end of the
 * file. Print format directive <code>%0</code> refers the whole
 * match, while <code>%1</code> etc. refers to the values provided as
 * attributes named <code>p1</code> etc. in the <code>&lt;r&gt;</code> and
 * <code>&lt;t&gt;</code> elements.</p>
 *
 * <h5>The <code>&lt;t&gt;</code> element</h5> <p>describes a
 * dictionary term. It will be converted to a regular expression with
 * {@link Term2Re#convert Term2Re.convert()}. Note in particular that
 * terms are matched with a one character trailing context.</p>
 *
 * <h5>The <code>&lt;r&gt;</code> element</h5>
 * <p>allows to specify an arbitrary regular expression. The regular
 * expression is taken as-is. In addition to the attributes
 * <code>p1</code>, <code>p2</code>, etc. as described above, the
 * following attributes are allowed:</p>
 * <dl>
 * <dt>tc</dt><dd>denotes the length of a <em>trailing context</em>
 * matched by this regular expression. It must be a non-negative
 * integer which is guaranteed to be smaller than the whole match. The
 * specified number of characters are then chopped off the end of the
 * match and pushed back into the input before the
 * <code>&lt;template&gt;</code> is processed. For example
 * <blockquote>
 *   <code>&lt;r tc="1"&gt;[a-z]+ &lt;/r&gt;</code>
 * </blockquote>
 * would match a string
 * of lowercase characters only if followed by a blank, but only the
 * lowercase characters are considered part of the match. Should a match be
 * shorter than or equal to the length given, nothing at all is pushed
 * back into 
 * the input.</dd>
 * </dl>
 *
 * <h3>Encodings used</h3>
 * <p>The input encoding is guessed from the input file
 * with {@link monq.stuff.EncodingDetector#detect
 * EncodingDetector.detect()}. This can be 
 * changed with {@link #setInputEncoding setInputEncoding()}. The
 * output encoding is defaults to the platforms default encoding but
 * can be changed with {@link #setOutputEncoding setOutputEncoding()}.</p>
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class DictFilter implements ServiceFactory {

  // the dictionary Dfa. It is set up in a way that it can be reused. 
  private Dfa dictDfa;

  // If the inputEncoding is null, it will
  // be guessed with monq.stuff.EncodingDetector
  private String inputEncoding = null;
  private String outputEncoding = EncodingDetector.defaultEnc;

  /**********************************************************************/
  /**
   * <p>force {@link #createService createService()} to set up the
   * filter with the given input encoding.</p>
   */
  public void setInputEncoding(String enc) 
    throws UnsupportedEncodingException
  {
    this.inputEncoding = enc;
  }
  /**********************************************************************/
  public void setOutputEncoding(String enc) 
    throws UnsupportedEncodingException
  {
    this.outputEncoding = enc;
  }
  /**********************************************************************/
  // used while parsing the mwt file.
  private static final class ReadHelper implements ContextStackProvider {
    private List stack = new ArrayList();

    // used during setup of the mwt filter, records most recently seen
    // <template> 
    private PrintfFormatter recentTemplate;

    // the dictionary Nfa filled up while reading the mwt file
    private Nfa dict;

    // print generated regexps to stderr, one per line if this is true
    private boolean verbose = false;

    // callbacks for dictionary terms receive increasing priority in
    // order to not having to deal with ambiguities.
    private int nextPrio = 1;
    
    // a little helper to be reused while reading in the mwt file
    monq.jfa.xml.StdCharEntities helper 
      = new monq.jfa.xml.StdCharEntities();
    
    public List getStack() { return stack; }

    public ReadHelper(boolean verbose) { this.verbose = verbose; }
  }
  /********************************************************************/
  /**
   * <p>after calling {@link monq.stuff.EncodingDetector#detect}, the
   * other constructor is called with a reader prepared with the
   * detected encoding from <code>mwtFile</code>.</p>
   *
   * @deprecated Use a proper {@link java.io.Reader} with the other
   * constructor, please.
   */
  public DictFilter(java.io.InputStream mwtFile, String inputType, 
		    String elemName, boolean verbose) 
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    String enc = monq.stuff.EncodingDetector.detect(mwtFile);
    Reader in = new InputStreamReader(mwtFile, enc);
    init(in, inputType, elemName, verbose, false, true);
  }
  /********************************************************************/
  public DictFilter(Reader mwtFile, String inputType, 
		    String elemName, boolean verbose) 
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    init(mwtFile, inputType, elemName, verbose, false, true);
  }
  /**********************************************************************/
  /**
   * creates a <code>DictFilter</code> from the given
   * <code>Reader</code> which must comply to the format
   * described above. The <code>inputType</code> is one of the strings
   * <code>"raw"</code>, <code>"xml"</code> or <code>"elem"</code>
   * with the following meaning:</p>
   * <dl>
   * <dt><code>"raw"</code></dt><dd>to apply the dictionary everywhere
   * in the input string,</dd>
   * <dt><code>"xml"</code></dt><dd>to recognize and skip XML tags,
   * or</dd> 
   * <dt><code>"elem"</code></dt><dd>to apply the dictionary only in
   * within the XML element given by <code>elemName</code>.</dd>
   * </dl>
   *
   * @param mwtFile dictionary file as described above in the class
   * description 
   * @param inputType one of the strings <code>"raw"</code>,
   * <code>"xml"</code> or <code>"elem"</code> 
   * @param elemName is the XML element to work on in case
   * <code>inputType=="elem"</code> 
   * @param verbose when true, will dump all generated regular
   * expressions to  <code>System.err</code> 
   * @param memDebug if <code>true</code>, estimated object sizes of
   * Nfa and Dfa will be dumped to <code>System.err</code> after
   * compilation of the Nfa. Normally set this to <code>false</code>.
   * @param defaultWord when true, add a catch all word to the
   * generated automaton to prevent against matching in the middle of
   * a word. This is here for historical reasons. Normally the
   * catch-all should be in the mwt file.
   */
  public DictFilter(Reader mwtFile, String inputType, 
		    String elemName, 
		    boolean verbose, boolean memDebug,
		    boolean defaultWord)
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    init(mwtFile, inputType, elemName, verbose, memDebug, defaultWord);
  }
  private void init(Reader mwtFile, String inputType, 
		    String elemName, boolean verbose, 
		    boolean memDebug, boolean defaultWord) 
    throws java.io.IOException, ReSyntaxException, CompileDfaException
  {
    ReadHelper rh = new ReadHelper(verbose);
    DfaRun r;

    // create the Dfa with which we read the input file.
    try {
      Nfa nfa = new Nfa(Nfa.NOTHING);
      ContextManager mgr = new ContextManager(nfa)
	.setDefaultAction(Drop.DROP)
	.setDefaultFMB(DfaRun.UNMATCHED_THROW)
	;
      Context mwt = mgr.addXml((Context)null, "mwt");

      nfa.or(Xml.S, Drop.DROP)
       	.or(Xml.XMLDecl, 
	    // FIX ME: should extract the encoding
	    new IfContext(null, Drop.DROP)
	    .elsedo(new Fail("XML declaration in the wrong place")))
	.or(Xml.GoofedElement("template"), 
	    new IfContext(mwt, do_template)
	    .elsedo(new Fail("`template' must be child of `mwt'")))
	.or(Xml.GoofedElement("t"), 
	    new IfContext(mwt, do_t_r)
	    .elsedo(new Fail("`t' must be child of `mwt'")))
	.or(Xml.GoofedElement("r"), 
	    new IfContext(mwt, do_t_r)
	    .elsedo(new Fail("`r' must be child of `mwt'")))	
	//.or(Xml.GoofedElement("rx", do_rx))
	.or(Xml.Comment, Drop.DROP)
	;
      FaAction eofAction = 
	new IfContext(null, Drop.DROP)
	.elsedo(new AbstractFaAction() {
	    public void invoke(StringBuffer b, int start, DfaRun r) 
	      throws CallbackException
	    {
	      ReadHelper rh = (ReadHelper)r.clientData;
	      List stack = rh.getStack();
	      Context ctx = (Context)stack.get(stack.size()-1);
	      throw new CallbackException
		("open context `"+ctx.getName()+"'");
	    }
	  });
      Dfa dfa = nfa.compile(DfaRun.UNMATCHED_THROW, eofAction);

      r = new DfaRun(dfa, new ReaderCharSource(mwtFile));

      // initialize the dictionary with a catch all word to prevent
      // matching of dictionary terms in the middle of words.
      if( defaultWord ) {
	rh.dict = new Nfa("[A-Za-z0-9]+", new Copy(Integer.MIN_VALUE));
      } else {
	rh.dict = new Nfa(Nfa.NOTHING);
      }
    } catch( ReSyntaxException e ) {
      throw new Error("this cannot happen", e);
    } catch( CompileDfaException e ) {
      throw new Error("this cannot happen", e);
    }

    // now read the dictionary file, this should not produce any output
    r.clientData = rh;
    r.filter(System.out);
    
    // set up the fa to only care for certain types of input
    Nfa nfa = rh.dict;

    if( "raw".equals(inputType) ) {
      // nothing to add to dict
      dictDfa = nfa.compile(DfaRun.UNMATCHED_COPY);

    } else if( "xml".equals(inputType) ) {
      nfa.or(Xml.STag()+"|"+Xml.ETag()+"|"+Xml.EmptyElemTag()
	     +"|"+Xml.Reference, Copy.COPY)      
	.or("<[?](.*[?]>)!", Copy.COPY)
	.or("<[!]--(.*-->)!", Copy.COPY)
	 ;
      dictDfa = nfa.compile(DfaRun.UNMATCHED_COPY);

    } else if( "elem".equals(inputType) ) {
      nfa.or(Xml.Reference, Copy.COPY);

      SwitchDfa toEnv = new SwitchDfa(Copy.COPY);
      toEnv.setPriority(Integer.MAX_VALUE);
      SwitchDfa toWork = new SwitchDfa(Copy.COPY);
      Dfa env = new Nfa(Xml.STag(elemName), toWork)
	.compile(DfaRun.UNMATCHED_COPY);
      nfa.or(Xml.ETag(elemName), toEnv);
      dictDfa = nfa.compile(DfaRun.UNMATCHED_COPY);
      toEnv.setDfa(env);
      toWork.setDfa(dictDfa);
      dictDfa = env;
    } else {
      throw new IllegalArgumentException
	("`"+inputType+"' is not a valid input type");
    }

    if( memDebug ) {
      java.util.Hashtable h;
      System.err.println("# Size of Nfa");
      h = monq.stuff.Sizeof.sizeof(nfa);
      monq.stuff.Sizeof.printTypes(System.err, h);
      
      h = monq.stuff.Sizeof.sizeof(dictDfa);
      System.err.println("# Size of Dfa");
      monq.stuff.Sizeof.printTypes(System.err, h);
      //dictDfa.toDot(new PrintStream(new FileOutputStream("dfa.dot")));
    }

    // give gc a chance on this, and convince it to give memory back
    // to the OS. Remember to run this class with -XX:MaxHeapFreeRatio.
    nfa = null;
    r = null;

    ConvinceGC c = new ConvinceGC(10);
    if( memDebug ) c.setLogging(System.err); 
    Thread t = new Thread(c);
    t.setDaemon(true);
    t.start();
  }
  /********************************************************************/
  /**
   * create a {@link monq.jfa.DfaRun} object suitable to operate the
   * dictionary DFA.
   */
  public DfaRun createRun() {
    return new DfaRun(dictDfa);
  }
  /********************************************************************/
  /**
   * returns the dictionary DFA.
   */
  public Dfa getDfa() { return dictDfa; }
  /********************************************************************/
  public Service createService(InputStream in, OutputStream out, Object p) 
    throws ServiceCreateException
  {
    DfaRun r = createRun();
    Reader rin;
    if( inputEncoding==null ) {
      if( !in.markSupported() ) in = new BufferedInputStream(in);
      String enc = null;
      try {
	enc = EncodingDetector.detect(in, 1000, "UTF-8");
	rin = new InputStreamReader(in, enc);
      } catch( UnsupportedEncodingException e ) {
	throw new ServiceUnavailException("unsupported encoding `"
					  +enc+"' found in file", e);
      } catch( IOException e ) {
	throw new ServiceUnavailException("problems reading encoding", e);
      }
    } else {
      try {
	rin = new InputStreamReader(in, inputEncoding);
      } catch( UnsupportedEncodingException e ) {
	throw new ServiceCreateException("non-existant input encoding "+
					 "set in DictFilter", e);
      }
    }
      
    ReaderCharSource rc = new ReaderCharSource(rin);
    r.setIn(rc);

    try {
      return new DfaRunService(r, new PrintStream(out, 
						  true, outputEncoding));
    } catch( UnsupportedEncodingException e ) {
      throw new ServiceCreateException
	("non-existant output encoding specified in DictFilter", e);
    }
  }
  /********************************************************************/
  private static final FaAction do_template = new AbstractFaAction() {
      Map m = new HashMap();
      public void invoke(StringBuffer yytext, int start, DfaRun r) 
	throws CallbackException
      {
	ReadHelper rh = (ReadHelper)r.clientData;
	
	m.clear();
	Xml.splitElement(m, yytext, start);
	
	if( m.size()!=2 ) {
	  throw new CallbackException
	    ("malformed template, attributes not allowed");
	}
	// don't clean yytext yet to be able to generate meaningful
	// error message, just record position to where to clean.
	int l = yytext.length();
	
	yytext.append(m.get(Xml.CONTENT));
	StdCharEntities.toChar(yytext, l);
	
	try {
	  rh.recentTemplate = new PrintfFormatter(yytext, l);
	} catch( ReSyntaxException e ) {
	  yytext.setLength(l);
	  throw new CallbackException
	    ("malformed template content (see cause)", e);
	}
	yytext.setLength(start);	
      }
    };
  /********************************************************************/
  private static final FaAction do_t_r = new AbstractFaAction() {
      Map m = new HashMap();
      DfaRun convert;
      {
	try {
	  convert = 
	    Term2Re.createConverter(Term2Re.wordSplitRe,
				    //"[ \\-_]?",
				    Term2Re.wordSepRe,
				    Term2Re.trailContextRe);
	} catch( ReSyntaxException e ) {
	  throw new Error("impossible", e);
	}
      }
      private String convert(String txt) {
	try {
	  return convert.filter(txt);
	} catch( IOException e ) {
	  throw new Error("impossible", e);
	}
      }

      public void invoke(StringBuffer yytext, int start, DfaRun r) 
	throws CallbackException
      {
	ReadHelper rh = (ReadHelper)r.clientData;
	
	// <t> and <r> must come after some <template>
	if( rh.recentTemplate==null ) {
	  throw new CallbackException("no <template> yet");
	}
	
	boolean isTerm = yytext.charAt(start+1)=='t';
	
	m.clear();
	Xml.splitElement(m, yytext, start);
	
	// We keep the content of yytext almost to the very end to be
	// able to set up a meaningful context for a throw.
	int l = yytext.length();
	
	// This TextStore will be preloaded with the p1, p2,
	// ... attributes of this <t> or <r> element, will be stored in
	// the MwtCallback and will eventually be used to call a
	// PrintfFormatter. Part 0 will be filled on the fly later on by
	// the MwtCallback with the match itself
	TextStore fsp = new TextStore();
	fsp.appendPart(yytext, 0, 0);
	
	// Fetch all attributes p1, p2, etc. in consecutive order and
	// fill them into fsp. We also delete them from m to check
	// later that there are no superfluous attributes
	int i = 1;
	yytext.append('p');
	while( true ) {
	  yytext.append(i++);
	  String key = yytext.substring(l);
	  yytext.setLength(l+1);
	  
	  Object pAttrib = m.remove(key);
	  if( pAttrib==null ) break;
	  
	  // transform standard XML character entities to characters
	  // while misusing yytext as a buffer
	  yytext.append(pAttrib);
	  StdCharEntities.toChar(yytext, l+1);
	  fsp.appendPart(yytext, l+1, yytext.length());
	  yytext.setLength(l+1);
	}
	yytext.setLength(l);
	
	
	String re = StdCharEntities.toChar((String)m.remove(Xml.CONTENT));
	
	int tc = 0;		// length of trailing context
	if( isTerm ) {
	  re = convert(re);
	  tc = 1;
	} else {
	  String tmp = (String)m.remove("tc");
	  if( tmp==null ) tmp="0";
	  try {
	    tc = Integer.parseInt(tmp);	      
	  } catch( NumberFormatException e ) {
	    throw new CallbackException
	      ("found tc attribute which is not a number", e);
	  }
	  if( tc<0 ) {
	    throw new CallbackException("found negative tc attribute");
	  }
	}

	// the map must be empty by now (except one last key/value pair)
	m.remove(Xml.TAGNAME);
	if( m.size()>0 ) {
	  StringBuffer sb = new StringBuffer();
	  sb.append("superfluous attributes:");
	  Iterator it = m.keySet().iterator();
	  while( it.hasNext() ) {
	    Object key = it.next();
	    sb.append(' ').append(key).append('=').append(m.get(key));
	  }
	  throw new CallbackException(sb.toString());
	}
	
	if( rh.verbose ) System.err.println(">>"+re+"<<");
	try {
	  rh.dict.or(re, new MwtCallback(fsp, rh.recentTemplate, tc, 
					 rh.nextPrio++));
	} catch( ReSyntaxException e ) {
	  throw new CallbackException
	    ("regular expression syntax error (see cause)", e);
	}
	yytext.setLength(start);
      }
    };
    
  /********************************************************************/
  private static class MwtCallback extends AbstractFaAction {
    TextStore store;
    PrintfFormatter f;
    int tc;			// length of trailing context
    public MwtCallback(TextStore store, PrintfFormatter f, 
		       int tc, int prio) {
      this.store = store;
      this.f = f;
      this.tc = tc;
      this.priority = prio;
    }
    public void invoke(StringBuffer yytext, int start, DfaRun r) 
      throws CallbackException
    {
      int L = yytext.length();
      // matches from regular expressions created by Term2Re always
      // match a trailing character, normally blank. We have to give it
      // back.
      if( tc>0 && tc<L-start ) r.unskip(yytext, L=L-tc);

      synchronized(store) {
	int storeLen = store.length();
	store.append(yytext, start, L);
	store.setPart(0, storeLen, storeLen+(L-start));
	yytext.setLength(start);
	f.format(yytext, store, null);
	store.setLength(storeLen);
      }
    }
    public String toString() {
      StringBuffer sb = new StringBuffer(80);
      sb.append(super.toString()).append("[store=`");
      store.getPart(sb, 0);
      //sb.append("', prio=").append(getPriority())
      sb.append("', formatter=`").append(f);
      sb.append("']");
      return sb.toString();
    }
  }
  /********************************************************************/
  /**
   * run on the commandline with <code>-h</code> to get a description.
   */
  public static void main(String[] argv) 
    throws java.io.IOException, CompileDfaException, ReSyntaxException 
  {
    String prog = System.getProperty("argv0", "DictFilter");

    Commandline cmd = new Commandline
      (prog, 
       "filter and tag text according to a dictionary (mwt file)",
       "filter", "dictionary file in mwt-format", 1, 1);
    cmd.addOption(new BooleanOption("-v", "write all generated regular "+
				    "expressions to standard error or the "+
				    "logfile"));
    cmd.addOption(new EnumOption
		  ("-t", "type", 
		   "type of input: raw=plain ascii, xml=recognize and "+
		   "skip xml tags, elem=tag only within xml element given "+
		   "with option -e",
		   1, 1, "|raw|xml|elem", null).required());
    String[] dflt = {"plain"};
    cmd.addOption(new Option
		  ("-e", "elem",
		   "specifies xml element within which to work, when "+
		   "'-t elem' is specified",
		   1, 1, dflt));
    cmd.addOption(new LongOption
		  ("-p", "port", 
		   "run as a server on given port instead of filtering "+
		   "stdin->stdout",
		   1, 1, 0, 65535, null));
    cmd.addOption(new Option
		  ("-c", "fname",
		   "store the compiled DFA in file fname and exit",
		   1, 1, null));
    cmd.addOption(new BooleanOption
		  ("-caw", "suppress additon of a catch-all word to the "
		   +"automaton that prevents against matching within "
		   +"words. You then could add the catch-all to the mwt "
		   +"file."));
    cmd.addOption(new Option("-ie", "inEnc",
			     "encoding used for input stream, guessed "
			     +"from input if not specified and then "
			     +"defaults to the platform encoding",
			     1, 1, null));

    cmd.addOption(new Option("-oe", "outEnc",
			     "encoding used for output stream, "
			     +"defaults to the platform encoding",
			     1, 1, null));
    cmd.addOption(new BooleanOption("-dm", "debug memory: write memory "+
				    "of Dfa and Nfa to stderr"));
    
    try {
      cmd.parse(argv);
    } catch( CommandlineException e ) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    // fail fast on wrong encodings
    String[] enc = {null, null};
    String[] eopts = {"-ie", "-oe"};
    for(int i=0; i<eopts.length; i++) {
      if( !cmd.available(eopts[i]) ) continue;
      String s = enc[i] = cmd.getStringValue(eopts[i], null);
      try {
	java.nio.charset.Charset.forName(s);
      } catch( java.nio.charset.UnsupportedCharsetException e ) {
	System.err.println(prog+": character set `"+s+"' not supported");
	System.exit(1);
      }
    }
      
    boolean verbose = cmd.available("-v");
    boolean memDebug = cmd.available("-dm");
    boolean defaultWord = !cmd.available("-caw");
    String mwtFileName = (String)cmd.getValue("--");
    String inputType = (String)cmd.getValue("-t");
    String elemName = (String)cmd.getValue("-e");

    java.io.InputStream mwtFile = 
      new BufferedInputStream(new java.io.FileInputStream(mwtFileName));
    String mwtEnc = monq.stuff.EncodingDetector.detect(mwtFile);
    Reader rin = new InputStreamReader(mwtFile, mwtEnc);
    DictFilter dict = new DictFilter(rin, inputType, elemName, 
				     verbose, memDebug, defaultWord);
    mwtFile.close();

    // now set the encodings verified earlier
    if( enc[0]!=null ) dict.setInputEncoding(enc[0]);
    if( enc[1]!=null ) dict.setOutputEncoding(enc[1]);


    if( cmd.available("-c") ) {
      String dfaFileName = cmd.getStringValue("-c", null);
      ObjectOutputStream out = 
	new ObjectOutputStream(new FileOutputStream(dfaFileName));
      if( verbose ) { 
	System.err.println("Writing DFA to `"+dfaFileName+"'");
      }
      out.writeObject(DfaRun.UNMATCHED_COPY);
      out.writeObject(dict.getDfa());
      out.close();
      System.exit(0);
    }

    if( cmd.available("-p") ) {
      //go into server mood
      FilterServiceFactory fsf = new FilterServiceFactory(dict);
      int port = ((Long)cmd.getValue("-p")).intValue();
      new TcpServer(port, fsf, 20).setLogging(System.out).serve();
    } else {
      // filter stdin to stdout, use the file descriptor to be able to
      // reset the encoding and, furthermore, to not unnecessarily
      // wrap one PrintStream into another.
      OutputStream out = new FileOutputStream(FileDescriptor.out);
      Service s = dict.createService(System.in, out, null);
      s.run();
    }
  }
  /********************************************************************/
}
 
