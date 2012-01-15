package monq.stuff;

import monq.jfa.*;
import monq.jfa.actions.*;

import java.io.*;
import java.util.*;

/**
 * <p>provides static methods to guess the character encoding used in an
 * <code>InputStream</code> supposedly containing XML or HTML.</p>
 *
 * <p><b>Note:</b> Only parts of the recommendation mentioned below
 * are implemented.</p>
 *
 * @see <a
 * href="http://www.w3.org/TR/REC-xml/#sec-guessing-no-ext-info">XML
 * recommendation on guessing the encoding</a> 
 *
 * @version $Revision: 1.3 $, $Date: 2005-07-08 13:01:26 $
 * @author &copy; Harald Kirsch
 */
public class EncodingDetector {

  private static final Dfa dfa;
  /**
   * <p>the platform's default encoding determined by opening an
   * <code>InputStreamReader</code> on <code>System.in</code> and
   * asking for its encoding. </p>
   */
  public static final String defaultEnc =
    new InputStreamReader(System.in).getEncoding();

  // this will be filled some time with the detected encoding.
  private String enc = null;

  /**********************************************************************/
  // no need to find this in the docs
  private EncodingDetector() {}
  /**********************************************************************/
  private static FaAction do_encoding = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun r) {
	//System.err.println("gotcha: `"+yytext.substring(start)+"'");
	Map m = Xml.splitElement(yytext, start);
	yytext.setLength(start);
      
	EncodingDetector ed = (EncodingDetector)r.clientData;
	ed.enc = (String)m.get("encoding");
      
	//System.err.println("now we have enc="+ed.enc);
	if( ed.enc!=null ) r.setIn(new CharSequenceCharSource(""));
      }
    };

  private static FaAction do_content = new AbstractFaAction() {
      public void invoke(StringBuffer yytext, int start, DfaRun r) {
	//System.err.println("gotcha: `"+yytext.substring(start)+"'");
	Map m = Xml.splitElement(yytext, start);
	yytext.setLength(start);
	
	EncodingDetector ed = (EncodingDetector)r.clientData;
	String s = null;
	if( !"Content-Type".equals(m.get("http-equiv"))
	    || (s=(String)m.get("content"))==null ) return;

	// fetch the charset=... out of s
	int pos = s.indexOf("charset=");
	ed.enc = s.substring(pos+8);
	r.setIn(new CharSequenceCharSource(""));
      }
    };
  /**********************************************************************/
  static {
    try {
      Nfa nfa = new 
	Nfa(Xml.XMLDecl, do_encoding)
	.or(Xml.STag("meta")+"|"+Xml.EmptyElemTag("meta"), do_content)
	.or(Xml.STag()+"|"+Xml.EmptyElemTag(), new Drop(-1))
	;
      dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    } catch( ReSyntaxException e ) {
      throw new Error("bug", e);
    } catch( CompileDfaException e ) {
      throw new Error("bug", e);
    }
  }
  /**********************************************************************/
  private static class TrivCharSource extends EmptyCharSource {
    private InputStream in;
    private int rest;
    private int nbytes;
    private int which;
    public TrivCharSource(InputStream in, int count, 
			  int nbytes, int which) {
      this.in = in;
      this.rest = count;
      this.nbytes = nbytes;
      this.which = which;
    }
    private int readOneChar() throws IOException {
      int result = 0;
      for(int i=0; i<nbytes; i++) {
	if( rest==0 ) return -1;
	rest -= 1;
	int ch = in.read();
	if( ch<0 ) return -1;
	if( i==which ) result = ch;
      }
      return result;
    }
    public int read() throws java.io.IOException {
      int ch = super.readOne();
      if( ch>=0 ) return ch;
      return readOneChar();
    }
  }
  /**********************************************************************/
  /**
   * <p>reads up to <code>limit</code> bytes from the given input stream
   * to find out the character encoding used. If no encoding can be
   * derived, the given default value is returned.</p>
   *
   * <p>This method implements a partial and slightly modified
   * version of the recommendations described in the <a
   * href="http://www.w3.org/TR/REC-xml/#sec-guessing-no-ext-info">XML
   * specification</a>.</p>
   *
   * <p><b>With a Byte Order Mark:</b> These seem to apply also to
   * HTML. If any of those is recognized, this method returns
   * immediatly with the appropriate encoding name. However, for
   * <b>UCS-4</b> with unusual 
   * octet order, <code>deflt</code> is returned in lack of a useful
   * Java encoding name. The possible return values in this case are
   * <code>UTF-32BE</code>, <code>UTF-32LE</code>,
   * <code>UTF-16BE</code>, <code>UTF-16LE</code>, <code>UTF-8</code>.</p>
   *
   * <p><b>Without a Byte Order Mark:</b> To cover HTML too, first the
   * position of the <code>'&lt;'</code> byte is detected in the first
   * four byte. This is taken as an indication of how many bytes have
   * to be read per character and which of those contains the ASCII
   * equivalent of the character &mdash; at least until the encoding
   * name was found. If no <code>0x3C</code> is found,
   * <code>deflt</code> is returned immediatly. In particular this
   * means that EBCDIC is not handled by this implemention.</p>
   *
   * <p>After the byte setup has been guessed, the
   * input stream is scanned for up to <code>limit</code> bytes to
   * either find an 
   * <a href="http://www.w3.org/TR/REC-xml/#NT-XMLDecl">XML
   * declaration</a> or an HTML
   * <a href="http://www.w3.org/TR/html4/charset.html#h-5.2.2"><code>meta</code> tag</a> 
   * which describes the content type and character set used. The
   * possible return values are whatever was found as either
   * <code>encoding</code> (XML) or as <code>charset</code> (HTML) in
   * the file.</p>
   *
   * <p>Under all circumstances, the <code>InputStream</code> is reset to
   * it start before this method returns. This requires that the
   * <code>InputStream</code> supports the <code>mark()</code>
   * method.</p>
   * 
   * @param in the input stream to read
   * @param limit maximum number of <b>bytes</b> to read for guessing
   * @param deflt a default value to return when nothing can be guessed;
   * consider passing in {@link #defaultEnc}
   * @return the encoding guessed or the given <code>deflt</code>.
   *
   * @throws IllegalArgumentException if <code>in</code> does not
   * support the <code>mark()</code> method.
   */
  public static String detect(InputStream in, int limit, String deflt) 
    throws IOException
  {
    if( !in.markSupported() ) {
      throw new IllegalArgumentException 
	("input stream doe not support mark");
    }
    in.mark(limit);

    // read first four bytes and act according to
    // http://www.w3.org/TR/REC-xml/#sec-guessing-no-ext-info
    int tag = 0;
    for(int i=0; i<4; i++) {
      int b = in.read();
      if( b<0 ) {
	in.reset();
	return deflt;
      }
      tag = (tag<<8)|b;
    }
    in.reset();

    // First check the possibilities where there is a byte order mark
    if( tag==0x0000feff ) return "UTF-32BE";
    if( tag==0xfffe0000 )return "UTF-32LE";
    // FIX ME: What would be a name for those:
    if( tag==0x0000fffe ) return deflt;
    if( tag==0xfeff0000 ) return deflt;
    if( (tag&0xfeff0000)==0xfeff0000 ) return "UTF-16BE";
    if( (tag&0xfffe0000)==0xfffe0000 ) return "UTF-16LE";
    if( (tag&0xefbbbf00)==0xefbbbf00 ) return "UTF-8";


    // Without byte order mark we end up in classes of encodings and
    // the encoding must be read from the XML declaration. In all
    // cases, it is ok to read a fixed number of bytes for a character
    // and convert one of the bytes to a character by casting.
    // To cover HTML as well as XML, we only look at the '<' and where
    // it appears.
    int nbytes = -1;
    int whichByte = -1;
    if( tag==0x0000003c ) {nbytes=4; whichByte=3;}
    if( tag==0x00003c00 ) {nbytes=4; whichByte=2;}
    if( tag==0x003c0000 ) {nbytes=4; whichByte=1;}
    if( tag==0x3c000000 ) {nbytes=4; whichByte=0;}
    if( (tag>>16)==0x003c ) {nbytes=2; whichByte=1;}
    if( (tag>>16)==0x3c00 ) {nbytes=2; whichByte=0;}
    if( (tag>>24)==0x3c ) {nbytes=1; whichByte=0;}

    if( nbytes<0 ) return deflt;

    TrivCharSource tcs = new TrivCharSource(in, limit, nbytes, whichByte);
    DfaRun r = new DfaRun(dfa, tcs);
    EncodingDetector ed = new EncodingDetector();
    r.clientData = ed;
    r.filter();
    in.reset();
    if( ed.enc==null ) return deflt;
    else return ed.enc;
  }
  /**********************************************************************/
  /**
   * <p>tries to detect the character encoding used in the given
   * <code>InputStream</code> within the first 1000 bytes. It returns
   * the {@link #defaultEnc} if no encoding can be
   * guessed.</p>
   *
   * @see #detect(InputStream,int,String)
   */
  public static String detect(InputStream in) throws IOException {
    String result = detect(in, 1000, null);
    if( result==null ) return defaultEnc;

    return result;
  }
  /**********************************************************************/
  /**
   * @deprecated  for testing purposes only
   */
  public static void main(String[] argv) throws Exception {
    InputStream is = new FileInputStream(argv[0]);
    if( !is.markSupported() ) is = new BufferedInputStream(is);
    String encoding = detect(is);
    System.err.println("Encoding: `"+encoding+"'");
    Reader r = new InputStreamReader(is);
    for(int i=0; i<100; i++) {
      System.out.print((char)r.read());
    }
    System.out.println();
  }
}
