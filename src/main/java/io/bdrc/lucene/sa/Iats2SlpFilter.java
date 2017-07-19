package io.bdrc.lucene.sa;

import java.io.Reader;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

/**
 * IATS -> SLP1 charfilter
 * @author drupchen
 *
 */

public class Iats2SlpFilter extends MappingCharFilter {

    public Iats2SlpFilter(Reader in) {
        super(getSkrtNormalizeCharMap(), in);
    }

    public final static NormalizeCharMap getSkrtNormalizeCharMap() {
        final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();

        builder.add("A", "a");
        builder.add("\u0101", "A"); //  // ā
        builder.add("\u0100", "A"); // Ā
        builder.add("I", "i");
        builder.add("\u012b", "I"); // ī
        builder.add("\u012a", "I"); // Ī
        builder.add("U", "u");
        builder.add("\u016b", "U"); // ū
        builder.add("\u016a", "U"); // Ū
        builder.add("R", "r");
        builder.add("\u1e5b", "f"); // ṛ
        builder.add("\u1e5b", "f"); // ṛ
        builder.add("\u1e5a", "f"); // Ṛ
        builder.add("\u1e5d", "F"); // ṝ
        builder.add("\u1e5c", "F"); // Ṝ
        builder.add("L", "r");
        builder.add("\u1e37", "x"); // ḷ
        builder.add("\u1e36", "x"); // Ḷ
        builder.add("\u1e39", "X"); // ḹ
        builder.add("\u1e38", "X"); // Ḹ
        builder.add("au", "O");
        builder.add("AU", "O");
        builder.add("F", "f");
        builder.add("X", "x");
        builder.add("ai", "E");
        builder.add("AI", "E");
        builder.add("E", "e");
        builder.add("O", "o");
        builder.add("M", "m");
        builder.add("\u1e43", "M"); // ṃ
        builder.add("\u1e42", "M"); // Ṃ
        builder.add("H", "h");
        builder.add("\u1e25", "H"); // ḥ
        builder.add("\u1e24", "H"); // Ḥ
        builder.add("Z", "z");
        builder.add("V", "v");
        builder.add("K", "k");
        builder.add("kh", "K");
        builder.add("KH", "K");
        builder.add("G", "g");
        builder.add("gh", "G");
        builder.add("GH", "G");
        builder.add("N", "n");
        builder.add("\u1e45", "N"); // ṅ
        builder.add("\u1e44", "N"); // Ṅ
        builder.add("C", "c");
        builder.add("ch", "C");
        builder.add("CH", "C");
        builder.add("J", "j");
        builder.add("jh", "J");
        builder.add("JH", "J");
        builder.add("\u00f1", "Y"); // ñ
        builder.add("\u00d1", "Y"); // Ñ
        builder.add("Y", "y");
        builder.add("W", "w");
        builder.add("Q", "q");
        builder.add("\u1e6d", "w"); // ṭ
        builder.add("\u1e6c", "w"); // Ṭ
        builder.add("\u1e6dh", "W"); // ṭh
        builder.add("\u1e6cH", "W"); // ṬH
        builder.add("\u1e0d", "q"); // ḍ
        builder.add("\u1e0c", "q"); // Ḍ
        builder.add("\u1e0dh", "Q"); // ḍh
        builder.add("\u1e0cH", "Q"); // ḌH
        builder.add("\u1e47", "R"); // ṇ
        builder.add("\u1e46", "R"); // Ṇ
        builder.add("th", "T"); // ṇh
        builder.add("TH", "T"); // ṆH
        builder.add("T", "t");
        builder.add("D", "d");
        builder.add("dh", "D");
        builder.add("DH", "D");
        builder.add("P", "p");
        builder.add("ph", "P");
        builder.add("PH", "P");
        builder.add("B", "b");
        builder.add("bh", "B");
        builder.add("BH", "B");
        builder.add("S", "s");
        builder.add("\u015b", "S"); // ś
        builder.add("\u015a", "S"); // Ś
        builder.add("\u1e63", "z"); // ṣ
        builder.add("\u1e62", "z"); // Ṣ
        builder.add("\u1e3B", "L"); // ḻ
        builder.add("\u1e3a", "L"); // Ḻ
        builder.add("\u1e3Bh", "|"); // ḻh
        builder.add("\u1e3AH", "|"); // ḺH
        builder.add("\u0303", "~"); // ̃

        return builder.build();
    }

}
