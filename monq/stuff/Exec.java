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
import monq.net.Service;

/**
 * <p>encapsulates a <code>java.lang.Process</code> object, feeds it with
 * input and collects the output.</p>
 *
 * <p>Example use:<pre>
 *    Process p = new Process("your command here");
 *    Exec exec = new Exec(p);
 *    if( !exec.done() ) {
 *       // do something with exec.getErrorText()
 *       // check p.exitValue() if the above is empty
 *    }</pre>
 * </p>
 *
 * @author &copy; 2004 Harald Kirsch
 * @version $Revision: 1.5 $, $Date: 2005-11-17 09:34:47 $
 */
public class Exec implements Service {

  private InputStream stdin;
  private Process p;
  private OutputStream stdout;
  private OutputStream stderr;
  private ByteArrayOutputStream out = null;
  private ByteArrayOutputStream err = null;
  private boolean finished = false;

  private Exception exception;
  
  // for decent exception message only.
  String[] ioNames = {"stdin", "stdout", "stderr"};

  /**********************************************************************/
  /**
   * <p>creates an execution environment which feeds the given process
   * input, collects its output and registers the exit code. You have
   * to call either {@link #run} or {@link #done} explicitly to get
   * things going.</p> 
   *
   * @param p a running process
   * @param in is the data source to feed into the standard input of
   * <code>p</code>. May be <code>null</code> in which case the standard
   * input of <code>p</code> is immediately closed.
   * @param output is the output sink for the standard output of
   * <code>p</code>. It may be <code>null</code> in which case the
   * output is collected internally and made available via {@link
   * #getOutputText}. 
   * @param error is the sink for the standard error output of
   * <code>p</code>. If it is <code>null</code>, it is collected and
   * made available via {@link #getErrorText}.
   */
  public Exec(Process p, InputStream in, 
	      OutputStream output, OutputStream error) {
    this.p = p;
    this.stdin = in;
    if( output==null ) {
      this.out = new ByteArrayOutputStream();
      this.stdout = this.out;
    } else {
      this.stdout = output;
    }

    if( error==null ) {
      this.err = new ByteArrayOutputStream();
      this.stderr = this.err;
    } else {
      this.stderr = error;
    }
  }
  /**********************************************************************/
  /**
   * creates an execution environment for the running process
   * <code>p</code> which immediately closes its standard input and
   * collects standard output and standard error output
   * internally. The latter two can then be retrieved with {@link
   * #getOutputText} and it is <code>null</code>, it is collected and
   * made available via {@link #getErrorText}.</p>
   *
   * <p>This is equivalent to <code>Exec(p, null, null, null)</code>.</p>
   */
  public Exec(Process p) {
    this(p, null, null, null);
  }
  /**********************************************************************/
  /**
   * <p>asynchronuously tests if the process finished in the meantime.</p>
   */
  public synchronized boolean finished() {
    return finished;
  }
  /**********************************************************************/
  /**
   * <p>returns <code>true</code> if the process execution went
   * well. The return value will be <code>false</code> if either</p>
   * <ul>
   * <li>the exit value was non-zero,</li>
   * <li>an exception was thrown by one of the threads piping to from
   * and to the subprocess (see {@link #getException()}),</li>
   * <li>no output sink was set up in the constructor for the error
   * output stream of the process and some output was generated.</li>
   * </ul>
   *
   * <p><span style="color:green">Note:</span>If an error output sink
   * was explicitely given in the constructor, it is not watched to
   * see if any data is produced. In this case <code>ok()</code>
   * merely checks the exit value of the process and exceptions.
   * </p>
   *
   * @throws IllegalStateException if the process has not finished
   * yet.
   */
  public boolean ok() {
    if( !finished() ) 
      throw new IllegalStateException("process not finished");
    return 
      p.exitValue()==0 
      && (err==null || err.size()==0) 
      && exception==null;
  }
  /**********************************************************************/
  /**
   * starts feeding the process by calling {@link #run}, waits for it
   * to finish and then returns {@link #ok}.
   */
  public boolean done() {
    run();
    return ok();
  }
  /**********************************************************************/
  /**
   * <p>returns whatever <code>Exception</code> happened in one of the
   * {@link Pipe} threads used to copy data to the process or read
   * output or error from the process. Since there are up to three
   * data streams connected to the process in parallel threads, it can
   * not be guaranteed that the first one failing will be
   * reported. Rather they are prioritized in the order stdin, stdout,
   * stderr.</p>
   */
  public synchronized Exception getException() {
    if( !finished() ) 
      throw new IllegalStateException("process not finished");
    return exception;
  }
  /**********************************************************************/
  /**
   * <p>if no explicit output sink was specified in the constructor, this
   * method returns the output collected from the process.</p>
   *
   *  @throws IllegalStateException if the process has not finished
   * yet.
   */
  public String getErrorText() {
    if( !finished() ) 
      throw new IllegalStateException("process not finished");
    return err==null ? null : err.toString();
  }
  /**********************************************************************/
  /**
   * <p>if no explicit error output sink was specified in the
   * constructor, this method returns the error text collected from
   * the process.</p>
   *
   * @throws IllegalStateException if the process has not finished
   * yet.
   */
  public String getOutputText() {
    if( !finished() ) 
      throw new IllegalStateException("process not finished");
    return out==null ? null : out.toString();
  }
  /**********************************************************************/
  /**
   * <p>feeds the process with input and collects the output according to
   * the information given in the constructor. The method will return
   * when the process has finished and all data produced by it was
   * read. In addition, <code>run()</code> will kill the process and
   * return, if the thread it is running in is interrupted.</p>
   */
  public void run() {

    Thread[] t = new Thread[3];
    Pipe[] pipe = new Pipe[3];

    // set up the input source and let it go
    if( stdin!=null ) {
      pipe[0] = new Pipe(stdin, p.getOutputStream(), true, 4096);
      t[0] = new Thread(pipe[0]);
      t[0].start();
    } else {
      try {
	p.getOutputStream().close();
      } catch( IOException e ) {
	throw new Error("how can this happen", e);
      }
    }
    
    pipe[1] = new Pipe(p.getInputStream(), stdout, true, 4096);
    t[1] = new Thread(pipe[1]);
    t[1].start();
    pipe[2] = new Pipe(p.getErrorStream(), stderr, true, 4096);
    t[2] = new Thread(pipe[2]);
    t[2].start();

    // wait for each of the threads to finish
    for(int i=0; i<t.length; i++) {
      while( t[i]!=null && t[i].isAlive() ) {
	try {
	  t[i].join();
	} catch( InterruptedException e ) {
	  // this tells us to shut down things as orderly as possible
	  shutdown(t);
	}
      }
    }

    // Wait for the process to finish. This should actually have
    // happened already, but need not always be the case.

    while( true ) {
      try {
	p.waitFor();
	break;
      } catch( InterruptedException e ) {
	p.destroy();
      }
    }

    synchronized(this) { 
      finished = true; 
      Exception e;
      for(int i=0; i<t.length; i++) {
	if( (e=pipe[0].getException())==null ) continue;
	exception = new IOException(ioNames[i]+" threw exception");
	exception.initCause(e);
	break;
      }
    }
  }
  /**********************************************************************/
  private void shutdown(Thread[] t) {
    for(int i=0; i<t.length; i++) {
      if( t[i]!=null ) t[i].interrupt();
    }
    p.destroy();
  }
  /**********************************************************************/
}
