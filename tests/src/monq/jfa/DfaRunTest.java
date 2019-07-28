package monq.jfa;
/*
 * additional tests not naturally covered by tests of Dfa and Nfa
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import monq.jfa.actions.Copy;

public class DfaRunTest {

  @Test
  public void deliverPushedBack() throws Exception {
    Nfa nfa = new Nfa("a", Copy.COPY);
    DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP));
    r.setIn(new CharSequenceCharSource("a"));
    StringBuilder sb = new StringBuilder();
    sb.append("123");
    r.pushBack(sb, 0);

    assertEquals('1', r.read());
    sb.append("xyz");
    r.pushBack(sb, 0);
    r.read(sb);
    assertEquals("xyz23a", sb.toString());
  }
}
