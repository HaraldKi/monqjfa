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

import java.util.*;
import java.lang.reflect.*;
import java.io.PrintStream;

/**
 * <p>estimator for the size of data structures. The sizes of
 * individual elements used to compute the overall size of a data
 * structure are estimates only derived on 1.4.2 on a 32bit Linux.</p>
 *
 * <p>The implementation will be changed to
 * <code>java.lang.instrument.Instrumentation</code> as soon as we
 * switch to Java 5.</p>
 *
 * @author &copy; 2005 Harald Kirsch
 */
public class Sizeof {

  // no need for this to show up in the docs
  private Sizeof() {}

  static final Map<Object,Integer> pSizes;
  static {
    pSizes = new HashMap<Object,Integer>();
    pSizes.put(Boolean.TYPE, new Integer(1));
    pSizes.put(Character.TYPE, new Integer(2));
    pSizes.put(Byte.TYPE, new Integer(1));
    pSizes.put(Short.TYPE, new Integer(2));
    pSizes.put(Integer.TYPE, new Integer(4));
    pSizes.put(Long.TYPE, new Integer(8));
    pSizes.put(Float.TYPE, new Integer(4));
    pSizes.put(Double.TYPE, new Integer(8));
    pSizes.put(Void.TYPE, new Integer(1));
  }
  private static final class Root {}

  private static void sizeof(Object obj, IdentityHashMap<Object,Object> known, 
			     Map<Class,Map<Class,Pair>> types) 
  {
    // The stack always contains a parent class and an object. The
    // parent class may be null
    Stack<Object> stack = new Stack<Object>();
    stack.push(new Root().getClass());
    stack.push(obj);
    int rounds = 0;
    while( !stack.empty() ) {
      Object o = stack.pop();
      Class parentClass = (Class)stack.pop();
      known.put(o, o);
      rounds += 1;
      if( rounds%100000==0 ) {
	System.err.println("rounds: "+rounds+", stacksize:"+stack.size());
      }
      int size = 8;
      Class oc = o.getClass();

      // arrays need totally different handling
      if( oc.isArray() ) {
	// arrays seem to have a minimum size of 16
	size += 8;
	int alen = Array.getLength(o);
	Class ec = oc.getComponentType();
	Integer l = (Integer)pSizes.get(ec);
	if( l!=null ) {
	  // array of primitive types
	  size = size + alen*l.intValue();
	} else {
	  size = size + 4*alen;
	  Object[] oa = (Object[])o;
	  for(int i=0; i<alen; i++) {
	    Object child = oa[i];
	    if( child!=null && !known.containsKey(child) ) {
	      // don't use arrays as parents (does not tell a lot)
	      stack.push(parentClass);
	      stack.push(child);
	    }
	  }
	}

      } else {
	// handle o not being an array

	// iterate up the inheritance hierarchy
	Class c = oc;
	while( c!=null ) {
	  Field[] fields = c.getDeclaredFields();
	  for(int i=0; i<fields.length; i++) {
	    Field f = fields[i];
	    if( Modifier.isStatic(f.getModifiers()) ) continue;
	  
	    Class fc = f.getType();
	    
	    // take care of primitive types
	    // FIX ME: this disregards alignment issues
	    Integer l = (Integer)pSizes.get(fc);
	    if( l!=null ) {
	      size+=l.longValue();
	      continue;
	    }
	    
	    size+=4;
	    Object child;
	    f.setAccessible(true);
	    try {
	      child = f.get(o);
	      if( child==null || known.containsKey(child) ) continue;
	      stack.push(oc);	// should this rather be c?
	      stack.push(child);
	      //System.out.println(""+fc+" in "+c);
	    } catch( IllegalAccessException e ) {
	      System.err.println("cannot access "+f+" in "+oc);
	    }
	    
	  }
	  c = c.getSuperclass();
	}
	
      }
     
      Map<Class,Pair> parents = types.get(oc);
      if( parents==null ) types.put(oc, parents=new HashMap<Class,Pair>());
      Pair p = (Pair)parents.get(parentClass);
      if( p==null ) {
	p = new Pair();
	parents.put(parentClass, p);
      }
      p.count +=1 ;
      p.size += size;
    }//while stack not empty
  }
  /**********************************************************************/
  /**
   * recursively delves into the object graph <code>o</code> and
   * returns a <code>Hashtable</code> with information about object
   * sizes and counts. The keys used in the result are
   * <code>java.lang.Class</code> objects.
   * Each key <em>k</em> is mapped to a <code>java.util.Map</code>
   * object which pairs a class object <em>r</em> and a {@link
   * Sizeof.Pair}. The <code>Pair</code> describes how often
   * <em>k</em> appears as a field in an object of type <em>r</em>. In
   * other words: within the object graph, objects of type <em>r</em>
   * are parents of objects of type <em>k</em>.</p>
   */
  public static Map<Class, Map<Class,Pair>> sizeof(Object o) {
    Map<Class, Map<Class,Pair>> types = new HashMap<Class, Map<Class,Pair>>();
    sizeof(o, new IdentityHashMap<Object,Object>(), types);
    return types;
  }
  /**
   * prints the result produced with {@link #sizeof sizeof()}.
   */
  public static void printTypes(PrintStream out, Map<Class, Map<Class,Pair>> types) {
    long count=0, size=0;
    for(Class c: types.keySet()) {
      Map<Class,Pair> parents = types.get(c);

      Iterator pit = parents.keySet().iterator();
      while( pit.hasNext() ) {
	Class parenClass = (Class)pit.next();
	Pair p = (Pair)parents.get(parenClass);
	String pname = parenClass.getName();
	out.println(""+p.count+" "+p.size+" "+c.getName()+" "+pname);
	count += p.count;
	size += p.size;
      }
    }
    out.println(""+count+" "+size+ " TOTAL");
  }
  /**********************************************************************/
  /**
   * a pair of count and size describing use and size of a data type.
   */
  public static class Pair {
    /**
     * number of objects of a certain type.
     */
    public int count;
    /**
     * accumulated size of objects counted.
     */
    public long size;
  }
//   public static void main(String[] argv) 
//     throws ReSyntaxException
//   {
//     Nfa nfa = new Nfa("bla", monq.jfa.actions.Copy.COPY);
//     Object[] o = new Object[2];
//     o[1] = new Integer(1);
//     Hashtable types = sizeof(nfa);
//     printTypes(System.out, types);
//   }
}
