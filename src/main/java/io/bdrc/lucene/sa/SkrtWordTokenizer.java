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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.RollingCharBuffer;

import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

/**
 * A maximal-matching word tokenizer for Sanskrit that uses a {@link Trie}.
 *
 * <p>
 * 	The expected input is an SLP string<br>
 *	{@link SkrtSyllableTokenizer#isSLP(int)} is used to filter out nonSLP characters.
 *
 * <p>
 * The necessary information for unsandhying finals and initials is taken from
 * {@link resources/sanskrit-stemming-data/output/total_output.txt} 
 * 
 * <br>
 * Due to its design, this tokenizer doesn't deal with contextual ambiguities.<br>
 * For example, "nagaraM" could either be a word of its own or "na" + "garaM",
 * but is always parsed as a single word
 *
 * Derived from Lucene 6.4.1 CharTokenizer, but differs by using a RollingCharBuffer
 * to still find tokens that are on the IO_BUFFER_SIZE (4096 chars)
 *
 * @author Élie Roux
 * @author Drupchen
 *
 */
public final class SkrtWordTokenizer extends Tokenizer {
	
	private Trie scanner;
	private boolean debug = false;
	String compiledTrieName = "src/main/resources/skrt-compiled-trie.dump"; 

	/* attributes allowing to modify the values of the generated terms */
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	/**
	 * Default constructor: uses the default compiled Trie, builds it if it is missing.
	 * 
	 * @throws FileNotFoundException the file containing the Trie can't be found
	 * @throws IOException the file containing the Trie can't be read
	 * 
	 */
	public SkrtWordTokenizer() throws FileNotFoundException, IOException {
	    InputStream stream = null;
	    stream = SkrtWordTokenizer.class.getResourceAsStream("/skrt-compiled-trie.dump");
	    if (stream == null) {  // we're not using the jar, there is no resource, assuming we're running the code
	        if (!new File(compiledTrieName).exists()) {
	            System.out.println("The default compiled Trie is not found ; building it will take some time!");
	            long start = System.currentTimeMillis();
	            BuildCompiledTrie.compileTrie();
	            long end = System.currentTimeMillis();
	            System.out.println("Trie built in " + (end - start) / 1000 + "s.");
	        }
	        init(new FileInputStream(compiledTrieName));    
	    } else {
	        init(stream);
	    }
	}
	
	/**
	 * Builds the Trie using the the given file
	 * @param filename the file containing the entries of the Trie
	 */
	public SkrtWordTokenizer(String filename) throws FileNotFoundException, IOException {
		init(filename);
	}

	/**
	 * Opens an already compiled Trie
	 * @param trieStream an InputStream (FileInputStream, for ex.) containing the compiled Trie
	 */
	public SkrtWordTokenizer(InputStream trieStream) throws FileNotFoundException, IOException {
		init(trieStream);
	}
	
	/**
	 * Uses the given Trie
	 * @param trie a Trie built using {@link BuildCompiledTrie}
	 */
	public SkrtWordTokenizer(Trie trie) {
		init(trie);
	}
	
	public SkrtWordTokenizer(boolean debug) throws FileNotFoundException, IOException {
		if (!new File(compiledTrieName).exists()) {
			System.out.println("The default compiled Trie is not found ; building it will take some time!");
			long start = System.currentTimeMillis();
			BuildCompiledTrie.compileTrie();
			long end = System.currentTimeMillis();
			System.out.println("Trie built in " + (end - start) / 1000 + "s.");
		}
		init(new FileInputStream(compiledTrieName));
		this.debug = debug;
	}
	
	/**
	 * Builds the Trie using the the given file. Prints debug info
	 * @param debug
	 * @param filename the file containing the entries of the Trie
	 */
	public SkrtWordTokenizer(boolean debug, String filename) throws FileNotFoundException, IOException {
		init(filename);
		this.debug = debug;
	}

	/**
	 * Opens an already compiled Trie. Prints debug info
	 * @param debug
	 * @param trieStream  an InputStream (FileInputStream, for ex.) containing the compiled Trie
	 */
	public SkrtWordTokenizer(boolean debug, InputStream trieStream) throws FileNotFoundException, IOException {
		init(trieStream);
		this.debug = debug;
	}
	
