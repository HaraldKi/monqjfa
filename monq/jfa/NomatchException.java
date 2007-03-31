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

/**
 * <p>is the exception thrown by methods in {@link DfaRun} whenever no
 * match can be found at the current input position.
 *
 * @version $Revision: 1.4 $, $Date: 2005-02-14 10:23:38 $
 * @author &copy; Harald Kirsch
 */
public class NomatchException extends CallbackException {
  public NomatchException(String emsg) { super(emsg); }
}
