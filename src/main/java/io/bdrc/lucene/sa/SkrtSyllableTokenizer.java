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
 * A Syllable Tokenizer for Sanskrit encoded in SLP1.
 * <p>
 * Does not implement complex syllabation rules, does the same syllabation as
 * {@link http://www.sanskritlibrary.org/Sanskrit/SanskritTransliterate/syllabify.html}
 * 
 * <p>
 * Derived from Lucene 6.4.1 analysis.util.CharTokenizer
 * 
 * @author Hélios Hildt
 * @author Élie Roux
 * 
 */
public final class SkrtSyllableTokenizer extends Tokenizer {

	/**
	 * Construct a new SkrtSyllableTokenizer.
	 */
	public SkrtSyllableTokenizer() {
	}
	
	private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
	private int previousChar = -1;
	public static final int DEFAULT_MAX_WORD_LEN = 255;
	private static final int IO_BUFFER_SIZE = 4096;
	private final int maxTokenLen = 10;

	// valid SLP characters' types
	public final static int VOWEL = 0;
	public final static int SPECIALPHONEME = 1;
	public final static int CONSONANT = 2;
	public final static int MODIFIER = 3;
	
	// SLP punctuation
	public final static int PUNCT = 4;

	// states returned by isTrailingCluster()
	public final static int CLUSTER_N_VOWEL = 20;
	public final static int CLUSTER_N_PUNCT = 21;
	public final static int CLUSTER_N_END = 22;
	public final static int NOT_A_CLUSTER = 23;
	
	// states returned by isSylEnd()
	public final static int SLP_N_NONSLP = 10;
	public final static int MODIFIER_N_CONSONANT = 11;
	public final static int SPECIALPHONEME_N_CONSONANT = 12;
	public final static int VOWEL_N_CONSONANT = 13;
	public final static int NOT_SYLL_END = 14;

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

	static final HashMap<Integer, Integer> charType = createMap();
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
		charType.put((int)'M', SPECIALPHONEME);
		charType.put((int)'H', SPECIALPHONEME);
		charType.put((int)'V', SPECIALPHONEME);
		charType.put((int)'Z', SPECIALPHONEME);
		charType.put((int)'~', SPECIALPHONEME);
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
		charType.put((int)'_', MODIFIER);
		charType.put((int)'=', MODIFIER);
		charType.put((int)'!', MODIFIER);
		charType.put((int)'#', MODIFIER);
		charType.put((int)'1', MODIFIER);
		charType.put((int)'2', MODIFIER);
		charType.put((int)'3', MODIFIER);
		charType.put((int)'4', MODIFIER);
		charType.put((int)'/', MODIFIER);
		charType.put((int)'\\', MODIFIER);
		charType.put((int)'^', MODIFIER);
		charType.put((int)'6', MODIFIER);
		charType.put((int)'7', MODIFIER);
		charType.put((int)'8', MODIFIER);
		charType.put((int)'9', MODIFIER);
		charType.put((int)'+', MODIFIER);
		return charType;
	}

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
				
				// Here is where the syllabation logic really happens
				int maybeTrailingConsonants = afterConsonantCluster(ioBuffer, bufferIndex-1);
				int maybeSylEnd = syllEndingCombinations(previousChar, c);
				
				boolean endOfSyllable;
				if (maybeTrailingConsonants == CLUSTER_N_VOWEL || maybeTrailingConsonants == NOT_A_CLUSTER) {
					if (maybeSylEnd == VOWEL_N_CONSONANT || maybeSylEnd == SPECIALPHONEME_N_CONSONANT ||
							maybeSylEnd == MODIFIER_N_CONSONANT || maybeSylEnd == SLP_N_NONSLP ||
							maybeSylEnd == MODIFIER_N_CONSONANT) {
						endOfSyllable = true;
					} else if (maybeSylEnd == NOT_SYLL_END) {
						endOfSyllable = false;
					} else {
						endOfSyllable = false;
					}
				} else if (maybeTrailingConsonants == CLUSTER_N_PUNCT || maybeTrailingConsonants == CLUSTER_N_END) {
					endOfSyllable = false;
				} else {
					endOfSyllable = false;
				}

				if (endOfSyllable) {
					// previousChar is the end of the current syllable
					// setting the cursor one step back and ending this token/syllable 
					bufferIndex = bufferIndex - charCount;
					length = length - charCount;
					end = end - charCount;
					previousChar = c;
                    break;
				}  // end of syllabation logic
				
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

	
	public static boolean isSLP(int c) {
		/**
		 * filters only legal SLP1 characters
		 * @return true if c is a SLP character, else false
		 */
		Integer res = charType.get(c);
		return (res != null); 
	}
	
	public int syllEndingCombinations(int char1, int char2) {
		/**
		 * Finds all combinations that correspond to a syllable ending
		 * @param corresponds to previousChar
		 * @param corresponds to c
		 * @return true if a syllable ends between char1 and char2, else false
		 */
		//   char1\char2  | nonSLP | MODIFIER | CONSONANT | SPECIALPHONEME | VOWEL |
		//----------------|--------|----------|-----------|----------------|-------|
		//      nonSLP    |        |          |           |                |       |
		//     MODIFIER   |   X    |          |     X     |                |       |
		//    CONSONANT   |   X    |          |           |                |       |
		// SPECIALPHONEME |   X    |          |     X     |                |       |
		//       VOWEL    |   X    |          |     X     |                |       |
		//--------------------------------------------------------------------------
		if (charType.containsKey(char1) && !charType.containsKey(char2)) {
			return SLP_N_NONSLP;
		} else if (charType.containsKey(char2) && charType.get(char2) == CONSONANT) {
			if (charType.containsKey(char1) && charType.get(char1) == MODIFIER) {
				return MODIFIER_N_CONSONANT;
			} else if (charType.containsKey(char1) && charType.get(char1) == SPECIALPHONEME) {
				return SPECIALPHONEME_N_CONSONANT;
			} else if (charType.containsKey(char1) && charType.get(char1) == VOWEL) {
				return VOWEL_N_CONSONANT;
			} else {
				return NOT_SYLL_END;
			}
		} else {
			return NOT_SYLL_END;
		}
	}
	
	private int afterConsonantCluster(CharacterBuffer inputBuffer, int currentIdx ) {
		/**
		 * checks whether the next consonants constitute a trailing cluster of consonants or not.
		 * @return the combination
		 */
		// see who comes first, a vowel, a legal punctuation or the end of the buffer
		int nextSylEndIdx = currentIdx;
		char[] buffer = inputBuffer.getBuffer();
		while (nextSylEndIdx < inputBuffer.getLength()) {
			if (charType.containsKey((int)buffer[nextSylEndIdx]) && charType.get((int)buffer[nextSylEndIdx]) == CONSONANT) {
				if (nextSylEndIdx+1 == inputBuffer.getLength()) {
					return CLUSTER_N_END;
				}// if char at nextSylIdx
				else if (charType.containsKey((int)buffer[nextSylEndIdx+1]) && charType.get((int)buffer[nextSylEndIdx+1]) == VOWEL) {
					return CLUSTER_N_VOWEL;
				} else if (skrtPunct.containsKey((int)buffer[nextSylEndIdx+1])) {
					//System.out.print(Arrays.asList(buffer).subList(0, nextSylEndIdx).toString());
					return CLUSTER_N_PUNCT;
				}
			}
			nextSylEndIdx++;
		}
		return NOT_A_CLUSTER;
	}
}