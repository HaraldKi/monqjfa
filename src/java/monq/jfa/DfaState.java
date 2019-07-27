package monq.jfa;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class DfaState implements FaState<DfaState> {
  // one entry for each action that can be reached by passing through this
  // state.
  private Map<FaAction,FaSubinfo[]> subinfos = null;
  private CharTrans<DfaState> trans = EmptyCharTrans.instance();
  private FaAction action = null;
  
  public DfaState() {
    // nothing
  }
  public DfaState(FaAction a) {
    this.action = a;
  }
  @Override
  public boolean isImportant() {
    return getTrans().size()==0 || getAction()!=null || subinfos!=null;
  }

  @Override
  public DfaState[] getEps() {
    return null;
  }

  @Override
  public void setEps(DfaState[] newEps) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addEps(DfaState[] others) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addEps(DfaState other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CharTrans<DfaState> getTrans() {
    return trans;
  }

  @Override
  public void setTrans(CharTrans<DfaState> trans) {
    if (trans==null) {
      this.trans = EmptyCharTrans.instance();
    } else {
      this.trans = trans;
    }
  }

  @Override
  public FaAction getAction() {
    return action;
  }

  @Override
  public void clearAction() {
    action = null;
  }

  @Override
  public Iterator<DfaState> getChildIterator(monq.jfa.FaState.IterType iType) {
    return new ChildIterator(trans);
  }

  @Override
  public DfaState follow(char ch) {
    CharTrans<DfaState> t = getTrans();
    if( t==null ) return null;
    DfaState state = t.get(ch);
    return state;
  }

  @Override
  public void addUnassignedSub(FaSubinfo sfi) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reassignSub(FaAction from, FaAction to) {
    throw new UnsupportedOperationException();    
  }

  @Override
  public <X extends FaState<X>> void mergeSubinfos(Set<X> nfaStates) {
    subinfos = AbstractFaState.mergeSubinfosInto(subinfos, nfaStates);
  }

  @Override
  public Map<FaAction,FaSubinfo[]> getSubinfos() {
    return subinfos;
  }
  
  private static final class ChildIterator implements Iterator<DfaState> {
    private final CharTrans<DfaState> trans;
    private int next = 0;

    public ChildIterator(CharTrans<DfaState> t) {
      this.trans = t;
    }
    @Override
    public boolean hasNext() {
      return next<trans.size();
    }

    @Override
    public DfaState next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return trans.getAt(next++);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
    
  }
}
