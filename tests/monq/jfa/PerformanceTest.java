package monq.jfa;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.junit.Before;
import org.junit.Test;

import monq.jfa.actions.MapProvider;

/**
 * contains one test to verify that {@link Nfa#setMemoryForSpeedTradeFactor}
 * has indeed an effect of at least 20%.
 */
public class PerformanceTest {
  private double speedupFactor;
  
  private CharStatistics[] charStats;
  @Before
  public void setup() throws Exception {
      charStats = readBiStats("resources/char-2stat-english.txt.gz");
      if (null!=System.getProperty("cobertura.active")) {
        speedupFactor = 1.01;
      } else {
        speedupFactor = 1.2;
      }
    }

    @Test
    public void performanceTest() throws Exception {
      StringBuilder text = createText(20_000_000);
      CountWords cw = new CountWords();
      Nfa nfa = createNfa(cw);

      for (int i=0; i<4; i++) {
        Timing slow = runFilter(nfa, text, 1.0f);
        Map<String,Count> slowMap = cw.reset();
        
        Timing fast = runFilter(nfa, text, 100000.0f);
        Map<String,Count> fastMap = cw.reset();

        System.out.printf("slow and fast: %s>%s, speedup=%.1f%n", slow, fast,
                          slow.speedUpOver(fast));
        //System.out.println(slowMap);
        assertTrue("slow and fast: "+slow+">"+fast, 
                   slow.dtSeconds()>fast.dtSeconds());
        assertEquals(slowMap, fastMap);
        assertTrue(slow.speedUpOver(fast)>speedupFactor);
      }
    }

    @Test
    public void compareToRegexTest() throws Exception {
      // we have this test here, because everything is there to create random
      // text.
      final Map<Object,Object> counts = new HashMap<>();
      MapProvider mp = new MapProvider() {
        @Override
        public Map<Object,Object> getMap() {
          return counts;
        }        
      };
      String[] rexes = {"<[^>]*>", "[a-zA-Z]+", "[0-9]+", "[ \r\n\t]+"};
      String[] names = {"xml", "alpha", "num", "space"};
        
      Nfa nfa = new Nfa(Nfa.NOTHING);
      for (int i=0; i<rexes.length; i++) {
        nfa.or(rexes[i], new CountAction(names[i]));
      }
          
      StringBuilder text = createText(200_000);
      DfaRun r = new DfaRun(nfa.compile(DfaRun.UNMATCHED_DROP));
      r.setIn(new CharSequenceCharSource(text));
      r.clientData = mp;
      r.filter();
      System.out.println(counts);
      Map<String,Count> javaResult = countMatches(rexes, names, text);
      System.out.println(javaResult);
      assertEquals(javaResult, counts);
    }
    
    private static Map<String,Count>
    countMatches(String[] rexes, String[] names, StringBuilder text) {
      Map<String,Count> result = new HashMap<>();
      for (String name : names) {
        result.put(name, new Count());
      }
      StringBuilder re = new StringBuilder();
      re.append('(');
      for (String s : rexes) {
        re.append(s).append(")|(");
      }
      re.setLength(re.length()-2);
      Pattern p = Pattern.compile(re.toString()); 
      Matcher m = p.matcher(text);

      int start = 0;
      while (m.find(start)) {
        for(int i=0; i<rexes.length; i++) {
          if (m.group(i+1)!=null) {
            result.get(names[i]).count+=1;
          }
        }
        start = m.end();
      }
      return result;
    }
    
    private static Timing runFilter(Nfa nfa, CharSequence text, float tradeOff)
        throws CompileDfaException, IOException
    {
      nfa.setMemoryForSpeedTradeFactor(tradeOff);
      Intervals.resetStats();
      Dfa dfa = nfa.compile(DfaRun.UNMATCHED_DROP);
      System.out.println(Intervals.logStats());
      
      DfaRun r = new DfaRun(dfa);
      r.setIn(new CharSequenceCharSource(text));
      Timing t = new Timing();
      r.filter();
      t.stop();
      return t;
    }

    private Nfa createNfa(CountWords cw) throws Exception {
      String[] words = new String(createText(700)).split("[ ]+");
      Nfa nfa = new Nfa();
      for (String word : words) {
        if (word.length()<4) {
          continue;
        }
        //System.out.println("word=`"+word+"'");
        nfa.or(nfa.escape(word), cw);
      }
      return nfa;
    }

