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

package monq.stuff;

import java.io.*;

/**
 * <p>this class should not be necessary, but I did not find a better way
 * to get the GC release memory to the operating system.</p>
 *
 * <p>When using <code>java</code> with the option
 * <code>-XX:MaxHeapFreeRatio</code>, a successfull GC freeing huge
 * percentages of memory does not immediately result in deallocation
 * of memory. My experiments show that several (5&ndash;10) calls to
 * the GC are necessary before memory is really given back to the
 * operating system to comply with the given
 * <code>MaxHeapFreeRatio</code>.</p>
 *
 * <p>A typical use is</p>
 * <pre>  new Thread(new ConvinceGC(10)).start();</pre>
 * <p>which starts a new thread that calls the garbage collector 10
 * times and then exits.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class ConvinceGC implements Runnable {
  
  private int count;
  private PrintStream out = null;
  private Runtime rt = Runtime.getRuntime();
  /**
   * <p>get a thread which, after being started, calls the garbage
   * collector the given number of times with an interval of 1 second
   * in between.</p>
   * <p>You need to call the <code>start()</code> method to get things
   * going.</p>
   */
  public ConvinceGC(int count) {
    this.count = count;
  }
  /**********************************************************************/
  /**
   * <p>after every call to gc, report the amount of memory allocated,
   * used and the resulting percentage of free memory.</p>
   */
  public ConvinceGC setLogging(PrintStream out) {
    this.out = out;
    return this;
  }
  /**********************************************************************/
  /**
   * <p>is only public to satisfy the <code>Runnable</code> interface,
   * so rather wrap an object of this class in a <code>Thread</code>
   * and call its <code>start()</code> method.</p>
   */
  public void run() {
    while( count-->0 ) {
      rt.gc();
      if( out!=null ) {
	long free = rt.freeMemory();
	long total = rt.totalMemory();
	String pcent 
	  = new Double((double)free/(double)total*100.0).toString();
	pcent = pcent.substring(0,pcent.indexOf(".")+2);
	out.println("ConvinceGC: allocated="+total+
		    ", used="+(total-free)+
		    ", free="+free+" ("+pcent+")");
      }
      if( Thread.interrupted() ) break;
      try {
	Thread.sleep(1000);
      } catch( InterruptedException e ) {
	// Assume this asks us to quit working
	break;
      }
    }
    if( out!=null ) out.println("ConvinceGC exiting");
  }
  /**********************************************************************/
}
