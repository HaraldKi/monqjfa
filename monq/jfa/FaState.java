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
import java.util.Map;
import java.util.Iterator;

/**
  describes the interface obeyed by states of deterministic and
  non-deterministic finite automata.
  *
  * @author &copy; 2003, 2004 Harald Kirsch
*****/
interface FaState {

  /**
    returns true, if the state has at least one outgoing transition on
    a character (not only epsilons), if it is a stop-state or if it is
    part of a subautomaton.
  *****/
  boolean isImportant();

  /**
    returns the states that can be reached by epsilon transitions. The
    result must be <code>null</code> if there are no epsilon
    transitions. Otherwise the <code>Set</code> returned contains at
    least one element.
  *****/
  FaState[] getEps();
  //Set getEps();

  /**
    is an optional operation <code>FaState</code>.
  *****/
  void setEps(FaState[] newEps);

  /**
   * returns the character transition table of this state.
   */
  CharTrans getTrans();
  /**
   * changes the character transition table of this state. The
   * paramter may be <code>null</code>. Some states don't have a
   * character transition table, in which case an
   * <code>UnsupportedOperationException</code> will be thrown.
   */
  void setTrans(CharTrans trans);
  FaAction getAction();
  void clearAction();
  
  /**
   * returns a {@link java.util.Iterator} which iterates over all
   * children of the state. This includes those reachable by character
   * transitions as well as those reachable by epsilon
   * transitions. This iterator <b>must</b> support the 
   * {@link java.util.Iterator#remove remove} operation.
   */
  Iterator getChildIterator();

  //boolean isStop();

  /**
   * <p>returns the state which can be reached from this state by
   * character <code>ch</code>. If the given character does not lead
   * to another state, <code>null</code> is returned.</p>
   */
  FaState follow(char ch);

  /**
   * adds an epsilon transition to the given state (optional
   * operation). 
   * @throws UnsupportedOperationException if the <code>addEps</code>
   * operation is not supported by this state.
   */
  void addEps(FaState other);
  void addEps(FaState[] others);

  /** 
   * mark this state part of the subautomaton <code>sfi</code>.
   *
   * @see #assignSub
   */
  void addUnassignedSub(FaSubinfo sfi);

  /** 
   * <p>If this state is part of a  subautomaton assigned to
   * <code>from</code> , relate it now to <code>to</code>. When
   * subgraph information is initialized, it is first "assigned" to
   * the <code>null</code> action which means it is not yet assigned
   * at all. Consequently <code>from==null</code> is used to first
   * assign this state to an action.</p>
   *
   * <p>While traversing huge FAs, many subautomata may be passed
   * through in parallel. Only when a stop state is finally
   * determined, the relevant subautomatons traversed can be singled
   * out by comparing their action with the reference action set
   * here.</p>
   */
  void reassignSub(FaAction from, FaAction to);

  /**
   * <p>during construction of a {@link Dfa} a state must inherit all
   * subautomata assignments from a set of nfa states. This method
   * performs the transfer of the assignments.</p>
   */
  void mergeSubinfos(Set nfaStates);

  /**
   * returns a <code>Map</code> from {@link FaAction} objects to a
   * <code>Set</code> of {@link FaSubinfo} objects. For every
   * <code>FaAction</code> the set denotes the subautomata this state
   * belongs to.
   */
  Map getSubinfos();

}
