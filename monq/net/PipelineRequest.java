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

import monq.jfa.*;
import java.util.*;

/**
 * <p>Encapsulates a request for contact to a {@link
 * FilterServiceFactory}. In addition to the host and port to contact,
 * key/value pairs can be added as parameters for that server with
 * {@link #put put()}. These key values pairs
 * will be send to the contacted host as a request.</p>
 *
 * <p>Objects of this class have a name to support adding parameters
 * by looking up the name in a repository or property set of
 * parameters.</p> 
 *
 * @author &copy; 2005 Harald Kirsch
 * @version $Revision: 1.4 $, $Date: 2005-11-22 15:13:06 $
 * @see DistPipeFilter
 * @see monq.programs.DistFilter
 */

public class PipelineRequest {
  /**
   * <p>is the regular that must be matched by a key passed to {@link
   * #put put()}. Follow the link to below to see the actual value.</p>
   */
  public static final String KEYRE = "[A-Za-z_][A-Za-z_0-9]*";

  private Map<String,String> m = new HashMap<String,String>();
  private final String host;
  private final String port;
  private final String name;

  /**********************************************************************/
  // To check the proper name of a key we use the same static object
  // all the time, but we synchronize on its use. (see put())
  private static final Regexp iskey = new Regexp(KEYRE);

  /**********************************************************************/
  /**
   * <p>creates a <code>PipelineRequest</code> to contact the given host
   * on the given port. Use {@link #put put()} to add key/value pairs
   * to the request.</p>
   */
  public PipelineRequest(String name, String host, int port) {
    if( port<0 || port >0xffff ) 
      throw new IllegalArgumentException
	("port must be in the range 0..65535 but is "+port);
    this.host = host;
    this.port = Integer.toString(port);
    this.name = name;
  }  
  /**********************************************************************/
  /**
   * <p>calls the three parameter constructor with
   * <code>name=null</code>.</p> 
   */
  public PipelineRequest(String host, int port) {
    this(null, host, port);
  }
  /**********************************************************************/
  /**
   * returns copy of the given <code>PipelineRequest</code>.
   */
  public PipelineRequest(PipelineRequest other) {
    this.host = other.host;
    this.port = other.port;
    this.name = other.name;
    other.putParams(m);
  }
  /**********************************************************************/
  /**
   * <p>add a parameter to this request. The key/value pair is
   * send upstream as soon as a connection is made with this
   * object. The key must match the regular expression {@link #KEYRE}.</p>
   *
   * @throws IllegalStateException if the key does not match {@link
   * #KEYRE}. 
   */
  public void put(String key, String value) {
    synchronized(iskey) {
      if( !iskey.matches(key) ) 
	throw new IllegalArgumentException
	  ("key `"+key+"' does not match the regex `"+KEYRE+"' for keys");
    }
    m.put(key, value);
  }
  /**********************************************************************/
  private void encodeValue(StringBuffer out, String value) {
    int l = value.length();
    for(int i=0; i<l; i++) {
      char ch = value.charAt(i);
      if( ch=='\n' || ch==';' || ch=='|' ) out.append('\\');
      out.append(ch);
    }
  }
  /**********************************************************************/
  /**
   * encode this request into the given StringBuffer. The very first
   * request in the control string does not need host/port to be
   * encoded, because they are taken directly from the request
   * itself. Use withHead=false in this case.
   */
  void encode(StringBuffer out, boolean withHead) {
    if( withHead ) {
      if( out.length()>0 ) out.append(';');
      out.append(".host=");
      encodeValue(out, host);
      out.append(";.port=").append(port)
	.append('\n');
      
    }
    Iterator it = m.keySet().iterator();
    String sep = "";
    while( it.hasNext() ) {
      String key = (String)it.next();
      out.append(sep).append(key).append('=');
      encodeValue(out, (String)m.get(key));
      sep = ";";
    }
  }
  /**********************************************************************/
  /**
   * <p>retuns the name as set with in the constructor or the
   * string <code>getHost()+":"+getPort()</code>.</p>
   */
  public String getName() {
    if( name!=null ) return name;
    return host+':'+port;
  }
  /**********************************************************************/
  /**
   * <p>returns the hostname to contact as it was specified in the
   * constructor.</p>
   */
  public String getHost() { return host;}
  /**
   * <p>returns the port to contact as it was specified in the
   * constructor.</p>
   */
  public String getPort() { return port;}
  /**
   * <p>puts all key/value pairs that were stored via {@link #put
   * put()} into <code>params</code>.</p>
   */
  public void putParams(Map<String,String> params) { params.putAll(m); }
  /**********************************************************************/
  /**
   * <p>deletes all key/value pairs that were stored with {@link #put
   * put()}.</p>
   */
  public void clear() { m.clear(); }
  /**********************************************************************/
}
