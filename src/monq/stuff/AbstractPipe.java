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
 * <p>a skeleton implementation of a combination of {@link Feeder}
 * and {@link Drainer}. Subclasses only need
 * to implement {@link #pipe}.</p>
 *
 * <p>This class implements the <code>run()</code> method required by
 * the <code>Feeder</code> and <code>Drainer</code> interfaces. It
 * calls <code>pipe()</code>, catches any exceptions thrown and closes
 * the provided streams as requested by the 2nd
 * parameters of {@link #setOut setOut()} and {@link #setIn setIn()}.</p>
 *
 * <p>Objects of this class may be reused within a <em>single</em>
 * thread as long as <code>setIn()</code> and/or <code>setOut()</code>
 * are called as necessary before each invocation of the
 * <code>run()</code> method.</p>
 *
 * <h3><em>Half</em> Subclasses</h3>
 *
 * <p>A subclass need not make use of both, input and output. If it
 * generates data, it can choose to only use the output stream
 * provided by {@link #setOut setOut()}, thereby actually being a
 * {@link Feeder} only. A pure data sink, i.e. a {@link Drainer},
 * results if only the provided input stream is used.</p>
 * 
 * <p>Of course such subclasses should then only be used as either
 * <code>Feeder</code> or <code>Drainer</code> such that the unused
 * stream is never set anyway.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public abstract class AbstractPipe implements Feeder, Drainer {
  private Exception e = null;

  private boolean closeOut;
  private boolean closeIn;

  /**
   * <p>output stream to which the {@link #pipe pipe()} method of
   * subclasses should write. Value as set by {@link #setOut
   * setOut()}. Sublcasses should not close this stream in
   * <code>pipe</code> as this is done, if required by {@link #run}.</p>
   */
  protected OutputStream out;

  /**
   * <p>input stream from which the {@link #pipe pipe()} method of
   * subclasses should read. Value as set by {@link #setIn
   * setIn()}. Sublcasses should not close this stream in
   * <code>pipe</code> as this is done, if required by {@link #run}.</p>
   */
  protected InputStream in;

  /**********************************************************************/
  /**
   * <p>is the method that does the work of reading and/or writing
   * data. It is the only
   * method that <b>must</b> be implemented by subclasses. It may
   * throw exceptions but it must not close the streams.</p>

   * <p>A subclass is not required to do both, read the input
   * specified by {@link #setIn setIn()} and write to the output as
   * specified by <code>{@link #setOut setOut()}</code>. By ignoring
   * either one, the subclass functions as either a pure {@link Feeder} or
   * a pure {@link Drainer}.</p>
   */
  protected abstract void pipe() throws IOException, InterruptedException;
  /**********************************************************************/
  public Exception getException() { return e; }
  
  /**********************************************************************/
  public void setOut(OutputStream out, boolean closeOnExit) {
    this.out = out;
    this.closeOut = closeOnExit;
  }
  /**********************************************************************/
  public void setIn(InputStream in, boolean closeOnExit) {
    this.in = in;
    this.closeIn = closeOnExit;
  }
  /**********************************************************************/

  /**
   * <p>calls {@link #pipe}, handles exceptions and closes input and
   * output streams where necessary and requested.</p>
   *
   * <p>This method <b>ignores</b> the exceptions {@link
   * java.io.InterruptedIOException} and {@link
   * java.lang.InterruptedException} when thrown or passed on by
   * <code>pipe()</code>as valid requests to stop operations
   * prematurely.</p>
   */
  public void run() {
    this.e = null;		// allow for reuse of subclass objects
    try {
      pipe();
    } catch( InterruptedIOException e ) {
      // we understand this as a valid request to quit
    } catch( InterruptedException e ) {
      // we understand this as a valid request to quit
    } catch( IOException e ) {
      this.e = e;
    }
    try {
      out.flush();
    } catch( IOException e ) {
      if( this.e==null ) this.e = e;
    }
    try {
      if(  out!=null && closeOut ) out.close();
    } catch( IOException e ) {
      if( this.e==null ) this.e = e;
    }
    try {
      if( in!=null && closeIn ) in.close();
    } catch( IOException e ) {
      if( this.e==null ) this.e = e;
    }
  }
  /**********************************************************************/
}
