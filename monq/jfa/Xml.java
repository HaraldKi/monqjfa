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

import monq.jfa.actions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>provides static fields and methods which help to create regular
 * expressions needed to parse XML.</p>
 *
 * <p>Definitions as well as names are taken from
 * <a href="http://www.w3.org/TR/REC-xml">XML 2nd Edition</a>.
 * @author (C) 2003 Harald Kirsch
 * @version $Revision#, $Date: 2007-03-09 21:21:40 $
 */
public final class Xml {
  
  // no constructor yet
  ///CLOVER:OFF
  private Xml() {  }
  //CLOVER:ON

  /** a {@link Dfa} which matches a {@link #Name}. */
  public static final Dfa DFA_Name;
  /** a {@link Dfa} which matches a {@link #S}. */
  public static final Dfa DFA_S;
  /** a {@link Dfa} which matches a {@link #Eq}. */
  public static final Dfa DFA_Eq;
  /** a {@link Dfa} which matches a {@link #AttValue}. */
  public static final Dfa DFA_AttValue;

  /**
   * use this to retrieve the name of the tag from the map passed to
   * {@link #splitElement(java.util.Map,java.lang.StringBuffer,int)
   * splitElement(Map,StringBuffer,int)}.
   */
  public static final String TAGNAME = "<";
  /**
   * use this to retrieve the element content from the map passed to
   * {@link #splitElement(java.util.Map,java.lang.StringBuffer,int)
   * splitElement(Map,StringBuffer,int)}.
   */
  public static final String CONTENT = ">";

  /**********************************************************************/
  //
  // STag
  //
  /**
   * <p>returns a string matching an XML start tag the name of which
   * matches the given regular expression. To get a regular expression
   * which matches any tag name, call
   * {@link #STag()}.</p>
   */
  public static String STag(String nameRe) {
    return startOrEmptyTag(nameRe, false);
  }
  /**
   * returns a regular expression matching any start tag.
   */
  public static String STag() { return STag(Name); }

  /**
   * <p>creates an {@link Nfa} to match any XML start tag. It is a
   * convenience shortcut for</p>
   * <blockquote>
   *   <code>STagNfa({@link Xml#Name}, handler)</code>.
   * </blockquote>
   */
//   public static Nfa STagNfa(Formatter handler) {
//     Nfa result;
//     try {
//       result = STagNfa(Name, handler); 
//     } catch( ReSyntaxException e) {
//       ///CLOVER:OFF
//       throw new Error("this cannot happen", e);
//       ///CLOVER:ON
//     }
//     return result;
//   }

  /**
   * <p>creates an {@link Nfa} to match an XML start tag with a given
   * tag name. An action is set up and added to the <code>Nfa</code>
   * which splits the start tag into tag name and attribute/value
   * pairs by means of a {@link #splitSTag splitSTag()} and then calls the
   * <code>handler</code> with the {@link TextStore} filled by the
   * splitter.</p>
   *
   * <p><b>Hint:</b> Because the handler is a {@link Formatter}, it does
   * not get the calling {@link DfaRun} passed in. In case this is
   * necessary, don't use this convenience function, but write your
   * own {@link FaAction} which does have access to the calling
   * <code>DfaRun</code> and use <code>splitSTag()</code> to split
   * the match into the parts described above.</p>
   *
   * @param nameRe is the regular expression to match the name of the
   * start tag. Most of the time it will be just a string.
   * 
   * @param handler is the callback invoked by the action added to the
   * <code>Nfa</code>. 
   *
   * @throws ReSyntaxException if the supplied regular expression
   * contains a syntax error.
   * 
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-STag">XML
   * start tag</a>
   */
//   public static Nfa STagNfa(String nameRe, 
// 			    Formatter handler) 
//     throws ReSyntaxException {

//     String re = STag(nameRe);
//     FaAction a = new Printf(STAGSPLITTER, handler, 0);
//     return new Nfa(re, a);
//   }
  /**********************************************************************/
  //
  // EmptyElemTag
  //
  /**
   * <p>returns a string matching an empty element XML tag the name of
   * which matches the given regular expression. To get a regular
   * expression which matches any tag name, call
   * {@link #EmptyElemTag()}.</p>
   *
   * <p>Reminder: An empty element tag is a tag which ends
   * in "<code>/&gt;</code>", like "<code>&lt;br/&gt;</code>".</p>
   */
  public static String EmptyElemTag(String nameRe) {
    return startOrEmptyTag(nameRe, true);
  }
  /**
   * <p>returns a regular expression matching any empty element.</p>
   */
  public static String EmptyElemTag() { return EmptyElemTag(Name); }

