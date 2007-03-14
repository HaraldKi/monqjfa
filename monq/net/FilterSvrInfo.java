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

package monq.net;

import java.io.*;
import java.util.*;
import com.thoughtworks.xstream.*;
import com.thoughtworks.xstream.io.xml.*;
/**
 * <p>describes a filter server running somewhere on the
 * network. Objects of this class can by now only be created with
 * {@link #fromFile fromFile()}. An example configuration file looks
 * like this:</p>
 * <pre>
&lt;svr>
  &lt;synopsis>tags words which look like protein names and links them
    to UniProt&lt;/synopsis>

  &lt;environment>
    &lt;env>
      &lt;key>PATH&lt;/key> 
      &lt;value>${PATH}:/ebi/textmining/Linux/bin&lt;/value>
    &lt;/env>
    &lt;env>
      &lt;key>CLASSPATH&lt;/key>
      &lt;value>/ebi/textmining/Linux/lib/java&lt;/value>
    &lt;/env>
  &lt;/environment>
  
  &lt;access>public&lt;/access>
  &lt;host>web62-node1&lt;/host>
  &lt;port>8004&lt;/port>
  &lt;cmd>DictFilter -t elem -e plain -p ${port} /ebi/textmining/Web/automata/swissprot.mwt&lt;/cmd>
&lt;/svr></pre>
 * <p><b>Note:</b> The fields of this object are public, but should be
 * considered read-only. I could make the fields final, but for the
 * <code>environment</code>, which is a <code>Map</code>, this would
 * not help anyway. Therefore: if you mess with the data, it is your
 * own fault.</p> 
 *
 * @author &copy; 2004 Harald Kirsch
 * @version $Revision: 1.9 $, $Date: 2006-01-25 15:49:43 $
 */
public class FilterSvrInfo {

  /** 
   * the name of the server, usually derived from the file name from
   * which the configuration was loaded.
   */
  public String name;

  /** a one line description of the server function */
  public String synopsis;

  /** a (non-formal) description of the input encoding */
  public String inputenc = "UTF-8";

  /** name of the machine on which the server is running */
  public String host;

  /** port number on which the server is listening */
  public int port;


  /** is this exported to whatizit/pipe for programmatic access? */
  String access;
  
  /** 
   * <p>Any environment variables which must be set before the command
   * can be run.</p>
   */
  public HashMap environment = new HashMap();

  /** 
   * The sh command to start the server on the machine given by
   * host. The port number should be referenced with
   * <code>${port}</code>.
   */
  public String cmd;

  /**
   * <p>set by {@link #fromFile} if anything goes wrong when reading the
   * config file.</p>
   */
  public Exception e = null;

  /**
   * <p>the Request used by a DistPipeFilter to access this server. It
   * is assembled from <code>host</code> and <code>port</code>
   * immediately after the config file is read.</p>
   */
  private PipelineRequest request; 

  /**
   * <p>input string and output regular expression that should be used
   * to check if the server is 
   * running at all.</p>
   */
  public TestInfo test;

