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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/** 
 * Parses cmds from the total Trie {@code total_output.txt} and reconstructs
 * the sandhied substring found between the two words undergoing sandhi.
 * <p> 
 * Is used by {@link SkrtWordTokenizer#reconstructLemmas} 
 * 
 * @author Hélios Hildt
 * 
 */
public class CmdParser {
	private String[] t = new String[2];  // temporary variable constantly reused
	
	private int sandhiType = -1;
	private Integer pos = null;
	private String entry = null;
	
	private String[] initials = null;
	
	private String diffInitial = null;   // there can only be one initial diff
	private String[] diffFinals = null;
	
	// for sandhied (HashMap key)
	private Integer toDelete = null;
	private String initialCharsSandhied = null;
	
	// for unsandhied (HashMap value)
	private String initialCharsOriginal = null;
	private String toAdd = null;
	
	private TreeMap<String, TreeSet<DiffStruct>> sandhis = null;

    private int idempotentGroup = -1;
	
	private static final HashMap<Integer, List<String>> idempotentInitials = new HashMap<Integer, List<String>>() 
	{
        private static final long serialVersionUID = 1L;

    { 
	    // vowels
        put(1, Arrays.asList("F", "x", "X", "k", "K", "g", "G", "N", "c", "C", "j", "J", "Y", 
	            "w", "W", "q", "L", "Q", "|", "R", "t", "T", "d", "D", "n", "p", "P", "b", "B", "m", 
	            "y", "r", "l", "v", "S", "z", "s", "h", "H", "Z", "V"));
	    // consonants1
        put(2, Arrays.asList("a", "A", "i", "I", "u", "U", "f", "F", "x", "X", "e", "E", "o", "O", 
                "k", "K", "g", "G", "N", "c", "C", "j", "J", "Y", "w", "W", "q", "L", "Q", "|", "R", 
                "t", "T", "d", "D", "n", "p", "P", "b", "B", "m", "H", "Z", "V"));
	    // consonants2
	    put(3, Arrays.asList("a", "A", "i", "I", "u", "U", "f", "F", "x", "X", "e", "E", "o", "O", 
	            "N", "Y", "L", "|", "R", "y", "r", "l", "v", "S", "z", "s", "h", "H", "Z", "V"));
	    // cC_words
	    put(4, Arrays.asList("a", "A", "i", "I", "u", "U", "f", "F", "x", "X", "e", "E", "o", "O", 
	            "k", "K", "g", "G", "N", "c", "j", "J", "Y", "w", "W", "q", "L", "Q", "|", "R", 
	            "t", "T", "d", "D", "n", "p", "P", "b", "B", "m", "y", "r", "l", "v", "S", "z", "s", 
	            "h", "H", "Z", "V"));
	    // consonants1_vowels
	    put(5, Arrays.asList("F", "x", "X", "k", "K", "g", "G", "N", "c", "C", "j", "J", "Y", 
	            "w", "W", "q", "L", "Q", "|", "R", "t", "T", "d", "D", "n", "p", "P", "b", "B", "m", 
	            "y", "r", "l", "v", "S", "z", "s", "h", "H", "Z", "V"));
	    // visarga1
	    put(6, Arrays.asList("F", "x", "X", "k", "K", "g", "G", "N", "c", "C", "j", "J", "Y", 
	            "w", "W", "q", "L", "Q", "|", "R", "t", "T", "d", "D", "n", "p", "P", "b", "B", "m", 
	            "H", "Z", "V"));
	    // visarga2
	    put(7, Arrays.asList("a", "A", "i", "I", "u", "U", "f", "F", "x", "X", "e", "E", "o", "O", 
	            "N", "Y", "L", "|", "R", "y", "r", "l", "v", "S", "z", "s", "h", "H", "Z", "V"));
	    // punar
	    put(8, Arrays.asList("I", "F", "x", "X", "L", "|", "H", "Z", "V"));
	    // all SLP
	    put(9, Arrays.asList("a", "A", "i", "I", "u", "U", "f", "F", "x", "X", "e", "E", "o", "O", 
	            "k", "K", "g", "G", "N", "c", "C", "j", "J", "Y", "w", "W", "q", "L", "Q", "|", "R", 
	            "t", "T", "d", "D", "n", "p", "P", "b", "B", "m", "y", "r", "l", "v", "S", "z", "s", 
	            "h", "H", "Z", "V", "M"));
	}};