	/**
	 * Uses the given Trie. Prints debug info
	 * @param debug
	 * @param trie a Trie built using {@link BuildCompiledTrie}
	 */
	public SkrtWordTokenizer(boolean debug, Trie trie) {
		init(trie);
		this.debug = debug;
	}
	
	/**
	 * Opens an existing compiled Trie
	 * 
	 * @param inputStream the compiled Trie opened as a Stream 
	 */
	private void init(InputStream inputStream) throws FileNotFoundException, IOException {
		DataInputStream inStream = new DataInputStream(inputStream);
		this.scanner = new Trie(inStream);

		ioBuffer = new RollingCharBuffer();
		ioBuffer.reset(input);
	}
	
	/**
	 * Builds a Trie from the given file
	 * 
	 * @param filename the Trie as a {@code .txt} file
	 */
	private void init(String filename) throws FileNotFoundException, IOException {
		this.scanner = BuildCompiledTrie.buildTrie(Arrays.asList(filename));
		
		ioBuffer = new RollingCharBuffer();
		ioBuffer.reset(input);
	}
	
	/**
	 * Uses the given Trie
	 * @param trie  a Trie built using {@link BuildCompiledTrie}
	 */
	private void init(Trie trie) {
		this.scanner = trie;
		
		ioBuffer = new RollingCharBuffer();
		ioBuffer.reset(input);
	}
	
	/* current token related */
	private int tokenStart, tokenEnd, tokenLength;
	private Row rootRow, currentRow;
	private int cmdIndex, foundMatchCmdIndex;
	private boolean foundMatch;
	
	/* nonMaxMatch related */
	private boolean foundNonMaxMatch, wentToMaxDownTheTrie;;
	private int nonMaxTokenStart, nonMaxTokenEnd, nonMaxTokenLength, nonMaxBufferIndex, nonMaxNonWordLength;
	
	/* tokens related */
	private LinkedHashMap<String, Integer[]> potentialTokens = new LinkedHashMap<String, Integer[]>();	
	// contains : {startingIndex, endingIndex, tokenLength, (isItAMatchInTheTrie ? 1 : 0), 
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
	private int firstInitialIndex;
	private boolean applyOtherInitial;
	
	/* ioBuffer related (contains the input string) */
	private RollingCharBuffer ioBuffer;
	private int bufferIndex = 0, finalOffset = 0;
	private int charCount;
	int MAX_WORD_LEN = 255;
    
	

	
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

		if (bufferIndex - 4 >= 0) ioBuffer.freeBefore(bufferIndex - 4);
		tokenStart = -1;
		tokenEnd = -1;	
		tokenLength = 0;
		rootRow = scanner.getRow(scanner.getRoot());
		currentRow = null;
		cmdIndex = -1;
		foundMatchCmdIndex = -1;
		foundMatch = false;

		foundNonMaxMatch = false;
		wentToMaxDownTheTrie = false;
		nonMaxTokenStart = -1;		
		nonMaxTokenEnd = -1;
		nonMaxTokenLength = -1;
		nonMaxBufferIndex = -1;
		nonMaxNonWordLength = 0;
		firstInitialIndex = -1;
		applyOtherInitial = false;

		nonWordStart = -1;
		nonWordEnd = -1;
		resetNonWordChars(0);
		
		charCount = -1;
		
		char[] tokenBuffer = termAtt.buffer();
		boolean potentialTokensContainMatches = false;
		
		if (debug) System.out.println("----------------------");

