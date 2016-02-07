package monq.jfa;

/**
 * <p>implements the <a href="doc-files/resyntax.html">regular
 * expression syntax</a> classically used by <code>monq.jfa</code>. To
 * use the classic syntax in your application, call {@link
 * Nfa#setDefaultParserFactory Nfa.setDefaultParserFactory()} with
 * {@link #factory}.</p>
 */
public class ReClassicParser implements ReParser {
  private static final int TOK_EOF      = Character.MAX_VALUE+1;
  private static final int TOK_OBRACKET = Character.MAX_VALUE+'[';
  private static final int TOK_CBRACKET = Character.MAX_VALUE+']';
  private static final int TOK_OPAREN   = Character.MAX_VALUE+'(';
  private static final int TOK_CPAREN   = Character.MAX_VALUE+')';
  private static final int TOK_OCURLY   = Character.MAX_VALUE+'{';
  private static final int TOK_QMARK    = Character.MAX_VALUE+'?';
  private static final int TOK_STAR     = Character.MAX_VALUE+'*';
  private static final int TOK_PLUS     = Character.MAX_VALUE+'+';
  private static final int TOK_OR       = Character.MAX_VALUE+'|';
  private static final int TOK_DOT      = Character.MAX_VALUE+'.';
  private static final int TOK_EXCL     = Character.MAX_VALUE+'!';
  private static final int TOK_TILDE    = Character.MAX_VALUE+'~';
  private static final int TOK_HAT      = Character.MAX_VALUE+'^';
  private static final int TOK_MINUS    = Character.MAX_VALUE+'-';
  private static final int TOK_AT       = Character.MAX_VALUE+'@';

  private static final char[] specialChars


    = "[]{}()?*+|.!^-\\~@,".toCharArray();
  static { java.util.Arrays.sort(specialChars); }

  private static final String INTERNAL_ERROR_TMPL =
      "internal error, this should not happen. A call to %s "+
          "results in a compilation of the Nfa constructed "+
          "so far. Because this Nfa should not yet have any Actions "+
          "associated, there cannot be any ambiguous.";

  //private Reader in;
  private CharSequence in;
  private int inNext = 0;
  private StringBuilder tmp = new StringBuilder(30);
  private int token;

  // the following two are needed to efficiently collect long strings
  // into a sequential automaton. In an re like "abcd?" however, the
  // '?' applies only to the 'd', so 'd' and '?' must be seen to
  // decide that 'c' is the last character of teh sequential
  // automaton.
  private int lookaheadToken;
  private boolean lookaheadValid = false;

  // recent input, kept for error messages
  private char[] recent = new char[60];
  private int recentNext = 0;                   // first free in recent
  private boolean recentWrapped = false;

  /********************************************************************/
  /**
   * <p>is a factory object that creates instances of
   * <code>ReClassicParser</code>.</p>
   *
   * @see Nfa#defaultParserFactory
   */
  public static final ReParserFactory factory = new ReParserFactory() {
      @Override
      public ReParser newReParser() {
	return new ReClassicParser();
      }
    };
  /********************************************************************/
  private ReClassicParser() {}
  /********************************************************************/
  @Override
  public synchronized void parse(NfaParserView nfa, CharSequence s)
    throws  ReSyntaxException
  {
    in = s;
    inNext = 0;
    recentNext = 0;
    recentWrapped = false;
    nextToken();
    parseOr(nfa);
    if( token!=TOK_EOF ) throw error(ReSyntaxException.EEXTRACHAR);
  }
  /**********************************************************************/
  @Override
  public String specialChars() { return new String(specialChars); }
  /**
   * @see #escape(String)
   */
  /**********************************************************************/
  @Override
  public void escape(StringBuilder out, CharSequence cin, int startAt) {
    int l = cin.length();
    for(int i=startAt; i<l; i++) {
      char ch = cin.charAt(i);
      int pos = java.util.Arrays.binarySearch(specialChars, ch);
      if( pos>=0 ) out.append('\\');
      out.append(ch);
    }
  }
  /**********************************************************************/
  private ReSyntaxException error(String msg) {
    tmp.setLength(0);
    getRecent(tmp);
    int column = tmp.length();

    int ch;
    for(int i=0; -1!=(ch=nextChar()) && i<10; i++) tmp.append((char)ch);

    return new ReSyntaxException(msg, tmp.toString(), 0, column);
  }

  private void getRecent(StringBuilder b) {
    int pos = recentWrapped ? recentNext+1 : 0;
    while( pos!=recentNext ) {
      b.append(recent[pos]);
      pos += 1;
      if( pos==recent.length ) pos=0;
    }
  }
  private int nextChar() {
    //int ch = in.read();
    //if( ch==-1 ) return ch;
    if( inNext==in.length() ) return -1;
    int ch = in.charAt(inNext++);

    //System.out.println("-->"+(char)ch+" "+ch);

    recent[recentNext] = (char)ch;
    recentNext += 1;
    if( recentNext==recent.length ) {
      recentWrapped = true;
      recentNext = 0;
    }
    return ch;
  }
  private void pushBack(int valueForToken) {
    lookaheadValid = true;
    lookaheadToken = token;
    token = valueForToken;
  }

