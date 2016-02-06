package monq.jfa.actions;

import monq.jfa.CallbackException;
import monq.jfa.DfaRun;
import monq.jfa.FaAction;

/**
 * provides an action that always returns the other action from
 * {@link #mergeWith}. The intention is that {@code this} never
   * survives in a DFA compilation. Should it do nevertheless, the default
   * action is called in case {@code this} is called.
 */
public class DefaultAction implements FaAction {
  private final FaAction dflt;
  private static final DefaultAction NULL_INSTANCE = new DefaultAction(null);
  public DefaultAction(FaAction dflt) {
    this.dflt = dflt;
  }
  /**
   * returns an instance with no default. It should only be used to implement
   * operations on automata. If this action is hit during operation of an
   * automaton, it will result in an {@code NullPointerException}.
   */
  public static DefaultAction nullInstance() {
    return NULL_INSTANCE;
  }
  @Override
  public void invoke(StringBuilder yytext, int start, DfaRun runner)
    throws CallbackException
  {
    dflt.invoke(yytext, start, runner);
  }

  /**
   * always returns {@code other}. 
   */
  @Override
  public FaAction mergeWith(FaAction other) {
    return other;
  }
  @Override
  public String toString() {
    return getClass().getName()+'('+dflt+')';
  }
}