 /**
   * <p>creates an {@link Nfa} to match any XML empty element tag. It
   * is a  convenience shortcut for</p>
   * <blockquote>
   *   <code>EmptyElemTag(Name, handler)</code>.
   * </blockquote>
   */
//   public static Nfa EmptyElemTagNfa(Formatter handler) {
//     Nfa result;
//     try {
//       result = EmptyElemTagNfa(Name, handler); 
//     } catch( ReSyntaxException e) {
//       ///CLOVER:OFF
//       throw new Error("this cannot happen", e);
//       ///CLOVER:ON
//     }
//     return result;
//   }
  /**
   * <p>creates an {@link Nfa} to match an XML empty element tag with
   * a given tag name. For more details see {@link
   * #STagNfa(String,Formatter)}.</p>
   *
   * <p><b>Hint:</b> see <em>Hint</em> for {@link
   * #STagNfa(String,Formatter)}</p> 
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-EmptyElemTag">XML
   * empty element tag</a>
   */
//   public static Nfa EmptyElemTagNfa(String nameRe, Formatter handler) 
//     throws ReSyntaxException
//   {
//     String re = EmptyElemTag(nameRe);
//     FaAction a = new Printf(STAGSPLITTER, handler, 0);
//     return new Nfa(re, a);
//   }
  /**********************************************************************/
  /**
   * <p>creates a regular expression to match a whole XML element
   * including the start tag with its optional attributes, the content
   * and the end tag. The method has its name because it does not
   * match an XML element according to the strict rules of the
   * standard. The following things can go wrong:</p>
   *
   * <ol>
   * <li>If <code>nameRe</code> is indeed a regular expression and
   * not just a string, you cannot be sure that the start tag and
   * the end tag are indeed the same. For example with
   * <code>nameRe="[AB]"</code> the text 
   * <code>&lt;A&gt;hallo&lt;/B&gt;</code> will be matched although it
   * is not well formed XML.</li>
   * <li>Regular expressions cannot handle nested elements well. Given
   * the text  
   * <code>&lt;A&gt;&lt;A&gt;..&lt;/A&gt;&lt;/A&gt;</code>
   * only the part up to the first closing tag, namely
   * <code>&lt;A&gt;&lt;A&gt;..&lt;/A&gt;</code>, will be
   * matched. Matching an element which contains elements with
   * different names, however, is not a problem.</li> 
   * </ol>
   */
  public static String GoofedElement(String nameRe) {
    return startOrEmptyTag(nameRe, false) + "(.*" + ETag(nameRe) + ")!";
  }
  /**
   * <p>creates an {@link Nfa} to match a whole XML element including
   * the start tag with its optional attributes, the content and the
   * end tag.
   * The method has its name because it does not match an XML element
   * according to the strict rules of the standard. The following
   * things can go wrong:</p>
   *
   * <ol>
   * <li>If <code>nameRe</code> is indeed a regular expression and
   * not just a string, you cannot be sure that the start tag and
   * the end tag are indeed the same. For example with
   * <code>nameRe="[AB]"</code> the text 
   * <code>&lt;A&gt;hallo&lt;/B&gt;</code> will be matched although it
   * is no well formed XML.</li>
   * <li><code>GoofedElementNfa</code> creates a plain regular expression
   * and therefore cannot handle nested elements with the same name
   * correctly. Given
   * the text  
   * <code>&lt;A&gt;&lt;A&gt;..&lt;/A&gt;&lt;/A&gt;</code>
   * only the part up to the first closing tag, namely
   * <code>&lt;A&gt;&lt;A&gt;..&lt;/A&gt;</code>, will be
   * matched. Matching an element which contains elements with
   * different names, however, is not a problem.</li> 
   * </ol>
   *
   * <p><b>Hint:</b> see <em>Hint</em> for {@link
   * #STagNfa(String,Formatter)} but use {@link #splitElement}
   * instead of <code>splitSTag</code>.</p> 
   *
   */
//   public static Nfa GoofedElementNfa(String nameRe, Formatter handler) 
//     throws ReSyntaxException 
//   {
//     FaAction a = new Printf(ELEMENTSPLITTER, handler, 0);
//     return new Nfa(GoofedElement(nameRe), a);
//   }
  /**********************************************************************/
  //
  // ETag
  //
  /**
   * <p>returns a string matching an XML end tag the name of which
   * matches the given regular expression. To get a regular expression
   * which matches any tag name, call
   * {@link #ETag()}.</p>
   */
  public static String ETag(String nameRe) {
    return "</("+nameRe+")"+S+"?>";
  }
  /**
   * returns a regular expression which matches any end tag.
   */
  public static String ETag() { return ETag(Name); }

