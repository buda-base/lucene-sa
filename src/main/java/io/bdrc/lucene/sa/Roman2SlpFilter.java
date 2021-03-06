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

import java.io.Reader;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

/**
 * Roman → SLP1 charfilter 
 * 
 * Following the naming convention used by Peter Scharf, we use "Roman" instead of "IAST" to show that, 
 * on top of supporting the full IAST character set,
 * we support the extra distinctions within devanagari found in ISO 15919
 * A list of non-Sanskrit and non-Devanagari characters (see below) are deleted. 
 * 
 * see @see <a href="https://en.wikipedia.org/wiki/ISO_15919#Comparison_with_UNRSGN_and_IAST">Comparison_with_UNRSGN_and_IAST</a> 
 * and the Overview section of the same page.
 * 
 * @author Hélios Hildt
 * @author Élie Roux
 *
 */

public class Roman2SlpFilter extends MappingCharFilter {
    public final static NormalizeCharMap map = getSkrtNormalizeCharMap();
    
    public Roman2SlpFilter(Reader in) {
        super(map, in);
    }

    public final static NormalizeCharMap getSkrtNormalizeCharMap() {
    	// This list is based on "roman_slp1.xml" found in "http://sanskritlibrary.org/software/transcodeFile.zip" and on the table in the link above
    	// the square brackets and the curly brackets in the <out> tag were removed as they have no equivalent in SLP
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        
        // Anusvara (including ISO 15919)
        builder.add("\u1e41", "M"); // ṁ
        builder.add("m\u0307", "M"); // NFD ṁ
        builder.add("\u1e40", "M"); // Ṁ
        builder.add("M\u0307", "M"); // NFD Ṁ
        builder.add("m\u0310", "~"); // m̐
        builder.add("M\u0310", "~"); // M̐
        builder.add("\u1e43", "M"); // ṃ
        builder.add("m\u0323", "M"); // NFD ṃ 
        builder.add("\u1e42", "M"); // Ṃ
        builder.add("M\u0323", "M"); // NFD Ṃ

        // Plain normalizations
        builder.add("ē", "e"); // simply normalizes to o, since Sanskrit doesn't distinguish between long and short e vowel
        builder.add("ō", "o"); // same as above for the o vowel
        builder.add("ṟ", "r"); // ऱ not in IAST, yet in the deva unicode table
        builder.add("r̆", "r"); // ऱ् same as above
        builder.add("ṉ", "n"); // ऩ same as above
        builder.add("ẏ", "y"); // य़ same as above
        
        // danda 
        builder.add("|", ".");
        builder.add("‖", "..");
        
        // avagraha normalisation (covers a maximum of possibilities. maybe too much?)
        builder.add("’", "'"); // U+2019 RIGHT SINGLE QUOTATION MARK
        builder.add("＇", "'"); // U+FF07 FULLWIDTH APOSTROPHE
        builder.add("ʼ", "'"); // U+02BC MODIFIER LETTER APOSTROPHE
        builder.add("´", "'"); // U+00B4 ACUTE ACCENT 
        builder.add("ˊ", "'"); // U+02CA MODIFIER LETTER ACUTE ACCENT
        builder.add("′", "'"); // U+2032 PRIME
        builder.add("ʹ", "'"); // U+02B9 MODIFIER LETTER PRIME
        builder.add("ʹ", "'"); // U+0374  GREEK NUMERAL SIGN
        
        // Ignored ISO 15919 characters with devanagari:
        // ô (ऑ, transcribes English borrowings), ẖ and ḫ (ᳵ and ᳶ, specific to Vedic), k͟h and ġ (ख़ and ग़, specific to Persian),
        // Ignored ISO 15919 characters with no devanagari equivalent:
        // æ, ḵ, ǣ, ŭ, n̆, n̆g, ĉ, n̆j, n̆ḍ, n̆d, m̆b, ṯ, ş, đ, ḑ, ẓ, ţ
        // ṛh (ढ़) is not included below, so it will be processed as the concatenation of two characters
        builder.add("ô", "");
        builder.add("ẖ", "");
        builder.add("ḫ", "");
        builder.add("k͟h", "");
        builder.add("ġ", "");
        builder.add("æ", "");
        builder.add("ḵ", "");
        builder.add("ǣ", "");
        builder.add("ŭ", "");
        builder.add("n̆", "");
        builder.add("n̆g", "");
        builder.add("ĉ", "");
        builder.add("n̆j", "");
        builder.add("n̆ḍ", "");
        builder.add("n̆d", "");
        builder.add("m̆b", "");
        builder.add("ṯ", "");
        builder.add("ş", "");
        builder.add("đ", "");
        builder.add("ḑ", "");
        builder.add("ẓ", "");
        builder.add("ţ", "");  
        
        // IAST characters and combinations
        builder.add("A", "a");
        builder.add("\u0101", "A"); // ā
        builder.add("a\u0304", "A"); // NFD ā 
        builder.add("\u0100", "A"); // Ā
        builder.add("A\u0304", "A"); // NFD Ā 
        builder.add("I", "i");
        builder.add("\u012b", "I"); // ī
        builder.add("i\u0304", "I"); // NFD ī 
        builder.add("\u012a", "I"); // Ī
        builder.add("I\u0304", "I"); // NFD Ī
        builder.add("U", "u");
        builder.add("\u016b", "U"); // ū
        builder.add("u\u0304", "U"); // NFD ū
        builder.add("\u016a", "U"); // Ū
        builder.add("U\u0304", "U"); // NFD Ū 
        builder.add("R", "r");
        builder.add("r\u0304", "r"); // r̄ (NFD only)
        builder.add("R\u0304", "r"); // R̄ (NFD only)
        builder.add("\u1e5b", "f"); // ṛ
        builder.add("r\u0323", "f"); // NFD ṛ
        builder.add("\u1e5a", "f"); // Ṛ
        builder.add("R\u0323", "f"); // NFD Ṛ
        builder.add("\u1e5d", "F"); // ṝ
        builder.add("\u1e5b\u0304", "F"); // semi-NFD ṝ
        builder.add("r\u0323\u0304", "F"); // NFD ṝ
        builder.add("\u1e5c", "F"); // Ṝ
        builder.add("\u1e5a\u0304", "F"); // semi-NFD Ṝ
        builder.add("R\u0323\u0304", "F"); // NFD Ṝ
        builder.add("r\u0325", "f"); // NFD r̥
        builder.add("R\u0325", "f"); // NFD R̥
        builder.add("r\u0325\u0304", "F"); // r̥̄
        builder.add("R\u0325\u0304", "F"); // R̥̄
        builder.add("L", "l");
        builder.add("\u1e37", "x"); // ḷ
        builder.add("l\u0323", "x"); // NFD ḷ
        builder.add("\u1e36", "x"); // Ḷ
        builder.add("L\u0323", "x"); // NFD Ḷ
        builder.add("\u1e39", "X"); // ḹ
        builder.add("\u1e37\u0304", "X"); // semi-NFD ḹ
        builder.add("l\u0323\u0304", "X"); // NFD ḹ
        builder.add("\u1e38", "X"); // Ḹ
        builder.add("\u1e36\u0304", "X"); // semi-NFD Ḹ
        builder.add("L\u0323\u0304", "X"); // NFD Ḹ
        builder.add("au", "O");
        builder.add("Au", "O");
        builder.add("AU", "O");
        builder.add("F", "f");
        builder.add("X", "x");
        builder.add("ai", "E");
        builder.add("Ai", "E");
        builder.add("AI", "E");
        builder.add("E", "e");
        builder.add("O", "o");
        builder.add("M", "m");
        builder.add("H", "h");
        builder.add("\u1e25", "H"); // ḥ
        builder.add("h\u0323", "H"); // NFD ḥ 
        builder.add("\u1e24", "H"); // Ḥ
        builder.add("H\u0323", "H"); // NFD Ḥ
        builder.add("f", "H");	// added to cover special visargas found in old stone inscriptions
        builder.add("x", "H");	// idem
        builder.add("Z", "z");
        builder.add("V", "v");
        builder.add("K", "k");
        builder.add("kh", "K");
        builder.add("Kh", "K");
        builder.add("KH", "K");
        builder.add("G", "g");
        builder.add("gh", "G");
        builder.add("Gh", "G");
        builder.add("GH", "G");
        builder.add("N", "n");
        builder.add("\u1e45", "N"); // ṅ
        builder.add("n\u0307", "N"); // NFD ṅ
        builder.add("\u1e44", "N"); // Ṅ
        builder.add("N\u0307", "N"); // NFD Ṅ
        builder.add("C", "c");
        builder.add("ch", "C");
        builder.add("Ch", "C");
        builder.add("CH", "C");
        builder.add("J", "j");
        builder.add("jh", "J");
        builder.add("Jh", "J");
        builder.add("JH", "J");
        builder.add("\u00f1", "Y"); // ñ
        builder.add("n\u0303", "Y"); // NFD ñ
        builder.add("\u00d1", "Y"); // Ñ
        builder.add("N\u0303", "Y"); // NFD Ñ
        builder.add("Y", "y");
        builder.add("W", "w");
        builder.add("Q", "q");
        builder.add("\u1e6d", "w"); // ṭ
        builder.add("t\u0323", "w"); // NFD ṭ
        builder.add("\u1e6c", "w"); // Ṭ
        builder.add("T\u0323", "w"); // NFD Ṭ
        builder.add("\u1e6dh", "W"); // ṭh
        builder.add("t\u0323h", "W"); // NFD ṭh
        builder.add("\u1e6ch", "W"); // Ṭh
        builder.add("T\u0323h", "W"); // NFD Ṭh
        builder.add("\u1e6cH", "W"); // ṬH
        builder.add("T\u0323H", "W"); // NFD ṬH
        builder.add("\u1e0d", "q"); // ḍ
        builder.add("d\u0323", "q"); // NFD ḍ
        builder.add("\u1e0c", "q"); // Ḍ
        builder.add("D\u0323", "q"); // NFD Ḍ
        builder.add("\u1e0dh", "Q"); // ḍh
        builder.add("d\u0323h", "Q"); // NFD ḍh
        builder.add("\u1e0ch", "Q"); // Ḍh
        builder.add("D\u0323h", "Q"); // NFD Ḍh
        builder.add("\u1e0cH", "Q"); // ḌH
        builder.add("D\u0323H", "Q"); // NFD ḌH
        builder.add("\u1e47", "R"); // ṇ
        builder.add("n\u0323", "R"); // NFD ṇ
        builder.add("\u1e46", "R"); // Ṇ
        builder.add("N\u0323", "R"); // NFD Ṇ
        builder.add("th", "T"); 
        builder.add("Th", "T");
        builder.add("TH", "T"); 
        builder.add("T", "t");
        builder.add("D", "d");
        builder.add("dh", "D");
        builder.add("Dh", "D");
        builder.add("DH", "D");
        builder.add("P", "p");
        builder.add("ph", "P");
        builder.add("Ph", "P");
        builder.add("PH", "P");
        builder.add("B", "b");
        builder.add("bh", "B");
        builder.add("Bh", "B");
        builder.add("BH", "B");
        builder.add("S", "s");
        builder.add("\u015b", "S"); // ś
        builder.add("s\u0301", "S"); // NFD ś
        builder.add("\u015a", "S"); // Ś
        builder.add("S\u0301", "S"); // NFD Ś
        builder.add("\u1e63", "z"); // ṣ
        builder.add("s\u0323", "z"); // NFD ṣ
        builder.add("\u1e62", "z"); // Ṣ
        builder.add("S\u0323", "z"); // NFD Ṣ
        builder.add("\u1e3B", "L"); // ḻ
        builder.add("l\u0331", "L"); // NFD ḻ
        builder.add("\u1e3a", "L"); // Ḻ
        builder.add("L\u0331", "L"); // NFD Ḻ
        builder.add("\u1e3Bh", "|"); // ḻh
        builder.add("l\u0331h", "L"); // NFD ḻh
        builder.add("\u1e3Ah", "|"); // Ḻh
        builder.add("L\u0331h", "L"); // NFD Ḻh
        builder.add("\u1e3AH", "|"); // ḺH
        builder.add("L\u0331H", "L"); // NFD ḺH
        builder.add("\u0303", "~"); // ̃

        return builder.build();
    }

}