		/* A. FINDING TOKENS */
		while (true) {
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
 
			int c = ioBuffer.get(bufferIndex);	// take next char in ioBuffer
			charCount = Character.charCount(c);
			bufferIndex += charCount; 			// increment bufferIndex for next value of c
			
			if (debug) System.out.print((char) c);
			
			/* when ioBuffer is empty (end of input, ...) */
			if (c == -1) {
				bufferIndex -= charCount;
				if (tokenLength == 0 && nonWordChars.length() == 0) {
					finalOffset = correctOffset(bufferIndex);
					return false;
				}
				break;
			}
			
			if (thereAreInitialsToConsume()) {
 				if (currentCharIsSpaceWithinSandhi(c)) {
					continue;		// if there is a space in the sandhied substring, moves beyond the space				
 				} else if (initialIsNotFollowedBySandhied(c)) {
 					initials = null;
 					initialCharsIterator = null;
 					ifNoInitialsCleanupPotentialTokensAndNonwords();
 					setTermLength();
 					continue; 
 				} else if (startConsumingInitials()) {	
 				/* we enter here on finalOffset ==  first initials. (when all initials are consumed, initials == []) */
					storeCurrentState();
					initializeInitialCharsIteratorIfNeeded();
					firstInitialIndex = bufferIndex;
					c = applyInitialChar();
					if (debug) System.out.print("=>" + (char) c);

				} else if (stillConsumingInitials() || applyOtherInitial) {
				/* we enter here if all initial chars are not yet consumed */
					initializeInitialCharsIteratorIfNeeded();
					c = applyInitialChar();
					if (debug) System.out.print("=>" + (char) c);
					applyOtherInitial = false;
				}
			}
			
			if (debug) System.out.println("");

			/* A.2. PROCESSING c */
			
			/* A.2.1) if it's a token char */
			if (isSLPTokenChar(c)) {
				
				/* Go one step down the Trie */
				if (isStartOfTokenOrIsNonwordChar(c)) {
				/* we enter on two occasions: at the actual start of a token and at each new non-word character. */
					tryToFindMatchIn(rootRow, c);					// if foundMatch == true, there is a match  
					tryToContinueDownTheTrie(rootRow, c);			// if currentRow != null, can continue
					incrementTokenIndices();
					ifIsNeededInitializeStartingIndexOfNonword();

				} else {
				/* we enter here on all other occasions: we don't know if word chars will be a match or not */
					
					ifNeededResize(tokenBuffer);
					tokenEnd += charCount;							// incrementing to correspond to the index of c					
					tryToFindMatchIn(currentRow, c);
					tryToContinueDownTheTrie(currentRow, c);
					if (reachedNonwordCharacter()) {
						wentToMaxDownTheTrie = true;
						tryToFindMatchIn(rootRow, c);
						tryToContinueDownTheTrie(rootRow, c);
						ifNeededReinitializeTokenBuffer();			// because no word ever started in the first place
					}
				}
				
				/* Decide what to do with the SLP chars currently processed */
				if (wentBeyondLongestMatch()) {
					if (foundNonMaxMatch) {
						restoreNonMaxMatchState();
					}
					
					ifNoInitialsCleanupPotentialTokensAndNonwords();
					
					
					if (thereIsNoTokenAndNoNonword()) {
						foundNonMaxMatch = false;
						continue;							// resume looping over ioBuffer
					} else if (isLoneInitial()) {
					    foundNonMaxMatch = false;
					    foundMatch = false;
					    setTermLength();
					    continue;
					} else {
						break;								// and resume looping over ioBuffer
					}
				
				} else if (thereAreRemainingInitialsToTest()) {
				    restorePreviousState();
				    resetNonWordChars(0);
				    wentToMaxDownTheTrie = false;
                    applyOtherInitial = true;
                    continue;
				    
				} else if (reachedNonwordCharacter()) {
					nonWordChars.append((char) c);
					nonWordEnd = tokenEnd;					// needed for term indices
					ifNeededReinitializeTokenBuffer();		// because no word ever started in the first place

				} else if (foundAToken()) {
					IncrementTokenLengthAndAddCurrentCharTo(tokenBuffer, c);
					cutOffTokenFromNonWordChars();
					
					if (allCharsFromCurrentInitialAreConsumed()) {
						potentialTokensContainMatches = true;					// because we reachedEndOfToken
						addFoundTokenToPotentialTokensIfThereIsOne(tokenBuffer);
						if (allInitialsAreConsumed()) {
							ifNoInitialsCleanupPotentialTokensAndNonwords();
							setTermLength();											// same as above
							if (thereIsNoTokenAndNoNonword()) {
								continue;							// resume looping over ioBuffer
							} else {
								setTermLength();
								break;								// and resume looping over ioBuffer
							}
						}
						resetInitialCharsIterator();
						restorePreviousState();
					} else {
						ifNoInitialsCleanupPotentialTokensAndNonwords(); 
						setTermLength();											// same as above
						break;
					}
				} else {													// we are within a potential token
					IncrementTokenLengthAndAddCurrentCharTo(tokenBuffer, c);
					nonWordChars.append((char) c);							// later remove chars belonging to a token
					nonWordEnd = tokenEnd;								// needed for term indices
					
					if (reachedEndOfInputString()) {
						ifNeededReinitializeTokenBuffer();


						if (allCharsFromCurrentInitialAreConsumed()) {
	                        addNonwordToPotentialTokens();                  // we do have a non-word token
	                        if (allInitialsAreConsumed()) {
	                            ifNoInitialsCleanupPotentialTokensAndNonwords(); 
	                            setTermLength();
	                            break;
	                        }
						} else {
							if (foundNonMaxMatch) {
								restoreNonMaxMatchState();
								ifNoInitialsCleanupPotentialTokensAndNonwords();
								break;
							} else {
								setTermLength();								// same as above
								ifNoInitialsCleanupPotentialTokensAndNonwords();
								break;
							}
						}					
                    }
				}
				
				/* tokenBuffer corner case: buffer overflow! */
				if (tokenLength >= MAX_WORD_LEN) {		// make sure to check for >= surrogate pair could break == test
					break;
				}
			
			/* A.2.2) if it is not a token char */
			} else if (foundNonMaxMatch) {
			    restoreNonMaxMatchState();
                if (matchIsLoneInitial()) {
                    ifNeededReinitializeTokenBuffer();
                    setTermLength();
                    foundNonMaxMatch = false;
                    continue;
                }
				nonWordEnd = tokenEnd;				// needed for term indices
				ifNoInitialsCleanupPotentialTokensAndNonwords();
				break;
				
			} else if (isNonSLPprecededByNonword()) {			// we have a nonword token
				nonWordEnd = tokenEnd;							// needed for term indices
				if (allCharsFromCurrentInitialAreConsumed()) {
				    if (nonwordIsLoneInitial()) {
                        ifNeededReinitializeTokenBuffer();
                        setTermLength();
                        continue;
				    }
					addNonwordToPotentialTokens();
					if (allInitialsAreConsumed()) {
						ifNoInitialsCleanupPotentialTokensAndNonwords();
						setTermLength();											// same as above
						break;
					}
					resetNonWordChars(0);
					resetInitialCharsIterator();
					restorePreviousState();					
				} else {
					ifNoInitialsCleanupPotentialTokensAndNonwords(); 
					if (isSLPModifier(c)) {
						continue;								// move on and do as if the modifier didn't exist
					} else {
						setTermLength();											// same as above
						break;
					}
				}
			} else if (isNonSLPprecededByNotEmptyNonWord()) {
				nonWordEnd = tokenEnd;							// needed for term indices

				if (allCharsFromCurrentInitialAreConsumed()) {
					addFoundTokenToPotentialTokensIfThereIsOne(tokenBuffer);
					if (allInitialsAreConsumed()) {
						ifNoInitialsCleanupPotentialTokensAndNonwords();  
						if (thereIsNoTokenAndNoNonword()) {
							continue;							// resume looping over ioBuffer
						} else {
							setTermLength();
							break;								// and resume looping over ioBuffer
						}
					}
					resetNonWordChars(0);
					resetInitialCharsIterator();
					restorePreviousState();					
				} else {
					ifNoInitialsCleanupPotentialTokensAndNonwords();
					setTermLength();
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
			boolean lemmasWereAdded = ifUnsandhyingFinalsYieldsLemmasAddThemToTotalTokens();
			
			if (lemmasWereAdded) {
				ifSandhiMergesStayOnSameCurrentChar();	// so we can unsandhi the initial and find the start of next word
				tokenEnd -= charCount;					
				
				finalsIndex = bufferIndex;				// save index of finals for currentCharIsSpaceWithinSandhi()
			} else if (aNonwordWasAdded) {              // if a non-word was added, there was a match but no sandhi
                ifThereIsMatchAddItToTotalTokens(tokenBuffer);
            }
			
		}
		
		/* B.2. EXITING incrementToken() WITH THE TOKEN (OR THE FIRST ONE FROM totalTokens) */
		ifThereAreInitialsFillIterator();
		ifEndOfInputReachedEmptyInitials();

		if (thereAreTokensToReturn()) {
			hasTokenToEmit = true;
			final Map.Entry<String, Integer[]> firstToken = takeFirstToken();
			fillTermAttributeWith(firstToken);
			changeTypeOfNonwords(firstToken);
			return true;						// we exit incrementToken()
		
		} else {					// there is no non-word nor extra lemma to add. there was no sandhi for this token 			
			assert(tokenStart != -1);
			finalizeSettingTermAttribute();
			return true;						// we exit incrementToken()
		}
	}

	private void ifNeededResize(char[] tokenBuffer) {
		if (tokenLength >= tokenBuffer.length-1) {			// check if a supplementary could run out of bounds
			tokenBuffer = termAtt.resizeBuffer(2+tokenLength);	// make sure a supplementary fits in the buffer
		}
	}
	
	/**
	 * Reconstructs all the possible sandhied strings for the first word using CmdParser.parse(),
	 * iterates through them, checking if the sandhied string is found in the sandhiable range,
	 * only reconstructs the lemmas if there is a match.
	 *
	 *
	 * @return: the list of all the possible lemmas given the current context
	 */
	HashSet<String> reconstructLemmas(String cmd, String inflected) throws NumberFormatException, IOException {
		HashSet<String> totalLemmas = new HashSet<String>();	// uses HashSet to avoid duplicates
		String[] t = new String[0];

		TreeMap<String, HashSet<String>> parsedCmd = new CmdParser().parse(inflected, cmd);
		for (Entry<String, HashSet<String>> current: parsedCmd.entrySet()) {
			String sandhied = current.getKey();
			HashSet<String> diffs = current.getValue();
			boolean foundAsandhi = false; 
			for (String lemmaDiff: diffs) {
				assert(lemmaDiff.contains("+"));		// all lemmaDiffs should contain +

				t = lemmaDiff.split("=");
				int sandhiType = Integer.parseInt(t[1]);
				if (noSandhiButLemmatizationRequired(sandhiType, t[0])) {
					continue;							// there is no sandhi nor, so we skip this diff
				}
				String diff = t[0];
				if (containsSandhiedCombination(ioBuffer, bufferIndex - 1, sandhied, sandhiType)) {
				    foundAsandhi = true;
					t = diff.split("\\+");

					if (diff.endsWith("+")) {			// ensures t has alway two elements
						t = new String[2];
						t[0] = diff.split("\\+")[0];
						t[1] = "";
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
			if (foundAsandhi) break;
		}
		return totalLemmas;
	}

	/**
	 * Tells whether sandhied could be found between the two words.
	 * Does it by generating all the legal combinations, filtering spaces and checking for equality.
	 * <p>
	 * See SandhiedCombinationTests for how these figures were obtained
	 *
	 * @param ioBuffer: is given as parameter for the tests
	 * @return: true if sandhied is one of the combinations; false otherwise
	 */
	static boolean containsSandhiedCombination(RollingCharBuffer ioBuffer, int bufferIndex, String sandhied, int sandhiType) throws IOException {
		switch(sandhiType) {
		
		case 0:
		    return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0);    // no sandhi, but lemmatization required
		
		case 1:																			
			if (isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0)) {     // vowel sandhi
	            if (sandhied.length() == 1) {
	                mergesInitials = true;
	            }
	            return true;
			} else {
			    return false;
			}

		case 2:
			return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0);    // consonant sandhi 1

		case 3:
			return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, -1);   // consonant sandhi 1 vowels
			
		case 4:
			return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0);    // consonant sandhi 2

		case 5:
			return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, -1);   // visarga sandhi