  /**
   * creates an {@link Nfa} to match an XML end tag with the given tag
   * name and adds the given action to the <code>Nfa</code>.
   *
   * @throws ReSyntaxException if <code>nameRe</code> contains a
   * syntax error.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-ETag">XML
   * end tag</a>
   */
//   public static Nfa ETagNfa(String nameRe, FaAction a) 
//     throws ReSyntaxException
//   {
//     return new Nfa(ETag(nameRe), a);
//   }
//   /**
//    * creates an {@link Nfa} to match any XML end tag and adds the
//    * given action to the <code>Nfa</code>.
//    */
//   public static Nfa ETagNfa(FaAction a) {
//     try {
//       return new Nfa("</"+Name+S+"?>", a);
//     } catch( ReSyntaxException e) {
//       ///CLOVER:OFF
//       throw new Error("this cannot happen", e);
//       ///CLOVER:ON
//     }
//   }
  /**********************************************************************/
  // combines functionality to create a start tag or an empty element
  // tag. 
  private static String startOrEmptyTag(String nameRe, 
					boolean emptyElement) {
    // FIX ME: does it make sense to forbid an empty string
    // here. Rather it would make sense to check whether the empty
    // string can be matched, but is it worth the effort?
    //if( nameRe.length()==0 ) 
    //  throw new IllegalArgumentException("tagname may not be empty");

    String tail = emptyElement ? "/>" : ">";
    return "<("+nameRe+")("+S+Attribute+")*"+S+"?"+tail;
  }

  /**
   * a regular expression matching an XML S, which is white space.

   * @see <a href="http://www.w3.org/TR/REC-xml#NT-S">XML syntax: S</a>
   */
  public static final String S = "([ \t\r\n]+)";

  /**
   * @see
   * <a href="http://www.w3.org/TR/REC-xml#NT-Ideographic">XML syntax:
   * Ideographic</a>
   */
  private static final String Ideographic = 
    "[\u4E00-\u9FA5\u3007\u3021-\u3029]";

