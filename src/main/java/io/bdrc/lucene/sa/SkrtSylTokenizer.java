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
import java.util.Arrays;
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
	 * Construct a new SkrtSyllableTokenizer.
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
	public final static int CONSONANT = 2;
	public final static int OTHER = 3;
	public final static int PUNCT = 4;

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);

	private static final HashMap<Integer, Integer> skrtPunct = punctMap();
	private static final HashMap<Integer, Integer> punctMap()
	{
		HashMap<Integer, Integer> skrtPunct = new HashMap<>();
		skrtPunct.put((int)'.', PUNCT);
		skrtPunct.put((int)' ', PUNCT);
		skrtPunct.put((int)',', PUNCT);
		return skrtPunct;
	}

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
		charType.put((int)'k', CONSONANT);
		charType.put((int)'K', CONSONANT);
		charType.put((int)'g', CONSONANT);
		charType.put((int)'G', CONSONANT);
		charType.put((int)'N', CONSONANT);
		charType.put((int)'c', CONSONANT);
		charType.put((int)'C', CONSONANT);
		charType.put((int)'j', CONSONANT);
		charType.put((int)'J', CONSONANT);
		charType.put((int)'Y', CONSONANT);
		charType.put((int)'w', CONSONANT);
		charType.put((int)'W', CONSONANT);
		charType.put((int)'q', CONSONANT);
		charType.put((int)'Q', CONSONANT);
		charType.put((int)'R', CONSONANT);
		charType.put((int)'t', CONSONANT);
		charType.put((int)'T', CONSONANT);
		charType.put((int)'d', CONSONANT);
		charType.put((int)'D', CONSONANT);
		charType.put((int)'n', CONSONANT);
		charType.put((int)'p', CONSONANT);
		charType.put((int)'P', CONSONANT);
		charType.put((int)'b', CONSONANT);
		charType.put((int)'B', CONSONANT);
		charType.put((int)'m', CONSONANT);
		charType.put((int)'y', CONSONANT);
		charType.put((int)'r', CONSONANT);
		charType.put((int)'l', CONSONANT);
		charType.put((int)'v', CONSONANT);
		charType.put((int)'L', CONSONANT);
		charType.put((int)'|', CONSONANT);
		charType.put((int)'S', CONSONANT);
		charType.put((int)'z', CONSONANT);
		charType.put((int)'s', CONSONANT);
		charType.put((int)'h', CONSONANT);
		
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
		// char1\char2 | nonSLP | OTHER | CONSONANT | MODIFIER | VOWEL |
		//-------------|--------|-------|------------|----------|-------|
		//    nonSLP   |   x    |   x   |     x      |    x     |   x   |
		//     OTHER   |   A.   |   x   |     B.     |    x     |   x   |
		//   CONSONANT |   A.   |   x   |     x      |    x     |   x   |
		//    MODIFIER |   A.   |   x   |     C.     |    x     |   x   |
		//     VOWEL   |   A.   |   x   |     D.     |    x     |   x   |
		//---------------------------------------------------------------
		//
		if (charType.containsKey(char1) && !charType.containsKey(char2)) {
			// A.
			return true;
		} else if (charType.containsKey(char2) && charType.get(char2) == CONSONANT) {
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
	
	protected boolean isSLP(int c) {
		Integer res = charType.get(c);
		return (res != null); 
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
			int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
			final int charCount = Character.charCount(c);
			bufferIndex += charCount;

			if (isSLP(c)) {               // if it's a token char
				if (length == 0) {                // start of token
					assert start == -1;
					start = offset + bufferIndex - charCount;
					end = start;
				} else if (length >= buffer.length-1) { // check if a supplementary could run out of bounds
					buffer = termAtt.resizeBuffer(2+length); // make sure a supplementary fits in the buffer
				}
				end += charCount;
				length += Character.toChars(c, buffer, length); // buffer it
				
				if (!isTrailingCluster(ioBuffer, bufferIndex-1) && isSylEnd(previousChar, c)) {
					// we need to come back to the previous state for all variables
					// since the detected boundary is between previousChar and c,
					// meaning c already pertains to the next syllable
					bufferIndex = bufferIndex - charCount;
					length = length - charCount;
					end = end - charCount;
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

	private boolean isTrailingCluster(CharacterBuffer inputBuffer, int bufferIndex ) {
		// see who comes first, a vowel, a legal punctuation or the end of the buffer
		int nextSylEndIdx = bufferIndex;
		char[] buffer = inputBuffer.getBuffer();
		while (nextSylEndIdx < inputBuffer.getLength()) {
			if (charType.containsKey((int)buffer[nextSylEndIdx]) && charType.get((int)buffer[nextSylEndIdx]) == CONSONANT) {
				if (nextSylEndIdx+1 == inputBuffer.getLength()) {
					System.out.print(Arrays.asList(buffer).subList(bufferIndex, nextSylEndIdx).toString());
					return true;
				}// if char at nextSylIdx
				else if (charType.containsKey((int)buffer[nextSylEndIdx+1]) && charType.get((int)buffer[nextSylEndIdx+1]) == VOWEL) {
					return false;
				} else if (skrtPunct.containsKey((int)buffer[nextSylEndIdx+1])) {
					//System.out.print(Arrays.asList(buffer).subList(0, nextSylEndIdx).toString());
					return true;
				}
			}
			nextSylEndIdx++;
		}
		return false;
	}
}