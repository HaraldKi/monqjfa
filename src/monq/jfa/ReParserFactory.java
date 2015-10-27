package monq.jfa;

/**
 * <p>defines the factory interface to create instances of {@link
 * ReParser}. Different implementations of <code>ReParser</code>
 * understand different regular expression syntax. To provide
 * consistent syntax in an application, {@link
 * Nfa#setDefaultParserFactory} can be used to define the factory used
 * to create parser instances for all <code>Nfa</code>s.</p>
 */
public interface ReParserFactory {

  /**
   * <p>is a factory method that creates (must create) a new
   * <code>ReParser</code> of the same type each time it is
   * called. All parsers returned must understand the same
   * syntax. Thereby an application can use consistent regular
   * expression syntax by always using the same factory.</p>
   *
   */
  ReParser newReParser();
}
