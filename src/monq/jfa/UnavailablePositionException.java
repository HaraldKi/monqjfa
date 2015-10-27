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
 * <p>is thrown by {@link ByteCharSource#position
 * FileCharSource.position()} if the position of a character is
 * requested for which this position is no longer or not yet
 * available.</p>
 *
 * @author (C) 2003 Harald Kirsch
 * @version $Revision: 1.4 $, $Date: 2005-02-14 10:23:38 $
 */
public class UnavailablePositionException extends Exception {
  public static final String EXPIRED =
    "the file position of the requested character "+
    "has expired from the cache";
  public static final String NOTYET = 
    "the file postion of the requested character "+
    "is not yet available";
  public UnavailablePositionException(String emsg) {super(emsg);}
}