  private void nextBracketToken() throws ReSyntaxException {
    _nextToken(true);
  }

  private void nextToken() throws ReSyntaxException {
    _nextToken(false);
  }

  private void _nextToken(boolean withinBracket) throws ReSyntaxException {

    if( lookaheadValid ) {
      token = lookaheadToken;
      lookaheadValid = false;
      return;
    }

    int ch = nextChar();

    if( ch==-1 ) {
      token = TOK_EOF;
      return;
    }

    // a backslash escapes everything, everywhere, except EOF
    if( ch=='\\' ) {
      ch = nextChar();
      if( ch==-1 ) throw error(ReSyntaxException.EBSATEOF);
      if (ch=='u') {
        token = hexChar(4);
      } else if (ch=='x') {
        token = hexChar(2);
      } else {
        token = ch;
      }
      return;
    }

    if( withinBracket ) {
      switch( ch ) {
      case '-': token = TOK_MINUS; break;
      case ']': token = TOK_CBRACKET; break;
      case '^': token = TOK_HAT; break;
      default: token = ch;
      }
    } else {
      switch( ch ) {
      case '[': token = TOK_OBRACKET; break;
      case '{': token = TOK_OCURLY; break;
      case '(': token = TOK_OPAREN; break;
      case ')': token = TOK_CPAREN; break;
      case '?': token = TOK_QMARK; break;
      case '*': token = TOK_STAR; break;
      case '+': token = TOK_PLUS; break;
      case '|': token = TOK_OR; break;
      case '.': token = TOK_DOT; break;
      case '!': token = TOK_EXCL; break;
      case '~': token = TOK_TILDE; break;
      case '^': token = TOK_HAT; break;
      case '@': token = TOK_AT; break;
      default: token = ch;
      }
      //System.out.println("xx>"+(char)ch+" "+ch);
    }
  }
  /********************************************************************/
  private char hexChar(int numDigits) throws ReSyntaxException {
    int result = 0;
    for (int i=0; i<numDigits; i++) {
      int ch = nextChar();
      if (ch<0) {
        throw error(ReSyntaxException.EEOFUNEX);
      }
      if (ch>='0' && ch<='9') {
        result = (16*result)+(ch-'0');
        continue;
      }
      ch = Character.toLowerCase(ch);
      if (ch>='a' && ch<='f') {
        result = (16*result)+(ch-'a'+10);
        continue;
      }
      throw error(ReSyntaxException.ENOHEX);
    }
    return (char)result;
  }
  /********************************************************************/
  private void parseBracket(NfaParserView nfa) throws  ReSyntaxException {
    // we just saw the opening bracket

    boolean invert = false;
    tmp.setLength(0);

    // At the start of a character range, the following characters are
    // recognized specially in this order "^]-"
    if( token==TOK_HAT ) {
      invert = true;
      nextBracketToken();
    }
    if( token==TOK_CBRACKET ) {
      tmp.append("]]");
      nextBracketToken();
    }
    if( token==TOK_MINUS ) {
      tmp.append("--");
      nextBracketToken();
    }

    // collect single characters and character ranges
    while( token!=TOK_CBRACKET ) {
      if( token==TOK_EOF) throw error(ReSyntaxException.EEOFUNEX);
      if( token>Character.MAX_VALUE || token<Character.MIN_VALUE) {
        throw error(ReSyntaxException.ECHARUNEX);
      }
      int ch = token;
      nextBracketToken();
      if( token!=TOK_MINUS ) {
	tmp.append((char)ch).append((char)ch);
        continue;
      }
      nextBracketToken();
      if( token>Character.MAX_VALUE ) {
        throw error(ReSyntaxException.EINVALUL);
      }
      if( ch>token ) {
        throw error(ReSyntaxException.EINVRANGE);
      }
      tmp.append((char)ch).append((char)token);
      nextBracketToken();
    }
    nextToken();

    nfa.pushCharSet(tmp, invert);
  }
  /********************************************************************/

