package io.bdrc.lucene.sa;

import java.util.HashMap;
import java.util.HashSet;

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
	private HashMap<String, HashSet<String>> sandhis = null;

	public HashMap<String, HashSet<String>> parse(String inflected, String cmd) { // TODO: create a class to parse the cmds
		/**
		 * note: currently, parsing cmd is not done using indexes. this method might be slow.
		 * 
		 * This is how cmd is structured, with the names used in this method:
		 * 
		 *       DarmA,a                        $-A+an      ;-A+a             /-+a          |
		 *             A                        $-A+an      ;-A+a             /-+A          |
		 *                                      $-A+an      ;-A+a             /             |
		 *             c                        $-A+an      ;-A+a             /- c+c        |
		 *             C                        $-A+an      ;-A+a             /- C+C
		 *             
		 *      <form>,<initial>:<initial>:<...>$<finalDiff>;<finalDiff>;<...>/<initialDiff>|<...>$<...>/<...>|
		 * [inflected],[cmd                                                                                  ]
		 *             [entry                                                              ]|[entry          ]
		 *             [initials               ]$[diffs                                    ]
		 *             [initial]:[initial]:[...]$[diffFinals                 ]/[diffInitial]
		 *                                       [diffFinal];[diffFinal];[...]
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
		// <initial>:<initial>:<...>$<finalDiff>;<finalDiff>;<...>/<initialDiff>|
		sandhis = new HashMap<String, HashSet<String>>();
		
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
						splitFinalDiff(diffFinal);								// -<toDelete>+<toAdd>
						
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
						splitFinalDiff(diffFinal);								// -<toDelete>+<toAdd>
						splitDiffInitial();										// -<sandhiedInitial>+<unsandhiedInitial>

						String sandhied = sandhiedFinal+initialCharsSandhied;
						String unsandhied = String.format("%s+%s/%s=%s", toDelete, toAdd, initialCharsOriginal, sandhiType);
						addEntry(sandhied, unsandhied);
					}
				}
			} else if (isNonModifying(fullEntry)) {
				// pass
			} else {
				throw new IllegalArgumentException("There is a problem with cmd: "+cmd);
			}
		}
		return sandhis;
	}

	private void splitFinalDiff(String finalDiff) {
		t = finalDiff.split("\\+");
		toDelete = t[0];
		if (t.length == 2) {
			toAdd = t[1];
		} else {
			toAdd = "";
		}
	}

	private void addEntry(String sandhied, String unsandhied) {
		sandhis.putIfAbsent(sandhied, new HashSet<String>());
		sandhis.get(sandhied).add(unsandhied);
	}

	private void splitDiffInitial() {
		t = diffInitial.split("\\+"); 
		initialCharsSandhied = t[0];
		initialCharsOriginal = t[1];
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

	private String trimDiff(String diff) {
		return diff.replaceFirst("\\-", "").trim();  // remove "-" and extra space
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

	final private boolean thereAreModifications() {
		// there is no change || no change and a space is added
		return !entry.equals("$/") && !entry.contains("$/- +");
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
	
	final private boolean isNonModifying(String fullEntry) {
		return fullEntry.contains("$/");
	}
	
	private void splitFullEntry(String fullEntry, String[] t) {
		t = fullEntry.split("=");
		assert(t.length == 2); // there should always be a sandhi type for an entry
		entry = t[0];
		sandhiType = Integer.parseInt(t[1]);
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
}
