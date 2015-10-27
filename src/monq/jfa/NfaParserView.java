package monq.jfa;

/**
 * <p>is the interface to be used by an {@link ReParser} to fill an
 * {@link Nfa}. The class <code>Nfa</code> implements this interface
 * to provide a parser-specific, slightly restricted view to the
 * <code>Nfa</code> that allows for efficient and controlled built-up
 * of the finite automaton graph.</p>
 *
 * <p>The interface provides a stack-automaton for evaluation of
 * expressions creating the <code>Nfa</code> step by step. Once
 * parsing has finished, the stack must contain exactly one
 * element.</p>
 */
public interface NfaParserView {

  /**
   * <p>pushes an automaton representation on the expression stack that
   * recognizes a string of length 1 made up of one character from a
   * character set. The character set is specified in
   * <code>pairs</code> as an even number of characters. Every pair of
   * characters <em>(from,to)</em> denotes an inclusive range of
   * allowed characters. The ranges may overlap, and if <em>from</em>
   * is greater than <em>to</em>, their order is silently
   * reversed. The set specified is the union of all character ranges
   * specified by the pairs. If <code>invert</code> is
   * <code>true</code>, the complement of the resulting character set
   * is used.</p>
   *
   * <p>If the specified character set is empty, the resulting
   * automaton will not recognize anything.</p>
   * 
   * @param pairs may be <code>null</code> or an empty sequence or
   * must have an even number of characters
   *
   * @param invert requests to invert the character set before use
   */
  void pushCharSet(CharSequence pairs, boolean invert);

  /**
   * <p>is a convenience method that calls {@link #pushCharSet} such
   * that all characters are specified.</p>
   */
  void pushDot();

  /**
   * <p>pushes an automaton representation on the expression stack that
   * recognizes exactly the given character sequence.</p>
   *
   * <p><b>NOTE</b>: the given <code>CharSequence</code> is not parsed
   * as a regular expression.</p>
   *
   * @param str may <b>not</b> be <code>null</code> or empty
   */
  void pushString(CharSequence str);

  /**
   * <p>performs the equivalent of {@link Nfa#or(Nfa)} on the two top
   * elements on the expression stack.</p>
   */
  void or();
  /**
   * <p>performs the equivalent of {@link Nfa#seq(Nfa)} on the two top
   * elements on the expression stack.</p>
   */
  void seq();
  /** 
   * <p>performs the equivalent of {@link Nfa#star} on the top
   * element of the expression stack.</p>
   */
  void star();
  /** 
   * <p>performs the equivalent of {@link Nfa#plus} on the top
   * element of the expression stack.</p>
   */
  void plus();
  /** 
   * <p>performs the equivalent of {@link Nfa#optional} on the top
   * element of the expression stack.</p>
   */
  void optional();
  /** 
   * <p>performs the equivalent of {@link Nfa#not} on the top
   * element of the expression stack.</p>
   */
  void not() throws CompileDfaException;
  /** 
   * <p>performs the equivalent of {@link Nfa#invert} on the top
   * element of the expression stack.</p>
   */
  void invert() throws CompileDfaException;
  /** 
   * <p>performs the equivalent of {@link Nfa#shortest} on the top
   * element of the expression stack.</p>
   */
  void shortest() throws CompileDfaException;
  /**
   * <p>swaps the two top level elements on the expression stack.</p>
   */
  void swap();
  /** 
   * <p>performs the equivalent of {@link Nfa#markAsSub} on the top
   * element of the expression stack.</p>
   */

  boolean markAsSub();
}