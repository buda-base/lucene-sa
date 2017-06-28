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

import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.CharacterUtils.CharacterBuffer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;


/** 
 * <p>
 * Derived from Lucene 6.4.1 analysis.core.WhitespaceTokenizer.java
 * </p>
 * 
 * @author Hélios Hildt
 * 
 */
public final class SkrtSylTokenizer extends Tokenizer {

	/**
	 * Construct a new TibSyllableTokenizer.
	 */
	public SkrtSylTokenizer() {
	}

	private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
	private int previousChar = -1;
	public static final int DEFAULT_MAX_WORD_LEN = 255;
	private static final int IO_BUFFER_SIZE = 4096;
	private final int maxTokenLen = 10;

	public final static int VOWEL = 0;
	public final static int MODIFIER = 1;
	public final static int CONSONNANT = 2;
	public final static int OTHER = 3;

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);


	private static final HashMap<Integer, Integer> charType = createMap();
	private static final HashMap<Integer, Integer> createMap()
	{
		HashMap<Integer, Integer> charType = new HashMap<>();
		// vowels
		charType.put((int)'a', VOWEL);
		charType.put((int)'A', VOWEL);
		charType.put((int)'i', VOWEL);
		charType.put((int)'I', VOWEL);
		charType.put((int)'u', VOWEL);
		charType.put((int)'U', VOWEL);
		charType.put((int)'f', VOWEL);
		charType.put((int)'F', VOWEL);
		charType.put((int)'x', VOWEL);
		charType.put((int)'X', VOWEL);
		charType.put((int)'e', VOWEL);
		charType.put((int)'E', VOWEL);
		charType.put((int)'o', VOWEL);
		charType.put((int)'O', VOWEL);
		// special class for anusvara & visarga, jihvamuliya, upadhmaniya
		charType.put((int)'M', MODIFIER);
		charType.put((int)'H', MODIFIER);
		charType.put((int)'V', MODIFIER);
		charType.put((int)'Z', MODIFIER);
		// consonants
		charType.put((int)'k', CONSONNANT);
		charType.put((int)'K', CONSONNANT);
		charType.put((int)'g', CONSONNANT);
		charType.put((int)'G', CONSONNANT);
		charType.put((int)'N', CONSONNANT);
		charType.put((int)'c', CONSONNANT);
		charType.put((int)'C', CONSONNANT);
		charType.put((int)'j', CONSONNANT);
		charType.put((int)'J', CONSONNANT);
		charType.put((int)'Y', CONSONNANT);
		charType.put((int)'w', CONSONNANT);
		charType.put((int)'W', CONSONNANT);
		charType.put((int)'q', CONSONNANT);
		charType.put((int)'Q', CONSONNANT);
		charType.put((int)'R', CONSONNANT);
		charType.put((int)'t', CONSONNANT);
		charType.put((int)'T', CONSONNANT);
		charType.put((int)'d', CONSONNANT);
		charType.put((int)'D', CONSONNANT);
		charType.put((int)'n', CONSONNANT);
		charType.put((int)'p', CONSONNANT);
		charType.put((int)'P', CONSONNANT);
		charType.put((int)'b', CONSONNANT);
		charType.put((int)'B', CONSONNANT);
		charType.put((int)'m', CONSONNANT);
		charType.put((int)'y', CONSONNANT);
		charType.put((int)'r', CONSONNANT);
		charType.put((int)'l', CONSONNANT);
		charType.put((int)'v', CONSONNANT);
		charType.put((int)'L', CONSONNANT);
		charType.put((int)'|', CONSONNANT);
		charType.put((int)'S', CONSONNANT);
		charType.put((int)'z', CONSONNANT);
		charType.put((int)'s', CONSONNANT);
		charType.put((int)'h', CONSONNANT);
		// Modifiers
		charType.put((int)'_', OTHER);
		charType.put((int)'=', OTHER);
		charType.put((int)'!', OTHER);
		charType.put((int)'#', OTHER);
		charType.put((int)'1', OTHER);
		charType.put((int)'2', OTHER);
		charType.put((int)'3', OTHER);
		charType.put((int)'4', OTHER);
		charType.put((int)'/', OTHER);
		charType.put((int)'\\', OTHER);
		charType.put((int)'^', OTHER);
		charType.put((int)'6', OTHER);
		charType.put((int)'7', OTHER);
		charType.put((int)'8', OTHER);
		charType.put((int)'9', OTHER);
		charType.put((int)'+', OTHER);
		charType.put((int)'~', OTHER);
		return charType;
	}

	public boolean isSylEnd(int char1, int char2) {
		/**
		 * Returns true if a syllable ends between char1 and char2
		 * @return
		 */
		// char1\char2 | nonSLP | OTHER | CONSONNANT | MODIFIER | VOWEL |
		//-------------|--------|-------|------------|----------|-------|
		//    nonSLP   |   x    |   x   |     x      |    x     |   x   |
		//      M      |   A.   |   x   |     B.     |    x     |   x   |
		//      C      |   A.   |   x   |     x      |    x     |   x   |
		//      X      |   A.   |   x   |     C.     |    x     |   x   |
		//      V      |   A.   |   x   |     D.     |    x     |   x   |
		//---------------------------------------------------------------
		//
		if (charType.containsKey(char1) && !charType.containsKey(char2)) {
			// A.
			return true;
		} else if (charType.containsKey(char2) && charType.containsKey(char2) && charType.get(char2) == CONSONNANT) {
			if (charType.containsKey(char1) && charType.get(char1) == OTHER) {
				// B.
				return true;
			} else if (charType.containsKey(char1) && charType.get(char1) == MODIFIER) {
				// C.
				return true;
			} else if (charType.containsKey(char1) && charType.get(char1) == VOWEL) {
				// D.
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	public boolean isSylStart(int char1, int char2) {
		/**
		 * Returns true if a syllable starts between char1 and char2
		 * @return
		 */
		// char1\char2 | nonSLP | OTHER | CONSONNANT | MODIFIER | VOWEL |
		//-------------|--------|-------|------------|----------|-------|
		//    nonSLP   |   x    |   A.  |     A.     |    A.    |   A.  |
		//      M      |   x    |   x   |     B.     |    x     |   x   |
		//      C      |   x    |   x   |     x      |    x     |   x   |
		//      X      |   x    |   x   |     B.     |    x     |   x   |
		//      V      |   x    |   x   |     B.     |    x     |   x   |
		//---------------------------------------------------------------
		//
		if (!charType.containsKey(char1) && charType.containsKey(char2)) {
			// A.
			return true;
		} else if ((charType.containsKey(char1) && charType.get(char1) != CONSONNANT) && (charType.containsKey(char2) && charType.get(char2) == CONSONNANT)) {
			// B.
			return true;
		} else {
			return false;
		}
	}

	protected boolean isTokenChar(int c) {
		Integer res = charType.get(c);
		return (res != null && res != OTHER); 
	}

	/** 
	 * adapted from CharTokenizer
	 */
	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		int length = 0;
		int start = -1; // this variable is always initialized
		int end = -1;
		char[] buffer = termAtt.buffer();
		while (true) {
			if (bufferIndex >= dataLen) {
				offset += dataLen;
				CharacterUtils.fill(ioBuffer, input); // read supplementary char aware with CharacterUtils
				if (ioBuffer.getLength() == 0) {
					dataLen = 0; // so next offset += dataLen won't decrement offset
					if (length > 0) {
						break;
					} else {
						finalOffset = correctOffset(offset);
						return false;
					}
				}
				dataLen = ioBuffer.getLength();
				bufferIndex = 0;
			}
			// use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based methods are gone
			final int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
			final int charCount = Character.charCount(c);
			bufferIndex += charCount;

			if (isTokenChar(c)) {               // if it's a token char
				if (length == 0) {                // start of token
					assert start == -1;
					start = offset + bufferIndex - charCount;
					end = start;
				} else if (length >= buffer.length-1) { // check if a supplementary could run out of bounds
					buffer = termAtt.resizeBuffer(2+length); // make sure a supplementary fits in the buffer
				}
				end += charCount;
				length += Character.toChars(c, buffer, length); // buffer it
				if (isSylStart(previousChar, c) || isSylEnd(previousChar, c)) {
                    previousChar = c;
                    break;
				}
				if (length >= maxTokenLen) { // buffer overflow! make sure to check for >= surrogate pair could break == test
				    previousChar = c;
					break;
				}
			} else if (length > 0) {           // at non-Letter w/ chars
			    previousChar = c;
				break;                           // return 'em
			}
			previousChar = c;
		}

		termAtt.setLength(length);
		assert start != -1;
		finalOffset = correctOffset(end);
		offsetAtt.setOffset(correctOffset(start), finalOffset);
		return true;

	}
}