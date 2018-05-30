/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;

/**
 * Tests emulating a whole Lucene system.
 * Creates a temporary folder that receives the temporary file with the input string 
 * and the index generated from it.
 * The index is then queried and the number of matching documents (1 or 0) is displayed,
 * together with the score.
 * 
 * @author Hélios Hildt
 *
 */
public class FullLuceneShowcase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testTC2PYstrict() throws IOException, ParseException {
        String input = "loke ’vināśi praṇāme ’py artti";
        String query = "loka";

        // indexing in words, from iats, with stopwords
        Analyzer indexingAnalyzer = new SanskritAnalyzer(true, 2, "skrt-stopwords.txt");
        // querying in words, from SLP, with stopwords  
        Analyzer queryingAnalyzer = new SanskritAnalyzer(true, 0, "skrt-stopwords.txt");

        File testSubFolder = folder.newFolder("test");

        indexTest(input, indexingAnalyzer, testSubFolder);
        int hits = searchIndex(query, queryingAnalyzer, testSubFolder, 1);
        folder.delete(); // just to be sure it is done

        assertEquals(hits, 1);
    }

    int searchIndex(String queryString, Analyzer analyzer, File indexFolder, int repeat)
            throws IOException, ParseException {
        String field = "contents";

        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFolder.toPath()));
        IndexSearcher searcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(queryString);
        TopDocs results = null;
        ScoreDoc[] hits = null;
        int numTotalHits = -1;

        if (repeat > 0) { // repeat & time as benchmark
            Date start = new Date();
            for (int i = 0; i < repeat; i++) {
                results = searcher.search(query, 100);
                hits = results.scoreDocs;
                numTotalHits = results.totalHits;
            }
            Date end = new Date();
            System.out.println("Time: " + (end.getTime() - start.getTime()) + "ms");
        }
        System.out.println(numTotalHits + " total matching documents");

        for (int i = 0; i < hits.length; i++) {
            // output raw format
            System.out.println("\tdoc=" + hits[i].doc + " score=" + hits[i].score);

        }

        reader.close();
        return numTotalHits;
    }

    /** Bootstrapping for indexDoc() */
    void indexTest(String input, Analyzer analyzer, File testSubFolder) throws IOException {
        // create temp file and write input string in it.
        File testFile = File.createTempFile("test-content_", ".txt", testSubFolder);
        BufferedWriter bw = new BufferedWriter(new FileWriter(testFile));
        bw.write(input);
        bw.close();

        // config for indexDoc()
        final Path docPath = Paths.get(testFile.getAbsolutePath());
        Directory dir = FSDirectory.open(Paths.get(testSubFolder.getAbsolutePath()));

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, iwc);
        indexDoc(writer, docPath, Files.getLastModifiedTime(docPath).toMillis());
    }

    /** Indexes a single document */
    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            // make a new, empty document
            Document doc = new Document();

            // path field in the index
            Field pathField = new StringField("path", file.toString(), Field.Store.YES);
            doc.add(pathField);

            // modified field in index (last modified date of file)
            doc.add(new LongPoint("modified", lastModified));

            // file content is tokenized and indexed, but not stored. (UTF-8 expected)
            doc.add(new TextField("contents",
                    new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

            // New index, so we just add the document (no old document can be there):
            writer.addDocument(doc);
            writer.close();
        }
    }
}