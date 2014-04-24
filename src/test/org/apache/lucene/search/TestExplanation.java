package org.apache.lucene.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.Version;

import java.io.Reader;

/**
 * Created by wangxc on 2014/4/24.
 */
public class TestExplanation extends LuceneTestCase {
    private RAMDirectory directory;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        directory = new RAMDirectory();
        Analyzer analyzer = new Analyzer() {
            @Override //wangxc 这里的实现没有用到fieldName，原因？
            public TokenStream tokenStream(String fieldName, Reader reader) {
                return new WhitespaceTokenizer(reader);
            }

            @Override //wangxc 这里写死是基于什么考虑的？
            public int getPositionIncrementGap(String fieldName) {
                return 100;
            }
        };
        IndexWriter writer = new IndexWriter(directory, analyzer, true,
                IndexWriter.MaxFieldLength.LIMITED);

        Document doc = new Document();
        doc.add(new Field("contents", "one two three four five", Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("contents", "this is a repeated field - first part", Field.Store.YES, Field.Index.ANALYZED));
        //wangxc 第一次见这个类。 除了Field外， 还有其它实现么？ 看到四个类： AbstractField Field NumericField FieldsReader中的LazyField
        //wangxc 还有一个问题。 这里怎么特意定义一个Fieldable？
        //wangxc 在PhraseQuery的过程中， 是否重复有什么影响么？
        Fieldable repeatedField = new Field("contents", "second part of a repeated field", Field.Store.YES, Field.Index.ANALYZED);
        doc.add(repeatedField);
        doc.add(new Field("contents", "one two three two one", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new Field("contents", "phrase exist notexist exist found", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new Field("contents", "phrase exist notexist exist found", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        writer.optimize();
        writer.close();
    }

    public void testExplanation() throws Exception{
        String queryExpression = "second part of";

        QueryParser parser = new QueryParser(Version.LUCENE_30,
                "contents", new SimpleAnalyzer());
        Query query = parser.parse(queryExpression);

        System.out.println("Query: " + queryExpression);

        IndexSearcher searcher = new IndexSearcher(directory);
        TopDocs topDocs = searcher.search(query, 10);

        for (ScoreDoc match : topDocs.scoreDocs) {
            Explanation explanation
                    = searcher.explain(query, match.doc);     //#A

            System.out.println("----------");
            Document doc = searcher.doc(match.doc);
            System.out.println(explanation.toString());  //#B
        }
        searcher.close();
        directory.close();
    }
}
