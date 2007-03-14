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

import java.io.Serializable;

/**
 * <p>helps to mark subgraphs in a DFA/NFA.</p>
 *
 * @author &copy; 2004, 2005 Harald Kirsch
 */
class FaSubinfo implements Comparable, Serializable {
 
  private static final byte SUBINNER = 0x1;
  private static final byte SUBSTART = 0x2;
  private static final byte SUBSTOP = 0x4;
  private static final String[] TYPESTRINGS = {
    "?", "@", "[", "[@", "]", "@]", "[]", "[@]",
  };
  //private static int nextID = 0;
  //private static int uniqueID() { return nextID++;}

  /**********************************************************************/
  private byte type;
  private byte id;

  /**********************************************************************/
  public int compareTo(Object other) {
    FaSubinfo o = (FaSubinfo)other;
    if( id<o.id ) return -1;
    if( id>o.id ) return 1;
    return 0;
  }

  public FaSubinfo(FaSubinfo other) {
    this.type = other.type;
    this.id = other.id;
  }

  private FaSubinfo(byte id, byte type) {
    this.id = id;
    this.type = type;
  }

  public byte id() {return id;}

  public static FaSubinfo start(byte id) { 
    return new FaSubinfo(id, SUBSTART); 
  }
  public static FaSubinfo stop(byte id)  { 
    return new FaSubinfo(id, (byte)(SUBSTOP));  
  }
  public static FaSubinfo inner(byte id) { 
    return new FaSubinfo(id, SUBINNER); 
  }

  public void merge(FaSubinfo other) {
    type |= other.type;
  }
  public boolean isStart() {return (type&SUBSTART)!=0;}
  public boolean isStop() {return (type&SUBSTOP)!=0;}
  public boolean isInner() {return (type&SUBINNER)!=0;}


  public String typeString() {
    return TYPESTRINGS[type];    
  }

  public String toString() {
    return super.toString()+"["+id+","+typeString()+"]";
  }
}
