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
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.CharacterUtils.CharacterBuffer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

/**
 * A maximal-matching word tokenizer for Sanskrit that uses a {@link Trie}.
 *
 * <p>
 * Loops over a <br>
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

	// attributes allowing to modify the values of the generated terms
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

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
	// ioBuffer related (contains the input string)
	private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
	private static final int MAX_WORD_LEN = 255;
	private static final int IO_BUFFER_SIZE = 4096;
	private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);
	// totalTokens related
	private LinkedHashMap<String, Integer[]> totalTokens = new LinkedHashMap<String, Integer[]>(); // always initialized. value reset when emitExtraToken == false
	private Iterator<Entry<String, Integer[]>> totalTokensIterator;
	private boolean hasTokenToEmit;
	// initials related
	private HashSet<String> initials = null;
	private Iterator<String> initialsIterator = null;
	private StringCharacterIterator initialCharsIterator = null;
	private int initialsOrigBufferIndex = -1;
	private int initialsOrigTokenStart = -1;
	private int initialsOrigTokenEnd = -1;
	private LinkedHashMap<String, Integer[]> potentialTokens = new LinkedHashMap<String, Integer[]>(); // Integer[] contains : {startingIndex, endingIndex, tokenLength, (isItAMatchInTheTrie ? 1 : 0), (isItAMatchInTheTrie ? theIndexOfTheCmd : -1)}
	private static boolean mergesInitials = false;
	private int finalsIndex = -1;

	private StringBuilder nonWordChars = new StringBuilder();

	// current token related
	private int tokenStart;
	private int tokenEnd;
	private int tokenLength;

	private Row currentRow;
	private Row rootRow;
	private int cmdIndex;
	private boolean foundMatch;
	private int foundMatchCmdIndex;
	private boolean foundNonMaxMatch;
	private int nonWordStart;
	private int nonWordEnd;
	private int charCount;

	private int nonMaxTokenStart;
	private int nonMaxTokenEnd;
	private int nonMaxTokenLength;
	private int nonMaxBufferIndex;
	private int nonMaxNonWordLength;

	private HashSet<String> storedInitials = null;

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

		// B.3. ADDING REMAINING EXTRA TOKENS
		if (hasTokenToEmit == true) {
			addExtraToken();
			if (hasTokenToEmit == true) {
				return true;
			} else {
				totalTokens.clear();  // and resume looping over ioBuffer
			}
		}

		tokenLength = 0;
		tokenStart = -1;
		tokenEnd = -1;		
		char[] tokenBuffer = termAtt.buffer();
		// ???
		foundNonMaxMatch = false;
		foundMatchCmdIndex = -1;
		foundMatch = false;
		nonWordStart = -1;
		nonWordEnd = -1;
		resetNonWordChars(0);
		charCount = -1;
		// Trie related
		rootRow = scanner.getRow(scanner.getRoot());
		currentRow = null;
		cmdIndex = -1;
		// initals related
		boolean potentialTokensContainMatches = false;
		// nonMaxToken related
		nonMaxTokenStart = -1;
		nonMaxTokenEnd = -1;
		nonMaxTokenLength = -1;
		nonMaxBufferIndex = -1;
		nonMaxNonWordLength = 0;

		System.out.println("----------------------");
		// A. FINDING TOKENS
		while (true) {
			// **** Deals with the beginning and end of the input string ****
			// (bufferIndex == 0 and bufferIndex == input.length)
			if (bufferIndex >= dataLen) {
				offset += dataLen;
				CharacterUtils.fill(ioBuffer, input); // read supplementary char aware with CharacterUtils
				if (ioBuffer.getLength() == 0) {
					dataLen = 0; // so next offset += dataLen won't decrement offset
					if (tokenLength > 0 || nonWordChars.length() > 0) {
						break;
					} else {
						finalOffset = correctOffset(offset);
						return false;
					}
				}
				dataLen = ioBuffer.getLength();
				bufferIndex = 0;
			}
			// **************************************************************

			// A.1. FILLING c WITH CHARS FROM ioBuffer OR FROM UNSANDHIED INITIALS
			// We want to replace the sandhied initials from ioBuffer by the unsandhied initials in "initials" if there is a sandhi
			// the unsandhied initials (in "initials") replace the sandhied ones (in ioBuffer), then we resume to ioBuffer
			// when the token is consumed (either a match in the Trie or a non-word),
			// we come back to the sandhied character index in ioBuffer and reiterate as long as there are alternative initials

			// take the next char in the input (ioBuffer) for processing it and increment bufferIndex for next value of c
			// (use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based methods are gone)
			int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
			charCount = Character.charCount(c);
			bufferIndex += charCount; // the index for next c

			if (thereAreInitialsToConsume()) {
 				if (currentCharIsSpaceWithinSandhi(c)) {
					continue;  // this allows to move beyond the space in case there is a sandhi that is separated by a space.				

 				} else if (startConsumingInitials()) {
				// we enter here on finalOffset ==  first initials. when all initials are consumed, initials == []
					storeCurrentState();  // save the indices of the current state to be able to restore it later
					initializeInitialCharsIteratorIfNeeded();
					c = applyInitialChar();  // charCount is not updated with the new value of c since we only process SLP, so there are never surrogate pairs

				} else if (stillConsumingInitials()) {
				// we enter here if all initial chars are not yet consumed
					initializeInitialCharsIteratorIfNeeded();
					c = applyInitialChar();  // idem
				}
			}

			System.out.println((char) c);

			// A.2. PROCESSING c
			if (SkrtSylTokenizer.isSLP(c)) {  // if it's a token char
				if (isStartOfTokenOrIsNonwordChar(c)) {
				// we enter on two occasions: at the actual start of a token, at each new non-word character.
				// see (1) for how non-matching word characters are handled
				// this way, we catch the start of new tokens even after any number of non-word characters
					tryToFindMatchIn(rootRow, c); // if foundMatch == true, there is a match 
					tryToContinueDownTheTrie(rootRow, c); // if currentRow != null, we can continue
					incrementTokenIndices();
					ifIsNeededAttributeStartingIndexOfNonword();

				} else {
				// we enter here on all other occasions: while we have word characters, but we don't know yet if there will be a match or not
					// **** corner case for ioBuffer ****
					if (tokenLength >= tokenBuffer.length-1) { // check if a supplementary could run out of bounds
						tokenBuffer = termAtt.resizeBuffer(2+tokenLength); // make sure a supplementary fits in the buffer
					}
					// **********************************

					tokenEnd += charCount; // incrementing to correspond to the index of c					
					tryToFindMatchIn(currentRow, c);
					tryToContinueDownTheTrie(currentRow, c);
					if (reachedNonwordCharacter()) {
						tryToFindMatchIn(rootRow, c);
						tryToContinueDownTheTrie(rootRow, c);
						ifNeededReinitializeTokenBuffer();  // because no word ever started in the first place

					}
				}

				if (wentBeyondLongestMatch()) {
					restoreNonMaxMatchState();
					nonWordEnd = tokenEnd; // needed for term indices
					setTermLength();  // so string in tokenBuffer is correct. (part of Lucene's non-allocation policy)
					
					cleanupPotentialTokens();
					cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
					if (thereIsNoTokenAndNoNonword()) {continue;} else {break;}
					
				} else if (reachedNonwordCharacter()) {
					nonWordChars.append((char) c);
					ifNeededReinitializeTokenBuffer();  // because no word ever started in the first place
					nonWordEnd = tokenEnd; // needed for term indices

				} else if (foundAToken()) {
				// We reached the end of the token: we add c to buffer and cut off the token from nonWordChars
					IncrementTokenLengthAndAddCurrentCharTo(tokenBuffer, c);
					cutOffTokenFromNonWordChars();
					setTermLength();  // so string in tokenBuffer is correct. (part of Lucene's non-allocation policy)
					
					if (allCharsFromCurrentInitialAreConsumed()) {
						potentialTokensContainMatches = true; // because we reachedEndOfToken
						addFoundTokenToPotentialTokensIfThereIsOne(tokenBuffer);
						if (allInitialsAreConsumed()) {
							
							cleanupPotentialTokens();
							cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
							if (thereIsNoTokenAndNoNonword()) {continue;} else {break;} // if break, resume looping over ioBuffer
						}
						resetInitialCharsIterator();
						restorePreviousState();
					} else {
						cleanupPotentialTokens();
						cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
						if (thereIsNoTokenAndNoNonword()) {continue;} else {break;}
					}
				} else { // we are within a potential token
					IncrementTokenLengthAndAddCurrentCharTo(tokenBuffer, c);
					nonWordChars.append((char) c); // now adds every char, but will cut off found tokens

					if (reachedEndOfInputString()) {
						ifNeededReinitializeTokenBuffer();
						nonWordEnd = tokenEnd; // needed for term indices

						if (allCharsFromCurrentInitialAreConsumed()) {
							addNonwordToPotentialTokens(); // we have a non-word token
							if (allInitialsAreConsumed()) {
								cleanupPotentialTokens();
								cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
								if (thereIsNoTokenAndNoNonword()) {continue;} else {break;}
							}
						} else {
							setTermLength();  // so string in tokenBuffer is correct. (part of Lucene's non-allocation policy)
							cleanupPotentialTokens();
							cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
							if (thereIsNoTokenAndNoNonword()) {continue;} else {break;}
						}					
					}
				}
				// **** ioBuffer corner case: buffer overflow! ****
				// make sure to check for >= surrogate pair could break == test
				if (tokenLength >= MAX_WORD_LEN) {
					cleanupPotentialTokens();
					cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
					if (thereIsNoTokenAndNoNonword()) {continue;} else {break;}
				}
				// ************************************************
			} else if (isNonSLPprecededByNonword()) {
				if (allCharsFromCurrentInitialAreConsumed()) {
				// all initial chars are consumed and we have a non-word token
					nonWordEnd = tokenEnd; // needed for term indices
					addNonwordToPotentialTokens();
					if (allInitialsAreConsumed()) {
						cleanupPotentialTokens();
						cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
						if (thereIsNoTokenAndNoNonword()) {continue;} else {break;} // if break, resume looping over ioBuffer
					}
					resetNonWordChars(0);
					resetInitialCharsIterator();
					restorePreviousState();					
				} else {
					cleanupPotentialTokens();
					cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
					if (thereIsNoTokenAndNoNonword()) {continue;} else {break;}
				}
			} else if (isNonSLPprecededByNotEmptyNonWord()) {
				nonWordEnd = tokenEnd; // needed for term indices
				// (2) we reached the end of a non-word that is followed by a nonSLP char (current c)

				if (allCharsFromCurrentInitialAreConsumed()) {
				// all initial chars are consumed and we have a non-word token
					addFoundTokenToPotentialTokensIfThereIsOne(tokenBuffer);
					if (allInitialsAreConsumed()) {
						cleanupPotentialTokens();
						cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens()  
						if (thereIsNoTokenAndNoNonword()) {continue;} else {break;} // if break, resume looping over ioBuffer
					}
					resetNonWordChars(0);
					resetInitialCharsIterator();
					restorePreviousState();					
				} else {
					cleanupPotentialTokens();
					cleanupNonWords(); // resets storeInitials to null, so must be executed after setTermLength() and cleanupPotentialTokens() 
					if (thereIsNoTokenAndNoNonword()) {continue;} else {break;}
				}
			}
		}

		// B. HANDING THEM TO LUCENE

		// all the initials from last sandhi have been consumed, reinitializing "initials". new initials can be added by next call(s) of reconstructLemmas()
		initials = null;
		initialCharsIterator = null;
		
		// B.1. FILLING totalTokens
		if (unsandhyingInitialsYieldedPotentialTokens()) {				
			if (potentialTokensContainMatches) {
			// there is one or more potential tokens that are matches in the Trie (second last value of potentialTokens[potentialToken] == 1)
				unsandhiFinalsAndAddLemmatizedMatchesToTotalTokens();

			} else {  // there are only non-words in potentialTokens (last value of potentialTokens[potentialToken] == 0)
				ifThereIsNonwordAddItToTotalTokens();
			}
			potentialTokens.clear();  // all potential tokens have been consumed, empty the variable

			ifSandhiMergesStayOnSameCurrentChar(); // so we can unsandhi the initial and successfully find the start of a potential word in the Trie
		} else {  // general case: no potential tokens

			boolean aNonwordWasAdded = ifThereIsNonwordAddItToTotalTokens();
			if (aNonwordWasAdded) {
				ifThereIsMatchAddItToTotalTokens(tokenBuffer);
			}

			boolean lemmasWereAdded = ifUnsandhyingFinalsYieldsLemmasAddThemToTotalTokens();
			if (lemmasWereAdded) {
				ifSandhiMergesStayOnSameCurrentChar(); // so we can unsandhi the initial and successfully find the start of a potential word in the Trie
				tokenEnd -= charCount;  // TODO not really sure how come this is needed
				
				finalsIndex = bufferIndex; // save index of finals for currentCharIsSpaceWithinSandhi()
			}
		}
		
		// B.2. EXITING incrementToken() WITH THE TOKEN (OR THE FIRST ONE FROM totalTokens)
		ifConsumedAllInitialsResetInitialsAndIterator();  //  so we don't create an empty iterator
		ifThereAreInitialsFillIterator();  // for next iteration of incrementToken()
		ifEndOfInputReachedEmptyInitials();

		if (thereAreTokensToReturn()) {
			hasTokenToEmit = true;
			final Map.Entry<String, Integer[]> firstToken = takeFirstToken();
			fillTermAttributeWith(firstToken);
			changeTypeOfNonwords(firstToken);
			return true;  // we exit incrementToken()

		} else { // there is a match without sandhi
		// we enter here when there is no non-word nor any extra lemma to add
			assert(tokenStart != -1);
			finalizeSettingTermAttribute();
			return true;  // we exit incrementToken()
		}
	}

	private void cleanupNonWords() {
		if (storedInitials != null) {
			final String nonword = nonWordChars.toString();
			if (storedInitials.contains(nonword)) {
				resetNonWordChars(0);
				storedInitials = null; // !!! only reset after executing cleanupPotentialTokens() and setTermLength()
			}
		}
	}

	private void cleanupPotentialTokens() {
		if (storedInitials != null) {
			for (String key: storedInitials) {
				 if (potentialTokens.containsKey(key)) {
					 potentialTokens.remove(key);
				 }
			}
		}
	}

	private void ifEndOfInputReachedEmptyInitials() {
		if (bufferIndex + 1 == dataLen) {
			initials = null;
		}
	}

	private void setTermLength() {
		// goes together with finalizeSettingTermAttribute().
		termAtt.setLength(tokenEnd - tokenStart);
		if (storedInitials != null && storedInitials.contains(termAtt.toString())) {
			termAtt.setEmpty();
		}
	}

	private void finalizeSettingTermAttribute() {
		// the token string is already set in tokenBuffer, a view into TermAttribute
		finalOffset = correctOffset(tokenEnd);
		offsetAtt.setOffset(correctOffset(tokenStart), finalOffset);
	}

	private void changeTypeOfNonwords(Entry<String, Integer[]> token) {
		if (token.getValue()[3] == 0) {  
			typeAtt.setType("non-word"); // default type value: "word"
		}
	}

	private void fillTermAttributeWith(Entry<String, Integer[]> token) {
		termAtt.setEmpty().append(token.getKey());	// add the token string
		termAtt.setLength(token.getValue()[2]);  // declare its size
		finalOffset = correctOffset(token.getValue()[1]);  // get final offset 
		offsetAtt.setOffset(correctOffset(token.getValue()[0]), finalOffset);	// set its offsets (initial & final)

	}

	private Entry<String, Integer[]> takeFirstToken() {
		totalTokensIterator = totalTokens.entrySet().iterator(); // fill iterator
		return (Map.Entry<String, Integer[]>) totalTokensIterator.next();
	}

	private void ifThereAreInitialsFillIterator() {
		if (initials != null && !initials.isEmpty()) {  // 
			initialsIterator = initials.iterator(); // there might be many unsandhied initials behind the current sandhied initial
		}
	}

	private void ifConsumedAllInitialsResetInitialsAndIterator() {
		if (initials != null && initials.isEmpty()) {
			initials = null;
			initialsIterator = null;
		}
	}

	private void ifSandhiMergesStayOnSameCurrentChar() {
		if (charCount != -1 && mergesInitials) { // if sandhi merges
			if (bufferIndex < dataLen) { // if the end of the input has not been reached. (the reconstructed initials can't give any new token)
				bufferIndex -= charCount;
			}
			mergesInitials = false; // reinitialize variable
		}
	}

	private boolean ifUnsandhyingFinalsYieldsLemmasAddThemToTotalTokens() {
		String cmd = scanner.getCommandVal(foundMatchCmdIndex);
		if (cmd != null) {
			final Set<String> lemmas = reconstructLemmas(cmd, termAtt.toString()); // (4)
			if (lemmas.size() != 0) {
				for (String l: lemmas) {
					totalTokens.put(l, new Integer[] {tokenStart, tokenEnd, l.length(), 1}); // (5) adding every extra lemma, using the same indices for all of them, since they correspond to the same inflected form from the input
				}
				return true;
			}
		}
		return false;
	}

	private void ifThereIsMatchAddItToTotalTokens(char[] tokenBuffer) {
		if (tokenLength > 0) {
			final String token = String.copyValueOf(tokenBuffer, 0, termAtt.length());
			totalTokens.put(token, new Integer[] {tokenStart, tokenEnd, token.length(), 1}); // (5)
		}
	}

	private boolean ifThereIsNonwordAddItToTotalTokens() {
		final String nonWord = nonWordChars.toString();
		if (nonWord.length() > 0) {
			totalTokens.put(nonWord, new Integer[] {nonWordStart, nonWordEnd, nonWord.length(), 0}); // (5) ignore all potential tokens. add the non-word with sandhied initials
			return true;
		}
		return false;
	}

	private void unsandhiFinalsAndAddLemmatizedMatchesToTotalTokens() {
		for (Entry<String, Integer[]> entry: potentialTokens.entrySet()) {
		// add all potential tokens except if they are non-words (value[3] == 0)
			final String key = entry.getKey();
			final Integer[] value = entry.getValue();
			if (value[3] == 1) {
				String cmd = scanner.getCommandVal(value[4]);
				final Set<String> lemmas = reconstructLemmas(cmd, key); // (4) note: there can be more than one call of reconstructLemmas(), but initials filters duplicates (HashSet) and all potential lemmas yield the same initials because they only differ on initials
				if (lemmas.size() != 0) {
				// there are more than one lemma. this can happen because eventhough we have unsandhied the initials of the current form, the finals have not been analyzed
					for (String l: lemmas) {
						totalTokens.put(l, new Integer[] {value[0], value[1], value[2], value[3]}); // (5) adding every extra lemma, using the same indices for all of them, since they correspond to the same inflected form from the input
					}
				} else {
				// the finals of the current form are not sandhied, so there is only one token to add
					totalTokens.put(key, new Integer[] {value[0], value[1], value[2], value[3]}); // (5)
				}
			}
		}
	}

	private void cutOffTokenFromNonWordChars() {
		nonWordChars.setLength(nonWordChars.length() - (tokenLength - charCount));
		nonWordEnd = tokenEnd - tokenLength; // the end of a non-word can either be: when a matching word starts (potentialEnd == true) or when a non SLP char follows a non-word. see (2)
	}

	private void IncrementTokenLengthAndAddCurrentCharTo(char[] tokenBuffer, int c) {
		// normalize c and add it to tokenBuffer
		tokenLength += Character.toChars(normalize(c), tokenBuffer, tokenLength);
	}

	private void ifIsNeededAttributeStartingIndexOfNonword() {
		// the starting index of a non-word token is attributed once.
		// it doesn't increment like token indices
		if (nonWordStart == -1) {
			nonWordStart = tokenStart;
		}
	}

	private void incrementTokenIndices() {
		tokenStart = offset + bufferIndex - charCount;
		tokenEnd = tokenStart + charCount; // tokenEnd must always be one char ahead of tokenStart, because the ending index is exclusive
	}

	private void tryToContinueDownTheTrie(Row row, int c) {
		// check done modifying values in place.
		int ref = row.getRef((char) c);
		currentRow = (ref >= 0) ? scanner.getRow(ref) : null;
	}

	private void tryToFindMatchIn(Row row, int c) {
		// check done modifying values in place.
		cmdIndex = row.getCmd((char) c);
		foundMatch = (cmdIndex >= 0);
		if (foundMatch) {
			foundMatchCmdIndex = cmdIndex;
			foundNonMaxMatch = storeNonMaxMatchState();
		}
	}

	private boolean storeNonMaxMatchState() {
		nonMaxBufferIndex = bufferIndex;
		nonMaxTokenStart = tokenStart;
		nonMaxTokenEnd = tokenEnd;
		nonMaxTokenLength = tokenLength;
		if (nonWordChars.length() - 2 <= 0) {
			nonMaxNonWordLength = 0;
		} else {
			nonMaxNonWordLength = nonWordChars.length()-2;
		}
		return true;
	}

	private void restoreNonMaxMatchState() {
		bufferIndex = nonMaxBufferIndex;
		tokenStart = nonMaxTokenStart;
		tokenEnd = nonMaxTokenEnd;
		tokenLength = nonMaxTokenLength;
		currentRow = rootRow;
		resetNonWordChars(nonMaxNonWordLength);
	}

	private void ifNeededReinitializeTokenBuffer() {
		// we reinitialize tokenBuffer (through the index of tokenLength) and tokenEnd
		if (tokenLength > 0) {
			tokenLength = 0;
			tokenEnd = tokenStart + charCount;
		}
	}

	private void restorePreviousState() {
		// restore the previous state, return to the beginning of the token in ioBuffer
		bufferIndex = initialsOrigBufferIndex;
		tokenStart = initialsOrigTokenStart;
		tokenEnd = initialsOrigTokenEnd;
		tokenLength = 0;
		currentRow = rootRow;
	}

	private void resetNonWordChars(int i) {
		if (nonWordChars.length() - i > 1) {
			nonWordChars.setLength(i);
		} else {
			nonWordChars.setLength(0);
		}
	}

	private void addNonwordToPotentialTokens() {
		// add the token just found to totalTokens
		final String potentialToken = nonWordChars.toString();
		potentialTokens.put(potentialToken,  new Integer[] {nonWordStart, nonWordEnd, potentialToken.length(), 0, -1});
	}

	private void addFoundTokenToPotentialTokensIfThereIsOne(char[] tokenBuffer) {
		// add the token just found to totalTokens
		if (tokenLength > 0) { 
		// avoid empty tokens
			final String potentialToken = String.copyValueOf(tokenBuffer, 0, tokenLength);
			potentialTokens.put(potentialToken,  new Integer[] {tokenStart, tokenEnd, potentialToken.length(), 1, foundMatchCmdIndex});
		}
	}

	private void initializeInitialCharsIteratorIfNeeded() {
		if (initialCharsIterator == null) {
		// initialize the iterator with the first initials
			initialCharsIterator = new StringCharacterIterator(initialsIterator.next());
		} else if (initialCharsIterator.getIndex() == 0 && initialsIterator.hasNext()) {
		// either first time or initialCharsIterator has been reset AND there are more initials to process
		// when we reach the end of a token (either a Trie match or a non-word), the char iterator is reinitialized (see (a) )
			initialCharsIterator.setText(initialsIterator.next()); // reset the iterator with the next initials, to avoid a re-allocation
		}

		initialsIterator.remove(); // remove the initials just put in the iterator
	}

	private int applyInitialChar() {
		int initial = initialCharsIterator.current();
		initialCharsIterator.setIndex(initialCharsIterator.getIndex()+1); // increment iterator index
		return initial;
	}

	private void storeCurrentState() {
		initialsOrigBufferIndex = bufferIndex - 1;
		initialsOrigTokenStart = tokenStart;
		initialsOrigTokenEnd = tokenEnd;
	}

	private void addExtraToken() { // for comments, see after "if (!totalTokens.isEmpty()) {...}"
		if (totalTokensIterator.hasNext()) {
			final Map.Entry<String, Integer[]> extra = (Map.Entry<String, Integer[]>) totalTokensIterator.next();
			termAtt.setEmpty().append(extra.getKey());
			if (extra.getValue()[3] == 0) {
				typeAtt.setType("non-word");
			}
			termAtt.setLength(extra.getValue()[2]);
			finalOffset = correctOffset(extra.getValue()[1]);
			offsetAtt.setOffset(correctOffset(extra.getValue()[0]), finalOffset);
		} else {
			hasTokenToEmit = false;
		}
	}
	
	final private boolean thereIsNoTokenAndNoNonword() {
		return tokenLength == 0 && nonWordChars.length() == 0;
	}
	
	final private boolean wentBeyondLongestMatch() {
		return foundNonMaxMatch && foundMatch == false;
	}

	final private boolean thereAreTokensToReturn() {
		return !totalTokens.isEmpty();
	}
	
	final private boolean reachedNonwordCharacter() {
		// (1) in case we can't continue anymore in the Trie (currentRow == null), but we don't have any match,
		return currentRow == null && foundMatch == false;
		// checking that we can't continue down the Trie (currentRow == null) ensures we do maximal matching.
	}
	
	final private boolean unsandhyingInitialsYieldedPotentialTokens() {
		return !potentialTokens.isEmpty();
	}

	final private boolean isNonSLPprecededByNotEmptyNonWord() {
		return nonWordChars.toString().length() != 0;
	}

	final private boolean isNonSLPprecededByNonword() {
		return tokenLength > 0;
	}

	final private boolean reachedEndOfInputString() {
		return tokenEnd == dataLen;
	}

	final private boolean allCharsFromCurrentInitialAreConsumed() {
		return initials != null && initialCharsIterator.current() == CharacterIterator.DONE;
	}

	final private boolean isStartOfTokenOrIsNonwordChar(int c) {
		return tokenLength == 0;
	}

	final private boolean startConsumingInitials() {
		return initialCharsIterator == null;
	}

	final private boolean stillConsumingInitials() {
		return (initialCharsIterator.getIndex() < initialCharsIterator.getEndIndex()) ? true : false;
	}

	final private boolean currentCharIsSpaceWithinSandhi(int c) {
		return c == ' ' && bufferIndex == finalsIndex + 1;
	}

	final private boolean allInitialsAreConsumed() {
		return !initialsIterator.hasNext();
	}

	final private void resetInitialCharsIterator() {
		initialCharsIterator.setIndex(0);
	}

	final private boolean thereAreInitialsToConsume() {
		return initials != null && !initials.isEmpty() && bufferIndex != dataLen;
	}

	final private boolean foundAToken() {
		return currentRow == null && foundMatch == true;
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
			HashSet<String> diffs = current.getValue();

			for (String lemmaDiff: diffs) {
				assert(lemmaDiff.contains("+")); // all lemmaDiffs should contain +

				t = lemmaDiff.split("=");
				int sandhiType = Integer.parseInt(t[1]);
				if (sandhiType == 0) {
				// there is no sandhi, so we skip this diff
					continue; 
				}
				String diff = t[0];
				if (containsSandhiedCombination(ioBuffer.getBuffer(), bufferIndex - 1, sandhied, sandhiType)) {

					t = diff.split("\\+");

					// ensures t has alway two elements
					if (diff.endsWith("+")) {
						t = new String[2];
						t[0] = diff.split("\\+")[0];
						t[1] = "";
					}
					if (diff.startsWith("+")) {
						t = new String[2];
						t[0] = "";
						t[1] = diff.split("\\+")[0];
					}

					int toDelete = Integer.parseInt(t[0]);
					String toAdd;
					String newInitial = "";

					if (t[1].contains("/")) {
					// there is a change in initial
						t = t[1].split("/");
						toAdd = t[0];
						newInitial = t[1]; // TODO: needs to be a possible first element of termAtt#buffer on next iteration of incrementToken()
						if (initials == null) {
							initials = new HashSet<String>();
							storedInitials = new HashSet<String>(); // because deep-copying seems difficult
						}
						initials.add(newInitial);
						storedInitials.add(newInitial);
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

	public static boolean containsSandhiedCombination(char[] buffer, int bufferIndex, String sandhied, int sandhiType) {
		/**
 		 * Tells whether sandhied could be found between the two words.
 		 * Does it by generating all the legal combinations, filtering spaces and checking for equality.
 		 *
		 * See SamdhiedCombinationTests for how these figures were obtained
		 *
		 * @return: true if sandhied is one of the combinations; else otherwise
		 */
		int[][] combinations;

		switch(sandhiType) {
		case 0: // no sandhi
			return false; // no sandhi is possible

		case 1: // vowel sandhi
			if (sandhied.length() == 1) {
				mergesInitials = true;
			}
			combinations = new int[][]{{0, 3}, {0, 2}, {0, 1}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 2: // consonant sandhi 1
			combinations = new int[][]{{0, 2}, {0, 1}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

//		 case 3: // consonant sandhi 1 vowels
//			combinations = new int[][]{{-1, 2}, {-1, 3}, {-1, 4}};
//			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);
			
		case 3: // consonant sandhi 2
			combinations = new int[][]{{0, 4}, {0, 3}, {0, 2}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 4: // visarga sandhi
			combinations = new int[][]{{-1, 3}, {-1, 2}, {-1, 1}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 5: // absolute finals sandhi (consonant clusters are always reduced to the first consonant)
			combinations = new int[][]{{0, 1}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 6: // "cC"-words sandhi
			combinations = new int[][]{{0, 4}, {0, 3}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 7: // special sandhi: "punar"
			combinations = new int[][]{{-4, 3}, {-4, 2}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		default:
			return false;
		}
	}

	public static boolean isSandhiedCombination(char[] inputBuffer, int bufferIndex, String sandhied, int[][] combinations) {
		for (int i = 0; i <= combinations.length-1; i++) {
			int start = combinations[i][0];
			int end = combinations[i][1];
			String current = "";
			for (char c: Arrays.copyOfRange(inputBuffer, bufferIndex + start, bufferIndex + end)) {
				current += Character.toString(c);
			}
			if (sandhied.equals(current) || sandhied.equals(current.replaceAll(" ", ""))) {
				return true;
			}
		}
		return false;
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

		finalsIndex = -1;

		// for emitting multiple tokens
		hasTokenToEmit = false;

	}
}