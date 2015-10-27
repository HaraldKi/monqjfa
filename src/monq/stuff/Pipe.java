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

/*****************************************************************/
/**
  <p>implements a threadable pipe. The idea of this class is to collect
  information from an input stream in an asynchronous fashion, or send
  output to a stream asynchronously.</p>

  <p>After setting up the <code>Pipe</code> with an input and an output
  stream, it is usually run with something along the lines of
  * <pre>
  * new Thread(pipe).start()
  * </pre>
  As long as input is available from the input stream, the thread will
  copy it to the output stream.
  
  <p>Typical use of this class would be to collect standard input and
  standard error of a subprocess.

  @author &copy; 1998,1999,2000,2003,2004,2005 Harald Kirsch
*****/
public class Pipe extends AbstractPipe {
  private byte[] buf;

  /*****************************************************************/
  /**
   * <p>constructs a <code>Pipe</code> with no input or output yet
   * specified. Use {@link #setIn setIn()} and {@link #setOut
   * setOut()} to specify them later.
   */
  public Pipe(int bsize) {
    this(null, null, false, bsize);
  }
  /*****************************************************************/
  /**
   * <p>creates a <code>Pipe</code> which connects an
   * <code>InputStream</code> with an <code>OutputStream</code>. It will
   * start copying data, as soon as the <code>run</code>-method of this
   * object is called, typically from a new thread. If the input
   * stream signals EOF, all pending output is flushed. If
   * <code>closeOnExit</code> is set, both streams are closed. Then
   * the <code>run()</code> method terminates.</p>
   * 
   * <p>In cases of an <code>IOException</code>, <code>run()</code>
   * records the exception to be retrieved by {@link #getException()},
   * then closes the streams subject to <code>closeOnExit</code> and
   * then terminates.</p>
   *
   * <p>To have individual control over closing only one of the
   * streams on exit, use {@link #setIn setIn()} and {@link #setOut
   * setOut()}.</p>
   *
   * @param bsize must be greater than 0
   */
  public Pipe(InputStream in, OutputStream out, 
	      boolean closeOnExit, int bsize) {
    setIn(in, closeOnExit);
    setOut(out, closeOnExit);
    if( bsize<1 ) 
      throw new IllegalArgumentException
	("bsize must be greater 0 but is "+bsize);
    this.buf = new byte[bsize];
  }
  /*****************************************************************/
  /**
    returns the current input stream the pipe is reading.
  *****/
  public InputStream getIn() { return this.in; }

  /**
    returns the current output stream the pipe is writing to.
  *****/
  public OutputStream getOut() { return this.out; }

  /********************************************************************/
  /**
   * <p>copies from the input stream of this pipe to the output
   * stream. If the input stream is <code>null</code> this method
   * returns immediately. If the output stream is <code>null</code>,
   * the input stream is drained.</p>
   */
  protected void pipe() throws IOException {
    if( in==null ) return;

    int l;
    while( -1!=(l=in.read(buf)) ) {
      if( out!=null ) out.write(buf, 0, l);
    }
  }
  /*****************************************************************/
}
