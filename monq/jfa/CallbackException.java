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

import java.io.IOException;

/**
 * <p>is the exception originally thrown by {@link FaAction#invoke}.</p>
 * @version $Revision: 1.3 $, $Date: 2005-02-14 10:23:38 $
 * @author &copy; Harald Kirsch
 */
public class CallbackException extends IOException {
  public CallbackException(String emsg, Throwable e) { 
    super(emsg);
    initCause(e);
  }
  public CallbackException(String emsg) { super(emsg); }
}
