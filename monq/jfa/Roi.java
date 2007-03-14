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

import monq.jfa.actions.SwitchDfa;

/**
 * <p>is a data container that describes the bits necessary to implement
 * a ROI (region of interest) with a finite automaton. Currently it is
 * used together with {@link JyFA} but its use may be extended to
 * {@link monq.programs.Grep} and {@link monq.jfa.ctx.Context} in the
 * future.</p>
 *
 * <p>Set up the <code>Roi</code> with the constructor and the
 * setters. Then call {@link #wrap(Nfa,Nfa)}.</p>
 */
public class Roi {
  private final String startRe;
  private final String endRe;

  private FaAction startAction = null;
  private FaAction endAction = null;

  private int startPrio = 0;
  private int endPrio = 0;

  private DfaRun.FailedMatchBehaviour startFmb = DfaRun.UNMATCHED_COPY;
  private DfaRun.FailedMatchBehaviour endFmb = DfaRun.UNMATCHED_COPY;

  private FaAction eofAction = null;

  /**
   * <p>defines the ROI to be a stretch of text starting with a match of
   * <code>startRe</code> and extending to the first match of
   * <code>stopRe</code>. Note that <code>stopRe</code> usually will
   * compete with regular expressions to be applied within the
   * ROI. Consequently it should be as specific as possible. In cases
   * where matches of <code>stopRe</code> also match other regular
   * expressions applied within the ROI, {@link #setEndPrio} can
   * be used for disambiguation.</p>
   */
  public Roi(String startRe, String endRe) {
    this.startRe = startRe;
    this.endRe = endRe;
  }

  /**
   * Get the StartAction value.
   * @return the StartAction value.
   */
  public FaAction getStartAction() {
    return startAction;
  }

  /**
   * <p>sets the action to run at the start of the ROI. This is in
   * particular needed to modify the match starting the ROI. A
   * <code>null</code> is equivalent to copying the match to the
   * output. </p>
   * @return this
   */
  public Roi setStartAction(FaAction newStartAction) {
    this.startAction = newStartAction;
    return this;
  }

  /**
   * Get the EndAction value.
   * @return the EndAction value.
   */
  public FaAction getEndAction() {
    return endAction;
  }

  /**
   * <p>sets the action to run at the end of the ROI. This is in
   * particular needed to modify the match terminatin the ROI. A
   * <code>null</code> is equivalent to copying the match to the
   * output.</p>
   * @return this
   */
  public Roi setEndAction(FaAction newEndAction) {
    this.endAction = newEndAction;
    return this;
  }

  /**
   * Get the StartFmb value.
   * @return the StartFmb value.
   */
  public DfaRun.FailedMatchBehaviour getStartFmb() {
    return startFmb;
  }

  /**
   * <p>defines how non-matching input shall be handled within the
   * ROI. The default is {@link DfaRun#UNMATCHED_COPY}.</p>
   * @return this;
   */
  public Roi setStartFmb(DfaRun.FailedMatchBehaviour newStartFmb) {
    this.startFmb = newStartFmb;
    return this;
  }

  /**
   * Get the EndFmb value.
   * @return the EndFmb value.
   */
  public DfaRun.FailedMatchBehaviour getEndFmb() {
    return endFmb;
  }

  /**
   * <p>defines how non-matching input shall be handled outside the
   * ROI. The default is {@link DfaRun#UNMATCHED_COPY}.</p>
   * @return this;
   */
  public Roi setEndFmb(DfaRun.FailedMatchBehaviour newEndFmb) {
    this.endFmb = newEndFmb;
    return this;
  }

  /**
   * Get the StartPrio value.
   * @return the StartPrio value.
   */
  public int getStartPrio() {
    return startPrio;
  }

  /**
   * <p>Sets the priority for the action to be called at the start of
   * the ROI. The action called first is <b>not</b> the one
   * specified with {@link #setStartAction setStartAction()}, but
   * rather one to enter the ROI. That action's priority for
   * disambiguation can be set here.</p>
   * @return this
   */
  public Roi setStartPrio(int newStartPrio) {
    this.startPrio = newStartPrio;
    return this;
  }

  /**
   * Get the EndPrio value.
   * @return the EndPrio value.
   */
  public int getEndPrio() {
    return endPrio;
  }

  /**
   * <p>Sets the priority for the action to be called at the start of
   * the ROI. The action called first is <b>not</b> the one
   * specified with {@link #setStartAction setStartAction()}, but
   * rather one to enter the ROI. That action's priority for
   * disambiguation can be set here.</p>
   * @return this
   */
  public Roi setEndPrio(int newEndPrio) {
    this.endPrio = newEndPrio;
    return this;
  }

  /**
   * <p>creates a {@link Dfa} that implements the ROI. First,the
   * regular expression to start the ROI is added to
   * <code>envelope</code> and the regular expression to end the ROI
   * is added to <code>work</code>. Then both <code>Nfa</code>s are
   * compiled and interlinked. Findally the <code>Dfa</code> compiled
   * from <code>envelope</code> is returned.
   */
  public Dfa wrap(Nfa envelope, Nfa worker) 
    throws ReSyntaxException, CompileDfaException 
  {
    SwitchDfa toWork = new SwitchDfa(startAction);
    toWork.setPriority(startPrio);
    SwitchDfa toEnv = new SwitchDfa(endAction);
    toEnv.setPriority(endPrio);

    envelope.or(startRe, toWork);
    worker.or(endRe, toEnv);
    toWork.setDfa(worker.compile(endFmb));

    Dfa dfa = envelope.compile(startFmb);
    toEnv.setDfa(dfa);
    return dfa;
  }
  /**
   * <p>creates a fresh and empty {@link Nfa} as the envelope and then
   * calls the {@link #wrap(Nfa,Nfa) 2 parameter method}.</p>
   */
  public Dfa wrap(Nfa worker)  
    throws ReSyntaxException, CompileDfaException 
  {
    return wrap(new Nfa(Nfa.NOTHING), worker);
  }

  /**
   * Get the EofAction value.
   * @return the EofAction value.
   */
  public FaAction getEofAction() {
    return eofAction;
  }

  /**
   * <p>set the action to be compiled into the {@link Dfa} wrapped
   * around the one working in the ROI. It is assumed that the end of
   * input will never occur within a ROI. Consequently, the
   * <code>eofAction</code> is only compiled into the automaton which
   * handles the text outside the ROI.</p>
   * @return this.
   */
  public Roi setEofAction(FaAction newEofAction) {
    this.eofAction = newEofAction;
    return this;
  }

}
