package monq.jfa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import monq.jfa.FaState.IterType;

/**
 * traverses all states reachable from a given {@link FaState} and applies a
 * visitor.
 * 
 * @param <D> is the type of fixed object passed to every visit call of the
 *        visitor.
 */
class FaStateTraverser<STATE extends FaState<STATE>, D> {

  private final D data;
  private final Set<STATE> visited =
      Collections.newSetFromMap(new IdentityHashMap<STATE, Boolean>());
  
  private final List<STATE> stack = new ArrayList<>(100);
  private final IterType iType;

  public FaStateTraverser(IterType iType, D data) {
    this.iType = iType;
    this.data = data;
  }

  public void traverse(STATE state, StateVisitor<STATE, D> stateVis) {
    stack.add(state);
    while (!stack.isEmpty()) {
      state = stack.remove(stack.size()-1);
      if (visited.contains(state)) {
        continue;
      }
      visited.add(state);
      stackNewChildren(state);
      stateVis.visit(state, data);      
    }
  }

  private void stackNewChildren(STATE state) {
    for(Iterator<STATE> it=state.getChildIterator(iType); it.hasNext();) {
      STATE child = it.next();
      if (!visited.contains(child)) {
        stack.add(child);
      }
    }
  }

  public static interface StateVisitor<STATE extends FaState<STATE>, Data> {
    void visit(STATE state, Data d);
  }
}
