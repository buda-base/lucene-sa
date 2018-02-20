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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
 * The expected input is an SLP string<br>
 * {@link SkrtSyllableTokenizer#isSLP(int)} is used to filter out nonSLP
 * characters.
 *
 * <p>
 * The necessary information for unsandhying finals and initials is taken from
 * {@link resources/sanskrit-stemming-data/output/total_output.txt}
 * 
 * <br>
 * Due to its design, this tokenizer doesn't deal with contextual
 * ambiguities.<br>
 * For example, "nagaraM" could either be a word of its own or "na" + "garaM",
 * but is always parsed as a single word
 *
 * Derived from Lucene 6.4.1 CharTokenizer, but differs by using a
 * RollingCharBuffer to still find tokens that are on the IO_BUFFER_SIZE (4096
 * chars)
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
     * Default constructor: uses the default compiled Trie, builds it if it is
     * missing.
     * 
     * @throws FileNotFoundException
     *             the file containing the Trie can't be found
     * @throws IOException
     *             the file containing the Trie can't be read
     * 
     */
    public SkrtWordTokenizer() throws FileNotFoundException, IOException {
        InputStream stream = null;
        stream = SkrtWordTokenizer.class.getResourceAsStream("/skrt-compiled-trie.dump");
        if (stream == null) { // we're not using the jar, there is no resource, assuming we're running the
                              // code
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
     * 
     * @param filename
     *            the file containing the entries of the Trie
     */
    public SkrtWordTokenizer(String filename) throws FileNotFoundException, IOException {
        init(filename);
    }

    /**
     * Opens an already compiled Trie
     * 
     * @param trieStream
     *            an InputStream (FileInputStream, for ex.) containing the compiled
     *            Trie
     */
    public SkrtWordTokenizer(InputStream trieStream) throws FileNotFoundException, IOException {
        init(trieStream);
    }

    /**
     * Uses the given Trie
     * 
     * @param trie
     *            a Trie built using {@link BuildCompiledTrie}
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
     * 
     * @param debug
     * @param filename
     *            the file containing the entries of the Trie
     */
    public SkrtWordTokenizer(boolean debug, String filename) throws FileNotFoundException, IOException {
        init(filename);
        this.debug = debug;
    }

    /**
     * Opens an already compiled Trie. Prints debug info
     * 
     * @param debug
     * @param trieStream
     *            an InputStream containing the compiled Trie
     */
    public SkrtWordTokenizer(boolean debug, InputStream trieStream) throws FileNotFoundException, IOException {
        init(trieStream);
        this.debug = debug;
    }

    /**
     * Uses the given Trie. Prints debug info
     * 
     * @param debug
     * @param trie
     *            a Trie built using {@link BuildCompiledTrie}
     */
    public SkrtWordTokenizer(boolean debug, Trie trie) {
        init(trie);
        this.debug = debug;
    }

    /**
     * Opens an existing compiled Trie
     * 
     * @param inputStream
     *            the compiled Trie opened as a Stream
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
     * @param filename
     *            the Trie as a {@code .txt} file
     */
    private void init(String filename) throws FileNotFoundException, IOException {
        this.scanner = BuildCompiledTrie.buildTrie(Arrays.asList(filename));

        ioBuffer = new RollingCharBuffer();
        ioBuffer.reset(input);
    }

    /**
     * Uses the given Trie
     * 
     * @param trie
     *            a Trie built using {@link BuildCompiledTrie}
     */
    private void init(Trie trie) {
        this.scanner = trie;

        ioBuffer = new RollingCharBuffer();
        ioBuffer.reset(input);
    }

    static List<Character> SANDHIABLE_SEP = Arrays.asList(' ', '-');
    
    /* current token related */
    private int wordStart;
    private StringBuilder wordBuffer = new StringBuilder();
    private Row rootRow, currentRow;
    private int cmdIndex, foundMatchCmdIndex;
    private boolean foundMatch;
    private boolean afterNonwordMatch;

    /* nonMaxMatch related */
    private boolean foundNonMaxMatch, wentToMaxDownTheTrie;
    private StringBuilder nonMaxBuffer = new StringBuilder();
    private int nonMaxWordStart, nonMaxBufferIndex, nonMaxFoundMatchCmdIndex, nonMaxNonWordLength;

    /* tokens related */
    private TreeMap<Integer, HashMap<String, PreToken>> potentialTokens = new TreeMap<Integer, HashMap<String, PreToken>>();
    // contains : {startingIndex, endingIndex, tokenLength, 1 (if match) or 0, cmdIndex or -1}

    /* nonWords related */
    private int nonWordStart;
    private StringBuilder nonWordBuffer = new StringBuilder();

    /* totalTokens related */
    private LinkedList<PreToken> totalTokens = new LinkedList<PreToken>();
    private boolean hasTokenToEmit;

    /* initials related */
    private HashSet<String> initials = null;
    private Iterator<String> initialsIterator = null;
    private StringCharacterIterator initialCharsIterator = null;

    private int initialsOrigBufferIndex, initialsOrigWordStart, initialsOrigFoundMatchCmdIndex = -1;
    private StringBuilder initialsOrigBuffer = new StringBuilder();
    private HashSet<String> storedInitials = null;

    private static boolean mergesInitials = false;
    private int finalsIndex = -1;
    private int firstInitialIndex;
    private boolean applyOtherInitial;

    /* ioBuffer related (contains the input string) */
    private RollingCharBuffer ioBuffer;
    private int ioBufferIndex = 0, finalOffset = 0;
    int MAX_WORD_LEN = 255;

    /* previous state related */
    private int storedNoMatchState, noMatchWordStart, noMatchBufferIndex, noMatchFoundMatchCmdIndex;
    private StringBuilder noMatchBuffer = new StringBuilder();

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();

        /* B.3. ADDING REMAINING EXTRA TOKENS */
        if (hasTokenToEmit == true) {
            if (totalTokens.peekFirst() != null) {
                fillTermAttributeWithNextToken();
                return true;
            } else {
                hasTokenToEmit = false;
                totalTokens.clear();
            }
        }

        if (ioBufferIndex - 4 >= 0)
            ioBuffer.freeBefore(ioBufferIndex - 4);
        wordStart = -1;
        rootRow = scanner.getRow(scanner.getRoot());
        currentRow = null;
        cmdIndex = -1;
        foundMatchCmdIndex = -1;
        foundMatch = false;
        afterNonwordMatch = false;

        nonWordBuffer.setLength(0);
        nonWordStart = -1;

        nonMaxBuffer.setLength(0);
        nonMaxWordStart = -1;
        nonMaxBufferIndex = -1;
        nonMaxFoundMatchCmdIndex = -1;
        nonMaxNonWordLength = -1;
        foundNonMaxMatch = false;
        wentToMaxDownTheTrie = false;
        firstInitialIndex = -1;
        applyOtherInitial = false;

        initialsOrigBufferIndex = -1; // did not fix the bug
        initialsOrigWordStart = -1;
        initialsOrigFoundMatchCmdIndex = -1;
        
        noMatchBuffer.setLength(0);
        noMatchWordStart = -1;
        noMatchBufferIndex = -1;
        storedNoMatchState = -1;
        noMatchFoundMatchCmdIndex = -1;

        wordBuffer.setLength(0);
        
        @SuppressWarnings("unused") // these two variables are not used.
        boolean match = false;
        @SuppressWarnings("unused") // they only provide humans an easy way to understand what is happening
        boolean continuing = false;
        if (debug) System.out.println("----------------------");

        /* A. FINDING TOKENS */
        while (true) {

            /* fill c with cars from ioBuffer */
            int c = ioBuffer.get(ioBufferIndex); // take next char in ioBuffer
            ioBufferIndex += Character.charCount(c); // increment bufferIndex for next value of c
            if (debug) System.out.print((char) c);

            /* when ioBuffer is empty (end of input, ...) */
            if (c == -1) {
                ioBufferIndex--;
                if (wordBuffer.length() == 0 && nonWordBuffer.length() == 0) {
                    finalOffset = correctOffset(ioBufferIndex);
                    return false;
                }
                break;
            }
            
            /* 3>>>>> PROCESS INITIALS >>>>>3 */
            
            /* if (there are initials to consume) */
            if (initials != null && !initials.isEmpty() && ioBuffer.get(ioBufferIndex) != -1) {

                /* if (current char is a space directly following the sandhied word on the left) 
                 * we simply ignore this particular space */
                if (finalsIndex + 1 == ioBufferIndex && SANDHIABLE_SEP.contains((char) c)) {
                    continue;

                /* if (the initial is not followed by a sandhied word) */
                } else if (SANDHIABLE_SEP.contains((char) c) && 
                        (firstInitialIndex == -1 || ioBufferIndex == firstInitialIndex + 1)) {
                    initials = null;
                    initialCharsIterator = null;
                    continue;
                
                /* if (consuming first initial)
                 * since initials == [] when all initials are consumed */
                } else if (initialCharsIterator == null) {
                    storeInitialsOrigState();
                    initializeInitialCharsIteratorIfNeeded();
                    firstInitialIndex = ioBufferIndex;
                    c = applyInitialChar();
                    if (debug) System.out.print("=>" + (char) c);
                    
                /* if (there are more initials to consume) */
                } else if (initialCharsIterator.getIndex() < initialCharsIterator.getEndIndex() || applyOtherInitial) {
                    initializeInitialCharsIteratorIfNeeded();
                    c = applyInitialChar();
                    if (nonWordBuffer.length() > 0) 
                        decrement(nonWordBuffer);
                    applyOtherInitial = false;
                    if (debug) System.out.print("=>" + (char) c);
                }
            }
            /* 3<<<<<<<<<<<<<<<<<<<<<<<<<<<<3 */
            
            /* increment buffers */
            wordBuffer.append((char) c);
            nonWordBuffer.append((char) c); // later remove chars belonging to a token
            if (debug) System.out.println("");
            
            /* 1>>>>> DECISION PROCESS: c IS SLP >>>>>1 */
            
            /* if (current char is valid SLP) 
             * TODO: If a modifier occurs between two sandhied words, 
             * second word won't be considered sandhied*/
            if (SkrtSyllableTokenizer.charType.get(c) != null) {
                
                /* if (current char is a SLP modifying char) */
                if (isSLPModifier(c)) {
                    decrement(wordBuffer);
                    decrement(nonWordBuffer);
                    continue;
                }
                
                /* +>+>+ WALK THE TRIE +>+>+ */

                /* if (is beginning of a word. also happens after each non-word char)
                 * note: increment just happened, hence "== 1" */ 
                if (wordBuffer.length() == 1) {
                    /*
                     * we enter on two occasions: at the actual start of a token, 
                     *                            at each new non-word character.
                     */
                    match = tryToFindMatchIn(rootRow, c);
                    continuing = tryToContinueDownTheTrie(rootRow, c);
                    wordStart = ioBufferIndex - 1;
                    if (nonWordStart == -1)
                        nonWordStart = wordStart;

                } else {
                    /*
                     * we enter here on all other occasions: 
                     * we don't know if word chars will be a match or not
                     */

                    match = tryToFindMatchIn(currentRow, c);
                    continuing = tryToContinueDownTheTrie(currentRow, c);
                    /* if (we reached a non-word char) 
                     * equals "match == false && continuing == false" */
                    if (currentRow == null && foundMatch == false) {
                        if (!foundNonMaxMatch && storedNoMatchState == 1) {
                            restoreNoMatchState();
                            storedNoMatchState = 0;
                            continue;
                            
                        } else if (!foundNonMaxMatch) {
                            match = tryToFindMatchIn(rootRow, c);
                            continuing = tryToContinueDownTheTrie(rootRow, c);
                            wordBuffer.setLength(0);
                            if (foundMatch)
                                afterNonwordMatch = true;
                        }
                        wentToMaxDownTheTrie = true;
                        storedNoMatchState = -1;
//                        if (wordBuffer.length() == 0)
//                            wordBuffer.append((char) c);
                    }
                }
                /* +<+<+<+<+<+<+<+<+<+<+<+<+ */
                
                /* if (went farther than the longest match) */  
                if (wentToMaxDownTheTrie && foundNonMaxMatch && foundMatch == false) {  //TOTEST: foundMatch == false superfluous???
                    restoreNonMaxMatchState();

                    /* if (current word is a reconstructed initial without matching sandhi context) 
                     * TODO: currently does not check for a non-sandhi context */
//                    if (wordBuffer.length() == 1 && storedInitials != null && storedInitials.contains(wordBuffer.toString())) {
                        if (storedInitials != null && ((wordBuffer.length() == 1 && storedInitials.contains(wordBuffer.toString()) 
                                || (nonWordBuffer.length() == 1 && storedInitials.contains(nonMaxBuffer.toString())) ))) {
                        foundNonMaxMatch = false;
                        foundMatch = false;  // TOTEST: superfluous?
                        foundMatchCmdIndex = -1;

                    /* if (there are more initials to consume) 
                     * note: fails if (foundMatch == true), even if there are remaining initials */ 
                    } else if (wentToMaxDownTheTrie && foundMatch == false && // TOTEST: 1st 2 conditions superfluous???
                            initials != null && storedInitials != null && !initials.isEmpty()
                            && initials.size() <= storedInitials.size() - 1 ) { // TOTEST: last condition superfluous???
                        /* Add all found words and non-words, then go on with the next initial*/
                        fillPotentialTokens();
                        restoreInitialsOrigState();
                        nonWordBuffer.setLength(0);
                        wentToMaxDownTheTrie = false;
                        applyOtherInitial = true;

                    } else {
                        cutOffTokenFromNonWordBuffer();
                        finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
                        break;
                    }

                /* if (there are more initials to consume) */
                } else if (wentToMaxDownTheTrie && foundMatch == false && initialsNotEmpty()) { // TOTEST: 1st 2 conditions superfluous???
                    fillPotentialTokens();
                    restoreInitialsOrigState();
                    nonWordBuffer.setLength(0);
                    wentToMaxDownTheTrie = false;
                    applyOtherInitial = true;

                /* if (we reached a non-word char) 
                 * equals "match == false && continuing == false" */
                } else if (currentRow == null && foundMatch == false) {
                    wordBuffer.setLength(0); // because no word ever started in the first place

                /* if (there is a match and either can't continue down the Trie or reached the end of input) */
                } else if (foundMatch == true  && (currentRow == null || ioBuffer.get(ioBufferIndex) == -1)) {
                    if (!afterNonwordMatch)
                        cutOffTokenFromNonWordBuffer();
                    
                    /* if (all chars from current initial have been consumed) */
                    if (initials != null && initialCharsIterator.current() == CharacterIterator.DONE) { // TOTEST check that initialCharsIterator != null
                        
                        /* if (all initials have been consumed, there is no word, there is no non-word)  */
                        if (!initialsIterator.hasNext() && wordBuffer.length() != 0 && nonWordBuffer.length() != 0) { // last 2 superfluous ???
                            finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
                            break;
                        }
                        initialCharsIterator.setIndex(0);
                        fillPotentialTokens();
                        restoreInitialsOrigState();
                        if (wentToMaxDownTheTrie && initialsNotEmpty()) {
                            foundNonMaxMatch = false;
                            nonWordBuffer.setLength(0);
                        }
                    } else {
                        finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
                        break;
                    }
                    
                /* if (the entire input string has been consumed) */
                } else if (ioBuffer.get(ioBufferIndex) == -1) { // we are within a potential token
                    wordBuffer.setLength(0);

                    /* if (all chars from current initial have been consumed) */
                    if (initials != null && initialCharsIterator.current() == CharacterIterator.DONE) { // TOTEST check that initialCharsIterator != null
                        
                        /* if (all initials have been consumed) 
                         * break if true, else re-attempt to find a match with next initial */
                        if (!initialsIterator.hasNext())
                            finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
                            break;
                        
                    } else {
                        
                        /* if (there is a match to restore and return) */
                        if (foundNonMaxMatch) {
                            restoreNonMaxMatchState();
                            cutOffTokenFromNonWordBuffer();
                        }
                        finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
                        break;
                    }
                }
            /* 1<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<1 */
                
                /* wordBuffer corner case: buffer overflow! */
                if (wordBuffer.length() >= MAX_WORD_LEN)
                    break;

            /* 2>>>>> DECISION PROCESS: c IS NON-SLP >>>>>2 */
            
            /* if (there is a match to restore and return) */
            } else if (foundNonMaxMatch) {
                restoreNonMaxMatchState();

                /* if (current word is a reconstructed initial without matching sandhi context) 
                 * TODO: currently does not check for a non-sandhi context */
//                if (wordBuffer.length() == 1 && storedInitials != null && storedInitials.contains(wordBuffer.toString())) {
//                    wordBuffer.setLength(0);
                if (storedInitials != null && ((wordBuffer.length() == 1 && storedInitials.contains(wordBuffer.toString()) 
                        || (nonWordBuffer.length() == 1 && storedInitials.contains(nonMaxBuffer.toString())) ))) {
                    nonWordBuffer.setLength(0);
                    foundNonMaxMatch = false;
                    foundMatchCmdIndex = -1;
                    storedNoMatchState = -1;
                    continue;
                }
                finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
                break;

            /* if (the previous char was a SLP char) 
             * word has content or (non-word has content and can't continue down the Trie) */ 
            } else if ((wordBuffer.length() > 1) || (currentRow == null && nonWordBuffer.length() > 1)) {
                wordBuffer.setLength(0);   // there was no match in the first place (we are after "if (foundNonMaxMatch)")
                decrement(nonWordBuffer);
                
                /* if (all chars from current initial have been consumed) */
//                if (initials != null && initialCharsIterator.current() == CharacterIterator.DONE) {  // TOTEST check that initialCharsIterator != null
                if (initials != null && initialCharsIterator.current() == CharacterIterator.DONE) {
                    
                    /* if (current word is a reconstructed initial without matching sandhi context) 
                     * TODO: currently does not check for a non-sandhi context */
                    if (wordBuffer.length() == 1 && storedInitials != null && storedInitials.contains(wordBuffer.toString())) {
                        wordBuffer.setLength(0);
                        foundMatchCmdIndex = -1;
                    }
                    
                    /* if (all initials have been consumed) 
                     * break if true, else re-attempt to find a match with next initial */
                    if (!initialsIterator.hasNext()) {
                        finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
                        break;
                    }
                    
                    nonWordBuffer.setLength(0);
                    initialCharsIterator.setIndex(0);
                    fillPotentialTokens();
                    restoreInitialsOrigState();
                
                } else {
                    /* if (current char is a SLP modifying char) */
                    if (isSLPModifier(c)) {
                        finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
                    }
                    break;
                }
            } else {
                decrement(wordBuffer);
                decrement(nonWordBuffer);
            }
            /* 2<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<2 */
        }
        
        fillPotentialTokens();
        
        /* B. HANDING THEM TO LUCENE */
        initials = null; // all initials are consumed. reinitialize for next call of reconstructLemmas()
        initialCharsIterator = null;

        /* fill totalTokens */
        assert(!potentialTokens.isEmpty()); // Check that everything goes through potentialTokens
        for (HashMap<String, PreToken> potentialToken: potentialTokens.values()) {
            if (potentialToken.containsKey("word")) {
                fillTotalTokensWithLemmas(potentialToken.get("word"));
            } else {
                totalTokens.add(potentialToken.get("nonWord"));
            }
        }
        potentialTokens.clear();
        
        ifSandhiMergesStayOnCurrentChar(); // so we can unsandhi the initial and find the start of next word

        /* fill initial iterator */
        if (initials != null && !initials.isEmpty())
            initialsIterator = initials.iterator();

        /* empty initials if reached end of input */
        if (ioBuffer.get(ioBufferIndex) == -1)  // TODO: move this inside fillPotentialTokens
            initials = null;

        hasTokenToEmit = true;
        fillTermAttributeWith(totalTokens.removeFirst());
        return true;
    }

    private boolean tryToContinueDownTheTrie(Row row, int c) {
        int ref = row.getRef((char) c);
        currentRow = (ref >= 0) ? scanner.getRow(ref) : null;
        return (currentRow == null) ? false : true;
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
        noMatchBufferIndex = ioBufferIndex;
        noMatchWordStart = (wordStart == -1) ? 0 : wordStart;
        noMatchBuffer.setLength(0);
        noMatchBuffer.append(wordBuffer);
        noMatchFoundMatchCmdIndex = foundMatchCmdIndex;
        return true;
    }

    private void restoreNoMatchState() {
        ioBufferIndex = noMatchBufferIndex;
        wordStart = noMatchWordStart;
        wordBuffer.setLength(0);
        wordBuffer.append(noMatchBuffer);
        currentRow = rootRow;
        foundMatchCmdIndex = noMatchFoundMatchCmdIndex;
    }

    private boolean storeNonMaxMatchState() {
        nonMaxBufferIndex = ioBufferIndex;
        nonMaxWordStart = (wordStart == -1) ? 0 : wordStart;
        nonMaxBuffer.setLength(0);
        nonMaxBuffer.append(wordBuffer);
        nonMaxFoundMatchCmdIndex = foundMatchCmdIndex;
        nonMaxNonWordLength = nonWordBuffer.length();
        return true;
    }

    private void restoreNonMaxMatchState() {
        ioBufferIndex = nonMaxBufferIndex;
        wordStart = nonMaxWordStart;
        currentRow = rootRow;
        wordBuffer.setLength(0);
        wordBuffer.append(nonMaxBuffer);
        foundMatchCmdIndex = nonMaxFoundMatchCmdIndex;
        nonWordBuffer.setLength(nonMaxNonWordLength);
    }

    private void storeInitialsOrigState() {
        initialsOrigBufferIndex = ioBufferIndex - 1;
        initialsOrigWordStart = (wordStart == -1) ? initialsOrigBufferIndex: wordStart;
        initialsOrigBuffer.setLength(0);
        initialsOrigBuffer.append(wordBuffer);
        initialsOrigFoundMatchCmdIndex = foundMatchCmdIndex;
    }

    private void restoreInitialsOrigState() {
        ioBufferIndex = initialsOrigBufferIndex;
        wordStart = initialsOrigWordStart;
        currentRow = rootRow;
        wordBuffer.setLength(0);
        wordBuffer.append(initialsOrigBuffer);
        foundMatchCmdIndex = initialsOrigFoundMatchCmdIndex;
        storedNoMatchState = -1;
    }
    
    /**
     * fill potentialTokens in the order of precedence in the input string
     * @param clear: if true, empty potentialTokens
     */
    private void fillPotentialTokens() {
        HashMap<String, PreToken> t;
        PreToken nonWordPreToken = null, wordPreToken = null;
        if (wordBuffer.length() > 0) 
            wordPreToken = new PreToken(wordBuffer.toString(), 
                    new Integer[] {wordStart, wordStart + wordBuffer.length(), wordBuffer.length(), 1, foundMatchCmdIndex});
        if (nonWordBuffer.length() > 0)
            nonWordPreToken = new PreToken(nonWordBuffer.toString(),
                    new Integer[] {nonWordStart, nonWordStart + nonWordBuffer.length(), nonWordBuffer.length(), 0});
        
        if (wordPreToken != null) {
            if (!potentialTokens.isEmpty() && potentialTokens.containsKey(wordStart)) {
                t = potentialTokens.get(wordStart);
                t.put("word", wordPreToken);
                potentialTokens.replace(wordStart, t);
            } else {
                t = new HashMap<String, PreToken>();
                t.put("word", wordPreToken);
                potentialTokens.put(wordStart, t);
            }
        }
        
        if (nonWordPreToken != null) {
            if (potentialTokens.containsKey(nonWordStart)) {
                t = potentialTokens.get(nonWordStart);
                t.put("nonWord", nonWordPreToken);
                potentialTokens.replace(nonWordStart, t);
            } else {
                t = new HashMap<String, PreToken>();
                t.put("nonWord", nonWordPreToken);
                potentialTokens.put(nonWordStart, t);
            }
        } 
    }

    private void fillTotalTokensWithLemmas(PreToken p) throws NumberFormatException, IOException {                
        if (debug) System.out.println("form found: " + p.getString() + "\n");
        
        final Set<String> lemmas = reconstructLemmas(scanner.getCommandVal(p.getCmdIdx()), p.getString(), p.getEndIdx());
        if (lemmas.size() != 0) {
            for (String l : lemmas) { // multiple lemmas are possible: finals remain unanalyzed
                final PreToken newToken = new PreToken(l,
                        new Integer[] {p.getStartIdx(), p.getEndIdx(), l.length(), 2});
                totalTokens.add(newToken);
                // use same indices for all (all are from the same inflected form)
//                finalsIndex = ioBufferIndex;              // save index of finals for currentCharIsSpaceWithinSandhi()
            }
        } else {
            totalTokens.add(p);
            mergesInitials = false;
        }
    }

    private void fillTermAttributeWithNextToken() {
        final PreToken p = totalTokens.removeFirst();
        termAtt.setEmpty().append(p.getString());
        changeTypeOfToken(p.getTokenType());
        termAtt.setLength(p.getLength());
        finalOffset = correctOffset(p.getEndIdx());
        offsetAtt.setOffset(correctOffset(p.getStartIdx()), finalOffset);
    }

    private void decrement(StringBuilder buffer) {
        buffer.setLength(buffer.length() - 1);
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

    private void fillTermAttributeWith(PreToken p) {
        termAtt.setEmpty().append(p.getString()); // add the token string
        termAtt.setLength(p.getLength()); // declare its size
        finalOffset = correctOffset(p.getEndIdx()); // get final offset
        offsetAtt.setOffset(correctOffset(p.getStartIdx()), finalOffset); // set its offsets (initial & final)
        changeTypeOfToken(p.getTokenType());
    }

    private void ifSandhiMergesStayOnCurrentChar() throws IOException {
        if (mergesInitials) {
            if (ioBuffer.get(ioBufferIndex) != -1) { // if end of input is not reached
                ioBufferIndex--;
            }
            mergesInitials = false; // reinitialize variable
        }
    }

    private void cutOffTokenFromNonWordBuffer() {
        int newSize = nonWordBuffer.length() - wordBuffer.length();
        newSize = newSize < 0 ? 0 : newSize; // ensure the new size is never negative
        nonWordBuffer.setLength(newSize);
        // end of non-word can be: a matching word starts (potentialEnd == true) 
        //                         OR 
        //                         a nonSLP char follows a nonWord.
    }

    private void initializeInitialCharsIteratorIfNeeded() {
        if (initialCharsIterator == null) {
            initialCharsIterator = new StringCharacterIterator(initialsIterator.next());
            // initialize the iterator with the first initials
            initialsIterator.remove(); // remove the initials just fed to the initialsCharsIterator
        } else if (initialsIterator.hasNext()) {
            /*
             * either first time or initialCharsIterator has been reset 
             * AND 
             * there are more initials to process
             */
            initialCharsIterator.setText(initialsIterator.next());
            initialCharsIterator.setIndex(0);
            // fill with new initials. happens if we reach the end of a token 
            // (either a Trie match or a non-word)
            initialsIterator.remove(); // remove the initials just fed to the initialsCharsIterator
        }
    }

    private int applyInitialChar() { // TODO: a StringCharIterator is not what we want. https://stackoverflow.com/a/3925162
        int initial = initialCharsIterator.next();
        
//        if (initialCharsIterator.getIndex() == initialCharsIterator.getEndIndex()) {
//            initialCharsIterator.setIndex(0);
//        } else {
//            initialCharsIterator.setIndex(initialCharsIterator.getIndex() + 1); // increment iterator index
//        }
        return initial;
    }

    final private boolean initialsNotEmpty() {
        return initials != null && storedInitials != null && !initials.isEmpty()
                && initials.size() <= storedInitials.size() - 1;
    }

    final private boolean isSLPModifier(int c) {
        return SkrtSyllableTokenizer.charType.get(c) != null
                && SkrtSyllableTokenizer.charType.get(c) == SkrtSyllableTokenizer.MODIFIER;
    }

    HashSet<String> reconstructLemmas(String cmd, String inflected) throws NumberFormatException, IOException {
        return reconstructLemmas(cmd, inflected, -1);
    }

    /**
     * Reconstructs all the possible sandhied strings for the first word using
     * CmdParser.parse(), iterates through them, checking if the sandhied string is
     * found in the sandhiable range, only reconstructs the lemmas if there is a
     * match.
     * 
     * @param i
     *
     *
     * @return: the list of all the possible lemmas given the current context
     */
    HashSet<String> reconstructLemmas(String cmd, String inflected, int tokenEndIdx)
            throws NumberFormatException, IOException {
        HashSet<String> totalLemmas = new HashSet<String>(); // uses HashSet to avoid duplicates
        String[] t = new String[0];

        if (tokenEndIdx == -1)
            tokenEndIdx = ioBufferIndex;

        TreeMap<String, HashSet<String>> parsedCmd = new CmdParser().parse(inflected, cmd);
        for (Entry<String, HashSet<String>> current : parsedCmd.entrySet()) {
            String sandhied = current.getKey();
            HashSet<String> diffs = current.getValue();
            boolean foundAsandhi = false;
            for (String lemmaDiff : diffs) {
                assert (lemmaDiff.contains("+")); // all lemmaDiffs should contain +

                t = lemmaDiff.split("=");
                String diff = t[0];
                t = t[1].split("#");
                int sandhiType = Integer.parseInt(t[0]);
                // String pos = t[1]; // TODO: uncomment when implementing the token filter

                /* if (there is no sandhi and no lemmatization to do) */
                if (sandhiType == 0 && diff.equals("/")) {
                    continue; // there is no sandhi nor, so we skip this diff
                }

                if (containsSandhiedCombination(ioBuffer, tokenEndIdx - 1, sandhied, sandhiType)) {
                    foundAsandhi = true;
                    t = diff.split("\\+");

                    if (diff.endsWith("+")) { // ensures t has alway two elements
                        t = new String[2];
                        t[0] = diff.split("\\+")[0];
                        t[1] = "";
                    }

                    int toDelete = Integer.parseInt(t[0]);
                    String toAdd;
                    String newInitial = "";

                    if (t[1].contains("/")) { // there is a change in initial
                        t = t[1].split("/");
                        toAdd = t[0];
                        newInitial = t[1];
                        if (initials == null) {
                            initials = new HashSet<String>();
                            storedInitials = new HashSet<String>();
                        }
                        initials.add(newInitial);
                        storedInitials.add(newInitial);
                    } else { // there no change in initial
                        toAdd = t[1];
                    }

                    String lemma = inflected.substring(0, inflected.length() - toDelete) + toAdd; // TODO: append pos
                                                                                                  // once the token
                                                                                                  // filter is in place
                    totalLemmas.add(lemma);
                }
            }
            if (foundAsandhi)
                break;
        }
        return totalLemmas;
    }

    /**
     * Tells whether sandhied could be found between the two words. Does it by
     * generating all the legal combinations, filtering spaces and checking for
     * equality.
     * <p>
     * See SandhiedCombinationTests for how these figures were obtained
     *
     * @param ioBuffer:
     *            is given as parameter for the tests
     * @return: true if sandhied is one of the combinations; false otherwise
     */
    static boolean containsSandhiedCombination(RollingCharBuffer ioBuffer, int bufferIndex, String sandhied,
            int sandhiType) throws IOException {
        switch (sandhiType) {

        case 0:
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0); // no sandhi, but lemmatization required

        case 1:
            if (isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0)) { // vowel sandhi
                if (sandhied.length() == 1) {
                    mergesInitials = true;
                }
                return true;
            } else {
                return false;
            }

        case 2:
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0); // consonant sandhi 1

        case 3:
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, -1); // consonant sandhi 1 vowels

        case 4:
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0); // consonant sandhi 2

        case 5:
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, -1); // visarga sandhi

        case 6:
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, -1); // visarga sandhi 2

        case 7:
            // (consonant clusters are always reduced to the first consonant)
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0); // absolute finals sandhi

        case 8:
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, 0); // "cC"-words sandhi

        case 9:
            return isSandhiedCombination(ioBuffer, bufferIndex, sandhied, -4); // special sandhi: "punar"

        default:
            return false;
        }
    }

    static boolean isSandhiedCombination(RollingCharBuffer ioBuffer, int bufferIndex, String sandhied, int start)
            throws IOException {
        int j = 0;
        int nbIgnoredSpaces = 0;
        while (j < sandhied.length()) {
            final int res = ioBuffer.get(bufferIndex + start + j + nbIgnoredSpaces);
            if (SANDHIABLE_SEP.contains((char) res)) { //
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

    @Override
    public final void end() throws IOException {
        super.end();
        offsetAtt.setOffset(finalOffset, finalOffset); // set final offset
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        ioBufferIndex = 0;
        finalOffset = 0;
        ioBuffer.reset(input); // make sure to reset the IO buffer!!
        totalTokens = new LinkedList<PreToken>();

        finalsIndex = -1;
        hasTokenToEmit = false; // for emitting multiple tokens

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

        public int getStartIdx() {
            return tokenMetaData[0];
        }
        
        public int getEndIdx() {
            return tokenMetaData[1];
        }

        public int getLength() {
            return tokenMetaData[2];
        }
        
        public int getTokenType() {
            return tokenMetaData[3];
        }
        
        public int getCmdIdx() {
            if (tokenMetaData.length == 4) {    // is a non-word PreToken, so no cmd
                return -1;
            }
            return tokenMetaData[4];
        }
    }
}
