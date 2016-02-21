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

package monq.jfa;

import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

/**
 * implements a prototype of an {@link FaState} to
 * ease subclassing for specialized states of an automaton.
 */
class AbstractFaState implements FaState<AbstractFaState>, Serializable {
  // for use in addEps(STATE) only
  private final AbstractFaState[] tmp = new AbstractFaState[1];
  private AbstractFaState[] eps = null;
  private CharTrans<AbstractFaState> trans = null;
  private FaAction action = null;

  // A state can be part of several subgraphs. Subgraphs are
  // identified by a pair (FaAction, FaSubinfo.id). The FaAction is
  // used in this map as a key. The values will be of type
  // FaSubinfo[], sorted according to FaSubinfo.id (see
  // FaSubinfo.compareTo()).
  private Map<FaAction,FaSubinfo[]> subinfos = null;
  /*+******************************************************************/
  public AbstractFaState() {
    // empty
  }
  /*+******************************************************************/
  public AbstractFaState(FaAction a) {
    this.action = a;
  }
  /*+******************************************************************/
  @Override
  public Map<FaAction,FaSubinfo[]> getSubinfos() {
    return subinfos;
  }
  /**
   * <p>Marks this state as part of the subgraph denoted by
   * <code>sfi</code> relating to the <code>null</code> action. If
   * there is already subgraph information for the <code>null</code>
   * action, is available, the following two situations are possible:
   * <ol>
   * <li><code>sfi</code> describes a different subgraph. In this
   * case, it is added.</li>
   * <li><code>sfi</code> describes a subgraph already assigned to
   * <code>null</code>. In this case the information is added.</li>
   * </ol>
   */
  private static Map<FaAction, FaSubinfo[]>
  mergeSub(Map<FaAction, FaSubinfo[]> subinfos, FaAction a, FaSubinfo sfi) {
    if( subinfos==null ) subinfos = new HashMap<FaAction,FaSubinfo[]>();
    
    // If there is nothing yet for a, simply enter what we have as a
    // new entry.
    FaSubinfo[] ary = subinfos.get(a);
    if( ary==null ) {
      //System.err.println("merging null with "+sfi+" for "+this);
      ary = new FaSubinfo[1];
      ary[0] = new FaSubinfo(sfi);
      subinfos.put(a, ary);
      return subinfos;
    }

    // see if the subgraph denoted by sfi is already present
    int pos = java.util.Arrays.binarySearch(ary, sfi);

    // If sfi already present, just merge sfi in
    if( pos>=0 ) {
      ary[pos].merge(sfi);
      return subinfos;
    }

    // Since sfi denotes a new subgraph, we have to enlarge ary.
    pos = -(pos+1);
    FaSubinfo[] tmp = new FaSubinfo[ary.length+1];
    System.arraycopy(ary, 0, tmp, 0, pos);
    System.arraycopy(ary, pos, tmp, pos+1, ary.length-pos);
    ary = tmp;
    ary[pos] = new FaSubinfo(sfi);
    subinfos.put(a, ary);
    return subinfos;
  }
  
  @Override
  public void addUnassignedSub(FaSubinfo sfi) {
    subinfos = mergeSub(subinfos, null, sfi);
  }

  /**
   * <p>Assigns subgraph information currently assigned to the
   * <code>from</code> action to the given <code>to</code>
   * action. Under most circumstances <code>from==null</code> which
   * means the subgraph was not yet really assigned.</p>
   */
  @Override
  public void reassignSub(FaAction from, FaAction to) {
    if( subinfos==null ) return;
    FaSubinfo[] o = subinfos.get(from);

    // since we never store null as a value, o==null means there was
    // no sugraph info for null and we don't have anything to assign.
    if( o==null ) return;

    // switch o over from null to a
    subinfos.remove(from);
    subinfos.put(to, o);
  }

  /**
   * called during compilation where this is a new state of the Dfa
   * and the given nfaStates is the set of states of the Nfa that
   * represent this. All subgraphs referenced are kept. For a
   * subgraph uniquely identified by a Pair (action, FaSubInfo.id),
   * the node types, i.e. start, inner or stop are merged. In the
   * extreme case, this state may be a start/inner/stop node for a
   * certain subgraph.
   */
  @Override
  public <X extends FaState<X>> void mergeSubinfos(Set<X> nfaStates) {
    subinfos = mergeSubinfosInto(subinfos, nfaStates);
  }
  
