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
            public TokenStream tokenStream(String fieldName, Reader reader) {
                return new WhitespaceTokenizer(reader);
            }

            public int getPositionIncrementGap(String fieldName) {
                return 100;
            }
        };
        IndexWriter writer = new IndexWriter(directory, analyzer, true,
                IndexWriter.MaxFieldLength.LIMITED);

        Document doc = new Document();
        doc.add(new Field("title", "first", Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("contents", "one two three four five, this is a repeated field - first part", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new Field("contents", "one two three two one three", Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("title", "second", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new Field("title", "third", Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("contents", "phrase exist  three  notexist exist found", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        doc = new Document();
        doc.add(new Field("title", "fourth", Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("contents", "phrase exist three notexist exist found, this is a three repeated field - first part", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);

        writer.optimize();
        writer.close();
    }

    public void testExplanation() throws Exception{
        String queryExpression = "three found";

        QueryParser parser = new QueryParser(Version.LUCENE_30,
                "contents", new SimpleAnalyzer());
        Query query = parser.parse(queryExpression);

        System.out.println("Query: " + queryExpression);

        IndexSearcher searcher = new IndexSearcher(directory);
        TopDocs topDocs = searcher.search(query, 10);

        for (ScoreDoc match : topDocs.scoreDocs) {
            Explanation explanation = searcher.explain(query, match.doc);     //#A

            System.out.println("----------");
            Document doc = searcher.doc(match.doc);
            System.out.println("title: " + doc.get("title"));
            System.out.println(explanation.toString());  //#B
        }
        searcher.close();
        directory.close();
    }
}

/*
Query: three

1, 四个文档的idf值都一样是0.7768564。 这个怎么解释？
2, fieldWeight是最终看到的打分么？它是 tf * idf * fieldNorm.这三项跟打分公式里的六项怎么个对应关系？ 六项是： tf(t in d)、 idf(t) 、  t.getBoost()、  norm(t,d)和 coord(q,d) 、 queryNorm(q)
3.

----------
title: second
0.41199034 = (MATCH) fieldWeight(contents:three in 1), product of:
  1.4142135 = tf(termFreq(contents:three)=2)
  0.7768564 = idf(docFreq=4, maxDocs=4)
  0.375 = fieldNorm(field=contents, doc=1)

----------
title: third
0.29132116 = (MATCH) fieldWeight(contents:three in 2), product of:
  1.0 = tf(termFreq(contents:three)=1)
  0.7768564 = idf(docFreq=4, maxDocs=4)
  0.375 = fieldNorm(field=contents, doc=2)

----------
title: fourth
0.27466023 = (MATCH) fieldWeight(contents:three in 3), product of:  1.4142135 *  0.7768564 *  0.25 = 0.27466023
  1.4142135 = tf(termFreq(contents:three)=2)
  0.7768564 = idf(docFreq=4, maxDocs=4)
  0.25 = fieldNorm(field=contents, doc=3)

----------
title: first
0.1942141 = (MATCH) fieldWeight(contents:three in 0), product of:
  1.0 = tf(termFreq(contents:three)=1)
  0.7768564 = idf(docFreq=4, maxDocs=4)
  0.25 = fieldNorm(field=contents, doc=0)


====================================================================================================================================================
Query: three found
----------
title: third
0.6985731 = (MATCH) sum of:
  0.12148768 = (MATCH) weight(contents:three in 2), product of:
    0.41702318 = queryWeight(contents:three), product of:
      0.7768564 = idf(docFreq=4, maxDocs=4)
      0.53680855 = queryNorm
    0.29132116 = (MATCH) fieldWeight(contents:three in 2), product of:
      1.0 = tf(termFreq(contents:three)=1)
      0.7768564 = idf(docFreq=4, maxDocs=4)
      0.375 = fieldNorm(field=contents, doc=2)
  0.57708544 = (MATCH) weight(contents:found in 2), product of:
    0.9088959 = queryWeight(contents:found), product of:
      1.6931472 = idf(docFreq=1, maxDocs=4)
      0.53680855 = queryNorm
    0.6349302 = (MATCH) fieldWeight(contents:found in 2), product of:
      1.0 = tf(termFreq(contents:found)=1)
      1.6931472 = idf(docFreq=1, maxDocs=4)
      0.375 = fieldNorm(field=contents, doc=2)

----------
title: second
0.08590476 = (MATCH) product of:
  0.17180952 = (MATCH) sum of:
    0.17180952 = (MATCH) weight(contents:three in 1), product of:
      0.41702318 = queryWeight(contents:three), product of:
        0.7768564 = idf(docFreq=4, maxDocs=4)
        0.53680855 = queryNorm
      0.41199034 = (MATCH) fieldWeight(contents:three in 1), product of:
        1.4142135 = tf(termFreq(contents:three)=2)
        0.7768564 = idf(docFreq=4, maxDocs=4)
        0.375 = fieldNorm(field=contents, doc=1)
  0.5 = coord(1/2)

----------
title: fourth
0.05726984 = (MATCH) product of:
  0.11453968 = (MATCH) sum of:
    0.11453968 = (MATCH) weight(contents:three in 3), product of:
      0.41702318 = queryWeight(contents:three), product of:
        0.7768564 = idf(docFreq=4, maxDocs=4)
        0.53680855 = queryNorm
      0.27466023 = (MATCH) fieldWeight(contents:three in 3), product of:
        1.4142135 = tf(termFreq(contents:three)=2)
        0.7768564 = idf(docFreq=4, maxDocs=4)
        0.25 = fieldNorm(field=contents, doc=3)
  0.5 = coord(1/2)

----------
title: first
0.04049589 = (MATCH) product of:
  0.08099178 = (MATCH) sum of:
    0.08099178 = (MATCH) weight(contents:three in 0), product of:
      0.41702318 = queryWeight(contents:three), product of:
        0.7768564 = idf(docFreq=4, maxDocs=4)
        0.53680855 = queryNorm
      0.1942141 = (MATCH) fieldWeight(contents:three in 0), product of:
        1.0 = tf(termFreq(contents:three)=1)
        0.7768564 = idf(docFreq=4, maxDocs=4)
        0.25 = fieldNorm(field=contents, doc=0)
  0.5 = coord(1/2)
*/