	/**
	 * note: currently, parsing cmd is not done using indexes. this method might be slow.
	 * 
	 * This is how cmd is structured, with the names used in this method: (correct formatting in the code file)
	 * 
	 * <pre>
	 * {@code
	 *      <form>,<initial>:<initial>:<...>$<finalDiff>;<finalDiff>;<...>/<initialDiff>|<...>$<...>/<...>|
	 * [inflected],[cmd                                                                                  ]
	 *             [entry                                                              ]|[entry          ]
	 *             [initials               ]$[diffs                                    ]
	 *             [initial]:[initial]:[...]$[diffFinals                 ]/[diffInitial]
	 *                                       [diffFinal];[diffFinal];[...]
	 * For example:
	 *       DarmA,a                        $-A+an      ;-A+a             /-+a          |
	 *             A                        $-A+an      ;-A+a             /-+A          |
	 *                                      $-A+an      ;-A+a             /             |
	 *             c                        $-A+an      ;-A+a             /- c+c        |
	 *             C                        $-A+an      ;-A+a             /- C+C
	 *             
	 * 
	 * [diffFinal                                                      ]
	 * [-<numberOfcharsToDeleteFromInflected>+<charsToAddToGetTheLemma>]
	 * 
	 * [diffInitial                                   ]
	 * [-<initialCharsSandhied>+<initialCharsOriginal>]
	 * }
	 * </pre>
	 * 
	 * @param inflected  the inflected form (a substring of the input string)
	 * @param cmd to be parsed. contains the info for reconstructing lemmas 
	 * @return parsed structure 
	 */

	public TreeMap<String, TreeSet<DiffStruct>> parse(String inflected, String cmd) {
		// <initial>:<initial>:<...>$<finalDiff>;<finalDiff>;<...>/<initialDiff>|
		sandhis = new TreeMap<String, TreeSet<DiffStruct>>(new CommonHelpers.LengthComp());
		
		String[] fullEntries = cmd.split("\\|");								// <fullEntry>|<fullEntry>|<...>
		for (String fullEntry: fullEntries) {
			splitFullEntry(fullEntry, t);										// <entry>=<sandhiType>
			
			if (thereAreModifications()) {
				splitEntryAndInitials();										// <initial>:<initial>:<...> $ <diffs>
				splitDiffs();													// <diffFinals>/<diffInitial>				
				
				String sandhiedFinal = findSandhiedFinals(inflected, sandhiType);
				toDelete = null;
				initialCharsSandhied = "";
				
				toAdd = "";				
				initialCharsOriginal = "";
				
				if (onlyInitialsChange()) {
					splitDiffInitial();											// -<sandhiedInitial>+<unsandhiedInitial>
					
					String sandhied = sandhiedFinal+initialCharsSandhied;
					DiffStruct df = new DiffStruct(0, toAdd, initialCharsOriginal, sandhiType, pos, idempotentGroup);
					addEntry(sandhied, df);
					
				} else if (onlyFinalsChange()) {
					for (String diffFinal: diffFinals) {
						diffFinal = trimDiff(diffFinal);				
						splitDiffFinal(diffFinal);								// -<toDelete>+<toAdd>
						
						if (thereAreInitials()) {
							for (String initial: initials) {
								final DiffStruct df = new DiffStruct(toDelete, toAdd, initial, sandhiType, pos, idempotentGroup);
								addEntry(sandhiedFinal+initial, df);
							}
						} else {
							final DiffStruct df = new DiffStruct(toDelete, toAdd, null, sandhiType, pos, idempotentGroup);
							addEntry(sandhiedFinal, df);
						}
					}
				} else {	// both initials and finals change
					for (String diffFinal: diffFinals) {
						diffFinal = trimDiff(diffFinal);
						splitDiffFinal(diffFinal);								// -<toDelete>+<toAdd>
						splitDiffInitial();										// -<sandhiedInitial>+<unsandhiedInitial>

						final DiffStruct df = new DiffStruct(toDelete, toAdd, initialCharsOriginal, sandhiType, pos, idempotentGroup);
						addEntry(sandhiedFinal+initialCharsSandhied, df);
					}
				}
			} else if (thereAreNoModifications(fullEntry)) {
		        final String sandhiedFinal = inflected.substring(inflected.length()-1);
		        final DiffStruct df = new DiffStruct(0, null, null, sandhiType, pos, 0);
		        addEntry(sandhiedFinal, df);
			} else {
				throw new IllegalArgumentException("There is a problem with cmd: "+cmd);
			}
		}
		return sandhis;
	} 
	
    public HashMap<String, String> getIdemSandhied(String inflected, Integer group) {
        HashMap<String, String> sandhied = new HashMap<String, String>();
        String sandhiedFinal = findSandhiedFinals(inflected, 10);
        if (group == 9) {
            for (String initial: idempotentInitials.get(group)) {
                if (sandhiedFinal.length() == 1) {
                    sandhied.put(sandhiedFinal + initial, initial);
                } else {
                    sandhied.put(sandhiedFinal + initial, sandhiedFinal);
                }                    
            }
        } else {
            for (String initial: idempotentInitials.get(group)) {
                sandhied.put(sandhiedFinal + initial, sandhiedFinal);
            }
        }
        return sandhied;
    }
	
