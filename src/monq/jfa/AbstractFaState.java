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
 *
 * @author &copy; 2005 Harald Kirsch
 */

abstract class AbstractFaState 
  implements FaState, Serializable
{
  // A state can be part of several subgraphs. Subgraphs are
  // identified by a pair (FaAction, FaSubinfo.id). The FaAction is
  // used in this map as a key. The values will be of type
  // FaSubinfo[], sorted according to FaSubinfo.id (see
  // FaSubinfo.compareTo()).
  private Map<FaAction,FaSubinfo[]> subinfos = null;	// keys are FaAction objects

  public  Map getSubinfos() {
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
  void mergeSub(FaAction a, FaSubinfo sfi) {
    if( subinfos==null ) subinfos = new HashMap<FaAction,FaSubinfo[]>();
    FaSubinfo[] ary;

    // If there is nothing yet for a, simply enter what we have as a
    // new entry.
    Object o = subinfos.get(a);
    if( o==null ) {
      //System.err.println("merging null with "+sfi+" for "+this);
      ary = new FaSubinfo[1];
      ary[0] = new FaSubinfo(sfi);
      subinfos.put(a, ary);
      return;
    }

    // Something is already there for a, amend it.
    ary = (FaSubinfo[])o;

    //System.err.println("merging "+sfi+" into "+this+":");
    //for(int kk=0; kk<ary.length; kk++) System.err.print(" "+ary[kk]);
    //System.err.println();

    // see if the subgraph denoted by sfi is already present
    int pos = java.util.Arrays.binarySearch(ary, sfi);

    // If sfi already present, just merge sfi in
    if( pos>=0 ) {
      ary[pos].merge(sfi);
      return;
    }

    // Since sfi denotes a new subgraph, we have to enlarge ary.
    pos = -(pos+1);
    FaSubinfo[] tmp = new FaSubinfo[ary.length+1];
    System.arraycopy(ary, 0, tmp, 0, pos);
    System.arraycopy(ary, pos, tmp, pos+1, ary.length-pos);
    ary = tmp;
    ary[pos] = new FaSubinfo(sfi);
    subinfos.put(a, ary);
  }
  public void addUnassignedSub(FaSubinfo sfi) {
    mergeSub(null, sfi);
  }

  /**
   * <p>Assigns subgraph information currently assigned to the
   * <code>from</code> action to the given <code>to</code>
   * action. Under most circumstances <code>from==null</code> which
   * means the subgraph was not yet really assigned.</p>
   */
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
//   String getSubs() {
//     if( subinfos==null ) return "null";
//     StringBuffer b = new StringBuffer();
//     Iterator it = subinfos.keySet().iterator();
//     while( it.hasNext() ) {
//       Object o = it.next();
//       FaSubinfo[] ary = (FaSubinfo[])subinfos.get(o);
//       for(int i=0; i<ary.length; i++) {
// 	b.append("(").append(o).append(',')
// 	  .append(ary[i]).append(')');
//       }
//     }
//       return b.toString();
//   }
  /**
   * called during compilation where this is a new state of the Dfa
   * and the given nfaStates is the set of states of the Nfa that
   * represent this. All subgraphs referenced are kept. For a
   * subgraph uniquely identified by a Pair (action, FaSubInfo.id),
   * the node types, i.e. start, inner or stop are merged. In the
   * extreme case, this state may be a start/inner/stop node for a
   * certain subgraph.
   */
  public void mergeSubinfos(Set nfaStates) {
    // loop over all given states
    Iterator states = nfaStates.iterator();
    while( states.hasNext() ) {
      FaState other = (FaState)(states.next());
      Map otherSubs = other.getSubinfos();
      if( otherSubs==null ) continue;

      // loop over all actions in the other's subinfo
      Iterator otherActions = otherSubs.keySet().iterator();
      while( otherActions.hasNext() ) {
	FaAction a = (FaAction)(otherActions.next());
	FaSubinfo[] ary = (FaSubinfo[])otherSubs.get(a);

	// loop over all subgraph markers and merge them in
	for(int i=0; i<ary.length; i++) mergeSub(a, ary[i]);
      }
    }
  }
  /**********************************************************************/
  
  public CharTrans getTrans() {return null;}
  public void setTrans(CharTrans trans) {
    throw new UnsupportedOperationException(getClass().getName());
  }
  public FaAction getAction() {return null;}
  public void clearAction() {;}
  public boolean isImportant() {
    return getTrans()!=null || getAction()!=null || subinfos!=null;
  }
  //public boolean isStop() {return getAction()!=null;}
  public FaState[] getEps() {return null;}

  public void addEps(FaState other) {
    throw new UnsupportedOperationException(getClass().getName());
  }
  public void addEps(FaState[] others) {
    throw new UnsupportedOperationException(getClass().getName());
  }
  public void setEps(FaState[] others) {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public FaState follow(char ch) {
    CharTrans trans = getTrans();
    //System.out.println("follow:"+ch+" in "+trans+"-->"+trans.get(ch));
    if( trans==null ) return null;
    Object o = trans.get(ch);
    return (FaState)o;
  }

  /**********************************************************************/
  /**
   * implements an {@link java.util.Iterator} over all states reachable
   * from this state. It iterates first over the character transitions
   * and then over the epsilon transitions.
   * <p>The {@link java.util.Iterator#remove() remove()} operation is
   * implemented.</p> 
   *
   * <p>Use {@link FaState#getChildIterator() getChildIterator()} of
   * an {@link FaState} object to create a <code>ChildIterator</code>
   * for that state.</p>
   */
  public class ChildIterator implements Iterator {
    private int trans_i = 0;
    private int trans_L = 0;
    private int eps_i = 0;
    private int eps_L = 0;
      
    ChildIterator() {
      FaState[] eps = getEps();
      if( eps!=null ) eps_L = eps.length;
      CharTrans t = getTrans();
      if( t!=null ) trans_L = t.size();
    }

    public boolean hasNext() {
      return (eps_i<eps_L) || (trans_i<trans_L);
    }

    public Object next() {
      if( trans_i<trans_L ) return getTrans().getAt(trans_i++);
      if( eps_i<eps_L ) return getEps()[eps_i++];
      throw  new java.util.NoSuchElementException();
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  /**********************************************************************/
  public Iterator getChildIterator() {
    return new ChildIterator();
  }
  /********************************************************************/
  public static FaState 
    createDfaState(FaAction a, boolean needEps) 
  {
    if( a!=null ) {
      // for stop states, there is anyway an epsilon
      return new DfaStopState(a);
    } else {
      if( needEps ) return new NfaState();
      else return new DfaState();
    }
  }
  /********************************************************************/
  /**
   * is an {@link FaState} which has only epsilon transitions. Other
   * subclasses of <code>AbstractFaState</code> which need epsilon
   * transitions inherit from this one.
   *
   * <p><b>FIX ME:</b>I don't really bother to check in the
   * <code>addEps</code> methods whether I add a transition a 2nd
   * time. Normally this does not happen in Thompson's
   * construction. And if it does, it is no harm anyway.</p>
   */
  static class EpsState extends AbstractFaState {
    private FaState[] eps=null;

    public FaState[] getEps() {return eps;}
    public void setEps(FaState[] eps) {this.eps = eps;}

    public void addEps(FaState other) {
      if( eps==null ) {
	eps = new FaState[1];
	eps[0] = other;
	return;
      }
      FaState[] tmp = new FaState[eps.length+1];
      System.arraycopy(eps, 0, tmp, 0, eps.length);
      eps = tmp;
      eps[eps.length-1] = other;
    }
    
    public void addEps(FaState[] others) {
      //throw new UnsupportedOperationException(getClass().getName());
      if( others==null ) return;
      if( eps==null ) {
	eps = new FaState[others.length];
	System.arraycopy(others, 0, eps, 0, others.length);
	return;
      }
      FaState[] tmp = new FaState[eps.length+others.length];
      System.arraycopy(eps, 0, tmp, 0, eps.length);
      System.arraycopy(others, 0, tmp, eps.length, others.length);
      eps = tmp;
    }
  }

  /********************************************************************/
  /**
   * is an {@link FaState} which has only explicit character
   * transitions. 
   */
  static class DfaState extends AbstractFaState {
    CharTrans trans = null;

    //public DfaState(CharTrans trans) { this.trans = trans; }
    public CharTrans getTrans() {return trans;}
    public void setTrans(CharTrans t) {this.trans = t;}
  }
  /********************************************************************/
  /**
   * implements a state which can store character transitions, an
   * action and, a bit surprisingly, epsilon transitions. The reason
   * for the latter is, that every FA shall always be usable in
   * regexp-operations. Most of these operations are all implemented
   * by means of epsilon transitions which are added in particular to
   * stop states.
   */
  static class DfaStopState extends EpsState {
    CharTrans trans = null;
    FaAction action = null;

    public DfaStopState(FaAction action) {
      this.trans = null;
      this.action = action;
    }
    public CharTrans getTrans() {return trans;}
    public void setTrans(CharTrans t) {this.trans = t;}
    public void clearAction() {action = null;}
    public FaAction getAction() {return action;}
  }
  /**
   * is an {@link FaState} which has epsilon transitions and can store
   * an action.
   */
  static class EpsStopState extends EpsState {
    FaAction action = null;
    public EpsStopState(FaAction a) { action = a; }
    public FaAction getAction() { return action; }
    public void clearAction() { action = null; }
  }
  /********************************************************************/
  /**
   * is an {@link FaState} with epsilon as well as character
   * transitions. 
   */
  static class NfaState extends EpsState {
    CharTrans trans = null;
    public CharTrans getTrans() {return trans;}
    public void setTrans(CharTrans t) {this.trans = t;}
  }
  /********************************************************************/
}
