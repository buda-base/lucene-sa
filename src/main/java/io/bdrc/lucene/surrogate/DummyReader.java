package io.bdrc.lucene.surrogate;

import java.io.IOException;
import java.io.Reader;

public class DummyReader extends Reader {
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        throw new IOException("read() not supported");
    }

    @Override
    public void close() throws IOException {
        throw new IOException("close() not supported");
    }

    public static final Reader  THE_READER = new DummyReader();
}
