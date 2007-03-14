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

import org.python.core.*; 
import org.python.util.PythonInterpreter; 

import monq.jfa.*;
import monq.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.regex.*;

/**
 * <p>sets up a Dfa by calling a Jython script. The
 * {@link #JyFA(Nfa,String) constructor} loads a Jython module
 * into an internal Jython interpreter and then retrieves information
 * from the module as follows.</p>
 *
 * <p><b><code>populate</code>:</b> The module must have a
 * function</p>
 *
 * <pre class="codex">def populate(nfa):
 *   nfa.or(...)
 *   ...</pre>
 *
 * <p>that sets up the {@link Nfa} passed as parameter. The method is
 * called exactly once to add regexp/action pairs to the
 * <code>Nfa</code>. As soon as <code>populate</code> returns, the
 * <code>Nfa</code> is compiled into a <code>Dfa</code>.</p>
 * 
 * <p><b><code>onFailedMatch</code>:</b> The module <em>may</em> have
 * the variable <code>onFailedMatch</code> holding a value of
 * type {@link monq.jfa.DfaRun.FailedMatchBehaviour}. It is retrieved
 * to be provided as a parameter to {@link Nfa#compile
 * Nfa.compile}. If the field does not exist, {@link
 * DfaRun#UNMATCHED_COPY} is assumed.</p>
 *
 * <p><b><code>eofAction</code>:</b> The module <em>may</em> have
 * the variable <code>eofAction</code> holding a value of
 * type {@link monq.jfa.FaAction}. It is retrieved
 * to be provided as a parameter to {@link Nfa#compile
 * Nfa.compile}. If the field does not exist, <code>null</code> is
 * assumed.</p> 
 *
 * <p><b><code>roi</code>:</b> The module <em>may</em> have the
 * variable <code>roi</code> holding a value of type {@link Roi}. If
 * it is available, the finite automaton constructed with
 * <code>populate</code> is activated only by a wrapper automaton for
 * the region of interest defined. In that case, <code>eofAction</code>
 * and <code>onFailedMatch</code> may not be defined in this module to
 * avoid ambiguities. Example:</p>
 * <pre class="codex">from monq.jfa import Roi, Xml
 *   roi = Roi(Xml.STag("x"), Xml.ETag("x"))</pre>
 *
 * <p>The automaton created when such a <code>Roi</code> is defined
 * will ignore everything outside an XML <code>x</code> element. To be
 * precise, everything up to the first occurence of the end tag for
 * <code>x</code> is ignored. For nested <code>x</code> elements, this
 * would be wrong. Of course all configuration methods of
 * <code>Roi</code> can be used in the python module.</p>
 *
 * <p><b><code>getClientData</code>:</b> The module <em>may</em> have
 * a function
 * <pre class="codex">def getClientData():
 *   ...</pre>
 * which is called each time the <code>Dfa</code> is wrapped into a
 * {@link DfaRun}, i.e. when {@link #createRun} gets called. The value
 * returned is entered into the {@link DfaRun#clientData} field. If
 * the value returned is mutable, it must be a freshly allocated
 * object each time. If
 * no function <code>getClientData</code> exists,
 * <code>DfaRun.clientData</code> is left to be <code>null</code>.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class JyFA {
  private PythonInterpreter pi = new PythonInterpreter();

  private String modLoadName;	// name used to load the module
  private PyModule mod;
  private PyFunction getClientData = null;
  private Dfa dfa;

  private static final String PYIDENT = "[A-Za-z_][A-Za-z0-9_]*";
  private static final Pattern PYMODULENAME
    = Pattern.compile(PYIDENT+"([.]"+PYIDENT+")*");

  /**********************************************************************/
  /**
   * <p>calls the 2 parameter constructor with a freshly allocated,
   * empty <code>Nfa</code>.</p>
   */
  public JyFA(String pyModuleName) 
    throws ReSyntaxException, CompileDfaException, IOException
  {
    this(new Nfa(Nfa.NOTHING), pyModuleName);
  }
  /**********************************************************************/
  /**
   * <p>loads the given Python module and calls its
   * <code>populate</code> function to add regexp/action pairs to the
   * given <code>Nfa</code>. The Jython interpreter that loads the
   * module will be freshly allocated. Consequently the module is
   * searched along the variable <code>sys.path</code>, which by
   * default is initialized from the system property
   * <code>python.path</code>. If parameter <code>pyModule</code>
   * matches the regular expression for a 
   * sequence of <em>Python identifiers</em> separated by dots (i.e. a
   * module name), it is loaded with the code
   *
   * <pre>  import pyModule</pre>
   *
   * <p>Otherwise the name is separated into a canonicalized
   * <em>path</em> and a filename with the help of a {@link
   * java.io.File} object. The filename gets <b>any</b> extension
   * starting with dot stripped off to produce the module name
   * <em>mname</em>. Then the following code is executed to load teh
   * module:</p>
   *
   * <pre>  import sys
   *  sys.path.insert(0, <em>path</em>)
   *  import mname</pre>
   *
   * <p>As soon as the <code>populate</code> function of the Jython
   * module returns, the <code>Nfa</code> is compiled into a
   * <code>Dfa</code>, which is then stored internally.</p>
   *
   * <p><b>Note</b>: The <code>Nfa</code> may have
   * regexp/action pairs preloaded, but after passing it to this
   * constructor, it cannot be used anymore.</p>
   *
   * @see <a href="http://www.python.org/doc/current/ref/identifiers.html#tok-identifier">Python identifier</a>
   *
   * @throws CompileDfaException if the python module sets up an Nfa
   * with conflicts
   * @throws java.io.IOException for any problem occuring while
   * loading/importing the python module
   */
  public JyFA(Nfa nfa, final String pyModule) 
    throws ReSyntaxException, CompileDfaException, IOException
  {
    this.modLoadName = pyModule;
    String modname = pyModule;
    Matcher m = PYMODULENAME.matcher(modname);

    try {
      // while "bla.py" matches a module name, we rather consider it
      // to be a file name
      if( !m.matches() || modname.endsWith(".py") ) {
	File f = new File(modname);
	File parentF = f.getParentFile();
	String path = ".";
	if( parentF!=null ) path = parentF.getCanonicalPath();
	modname = f.getName();
	int pos;
	if( -1!=(pos=modname.lastIndexOf(".")) ) {
	  modname = modname.substring(0, pos);
	}
	pi.exec("import sys");
	pi.exec("sys.path.insert(0, '"+path+"')");
	//pi.exec("print sys.path");
      }

      // import the module and get the module object for further reference
      pi.exec("import "+modname);
      mod = (PyModule)pi.get(modname);

      // if there is a variable roi, get it and prepare the roi-NFA
//       PyObject po = mod.__fintattr__("roi");
//       if( po!=null ) nfa = addRoi(nfa, po);

      //
      // get the method populate and run it on the nfa
      //
      PyFunction pf = getPyFunc("populate", false);
      pf.__call__(Py.java2py(nfa));

      // -------------------------------------------------
      // get the FailedMatchBehaviour 
      //
      DfaRun.FailedMatchBehaviour fmb 
	= (DfaRun.FailedMatchBehaviour)
	getJavaObj(DfaRun.FailedMatchBehaviour.class, "onFailedMatch");
      // -------------------------------------------------
      // get the eofAction, if any
      //
      FaAction eofAction 
	= (FaAction)getJavaObj(FaAction.class, "eofAction");

      // -------------------------------------------------
      // get the roi, if any
      //
      Roi roi = (Roi)getJavaObj(Roi.class, "roi");
      if( roi!=null ) {
	if( fmb!=null ) {
	  throw new IOException("roi and onFailedMatch may not be defined "
				+"both in module `"+pyModule+"'");
	}
	if( eofAction!=null ) {
	  throw new IOException("roi and eofAction may not be defined "
				+"both in module `"+pyModule+"'");
	}
	dfa = roi.wrap(nfa);
      } else {
	if( fmb==null ) fmb = DfaRun.UNMATCHED_COPY;
	dfa = nfa.compile(fmb, eofAction);
      }

      // Do a type check on getClientData to make sure a wrong type
      // does not cause exceptions later. We call this mainly to
      // trigger an exception 
      getClientData = getPyFunc("getClientData", true);

    } catch( org.python.core.PyException e ) {
      IOException ex = 
	new IOException
	("could not load/import `"+pyModule+"'; check cause for more info");
      ex.initCause(e);
      throw ex;
    }
  }
  /**********************************************************************/
  private Object getJavaObj(Class klass, String name) throws IOException
  {
    PyObject po = mod.__findattr__(name);
    if( po==null ) return null;
    Object o = po.__tojava__(klass);
    if( !klass.isInstance(o) ) {
      throw new IOException(name+" is not of type "+klass.getName()
			    +" in module `"+modLoadName+"'");
    }
    return o;
  }
  /**********************************************************************/
  private PyFunction getPyFunc(String name, boolean optional) 
    throws IOException
  {
    PyObject po = mod.__findattr__(name);
    if( po==null ) {
      if( optional ) return null;
      throw new IOException("method `"+name+
			    "' not found in `"+modLoadName+"'");
    }
    if( !(po instanceof PyFunction) ) {
      throw new IOException("method `"+name+
			    "' is not a function in `"
			    +modLoadName+"'");
    }
    return (PyFunction)po;
  }
  /**********************************************************************/
  /**
   * <p>creates a {@link DfaRun} object for the managed {@link
   * Dfa}. If the Python module passed to the constructor has a
   * parameterless function called <code>getClientData</code>, it is
   * called and the result is entered into the {@link
   * DfaRun#clientData} field of the value returned.</p>
   */
  public DfaRun createRun() {
    DfaRun r = new DfaRun(dfa);
    if( getClientData!=null ) {
      PyObject po = getClientData.__call__();
      if( po!=null ) r.clientData = po.__tojava__(java.lang.Object.class);
    }
    return r;
  }
  /**********************************************************************/
}