  static XStream xs;
  static {
    xs = new XStream(new DomDriver());    
    xs.alias("svr", FilterSvrInfo.class);   
    xs.alias("environment", HashMap.class);
    xs.alias("key", String.class);
    xs.alias("value", String.class);
    xs.alias("test", TestInfo.class);
    //xs.alias("name", null);
  }
  /**********************************************************************/
  // only here to satisfy XStream (untested)
  private static final class TestInfo {
    String in;
    String re;
  }
  /**********************************************************************/
  /**
   * <p>returns a copy of the request object to contact this server. The
   * copy can then be augmented with additional parameters before
   * making contact. The name of the object returned is identical to
   * {@link #name}.</p>
   */
  public PipelineRequest getRequest() {
    return new PipelineRequest(request);
  }
  /**********************************************************************/
  /**
   * can be used to create an inoperable <code>FilterSvrInfo</code> to
   * be kept for reference of what went wrong while the configuration
   * was read
   */
  private FilterSvrInfo(String name, Exception e) {
    this.name = name;
    this.e = e;
  }
  /**********************************************************************/
  private static String getName(String fName) {
    String name = new File(fName).getName();
    int pos = name.lastIndexOf(".");
    if( pos<0 ) pos=name.length();
    name = name.substring(0, pos);
    return name;
  }
  /**********************************************************************/
  /**
   * <p>reads the configuration file and returns a
   * <code>FilterSvrInfo</code> object. Try using {@link #fromFile} to
   * avoid any exception being thrown.
   * </p>
   * @throws Exception if anything goes wrong while reading the
   * configuration file.
   */
  public static FilterSvrInfo read(String fName) 
    throws Exception {

    Reader r = new FileReader(fName);
    FilterSvrInfo svr = (FilterSvrInfo)(xs.fromXML(r));

    svr.name = getName(fName);
    svr.request = new PipelineRequest(svr.name, svr.host, svr.port);
    return svr;
  }
  /**********************************************************************/
  /**
   * <p>calls {@link #read read()} and stores any exception thrown in
   * the object returned. This allows to keep inoperable objects for
   * later reference and explanation.
   */
  public static FilterSvrInfo fromFile(String fName) {
    try {
      return read(fName);
    } catch( java.lang.Exception e ) {
      return new FilterSvrInfo(getName(fName), e);
    }
  }
  /**********************************************************************/
  void write(Writer out) {
    xs.toXML(this, out);
  }
  /**********************************************************************/
  /**
   * return <code>true</code> iff the <code>access</code> field
   * contains exactly the string <code>"public"</code>.
   */
  public boolean isPublic() {return "public".equals(access);}
  /**********************************************************************/
  /**
   * <p>assumes that all files with suffix <code>".svr"</code> in the
   * given directory are configuration files. They are read and the
   * resulting objects are stored in the <code>Map</code>
   * returned. The basename of the file name is used as a key.</p>
   *
   * <p>This method does not throw any exceptions. Instead, exceptions
   * thrown while reading configuration files will end up in
   * <code>FilterSvrInfo</code> objects with their field
   * <code>e</code> set.</p>
   *
   * @param directory is the name of the directory where to find
   * configuration files with suffix <code>".svr"</code>
   *
   * @throws FileNotFoundException if the given name does not denote a
   * directory or cannot be accessed.
   */
  public static Map readAll(String directory) throws FileNotFoundException {
    Map m = new HashMap();
    
    File dir = new File(directory);
    final File[] files = dir.listFiles();
    if( files==null ) throw new 
      FileNotFoundException("`"+directory+"' cannot be accessed.");

    for(int i=0; i<files.length; i++) {
      File f = files[i];
      if( !f.isFile() ) continue;
      String name = f.getName();
      if( !name.endsWith(".svr") ) continue;
      name = name.substring(0, name.length()-4);
      FilterSvrInfo pipe = fromFile(files[i].getPath());
      m.put(pipe.name, pipe);
    }
    return m;
  }
  /**********************************************************************/
  /**
   * testing only.
   */
  public static void main(String[] argv) throws Exception {
    FilterSvrInfo svr;
    if( argv.length>0 ) {
      svr = (FilterSvrInfo)xs.fromXML(new FileReader(argv[0]));
    } else {
      svr = new FilterSvrInfo("", null);
      svr.name = "don't use this field. It is overridden by the file name";
      svr.synopsis = "A one line, human readable description of the server";
      svr.host = "host.at.somewhere.boo";
      svr.port = 21334;
      svr.access = "public";
      svr.cmd = "java -Xmx1000m my.class.DoThings -p ${port}";
      svr.environment.put("CLASSPATH", "your.stuff.jar:more.glue.jar");
      System.out.println
	("<!-- \n"+
	 " This must end up in file someServer.svr.\n"+
	 " The name element is not used and need not be set.\n"+
	 " -->");
    }
    svr.write(new OutputStreamWriter(System.out)); 
    System.out.println();
  }
}
