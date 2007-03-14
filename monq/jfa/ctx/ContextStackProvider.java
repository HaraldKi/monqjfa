package monq.jfa.ctx;

import java.util.List;

/**
 * <p>is the interface to be implemented by an object put into {@link
 * monq.jfa.DfaRun#clientData} when any of the actions in {@link
 * monq.jfa.ctx this} package are used in a {@link monq.jfa.Nfa}.
 * To implement this interface, a class merely needs a field holding
 * a {@link java.util.List} which it returns when {@link
 * #getStack} is called.</p>
 *
 */
public interface ContextStackProvider {

  /**
   * <p>returns the {@link List} provided as a stack.</p>
   */
  List getStack();
}
