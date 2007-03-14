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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class ByteCharSourceTest extends TestCase {

  public static void test1() throws Exception {
    byte[] b = new byte[5000];
    for(int i=0; i<b.length; i++) b[i] = (byte)((i%26)+'a');
    ByteArrayInputStream bis = new ByteArrayInputStream(b);
    CharsetDecoder d = Charset.forName("iso-8859-1").newDecoder();    
      
    ByteCharSource bcs = 
      new ByteCharSource(bis)
      .setDecoder(d);

    for(int i=0; i<5000; i++) {
      int ch = bcs.read();
      assertEquals(i%26+'a', ch);
    }
    bcs.close();

    // do the same again after setting the source again
    bcs.setSource(Channels.newChannel(new ByteArrayInputStream(b)));
    for(int i=0; i<5000; i++) {
      int ch = bcs.read();
      assertEquals(i%26+'a', ch);
    }
    bcs.close();

  }
  /********************************************************************/
  public static void test_smallInputBuffer() throws Exception {
    byte[] b = new byte[5000];
    for(int i=0; i<b.length; i++) b[i] = (byte)((i%26)+'a');
    ByteArrayInputStream bis = new ByteArrayInputStream(b);
    CharsetDecoder d = Charset.forName("UTF-8").newDecoder();    
      
    ByteCharSource bcs = 
      new ByteCharSource(bis)
      .setDecoder(d)
      .setInputBufferSize(1);
    

    for(int i=0; i<5000; i++) {
      int ch = bcs.read();
      assertEquals(i%26+'a', ch);
    }
    bcs.close();
  }
  /********************************************************************/
  public static void test_pushBack() throws Exception {
    byte[] b = new byte[5000];
    for(int i=0; i<b.length; i++) b[i] = (byte)((i%26)+'a');
    ByteArrayInputStream bis = new ByteArrayInputStream(b);
    CharsetDecoder d = Charset.forName("UTF-8").newDecoder();    
      
    ByteCharSource bcs = 
      new ByteCharSource(bis)
      .setDecoder(d)
      .setInputBufferSize(1);
    

    for(int i=0; i<2000; i++) {
      int ch = bcs.read();
      assertEquals(i%26+'a', ch);
    }
    StringBuffer sb = new StringBuffer();
    String s = "alle meine entchen";
    sb.append(s);
    int L = sb.length();
    bcs.pushBack(sb, 0);
    for(int i=0; i<L; i++) {
      long pos = bcs.position(0);
      int ch = bcs.read();
      assertEquals((long)2000-L+i, pos);
      assertEquals(s.charAt(i), (char)ch);
    }

    for(int i=2000; i<5000; i++) {
      int ch = bcs.read();
      assertEquals(i%26+'a', ch);
    }
    bcs.close();
  }
  /********************************************************************/
  public static void test_malformed() throws Exception {
    byte[] b = new byte[50];
    for(int i=0; i<b.length; i++) b[i] = 'a';
    b[30] = (byte)150;
    ByteArrayInputStream bis = new ByteArrayInputStream(b);
    CharsetDecoder d = Charset.forName("ascii").newDecoder();    
    ByteCharSource bcs = 
      new ByteCharSource(bis)
      .setDecoder(d)
      .setInputBufferSize(1);

    Exception e = null;
    int i=0;
    try {
      int ch;
      while( -1!=(ch=bcs.read()) ) {
	assertEquals('a', (char)ch);
	i += 1;
      }
    } catch( IOException _e ) {
      e = _e;
    }
    //System.out.println(e.getMessage());
    assertEquals(30, i);
    assertTrue(e instanceof IOException);
    assertTrue(e.getMessage().startsWith("decoder for character set"));
  }
  /********************************************************************/
  public static void test_setWindowSize() throws Exception {
    int N = 3000;
    byte[] b = new byte[N];
    for(int i=0; i<b.length; i++) b[i] = (byte)((i%26)+'a');
    ByteArrayInputStream bis = new ByteArrayInputStream(b);
    CharsetDecoder d = Charset.forName("iso-8859-1").newDecoder();    
      
    ByteCharSource bcs = 
      new ByteCharSource(bis)
      .setDecoder(d)
      .setWindowSize(30);
    
    int i=0;
    int ch;
    while( -1!=(ch=bcs.read()) ) {
      assertEquals(i%26+'a', ch);
      i+=1;
    }
    Exception e = null;
    try {
      for(i=0; i<31; i++) {
	assertEquals((long)N-i, bcs.position(-i));
      }
    } catch( UnavailablePositionException _e ) {
      e = _e;
    }
    assertEquals(30, i);
    assertTrue(e instanceof UnavailablePositionException);
    assertTrue(e.getMessage()
	       .startsWith(UnavailablePositionException.EXPIRED));


    // now shorten the window size and check againg
    bcs.setWindowSize(10);
    e = null;
    try {
      for(i=0; i<11; i++) {
	assertEquals((long)N-i, bcs.position(-i));
      }
    } catch( UnavailablePositionException _e ) {
      e = _e;
    }
    assertEquals(10, i);
    assertTrue(e instanceof UnavailablePositionException);
    assertTrue(e.getMessage()
	       .startsWith(UnavailablePositionException.EXPIRED));

    // for completeness, check ahead and get an exception
    e = null;
    try {
      bcs.position(1);
    } catch( UnavailablePositionException _e) {
      e = _e;
    }
    assertTrue(e instanceof UnavailablePositionException);
    assertTrue(e.getMessage()
	       .startsWith(UnavailablePositionException.NOTYET));

    bcs.close();
  }
  /********************************************************************/
  public static void test_forReal() throws Exception {

    String[] charsets = {"UTF-8", "UTF-16"};
    
    for(int k=0; k<charsets.length; k++) {
      String chsName = charsets[k];

      // chars in the higher regions are not really chars.
      int maxChar = 0xd800;
      String tmpfile = "/tmp/test-"+chsName;

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CharsetEncoder enc = Charset.forName(chsName).newEncoder();    
      OutputStreamWriter out = new OutputStreamWriter(baos, enc);
      int[] positions = new int[maxChar];
      char[] chars = new char[maxChar];
      int count = 0;
      for(int i=0; i<maxChar; i++) {
	//System.out.print(i+" ");
	if( !enc.canEncode((char)i) ) continue;
	out.write((char)i);
	out.flush();
	positions[count] = baos.size();
	chars[count++] = (char)i;	
      }
      out.close();
      
      FileOutputStream fos = new FileOutputStream(tmpfile);
      fos.write(baos.toByteArray());
      fos.close();
      
      ByteCharSource bcs = 
	new ByteCharSource(tmpfile)
	.setDecoder(Charset.forName(chsName).newDecoder());
      
      for(int i=0; i<count-2; i++) {
	int ch = bcs.read();
	assertEquals(chars[i], ch);
	assertEquals(positions[i], bcs.position(0));
      }
      bcs.close();
      new File(tmpfile).delete();
    }
  }
  /********************************************************************/

  public static void main(String[] argv)   {
    junit.textui.TestRunner.run(new TestSuite(ByteCharSourceTest.class));
  }
}