		case 6:
			return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, -1);   // visarga sandhi 2
			
		case 7:
			// (consonant clusters are always reduced to the first consonant)
			return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0);    // absolute finals sandhi

		case 8:
			return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0);    // "cC"-words sandhi

		case 9:
			return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, -4);   // special sandhi: "punar"
			
		default:
			return false;
		}
	}

	static boolean isSandhiedCombination(RollingCharBuffer ioBuffer, int bufferIndex, String sandhied, int start) throws IOException {
        int j = 0;
        int nbIgnoredSpaces = 0;
        while (j < sandhied.length()) {
            final int res = ioBuffer.get(bufferIndex+start+j+nbIgnoredSpaces);
            if (isValidCharWithinSandhi(res)) { // 
                nbIgnoredSpaces++;
                continue;
            }
            if (res == -1)
                return false;
            if (res != sandhied.codePointAt(j))
                return false;
            j++;
        }
		return true;
	}
	
	private void ifNoInitialsCleanupPotentialTokensAndNonwords() {
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

	private void ifEndOfInputReachedEmptyInitials() throws IOException {
		if (ioBuffer.get(bufferIndex) == -1) {
			initials = null;
		}
	}

	private void setTermLength() {					// goes together with finalizeSettingTermAttribute().
		termAtt.setLength(tokenEnd - tokenStart);
		tokenLength = (tokenEnd - tokenStart - 1 >= 0) ? tokenEnd - tokenStart - 1: 0;
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

	private void ifSandhiMergesStayOnSameCurrentChar() throws IOException {
		if (charCount != -1 && mergesInitials) {
			if (ioBuffer.get(bufferIndex) != -1) {	// if end of input is not reached
				bufferIndex -= charCount;
			}
			mergesInitials = false;						// reinitialize variable
		}
	}

	private boolean ifUnsandhyingFinalsYieldsLemmasAddThemToTotalTokens() throws NumberFormatException, IOException {
		String cmd = scanner.getCommandVal(foundMatchCmdIndex);
		if (cmd != null) {
			if (debug) System.out.println("form found: " + termAtt.toString() + "\n");
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

	private void unsandhiFinalsAndAddLemmatizedMatchesToTotalTokens() throws NumberFormatException, IOException {
		for (Entry<String, Integer[]> entry: potentialTokens.entrySet()) {
			final String key = entry.getKey();
			final Integer[] value = entry.getValue();
			if (debug) System.out.println("form found: " + termAtt.toString() + "\n");
			if (value[3] == 1) {
				String cmd = scanner.getCommandVal(value[4]);
				final Set<String> lemmas = reconstructLemmas(cmd, key);
				if (lemmas.size() != 0) {
					for (String l: lemmas) {	// multiple lemmas are possible: finals remain unanalyzed
						totalTokens.put(l, new Integer[] {value[0], value[1], l.length(), value[3]});	
						// use same indices for all (all are from the same inflected form)
					}
				} else {	// finals of current form are not sandhied. there is only one token to add
					totalTokens.put(key, new Integer[] {value[0], value[1], value[2], value[3]});
				}
			}
		}
	}

	private void cutOffTokenFromNonWordChars() {
		int newSize = nonWordChars.length() - (tokenLength - charCount);
		newSize = newSize < 0 ? 0: newSize;   // ensure the new size is never negative
	    nonWordChars.setLength(newSize);
		nonWordEnd = tokenEnd - tokenLength;
		// end of non-word can be: a matching word starts (potentialEnd == true) OR a nonSLP char follows a nonWord.
	}

	private void IncrementTokenLengthAndAddCurrentCharTo(char[] tokenBuffer, int c) {
		tokenLength += Character.toChars(normalize(c), tokenBuffer, tokenLength);	// add normalized c to tokenBuffer
		if (nonMaxIndicesRequireUpdating()) {
		    nonMaxTokenLength = tokenLength;
		}
		termAtt.setLength(tokenLength);   // TEST
	}

	private void ifIsNeededInitializeStartingIndexOfNonword() {
		if (nonWordStart == -1) {							// the starting index of a non-word token does not increment
			nonWordStart = tokenStart;
		}
	}

	private void incrementTokenIndices() {
		tokenStart = bufferIndex - charCount;
		tokenEnd = tokenStart + charCount;		// tokenEnd is one char ahead of tokenStart (ending index is exclusive)
		if (nonMaxIndicesRequireUpdating()) {
		    if (tokenStart != nonMaxTokenStart) {
		        nonMaxTokenStart = tokenStart;
		    }
		    if (tokenEnd != nonMaxTokenEnd) {
		        nonMaxTokenEnd = tokenEnd;
		    }
		}
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
		nonMaxNonWordLength = nonWordChars.length() - tokenLength;
		if (nonMaxNonWordLength < 0) {
		    nonMaxNonWordLength = 0;
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
		termAtt.setLength(tokenLength);
	}

	private void ifNeededReinitializeTokenBuffer() {	
		if (tokenLength > 0) {
			tokenLength = 0;				// reinitialize tokenBuffer through indices of tokenLength and tokenEnd
			termAtt.setLength(tokenLength);
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
			initialsIterator.remove();    // remove the initials just fed to the initialsCharsIterator
//		} else if (initialCharsIterator.getIndex() == 0 && initialsIterator.hasNext()) {
		} else if (initialsIterator.hasNext()) {
		/* either first time or initialCharsIterator has been reset AND there are more initials to process */
			initialCharsIterator.setText(initialsIterator.next());
			// fill with new initials. happens if we reach the end of a token (either a Trie match or a non-word)
			initialsIterator.remove();    // remove the initials just fed to the initialsCharsIterator
		}	
	}

	private int applyInitialChar() {
		int initial = initialCharsIterator.current();
		if (initialCharsIterator.getIndex() == initialCharsIterator.getEndIndex()) {
		    initialCharsIterator.setIndex(0);
		} else {
		    initialCharsIterator.setIndex(initialCharsIterator.getIndex()+1); // increment iterator index
		}
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
			changeTypeOfNonwords(extra);
			termAtt.setLength(extra.getValue()[2]);
			finalOffset = correctOffset(extra.getValue()[1]);
			offsetAtt.setOffset(correctOffset(extra.getValue()[0]), finalOffset);
		} else {
			hasTokenToEmit = false;
		}
	}
	
	final private boolean isLoneInitial() {
	    boolean isInitial = false;
	    if (storedInitials != null) {
	        String tokenStr = termAtt.toString();
	        for (String initial: storedInitials) {
	            if (tokenStr.equals(initial)) {
	                isInitial = true;
	            }
	        }
	    }
	    return isInitial;
	}
	
	final private boolean thereAreRemainingInitialsToTest() {
	    return initials != null && !initials.isEmpty() && wentToMaxDownTheTrie && foundMatch == false && initials.size() <= storedInitials.size() - 1;
	}
	
	final private boolean noSandhiButLemmatizationRequired(int sandhiType, String diff) {
	    return sandhiType == 0 && diff.equals("/");
	}
	
	final private boolean nonwordIsLoneInitial() {
	    return storedInitials != null && storedInitials.contains(nonWordChars.toString());
	}
	
	final private boolean matchIsLoneInitial() {
	    return storedInitials != null && storedInitials.contains(termAtt.toString());
	}
	
	final private boolean nonMaxIndicesRequireUpdating() {
	    return bufferIndex == nonMaxBufferIndex;
	}
	
	final private boolean isSLPTokenChar(int c) {
		return SkrtSyllableTokenizer.charType.get(c) != null && SkrtSyllableTokenizer.charType.get(c) != SkrtSyllableTokenizer.MODIFIER;
		// SLP modifiers are excluded because they are not considered to be part of a word/token. 
		// If a modifier occurs between two sandhied words, second word won't be considered sandhied
	}
	
	final private boolean currentCharIsSpaceWithinSandhi(int c) {
		return finalsIndex + 1 == bufferIndex && isValidCharWithinSandhi(c);
	}
	
	final private static boolean isValidCharWithinSandhi(int c) {
	    return c == ' ' || c == '-';
	}
	
	final private boolean isSLPModifier(int c) {
		return SkrtSyllableTokenizer.charType.get(c) != null && SkrtSyllableTokenizer.charType.get(c) == SkrtSyllableTokenizer.MODIFIER;
	}
	
	final private boolean thereIsNoTokenAndNoNonword() {
		return tokenLength == 0 && nonWordChars.length() == 0;
	}
	
	final private boolean wentBeyondLongestMatch() {
		return foundNonMaxMatch && wentToMaxDownTheTrie && foundMatch == false;
//		return wentToMaxDownTheTrie && foundMatch == false;
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

	final private boolean reachedEndOfInputString() throws IOException {
		return ioBuffer.get(bufferIndex) == -1;
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

	final private boolean initialIsNotFollowedBySandhied(int c) {
		return isValidCharWithinSandhi(c) && bufferIndex == firstInitialIndex + 1 ;
	}

	final private boolean allInitialsAreConsumed() {
		return !initialsIterator.hasNext();
	}

	final private void resetInitialCharsIterator() {
		initialCharsIterator.setIndex(0);
	}

	final private boolean thereAreInitialsToConsume() throws IOException {
		return initials != null && !initials.isEmpty() && ioBuffer.get(bufferIndex) != -1;
	}

	final private boolean foundAToken() throws IOException {
		return currentRow == null && foundMatch == true  || (foundMatch == true && reachedEndOfInputString());
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
		finalOffset = 0;
		ioBuffer.reset(input);		// make sure to reset the IO buffer!!

		finalsIndex = -1;
		hasTokenToEmit = false;	// for emitting multiple tokens

	}
}