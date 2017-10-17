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
 * 	The expected input is an SLP string<br>
 *	{@link SkrtSylTokenizer#isSLP(int)} is used to filter out nonSLP characters.
 *
 * <p>
 * The necessary information for unsandhying finals and initials are obtained from 
 * 
 * <br>
 * Due to its design, this tokenizer doesn't deal with contextual ambiguities.<br>
 * For example, "nagaraM" could either be a word of its own or "na" + "garam"
 *
 * Derived from Lucene 6.4.1 analysis.
 *
 * @author Élie Roux
 * @author Drupchen
 *
 */
public final class SkrtWordTokenizer extends Tokenizer {
	private Trie scanner;
	private boolean debug = false;

	/* attributes allowing to modify the values of the generated terms */
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	/**
	 * Constructs a SkrtWordTokenizer using a file
	 * @param filepath: input file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public SkrtWordTokenizer(String filepath) throws FileNotFoundException, IOException {
		init(filepath);
	}

	/**
	 * Same as above, but prints debug info:
	 * 		- a line of dashes at each iteration of incrementToken()
	 * 		- the current character
	 * @param debug
	 * @param filepath
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public SkrtWordTokenizer(boolean debug, String filepath) throws FileNotFoundException, IOException {
		init(filepath);
		this.debug = debug;
	}
	
	/**
	 * Constructs a SkrtWordTokenizer using a default lexicon file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public SkrtWordTokenizer() throws FileNotFoundException, IOException {
		init("resources/sanskrit-stemming-data/output/total_output.txt");
	}

	/**
	 * Initializes and populates {@see #scanner}
	 *
	 * The format of each line in filename must be as follows: "<sandhied_inflected_form>,<initial>~<diffs>/<initial_diff>"
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
	/* current token related */
	private int tokenStart, tokenEnd, tokenLength;
	private Row rootRow, currentRow;
	private int cmdIndex, foundMatchCmdIndex;
	private boolean foundMatch;
	
	/* nonMaxMatch related */
	private boolean foundNonMaxMatch;
	private int nonMaxTokenStart, nonMaxTokenEnd, nonMaxTokenLength, nonMaxBufferIndex, nonMaxNonWordLength;
	
	/* tokens related */
	private LinkedHashMap<String, Integer[]> potentialTokens = new LinkedHashMap<String, Integer[]>();	
	// Integer[] contains : {startingIndex, endingIndex, tokenLength, (isItAMatchInTheTrie ? 1 : 0), 
	//												(isItAMatchInTheTrie ? theIndexOfTheCmd : -1)}

	/* nonWords related */
	private int nonWordStart;
	private int nonWordEnd;
	private StringBuilder nonWordChars = new StringBuilder();
	
	/* totalTokens related */
	private LinkedHashMap<String, Integer[]> totalTokens = new LinkedHashMap<String, Integer[]>();
	private Iterator<Entry<String, Integer[]>> totalTokensIterator;
	private boolean hasTokenToEmit;
	
	/* initials related */
	private HashSet<String> initials = null;			// it is HashSet to filter duplicate initials
	private Iterator<String> initialsIterator = null;
	private StringCharacterIterator initialCharsIterator = null;
	private int initialsOrigBufferIndex = -1, initialsOrigTokenStart = -1, initialsOrigTokenEnd = -1;
	private HashSet<String> storedInitials = null;
	private static boolean mergesInitials = false;
	private int finalsIndex = -1;

	/* ioBuffer related (contains the input string) */
	private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
	private int charCount;
	private static final int MAX_WORD_LEN = 255, IO_BUFFER_SIZE = 4096;
	private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);
	
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

		/* B.3. ADDING REMAINING EXTRA TOKENS */
		if (hasTokenToEmit == true) {
			addExtraToken();
			if (hasTokenToEmit == true) {
				return true;
			} else {
				totalTokens.clear();		// and resume looping over ioBuffer
			}
		}

		tokenStart = -1;
		tokenEnd = -1;	
		tokenLength = 0;
		rootRow = scanner.getRow(scanner.getRoot());
		currentRow = null;
		cmdIndex = -1;
		foundMatchCmdIndex = -1;
		foundMatch = false;

		foundNonMaxMatch = false;
		nonMaxTokenStart = -1;		
		nonMaxTokenEnd = -1;
		nonMaxTokenLength = -1;
		nonMaxBufferIndex = -1;
		nonMaxNonWordLength = 0;

		nonWordStart = -1;
		nonWordEnd = -1;
		resetNonWordChars(0);
		
		charCount = -1;
		
		char[] tokenBuffer = termAtt.buffer();
		boolean potentialTokensContainMatches = false;
		
		if (debug) {System.out.println("----------------------");}

		/* A. FINDING TOKENS */
		while (true) {

			/*>>> Deals with the beginning and end of the input string >>>>>>>>>*/
			if (bufferIndex >= dataLen) {
				offset += dataLen;
				CharacterUtils.fill(ioBuffer, input);			// read supplementary char aware with CharacterUtils
				if (ioBuffer.getLength() == 0) {
					dataLen = 0;								// so next offset += dataLen won't decrement offset
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
			/*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*/

			/* A.1. FILLING c WITH CHARS FROM ioBuffer OR FROM UNSANDHIED INITIALS
			 * 
			 * In case there are initials to consume:
			 * 		- store the current state
			 * 		- replace c with first initial
			 * 		- resume looping over ioBuffer
			 * 		- when a token or a nonWord ends AND there are more initials:
			 * 				- restore state
			 * 				- do as before
			 */
 
			/* (use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based methods are gone) */
			int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());	// take next char in ioBuffer
			charCount = Character.charCount(c);
			bufferIndex += charCount; 			// increment bufferIndex for next value of c

			if (thereAreInitialsToConsume()) {
 				if (currentCharIsSpaceWithinSandhi(c)) {
					continue;		// if there is a space in the sandhied substring, moves beyond the space				

 				} else if (startConsumingInitials()) {	
 				/* we enter here on finalOffset ==  first initials. (when all initials are consumed, initials == []) */
					storeCurrentState();
					initializeInitialCharsIteratorIfNeeded();
					c = applyInitialChar();

				} else if (stillConsumingInitials()) {
				/* we enter here if all initial chars are not yet consumed */
					initializeInitialCharsIteratorIfNeeded();
					c = applyInitialChar();
				}
			}

			if (debug) {System.out.println((char) c);}

			/* A.2. PROCESSING c */
			
			/* A.2.1) if it's a token char */
			if (isSLPTokenChar(c)) {
				
				/* Go one step down the Trie */
				if (isStartOfTokenOrIsNonwordChar(c)) {
				/* we enter on two occasions: at the actual start of a token and at each new non-word character. */
					tryToFindMatchIn(rootRow, c);					// if foundMatch == true, there is a match  
					tryToContinueDownTheTrie(rootRow, c);			// if currentRow != null, can continue
					incrementTokenIndices();
					ifIsNeededAttributeStartingIndexOfNonword();

				} else {
				/* we enter here on all other occasions: we don't know if word chars will be a match or not */
					
					/*>>> corner case for ioBuffer >>>>>>>*/
					if (tokenLength >= tokenBuffer.length-1) {			// check if a supplementary could run out of bounds
						tokenBuffer = termAtt.resizeBuffer(2+tokenLength);	// make sure a supplementary fits in the buffer
					}
					/*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*/

					tokenEnd += charCount;							// incrementing to correspond to the index of c					
					tryToFindMatchIn(currentRow, c);
					tryToContinueDownTheTrie(currentRow, c);
					if (reachedNonwordCharacter()) {
						tryToFindMatchIn(rootRow, c);
						tryToContinueDownTheTrie(rootRow, c);
						ifNeededReinitializeTokenBuffer();			// because no word ever started in the first place

					}
				}
				
				/* Decide what to do with the SLP chars currently processed */
				if (wentBeyondLongestMatch()) {
					restoreNonMaxMatchState();
					nonWordEnd = tokenEnd;				// needed for term indices
					setTermLength();					// so string in tokenBuffer is correct. (non-allocation policy)
					cleanupPotentialTokensAndNonwords();
					break;
					
				} else if (reachedNonwordCharacter()) {
					nonWordChars.append((char) c);
					ifNeededReinitializeTokenBuffer();		// because no word ever started in the first place
					nonWordEnd = tokenEnd;					// needed for term indices

				} else if (foundAToken()) {
					IncrementTokenLengthAndAddCurrentCharTo(tokenBuffer, c);
					cutOffTokenFromNonWordChars();
					setTermLength();											// same as above
					
					if (allCharsFromCurrentInitialAreConsumed()) {
						potentialTokensContainMatches = true;					// because we reachedEndOfToken
						addFoundTokenToPotentialTokensIfThereIsOne(tokenBuffer);
						if (allInitialsAreConsumed()) {
							cleanupPotentialTokensAndNonwords();
							break;
						}
						resetInitialCharsIterator();
						restorePreviousState();
					} else {
						cleanupPotentialTokensAndNonwords(); 
						break;
					}
				} else {													// we are within a potential token
					IncrementTokenLengthAndAddCurrentCharTo(tokenBuffer, c);
					nonWordChars.append((char) c);							// later remove chars belonging to a token

					if (reachedEndOfInputString()) {
						ifNeededReinitializeTokenBuffer();
						nonWordEnd = tokenEnd;								// needed for term indices

						if (allCharsFromCurrentInitialAreConsumed()) {
							addNonwordToPotentialTokens();					// we do have a non-word token
							if (allInitialsAreConsumed()) {
								cleanupPotentialTokensAndNonwords(); 
								break;
							}
						} else {
							setTermLength();								// same as above
							cleanupPotentialTokensAndNonwords(); 
							break;
						}					
					}
				}
				
				/*>>>>>> ioBuffer corner case: buffer overflow! >>>*/
				if (tokenLength >= MAX_WORD_LEN) {		// make sure to check for >= surrogate pair could break == test
					break;
				}
				/*<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<*/
			
			/* A.2.2) if it is not a token char */
			} else if (isNonSLPprecededByNonword()) {			// we have a nonword token
				setTermLength();								// same as above
				if (allCharsFromCurrentInitialAreConsumed()) {
					addNonwordToPotentialTokens();
					if (allInitialsAreConsumed()) {
						cleanupPotentialTokensAndNonwords();
						break;
					}
					resetNonWordChars(0);
					resetInitialCharsIterator();
					restorePreviousState();					
				} else {
					cleanupPotentialTokensAndNonwords(); 
					if (isSLPModifier(c)) {
						continue;								// move on and do as if the modifier didn't exist
					} else {
						break;
					}
				}
			} else if (isNonSLPprecededByNotEmptyNonWord()) {
				nonWordEnd = tokenEnd;							// needed for term indices

				if (allCharsFromCurrentInitialAreConsumed()) {
					addFoundTokenToPotentialTokensIfThereIsOne(tokenBuffer);
					if (allInitialsAreConsumed()) {
						cleanupPotentialTokensAndNonwords();  
						if (thereIsNoTokenAndNoNonword()) {
							continue;							// resume looping over ioBuffer
						} else {
							break;								// and resume looping over ioBuffer
						}
					}
					resetNonWordChars(0);
					resetInitialCharsIterator();
					restorePreviousState();					
				} else {
					cleanupPotentialTokensAndNonwords(); 
					break;
				}
			} 
		}

		/* B. HANDING THEM TO LUCENE */
		initials = null;				// all initials are consumed. reinitialize for next call of reconstructLemmas()
		initialCharsIterator = null;
		
		/* B.1. FILLING totalTokens */
		if (unsandhyingInitialsYieldedPotentialTokens()) {				
			if (potentialTokensContainMatches) {
				unsandhiFinalsAndAddLemmatizedMatchesToTotalTokens();

			} else {
				ifThereIsNonwordAddItToTotalTokens();
			}
			potentialTokens.clear();				// all potential tokens have been consumed, empty the variable
			ifSandhiMergesStayOnSameCurrentChar();	// so we can unsandhi the initial and find the start of next word

		} else {									// general case: no potential tokens
			boolean aNonwordWasAdded = ifThereIsNonwordAddItToTotalTokens();
			if (aNonwordWasAdded) {
				ifThereIsMatchAddItToTotalTokens(tokenBuffer);
			}

			boolean lemmasWereAdded = ifUnsandhyingFinalsYieldsLemmasAddThemToTotalTokens();
			if (lemmasWereAdded) {
				ifSandhiMergesStayOnSameCurrentChar();	// so we can unsandhi the initial and find the start of next word
				tokenEnd -= charCount;					// TODO check if this adjusts the token offsets
				
				finalsIndex = bufferIndex;				// save index of finals for currentCharIsSpaceWithinSandhi()
			}
		}
		
		/* B.2. EXITING incrementToken() WITH THE TOKEN (OR THE FIRST ONE FROM totalTokens) */
		ifConsumedAllInitialsResetInitialsAndIterator();	// so we don't create an empty iterator
		ifThereAreInitialsFillIterator();
		ifEndOfInputReachedEmptyInitials();

		if (thereAreTokensToReturn()) {
			hasTokenToEmit = true;
			final Map.Entry<String, Integer[]> firstToken = takeFirstToken();
			fillTermAttributeWith(firstToken);
			changeTypeOfNonwords(firstToken);
			return true;						// we exit incrementToken()

		} else if (reachedEndOfInputString() && thereIsNoTokenAndNoNonword()) {
			return false;													// exit the tokenizer. input was only nonSLP
		
		} else {					// there is no non-word nor extra lemma to add. there was no sandhi for this token 			
			assert(tokenStart != -1);
			finalizeSettingTermAttribute();
			return true;						// we exit incrementToken()
		}
	}

	private void cleanupPotentialTokensAndNonwords() {
		if (storedInitials != null) {
			
			/* cleanup potentialTokens */
			for (String key: storedInitials) {
				 if (potentialTokens.containsKey(key)) {
					 potentialTokens.remove(key);
				 }
			}
			
			/* cleanup nonwords */
			final String nonword = nonWordChars.toString();
			if (storedInitials.contains(nonword)) {
				resetNonWordChars(0);
				storedInitials = null;			// !!! only reset after executing setTermLength()
			}
		}
	}

	private void ifEndOfInputReachedEmptyInitials() {
		if (bufferIndex + 1 == dataLen) {
			initials = null;
		}
	}

	private void setTermLength() {					// goes together with finalizeSettingTermAttribute().
		termAtt.setLength(tokenEnd - tokenStart);
		if (storedInitials != null && storedInitials.contains(termAtt.toString())) {
			termAtt.setEmpty();
		}
	}

	private void finalizeSettingTermAttribute() {
		finalOffset = correctOffset(tokenEnd);
		offsetAtt.setOffset(correctOffset(tokenStart), finalOffset);
		/* the token string is already set through tokenBuffer(only a view into TermAttribute) */
	}

	private void changeTypeOfNonwords(Entry<String, Integer[]> token) {
		if (token.getValue()[3] == 0) {  
			typeAtt.setType("non-word");	// default type value: "word"
		}
	}

	private void fillTermAttributeWith(Entry<String, Integer[]> token) {
		termAtt.setEmpty().append(token.getKey());								// add the token string
		termAtt.setLength(token.getValue()[2]);									// declare its size
		finalOffset = correctOffset(token.getValue()[1]);						// get final offset 
		offsetAtt.setOffset(correctOffset(token.getValue()[0]), finalOffset);	// set its offsets (initial & final)
	}

	private Entry<String, Integer[]> takeFirstToken() {
		totalTokensIterator = totalTokens.entrySet().iterator();			// fill iterator
		return (Map.Entry<String, Integer[]>) totalTokensIterator.next();
	}

	private void ifThereAreInitialsFillIterator() {
		if (initials != null && !initials.isEmpty()) {
			initialsIterator = initials.iterator();		// one sandhi can yield many unsandhied initials
		}
	}

	private void ifConsumedAllInitialsResetInitialsAndIterator() {
		if (initials != null && initials.isEmpty()) {
			initials = null;
			initialsIterator = null;
		}
	}

	private void ifSandhiMergesStayOnSameCurrentChar() {
		if (charCount != -1 && mergesInitials) {
			if (bufferIndex < dataLen) {				// if end of input is reached
				bufferIndex -= charCount;
			}
			mergesInitials = false;						// reinitialize variable
		}
	}

	private boolean ifUnsandhyingFinalsYieldsLemmasAddThemToTotalTokens() {
		String cmd = scanner.getCommandVal(foundMatchCmdIndex);
		if (cmd != null) {
			final Set<String> lemmas = reconstructLemmas(cmd, termAtt.toString());
			if (lemmas.size() != 0) {
				for (String l: lemmas) {
					totalTokens.put(l, new Integer[] {tokenStart, tokenEnd, l.length(), 1});
					// use same indices for all (all are from the same inflected form)
				}
				return true;
			}
		}
		return false;
	}

	private void ifThereIsMatchAddItToTotalTokens(char[] tokenBuffer) {
		if (tokenLength > 0) {
			final String token = String.copyValueOf(tokenBuffer, 0, termAtt.length());
			totalTokens.put(token, new Integer[] {tokenStart, tokenEnd, token.length(), 1});
		}
	}

	private boolean ifThereIsNonwordAddItToTotalTokens() {
		final String nonWord = nonWordChars.toString();
		if (nonWord.length() > 0) {
			totalTokens.put(nonWord, new Integer[] {nonWordStart, nonWordEnd, nonWord.length(), 0});
			// ignore all potential tokens. add the non-word with sandhied initials
			return true;
		}
		return false;
	}

	private void unsandhiFinalsAndAddLemmatizedMatchesToTotalTokens() {
		for (Entry<String, Integer[]> entry: potentialTokens.entrySet()) {
			final String key = entry.getKey();
			final Integer[] value = entry.getValue();
			if (value[3] == 1) {
				String cmd = scanner.getCommandVal(value[4]);
				final Set<String> lemmas = reconstructLemmas(cmd, key);
				if (lemmas.size() != 0) {
					for (String l: lemmas) {	// multiple lemmas are possible: finals remain unanalyzed
						totalTokens.put(l, new Integer[] {value[0], value[1], value[2], value[3]});	
						// use same indices for all (all are from the same inflected form)
					}
				} else {	// finals of current form are not sandhied. there is only one token to add
					totalTokens.put(key, new Integer[] {value[0], value[1], value[2], value[3]});
				}
			}
		}
	}

	private void cutOffTokenFromNonWordChars() {
		nonWordChars.setLength(nonWordChars.length() - (tokenLength - charCount));
		nonWordEnd = tokenEnd - tokenLength;
		// end of non-word can be: a matching word starts (potentialEnd == true) OR a nonSLP char follows a nonWord.
	}

	private void IncrementTokenLengthAndAddCurrentCharTo(char[] tokenBuffer, int c) {
		tokenLength += Character.toChars(normalize(c), tokenBuffer, tokenLength);	// add normalized c to tokenBuffer
	}

	private void ifIsNeededAttributeStartingIndexOfNonword() {
		if (nonWordStart == -1) {							// the starting index of a non-word token does not increment
			nonWordStart = tokenStart;
		}
	}

	private void incrementTokenIndices() {
		tokenStart = offset + bufferIndex - charCount;
		tokenEnd = tokenStart + charCount;		// tokenEnd is one char ahead of tokenStart (ending index is exclusive)
	}

	private void tryToContinueDownTheTrie(Row row, int c) {
		int ref = row.getRef((char) c);
		currentRow = (ref >= 0) ? scanner.getRow(ref) : null;
	}

	private void tryToFindMatchIn(Row row, int c) {
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
		if (tokenLength > 0) {
			tokenLength = 0;				// reinitialize tokenBuffer through indices of tokenLength and tokenEnd
			tokenEnd = tokenStart + charCount;
		}
	}

	private void restorePreviousState() {		/* return to the beginning of the token in ioBuffer */
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
		final String potentialToken = nonWordChars.toString();
		potentialTokens.put(potentialToken,  new Integer[] {nonWordStart, nonWordEnd, potentialToken.length(), 0, -1});
	}

	private void addFoundTokenToPotentialTokensIfThereIsOne(char[] tokenBuffer) {
		if (tokenLength > 0) {																// avoid empty tokens
			final String potentialToken = String.copyValueOf(tokenBuffer, 0, tokenLength);
			potentialTokens.put(potentialToken,  new Integer[] {tokenStart, tokenEnd, potentialToken.length(), 1, 
																							foundMatchCmdIndex});
		}
	}

	private void initializeInitialCharsIteratorIfNeeded() {
		if (initialCharsIterator == null) {
			initialCharsIterator = new StringCharacterIterator(initialsIterator.next());	
			// initialize the iterator with the first initials
		} else if (initialCharsIterator.getIndex() == 0 && initialsIterator.hasNext()) {
		/* either first time or initialCharsIterator has been reset AND there are more initials to process */
			initialCharsIterator.setText(initialsIterator.next());
			// fill with new initials. happens if we reach the end of a token (either a Trie match or a non-word)
		}
		initialsIterator.remove();	// remove the initials just fed to the initialsCharsIterator
	}

	private int applyInitialChar() {
		int initial = initialCharsIterator.current();
		initialCharsIterator.setIndex(initialCharsIterator.getIndex()+1);	// increment iterator index
		return initial;														
		// charCount is not updated with new value of c since we only process SLP, so there are never surrogate pairs
	}

	private void storeCurrentState() {
		initialsOrigBufferIndex = bufferIndex - 1;
		initialsOrigTokenStart = tokenStart;
		initialsOrigTokenEnd = tokenEnd;
	}

	private void addExtraToken() {
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
	
	final private boolean isSLPTokenChar(int c) {
		return SkrtSylTokenizer.charType.get(c) != null && SkrtSylTokenizer.charType.get(c) != SkrtSylTokenizer.MODIFIER;
		// SLP modifiers are excluded because they are not considered to be part of a word/token. 
		// If a modifier occurs between two sandhied words, second word won't be considered sandhied
	}
	
	final private boolean isSLPModifier(int c) {
		return SkrtSylTokenizer.charType.get(c) != null && SkrtSylTokenizer.charType.get(c) == SkrtSylTokenizer.MODIFIER;
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
	
	final private boolean reachedNonwordCharacter() {	// we can't continue down the Trie, yet we don't have any match
		return currentRow == null && foundMatch == false;
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

	HashSet<String> reconstructLemmas(String cmd, String inflected) {
		/**
		 * Reconstructs all the possible sandhied strings for the first word using CmdParser.parse(),
		 * iterates through them, checking if the sandhied string is found in the sandhiable range,
		 * only reconstructs the lemmas if there is a match.
		 *
		 *
		 * @return: the list of all the possible lemmas given the current context
		 */
		HashSet<String> totalLemmas = new HashSet<String>();	// uses HashSet to avoid duplicates
		String[] t = new String[0];

		HashMap<String, HashSet<String>> parsedCmd = CmdParser.parse(inflected, cmd);
		for (Entry<String, HashSet<String>> current: parsedCmd.entrySet()) {
			String sandhied = current.getKey();
			HashSet<String> diffs = current.getValue();

			for (String lemmaDiff: diffs) {
				assert(lemmaDiff.contains("+"));		// all lemmaDiffs should contain +

				t = lemmaDiff.split("=");
				int sandhiType = Integer.parseInt(t[1]);
				if (sandhiType == 0) {
					continue;							// there is no sandhi, so we skip this diff
				}
				String diff = t[0];
				if (containsSandhiedCombination(ioBuffer.getBuffer(), bufferIndex - 1, sandhied, sandhiType)) {

					t = diff.split("\\+");

					if (diff.endsWith("+")) {			// ensures t has alway two elements
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

					if (t[1].contains("/")) {				// there is a change in initial
						t = t[1].split("/");
						toAdd = t[0];
						newInitial = t[1];
						// TODO: needs to be a possible first element of termAtt#buffer on next iteration of incrementToken()
						if (initials == null) {
							initials = new HashSet<String>();
							storedInitials = new HashSet<String>();
						}
						initials.add(newInitial);
						storedInitials.add(newInitial);
					} else {								// there no change in initial
						toAdd = t[1];
					}

					String lemma = inflected.substring(0, inflected.length()-toDelete)+toAdd;
					totalLemmas.add(lemma);
				}
			}
		}
		return totalLemmas;
	}

	static boolean containsSandhiedCombination(char[] buffer, int bufferIndex, String sandhied, int sandhiType) {
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
		case 0:																			// no sandhi
			return false;

		case 1:																			// vowel sandhi
			if (sandhied.length() == 1) {
				mergesInitials = true;
			}
			combinations = new int[][]{{0, 3}, {0, 2}, {0, 1}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 2:																			// consonant sandhi 1
			combinations = new int[][]{{0, 2}, {0, 1}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 3:																		// consonant sandhi 1 vowels
			combinations = new int[][]{{-1, 2}, {-1, 3}, {-1, 4}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);
			
		case 4:																			// consonant sandhi 2
			combinations = new int[][]{{0, 4}, {0, 3}, {0, 2}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 5:																			// visarga sandhi
			combinations = new int[][]{{-1, 3}, {-1, 2}, {-1, 1}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 6:																			// absolute finals sandhi
			combinations = new int[][]{{0, 1}};		// (consonant clusters are always reduced to the first consonant)
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 7:																			// "cC"-words sandhi
			combinations = new int[][]{{0, 4}, {0, 3}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 8:																			// special sandhi: "punar"
			combinations = new int[][]{{-4, 3}, {-4, 2}};
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);

		case 9:
			combinations = new int[][]{{0, 1}, {0, 2}, {0, 3}}; // TODO check if it is ok
			return isSandhiedCombination(buffer, bufferIndex, sandhied, combinations);
			
		default:
			return false;
		}
	}

	static boolean isSandhiedCombination(char[] inputBuffer, int bufferIndex, String sandhied, int[][] combinations) {
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
		offsetAtt.setOffset(finalOffset, finalOffset);	// set final offset
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		bufferIndex = 0;
		offset = 0;
		dataLen = 0;
		finalOffset = 0;
		ioBuffer.reset();		// make sure to reset the IO buffer!!

		finalsIndex = -1;
		hasTokenToEmit = false;	// for emitting multiple tokens

	}
}