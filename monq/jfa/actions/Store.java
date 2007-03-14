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

package monq.jfa.actions;

import monq.jfa.*;
import java.util.Map;

/**
 * <p>an <code>FaAction</code> to store away the match in a
 * <code>Map</code>. The map is obtained from the {@link MapProvider}
 * which must be stored in the {@link monq.jfa.DfaRun#clientData}
 * field of the calling <code>DfaRun</code>.</p>
 *
 * @author &copy; 2003, 2004, 2005 Harald Kirsch
 * @version $Revision: 1.6 $, $Date: 2005-07-08 13:01:26 $
 */
public class Store extends AbstractFaAction {
  private String key;

  /**
   * <p>stores the match with the given <code>key</code> in a map
   * obtained from a {@link MapProvider} expected to be found in the
   * {@link monq.jfa.DfaRun#clientData} field of the calling
   * <code>DfaRun</code> object. The match will not be deleted from
   * the output stream. Consider combining a <code>Store</code>
   * callback via a {@link Run} with other callbacks to get a
   * different outcome.</p>
   */
  public Store(String key) {
    this.key = key;
  }
  public void invoke(StringBuffer out, int start, DfaRun runner) {
    MapProvider mp = (MapProvider)runner.clientData;
    Map map = mp.getMap();
    map.put(key, out.substring(start));
  }
}
 
