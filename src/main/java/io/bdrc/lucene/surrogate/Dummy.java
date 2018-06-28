package io.bdrc.lucene.surrogate;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.Reader;

/**
 * Dummy.READER is required to migrate these 3 classes from Lucene 6 to 4:
 * SkrtWordTokenizer, SkrtSyllableTokenizer, SanskritAnalyzer;
 * More precisely, Lucene 6 has Analyzer and Tokenizer constructors taking no arguments,
 * that set the `input' member to the (private) Tokenizer.ILLEGAL_STATE_READER;
 * OTOH Lucene 4 requires at least the provision of a Reader object,
 * (and using Tokenizer.ILLEGAL_STATE_READER is not an option as it is private)
 * Hence -- the need for a Dummy.READER, which will throw upon invocation
 * of its read() or close() method.
 */
public abstract class Dummy {

    public static final Reader  READER = new Reader() {
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            throw new IOException("read() not supported");
        }

        @Override
        public void close() throws IOException {
            throw new IOException("close() not supported");
        }
    };
}
