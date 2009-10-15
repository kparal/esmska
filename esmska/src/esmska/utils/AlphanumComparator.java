/*
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers.  Instead of sorting numbers in ASCII order like
 * a standard sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package esmska.utils;

import java.util.Comparator;

/** Class for sorting numeric strings in numeric order (as opposed to default lexicographic)
 * Use this class as a Comparator in Collections.sort(list, comparator) method.
 */
public class AlphanumComparator implements Comparator
{
  // TO USE THIS:
  //   Use the static "sort" method from the java.util.Collections class:
  //
  //   Collections.sort(your list, new AlphanumComparator());
  //

  char[] numbers = {'1','2','3','4','5','6','7','8','9','0' };

  private boolean isIn(char ch, char[] chars)
  {
    for (int i=0; i < chars.length; i++) {
      if (ch == chars[i]) return true;
    }
    return false;
  }

  private boolean inChunk(char ch, String s)
  {
    if (s.length() == 0) return true;

    char s0 = s.charAt(0);
    int chunkType = 0;   // 0 = alphabetic, 1 = numeric

    if (isIn(s0,numbers)) chunkType = 1;

    if ((chunkType == 0) && (isIn(ch,numbers))) return false;
    if ((chunkType == 1) && (!isIn(ch,numbers))) return false;

    return true;
  }

  public int compare(Object o1, Object o2)
  {
    if (!(o1 instanceof String) || !(o2 instanceof String)) {
        return 0;
    }

    // This is so much easier in a pattern-matching language like Perl!

    String s1 = (String)o1;
    String s2 = (String)o2;

    int thisMarker = 0;  int thisNumericChunk = 0;
    int thatMarker = 0;  int thatNumericChunk = 0;

    while ((thisMarker < s1.length()) && (thatMarker < s2.length())) {
      char thisCh = s1.charAt(thisMarker);
      char thatCh = s2.charAt(thatMarker);

      StringBuilder thisChunk = new StringBuilder();
      StringBuilder thatChunk = new StringBuilder();

      while ((thisMarker < s1.length()) && inChunk(thisCh,thisChunk.toString())) {
        thisChunk.append(thisCh);
        thisMarker++;
        if (thisMarker < s1.length()) thisCh = s1.charAt(thisMarker);
      }

      while ((thatMarker < s2.length()) && inChunk(thatCh,thatChunk.toString())) {
        thatChunk.append(thatCh);
        thatMarker++;
        if (thatMarker < s2.length()) thatCh = s2.charAt(thatMarker);
      }

      int thisChunkType = isIn(thisChunk.charAt(0),numbers) ? 1:0;
      int thatChunkType = isIn(thatChunk.charAt(0),numbers) ? 1:0;

      // If both chunks contain numeric characters, sort them numerically
      int result = 0;
      if ((thisChunkType == 1) && (thatChunkType == 1)) {
        thisNumericChunk = Integer.parseInt(thisChunk.toString());
        thatNumericChunk = Integer.parseInt(thatChunk.toString());
        if (thisNumericChunk < thatNumericChunk) result = -1;
        if (thisNumericChunk > thatNumericChunk) result =  1;
      } else {
        result = thisChunk.toString().compareTo(thatChunk.toString());
      }

      if (result != 0) return result;
    }

    // the longer string wins
    return new Integer(s1.length()).compareTo(s2.length());
  }
}
