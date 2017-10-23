package io.bdrc.lucene.sa;

import java.util.HashMap;
import java.util.HashSet;

public class CmdParser {
	public static HashMap<String, HashSet<String>> parse(String inflected, String cmd) { // TODO: create a class to parse the cmds
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
		HashMap<String, HashSet<String>> sandhis = new HashMap<String, HashSet<String>>();
		// variables
		String[] initials = null;
		String diffInitial = null; // there can only be one initial diff
		String[] diffFinals = null;
		int sandhiType = -1; 

		String[] t; // temporary variable
		String entry = null;
		
		String[] entries = cmd.split("\\|"); // split into entries
		for (String fullEntry: entries) {
			t = fullEntry.split("=");
			assert(t.length == 2); // there should always be a sandhi type for an entry
			sandhiType = Integer.parseInt(t[1]);
			
			entry = t[0];
			if (entry.equals("$/") || entry.contains("$/- +")) {
				// filters all non-modifying sandhis: either there is no change or words are separated by a space.
			} else {
				// 0. populate variables for the current entry
				
				// initials $ diffs
				t = entry.split("\\$");
				if (t[0].contains(":")) {
					initials = t[0].split("\\:");
				} else if (!t[0].equals("")) {
					initials = new String[1];
					initials[0] = t[0];
				} else {
					initials = new String[0];
				}
				
				// diffFinals / diffInitial
				if (t[1].equals("/")) {
					diffFinals = new String[0];
					diffInitial = "";					
				} else {
					t = t[1].split("/");
					if (t[0].contains(";")) {
						diffFinals = t[0].split(";");
					} else if (!t[0].equals("")) {
						diffFinals = new String[1];
						diffFinals[0] = t[0].replaceFirst("\\-", "").trim(); // left-strip minus sign. uses regex 
					} else {
						diffFinals = new String[0];
					}
					if (t.length <= 1) {
						diffInitial = "";
					} else if (!t[1].equals("- +") && !t[1].equals("-+")) { // filters unchanged initial diffs
						diffInitial = t[1].replaceFirst("\\-", "").trim(); // left-strip minus sign. uses regex
					} else {
						diffInitial = "";
					}
				}
				
				// delete non-changing initial diff
				
				
				// 1. reconstruct sandhied + unsandhied pairs that are possible with the value of sandhiedFinal
				
				// for sandhied (HashMap key)
				String sandhiedFinal = findSandhiedFinals(inflected, sandhiType);
				String toDelete = "";
				String initialCharsSandhied = ""; // remember to trim space used by resources/sansksrit-stemming-data/sandhify/sandhifier.py
				// for unsandhied (HashMap value)
				String toAdd = "";				
				String initialCharsOriginal = "";
				
				if (diffFinals.length == 0 && !diffInitial.equals("")) {
				// a. diff only on initial
					t = diffInitial.split("\\+");
					initialCharsSandhied = t[0].trim();
					initialCharsOriginal = t[1];
					
					String sandhied = sandhiedFinal+initialCharsSandhied;
					//String unsandhied = String.format("%s+%s/%s=%s", "0", sandhiedFinal, initialCharsOriginal, sandhiType);
					String unsandhied = String.format("0+/%s=%s", initialCharsOriginal, sandhiType);
					sandhis.putIfAbsent(sandhied, new HashSet<String>());
					sandhis.get(sandhied).add(unsandhied);
				} else if (diffFinals.length > 0 && diffInitial.equals("")) { 
				// b. diff only on final
					for (String finalDiff: diffFinals) {
						finalDiff = finalDiff.replaceFirst("\\-", ""); // left-strip minus sign. uses regex
						t = finalDiff.split("\\+");
						toDelete = t[0];
						if (t.length == 2) {
							toAdd = t[1];
						} else {
							toAdd = "";
						}
						
						
						if (initials.length > 0) {
							for (String initial: initials) {
								String sandhied = sandhiedFinal+initial;
								String unsandhied = String.format("%s+%s/%s=%s", toDelete, toAdd, initial, sandhiType);
								sandhis.putIfAbsent(sandhied, new HashSet<String>());
								sandhis.get(sandhied).add(unsandhied);
							}
						} else {
							String sandhied = sandhiedFinal;
							String unsandhied = String.format("%s+%s=%s", toDelete, toAdd, sandhiType);
							sandhis.putIfAbsent(sandhied, new HashSet<String>());
							sandhis.get(sandhied).add(unsandhied);
						}
					}
				} else if (diffFinals.length > 0 && !diffInitial.equals("")) { 
				// c. diff on both final and initial
					for (String finalDiff: diffFinals) {
						finalDiff = finalDiff.replaceFirst("\\-", ""); // left-strip minus sign. uses regex
						if (finalDiff.contains("+") && diffInitial.contains("+")) { // diff on both final and initial
							t = finalDiff.split("\\+");
							toDelete = t[0];
							if (t.length == 2) {
								toAdd = t[1];
							} else {
								toAdd = "";
							}
							
							t = diffInitial.split("\\+");
							if (t.length == 2) {
								initialCharsSandhied = t[0];
								initialCharsOriginal = t[1].trim();
								
								String key = sandhiedFinal+initialCharsSandhied;
								String value = String.format("%s+%s/%s=%s", toDelete, toAdd, initialCharsOriginal, sandhiType);
								sandhis.putIfAbsent(key, new HashSet<String>());
								sandhis.get(key).add(value);
							} else if (t.length < 2 && initials.length > 1) {
								for (String initial: initials) {
									String key = sandhiedFinal+initial;
									String value = String.format("%s+%s/%s=%s", toDelete, toAdd, initial, sandhiType);
									sandhis.putIfAbsent(key, new HashSet<String>());
									sandhis.get(key).add(value);
								}
							} else {
								throw new IllegalArgumentException("A.there is a problem with cmd: "+cmd);
							}
						}
					}
				} else {
					throw new IllegalArgumentException("A.there is a problem with cmd: "+cmd);	
				}
			}
		}
		return sandhis;
	}

	private static String findSandhiedFinals(String inflected, int sandhiType) {
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
