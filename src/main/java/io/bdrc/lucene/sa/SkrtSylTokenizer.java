/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear 
 * below; otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the 'License"); you may not use this file except in compliance with the 
 * License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.bdrc.lucene.sa;

import java.util.HashMap;

import org.apache.lucene.analysis.util.CharTokenizer;

/** 
 * <p>
 * Derived from Lucene 6.4.1 analysis.core.WhitespaceTokenizer.java
 * </p>
 * 
 * @author Hélios Hildt
 * 
 */
public final class SkrtSylTokenizer extends CharTokenizer {
  
	/**
	 * Construct a new TibSyllableTokenizer.
	 */
	public SkrtSylTokenizer() {
	}
	
	private static final HashMap<Character, Character> charType = createMap();
	private static final HashMap<Character, Character> createMap()
	{
		HashMap<Character, Character> charType = new HashMap<Character, Character>();
		// vowels
		charType.put('a', 'V');
		charType.put('A', 'V');
		charType.put('i', 'V');
		charType.put('I', 'V');
		charType.put('u', 'V');
		charType.put('U', 'V');
		charType.put('f', 'V');
		charType.put('F', 'V');
		charType.put('x', 'V');
		charType.put('X', 'V');
		charType.put('e', 'V');
		charType.put('E', 'V');
		charType.put('o', 'V');
		charType.put('O', 'V');
		// special class for anusvara & visarga, jihvamuliya, upadhmaniya
		charType.put('M', 'X');
		charType.put('H', 'X');
		charType.put('V', 'X');
		charType.put('Z', 'X');
		// consonants
		charType.put('k', 'C');
		charType.put('K', 'C');
		charType.put('g', 'C');
		charType.put('G', 'C');
		charType.put('N', 'C');
		charType.put('c', 'C');
		charType.put('C', 'C');
		charType.put('j', 'C');
		charType.put('J', 'C');
		charType.put('Y', 'C');
		charType.put('w', 'C');
		charType.put('W', 'C');
		charType.put('q', 'C');
		charType.put('Q', 'C');
		charType.put('R', 'C');
		charType.put('t', 'C');
		charType.put('T', 'C');
		charType.put('d', 'C');
		charType.put('D', 'C');
		charType.put('n', 'C');
		charType.put('p', 'C');
		charType.put('P', 'C');
		charType.put('b', 'C');
		charType.put('B', 'C');
		charType.put('m', 'C');
		charType.put('y', 'C');
		charType.put('r', 'C');
		charType.put('l', 'C');
		charType.put('v', 'C');
		charType.put('L', 'C');
		charType.put('|', 'C');
		charType.put('S', 'C');
		charType.put('z', 'C');
		charType.put('s', 'C');
		charType.put('h', 'C');
		// Modifiers
		charType.put('_', 'M');
		charType.put('=', 'M');
		charType.put('!', 'M');
		charType.put('#', 'M');
		charType.put('1', 'M');
		charType.put('1', 'M');
		charType.put('2', 'M');
		charType.put('3', 'M');
		charType.put('4', 'M');
		charType.put('/', 'M');
		charType.put('\\', 'M');
		charType.put('^', 'M');
		charType.put('6', 'M');
		charType.put('7', 'M');
		charType.put('8', 'M');
		charType.put('9', 'M');
		charType.put('+', 'M');
		charType.put('~', 'M');
		return charType;
	}
		
	public boolean isSylEnd(char char1, char char2) {
		/**
		 * Returns true if a syllable ends between char1 and char2
		 * @ return
		 */
		// char1\char2 | nonSLP | M | C  | X | V |
		//-------------|--------|---|----|---|---|
		//    nonSLP   |   x    | x | x  | x | x |
		//      M      |   A.   | x | B. | x | x |
		//      C      |   A.   | x | x  | x | x |
		//      X      |   A.   | x | C. | x | x |
		//      V      |   A.   | x | D. | x | x |
		//---------------------------------------
		//
		if (charType.containsKey(char1) && !charType.containsKey(char2)) {
			// A.
			return true;
		} else if (charType.containsKey(char2) && charType.containsKey(char2) && charType.get(char2) == 'C') {
			if (charType.containsKey(char1) && charType.get(char1) == 'M') {
				// B.
				return true;
			} else if (charType.containsKey(char1) && charType.get(char1) == 'X') {
				// C.
				return true;
			} else if (charType.containsKey(char1) && charType.get(char1) == 'V') {
				// D.
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
  
	/** 
	 * @return 
	 */
	@Override
	protected boolean isTokenChar(int c) {
		return false;
	}
}