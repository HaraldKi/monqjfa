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

package monq.stuff;

import java.io.*;

/**
  is an interface for a {@link java.lang.Runnable} which wants to have
  set an input stream to read from and/or an output stream to write to
  before it is run.

  <p>It is expected, that classes implementing this interface will
  read from an input stream, write to an output stream or do both in
  their <kbd>run()</kbd> method. The point of having the set-functions
  instead of specifying the streams in the constructor of such a class
  is that objects of the class can be created and passed around to
  only later have the stream(s) sepcified.
  @author &copy; 2000,2001,2002,2003,2004 Harald Kirsch
  @version $Revision: 1.3 $ $Date: 2005-02-14 10:23:39 $
*****/
//<p>The {@link Exec} class make have use of <kbd>Plumbable</kbd>s.
public interface Plumbable extends Runnable {
  void setOut(OutputStream out, boolean closeOnExit);
  void setIn(InputStream in, boolean closeOnExit);

  /**
    returns an exception which happend in the
    <kbd>run()</kbd>-method, if any.

    <p>Because input and output may cause exceptions while
    the <kbd>run</kbd>-method can not throw anything, an implementing
    class must have this function which allows it to return an
    exception which might happen during <kbd>run()</kbd>.
  *****/
  Exception getException();
}