  /**
   * for the benefit of DfaState to reuse this code
   * 
   * TODO: move to some other place, possibly make the first parameter into a
   * separate class.
   */
  public static <X extends FaState<X>> Map<FaAction, FaSubinfo[]>
  mergeSubinfosInto(Map<FaAction,FaSubinfo[]> subinfos, Set<X> nfaStates) {
    // loop over all given states
    Iterator<X> states = nfaStates.iterator();
    while( states.hasNext() ) {
      FaState<X> other = states.next();
      Map<FaAction,FaSubinfo[]> otherSubs = other.getSubinfos();
      if( otherSubs==null ) continue;

      // loop over all actions in the other's subinfo
      Iterator<FaAction> otherActions = otherSubs.keySet().iterator();
      while( otherActions.hasNext() ) {
	FaAction a = otherActions.next();
	FaSubinfo[] ary = otherSubs.get(a);

	// loop over all subgraph markers and merge them in
	for(int i=0; i<ary.length; i++) {
	  subinfos = mergeSub(subinfos, a, ary[i]);
	}
      }
    }
    return subinfos;
  }
  /**********************************************************************/
  @Override
  public CharTrans<AbstractFaState> getTrans() {
    return trans;
  }
  public void setTrans(CharTrans<AbstractFaState> trans) {
    this.trans = trans;
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
  public boolean isImportant() {
    return getTrans()!=null || getAction()!=null || subinfos!=null;
  }
   @Override
  public AbstractFaState[] getEps() {
     return eps;
   }
   @Override
  public void setEps(AbstractFaState[] eps) {
     this.eps = eps;
   }

   @Override
   public void addEps(AbstractFaState state) {
     tmp[0] = state;
     addEps(tmp);
   }
   @Override
   public void addEps(AbstractFaState[] others) {
     if( others==null ) return;
     if( eps==null ) {
       eps = new AbstractFaState[others.length];
       System.arraycopy(others, 0, eps, 0, others.length);
       return;
     }
     AbstractFaState[] tmp = new AbstractFaState[eps.length+others.length];
     System.arraycopy(eps, 0, tmp, 0, eps.length);
     System.arraycopy(others, 0, tmp, eps.length, others.length);
     eps = tmp;
   }

   @Override
  public AbstractFaState follow(char ch) {
     CharTrans<AbstractFaState> trans = getTrans();
     if( trans==null ) return null;
     AbstractFaState state = trans.get(ch);
     return state;
   }
  /**********************************************************************/
  /**
   * implements an {@link java.util.Iterator} over all states reachable
   * from this state. It iterates first over the character transitions
   * and then over the epsilon transitions.
   * <p>The {@link java.util.Iterator#remove() remove()} operation is not
   * implemented.</p>
   *
   * <p>Use {@link FaState#getChildIterator() getChildIterator()} of
   * an {@link FaState} object to create a <code>ChildIterator</code>
   * for that state.</p>a
   */
  public class ChildIterator implements Iterator<AbstractFaState> {
    private int trans_i = 0;
    private int trans_L = 0;
    private int eps_i = 0;
    private int eps_L = 0;

    ChildIterator(IterType iType) {
      if (iType==IterType.ALL || iType==IterType.EPSILON) {
        AbstractFaState[] eps = getEps();
        if( eps!=null ) eps_L = eps.length;
      }
      if (iType==IterType.ALL || iType==IterType.CHAR) {
        CharTrans<AbstractFaState> t = getTrans();
        if( t!=null ) trans_L = t.size();
      }
    }

    @Override
    public boolean hasNext() {
      return (eps_i<eps_L) || (trans_i<trans_L);
    }

    @Override
    public AbstractFaState next() {
      if( trans_i<trans_L ) {
        return getTrans().getAt(trans_i++);
      }
      if( eps_i<eps_L ) return getEps()[eps_i++];
      throw  new java.util.NoSuchElementException();
    }
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  /**********************************************************************/
  @Override
  public Iterator<AbstractFaState> getChildIterator(IterType iType) {
    return new ChildIterator(iType);
  }
  /********************************************************************/
}
