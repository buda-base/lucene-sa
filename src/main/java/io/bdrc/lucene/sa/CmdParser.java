package io.bdrc.lucene.sa;

import java.util.HashMap;

public class CmdParser {
	public static HashMap<String, String[]> parse(String sandhiedFinal, String cmd) { // TODO: create a class to parse the cmds
		/**
		 * note: currently, parsing cmd is not done using indexes. this method might be slow.
		 * 
		 * This is how cmd is structured, with the names used in this method:
		 * 
		 *      <form>,<initial>:<initial>:<...>~<finalDiff>;<finalDiff>;<...>/<initialDiff>|<...>~<...>/<...>|
		 * [inflected],[cmd                                                                                  ]
		 *             [entry                                                              ]|[entry          ]
		 *             [initials               ]~[diffs                                    ]
		 *             [initial]:[initial]:[...]~[diffFinals                 ]/[diffInitial]
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
		// <initial>:<initial>:<...>~<finalDiff>;<finalDiff>;<...>/<initialDiff>|
		HashMap<String, String[]> sandhis = new HashMap<String, String[]>();
		// variables
		String[] initials = null;
		String[] diffFinals = null;
		String diffInitial = null; // there can only be one initial diff
		String[] t; // temporary variable
		String[] entries = cmd.split("\\|"); // split into entries
		for (String entry: entries) {
			
			// 0. populate variables for the current entry
			if (entry.equals("~/")) {
				// pass because there is no change, so no sandhi
			} else {
				// initials ~ diffs
				t = entry.split("~");
				assert(t.length == 2);
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
						diffFinals[0] = t[0];
					} else {
						diffFinals = new String[0];
					}
					diffInitial = t[1];
				}
			}
			assert(initials != null);
			assert(diffFinals != null);
			assert(diffInitial != null);
			
			// 1. generate diffInitials[]
			
			
			// 2. reconstruct sandhied + unsandhied pairs that are possible with sandhiedFinal
			
			
			
		}
		return sandhis;
	}
}
