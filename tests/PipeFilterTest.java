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

import monq.jfa.*;
import monq.programs.DictFilter;
import monq.net.*;
import monq.stuff.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.util.*;

/**
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class PipeFilterTest extends TestCase {

  private TcpServer tcp;
  private int filterPort;

  private static final class CopyService implements Service {
    private Exception e;
    private Map m;
    private InputStream in; 
    private OutputStream out;
    public CopyService(InputStream in, OutputStream out, Object o) {
      this.in = in;
      this.out = out;
      this.m = (Map)o;
    }
    public void run() {
      int b;
      // do all kinds of nasty things depending on the incoming
      // parameters in order to check proper handling of exceptions on
      // several sides
      if( m.containsKey("exitnow") ) return;

      try {
	PrintStream out = new PrintStream(this.out, true, "UTF-8");
	Object[] keys = m.keySet().toArray();
	Arrays.sort(keys);
	for(int i=0; i<keys.length; i++) {
	  Object key = keys[i];
	  Object value = m.get(key);
	  out.print(key);
	  out.print('=');
	  out.println(value);
	}
	while( -1!=(b=in.read()) ) out.write(b);
      } catch( IOException e ) {
	this.e = e;
	e.printStackTrace();
      }
    }
    public Exception getException() { return e; }
  }
  private static final class SFac implements ServiceFactory {
    public Service createService(InputStream in, OutputStream out, 
				 Object o) {
      return new CopyService(in, out, o);
    }
  }

  /**********************************************************************/
  public void setUp() throws Exception {
    FilterServiceFactory fac = new FilterServiceFactory(new SFac());
    java.net.ServerSocket s = new java.net.ServerSocket(0);

    tcp = new TcpServer(s, fac, 10);
    //tcp.setLogging(System.err);
    Thread t = new Thread(tcp);
    t.setDaemon(true);
    t.start();
    int i = 10;
    while( 0==(filterPort=s.getLocalPort()) && (i-=1)>0 ) {
      Thread.sleep(100);
    }
    if( filterPort<=0 ) {
      throw new Error("cannot determine local port of filter server");
    }
  }
  public void tearDown() {
    tcp.shutdown();
  }
  /**********************************************************************/
  public static String filter(DistPipeFilter dp,
			      PipelineRequest[] req, final String s) 
    throws IOException 
  {
    StringBuffer out = new StringBuffer();
    InputStream pipe = dp.open(req, new CharSequenceFeeder(s, "UTF-8"));
    InputStreamReader in = new InputStreamReader(pipe, "UTF-8");
    int ch;
    while( -1!=(ch=in.read()) ) out.append((char)ch);
    dp.close(pipe);
    return out.toString();
  }
  /**********************************************************************/
  public void test0() throws Exception {
    PipelineRequest req[] = new PipelineRequest[1];
    req[0] = new PipelineRequest("localhost", filterPort);
    DistPipeFilter dp = new DistPipeFilter(0, 3);
    dp.start();

    String s = filter(dp, req, "Harald Kirsch");
    //System.err.println(">>>"+s+"<<<");
    assertEquals("Harald Kirsch", s); 
    dp.shutdown();
  }
  /**********************************************************************/
  public void test1() throws Exception {
    PipelineRequest req[] = new PipelineRequest[1];
    req[0] = new PipelineRequest("localhost", filterPort);
    req[0].put("joki", "doki");
    req[0].put("blop", "bla;bla\nbla|bla");
    DistPipeFilter dp = new DistPipeFilter(0, 3);

    dp.start();
    dp.start();

    String s = filter(dp, req, "Harald Kirsch");
    //System.err.println(">>>"+s+"<<<");
    assertEquals("blop=bla;bla\nbla|bla\njoki=doki\nHarald Kirsch", s); 
    dp.shutdown();
    dp.shutdown();		// prove this does no harm
  }
  /**********************************************************************/
  public void test2() throws Exception {
    PipelineRequest req[] = new PipelineRequest[2];
    req[0] = new PipelineRequest("localhost", filterPort);
    req[1] = new PipelineRequest(req[0]);
    DistPipeFilter dp = new DistPipeFilter(0, 3);
    dp.start();

    String s = filter(dp, req, "Harald Kirsch");
    //System.err.println(">>>"+s+"<<<");
    assertEquals("Harald Kirsch", s); 
    dp.shutdown();
  }
  /**********************************************************************/
  public void test_FeederThrowsIAE() throws Exception {
    PipelineRequest req[] = new PipelineRequest[1];
    req[0] = new PipelineRequest("localhost", filterPort);
    DistPipeFilter dp = new DistPipeFilter(0, 3);
    dp.start();

    InputStream in = dp.open(req, new AbstractPipe() {
	public void pipe() { throw new IllegalArgumentException("x"); } });
    while( -1!=in.read() ) /* just run */;
    Throwable e = null;
    try {
      dp.close(in);
    } catch( Exception _e ) {
      e = _e.getCause();
    }
    assertTrue( e instanceof IllegalArgumentException);
    assertEquals("x", e.getMessage());
  }
  /**********************************************************************/
  public void test_FeederThrowsIOE() throws Exception {
    PipelineRequest req[] = new PipelineRequest[1];
    req[0] = new PipelineRequest("localhost", filterPort);
    DistPipeFilter dp = new DistPipeFilter(0, 3);
    dp.start();

    InputStream in = dp.open(req, new AbstractPipe() {
	public void pipe() throws IOException { 
	  throw new IOException("x"); 
	} 
      });
    while( -1!=in.read() ) /* just run */;
    Throwable e = null;
    try {
      dp.close(in);
    } catch( Exception _e ) {
      e = _e;
    }
    //System.err.println(">>>>"+e);
    assertTrue( e instanceof IOException );
    assertEquals("x", e.getMessage());
    
    // close again to get the reaction
    e = null;
    try {
      dp.close(in);
    } catch( IllegalArgumentException _e ) {
      e = _e;
    }
    assertTrue(e instanceof IllegalArgumentException);
    assertTrue(e.getMessage().startsWith("connection bound to given"));
  }
  /**********************************************************************/
  public void test_BrokenPipe() throws Exception {
    PipelineRequest req[] = new PipelineRequest[2];
    req[0] = new PipelineRequest("localhost", 12345);
    req[1] = new PipelineRequest("localhost", filterPort);

    DistPipeFilter dp = new DistPipeFilter(0, 3);
    dp.start();

    InputStream in = dp.open(req, new AbstractPipe() {
	public void pipe() throws IOException {} 
      });
    while( -1!=in.read() ) /* just run */;
    Throwable e = null;
    try {
      dp.close(in);
    } catch( Exception _e ) {
      e = _e;
    }
    //System.err.println(">>>>"+e);
    assertTrue( e instanceof IOException );
    assertTrue(e.getMessage().startsWith("The data was never asked for"));
  }
  /**********************************************************************/
  public void test_deadfilter() throws Exception {
    tcp.shutdown();
    PipelineRequest req[] = new PipelineRequest[1];
    req[0] = new PipelineRequest("localhost", filterPort);

    DistPipeFilter dp = new DistPipeFilter(0, 3);

    dp.start();

    Exception e = null;
    try {
      filter(dp, req, "Harald Kirsch");
    } catch( ServiceUnavailException ee ) {
      e = ee;
    }
    assertTrue( e instanceof ServiceUnavailException );
    assertTrue( e.getMessage().endsWith("localhost:"+filterPort) );
    dp.shutdown();
  }
  /**********************************************************************/