    /**
     * create a random text based on the charStats values.
     */
    private StringBuilder createText(int nChars) {
      StringBuilder text = new StringBuilder(nChars);
      char lastCh = 'e';   //arbitrary first character
      for (int i=0; i<nChars; i++) {
        CharStatistics cs = charStats[lastCh];
        char nextCh = cs.getRandomChar();
        text.append(nextCh);
        lastCh = nextCh;
      }
      return text;
    }
    /*+******************************************************************/
  /**
   * reads a file with lines like
   * 
   * <pre>
   * et 101 116 448651
   * </pre>
   * 
   * where the first entry is a character pair, the second and third are the
   * UNICODE code points of these characters and the forth item is the number
   * of times this pair was found in some text.
   * 
   * @param fname
   * @return each element of the result array, indexed by characters, returns
   *         the statistics for the character following the index character.
   * @throws IOException
   */
    private static CharStatistics[] readBiStats(String fname) throws IOException {
      CharStatistics[] result = new CharStatistics[Character.MAX_VALUE+1];

      try (InputStream in = new BufferedInputStream(new FileInputStream(fname));
          GZIPInputStream gin = new GZIPInputStream(in);
          Reader r = new InputStreamReader(gin, Charset.forName("UTF-8"));
          BufferedReader br = new BufferedReader(r)) {
        String line = null;

        while ( null!=(line = br.readLine())) {
          String[] parts = line.split("[ ]+");
          int l = parts.length;
          int count = Integer.parseInt(parts[l-1]);
          int charFrom = Integer.parseInt(parts[l-3]);
          int charTo = Integer.parseInt(parts[l-2]);
          if (result[charFrom]==null) {
            result[charFrom] = new CharStatistics();
          }
          CharStatistics current = result[charFrom];
          current.add((char)charTo, current.getMaxFrq()+count);
        }
      }
      return result;
    }
    /*+******************************************************************/
    private static final class Timing {
      private long start = System.currentTimeMillis();
      private long stop = -1;
      public void stop() {
        stop = System.currentTimeMillis();
      }
      public double speedUpOver(Timing other) {
        return (dtSeconds()/other.dtSeconds());
      }
      public double dtSeconds() {
        return (stop-start) / 1000.0;
      }
      @Override
      public String toString() {
        return String.format(Locale.ROOT, "%.2fs", dtSeconds());
      }      
    }
    /*+******************************************************************/
    private static final class CountAction extends AbstractFaAction {
      private final String name;
      public CountAction(String name) {
        this.name = name;
      }
      @Override
      public void invoke(StringBuilder yytext, int start, DfaRun runner)
        throws CallbackException
      {
        MapProvider mp = (MapProvider)runner.clientData;
        Map<Object,Object> m = mp.getMap();
        Count c = (Count)(m.get(name));
        if (c==null) {
          m.put(name, c=new Count());
        }
        c.count += 1;        
      }      
    }
    /*+******************************************************************/
    private static final class CountWords extends AbstractFaAction {
      private Map<String,Count> counts = new HashMap<>();

      @Override
      public void invoke(StringBuilder yytext, int start, DfaRun runner)
        throws CallbackException
      {
        String word = yytext.substring(start);
        Count c = counts.get(word);
        if (c==null) {
          c = new Count();
          counts.put(word, c);
        }
        c.count += 1;
      }
      public Map<String,Count> reset() {
        Map<String,Count> tmp = counts;
        counts = new HashMap<>();
        return tmp;
      }
    }
    /*+******************************************************************/
    /** mutable int object */
    private static final class Count {
      public int count;
      @Override
      public boolean equals(Object other) {
        if (other==null) {
          return false;
        }
        if (!(other instanceof Count)) {
          return false;
        }
        return count==((Count)other).count;
      }
      @Override
      public int hashCode() {
        return count;
      }
      @Override
      public String toString() {
        return Integer.toString(count);
      }
    }
    /*+******************************************************************/
    private static final class CharStatistics {
      private final static Random r = new Random(15121962);

      private ArrayList<Long> accumulatedFreq = new ArrayList<>();
      private ArrayList<Character> chars = new ArrayList<Character>();

      public void add(char ch, long accFrq) {
        accumulatedFreq.add(accFrq);
        chars.add(ch);
      }
      public long getMaxFrq() {
        if (accumulatedFreq.isEmpty()) {
          return 0;
        }
        return accumulatedFreq.get(accumulatedFreq.size()-1);
      }

    /**
     * computes a random character picked according to the frequency
     * statistics stored.
     */
      public char getRandomChar() {
        double dfrq =
            r.nextDouble()*accumulatedFreq.get(accumulatedFreq.size()-1);
        long frq= (long)dfrq;

        int idx = Collections.binarySearch(accumulatedFreq, frq);
        if (idx<0) {
          idx = -(idx+1);
        }
        return chars.get(idx);
      }
    }
}