  /**
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-BaseChar">XML
   * syntax: BaseChar</a>
   */
  private static final String BaseChar = "["
    +"\u0041-\u005A\u0061-\u007A\u00C0-\u00D6"
    +"\u00D8-\u00F6\u00F8-\u00FF\u0100-\u0131" 
    +"\u0134-\u013E\u0141-\u0148\u014A-\u017E" 
    +"\u0180-\u01C3\u01CD-\u01F0\u01F4-\u01F5" 
    +"\u01FA-\u0217\u0250-\u02A8\u02BB-\u02C1" 
    +"\u0386\u0388-\u038A\u038C"
    +"\u038E-\u03A1\u03A3-\u03CE\u03D0-\u03D6"
    +"\u03DA\u03DC\u03DE"
    +"\u03E0\u03E2-\u03F3\u0401-\u040C"
    +"\u040E-\u044F\u0451-\u045C\u045E-\u0481"
    +"\u0490-\u04C4\u04C7-\u04C8\u04CB-\u04CC"
    +"\u04D0-\u04EB\u04EE-\u04F5\u04F8-\u04F9"
    +"\u0531-\u0556\u0559\u0561-\u0586"
    +"\u05D0-\u05EA\u05F0-\u05F2\u0621-\u063A"
    +"\u0641-\u064A\u0671-\u06B7\u06BA-\u06BE"
    +"\u06C0-\u06CE\u06D0-\u06D3\u06D5"
    +"\u06E5-\u06E6\u0905-\u0939\u093D"
    +"\u0958-\u0961\u0985-\u098C\u098F-\u0990"
    +"\u0993-\u09A8\u09AA-\u09B0\u09B2"
    +"\u09B6-\u09B9\u09DC-\u09DD\u09DF-\u09E1"
    +"\u09F0-\u09F1\u0A05-\u0A0A\u0A0F-\u0A10"
    +"\u0A13-\u0A28\u0A2A-\u0A30\u0A32-\u0A33"
    +"\u0A35-\u0A36\u0A38-\u0A39\u0A59-\u0A5C"
    +"\u0A5E\u0A72-\u0A74\u0A85-\u0A8B"
    +"\u0A8D\u0A8F-\u0A91\u0A93-\u0AA8"
    +"\u0AAA-\u0AB0\u0AB2-\u0AB3\u0AB5-\u0AB9"
    +"\u0ABD\u0AE0\u0B05-\u0B0C"
    +"\u0B0F-\u0B10\u0B13-\u0B28\u0B2A-\u0B30"
    +"\u0B32-\u0B33\u0B36-\u0B39\u0B3D"
    +"\u0B5C-\u0B5D\u0B5F-\u0B61\u0B85-\u0B8A"
    +"\u0B8E-\u0B90\u0B92-\u0B95\u0B99-\u0B9A"
    +"\u0B9C\u0B9E-\u0B9F\u0BA3-\u0BA4"
    +"\u0BA8-\u0BAA\u0BAE-\u0BB5\u0BB7-\u0BB9"
    +"\u0C05-\u0C0C\u0C0E-\u0C10\u0C12-\u0C28"
    +"\u0C2A-\u0C33\u0C35-\u0C39\u0C60-\u0C61"
    +"\u0C85-\u0C8C\u0C8E-\u0C90\u0C92-\u0CA8"
    +"\u0CAA-\u0CB3\u0CB5-\u0CB9\u0CDE"
    +"\u0CE0-\u0CE1\u0D05-\u0D0C\u0D0E-\u0D10"
    +"\u0D12-\u0D28\u0D2A-\u0D39\u0D60-\u0D61"
    +"\u0E01-\u0E2E\u0E30\u0E32-\u0E33"
    +"\u0E40-\u0E45\u0E81-\u0E82\u0E84"
    +"\u0E87-\u0E88\u0E8A\u0E8D"
    +"\u0E94-\u0E97\u0E99-\u0E9F\u0EA1-\u0EA3"
    +"\u0EA5\u0EA7\u0EAA-\u0EAB"
    +"\u0EAD-\u0EAE\u0EB0\u0EB2-\u0EB3"
    +"\u0EBD\u0EC0-\u0EC4\u0F40-\u0F47"
    +"\u0F49-\u0F69\u10A0-\u10C5\u10D0-\u10F6"
    +"\u1100\u1102-\u1103\u1105-\u1107"
    +"\u1109\u110B-\u110C\u110E-\u1112"
    +"\u113C\u113E\u1140"
    +"\u114C\u114E\u1150"
    +"\u1154-\u1155\u1159\u115F-\u1161"
    +"\u1163\u1165\u1167"
    +"\u1169\u116D-\u116E\u1172-\u1173"
    +"\u1175\u119E\u11A8"
    +"\u11AB\u11AE-\u11AF\u11B7-\u11B8"
    +"\u11BA\u11BC-\u11C2\u11EB"
    +"\u11F0\u11F9\u1E00-\u1E9B"
    +"\u1EA0-\u1EF9\u1F00-\u1F15\u1F18-\u1F1D"
    +"\u1F20-\u1F45\u1F48-\u1F4D\u1F50-\u1F57"
    +"\u1F59\u1F5B\u1F5D"
    +"\u1F5F-\u1F7D\u1F80-\u1FB4\u1FB6-\u1FBC"
    +"\u1FBE\u1FC2-\u1FC4\u1FC6-\u1FCC"
    +"\u1FD0-\u1FD3\u1FD6-\u1FDB\u1FE0-\u1FEC"
    +"\u1FF2-\u1FF4\u1FF6-\u1FFC\u2126"
    +"\u212A-\u212B\u212E\u2180-\u2182"
    +"\u3041-\u3094\u30A1-\u30FA\u3105-\u312C"
    +"\uAC00-\uD7A3"
    +"]";

