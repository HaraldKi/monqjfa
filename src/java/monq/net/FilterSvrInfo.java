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
/**
 * <p>describes a filter server running somewhere on the
 * network.

 */
public class FilterSvrInfo {

  /**
   * the name of the server, derived from the file name from
   * which the configuration was loaded.
   */
  public final String name;

  /** a one line description of the server function */
  public final String synopsis;

  /** name of the machine on which the server is running */
  public final String host;

  /** port number on which the server is listening */
  public final int port;

  /**
   * <p>the Request used by a DistPipeFilter to access this server. It
   * is assembled from <code>host</code> and <code>port</code>
   * immediately after the config file is read.</p>
   */
  private PipelineRequest request;

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
  public FilterSvrInfo(String name, String synopsis, String host, int port) {
    this.name = name;
    this.synopsis = synopsis;
    this.host = host;
    this.port = port;
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
  public static FilterSvrInfo from(Properties props)
      throws ServiceCreateException
  {
    for (String key : new String[] {"host", "port"}) {
      if (null==props.getProperty(key)) {
        throw new ServiceCreateException("missing value for key "+key);
      }
    }

    int port;
    try {
      port = Integer.parseInt(props.getProperty("port"));
    } catch (NumberFormatException e) {
      throw new ServiceCreateException("see cause", e);
    }
    String host = props.getProperty("host");
    String synopsis = props.getProperty("synopsis", "(no synopsis given");
    String name = props.getProperty("name", "filter on "+host+":"+port);

    return new FilterSvrInfo(name, synopsis, host, port);
  }
  /**********************************************************************/
  /**
   * <p>calls {@link #read read()} and stores any exception thrown in
   * the object returned. This allows to keep inoperable objects for
   * later reference and explanation.
   * @throws IOException
   */
  public static FilterSvrInfo fromFile(String fName) throws IOException {
    Properties props = new Properties();
    try (InputStream in = new FileInputStream(fName);
        BufferedInputStream bin = new BufferedInputStream(in);
        Reader rin = new InputStreamReader(bin, "UTF-8");
        ) {
      props.load(rin);
      props.setProperty("name", getName(fName));
      return from(props);
    }
  }
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
  public static Map<String,FilterSvrInfo> readAll(String directory)
      throws IOException
  {
    Map<String,FilterSvrInfo> m = new HashMap<String,FilterSvrInfo>();

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
}
