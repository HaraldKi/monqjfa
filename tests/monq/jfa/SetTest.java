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

import java.util.*;
import junit.framework.TestCase;
/**
 *
 * @author &copy; 2004 Harald Kirsch
 */
public class SetTest extends TestCase {

//   abstract Set newSet();
//   abstract Set newSet(int initialCapacity);
//   abstract Set newSet(Collection col);

  <E> Set<E> newSet() { return new PlainSet<>(); }
  <E> Set<E> newSet(int s) { return new PlainSet<>(s); }
  <E> Set<E> newSet(Collection<E> c) { return new PlainSet<>(c); }

  /**********************************************************************/
  public void test0() throws Exception {
    Set<Long> s = newSet();
    for(long i=1; i!=0; i=i<<1) s.add(new Long(i));
    for(long i=1; i!=0; i=i<<1) {
      assertTrue(s.contains(new Long(i)));
    }
  }
  /**********************************************************************/
  public void testEqualsHashSet() throws Exception {
    Set<Long> s = newSet();
    for(long i=1; i!=0; i=i<<1) s.add(new Long(i));
    HashSet<Long> h = new HashSet<>(s);
    assertTrue(s.equals(h));

    Set<Long> other = newSet(h);
    assertTrue(s.equals(other));
    assertTrue(s.equals(s));

    other.add(new Long(11));
    assertTrue(!s.equals(other));

  }
  /**********************************************************************/
  public void test2() throws Exception {
    Set<Long> s = newSet();
    for(long i=1; i!=0; i=i<<1) s.add(new Long(i));
    Set<Long> other = newSet(s);
    assertTrue(s.equals(other));

    assertTrue(!s.addAll(other));
  }
  /**********************************************************************/
  public void test3() throws Exception {
    Set<Long> s = newSet();
    for(long i=1; i!=0; i=i<<1) s.add(new Long(i));
    Set<Long> other = newSet(1000);
    assertTrue(s.containsAll(s));
    assertTrue(s.containsAll(other));
  }
  /**********************************************************************/
  public void test4() throws Exception {
    Set<Long> s = newSet();
    for(long i=1; i!=0; i=i<<1) s.add(new Long(i));
    Set<Long> other = newSet(1000);

    Iterator<Long> it = s.iterator();
    while( it.hasNext() ) other.add(it.next());
    assertTrue(s.equals(other));
    assertTrue(other.equals(s));

    it = other.iterator();
    int i = 0;
    while( it.hasNext() ) {
      it.next();
      if( i%2==0 ) it.remove();
    }
    assertTrue(s.containsAll(other));
  }
  /**********************************************************************/
  // have a class which intentionally creates identical hashcodes
  // often
  private static class X {
    private int i;
    public X(int i) { this.i = i; }
    @Override
    public boolean equals(Object other) {
      if( !(other instanceof X) ) return false;
      return ((X)other).i==i;
    }
    @Override
    public int hashCode() {return i/10;}
  }
  public void test5() throws Exception {
    int N = 500;
    Set<X> s = newSet(N);
    for(int i=0; i<N; i+=10) {
      for(int j=0; j<10; j++) s.add(new X((j+5)%10+i));
    }
    Set<X> other = newSet();
    for(int i=3; i<N; i+=10) {
      other.add(new X(i));
    }


    // The following seemingly arbitrary tests were constructed while
    // looking at a test coverage of containsAll and in order to cover
    // all the possibilities
    assertTrue(s.containsAll(other));
    assertTrue(!other.containsAll(s));

    other.add(new X(N+30));
    assertTrue(!s.containsAll(other));

    s.add(new X(N+30));
    assertTrue(s.containsAll(other));

    s.remove(new X(N+30));
    s.add(new X(N+60));
    assertTrue(!s.containsAll(other));

    other.remove(new X(N+30));
    s.remove(new X(33));
    assertFalse(s.containsAll(other));
  }
  /**********************************************************************/
  public void test6() throws Exception {

    // trivial test of .isEmpty()
    Set<X> s = newSet();
    assertTrue(s.isEmpty());

    // another one, other result expected
    for(int i=0; i<1000; i+=3) s.add(new X(i));
    assertFalse(s.isEmpty());

    // trival test of .clear()
    Set<X> other = newSet(s);
    other.clear();
    assertTrue(other.isEmpty());

    // check if we can .addAll() after a .clear()
    other.addAll(s);
    assertTrue(other.equals(s));


    // check that remove returns the correct value, i.e. true iff
    // there was something to remove.
    for(int i=0; i<1000; i++) {
      assertEquals( i%3==0, other.remove(new X(i)));
    }
    assertTrue(other.isEmpty());
  }
  /**********************************************************************/
  public void test_UpsetIterator() throws Exception {
    // let the iterator shout
    Set<X> s = newSet();
    for(int i=0; i<100; i++) {
      s.add(new X(i));
    }
    Iterator<X> it = s.iterator();
    int i = 0;

    try {
      while( it.hasNext() ) {
	it.next();
	if( i==37 ) {
	  it.remove();
	  it.remove();
	}
	i += 1;
      }
      fail("excpected IllegalStateException");
    } catch( Exception e ) {
      assertTrue(e instanceof IllegalStateException);
    }

    while( it.hasNext() ) it.next();
    try {
      it.next();
      fail("excpected NoSuchElementException");
    } catch( Exception e ) {
      assertTrue(e instanceof NoSuchElementException );
    }
  }
  /**********************************************************************/
  public void test7() throws Exception {
    Set<Integer> s = newSet();
    for(int i=0; i<1000; i++) {
      if( i%7==0 ) continue;
      s.add(new Integer(i));
    }

    // do we have just the right elements in ?
    for(int i=0; i<1000; i++) {
      assertEquals(i%7!=0, s.contains(new Integer(i)));
    }

    // add elements which are already in
    for(int i=0; i<1000; i++) {
      assertEquals(i%7==0, s.add(new Integer(i)));
    }
  }
  /**********************************************************************/
  public void test_Interator() throws Exception {
    Set<Integer> s = newSet();
    int size = 0;
    for(int i=0; i<1000; i++) {
      if( i%7==0 ) continue;
      s.add(new Integer(i));
      size += 1;
    }
    Iterator<Integer> it = s.iterator();
    int i = 0;
    while( it.hasNext() ) {
      i += 1;
      Integer I = (Integer)it.next();
      assertFalse(I.intValue()%7==0);
    }
    assertEquals(size, i);
  }
  /**********************************************************************/
  public void test_toArray1() throws Exception {
    Set<Integer> s = newSet();
    int size = 0;
    for(int i=0; i<1000; i++) {
      if( i%7==0 ) continue;
      s.add(new Integer(i));
      size += 1;
    }
    assertEquals(size, s.size());
    Integer[] a = s.toArray(new Integer[s.size()]);
    HashSet<Integer> h = new HashSet<>();
    for(int i=0; i<a.length; i++) h.add(a[i]);

    assertTrue(s.equals(h));
    assertTrue(h.equals(s));
  }
  /**********************************************************************/
  public void test_toArray2() throws Exception {
    Set<Integer> s = newSet();
    for(int i=0; i<1000; i++) {
      if( i%7==0 ) continue;
      s.add(new Integer(i));
    }
    Integer[] a = s.toArray(new Integer[0]);
    HashSet<Integer> h = new HashSet<>();
    for(int i=0; i<a.length; i++) h.add(a[i]);

    assertTrue(s.equals(h));
    assertTrue(h.equals(s));


    Integer[] x = new Integer[1000];
    a = s.toArray(x);
    for(int i=s.size(); i<1000; i++) {
      assertTrue(""+i, null==a[i]);
    }
    assertTrue(x==a);
    h = new HashSet<>();
    for(int i=0; i<s.size(); i++) h.add(a[i]);

    assertTrue(s.equals(h));
    assertTrue(h.equals(s));
  }
  /**********************************************************************/
  public void test_massiveAddAll() throws Exception {
    Set<X> s = newSet(0);
    Set<X> s2 = newSet();
    for(int i=0; i<1000; i++) s2.add(new X(i));
    s.addAll(s2);

    Set<X> s3 = newSet();
    for(int i=-4000; i<0; i++) s3.add(new X(i));
    s.addAll(s3);

    assertTrue(s.containsAll(s2));
    assertTrue(s.containsAll(s3));
    assertFalse(s2.containsAll(s));
    assertFalse(s3.containsAll(s));
    s2.addAll(s3);
    assertEquals(s, s2);

  }
  /*+******************************************************************/

}

