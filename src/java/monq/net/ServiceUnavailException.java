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

/**
 * thrown if something goes wrong when creating a service because the
 * service is currently unavailable, but is expected to work again in
 * further requests.
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class ServiceUnavailException extends ServiceCreateException {
  public ServiceUnavailException(String s, Exception e) { super(s, e); }
  public ServiceUnavailException(String s) { super(s); }
}