//   public void test_exitnow() throws Exception {
//     PipelineRequest req[] = new PipelineRequest[1];
//     req[0] = new PipelineRequest("localhost", filterPort);
//     req[0].put("exitnow", "blarg");
//     DistPipeFilter dp = new DistPipeFilter(0, 3, System.err);

//     dp.start();

//     String s = dp.filter(req, "Harald Kirsch");
//     System.err.println(">>>"+s+"<<<");
//     assertEquals("blop=bla;bla\nbla|bla\njoki=doki\nHarald Kirsch", s); 
//     dp.shutdown();
//   }
  /**********************************************************************/
  public void test_DistPipeFilterNotRunning() throws Exception {
    PipelineRequest req[] = new PipelineRequest[1];
    req[0] = new PipelineRequest("localhost", filterPort);
    DistPipeFilter dp = new DistPipeFilter(0, 3);
    Exception e = null;
    try {
      filter(dp, req, "x");
    } catch( IllegalStateException ee ) {
      e = ee;
    }
    assertTrue(e instanceof IllegalStateException);
    assertEquals("server not running", e.getMessage());
  }
  /**********************************************************************/
  public void test_WrongPortOnPipelineRequest() throws Exception {
    Exception e = null;
    int wrongPort = 4343333;
    try {
      new PipelineRequest("gaga", wrongPort);
    } catch( IllegalArgumentException ee ) {
      e = ee;
    }
    assertTrue(e instanceof IllegalArgumentException);
    assertTrue(e.getMessage().endsWith("is "+wrongPort));
  }
  /**********************************************************************/
  public void test_ForbiddenKeyOnPipelineRequest() throws Exception {
    Exception e = null;
    String wrongKey = ".blorb";
    PipelineRequest req = new PipelineRequest("gaga", 112);
    try {
      req.put(wrongKey, "asdf");
    } catch( IllegalArgumentException ee ) {
      e = ee;
    }
    assertTrue(e instanceof IllegalArgumentException);
    assertTrue(e.getMessage().startsWith("key `"+wrongKey+"'"));
  }
  /**********************************************************************/
  // to be able to run on the command line
  public static void main(String[] argv) {
    junit.textui.TestRunner.run(new TestSuite(PipeFilterTest.class));
  }

}
