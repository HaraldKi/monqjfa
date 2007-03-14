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


import monq.stuff.Coder;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.Collection;
import java.util.Iterator;

import java.nio.charset.*;
import java.nio.*;
/**
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class CoderTest extends TestCase {
  private Collection charsets;
  public void setUp() {
   charsets = Charset.availableCharsets().values();
  }

  public void  test_plain() 
    throws CharacterCodingException
  {
    Iterator it = charsets.iterator();
    while( it.hasNext() ) {
      Charset chs = (Charset)it.next();
      if( !chs.canEncode() ) continue;
      if( chs.displayName().startsWith("x-") ) continue;
      Coder cod = new Coder((Charset)(it.next()));
      StringBuffer a = new StringBuffer("hallo");
      ByteBuffer b = ByteBuffer.allocate(1);      
      b = cod.encode(a, b);
      a.setLength(0);
      cod.decode(b, a);
      assertEquals(chs.displayName(), "hallo", a.toString());
    }
  }
  /**********************************************************************/
  public void test_umlauts() 
    throws CharacterCodingException
  {
    Iterator it = charsets.iterator();
    int i = 0;
    while( it.hasNext() ) {
      Charset chs = (Charset)(it.next());
      if( !chs.canEncode() ) continue;
      if( !chs.displayName().startsWith("UTF") ) continue;
      i += 1;
      Coder cod = new Coder(chs);
      StringBuffer a = new StringBuffer("äöüßÄÖÜ");
      ByteBuffer b = ByteBuffer.allocate(1);      
      b = cod.encode(a, b);
      a.setLength(0);
      cod.decode(b, a);
      assertEquals(chs.displayName(), "äöüßÄÖÜ", a.toString());
    }
    assertTrue(i>0);
  }
  /**********************************************************************/
  // to be able to run on the command line
  public static void main(String[] argv)   {
    junit.textui.TestRunner.run(new TestSuite(CoderTest.class));
  }

}
 