  /**
   * a regular expression matching an XML CombiningChar.
   *
   * @see <a
   * href="http://www.w3.org/TR/REC-xml#NT-CombiningChar>XML syntax: CombiningChar</a> 
   */
  public static final String CombiningChar = "["
    +"\u0300-\u0345\u0360-\u0361\u0483-\u0486"
    +"\u0591-\u05A1\u05A3-\u05B9\u05BB-\u05BD"
    +"\u05BF\u05C1-\u05C2\u05C4"
    +"\u064B-\u0652\u0670\u06D6-\u06DC"
    +"\u06DD-\u06DF\u06E0-\u06E4\u06E7-\u06E8"
    +"\u06EA-\u06ED\u0901-\u0903\u093C"
    +"\u093E-\u094C\u094D\u0951-\u0954"
    +"\u0962-\u0963\u0981-\u0983\u09BC"
    +"\u09BE\u09BF\u09C0-\u09C4"
    +"\u09C7-\u09C8\u09CB-\u09CD\u09D7"
    +"\u09E2-\u09E3\u0A02\u0A3C"
    +"\u0A3E\u0A3F\u0A40-\u0A42"
    +"\u0A47-\u0A48\u0A4B-\u0A4D\u0A70-\u0A71"
    +"\u0A81-\u0A83\u0ABC\u0ABE-\u0AC5"
    +"\u0AC7-\u0AC9\u0ACB-\u0ACD\u0B01-\u0B03"
    +"\u0B3C\u0B3E-\u0B43\u0B47-\u0B48"
    +"\u0B4B-\u0B4D\u0B56-\u0B57\u0B82-\u0B83"
    +"\u0BBE-\u0BC2\u0BC6-\u0BC8\u0BCA-\u0BCD"
    +"\u0BD7\u0C01-\u0C03\u0C3E-\u0C44"
    +"\u0C46-\u0C48\u0C4A-\u0C4D\u0C55-\u0C56"
    +"\u0C82-\u0C83\u0CBE-\u0CC4\u0CC6-\u0CC8"
    +"\u0CCA-\u0CCD\u0CD5-\u0CD6\u0D02-\u0D03"
    +"\u0D3E-\u0D43\u0D46-\u0D48\u0D4A-\u0D4D"
    +"\u0D57\u0E31\u0E34-\u0E3A"
    +"\u0E47-\u0E4E\u0EB1\u0EB4-\u0EB9"
    +"\u0EBB-\u0EBC\u0EC8-\u0ECD\u0F18-\u0F19"
    +"\u0F35\u0F37\u0F39"
    +"\u0F3E\u0F3F\u0F71-\u0F84"
    +"\u0F86-\u0F8B\u0F90-\u0F95\u0F97"
    +"\u0F99-\u0FAD\u0FB1-\u0FB7\u0FB9"
    +"\u20D0-\u20DC\u20E1\u302A-\u302F\u3099\u309A"
    +"]";
  /**
   * a regular expression matching an XML Extender.
   * @see <a
   * href="http://www.w3.org/TR/REC-xml#NT-Extender>XML syntax: Extender</a> 
   */
  public static final String Extender = "["
    +"\u00B7\u02D0\u02D1"
    +"\u0387\u0640\u0E46"
    +"\u0EC6\u3005\u3031-\u3035"
    +"\u309D-\u309E\u30FC-\u30FE"
    +"]";

  /**
   * a regular expression matching an XML Digit.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-Digit">XML syntax: Digit</a>
   */
  public static final String Digit = "["
    +"\u0030-\u0039\u0660-\u0669\u06F0-\u06F9"
    +"\u0966-\u096F\u09E6-\u09EF\u0A66-\u0A6F"
    +"\u0AE6-\u0AEF\u0B66-\u0B6F\u0BE7-\u0BEF"
    +"\u0C66-\u0C6F\u0CE6-\u0CEF\u0D66-\u0D6F"
    +"\u0E50-\u0E59\u0ED0-\u0ED9\u0F20-\u0F29"
    +"]";

    
  /**
   * a regular expression matching an XML Letter.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-Letter">XML syntax:
   * Letter</a>
   */
  public static final String Letter = "("+BaseChar +"|"+ Ideographic+")";
  
  /**
   * a regular expression matching an XML NameChar.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-NameChar">XML
   * syntax: NameChar</a>
   */
  public static final String NameChar =
    "("+Letter+"|"+Digit+"|[-._:]|"+CombiningChar+"|"+Extender+")";

  /**
   * a regular expression matching an XML CharRef.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-CharRef">XML
   * syntax: CharRef</a>
   */
  public static final String CharRef = "(&#([0-9]+|x[0-9a-fA-F]+);)";

  /**
   * defines the equation sign surrouned by optional space used to
   * separate attributes from their values.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-Eq">XML syntax: Eq</a>
   */
  public static final String Eq = "("+S+"?="+S+"?)";


  /**
   * defines the the regular expression which matches a tag- or
   * attribute-name.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-Name">XML syntax: Name</a>
   */
  public static final String Name = "(("+Letter+"|[_:])"+NameChar+"*)";

  /**
   * a regular expresion matching an XML EntityRef.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-EntityRef">XML
   * syntax: EntityRef</a>
   */
  public static final String EntityRef = "(&"+Name+";)";

