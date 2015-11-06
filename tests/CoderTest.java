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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.*;

import static org.junit.Assert.*;

import java.nio.*;
/**
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class CoderTest {
  private Collection<Charset> charsets;

  @Before
  public void setUp() {
   charsets = Charset.availableCharsets().values();
  }

  @Test
  public void  test_plain()
    throws CharacterCodingException
  {
    Set<String> excludes = new HashSet<>();
    Collections.addAll(excludes, new String[] {"JIS_X0212-1990"});

    Iterator<Charset> it = charsets.iterator();
    while( it.hasNext() ) {
      Charset chs = it.next();

      if( !chs.canEncode() ) continue;
      if( chs.displayName().startsWith("x-") ) continue;
      if (excludes.contains(chs.displayName())) continue;

      Coder cod = new Coder(chs);
      StringBuilder a = new StringBuilder("hallo");
      ByteBuffer b = ByteBuffer.allocate(1);
      b = cod.encode(a, b);
      a.setLength(0);
      cod.decode(b, a);
      assertEquals(chs.displayName(), "hallo", a.toString());
    }
  }
  /**********************************************************************/
  @Test
  public void test_umlauts()
    throws CharacterCodingException
  {
    Iterator<Charset> it = charsets.iterator();
    int i = 0;
    while( it.hasNext() ) {
      Charset chs = (it.next());
      if( !chs.canEncode() ) continue;
      if( !chs.displayName().startsWith("UTF") ) continue;
      i += 1;
      Coder cod = new Coder(chs);
      StringBuilder a = new StringBuilder("�������");
      ByteBuffer b = ByteBuffer.allocate(1);
      b = cod.encode(a, b);
      a.setLength(0);
      cod.decode(b, a);
      assertEquals(chs.displayName(), "�������", a.toString());
    }
    assertTrue(i>0);
  }
  /**********************************************************************/
}

