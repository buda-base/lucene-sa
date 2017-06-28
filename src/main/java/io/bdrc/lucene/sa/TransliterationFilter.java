package io.bdrc.lucene.sa;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.charfilter.BaseCharFilter;
import org.apache.lucene.analysis.util.RollingCharBuffer;

// attempt to do a Devanagari -> slp1 charfilter, to be generalized

public class TransliterationFilter extends BaseCharFilter {

    public static final int OTHER = 0;
    public static final int CONSONNANT = 1;
    public static final int VOWEL = 2;
    public static final int VOWEL_LETTER = 3;

    private final RollingCharBuffer buffer = new RollingCharBuffer();
    private int inputOff;

    public static class CharInfo {
        public final char slp;
        public final int type;
        public CharInfo(char slp, int type) {
            this.slp = slp;
            this.type = type;
        }
    }

    // TODO: handle \u0933\u094d\u0939 -> |
    // \u0950 -> oM
    // \u094d -> virama
    // \u0965 -> ..

    public static final Map<Character, CharInfo> charInfos = new HashMap<>();

    static {

        charInfos.put('\u0902', new CharInfo('M', OTHER));
        charInfos.put('\u0903', new CharInfo('H', OTHER));
        charInfos.put('\u0951', new CharInfo('^', OTHER));
        charInfos.put('\u0901', new CharInfo('~', OTHER));
        charInfos.put('\u0952', new CharInfo('\\', OTHER));
        charInfos.put('\u093D', new CharInfo('\'', OTHER));
        charInfos.put('\u0964', new CharInfo('.', OTHER));

        charInfos.put('\u0905', new CharInfo('a', VOWEL_LETTER));
        charInfos.put('\u0906', new CharInfo('A', VOWEL_LETTER));
        charInfos.put('\u0907', new CharInfo('i', VOWEL_LETTER));
        charInfos.put('\u0908', new CharInfo('I', VOWEL_LETTER));
        charInfos.put('\u0909', new CharInfo('u', VOWEL_LETTER));
        charInfos.put('\u090A', new CharInfo('U', VOWEL_LETTER));
        charInfos.put('\u090B', new CharInfo('f', VOWEL_LETTER));
        charInfos.put('\u0960', new CharInfo('F', VOWEL_LETTER));
        charInfos.put('\u090C', new CharInfo('x', VOWEL_LETTER));
        charInfos.put('\u0961', new CharInfo('X', VOWEL_LETTER));
        charInfos.put('\u090F', new CharInfo('e', VOWEL_LETTER));
        charInfos.put('\u0910', new CharInfo('E', VOWEL_LETTER));
        charInfos.put('\u0913', new CharInfo('o', VOWEL_LETTER));
        charInfos.put('\u0914', new CharInfo('O', VOWEL_LETTER));

        charInfos.put('\u093E', new CharInfo('A', VOWEL));
        charInfos.put('\u093F', new CharInfo('i', VOWEL));
        charInfos.put('\u0940', new CharInfo('I', VOWEL));
        charInfos.put('\u0941', new CharInfo('u', VOWEL));
        charInfos.put('\u0942', new CharInfo('U', VOWEL));
        charInfos.put('\u0943', new CharInfo('f', VOWEL));
        charInfos.put('\u0944', new CharInfo('F', VOWEL));
        charInfos.put('\u0962', new CharInfo('x', VOWEL));
        charInfos.put('\u0963', new CharInfo('X', VOWEL));
        charInfos.put('\u0947', new CharInfo('e', VOWEL));
        charInfos.put('\u0948', new CharInfo('E', VOWEL));
        charInfos.put('\u094B', new CharInfo('o', VOWEL));
        charInfos.put('\u094C', new CharInfo('O', VOWEL));

        charInfos.put('\u0915', new CharInfo('k', CONSONNANT));
        charInfos.put('\u0916', new CharInfo('K', CONSONNANT));
        charInfos.put('\u0917', new CharInfo('g', CONSONNANT));
        charInfos.put('\u0918', new CharInfo('G', CONSONNANT));
        charInfos.put('\u0919', new CharInfo('N', CONSONNANT));
        charInfos.put('\u091A', new CharInfo('c', CONSONNANT));
        charInfos.put('\u091B', new CharInfo('C', CONSONNANT));
        charInfos.put('\u091C', new CharInfo('j', CONSONNANT));
        charInfos.put('\u091D', new CharInfo('J', CONSONNANT));
        charInfos.put('\u091E', new CharInfo('Y', CONSONNANT));
        charInfos.put('\u091F', new CharInfo('w', CONSONNANT));
        charInfos.put('\u0920', new CharInfo('W', CONSONNANT));
        charInfos.put('\u0921', new CharInfo('q', CONSONNANT));
        charInfos.put('\u0922', new CharInfo('Q', CONSONNANT));
        charInfos.put('\u0923', new CharInfo('R', CONSONNANT));
        charInfos.put('\u0924', new CharInfo('t', CONSONNANT));
        charInfos.put('\u0925', new CharInfo('T', CONSONNANT));
        charInfos.put('\u0926', new CharInfo('d', CONSONNANT));
        charInfos.put('\u0927', new CharInfo('D', CONSONNANT));
        charInfos.put('\u0928', new CharInfo('n', CONSONNANT));
        charInfos.put('\u092A', new CharInfo('p', CONSONNANT));
        charInfos.put('\u092B', new CharInfo('P', CONSONNANT));
        charInfos.put('\u092C', new CharInfo('b', CONSONNANT));
        charInfos.put('\u092D', new CharInfo('B', CONSONNANT));
        charInfos.put('\u092E', new CharInfo('m', CONSONNANT));
        charInfos.put('\u092F', new CharInfo('y', CONSONNANT));
        charInfos.put('\u0930', new CharInfo('r', CONSONNANT));
        charInfos.put('\u0932', new CharInfo('l', CONSONNANT));
        charInfos.put('\u0933', new CharInfo('L', CONSONNANT));
        charInfos.put('\u0935', new CharInfo('v', CONSONNANT));
        charInfos.put('\u0936', new CharInfo('S', CONSONNANT));
        charInfos.put('\u0937', new CharInfo('z', CONSONNANT));
        charInfos.put('\u0938', new CharInfo('s', CONSONNANT));
        charInfos.put('\u0939', new CharInfo('h', CONSONNANT));

        charInfos.put('\u0966', new CharInfo('0', OTHER));
        charInfos.put('\u0967', new CharInfo('1', OTHER));
        charInfos.put('\u0968', new CharInfo('2', OTHER));
        charInfos.put('\u0969', new CharInfo('3', OTHER));
        charInfos.put('\u096A', new CharInfo('4', OTHER));
        charInfos.put('\u096B', new CharInfo('5', OTHER));
        charInfos.put('\u096C', new CharInfo('6', OTHER));
        charInfos.put('\u096D', new CharInfo('7', OTHER));
        charInfos.put('\u096E', new CharInfo('8', OTHER));
        charInfos.put('\u096F', new CharInfo('9', OTHER));
    }

    public TransliterationFilter(Reader in) {
        super(in);
        buffer.reset(in);
    }

    @Override
    public void reset() throws IOException {
        input.reset();
        buffer.reset(input);
        inputOff = 0;
    }

    @Override
    public int read() throws IOException {
        final int ret = buffer.get(inputOff);
        if (ret != -1) {
            inputOff++;
            buffer.freeBefore(inputOff);
        }
        return ret;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int numRead = 0;
        for(int i = off; i < off + len; i++) {
            int c = read();
            if (c == -1) break;
            cbuf[i] = (char) c;
            numRead++;
        }

        return numRead == 0 ? -1 : numRead;
    }

}