  /**
   * a regular expression matching an XML Reference.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-Reference">XML
   * syntax: Reference</a>
   */
  public static final String Reference = "("+EntityRef+"|"+CharRef+")";

  /**
   * a regular expression matching an XML attribute value. This
   * includes the surrounding quotes.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-AttValue">XML
   * syntax: AttValue</a>
   */
  public static final String AttValue =
    "("
    +"(\""+"([^<&\"]|"+Reference+")*\")"
    +"|('"+"([^<&']|"+Reference+")*')"
    +")";

  /**
   * a regular expression matching an XML attribute including name,
   * equal sign and attribute value.
   *
   * @see <a href="http://www.w3.org/TR/REC-xml#NT-Attribute">XML
   * syntax: Attribute</a>
   */
  public static final String Attribute = "("+Name+Eq+AttValue+")";

  /**
   * <p>a regular expression matching a processing instruction. In
   * deviation from the XML standard, also the processingn instruction
   * for <code>PITarget</code> xml, i.e. <code>&lt;?xml
   * ...?&gt;</code> will be matched.</p>
   *
   * @see <a href="http://www.w3c.org/TR/2000/REC-xml-20001006#dt-pi">XML
   * syntax: PI</a>
   */
  public static final String PI =
    "("
    +"<[?]"+Name+"([?]>|"+S+"(.*[?]>)!)"
    +")";

  /**
   * a regular expression matching a CDATA section. 
   *
   * @see <a
   * href="http://www.w3c.org/TR/2000/REC-xml-20001006#NT-CDSect">XML
   * syntax: CDSect</a> 
   */
  public static final String CDSect = "(<[!]\\[CDATA\\[(.*\\]\\]>)!)";

  /**
   * <p>a regular expression matching an XML comment.</p>
   *
   * <p>FIX ME: This regular expression currently allows a double dash
   * to be part of the comment</p>
   * @see <a href="http://www.w3c.org/TR/2000/REC-xml-20001006#NT-Comment">Xml syntax: Comment</a>
   */
  public static final String Comment = "(<[!]--(.*-->)!)";


  /**
   * <p>a regular expression matching an encoding name.</p>
   * @see <a href="http://www.w3.org/TR/REC-xml/#NT-EncName">Xml
   * syntax: EncName</a>
   */
  public static final String EncName = "[A-Za-z]([A-Za-z0-9._\\-])*";

  /**
   * <p>a regular expression matching the XML Declaration. The match
   * of an XMLDecl can be taken apart with {@link
   * #splitElement(Map,StringBuffer,int)}.</p>
   *
   * @see <a href="http://www.w3.org/TR/REC-xml/#NT-XMLDecl">Xml
   * syntax: XMLDecl</a>
   */
  public static final String XMLDecl = 
    "<[?]xml"+S+"version"+Eq+"('1.0'|\"1.0\")"
    +"("+S+"encoding"+Eq+"('"+EncName+"'|\""+EncName+"\"))?"
    +"("+S+"standalone"+Eq+"('(yes|no)'|\"(yes|no)\"))?"
    +S+"?"
    +"[?]>";

