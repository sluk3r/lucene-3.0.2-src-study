package org.apache.lucene;

/**
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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.Version;
import org.apache.lucene.util._TestUtil;

/**
 * A very simple demo used in the API documentation (src/java/overview.html).
 *
 * Please try to keep src/java/overview.html up-to-date when making changes
 * to this class.
 */
public class TestDemo extends LuceneTestCase {

  public void testDemo() throws IOException, ParseException {

    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

    // Store the index in memory:
    Directory directory = new RAMDirectory(); //wangxc 这个应该是最简单的锁机制？或者应该叫无锁Factory（看了实现， 是基于HashMap实现的）
    // To store an index on disk, use this instead:
    //Directory directory = FSDirectory.open("/tmp/testindex");
    IndexWriter iwriter = new IndexWriter(directory, analyzer, true,
                                          new IndexWriter.MaxFieldLength(25000));
    Document doc = new Document();
    String text = "This is the text to be indexed.";
    doc.add(new Field("fieldname", text, Field.Store.YES,
        Field.Index.ANALYZED));
    iwriter.addDocument(doc);//wangxc 这个是冰山下的一角。
    iwriter.close();
    
    // Now search the index:
    IndexSearcher isearcher = new IndexSearcher(directory, true); // read-only=true wangxc 这个注释写法不错。
    // Parse a simple query that searches for "text":
    QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "fieldname", analyzer);
    Query query = parser.parse("text");
    ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
    assertEquals(1, hits.length);
    // Iterate through the results:
    for (int i = 0; i < hits.length; i++) {
      Document hitDoc = isearcher.doc(hits[i].doc);
      assertEquals("This is the text to be indexed.", hitDoc.get("fieldname"));
    }
    isearcher.close();
    directory.close();
    
  }



    public void testDemoFSDirectory() throws IOException, ParseException {

        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

        // Store the index in memory:
        File f = new File("./indexResult");
        if (f.exists()) {
            f.delete();
        }
        Directory directory = FSDirectory.open(f);
        // To store an index on disk, use this instead:
        //Directory directory = FSDirectory.open("/tmp/testindex");
        IndexWriter iwriter = new IndexWriter(directory, analyzer, true,
                new IndexWriter.MaxFieldLength(25000));

        int docNum = 100000;
        for (int i=0;i<docNum;i++) {
            Document doc = new Document();
            String text = "This is the text to be indexed.Mozilla applications store a user's personal information in a unique profile. The first time you start any Mozilla application, it will automatically create a default profile; additional profiles can be created using the Profile Manager. The settings which form a profile are stored in files within a special folder on your computer — this is the profile folder. The installation directory also includes a \"profile\" folder but this folder contains program defaults, not your user profile data.\n" +
                    "For information on the profile folder specific to certain applications, including how to find the profile folder, see the following articles:";
            doc.add(new Field("fieldname", text, Field.Store.YES,
                    Field.Index.ANALYZED));
            iwriter.addDocument(doc);
        }
        iwriter.close();

        // Now search the index:
        IndexSearcher isearcher = new IndexSearcher(directory, true); // read-only=true wangxc 这个注释写法不错。
        // Parse a simple query that searches for "text":
        QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "fieldname", analyzer);
        Query query = parser.parse("text");
        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        assertEquals(1, hits.length);
        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            assertEquals("This is the text to be indexed.", hitDoc.get("fieldname"));
        }
        isearcher.close();
        directory.close();

    }


}
