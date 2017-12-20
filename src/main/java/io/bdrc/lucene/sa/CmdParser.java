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

import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeMap;

/** 
 * Parses cmds from the total Trie {@code total_output.txt} and reconstructs
 * the sandhied substring found between the two words undergoing sandhi.
 * <p> 
 * Is used by {@link SkrtWordTokenizer#reconstructLemmas()} 
 * 
 * @author Hélios Hildt
 * 
 */
public class CmdParser {
	private String[] t = new String[2];  // temporary variable constantly reused
	
	private int sandhiType = -1;
	private String entry = null;
	
	private String[] initials = null;
	
	private String diffInitial = null;   // there can only be one initial diff
	private String[] diffFinals = null;
	
	// for sandhied (HashMap key)
	private String toDelete = null;
	private String initialCharsSandhied = null;
	
	// for unsandhied (HashMap value)
	private String initialCharsOriginal = null;
	private String toAdd = null;
	
	private TreeMap<String, HashSet<String>> sandhis = null;

	/**
	 * note: currently, parsing cmd is not done using indexes. this method might be slow.
	 * 
	 * This is how cmd is structured, with the names used in this method: (correct formatting in the code file)
	 * 
	 *             
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
	 * 
	 * @param cmd to be parsed. contains the info for reconstructing lemmas 
	 * @return: parsed structure 
	 */
	public TreeMap<String, HashSet<String>> parse(String inflected, String cmd) { // TODO: create a class to parse the cmds
		// <initial>:<initial>:<...>$<finalDiff>;<finalDiff>;<...>/<initialDiff>|
		sandhis = new TreeMap<String, HashSet<String>>(new LengthComp());
		
		String[] fullEntries = cmd.split("\\|");								// <fullEntry>|<fullEntry>|<...>
		for (String fullEntry: fullEntries) {
			splitFullEntry(fullEntry, t);										// <entry>=<sandhiType>
			
			if (thereAreModifications()) {
				splitEntryAndInitials();										// <initial>:<initial>:<...> $ <diffs>
				splitDiffs();													// <diffFinals>/<diffInitial>				
				
				String sandhiedFinal = findSandhiedFinals(inflected, sandhiType);
				toDelete = "";
				initialCharsSandhied = "";
				
				toAdd = "";				
				initialCharsOriginal = "";
				
				if (onlyInitialsChange()) {
					splitDiffInitial();											// -<sandhiedInitial>+<unsandhiedInitial>
					
					String sandhied = sandhiedFinal+initialCharsSandhied;
					String unsandhied = String.format("0+/%s=%s", initialCharsOriginal, sandhiType);
					addEntry(sandhied, unsandhied);
					
				} else if (onlyFinalsChange()) {
					for (String diffFinal: diffFinals) {
						diffFinal = trimDiff(diffFinal);				
						splitDiffFinal(diffFinal);								// -<toDelete>+<toAdd>
						
						if (thereAreInitials()) {
							for (String initial: initials) {
								String sandhied = sandhiedFinal+initial;
								String unsandhied = String.format("%s+%s/%s=%s", toDelete, toAdd, initial, sandhiType);
								addEntry(sandhied, unsandhied);
							}
						} else {
							String sandhied = sandhiedFinal;
							String unsandhied = String.format("%s+%s=%s", toDelete, toAdd, sandhiType);
							addEntry(sandhied, unsandhied);
						}
					}
				} else {	// both initials and finals change
					for (String diffFinal: diffFinals) {
						diffFinal = trimDiff(diffFinal);
						splitDiffFinal(diffFinal);								// -<toDelete>+<toAdd>
						splitDiffInitial();										// -<sandhiedInitial>+<unsandhiedInitial>

						String sandhied = sandhiedFinal+initialCharsSandhied;
						String unsandhied = String.format("%s+%s/%s=%s", toDelete, toAdd, initialCharsOriginal, sandhiType);
						addEntry(sandhied, unsandhied);
					}
				}
			} else if (thereAreNoModifications(fullEntry)) {
				// pass
			} else {
				throw new IllegalArgumentException("There is a problem with cmd: "+cmd);
			}
		}
		return sandhis;
	}

	private String findSandhiedFinals(String inflected, int sandhiType) {
		if (sandhiType == 3 || sandhiType == 5 || sandhiType == 6) {
		// if consonants1_vowels, visarga1 or visarga2
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
		sandhiType = Integer.parseInt(t[1]);
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
		toDelete = t[0];
		if (t.length == 2) {
			toAdd = t[1];
		} else {
			toAdd = "";
		}
	}
	
	private String trimDiff(String diff) {
		return diff.replaceFirst("\\-", "").trim();  // remove "-" and extra space
	}
	
	private void addEntry(String sandhied, String unsandhied) {
		sandhis.putIfAbsent(sandhied, new HashSet<String>());
		sandhis.get(sandhied).add(unsandhied);
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
}

class LengthComp implements Comparator<String> {
    @Override
    public int compare(String s1, String s2) {
        final int lenComp = s2.length() - s1.length();
        if (lenComp != 0) {
            return lenComp;
        }
        return s1.compareTo(s2);
    }
}

