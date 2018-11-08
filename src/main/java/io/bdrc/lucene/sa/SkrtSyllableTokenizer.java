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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * <p>
 * A Syllable Tokenizer for Sanskrit encoded in SLP1.
 * <p>
 * Does not implement complex syllabation rules, does the same syllabation
 * as @see <a href="http://www.sanskritlibrary.org/Sanskrit/SanskritTransliterate/syllabify.html">Sanskrit Library</a>
 * <p>
 * Derived from Lucene 6.4.1 analysis.util.CharTokenizer
 * 
 * @author Hélios Hildt
 * @author Élie Roux
 * 
 */
public final class SkrtSyllableTokenizer extends Tokenizer {

    final HashMap<Integer, Integer> charType;
	/**
	 * Construct a new SkrtSyllableTokenizer.
	 */
	public SkrtSyllableTokenizer() {
	    this(false);
	}

    public SkrtSyllableTokenizer(final boolean lenientMode) {
        super();
        if (lenientMode) {
            this.charType = charTypeLenient;
        } else {
            this.charType = charTypeNonLenient;
        }
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
	static final Logger logger = LoggerFactory.getLogger(SkrtSyllableTokenizer.class);
	
	private static final HashMap<Integer, Integer> skrtPunct = punctMap();
	private static final HashMap<Integer, Integer> punctMap()
	{
		HashMap<Integer, Integer> skrtPunct = new HashMap<>();
		skrtPunct.put((int)'.', PUNCT);
		skrtPunct.put((int)' ', PUNCT);
		skrtPunct.put((int)',', PUNCT);
		skrtPunct.put((int)'-', PUNCT);
		return skrtPunct;
	}

	static final HashMap<Integer, Integer> charTypeNonLenient = new HashMap<>();
	static final HashMap<Integer, Integer> charTypeLenient = new HashMap<>();
	
	static void addToMap(int c, int type) {
	    charTypeNonLenient.put(c, type);
	    charTypeLenient.put(c, type);
	}

    static void addToMap(int c, int type, boolean lenient) {
        if (lenient) {
            charTypeLenient.put(c, type);
        } else {
            charTypeNonLenient.put(c, type);            
        }
    }
	
	static
	{
		// vowels
		addToMap((int)'a', VOWEL);
		addToMap((int)'A', VOWEL);
		addToMap((int)'i', VOWEL);
		addToMap((int)'I', VOWEL);
		addToMap((int)'u', VOWEL);
		addToMap((int)'U', VOWEL);
		addToMap((int)'f', VOWEL);
		addToMap((int)'F', VOWEL);
		addToMap((int)'x', VOWEL);
		addToMap((int)'X', VOWEL);
		addToMap((int)'e', VOWEL);
		addToMap((int)'E', VOWEL);
		addToMap((int)'o', VOWEL);
		addToMap((int)'O', VOWEL);
		// special class for anusvara & visarga, jihvamuliya, upadhmaniya
		addToMap((int)'M', SPECIALPHONEME);
		addToMap((int)'H', SPECIALPHONEME);
		addToMap((int)'V', SPECIALPHONEME);
		addToMap((int)'Z', SPECIALPHONEME);
		addToMap((int)'~', SPECIALPHONEME);
		// consonants
		addToMap((int)'k', CONSONANT);
		addToMap((int)'K', CONSONANT);
		addToMap((int)'g', CONSONANT);
		addToMap((int)'G', CONSONANT);
		addToMap((int)'N', CONSONANT);
		addToMap((int)'c', CONSONANT);
		addToMap((int)'C', CONSONANT);
		addToMap((int)'j', CONSONANT);
		addToMap((int)'J', CONSONANT);
		addToMap((int)'Y', CONSONANT);
		addToMap((int)'w', CONSONANT);
		addToMap((int)'W', CONSONANT);
		addToMap((int)'q', CONSONANT);
		addToMap((int)'Q', CONSONANT);
		addToMap((int)'R', CONSONANT);
		addToMap((int)'t', CONSONANT);
		addToMap((int)'T', CONSONANT);
		addToMap((int)'d', CONSONANT);
		addToMap((int)'D', CONSONANT);
		addToMap((int)'n', CONSONANT);
		addToMap((int)'p', CONSONANT);
		addToMap((int)'P', CONSONANT);
		addToMap((int)'b', CONSONANT);
		addToMap((int)'B', CONSONANT);
		addToMap((int)'m', CONSONANT);
		addToMap((int)'y', CONSONANT);
		addToMap((int)'r', CONSONANT, false);
		addToMap((int)'r', VOWEL, true);
		addToMap((int)'l', CONSONANT, false);
		addToMap((int)'l', VOWEL, true);
		addToMap((int)'v', CONSONANT);
		addToMap((int)'L', CONSONANT);
		addToMap((int)'|', CONSONANT);
		addToMap((int)'S', CONSONANT);
		addToMap((int)'z', CONSONANT);
		addToMap((int)'s', CONSONANT);
		addToMap((int)'h', CONSONANT);
		
		// Modifiers
		addToMap((int)'_', MODIFIER);
		addToMap((int)'=', MODIFIER);
		addToMap((int)'!', MODIFIER);
		addToMap((int)'#', MODIFIER);
		addToMap((int)'1', MODIFIER);
		addToMap((int)'2', MODIFIER);
		addToMap((int)'3', MODIFIER);
		addToMap((int)'4', MODIFIER);
		addToMap((int)'/', MODIFIER);
		addToMap((int)'\\', MODIFIER);
		addToMap((int)'^', MODIFIER);
		addToMap((int)'6', MODIFIER);
		addToMap((int)'7', MODIFIER);
		addToMap((int)'8', MODIFIER);
		addToMap((int)'9', MODIFIER);
		addToMap((int)'+', MODIFIER);
	}

	@Override
	public final boolean incrementToken() throws IOException {
	    logger.trace("incrementToken, offset={}, bufferIndex={}, dataLen={}, finalOffset={}, previousChar={}", offset, bufferIndex, dataLen, finalOffset, previousChar);
		clearAttributes();
		int length = 0;
		int start = -1; // this variable is always initialized
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
						logger.trace("incrementToken, returning false");
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

			if (isSLP(c)) {               // if it's a token char
				if (length == 0) {                // start of token
					assert start == -1;
					start = offset + bufferIndex - charCount;
				} else if (length >= buffer.length-1) { // check if a supplementary could run out of bounds
					buffer = termAtt.resizeBuffer(2+length); // make sure a supplementary fits in the buffer
				}
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
	    int initialOffset = correctOffset(start);
	    if (initialOffset <= finalOffset) { initialOffset = finalOffset + 1; }
	    finalOffset = correctOffset(start + length);
	    if (initialOffset < 0) {
	        logger.warn("initialOffset incorrect. start: {}, end: {}, orig: {}", initialOffset, finalOffset, termAtt);
	        initialOffset = 0;
	    }
	    if (finalOffset < initialOffset) {
	        logger.warn("finalOffset incorrect. start: {}, end: {}, orig: {}", initialOffset, finalOffset, termAtt);
	        finalOffset = initialOffset;
	    }
	    try {
	        offsetAtt.setOffset(initialOffset, finalOffset);
	    } catch (Exception ex) {
            logger.error("SkrtSyllableTokenizer.incrementToken error on term: {}; message: {}", termAtt, ex.getMessage());
        }
	    logger.trace("incrementToken, returning token with offsets {}-{}, termAtt='{}'", initialOffset, finalOffset, termAtt);
		return true;
	}

	@Override
	public final void end() throws IOException {
	    super.end();
	    try {
	        // set final offset
	        offsetAtt.setOffset(finalOffset, finalOffset);
	    } catch (Exception ex) {
	        logger.error("SkrtSyllableTokenizer.end error on term: {}; message: {}", termAtt, ex.getMessage());
	    }
	}

	  @Override
	  public void reset() throws IOException {
	    super.reset();
	    bufferIndex = 0;
	    offset = 0;
	    dataLen = 0;
	    previousChar = -1;
	    finalOffset = 0;
	    ioBuffer.reset(); // make sure to reset the IO buffer!!
	}
	
	public static boolean isSLP(final int c) {
		/**
		 * filters only legal SLP1 characters
		 * @return true if c is a SLP character, else false
		 */
		final Integer res = charTypeNonLenient.get(c);
		return (res != null); 
	}
	
	public int syllEndingCombinations(final int char1, final int char2) {
		/**
		 * Finds all combinations that correspond to a syllable ending
		 * 
		 * |   char1\char2  | nonSLP | MODIFIER | CONSONANT | SPECIALPHONEME | VOWEL |
		 * |----------------|--------|----------|-----------|----------------|-------|
		 * |      nonSLP    |        |          |           |                |       |
		 * |     MODIFIER   |   X    |          |     X     |                |       |
		 * |    CONSONANT   |   X    |          |           |                |       |
		 * | SPECIALPHONEME |   X    |          |     X     |                |       |
		 * |       VOWEL    |   X    |          |     X     |                |       |
		 * |--------------------------------------------------------------------------
		 * 
		 * @param corresponds to previousChar
		 * @param corresponds to c
		 * @return true if a syllable ends between char1 and char2, else false
		 */
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
	
	private int afterConsonantCluster(final CharacterBuffer inputBuffer, final int currentIdx ) {
		/**
		 * checks whether the next consonants constitute a trailing cluster of consonants or not.
		 * @return the combination
		 */
		// see who comes first, a vowel, a legal punctuation or the end of the buffer
		int nextSylEndIdx = currentIdx;
		final char[] buffer = inputBuffer.getBuffer();
		while (nextSylEndIdx < inputBuffer.getLength()) {
		    final int nextChar = (int)buffer[nextSylEndIdx];
		    Integer nextCharType = charType.get(nextChar);
			if (nextCharType != null && nextCharType == CONSONANT) {
				if (nextSylEndIdx+1 == inputBuffer.getLength()) {
					return CLUSTER_N_END;
				}// if char at nextSylIdx
				else {
				    if (charType.containsKey((int)buffer[nextSylEndIdx+1]) && charType.get((int)buffer[nextSylEndIdx+1]) == VOWEL) {
				        return CLUSTER_N_VOWEL;
				    } else if (skrtPunct.containsKey((int)buffer[nextSylEndIdx+1])) {
				        //System.out.print(Arrays.asList(buffer).subList(0, nextSylEndIdx).toString());
				        return CLUSTER_N_PUNCT;
				    }
				}
			}
			nextSylEndIdx++;
		}
		return NOT_A_CLUSTER;
	}
}