	private String findSandhiedFinals(String inflected, int sandhiType) {
	 // if consonants1_vowels, visarga1 or visarga2
	    if (sandhiType == 3 || sandhiType == 5 || sandhiType == 6) {
			return inflected.substring(inflected.length()-2);
		} else if (sandhiType == 9) {
			return inflected;
		} else {
			return inflected.substring(inflected.length()-1);
		}
	}

	private void splitFullEntry(String fullEntry, String[] t) {
		t = fullEntry.split("=");
		entry = t[0];
		t = t[1].split("#");
		pos = Integer.parseInt(t[1]);
		t = t[0].split("£");
		sandhiType = Integer.parseInt(t[0]);
		if (t.length == 2) idempotentGroup  = Integer.parseInt(t[1]);
	}
	
	private void splitEntryAndInitials() {
		t = entry.split("\\$");
		if (t[0].contains(":")) {
			initials = t[0].split("\\:");
		} else if (!t[0].equals("")) {
			initials = new String[1];
			initials[0] = t[0];
		} else {
			initials = new String[0];
		}
	}

	private void splitDiffs() {
		t = t[1].split("/");
		if (t[0].contains(";")) {
			diffFinals = t[0].split(";");
		} else if (!t[0].equals("")) {
			diffFinals = new String[1];
			diffFinals[0] = trimDiff(t[0]); 
		} else {
			diffFinals = new String[0];
		}
		if (t.length <= 1) {
			diffInitial = "";
		} else if (!t[1].equals("- +") && !t[1].equals("-+")) { // filters unchanged initial diffs
			diffInitial = trimDiff(t[1]); 
		} else {
			diffInitial = "";
		}
	}
	
	private void splitDiffInitial() {
		t = diffInitial.split("\\+"); 
		initialCharsSandhied = t[0];
		initialCharsOriginal = t[1];
	}
	
	private void splitDiffFinal(String finalDiff) {
		t = finalDiff.split("\\+");
		toDelete = Integer.parseInt(t[0]);
		if (t.length == 2) {
			toAdd = t[1];
		} else {
			toAdd = "";
		}
	}
	
	private String trimDiff(String diff) {
		return diff.replaceFirst("\\-", "").trim();  // remove "-" and extra space
	}
	
	private void addEntry(String sandhied, DiffStruct diff) {
		sandhis.putIfAbsent(sandhied, new TreeSet<DiffStruct>());
		sandhis.get(sandhied).add(diff);
	}
	
	final private boolean thereAreModifications() {
		// there is no change || no change and a space is added
		return !entry.equals("$/") && !entry.contains("$/- +");
	}
	
	final private boolean thereAreNoModifications(String fullEntry) {
		return fullEntry.contains("$/");
	}
	
	final private boolean onlyInitialsChange() {
		return diffFinals.length == 0 && !diffInitial.equals("");
	}

	final private boolean onlyFinalsChange() {
		return diffFinals.length > 0 && diffInitial.equals("");
	}
	
	final private boolean thereAreInitials() {
		return initials.length > 0;
	}
	
	public static class DiffStruct implements Comparable<DiffStruct> {
	    Integer nbToDelete;
	    String toAdd;
	    String initial;
	    Integer sandhiType;
	    Integer pos;
	    Integer idempotentGroup;
	    
	    public DiffStruct(int nbToDelete, String toAdd, String initial, int sandhiType, int pos, int idempotentGroup) {
	        this.nbToDelete = nbToDelete;
	        this.toAdd = (toAdd != null) ? toAdd: "";
	        this.initial = (initial != null) ? initial: "";
	        this.sandhiType = sandhiType;
	        this.pos = pos;
	        this.idempotentGroup = idempotentGroup;
	    }
	    
	    public String toString() {
	        return String.format("%s+%s/%s=%s£%s#%s", nbToDelete, toAdd, initial, sandhiType, idempotentGroup, pos);
	    }

        @Override
        public int compareTo(DiffStruct arg0) {
            int c = nbToDelete.compareTo(arg0.nbToDelete);
            if (c != 0)
                return c;
            c = toAdd.compareTo(arg0.toAdd);
            if (c != 0)
                return c;
            c = initial.compareTo(arg0.initial);
            if (c != 0)
                return c;
            c = sandhiType.compareTo(arg0.sandhiType);
            if (c != 0)
                return c;
            c = idempotentGroup.compareTo(arg0.idempotentGroup);
            if (c != 0)
                return c;
            return pos.compareTo(arg0.pos);
        }
	}
}
