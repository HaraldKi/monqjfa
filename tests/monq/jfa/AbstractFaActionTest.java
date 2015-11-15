package monq.jfa;

import static org.junit.Assert.*;

import org.junit.Test;

import monq.jfa.actions.Copy;

public class AbstractFaActionTest {

  @Test
  public void mergeWithOther() throws Exception {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    Copy sCopy1 = new Copy(-1);
    nfa.or("a0?", sCopy1);

    Copy sCopy2 = new Copy(1);
    nfa.or("b0?", sCopy2);

    NonAbstractFaAction specialCount = new NonAbstractFaAction();
    nfa.or("a|b", specialCount);

    Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
    //dfa.toDot(System.out);
    SubmatchData smd = new SubmatchData();
    StringBuilder out = new StringBuilder();
    FaAction a = dfa.match(new CharSequenceCharSource("aaa"), out, smd);
    assertEquals(specialCount, a);

    a = dfa.match(new CharSequenceCharSource("bbb"), out, smd);
    assertEquals(sCopy2, a);
    
    a = dfa.match(new CharSequenceCharSource("a0"), out, smd);
    assertEquals(sCopy1, a);

  }

  private static final class NonAbstractFaAction implements FaAction {
    @Override
    public void invoke(StringBuilder yytext, int start, DfaRun runner)
      throws CallbackException
    {
    }

    @Override
    public FaAction mergeWith(FaAction _other) {
      AbstractFaAction other = (AbstractFaAction)_other;
      if (other.priority<0) {
        return this;
      }
      return other;
    }
    @Override
    public String toString() {
      return "NonAbstract";
    }
  }
}
