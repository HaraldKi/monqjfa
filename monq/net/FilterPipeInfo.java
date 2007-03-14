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
 * describes a sequence of filter servers.
 *
 * @author &copy; 2004 Harald Kirsch
 * @version $Revision: 1.6 $, $Date: 2006-01-25 15:49:43 $
 */
public class FilterPipeInfo {
  /** 
   * the name of the pipeline, usually derived from the file name from
   * which the configuration was loaded.
   */
  public String name;

  /** a short hint to the pipeline function */
  public String desc;

  /** is this exported to whatizit/pipe for programmatic access? */
  String access;
  
  /**
   * This is a list of names of servers. It does <b>not</b>contain
   * <code>FilterSvrInfo</code> objects. For historical reasons, this
   * is called dfas and not servers, as 
   * it should be.
   */
  public ArrayList dfas; 

  /** 
   * Style sheet to apply to get readable HTML out of the pipeline.
   */
  public String style;

  /**
   * if anything goes wrong when reading in a FilterPipeInfo from a config
   * file, the exception is stored here. This object cannot be used
   * then for anything else but for reporting this exception.
   */
  public Exception e = null;

  /**
   * <p>the request to be used by a DistPipeFilter to access this
   * pipeline. It is set from the host and port entries of servers
   * referenced by the elements of {@link #dfas} and is constructed
   * when the configurationfile is read.</p>
   */
  private PipelineRequest[] request = null; 

  static XStream xs;
  static {
    xs = new XStream(new DomDriver());    
    xs.alias("pipe", FilterPipeInfo.class);   
    xs.alias("svr", String.class);
    //xs.alias("dfas", (new String[0]).getClass());
    //xs.alias("name", null);
  }
  /**********************************************************************/
  /**
   * <p>returns the request to be used by a DistPipeFilter to access this
   * pipeline. It is set from the host and port entries of servers
   * referenced by the elements of {@link #dfas} and is constructed
   * when the configuration file is read.</p>
   *
   * <p>This method returns copies of the internally stored requests
   * to allow for parameters to be added before the request is used to
   * contact the list of servers.
   */
  public PipelineRequest[] getRequest() {
    int l = request.length;
    PipelineRequest[] req = new PipelineRequest[l];
    for(int i=0; i<l; i++) req[i] = new PipelineRequest(request[i]);
    return req;
  }     
  /**********************************************************************/
  /**
   * can be used to create an inoperable <code>FilterPipeInfo</code> to
   * be kept for reference of what went wrong while the configuration
   * was read
   */
  private FilterPipeInfo(String name, Exception e) {
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
   * <code>FilterPipeInfo</code> object. 
   * </p>
   * @throws Exception if anything goes wrong while reading the
   * configuration file.
   */
  public static FilterPipeInfo read(String fName, Map servers) 
    throws Exception {

    Reader r = new FileReader(fName);
    FilterPipeInfo pipe = (FilterPipeInfo)(xs.fromXML(r));

    pipe.name = getName(fName);
    int l = pipe.dfas.size();
    pipe.request = new PipelineRequest[l];
    for(int i=0; i<l; i++) {
      //System.out.println("looking up `"+ pipe.dfas.get(i)+"'");
      FilterSvrInfo svr = (FilterSvrInfo)servers.get(pipe.dfas.get(i));
      if( svr==null ) {
	pipe.e = new IOException("no information for server `"+
				 pipe.dfas.get(i)+
				 "' available");
	return pipe;
      }
      if( svr.e!=null ) {
	Exception e =  new IOException
	  ("referenced server configuration for server `"+
	   svr.name+
	   "' is broken");
	e.initCause(svr.e);
	pipe.e = e;
	return pipe;
      }
      pipe.request[i] = svr.getRequest();
    }
    return pipe;
  }
  /**********************************************************************/
  /**
   * <p>calls {@link #read read()} and stores any exception thrown in
   * the object returned. This allows to keep inoperable objects for
   * later reference and explanation.</p>
   */
  public static FilterPipeInfo fromFile(String fName, Map servers) {
    try {
      return read(fName, servers);
    } catch( Exception e ) {
      return new FilterPipeInfo(getName(fName), e);
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
   * <p>assumes that all files with suffix <code>".pipe"</code> in the
   * given directory are configuration files. They are read and the
   * resulting objecst are stored in the <code>Map</code>
   * returned. The basename of the file name is used as a key.</p>
   *
   * <p>This method does not throw any exceptions. Instead, exceptions
   * thrown while reading configuration files will end up in
   * <code>FilterPipeInfo</code> objects with their field
   * <code>e</code> set.</p>
   *
   * @param directory is the name of the directory where to find
   * configuration files with suffix <code>".pipe"</code>
   *
   * @param servers is a map which must contain {@link FilterSvrInfo}
   * objects referenced in the <code>svr</code> elements of the
   * configuration file.
   *
   * @throws FileNotFoundException if the given name does not denote a
   * directory or cannot be accessed.
   */
  public static Map readAll(String directory, Map servers)
    throws FileNotFoundException 
  {
    Map m = new HashMap();
    
    File dir = new File(directory);
    final File[] files = dir.listFiles();
    if( files==null ) throw new 
      FileNotFoundException("`"+directory+"' cannot be accessed.");
  
    for(int i=0; i<files.length; i++) {
      File f = files[i];
      if( !f.isFile() ) continue;
      String name = f.getName();
      if( !name.endsWith(".pipe") ) continue;
      name = name.substring(0, name.length()-5);
      FilterPipeInfo pipe = fromFile(files[i].getPath(), servers);
      m.put(pipe.name, pipe);
    }
    return m;
  }
  /**********************************************************************/
  /**
   * testing only.
   */
  public static void main(String[] argv) throws Exception {
    Map servers = FilterSvrInfo.readAll(argv[0]);
    //System.out.println(servers);
    Map pipes = readAll(argv[0], servers);

    Iterator ii = pipes.keySet().iterator();
    while( ii.hasNext() ) {
      Object key = ii.next();
      FilterPipeInfo value = (FilterPipeInfo)pipes.get(key);
      value.write(new OutputStreamWriter(System.out));
      System.out.println();
    }
  }



}