  private void parseAtom(NfaParserView nfa) throws  ReSyntaxException {
    // a '(' starts a full regular expression. If the very next
    // character is TOK_EXCL, this is a reporting subexpression.
    if( token==TOK_OPAREN ) {
      boolean isReporting = false;
      nextToken();
      if( token==TOK_EXCL ) {
        isReporting = true;
        nextToken();
      }
      parseOr(nfa);
      if( token!=TOK_CPAREN ) throw error(ReSyntaxException.ECLOSINGP);
      nextToken();

      if( isReporting ) {
        if( !nfa.markAsSub() ) {
          throw error(ReSyntaxException.ETOOMANYREPORTING);
        }
      }
      return;
    }

    // a '[' starts a character class
    if( token==TOK_OBRACKET ) {
      nextBracketToken();
      parseBracket(nfa);
      return;
    }

    // a '.' stands for every character
    if( token==TOK_DOT ) {
      nextToken();
      nfa.pushDot();
      return;
    }

    // Normal characters are allowed here too. We collect consecutive
    // sequences of those because a string can be turned into an Fa
    // quite compact.
    if( token<=Character.MAX_VALUE ) {
      tmp.setLength(0);
      tmp.append((char)token);
      nextToken();
      for(/**/; token<=Character.MAX_VALUE; nextToken() ) {
        tmp.append((char)token);
      }
      int L = tmp.length();
      if( L>1 ) {
        // token might be a postfix operator and as such should not be
        // applied to more than one character.
        // FIX ME: in principle the pushback is only necessary if in
        // fact a postfix operator follows. However, I don't dare to
        // use this info because it is to easily forgotten the tokens
        // for postfix operators change.
        pushBack(tmp.charAt(L-1));
        tmp.setLength(L-1);
      }
      //System.out.println(":::"+tmp.toString());
      nfa.pushString(tmp);
      return;
    }

    // everything else is an error
    if( token==TOK_EOF ) throw error(ReSyntaxException.EEOFUNEX);
    throw error(ReSyntaxException.ECHARUNEX);
  }
  /********************************************************************/
  private void parsePostfixedAtom(NfaParserView nfa)
    throws  ReSyntaxException {

    parseAtom(nfa);

    boolean tryPostfix = true;
    while( tryPostfix ) {
      switch( token ) {
      case TOK_QMARK: nfa.optional(); nextToken(); break;
      case TOK_STAR: nfa.star(); nextToken(); break;
      case TOK_PLUS: nfa.plus(); nextToken(); break;
      case TOK_EXCL: {
        try {
          nfa.shortest();
        } catch( CompileDfaException e) {
          throw error(String.format(INTERNAL_ERROR_TMPL, "shortest()"));
        }
        nextToken();
        break;
      }
      case TOK_TILDE: {
        try {
          nfa.invert();
        } catch( CompileDfaException e) {
          throw error(String.format(INTERNAL_ERROR_TMPL, "invert()"));
        }
        nextToken();
        break;
      }
      case TOK_HAT: {
        try {
          nfa.not();
        } catch( CompileDfaException e) {
          throw error(String.format(INTERNAL_ERROR_TMPL, "not()"));
        }
        nextToken();
        break;
      }
      case TOK_AT:
        try {
          nfa.allPrefixes();
        } catch( CompileDfaException e ) {
          throw error(String.format(INTERNAL_ERROR_TMPL, "allPrefixes()"));
        }
        nextToken();
        break;
      case TOK_OCURLY:
        nextToken();
        parseRepeatCount(nfa);
        break;
      default:
        tryPostfix = false;
        break;
      }
    }
  }
  /********************************************************************/
  private void parseRepeatCount(NfaParserView nfa) throws ReSyntaxException {
    // have just seen the '{' and nextToken was called already
    int from = parseNum();
    int to = -1;
    if (token==',') {
      nextToken();
      if (token>='0' && token<='9') {
        to = parseNum();
      }
    } else {
      to = from;
    }
    if (token!='}') {
      throw error(ReSyntaxException.EMISSINGCURLYCLOSE);
    }
    nextToken();

    // TODO: there are too many cases below. Is there a better way to do this
    // with much fewer ifs?
    if (to>-1 && to<from) {
      throw error(ReSyntaxException.ETOLESSFROM);
    }
    if (to==0) {
      throw error(ReSyntaxException.EEMPTY);
    }

    if (to<0) {
      if (from==0) {
        nfa.star();
        return;
      }
      for (int i=0; i<from; i++) {
        nfa.dup();
      }
      for (int i=0; i<from-1; i++) {
        nfa.seq();
      }
      nfa.swap();
      nfa.star();
      nfa.seq();
      return;
    }

    for (int i=0; i<to-1; i++) {
      if (i==from) {
        nfa.optional();
      }
      nfa.dup();
    }
    if (from==to-1) {
      nfa.optional();
    }
    for (int i=0; i<to-1; i++) {
      nfa.seq();
    }
  }
  /*+******************************************************************/
  private int parseNum() throws ReSyntaxException {
    int result = 0;
    while (token>='0' && token<='9') {
      result = 10*result +(token-'0');
      nextToken();
    }
    return result;
  }
  /********************************************************************/
  private void parseSequence(NfaParserView nfa) throws  ReSyntaxException {
    parsePostfixedAtom(nfa);

    while( token!=TOK_EOF && token!=TOK_OR && token!=TOK_CPAREN ) {
      //System.out.println("token:"+token);
      parsePostfixedAtom(nfa);
      nfa.seq();
    }
  }
  /********************************************************************/
  private void parseOr(NfaParserView nfa) throws ReSyntaxException {
    parseSequence(nfa);

    while( token==TOK_OR ) {
      nextToken();
      parseSequence(nfa);
      nfa.or();
    }
  }
  /********************************************************************/
}
