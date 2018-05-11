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
import java.util.LinkedList;
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
 * {@code resources/sanskrit-stemming-data/output/total_output.txt} 
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
	            System.out.println("Trie built and stored in " + (end - start) / 1000 + "s.");
	        }
	        init(new FileInputStream(compiledTrieName));    
	    } else {
	        init(stream);
	    }
	}
	
	/**
	 * Builds the Trie using the the given file
	 * @param filename the file containing the entries of the Trie
     * @throws FileNotFoundException the file containing the Trie can't be found
     * @throws IOException the file containing the Trie can't be read
	 */
	public SkrtWordTokenizer(String filename) throws FileNotFoundException, IOException {
		init(filename);
	}

	/**
	 * Opens an already compiled Trie
	 * @param trieStream an InputStream (FileInputStream, for ex.) containing the compiled Trie
     * @throws FileNotFoundException the file containing the Trie can't be found
     * @throws IOException the file containing the Trie can't be read
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
			System.out.println("Default compiled Trie is not found\nCompiling it and writing it to file…");
			long start = System.currentTimeMillis();
			BuildCompiledTrie.compileTrie();
			long end = System.currentTimeMillis();
			System.out.println("Time: " + (end - start) / 1000 + "s.");
		}
		init(new FileInputStream(compiledTrieName));
		this.debug = debug;
	}
	
	/**
	 * Builds the Trie using the the given file. Prints debug info
	 * @param debug print debug information
	 * @param filename the file containing the entries of the Trie
     * @throws FileNotFoundException the file containing the Trie can't be found
     * @throws IOException the file containing the Trie can't be read
	 */
	public SkrtWordTokenizer(boolean debug, String filename) throws FileNotFoundException, IOException {
		init(filename);
		this.debug = debug;
	}

	/**
	 * Opens an already compiled Trie. Prints debug info
	 * @param debug print debuging information if true
	 * @param trieStream  an InputStream (FileInputStream, for ex.) containing the compiled Trie
     * @throws FileNotFoundException the file containing the Trie can't be found
     * @throws IOException the file containing the Trie can't be read
	 */
	public SkrtWordTokenizer(boolean debug, InputStream trieStream) throws FileNotFoundException, IOException {
		init(trieStream);
		this.debug = debug;
	}
	
	/**
	 * Uses the given Trie. Prints debug info
	 * @param debug print debuging information if true
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
	 * @throws FileNotFoundException compiled trie not found
	 * @throws IOException compiled trie could not open
	 */
	private void init(InputStream inputStream) throws FileNotFoundException, IOException {
	    System.out.println("Loading the compiled Trie…");
	    long start = System.currentTimeMillis();
	    DataInputStream inStream = new DataInputStream(inputStream);
		this.scanner = new Trie(inStream);
		long end = System.currentTimeMillis();
		System.out.println("Time: " + (end - start) / 1000 + "s.");
		
		ioBuffer = new RollingCharBuffer();
		ioBuffer.reset(input);
	}
	
	/**
	 * Builds a Trie from the given file
	 * 
	 * @param filename the Trie as a {@code .txt} file
     * @throws FileNotFoundException trie not found
     * @throws IOException trie could not open
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
	private int tokenStart;
	private StringBuilder tokenBuffer = new StringBuilder();
	private Row rootRow, currentRow;
	private int cmdIndex, foundMatchCmdIndex;
	private boolean foundMatch;
	private boolean afterNonwordMatch;
	
	/* nonMaxMatch related */
	private boolean foundNonMaxMatch, wentToMaxDownTheTrie;
	private StringBuilder nonMaxBuffer = new StringBuilder();
	private int nonMaxTokenStart, nonMaxBufferIndex, nonMaxFoundMatchCmdIndex, nonMaxNonWordLength;
	
	/* tokens related */
	private LinkedHashMap<String, Integer[]> potentialTokens = new LinkedHashMap<String, Integer[]>();	
	// contains : {startingIndex, endingIndex, tokenLength, (isItAMatchInTheTrie ? 1 : 0), 
	//												(isItAMatchInTheTrie ? theIndexOfTheCmd : -1)}

	/* nonWords related */
	private int nonWordStart;
	private StringBuilder nonWordBuffer = new StringBuilder();
	
	/* totalTokens related */
	private LinkedList<PreToken> totalTokens = new LinkedList<PreToken>();
	private boolean hasTokenToEmit;
	
	/* initials related */
	private HashSet<String> initials = null;			// it is HashSet to filter duplicate initials
	private Iterator<String> initialsIterator = null;
	private StringCharacterIterator initialCharsIterator = null;
	private static int sandhiIndex;
	
	private int initialsOrigBufferIndex = -1, initialsOrigTokenStart = -1;
	private StringBuilder initialsOrigBuffer = new StringBuilder();
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
    
	/* previous state related*/
    private int storedNoMatchState, noMatchTokenStart, noMatchBufferIndex, noMatchFoundMatchCmdIndex;
    private StringBuilder noMatchBuffer = new StringBuilder();
	
	/**
	 * Called on each token character to normalize it before it is added to the
	 * token. The default implementation does nothing. Subclasses may use this to,
	 * e.g., lowercase tokens.
	 * @param c current character
	 * @return normalized c
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
		rootRow = scanner.getRow(scanner.getRoot());
		currentRow = null;
		cmdIndex = -1;
		foundMatchCmdIndex = -1;
		foundMatch = false;
		afterNonwordMatch = false;
		
		nonWordBuffer.setLength(0);
	    nonWordStart = -1;
		
		nonMaxBuffer.setLength(0);
		nonMaxTokenStart = -1;		
		nonMaxBufferIndex = -1;
		nonMaxFoundMatchCmdIndex = -1;
		nonMaxNonWordLength = -1;
		foundNonMaxMatch = false;
		wentToMaxDownTheTrie = false;
		firstInitialIndex = -1;
		applyOtherInitial = false;

		noMatchBuffer.setLength(0);
		noMatchTokenStart = -1;      
	    noMatchBufferIndex = -1;
	    storedNoMatchState = -1;
	    noMatchFoundMatchCmdIndex = -1;
		
	    initialsOrigBuffer.setLength(0);
	    initialsOrigTokenStart = -1;
	    initialsOrigBufferIndex = -1;
	    
		charCount = -1;
		
		tokenBuffer.setLength(0);
		boolean potentialTokensContainMatches = false;
		@SuppressWarnings("unused")   // these two variables are not used.
        boolean match = false;
		@SuppressWarnings("unused")   // they only provide humans an easy way to understand what is happening
        boolean continuing = false;
		@SuppressWarnings("unused")
		char currentChar;
		
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
			currentChar = (char) c;
			charCount = Character.charCount(c);
			bufferIndex += charCount; 			// increment bufferIndex for next value of c
			
			ifIsNeededInitializeStartingIndexOfNonword();
			
			if (debug) System.out.print((char) c);
			
			/* when ioBuffer is empty (end of input, ...) */
			if (c == -1) {
				bufferIndex -= charCount;
				if (tokenBuffer.length() == 0 && nonWordBuffer.length() == 0) {
					finalOffset = correctOffset(bufferIndex);
					return false;
				}
				break;
			}
			
			if (thereAreInitialsToConsume()) {
 				if (currentCharIsSpaceWithinSandhi(c)) {
 				    nonWordStart = -1;
 				    continue;		// if there is a space in the sandhied substring, moves beyond the space

 				} else if (initialIsNotFollowedBySandhied(c)) {
 					initials = null;
 					initialCharsIterator = null;
 					ifNoInitialsCleanupPotentialTokensAndNonwords();
 					nonWordStart = -1;
 					continue;
 					
 				} else if (startConsumingInitials()) {	
 				/* we enter here on finalOffset ==  first initials. (when all initials are consumed, initials == []) */
 		           if (sandhiIndex != -1) {
 		               c = ioBuffer.get(sandhiIndex);
 		              bufferIndex = sandhiIndex + charCount;
 		              if (debug) System.out.print("=>" + (char) c);
 		            }
 				    storeCurrentState();
					initializeInitialCharsIteratorIfNeeded();
					firstInitialIndex = bufferIndex;
					c = applyInitialChar();
					if (debug) System.out.print("=>" + (char) c);

				} else if (stillConsumingInitials() || applyOtherInitial) {
				/* we enter here if all initial chars are not yet consumed */
					initializeInitialCharsIteratorIfNeeded();
					c = applyInitialChar();
					if (nonWordBuffer.length() > 0) decrement(nonWordBuffer);
					if (debug) System.out.print("=>" + (char) c);
					applyOtherInitial = false;                 
				}
			}
			
			tokenBuffer.append((char) normalize(c));
			nonWordBuffer.append((char) c);          // later remove chars belonging to a token
			if (debug) System.out.println("");

			/* A.2. PROCESSING c */
			
			/* A.2.1) if it's a token char */
			if (isSLPTokenChar(c)) {
			    if (isSLPModifier(c)) {
			        decrement(tokenBuffer);
			        decrement(nonWordBuffer);
			        continue;
			    }
				
				/* Go one step down the Trie */
				if (isStartOfTokenOrIsNonwordChar()) {
				/* we enter on two occasions: at the actual start of a token and at each new non-word character. */
				    tokenStart = bufferIndex - charCount;                   // update for potential word starting here
				    
				    match = tryToFindMatchIn(rootRow, c);					// if foundMatch == true, there is a match  
					continuing = tryToContinueDownTheTrie(rootRow, c);	    // if currentRow != null, can continue
					incrementTokenIndices();
					ifIsNeededInitializeStartingIndexOfNonword();

				} else {
				/* we enter here on all other occasions: we don't know if word chars will be a match or not */
										
					match = tryToFindMatchIn(currentRow, c);
					continuing = tryToContinueDownTheTrie(currentRow, c);
					if (reachedNonwordCharacter()) {
					    if (!foundNonMaxMatch && storedNoMatchState == 1) {
					        restoreNoMatchState();
					        storedNoMatchState = 0;
					        continue;
					    } else if (!foundNonMaxMatch) {
					        match = tryToFindMatchIn(rootRow, c);
					        continuing = tryToContinueDownTheTrie(rootRow, c);
					        tokenBuffer.setLength(0);
					        tokenStart = bufferIndex;
					        if (foundMatch) {
					            afterNonwordMatch = true;
					        }
					    }
						wentToMaxDownTheTrie = true;
						storedNoMatchState = -1;
						if (tokenBuffer.length() == 0) {
						    tokenBuffer.append((char) c);
						} else {
						    tokenBuffer.setLength(0);         // because no word ever started in the first place
						}
						
					}
				}
				
				/* Decide what to do with the SLP chars currently processed */
				if (wentBeyondLongestMatch()) {
					if (foundNonMaxMatch) {
					    restoreNonMaxMatchState();
					}
					
					if (storedInitials != null && storedInitials.contains(nonWordBuffer.toString())) {
                        foundNonMaxMatch = false;
                        foundMatch = false;
                        foundMatchCmdIndex = -1;
                        tokenBuffer.setLength(0);
                        wentToMaxDownTheTrie = false;
                        resetNonWordBuffer(0);
                        nonWordStart = -1;
//                        ifNoInitialsCleanupPotentialTokensAndNonwords();
                        continue;
                    }
					
					ifNoInitialsCleanupPotentialTokensAndNonwords();
					
					if (thereIsNoTokenAndNoNonword()) {
						foundNonMaxMatch = false;
						continue;							// resume looping over ioBuffer
					} else if (isLoneInitial()) {
					    foundNonMaxMatch = false;
					    foundMatch = false;
					    foundMatchCmdIndex = -1;
					    continue;
					} else if (thereAreRemainingInitialsToTest()) {
					    potentialTokensContainMatches = addFoundTokenToPotentialTokensIfThereIsOne();
					    restoreInitialsOrigState();
	                    resetNonWordBuffer(0);
	                    wentToMaxDownTheTrie = false;
	                    applyOtherInitial = true;
	                    continue;
	                    
	                } else {
	                    cutOffTokenFromNonWordBuffer();
	                    potentialTokensContainMatches = addFoundTokenToPotentialTokensIfThereIsOne();
	                    addNonwordToPotentialTokensIfThereIsOne();

	                    break;
					}
				
				} else if (thereAreRemainingInitialsToTest()) {
				    restoreInitialsOrigState();
				    resetNonWordBuffer(0);
				    wentToMaxDownTheTrie = false;
                    applyOtherInitial = true;
                    continue;
				    
				} else if (reachedNonwordCharacter()) {
					tokenBuffer.setLength(0);		// because no word ever started in the first place

				} else if (foundAToken()) {
					if (!afterNonwordMatch) {
					    cutOffTokenFromNonWordBuffer();
					}
					if (isLoneInitial()) {
					    tokenBuffer.setLength(0);
					}
					
					if (allCharsFromCurrentInitialAreConsumed()) {
					    potentialTokensContainMatches = addFoundTokenToPotentialTokensIfThereIsOne();
						addNonwordToPotentialTokensIfThereIsOne();                  // we do have a non-word token
						if (allInitialsAreConsumed()) {
							ifNoInitialsCleanupPotentialTokensAndNonwords();								// same as above
							storedInitials = null;
							if (thereIsNoTokenAndNoNonword()) {
								continue;							// resume looping over ioBuffer
							} else {
								break;								// and resume looping over ioBuffer
							}
						}
						resetInitialCharsIterator();
						restoreInitialsOrigState();
						if (wentToMaxDownTheTrie && initialsNotEmpty()) {
						    foundNonMaxMatch = false;
						    resetNonWordBuffer(0);
						}
					} else {
						ifNoInitialsCleanupPotentialTokensAndNonwords(); 
                        potentialTokensContainMatches = addFoundTokenToPotentialTokensIfThereIsOne();
                        addNonwordToPotentialTokensIfThereIsOne();                  // we do have a non-word token
						break;
					}
				} else {													// we are within a potential token					
					if (reachedEndOfInputString()) {
					    tokenBuffer.setLength(0);


						if (allCharsFromCurrentInitialAreConsumed()) {
	                        addNonwordToPotentialTokensIfThereIsOne();                  // we do have a non-word token
	                        if (allInitialsAreConsumed()) {
	                            ifNoInitialsCleanupPotentialTokensAndNonwords();
	                            storedInitials = null;
	                            break;
	                        }
						} else {
							if (foundNonMaxMatch) {
								restoreNonMaxMatchState();
								cutOffTokenFromNonWordBuffer();
								ifNoInitialsCleanupPotentialTokensAndNonwords();
								break;
							} else {
								ifNoInitialsCleanupPotentialTokensAndNonwords();
								break;
							}
						}					
                    }
				}
				
				/* tokenBuffer corner case: buffer overflow! */
				if (tokenBuffer.length() >= MAX_WORD_LEN) {		// make sure to check for >= surrogate pair could break == test
					break;
				}
			
			/* A.2.2) if it is not a token char */
			} else if (foundNonMaxMatch) {
			    restoreNonMaxMatchState();
                if (matchIsLoneInitial()) {
                    tokenBuffer.setLength(0);
                    foundNonMaxMatch = false;
                    foundMatchCmdIndex = -1;
                    storedNoMatchState = -1;
                    decrement(nonWordBuffer);
                    nonWordStart = -1;
                 
                    if (allCharsFromCurrentInitialAreConsumed()) {
                        addNonwordToPotentialTokensIfThereIsOne();                  // we do have a non-word token
                        if (allInitialsAreConsumed()) {
                            ifNoInitialsCleanupPotentialTokensAndNonwords();
                            storedInitials = null;
                            if (thereIsNoTokenAndNoNonword()) {
                                continue;                           // resume looping over ioBuffer
                            } else {
                                break;                              // and resume looping over ioBuffer
                            }
                        } else {
                            resetNonWordBuffer(0);
                            resetInitialCharsIterator();
                            restoreInitialsOrigState();
                            wentToMaxDownTheTrie = false;
                            applyOtherInitial = true;
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
				ifNoInitialsCleanupPotentialTokensAndNonwords();
				if (nonWordBuffer.toString().equals(tokenBuffer.toString()) && nonWordStart != tokenStart) {
				    nonWordStart = tokenStart;
				}
                if (allCharsFromCurrentInitialAreConsumed()) {
                    addNonwordToPotentialTokensIfThereIsOne();
                    potentialTokensContainMatches = addFoundTokenToPotentialTokensIfThereIsOne();
                    if (allInitialsAreConsumed()) {
                        ifNoInitialsCleanupPotentialTokensAndNonwords();
                        storedInitials = null;
                        if (thereIsNoTokenAndNoNonword()) {
                            continue;                           // resume looping over ioBuffer
                        } else {
                            break;                              // and resume looping over ioBuffer
                        }
                    } else {
                        if (potentialTokensContainMatches && initials != null) {
                            sandhiIndex = bufferIndex;
                        }
                        resetNonWordBuffer(0);
                        resetInitialCharsIterator();
                        restoreInitialsOrigState();
                        wentToMaxDownTheTrie = false;
                        applyOtherInitial = true;
                        continue;
                    }
                } else {
                    break;
                }
				
			} else if (isNonSLPprecededBySLP()) {			// we have a nonword token
				if (allCharsFromCurrentInitialAreConsumed()) {
				    decrement(nonWordBuffer);
				    if (nonwordIsLoneInitial()) {
                        tokenBuffer.setLength(0);
                        foundMatchCmdIndex = -1;
                        continue;
				    }
					addNonwordToPotentialTokensIfThereIsOne();
					if (allInitialsAreConsumed()) {
						ifNoInitialsCleanupPotentialTokensAndNonwords();
						storedInitials = null;
						break;
					}
					resetNonWordBuffer(0);
					resetInitialCharsIterator();
					restoreInitialsOrigState();	
				} else {
					ifNoInitialsCleanupPotentialTokensAndNonwords(); 
					tokenBuffer.setLength(0);  // there was no match in the first place (we are after "if (foundNonMaxMatch)") 
					decrement(nonWordBuffer);
					if (isSLPModifier(c)) {
						continue;			   // move on and do as if the modifier didn't exist
					} else {
						break;
					}
				}
			} else if (isNonSLPprecededByNotEmptyNonWord()) {
				if (allCharsFromCurrentInitialAreConsumed() && c != ' ') {
				    potentialTokensContainMatches = addFoundTokenToPotentialTokensIfThereIsOne();
					if (allInitialsAreConsumed()) {
					    
						ifNoInitialsCleanupPotentialTokensAndNonwords();  
						storedInitials = null;
						if (thereIsNoTokenAndNoNonword()) {
							continue;							// resume looping over ioBuffer
						} else {
							break;								// and resume looping over ioBuffer
						}
					}
					resetNonWordBuffer(0);
					resetInitialCharsIterator();
					restoreInitialsOrigState();	
				} else {
					ifNoInitialsCleanupPotentialTokensAndNonwords();
					tokenBuffer.setLength(0);
					decrement(nonWordBuffer);
					break;
				}
			} else {
			    decrement(tokenBuffer);
			    decrement(nonWordBuffer);
			    nonWordStart = -1;
			}
		}

		/* B. HANDING THEM TO LUCENE */
		initials = null;				// all initials are consumed. reinitialize for next call of reconstructLemmas()
		initialCharsIterator = null;
		
		/* B.1. FILLING totalTokens */
		if (unsandhyingInitialsYieldedPotentialTokens()) {				
			if (potentialTokensContainMatches) {
			    if (nonWordPrecedes()) {
			        ifThereIsNonwordAddItToTotalTokens();
			    }
			    unsandhiFinalsAndAddLemmatizedMatchesToTotalTokens();

			} else {
				ifThereIsNonwordAddItToTotalTokens();
			}
			potentialTokens.clear();				// all potential tokens have been consumed, empty the variable
			ifSandhiMergesStayOnSameCurrentChar();	// so we can unsandhi the initial and find the start of next word
			finalsIndex = bufferIndex;               // save index of finals for currentCharIsSpaceWithinSandhi()

		} else {									// general case: no potential tokens
		    boolean aNonwordWasAdded = false;
		    if ((nonWordBuffer.length() > 0 && tokenBuffer.length() <= 0) || 
		            (nonWordBuffer.length() > 0 && tokenBuffer.length() > 0 && nonWordStart < tokenStart)) {
		        aNonwordWasAdded = ifThereIsNonwordAddItToTotalTokens();
		    }
			boolean lemmasWereAdded = ifUnsandhyingFinalsYieldsLemmasAddThemToTotalTokens();
			
			if (lemmasWereAdded) {
				ifSandhiMergesStayOnSameCurrentChar();	// so we can unsandhi the initial and find the start of next word					
				
				finalsIndex = bufferIndex;				// save index of finals for currentCharIsSpaceWithinSandhi()
			} else if (aNonwordWasAdded) {              // if a non-word was added, there was a match but no sandhi
            }
			
		}
		
		/* B.2. EXITING incrementToken() WITH THE TOKEN (OR THE FIRST ONE FROM totalTokens) */
		ifThereAreInitialsFillIterator();
		ifEndOfInputReachedEmptyInitials();

		if (thereAreTokensToReturn()) {
			hasTokenToEmit = true;
			final PreToken firstToken = totalTokens.removeFirst();
			final Integer[] metaData = firstToken.getMetadata();
			fillTermAttributeWith(firstToken.getString(), metaData);
			changeTypeOfToken(metaData[3]);
			return true;						// we exit incrementToken()
		
		} else {					// there is no non-word nor extra lemma to add. there was no sandhi for this token 			
			assert(tokenStart != -1);
			finalizeSettingTermAttribute();
			return true;						// we exit incrementToken()
		}
	}
	
	HashSet<String> reconstructLemmas(String cmd, String inflected) throws NumberFormatException, IOException {
	    return reconstructLemmas(cmd, inflected, -1);
	}
	
	/**
	 * Reconstructs all the possible sandhied strings for the first word using CmdParser.parse(),
	 * iterates through them, checking if the sandhied string is found in the sandhiable range,
	 * only reconstructs the lemmas if there is a match.
	 * @param i 
	 *
	 *
	 * @return: the list of all the possible lemmas given the current context
	 */
	HashSet<String> reconstructLemmas(String cmd, String inflected, int tokenEndIdx) throws NumberFormatException, IOException {
		HashSet<String> totalLemmas = new HashSet<String>();	// uses HashSet to avoid duplicates
		String[] t = new String[0];
		
		if (tokenEndIdx == -1) tokenEndIdx = bufferIndex;
		
		TreeMap<String, HashSet<String>> parsedCmd = new CmdParser().parse(inflected, cmd);
		for (Entry<String, HashSet<String>> current: parsedCmd.entrySet()) {
			String sandhied = current.getKey();
			HashSet<String> diffs = current.getValue();
			boolean foundAsandhi = false; 
			for (String lemmaDiff: diffs) {
				assert(lemmaDiff.contains("+"));		// all lemmaDiffs should contain +

				t = lemmaDiff.split("=");
				String diff = t[0];
				t = t[1].split("#");
				int sandhiType = Integer.parseInt(t[0]);
//				String pos = t[1];  // TODO: uncomment when implementing the token filter
				if (noSandhiAndLemmatizationNotRequired(sandhiType, diff)) {
					continue;							// there is no sandhi nor, so we skip this diff
				}
				
				if (containsSandhiedCombination(ioBuffer, tokenEndIdx - 1, sandhied, sandhiType)) {
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
						if (initials == null) {
							initials = new HashSet<String>();
							storedInitials = new HashSet<String>();
						}
						initials.add(newInitial);
						storedInitials.add(newInitial);
					} else {								// there no change in initial
						toAdd = t[1];
					}

					String lemma = inflected.substring(0, inflected.length()-toDelete)+toAdd;  // TODO: append pos once the token filter is in place
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
	
	private void decrement(StringBuilder buffer) {
	    buffer.setLength(buffer.length() - charCount);
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
			final String nonword = nonWordBuffer.toString();
			if (storedInitials.contains(nonword)) {
				resetNonWordBuffer(0);
//				storedInitials = null;			// !!! only reset after executing setTermLength()
			}
		}
	}

	private void ifEndOfInputReachedEmptyInitials() throws IOException {
		if (ioBuffer.get(bufferIndex) == -1) {
			initials = null;
		}
	}

	private void finalizeSettingTermAttribute() {
		finalOffset = correctOffset(tokenStart + tokenBuffer.length());
		offsetAtt.setOffset(correctOffset(tokenStart), finalOffset);
		termAtt.setEmpty().append(tokenBuffer.toString());
	}

	private void changeTypeOfToken(int t) {
		if (t == 0) {  
			typeAtt.setType("non-word");
		} else if (t == 1) {
		    typeAtt.setType("word");
		} else if (t == 2) {
            typeAtt.setType("lemma");
        }
	}

	private void fillTermAttributeWith(String string, Integer[] metaData) {
		termAtt.setEmpty().append(string);								// add the token string
		termAtt.setLength(metaData[2]);									// declare its size
		finalOffset = correctOffset(metaData[1]);						// get final offset 
		offsetAtt.setOffset(correctOffset(metaData[0]), finalOffset);	// set its offsets (initial & final)
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
				sandhiIndex -= charCount;
			}
			mergesInitials = false;					// reinitialize variable
		}
	}

	private boolean ifUnsandhyingFinalsYieldsLemmasAddThemToTotalTokens() throws NumberFormatException, IOException {
		String cmd = scanner.getCommandVal(foundMatchCmdIndex);
		if (cmd != null) {
		    String token = tokenBuffer.toString();
			if (debug) System.out.println("form found: " + token + "\n");
			final Set<String> lemmas = reconstructLemmas(cmd, token);
			if (lemmas.size() != 0) {
				for (String l: lemmas) {
				    final PreToken newToken = new PreToken(l, 
				            new Integer[] {tokenStart, tokenStart + tokenBuffer.length(), l.length(), 2});
					totalTokens.add(newToken);
					// use same start-end indices since all are from the same inflected form)
				}
				return true;
			}
		}
		return false;
	}

	private boolean ifThereIsNonwordAddItToTotalTokens() {
		final String nonWord = nonWordBuffer.toString();
		if (nonWord.length() > 0) {
		    final PreToken newToken = new PreToken(nonWord,
		            new Integer[] {nonWordStart, nonWordStart + nonWordBuffer.length(), nonWord.length(), 0, 0});
			totalTokens.add(newToken);
			// ignore all potential tokens. add the non-word with sandhied initials
			return true;
		}
		return false;
	}

	private void unsandhiFinalsAndAddLemmatizedMatchesToTotalTokens() throws NumberFormatException, IOException {
		for (Entry<String, Integer[]> entry: potentialTokens.entrySet()) {
			final String key = entry.getKey();
			final Integer[] value = entry.getValue();
			if (debug) System.out.println("form found: " + key + "\n");
			if (value[3] == 1) {
				String cmd = scanner.getCommandVal(value[4]);
				final Set<String> lemmas = reconstructLemmas(cmd, key, value[1]);
				if (lemmas.size() != 0) {
					for (String l: lemmas) {	// multiple lemmas are possible: finals remain unanalyzed
					    final PreToken newToken = new PreToken(l, new Integer[] {value[0], value[1], l.length(), 2});
						totalTokens.add(newToken);	
						// use same indices for all (all are from the same inflected form)
					}
				} else {	// there is no applicable sandhi. the form is returned as-is.
					final PreToken newToken = new PreToken(key, new Integer[] {value[0], value[1], value[2], 1});
				    totalTokens.add(newToken);
				    mergesInitials = false;
				}
			}
		}
	}

	private void cutOffTokenFromNonWordBuffer() {
		int newSize = nonWordBuffer.length() - tokenBuffer.length();
		newSize = newSize < 0 ? 0: newSize;   // ensure the new size is never negative
	    nonWordBuffer.setLength(newSize);
		// end of non-word can be: a matching word starts (potentialEnd == true) OR a nonSLP char follows a nonWord.
	}

	private void ifIsNeededInitializeStartingIndexOfNonword() {
		if (nonWordStart == -1) {							// the starting index of a non-word token does not increment
			nonWordStart = bufferIndex - charCount;
		}
	}

	private void incrementTokenIndices() {
	    if (tokenStart == -1) {
	        tokenStart = bufferIndex - charCount;
	    }
	}

	private boolean tryToContinueDownTheTrie(Row row, int c) {
		int ref = row.getRef((char) c);
		currentRow = (ref >= 0) ? scanner.getRow(ref) : null;
		return (currentRow == null) ? false: true;
	}

	private boolean tryToFindMatchIn(Row row, int c) {
	    cmdIndex = row.getCmd((char) c);
		foundMatch = (cmdIndex >= 0);
		if (foundMatch) {
			foundMatchCmdIndex = cmdIndex;
			foundNonMaxMatch = storeNonMaxMatchState();
			if (storedNoMatchState == -1) {
			    storeNoMatchState();
			    storedNoMatchState = 1;
			}
			return true;
		}
		return false;
	}

    private boolean storeNoMatchState() {
        noMatchBufferIndex = bufferIndex;
        noMatchTokenStart = (tokenStart == -1) ? 0: tokenStart;
        noMatchBuffer.setLength(0);
        noMatchBuffer.append(tokenBuffer);
        noMatchFoundMatchCmdIndex = foundMatchCmdIndex;
        return true;
    }

    private void restoreNoMatchState() {
        bufferIndex = noMatchBufferIndex;
        tokenStart = noMatchTokenStart;
        currentRow = rootRow;
        foundMatchCmdIndex = noMatchFoundMatchCmdIndex;
    }
    
	private boolean storeNonMaxMatchState() {
		nonMaxBufferIndex = bufferIndex;
		nonMaxTokenStart = (tokenStart == -1) ? 0: tokenStart;
        nonMaxBuffer.setLength(0);
        nonMaxBuffer.append(tokenBuffer);
	    nonMaxFoundMatchCmdIndex = foundMatchCmdIndex;
	    nonMaxNonWordLength = nonWordBuffer.length();
		return true;
	}
	
    private void restoreNonMaxMatchState() {
        bufferIndex = nonMaxBufferIndex;
        tokenStart = nonMaxTokenStart;
        currentRow = rootRow;
        tokenBuffer.setLength(0);
        tokenBuffer.append(nonMaxBuffer);
        foundMatchCmdIndex = nonMaxFoundMatchCmdIndex;
        nonWordBuffer.setLength(nonMaxNonWordLength);
    }
	
    private void storeCurrentState() {
        initialsOrigBufferIndex = bufferIndex - 1;
        initialsOrigTokenStart = (tokenStart == -1) ? 0: tokenStart;
        initialsOrigBuffer.setLength(0);
        initialsOrigBuffer.append(tokenBuffer);
    }
	
    private void restoreInitialsOrigState() {       /* return to the beginning of the token in ioBuffer */
        bufferIndex = initialsOrigBufferIndex;
        tokenStart = initialsOrigTokenStart;
        currentRow = rootRow;
        tokenBuffer.setLength(0);
        tokenBuffer.append(initialsOrigBuffer);
        storedNoMatchState = -1;
    }

	private void resetNonWordBuffer(int i) {
		if (nonWordBuffer.length() - i > 0) {
			nonWordBuffer.setLength(i);
		} else {
			nonWordBuffer.setLength(0);
		}
	}

	private void addNonwordToPotentialTokensIfThereIsOne() {
		if (nonWordBuffer.length() != 0) {
		    potentialTokens.put(nonWordBuffer.toString(),  
	                new Integer[] {nonWordStart, nonWordStart + nonWordBuffer.length(), nonWordBuffer.length(), 0, -1});
		}
	}

	private boolean addFoundTokenToPotentialTokensIfThereIsOne() {
		if (tokenBuffer.length() > 0) {																// avoid empty tokens
			final String potentialToken = tokenBuffer.toString();
			potentialTokens.put(potentialToken,  
			        new Integer[] {tokenStart, tokenStart + tokenBuffer.length(), potentialToken.length(), 1, foundMatchCmdIndex});
			return true;
		}
		return false;
	}

	private void initializeInitialCharsIteratorIfNeeded() {
		if (initialCharsIterator == null) {
			initialCharsIterator = new StringCharacterIterator(initialsIterator.next());	
			// initialize the iterator with the first initials
			initialsIterator.remove();    // remove the initials just fed to the initialsCharsIterator
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

	private void addExtraToken() {
		if (totalTokens.peekFirst() != null) {
			final PreToken nextToken = totalTokens.removeFirst();
			final Integer[] metaData = nextToken.getMetadata();
                termAtt.setEmpty().append(nextToken.getString());
			changeTypeOfToken(metaData[3]);
			termAtt.setLength(metaData[2]);
			finalOffset = correctOffset(metaData[1]);
			offsetAtt.setOffset(correctOffset(metaData[0]), finalOffset);
		} else {
			hasTokenToEmit = false;
		}
	}
	
	final private boolean isLoneInitial() {
	    boolean isInitial = false;
	    if (storedInitials != null) {
	        String tokenStr = tokenBuffer.toString();
	        for (String initial: storedInitials) {
	            if (tokenStr.equals(initial) && nonWordBuffer.length() == 0) {
	                isInitial = true;
	            }
	        }
	    }
	    return isInitial;
	}
	
	final private boolean nonWordPrecedes() {
	    int nonWordStartIdx = -1;
	    int wordStartIdx = -1;
	    for (Integer[] value: potentialTokens.values()) {
	        if (value[3] == 0) {
	            nonWordStartIdx = value[0];
	        } else if (value[3] == 1) {
	            wordStartIdx = value[0];
	        }
	    }
	    return nonWordStartIdx != -1 && wordStartIdx > nonWordStartIdx;
	}
	
	final private boolean thereAreRemainingInitialsToTest() {
	    /* To remember: returns false if (foundMatch == true), even if there are remaining initials */
	    return wentToMaxDownTheTrie && foundMatch == false && initialsNotEmpty();
	}
	
	final private boolean initialsNotEmpty() {
	    return initials != null && storedInitials != null && !initials.isEmpty() && initials.size() <= storedInitials.size() - 1;
	}
	
	final private boolean noSandhiAndLemmatizationNotRequired(int sandhiType, String diff) {
	    return sandhiType == 0 && diff.equals("/");
	}
	
	final private boolean nonwordIsLoneInitial() {
	    return storedInitials != null && storedInitials.contains(nonWordBuffer.toString());
	}
	
	final private boolean matchIsLoneInitial() {
	    return tokenBuffer.length() == 1 && storedInitials != null && storedInitials.contains(tokenBuffer.toString());
	}
	
	final private boolean isSLPTokenChar(int c) {
	    return SkrtSyllableTokenizer.charType.get(c) != null;
		// SLP modifiers are excluded because they are not considered to be part of a word/token. 
		// TODO: If a modifier occurs between two sandhied words, second word won't be considered sandhied
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
		return tokenBuffer.length() == 0 && nonWordBuffer.length() == 0;
	}
	
	final private boolean wentBeyondLongestMatch() {
		return foundNonMaxMatch && wentToMaxDownTheTrie && foundMatch == false;
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
	    return currentRow == null && nonWordBuffer.length() - charCount > 0;
	}

	final private boolean isNonSLPprecededBySLP() {
		return tokenBuffer.length() > 1;
	}

	final private boolean reachedEndOfInputString() throws IOException {
		return ioBuffer.get(bufferIndex) == -1;
	}

	final private boolean allCharsFromCurrentInitialAreConsumed() {
		return initials != null && initialCharsIterator.current() == CharacterIterator.DONE;
	}

	final private boolean isStartOfTokenOrIsNonwordChar() {
		return tokenBuffer.length() == 1;
	}

	final private boolean startConsumingInitials() {
		return initialCharsIterator == null;
	}

	final private boolean stillConsumingInitials() {
		return initialCharsIterator.getIndex() < initialCharsIterator.getEndIndex();
	}

	final private boolean initialIsNotFollowedBySandhied(int c) {
	    return isValidCharWithinSandhi(c) && (firstInitialIndex == -1 || bufferIndex == firstInitialIndex + 1);
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
		totalTokens = new LinkedList<PreToken>();

		finalsIndex = -1;
		hasTokenToEmit = false;	// for emitting multiple tokens

	}
	
	public static class PreToken {
	    String tokenString;
	    Integer[] tokenMetaData;
	    
	    public PreToken(String string, Integer[] metaData) {
	        this.tokenString = string;
	        this.tokenMetaData = metaData;
	    }
	    
	    public String getString() {
	        return tokenString;
	    }
	    
	    public Integer[] getMetadata() {
	        return tokenMetaData;
	    }
	}
}
