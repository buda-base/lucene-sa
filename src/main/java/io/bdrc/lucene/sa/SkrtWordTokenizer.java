/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear 
 * below; otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the 
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.CharacterUtils.CharacterBuffer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

/**
 * A maximal-matching word tokenizer for Tibetan that uses a {@link Trie}.
 * 
 * <p>
 * Takes a syllable at a time and returns the longest sequence of syllable that form a word within the Trie.<br>
 * {@link #isTibLetter(int)} is used to distinguish clusters of letters forming syllables and {@code u\0F0B}(tsheg) to distinguish syllables within a word.
 * <br> 
 *  - Unknown syllables are tokenized as separate words.
 * <br>
 *  - All the punctuation is discarded from the produced tokens 
 * <p>
 * Due to its design, this tokenizer doesn't deal with contextual ambiguities.<br>
 * For example, (...)   
 * 
 * Derived from Lucene 6.4.1 analysis.
 * 
 * @author Élie Roux
 * @author Drupchen
 *
 */
public final class SkrtWordTokenizer extends Tokenizer {
	private Trie scanner;

	// this tokenizer generates three attributes:
	// term offset, positionIncrement and type
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	/**
	 * Constructs a SkrtWordTokenizer using the file designed by filename
	 * @param filename
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public SkrtWordTokenizer(String filename) throws FileNotFoundException, IOException {
		init(filename);
	}

	/**
	 * Constructs a SkrtWordTokenizer using a default lexicon file (here "resources/sanskrit-stemming-data/output/total_output.txt") 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public SkrtWordTokenizer() throws FileNotFoundException, IOException {
		init("resources/sanskrit-stemming-data/output/total_output.txt");
	}

	/**
	 * Initializes and populates {@see #scanner} 
	 * 
	 * The format of each line in filename must be as follows: "<sandhied_inflected_form>,<initial>,<diffs>/<initial_diff>"
	 * @param filename the file containing the entries to be added
	 * @throws FileNotFoundException 
	 * @throws IOException
	 */
	private void init(String filename) throws FileNotFoundException, IOException {
		this.scanner = new Trie(true);

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			while ((line = br.readLine()) != null) {
				int endOfFormIndex = line.indexOf(',');
				if (endOfFormIndex == -1) {
					throw new IllegalArgumentException("The dictionary file is corrupted in the following line.\n" + line);
				} else {
					this.scanner.add(line.substring(0, endOfFormIndex), line.substring(endOfFormIndex+1));
				}
			}
			Optimizer opt = new Optimizer();
			this.scanner.reduce(opt);
		}
	}

	private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
	private static final int MAX_WORD_LEN = 255;
	private static final int IO_BUFFER_SIZE = 4096;

	//	  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	//	  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

	private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);

	private LinkedHashMap<String, Integer[]> extraTokens;
	private Iterator extraTokensIterator;
	private boolean emitExtraTokens;  // we could maybe do without this variable since we now have iterator.hasNext()

	private HashSet<String> sandhiedInitials = null;
	private int multipleInitialsBufferIndex = -1;
	private int multipleInitialsEnd = -1;
	private Iterator<String> sandhiedInitialsIterator;

	/**
	 * Called on each token character to normalize it before it is added to the
	 * token. The default implementation does nothing. Subclasses may use this to,
	 * e.g., lowercase tokens.
	 */
	protected int normalize(int c) {
		return c;
	}

	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		
		if (emitExtraTokens) {
			addExtraToken();
			if (emitExtraTokens) { // need to test this because otherwise we will return intrementToken() while all extra tokens have been added
				return true;
			}
		}
		
		int tokenLength = 0;
		int tokenStart = -1; // this variable is always initialized
		int tokenEnd = -1;
		int confirmedEnd = -1;
		int confirmedEndIndex = -1;
		int nonWordStart = -1; // indices for non-word characters
		int nonWordEnd = -1;
		int charCount = -1;
		String cmd = null;
		int trieTmp = -1;
		int cmdIndex = -1;
		int potentialEndCmdIndex = -1;
		boolean potentialEnd = false;
		Row currentRow = null;
		Row rootRow = scanner.getRow(scanner.getRoot()); // initialized here because it will be used at every new non-word char
		StringBuilder nonWordChars = new StringBuilder();
		char[] tokenBuffer = termAtt.buffer();
		LinkedList<Character> initialSandhiedBuffer;
		System.out.println("----------------------");
		while (true) {
			// this if(){} deals with the beginning and end of the input string (bufferIndex == 0 and bufferIndex == input.length)
			if (bufferIndex >= dataLen) {
				offset += dataLen;
				CharacterUtils.fill(ioBuffer, input); // read supplementary char aware with CharacterUtils
				if (ioBuffer.getLength() == 0) {
					dataLen = 0; // so next offset += dataLen won't decrement offset
					if (tokenLength > 0) {
						break;
					} else {
						finalOffset = correctOffset(offset);
						return false;
					}
				}
				dataLen = ioBuffer.getLength();
				bufferIndex = 0;
			}
			
			// take the next char in the input (ioBuffer) for processing it and increment bufferIndex for next value of c
			// (use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based methods are gone)
			final int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
			charCount = Character.charCount(c);
			bufferIndex += charCount; // the index for next c
			System.out.println((char) c);
			
			// save the indices of the current state to be able to restore it later
			if (sandhiedInitials != null && sandhiedInitials.size() != 1 && sandhiedInitialsIterator.hasNext()) {
				multipleInitialsBufferIndex = bufferIndex;
				multipleInitialsEnd = tokenEnd;
				final String initials =  sandhiedInitialsIterator.next(); // working on this line
				
			}

			if (SkrtSylTokenizer.isSLP(c)) {  // if it's a token char
				if (tokenLength == 0) {                // start of token
				// we enter on two occasions: at the actual start of a token, at each new non-word character. 
				// see (1) for how non-matching word characters are handled
				// this way, we catch the start of new tokens even after any number of non-word characters

					// checking if there is a match in the root of the Trie
					cmdIndex = rootRow.getCmd((char) c);
					potentialEnd = (cmdIndex >= 0);
					if (potentialEnd) {
						potentialEndCmdIndex = cmdIndex;
					}
					// checking if we can continue down the Trie or not
					trieTmp = rootRow.getRef((char) c);
					currentRow = (trieTmp >= 0) ? scanner.getRow(trieTmp) : null;
					tokenStart = offset + bufferIndex - charCount;
					tokenEnd = tokenStart + charCount; // tokenEnd must always be one char in front of tokenStart, because the ending index is exclusive
					if (nonWordStart == -1) {
						nonWordStart = tokenStart; // the starting index of a non-word token won't increment like tokenStart will. the value is attributed only once
					} 
				} else {
				// we enter here on all other occasions: while we have word characters, but we don't know yet if there will be a match or not
					
					// corner case for ioBuffer
					if (tokenLength >= tokenBuffer.length-1) { // check if a supplementary could run out of bounds
						tokenBuffer = termAtt.resizeBuffer(2+tokenLength); // make sure a supplementary fits in the buffer
					}

					tokenEnd += charCount; // incrementing end to correspond to the index of c
					
					// checking if there is a match
					cmdIndex = currentRow.getCmd((char) c);
					potentialEnd = (cmdIndex >= 0);
					if (potentialEnd) {
						potentialEndCmdIndex = cmdIndex;
					}
					// checking if we can continue down the Trie
					trieTmp = currentRow.getRef((char) c);
					currentRow = (trieTmp >= 0) ? scanner.getRow(trieTmp) : null;
				}

				// checking that we can't continue down the Trie (currentRow == null) ensures we do maximal matching.
				if (currentRow == null && potentialEnd == false) {
				// (1) in case we can't continue anymore in the Trie (currentRow == null), but we don't have any match,
				// we consider no word ever started in the first place (tokenLength = 0). it was just a false positive
					
					nonWordChars.append((char) c);
					if (tokenLength > 0) {
					// we reinitialize buffer (through the index of tokenLength and tokenEnd)
						tokenLength = 0;
						tokenEnd = tokenStart + charCount;
					}
				} else if (currentRow == null && potentialEnd == true) {
				// We reached the end of the token: we add c to buffer and resize nonWordChars to exclude the token
					
					tokenLength += Character.toChars(normalize(c), tokenBuffer, tokenLength); // buffer it, normalized 
					nonWordChars.setLength(nonWordChars.length() - (tokenLength - charCount));
					nonWordEnd = tokenEnd - tokenLength; // the end of a non-word can either be: when a matching word starts (potentialEnd == true) or when a non SLP char follows a non-word. see (2)
					break;
				} else {
				// We are within a potential token: we add c to both tokenBuffer and nonWordChars. 
					tokenLength += Character.toChars(normalize(c), tokenBuffer, tokenLength); // buffer it, normalized
					nonWordChars.append((char) c);
					
					if (tokenEnd == dataLen) {
					// we reached the end of the input, reset tokenLength and nonWordEnd and break since on next iteration, we are going to enter "if (bufferIndex >= dataLen){}" then exit incrementToken() with "false"
						if (tokenLength > 0) {
							// we reinitialize tokenBuffer (through the indices tokenLength and tokenEnd)
							tokenLength = 0;
							tokenEnd = tokenStart + charCount;
						}
						nonWordEnd = tokenEnd; // we reached the end of a non-word that is followed by a nonSLP char (current c)
						break;
					}
				}
				if (tokenLength >= MAX_WORD_LEN) { // ioBuffer corner case: buffer overflow! make sure to check for >= surrogate pair could break == test
					break;
				}
			} else if (tokenLength > 0) {        
				break; // found a non-SLP char that is preceded by non-word chars, so break while to return them as a token
			} else if (nonWordChars.toString().length() != 0) {
				nonWordEnd = tokenEnd; // (2) we reached the end of a non-word that is followed by a nonSLP char (current c)
				break;
			}
		}

		// not too sure what these two "if(){}" do
		if (potentialEnd) {
			confirmedEnd = tokenEnd;
			confirmedEndIndex = bufferIndex;
		}
		if (confirmedEnd > 0) {
			bufferIndex = confirmedEndIndex;
			tokenEnd = confirmedEnd;
		}
		termAtt.setLength(tokenEnd - tokenStart); // (4)
		
		final String nonWord = nonWordChars.toString();
		if (nonWord.length() > 0) {
		// there is a non-word. we want to keep the order of the tokens, so we add it with its indices before any extra lemmas.
			extraTokens = new LinkedHashMap<String, Integer[]>(); // in case there is no nonWord to add, extraTokens is initialized at (3)
			extraTokens.put(nonWord, new Integer[] {nonWordStart, nonWordEnd, nonWord.length()});
			if (tokenLength > 0) {
				final String token = String.copyValueOf(tokenBuffer, 0, termAtt.length());  
				extraTokens.put(token, new Integer[] {tokenStart, tokenEnd, token.length()});
			}
			emitExtraTokens = true;
		}
		
		cmd = scanner.getCommandVal(potentialEndCmdIndex);
		if (cmd != null) {
			final Set<String> lemmas = reconstructLemmas(cmd, termAtt.toString()); // (4)
			if (lemmas.size() != 0) {
				if (extraTokens == null) {
					extraTokens = new LinkedHashMap<String, Integer[]>(); // (3) there was no nonWord, but there are extra lemmas
				}
				for (String l: lemmas) {
					extraTokens.put(l, new Integer[] {tokenStart, tokenEnd, l.length()}); // adding every extra lemma, using the same indices for all of them, since they correspond to the same inflected form from the input
				}
				emitExtraTokens = true;

				// restore state to the character just processed, so we can unsandhi the initial and successfully find the start of a potential word in the Trie 
				if (charCount != -1) {
					bufferIndex = bufferIndex - charCount;
				}
				tokenEnd = tokenEnd - charCount;
				sandhiedInitialsIterator = sandhiedInitials.iterator(); // there might be many unsandhied initials behind the current sandhied initial
			}
		}
		
		if (emitExtraTokens) {
			extraTokensIterator = extraTokens.entrySet().iterator();
			@SuppressWarnings("unchecked")
			final Map.Entry<String, Integer[]> extra = (Map.Entry<String, Integer[]>) extraTokensIterator.next();
			termAtt.setEmpty().append(extra.getKey());								// the string of the first token is fed into buffer
			termAtt.setLength(extra.getValue()[2]);									// its size is declared
			finalOffset = correctOffset(extra.getValue()[1]);
			offsetAtt.setOffset(correctOffset(extra.getValue()[0]), finalOffset);	// its offsets are set
			return true;															// we exit incrementToken()
		} else {
		// we enter here when there is no non-word nor any extra lemma to add 

			// termAtt.setLength() is needed before reconstructing lemmas so termAtt.toString() (see (4) ) doesn't return an empty string. that is why it is not here
			assert(tokenStart != -1);
			finalOffset = correctOffset(tokenEnd);
			offsetAtt.setOffset(correctOffset(tokenStart), finalOffset);
			return true;
		}
	}
	
	private void addExtraToken() {
		if (extraTokensIterator.hasNext()) {
			@SuppressWarnings("unchecked")
			final Map.Entry<String, Integer[]> extra = (Map.Entry<String, Integer[]>) extraTokensIterator.next();
			termAtt.setEmpty().append(extra.getKey());
			termAtt.setLength(extra.getValue()[2]);
			finalOffset = correctOffset(extra.getValue()[1]);
			offsetAtt.setOffset(correctOffset(extra.getValue()[0]), finalOffset);
		} else {
			emitExtraTokens = false;
		}
	}
	
	private HashSet<String> reconstructLemmas(String cmd, String inflected) {
		/**
		 * Reconstructs all the possible sandhied strings for the first word using CmdParser.parse(), 
		 * iterates through them, checking if the sandhied string is found in the sandhiable range,
		 * only reconstructs the lemmas if there is a match.
		 * 
		 * 
		 * @return: the list of all the possible lemmas given the current context
		 */ 
		HashSet<String> totalLemmas = new HashSet<String>(); // using HashSet to avoid duplicates
		String[] t = new String[0];
		
		HashMap<String, HashSet<String>> parsedCmd = CmdParser.parse(inflected.substring(inflected.length()-1), cmd);
		for (Entry<String, HashSet<String>> current: parsedCmd.entrySet()) {
			String sandhied = current.getKey();

			if (containsSandhiedCombination(sandhied)) {
				HashSet<String> diffs = current.getValue(); 
				for (String lemmaDiff: diffs) {
					t = lemmaDiff.split("\\+");
					assert(t.length == 2); // all lemmaDiffs should contain +
					int toDelete = Integer.parseInt(t[0]);
					String toAdd;
					String newInitial = "";
					
					if (t[1].contains(",")) { 
					// there is a change in initial
						t = t[1].split(",");
						toAdd = t[0];
						newInitial = t[1]; // TODO: needs to be a possible first element of termAtt#buffer on next iteration of incrementToken()
						if (sandhiedInitials == null) {
							sandhiedInitials = new HashSet<String>();
						}
						sandhiedInitials.add(newInitial);
					} else { 
					// there no change in initial
						toAdd = t[1];
					}
 
					String lemma = inflected.substring(0, inflected.length()-toDelete)+toAdd;
					totalLemmas.add(lemma);
				}
			}
		}
		return totalLemmas;
	}

	private boolean containsSandhiedCombination(String sandhied) {
		/**
 		 * Tells whether sandhied could be found between the two words.
 		 * Does it by generating all the legal combinations, filtering spaces and checking for equality.
 		 * 
 		 * Maximum range of characters where sandhi applies:
		 * - vowel sandhi          : currentCharacter   - currentCharacter+2 (ex. "-O a-"/"-Oa-"  => "-Ava-")
		 * - consonant sandhi1     : currentCharacter   - currentCharacter+2 (ex. "-k y-"/"-ky-"  => "-g y-"/"-gy-")
		 * - consonant sandhi2     : currentCharacter   - currentCharacter+3 (ex. "-n W-"/"-nW-"  => "-Mz W-"/"-MzW-")
		 * - visarga sandhi1       : currentCharacter-1 - currentCharacter+2 (ex. "-aH A-"/"-aHA" => "-A A-"/"-AA")
		 * - visarga sandhi2       : currentCharacter-1 - currentCharacter+2 (ex. "-aH c-"/"-aHc" => "-aS c-"/"-aSc-")
		 * - absolute finals sandhi: currentCharacter   - currentCharacter+X (X = consonant cluster ending a word. only one consonant remains)
		 * - cC words sandhi       : currentCharacter   - currentCharacter+3 (ex. "-a c-"/"-ac-"  => "-a cC-"/"-acC-")
		 * - punar sandhi          : currentCharacter   - currentCharacter+2 (ex. "-r k-"="-rk-"  => "-H k-"/"-Hk-")
		 * 
		 * Given this, each combination must:
		 * 		- start either from 0 or -1
		 * 		- can have a maximal value of 3  
		 * 
		 * TODO: ask to Charles the maximum X can be.
		 * 
		 * @return: true if sandhied is one of the combinations; else otherwise 
		 */
		boolean sandhiable = false;
		char[] inputBuffer = ioBuffer.getBuffer();
		int[][] combinations = new int[][]{
			{0, 1},
			{0, 2},
			{0, 3},
			{-1, 0},
			{-1, 1},
			{-1, 2},
			{-1, 3},
		};
		for (int i = 0; i <= 5; i++) {
			int start = combinations[i][0];
			int end = combinations[i][1];
			String current = "";
			for (char c: Arrays.copyOfRange(inputBuffer, bufferIndex + start, bufferIndex + end)) {
				if (c != ' ') {
					current = current + Character.toString(c);
				}
			}
			if (sandhied.equals(current)) {
				sandhiable = true;
			}
		}
		return sandhiable;
	}

	@Override
	public final void end() throws IOException {
		super.end();
		// set final offset
		offsetAtt.setOffset(finalOffset, finalOffset);
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		bufferIndex = 0;
		offset = 0;
		dataLen = 0;
		finalOffset = 0;
		ioBuffer.reset(); // make sure to reset the IO buffer!!
		
		// for emitting multiple tokens
		emitExtraTokens = false;
	}
}
