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

import java.util.Map;

/**
 * <p>Objects implementing this interface are needed by some {@link
 * monq.jfa.FaAction} callbacks to perform their task. When the
 * <code>invoke()</code> method of a such a callback is called, it
 * expects a <code>MapProvider</code> in the {@link
 * monq.jfa.DfaRun#clientData} object passed in. Examples are {@link
 * Hold}, {@link Count} and {@link Store} while {@link Printf} uses
 * the MapProvider only if available.</p>
 */
public interface MapProvider {
  /**
   * <p>return the <code>java.util.Map</code> provided by this object. 
   * The <code>Map</code> may be used for other tasks as long as it
   * is not totally cleared.</p>
   */
  Map<Object,Object> getMap(); 
}