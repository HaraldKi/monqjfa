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

import java.util.Map;
import monq.stuff.ArrayUtil;

/**
 * <p>This class takes care of collecting the states visited during a
 * match. Should the need arise, its <code>analyze</code> method
 * figures out the sub-automata which were visited and arranges for
 * the associated submatches to be stored in a
 * <code>TextStore</code>.</p>
 *
 *
 * @author &copy; 2004 Harald Kirsch
 */
class SubmatchData {
  // collects states we run through in match. If this turns out to be
  // non-empty, the above two come into play.
  // REMARK: This was a java.util.Stack before. Changing it to a plain
  // array and to my own resizing when necessary improved performance
  // by 16% on a typical text filtering application.
  private Map[] subInfos = new Map[5];
  private boolean haveSubs;

  // I allow access to this to allow a client to trim back to a
  // smaller size. The client is anyway only Dfa.match().
  int size = 0;

  // These are used and reused in analyze(). Currently occupied space
  // is kept there locally. Variable posPairs contains start/end pairs
  // indicating a substring relating to a submatch. As long as only
  // the start of a submatch was found, the end is kept at -1.
  private FaSubinfo[] activeSubs = new FaSubinfo[5];
  private int[] posPairs = new int[10];

  /**********************************************************************/
  void reset() {
    size = 0;
    haveSubs = false;
  }
  /**********************************************************************/
  void add(FaState s) {
    Map m = s.getSubinfos();
    haveSubs |= (m!=null);
    // NOTE: Just waiting for the exception seems to be faster than
    // an explicit test (last time I measured). Seems logical,
    // because the JVM will do the test anyway, whether I checked or
    // not.
    try {
      subInfos[size] = m;
    } catch( ArrayIndexOutOfBoundsException e ) {
      int newsize = size + 5 + size/10;
      subInfos = (Map[])ArrayUtil.resize(subInfos, newsize);
      subInfos[size] = m;
    }
    size += 1;
  }
  /**********************************************************************/
  /**
   * <p>after a match, this is called to find those subgroups which
   * belong to <code>a</code>. The respective submatches are recorded
   * in <code>out</code>, which must contain already the string
   * information.</p>
   */
  void analyze(TextStore out, FaAction a) {
    // If we have no subautomaton information at all, choose the fast
    // exit.
    if( !haveSubs ) return;
					 
    //System.out.println("  analyzing for "+a+" (size="+size+") "+out);
    // This is tedious, because even when we fish for the specific
    // action a, we get a list (in fact an FaSubinfo[]) back which can
    // contain information on more than one submatch. As a
    // consequence, we must be prepared to follow several developing
    // submatches in parallel.
    // Developing submatches are stored in activeSubs and posPairs
    // which are fields of this object.

    // currently used space in activeSubs and posPairs is held here
    int used = 0;

    // We should loop over all subInfos we have collected during
    // matching. Instead, however we use the minimum of the string
    // length and 'size' to allow some minimal tampering with the text
    // in an invoke()-callback before it is handed down to us for
    // analysis. 
    // Remember that Subinfos of 'null' were recorded to keep in sync
    // with the number of characters recorded.
    int analyseSize = size;
    if( analyseSize>out.length()+1 ) analyseSize = out.length()+1;

    for(int i=0; i<analyseSize; i++) {
      // subInfos for action a at character i, may end up being null
      FaSubinfo[] ary = null;

      // length of the above such that 0 shields a null value of the
      // above. 
      int hereLen = 0;

      Map sub = subInfos[i];
      if( sub!=null ) {
	ary = (FaSubinfo[])sub.get(a);
	if( ary!=null ) hereLen = ary.length;
      }

      // activeSubs as well as ary are sorted according to
      // FaSubinfo.id, so we can proceed in a kind of merge-sort
      // fashion.
      //   System.out.println("   "+i+" has "+hereLen+" subbies");
      int u = 0, n = 0;
      while( u<used || n<hereLen ) {
	int c;
	if( u==used ) c=1;
	else if( n==hereLen ) c=-1;
	else c = activeSubs[u].compareTo(ary[n]);
// 	System.out.print("u="+u+", n="+n+", c="+c);
// 	if( u<used ) {
// 	  System.out.print(", f[u]="+activeSubs[u]+
// 			   "("+posPairs[2*u]+","+posPairs[2*u+1]+")");
// 	}
// 	if( n<ary.length ) System.out.print(", g[n]="+ary[n]);
// 	System.out.println();

	if( c==0 ) {
	  // u and n denote the same subgroup, i.e. u is being
	  // extended if n is an inner node
	  if( ary[n].isInner() ) {
	    if( ary[n].isStop() ) posPairs[2*u+1] = i;
	    u += 1;
	    n += 1;
	    continue;
	  } 
	  if( ary[n].isStart() ) {
	    // ok, lets restart u after possibly recording it, if it
	    // had a stop before
	    if( posPairs[2*u+1]>=0 ) {
	      out.addPart(posPairs[2*u], posPairs[2*u+1],
			  activeSubs[u].id());
	      //System.out.println("restart ("+activeSubs[u].id()+"):"+out);
	    }
	    posPairs[2*u] = i;
	    posPairs[2*u+1] = -1;
	    n += 1;
	    u += 1;
	    continue;
	  }
	  // we should never come here
	  // FIX ME: in fact I cannot prove this easily, so I wait to
	  // be screwed.
	  throw new Error("screwed");
	}
	    
	if( c<0 ) {
	  // the group of u is discontinued. It may or may not get
	  // recorded. 
	  if( posPairs[2*u+1]>=0 ) {
	    out.addPart(posPairs[2*u], posPairs[2*u+1],
			activeSubs[u].id());
	    //System.out.println("discontinued:"+out);
	  }
	  ArrayUtil.delete(activeSubs, u, 1);
	  ArrayUtil.delete(posPairs, 2*u, 2);
	  used -= 1;
	} else {
	  // the group of n is new and needs to be initialized, if it
	  // is a start node
	  if( ary[n].isStart() ) {
	    //System.out.println("Starting "+ary[n].getID()+" at "+i);
	    if( used>=activeSubs.length ) {
	      activeSubs 
		= (FaSubinfo[])ArrayUtil.resize(activeSubs, used+5);
	      posPairs = ArrayUtil.resize(posPairs, used+10);
	    }
	    ArrayUtil.insert(activeSubs, u, ary[n]);
	    ArrayUtil.insert(posPairs, 2*u, i);
	    ArrayUtil.insert(posPairs, 2*u+1, -1);
	    u += 1;
	    used += 1;
	  }
	  n += 1;
	}
      }
    }
    // There may be left over finished groups which must be recorded
    for(int u=0; u<used; u++) {
      if( posPairs[2*u+1]>=0 ) {
	out.addPart(posPairs[2*u], posPairs[2*u+1],
		    activeSubs[u].id());
	//System.out.println("at end:"+out);
      }
    }

  }
  /**********************************************************************/
}

  
