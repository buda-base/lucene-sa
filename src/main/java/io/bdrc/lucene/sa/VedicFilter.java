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
 * Filters the Unicode Vedic Extensions
 * By default, all the characters are mapped to an empty string,
 * assuming those who use this filter will know how to do the mappings
 * 
 * @author Hélios Hildt
 * @author Élie Roux
 *
 */
public class VedicFilter extends MappingCharFilter {

    public VedicFilter(Reader in) {
        super(getSkrtNormalizeCharMap(), in);
    }

    public final static NormalizeCharMap getSkrtNormalizeCharMap() {
        
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
		// Marks of nasalization
		builder.add("\u1CE9", ""); // ᳩ VEDIC SIGN ANUSVARA ANTARGOMUKHA
		builder.add("\u1CEA", ""); // ᳪ VEDIC SIGN ANUSVARA BAHIRGOMUKHA
		builder.add("\u1CEB", ""); // ᳫ VEDIC SIGN ANUSVARA VAMAGOMUKHA
		builder.add("\u1CEC", ""); // ᳬ VEDIC SIGN ANUSVARA VAMAGOMUKHA WITH TAIL
		builder.add("\u1CED", ""); // ᳭ VEDIC SIGN TIRYAK
		builder.add("\u1CEE", ""); // ᳮ VEDIC SIGN HEXIFORM LONG ANUSVARA
		builder.add("\u1CEF", ""); // ᳯ VEDIC SIGN LONG ANUSVARA
		builder.add("\u1CF0", ""); // ᳰ VEDIC SIGN RTHANG LONG ANUSVARA
		builder.add("\u1CF1", ""); // ᳱ VEDIC SIGN ANUSVARA UBHAYATO MUKHA
		// Ardhavisarga (Ardhavisarga denotes the sounds jihvamuliya and upadhmaniya (velar and bilabial voiceless fricatives) in Sanskrit. Its use is not limited to Vedic.
		builder.add("\u1CF2", ""); // ᳲ VEDIC SIGN ARDHAVISARGA
		builder.add("\u1CF3", ""); // ᳳ VEDIC SIGN ROTATED ARDHAVISARGA
		// Signs
		builder.add("\u1CF5", ""); // ᳵ VEDIC SIGN JIHVAMULIYA
		// unvoiced velar stops
		builder.add("\u1CF6", ""); // ᳶ VEDIC SIGN UPADHMANIYA
		// unvoiced labial stops
		builder.add("\u1CF7", ""); //  VEDIC SIGN ATIKRAMA
		
		// Tone marks for the Samaveda
		builder.add("\u1CE5", ""); // ᳥  VEDIC SIGN VISARGA ANUDATTA
		builder.add("\u1CE6", ""); // ᳦ VEDIC SIGN REVERSED VISARGA ANUDATTA
		builder.add("\u1CE7", ""); // ᳧ VEDIC SIGN VISARGA UDATTA WITH TAIL
		builder.add("\u1CE8", ""); // ᳨ VEDIC SIGN VISARGA ANUDATTA WITH TAIL
		// Sign for Yajurvedic
		builder.add("\u1CF4", ""); // ᳴ VEDIC TONE CANDRA ABOVE
		// Signs for Jaiminiya Sama Veda
		builder.add("\u1CF8", ""); // ᳸ VEDIC TONE RING ABOVE
		builder.add("\u1CF9", ""); // ᳹ VEDIC TONE DOUBLE RING ABOVE
		// Tone marks for the Samaveda
		builder.add("\u1CD0", ""); // ᳐ VEDIC TONE KARSHANA
		builder.add("\u1CD1", ""); // ᳑ VEDIC TONE SHARA
		builder.add("\u1CD2", ""); // ᳒ VEDIC TONE PRENKHA
		// Breathing mark for the Samaveda
		builder.add("\u1CD3", ""); // ᳓ VEDIC SIGN NIHSHVASA
		// Signs for Yajurvedic
		builder.add("\u1CD4", ""); // ᳔ VEDIC SIGN YAJURVEDIC MIDLINE SVARITA
		builder.add("\u1CD5", ""); // ᳕ VEDIC TONE YAJURVEDIC AGGRAVATED INDEPENDENT SVARITA
		builder.add("\u1CD6", ""); // ᳖ VEDIC TONE YAJURVEDIC INDEPENDENT SVARITA
		builder.add("\u1CD7", ""); // ᳗ VEDIC TONE YAJURVEDIC KATHAKA INDEPENDENT SVARITA
		builder.add("\u1CD8", ""); // ᳘ VEDIC TONE CANDRA BELOW
		builder.add("\u1CD9", ""); // ᳙ VEDIC TONE YAJURVEDIC KATHAKA INDEPENDENT SVARITA SCHROEDER
		builder.add("\u1CDA", ""); // ᳚ VEDIC TONE DOUBLE SVARITA
		builder.add("\u1CDB", ""); // ᳛ VEDIC TONE TRIPLE SVARITA
		builder.add("\u1CDC", ""); // ᳜ VEDIC TONE KATHAKA ANUDATTA
		builder.add("\u1CDD", ""); // ᳝ VEDIC TONE DOT BELOW
		// Tone marks for the Satapathabrahmana
		builder.add("\u1CDE", ""); // ᳞ VEDIC TONE TWO DOTS BELOW
		builder.add("\u1CDF", ""); // ᳟ VEDIC TONE THREE DOTS BELOW
		// Tone mark for the Rigveda
		builder.add("\u1CE0", ""); // ᳠ VEDIC TONE RIGVEDIC KASHMIRI INDEPENDENT SVARITA
		// Tone mark for the Atharvaveda
		builder.add("\u1CE1", ""); // ᳡ VEDIC TONE ATHARVAVEDIC INDEPENDENT SVARITA
		// Diacritics for visarga
		builder.add("\u1CE2", ""); // ᳢ VEDIC SIGN VISARGA SVARITA
		builder.add("\u1CE3", ""); // ᳣ VEDIC SIGN VISARGA UDATTA
		builder.add("\u1CE4", ""); // ᳤ VEDIC SIGN REVERSED VISARGA UDATTA

        return builder.build();
    }

}
