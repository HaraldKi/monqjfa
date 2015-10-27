package monq.jfa;


/**
 * <p>defines the interface needed by an {@link Nfa} to "fill" itself
 * with a regular expression. An <code>ReParser</code> is always
 * called through an <code>Nfa</code>. In particular direct calls to
 * {@link #parse parse()} are not intended.</p>
 * 
 * <p><code>ReParser</code> objects type need not be instantiated
 * directly. Use constructors and methods of {@link Nfa} to parse
 * regular expressions into finite automata.
 */
public interface ReParser {

  /**
   * <p>parses <code>regex</code> while building up the automaton in
   * <code>nfa</code>. This method is solely used by {@link Nfa} and
   * only <code>Nfa</code> knows how to provide an implementation of
   * {@link NfaParserView}. This method must be implemented by
   * alternative implementations of <code>ReParser</code> but is not
   * intended to be called directly.</p>
   */
  void parse(NfaParserView nfa, CharSequence regex) 
    throws ReSyntaxException;
  
  /**
   * <p>provides all characters that have a special meaning for this
   * parser</p>
   */
  String specialChars();

  /**
   * <p>starting with the character at <code>startAt</code> the given
   * <code>CharSequence</code> is copied to <code>out</code> while all
   * characters with special meaning for this parser are suitably
   * escaped. The result can be used as a regular expression that
   * matches exactly the input sequence.</p>
   * 
   */
  void escape(StringBuffer out, CharSequence in, int startAt);

}