  static {
    try {
      DFA_Name = new Nfa(Name, Copy.COPY).compile(DfaRun.UNMATCHED_COPY);
      DFA_S = new Nfa(S, Copy.COPY).compile(DfaRun.UNMATCHED_COPY);
      DFA_Eq = new Nfa(Eq, Copy.COPY).compile(DfaRun.UNMATCHED_COPY);
      DFA_AttValue = new Nfa(AttValue, Copy.COPY)
	.compile(DfaRun.UNMATCHED_COPY);
    } catch( ReSyntaxException e) {
      ///CLOVER:OFF
      throw new Error("this cannot happen", e);
      ///CLOVER:ON
    } catch( CompileDfaException e) {
      ///CLOVER:OFF
      throw new Error("this cannot happen", e);
      ///CLOVER:ON
    }
  }
  /**********************************************************************/
//   /**
//    * <p>splits an XML start tag into tag name, attribute names and
//    * attribute values. If <code>dst</code> is initially empty, the
//    * whole start tag will be part 0, the tag name will be part no. 1
//    * the first attribute name will be part no. 2 and the first
//    * attribute value will be part 3. If <code>dst</code> contains
//    * already parts, these part numbers will be respectivly greater.</p>
//    *
//    * <p><b>Hint:</b> If you need a {@link TextSplitter} which uses
//    * this method, use the static object {@link #STAGSPLITTER}.</p>
//    *
//    * @return the position of the first unprocessed character,
//    * guaranteed to be before or equal to the position of the
//    * terminating '&gt;'.</p>
//    *
//    * @deprecated use {@link #splitElement(Map,StringBuffer,int)} which
//    * can also deal with the {@link #XMLDecl XML declaration}
//    */
//   public static int splitSTag(TextStore dst, StringBuffer s, int start) {
//     // how much string is already stored in dst
//     int oldLen = dst.length();

//     // make a copy of the content as required by the interface
//     dst.appendPart(s, start, s.length());

//     // fetch the tag
//     int cursor = 1;
//     int end = cursor+DFA_Name.match(s, start+cursor);
//     dst.addPart(oldLen+cursor, oldLen+end);
//     cursor = end;
      
//     // repeat for zero or more attributes
//     // NOTE: according to the interface definition, we don't have to
//     // check if the syntax of the input string matches our
//     // expectations. In fact, if it does not, the following loop may
//     // not terminate.
//     while( true ) {
//       // JUNK? CharSequence attr=null, value;
      
//       // we may see some space now. It may introduce an attribute,
//       // but may also be the optional space before the closing angle
//       // bracket.
//       end = cursor+DFA_S.match(s, start+cursor);
//       if( end<cursor ) break;
//       cursor = end;
      
//       // there may be an attribute name
//       end = cursor+DFA_Name.match(s, start+cursor);
//       if( end<cursor) break;
//       dst.addPart(oldLen+cursor, oldLen+end);
//       cursor = end;
      
//       // skip the Eq	
//       end = cursor+DFA_Eq.match(s, start+cursor);
//       cursor = end;
      
//       // get AttValue
//       end = cursor+DFA_AttValue.match(s, start+cursor);
//       dst.addPart(oldLen+cursor+1, oldLen+end-1);
//       cursor = end;
//     }
//     return cursor;
//   }
  /**********************************************************************/  
  /**
   * <p>calls {@link #splitElement(Map,StringBuffer,int)} with a freshly
   * allocated <code>HashMap</code> and returns the filled map.</p>
   *
   * <p><b>Hint:</b> to prevent frequent reallocation of the map,
   * allocate it yourself and call {@link
   * #splitElement(Map,StringBuffer,int)} directly.</p>
   */
  public static HashMap splitElement(StringBuffer s, int start) {
    HashMap h = new HashMap();
    splitElement(h, s, start);
    return h;
  }
  /**********************************************************************/
  /**
   * <p>splits a start tag, a complete XML element or the {@link
   * #XMLDecl XML declaration} into its parts and fills them
   * into the given <code>Map</code>. Attributes are stored in the map
   * in the obvious way. The tagname is stored with the key {@link
   * #TAGNAME}. If the method is applied to a complete element, the
   * element's content is stored with the key {@link #CONTENT}. The
   * input, starting at <code>start</code> in <code>s</code> may be a
   * start tag, an empty element,  an element with content or the XML
   * declaration. In all
   * cases, trailing <em>garbage</em> like space does not harm and is
   * ignored.</p>
   *
   * <p>The given <code>Map</code> is not cleared by this
   * method. The content of <code>s</code> is not changed.</p>
   *
   * <p><b>Note:</b> The outcome of the method is undefined, if the
   * input is not a well formed according to the allowed input listed
   * above. An exception is trailing space.</p>
   */
  public static void splitElement(Map dst, StringBuffer s, int start) {
    // use tail of s as scratch area, need start of that area for reset
    int l = s.length();

    // skip the leading '<' immediately, start counting the length of
    // the whole XML tag
    CharSequenceCharSource in = new CharSequenceCharSource(s, start+1);
    int tlen = 1;

    // skip '?' of XMLDecl
    if( s.charAt(start+1)=='?' ) {
      in.read();
      tlen += 1;
    }

    String tagname = null;
    try {
      // fetch the tagname
      DFA_Name.match(in, s, Dfa.dummySmd);
      dst.put(TAGNAME, tagname=s.substring(l));
      tlen += s.length()-l;
      s.setLength(l);
      
      // repeat for zero or more attributes. If we are not looking at
      // space right now, we are at the end of the start tag
      while( null!=DFA_S.match(in, s, Dfa.dummySmd) ) {
	tlen += s.length()-l;
	s.setLength(l);
	
	// Despite the space, we may be at the end of the tag, because
	// just before the end, space is also allowed. Otherwise we are
	// looking at an attribute name.
	if( null==DFA_Name.match(in, s, Dfa.dummySmd) ) break;
	String attr = s.substring(l);
	tlen += s.length()-l;
	s.setLength(l);
	
	// skip the Eq
	DFA_Eq.match(in, s, Dfa.dummySmd);
	tlen += s.length()-l;
	s.setLength(l);
	
	// get AttValue
	DFA_AttValue.match(in, s, Dfa.dummySmd);
	String value = s.substring(l+1, s.length()-1);
	tlen += s.length()-l;
	s.setLength(l);
	
	dst.put(attr, value);
      }
    } catch( java.io.IOException e ) {
      throw new Error("bug", e);
    }

    // We should be looking at either '/>', '>' or '?>' The latter
    // only in case this is an XMLDecl. According to the
    // contract of this method, we don't double check any violations.
    int ch = in.read();
    if( ch=='/' || ch=='?') return;

    // we should have ch=='>', but we won't check, we just skip
    tlen += 1;

    // reading backwards from the end to find the closing tag. We skip
    // garbage which might be after the closing tag. In addition we
    // speed up a little bit by skipping over most of the end tag. We
    // also allow that s contains only a start tag and not a full
    // element. 
    int end = l-tagname.length()-1;
    while( s.charAt(end)!='<' ) {
      // we allow to trailing garbage after a start tag, when there is
      // actually no end tag at all
      if( end<=start+tlen ) return;
      end -= 1;
    }
    dst.put(CONTENT, s.substring(start+tlen, end));
  }
  /**********************************************************************/
  /**
   * <p>splits an XML element into the parts tag name, attribute
   * names, attribute values and the content. Part 1 will be the tag
   * name, part 2 the first attribute name etc. The last part will be
   * the elements content.</p>
   *
   * <p><b>Hint:</b> If you need a {@link TextSplitter} which uses
   * this method, use the static object {@link #ELEMENTSPLITTER}.</p>
   * 
   * @deprecated use {@link #splitElement(Map,StringBuffer,int)} which
   * can also deal with the {@link #XMLDecl XML declaration}
   */
//   public static void splitElement(TextStore dst, StringBuffer s, int start) 
//   {
//     // what is already available in dst
//     int oldLen = dst.length();
//     int oldNum = dst.getNumParts();
    
//     // run split for the start tag. This copies the whole element
//     // into the TextStore
//     int cursor = start+splitSTag(dst, s, start);
    
//     // the end could have been the end of an AttValue in which case
//     // we are looking at a quote now, or there was just a tag name,
//     // in which case we are looking at white space or the closing
//     // angle bracket. Safest thing to do is just skip to the angle
//     // bracked. 
//     while( s.charAt(cursor++)!='>' ) /* just run */;
    
//     // the rest is a bit tricky. We cannot just skip to the
//     // next '</' because there might be enclosed elements. Therefore
//     // we start in the back. The first '<' scanning backwards is
//     // what we need. We don't need start really at the end because
//     // we know the length of the tag's name by now. The only thing
//     // we don't know is the length of the optional space before the
//     // very last '>'.
//     int end = s.length()-(dst.getPartLen(oldNum+1));
//     while( s.charAt(--end)!='<' ) /* just run */;
//     dst.addPart(oldLen+cursor-start, oldLen+end-start);
//   }
//   /**********************************************************************/
//   /**
//    * <p>implements a {@link TextSplitter} which uses {@link #splitSTag
//    * splitSTag()} as its <code>split</code> method. Use of this static
//    * object is only necessary where <code>splitSTag()</code> must be
//    * passed as a callback to some other method</p>
//    *
//    */
//   public static final TextSplitter STAGSPLITTER = new TextSplitter() {
//       public void split(TextStore dst, StringBuffer s, int start) {
// 	splitSTag(dst, s, start);
//       }
//     };    
//   /**********************************************************************/
//   /**
//    * <p>implements a {@link TextSplitter} which uses {@link
//    * #splitElement splitElement()} as its <code>split</code>
//    * method. Use of this static object is only necessary where
//    * <code>splitElement()</code> must be passed as a callback to some
//    * other method.</p>
//    */
//   public static final TextSplitter ELEMENTSPLITTER = new TextSplitter() {
//     public void split(TextStore dst, StringBuffer s, int start) {
//       splitElement(dst, s, start);
//     }
//     };
  /**********************************************************************/
}
