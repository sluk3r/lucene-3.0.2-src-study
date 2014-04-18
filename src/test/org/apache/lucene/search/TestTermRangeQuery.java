package org.apache.lucene.search;

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

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import org.apache.lucene.util.LuceneTestCase;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.text.Collator;


public class TestTermRangeQuery extends LuceneTestCase {

  private int docCount = 0;
  private RAMDirectory dir;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    dir = new RAMDirectory();
  }

  public void testExclusive() throws Exception {
    Query query = new TermRangeQuery("content", "A", "C", false, false); //wangxc 两段都排除地查。
    initializeIndex(new String[] {"A", "B", "C", "D"}); //wangxc 这是四篇文档 每个文档里只有一个字母。
    IndexSearcher searcher = new IndexSearcher(dir, true);
    ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("A,B,C,D, only B in range", 1, hits.length);
    searcher.close();

    initializeIndex(new String[] {"A", "B", "D"});//wangxc 这是要跟上面的重复？
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("A,B,D, only B in range", 1, hits.length);//wangxc 这个跟上面的测试有什么不同？这里想说明什么？
    searcher.close();

    addDoc("C");
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("C added, still only B in range", 1, hits.length);//wangxc 就是为了体现这个C加与不加的效果？ 这个加与不加有什么影响？
    searcher.close();
      //wangxc 可以进一步的想， 这个Exclusive在算法上怎么实现的？ 参数的传递是怎么设计的？
  }
  
  public void testInclusive() throws Exception {
    Query query = new TermRangeQuery("content", "A", "C", true, true);//wangxc 如果换成像“word”这样的词有什么影响？

    initializeIndex(new String[]{"A", "B", "C", "D"});
    IndexSearcher searcher = new IndexSearcher(dir, true);
    ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("A,B,C,D - A,B,C in range", 3, hits.length);
    searcher.close();

    initializeIndex(new String[]{"A", "B", "D"});
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("A,B,D - A and B in range", 2, hits.length);
    searcher.close();

    addDoc("C");
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("C added - A, B, C in range", 3, hits.length); //wangxc 这是一种什么测试理念？边界测试？
    searcher.close();
  }

  public void testEqualsHashcode() {//wangxc HashCode是Java里的一个基本原则，为什么要单拿出来搞这么个测试？
    Query query = new TermRangeQuery("content", "A", "C", true, true);
    
    query.setBoost(1.0f);
    Query other = new TermRangeQuery("content", "A", "C", true, true);
    other.setBoost(1.0f);

    assertEquals("query equals itself is true", query, query);
    assertEquals("equivalent queries are equal", query, other);
    assertEquals("hashcode must return same value when equals is true", query.hashCode(), other.hashCode());

    other.setBoost(2.0f);
    assertFalse("Different boost queries are not equal", query.equals(other));

    other = new TermRangeQuery("notcontent", "A", "C", true, true);
    assertFalse("Different fields are not equal", query.equals(other));

    other = new TermRangeQuery("content", "X", "C", true, true);
    assertFalse("Different lower terms are not equal", query.equals(other));

    other = new TermRangeQuery("content", "A", "Z", true, true);
    assertFalse("Different upper terms are not equal", query.equals(other));

    query = new TermRangeQuery("content", null, "C", true, true);
    other = new TermRangeQuery("content", null, "C", true, true);
    assertEquals("equivalent queries with null lowerterms are equal()", query, other);
    assertEquals("hashcode must return same value when equals is true", query.hashCode(), other.hashCode());

    query = new TermRangeQuery("content", "C", null, true, true);
    other = new TermRangeQuery("content", "C", null, true, true);
    assertEquals("equivalent queries with null upperterms are equal()", query, other);
    assertEquals("hashcode returns same value", query.hashCode(), other.hashCode());

    query = new TermRangeQuery("content", null, "C", true, true);
    other = new TermRangeQuery("content", "C", null, true, true);
    assertFalse("queries with different upper and lower terms are not equal", query.equals(other));

    query = new TermRangeQuery("content", "A", "C", false, false);
    other = new TermRangeQuery("content", "A", "C", true, true);
    assertFalse("queries with different inclusive are not equal", query.equals(other));
    
    query = new TermRangeQuery("content", "A", "C", false, false);
    other = new TermRangeQuery("content", "A", "C", false, false, Collator.getInstance());//wangxc Collator这个是Java标准API里支持。第一次见。
    assertFalse("a query with a collator is not equal to one without", query.equals(other));
  }

  public void testExclusiveCollating() throws Exception {
    Query query = new TermRangeQuery("content", "A", "C", false, false, Collator.getInstance(Locale.ENGLISH));//wangxc 这个Collator解决的是按什么Locale进行排序的问题？
    //wangxc 中文排序怎样？  看到一篇文章： http://dushanggaolou.iteye.com/blog/1198128。还行。
    initializeIndex(new String[] {"A", "B", "C", "D"});
    IndexSearcher searcher = new IndexSearcher(dir, true);
    ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("A,B,C,D, only B in range", 1, hits.length);
    searcher.close();

    initializeIndex(new String[] {"A", "B", "D"});
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("A,B,D, only B in range", 1, hits.length);
    searcher.close();

    addDoc("C");
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("C added, still only B in range", 1, hits.length);
    searcher.close();
  }

  public void testInclusiveCollating() throws Exception {
    Query query = new TermRangeQuery("content", "A", "C",true, true, Collator.getInstance(Locale.ENGLISH));

    initializeIndex(new String[]{"A", "B", "C", "D"});
    IndexSearcher searcher = new IndexSearcher(dir, true);
    ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("A,B,C,D - A,B,C in range", 3, hits.length);
    searcher.close();

    initializeIndex(new String[]{"A", "B", "D"});
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("A,B,D - A and B in range", 2, hits.length);
    searcher.close();

    addDoc("C");
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("C added - A, B, C in range", 3, hits.length);
    searcher.close();
  }

  public void testFarsi() throws Exception {//wangxc Farsi是波斯语
    // Neither Java 1.4.2 nor 1.5.0 has Farsi Locale collation available in
    // RuleBasedCollator.  However, the Arabic Locale seems to order the Farsi
    // characters properly.
    Collator collator = Collator.getInstance(new Locale("ar")); //wangxc 直接用了Arabic。
    Query query = new TermRangeQuery("content", "\u062F", "\u0698", true, true, collator);
    // Unicode order would include U+0633 in [ U+062F - U+0698 ], but Farsi
    // orders the U+0698 character before the U+0633 character, so the single
    // index Term below should NOT be returned by a TermRangeQuery with a Farsi
    // Collator (or an Arabic one for the case when Farsi is not supported).
    initializeIndex(new String[]{ "\u0633\u0627\u0628"});
    IndexSearcher searcher = new IndexSearcher(dir, true);
    ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("The index Term should not be included.", 0, hits.length);

    query = new TermRangeQuery("content", "\u0633", "\u0638",true, true, collator);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("The index Term should be included.", 1, hits.length);
    searcher.close();
  }
  
  public void testDanish() throws Exception { //wangxc 丹麦
    Collator collator = Collator.getInstance(new Locale("da", "dk"));
    // Danish collation orders the words below in the given order (example taken
    // from TestSort.testInternationalSort() ).
    String[] words = { "H\u00D8T", "H\u00C5T", "MAND" };//wangxc 在不能直接写字的情况下， 用类似"H\u00C5T"的表示也是不错的方式。
    Query query = new TermRangeQuery("content", "H\u00D8T", "MAND", false, false, collator);

    // Unicode order would not include "H\u00C5T" in [ "H\u00D8T", "MAND" ],
    // but Danish collation does.
    initializeIndex(words);
    IndexSearcher searcher = new IndexSearcher(dir, true);
    ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("The index Term should be included.", 1, hits.length);

    query = new TermRangeQuery("content", "H\u00C5T", "MAND", false, false, collator);
    hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals("The index Term should not be included.", 0, hits.length);
    searcher.close();
  }

  //wangxc 这个Analyzer只在最后的两个测试testExclusiveLowerNull和testInclusiveLowerNull中用到。
  private static class SingleCharAnalyzer extends Analyzer {

    private static class SingleCharTokenizer extends Tokenizer {
      char[] buffer = new char[1];
      boolean done;
      TermAttribute termAtt;
      
      public SingleCharTokenizer(Reader r) {
        super(r);
        termAtt = addAttribute(TermAttribute.class);
      }

      @Override
      public boolean incrementToken() throws IOException {
        int count = input.read(buffer);
        if (done)
          return false;
        else {
          clearAttributes();
          done = true;
          if (count == 1) {
            termAtt.termBuffer()[0] = buffer[0];
            termAtt.setTermLength(1);
          } else
            termAtt.setTermLength(0);
          return true;
        }
      }

      @Override
      public final void reset(Reader reader) throws IOException {
        super.reset(reader);
        done = false;
      }
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
      Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
      if (tokenizer == null) {
        tokenizer = new SingleCharTokenizer(reader);
        setPreviousTokenStream(tokenizer);
      } else
        tokenizer.reset(reader);
      return tokenizer;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SingleCharTokenizer(reader);
    }
  }

  private void initializeIndex(String[] values) throws IOException {
    initializeIndex(values, new WhitespaceAnalyzer());
  }

  private void initializeIndex(String[] values, Analyzer analyzer) throws IOException {
    IndexWriter writer = new IndexWriter(dir, analyzer, true, IndexWriter.MaxFieldLength.LIMITED);
    for (int i = 0; i < values.length; i++) {
      insertDoc(writer, values[i]);
    }
    writer.close();
  }

  private void addDoc(String content) throws IOException {
    IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), false, IndexWriter.MaxFieldLength.LIMITED);
    insertDoc(writer, content);
    writer.close();
  }

  private void insertDoc(IndexWriter writer, String content) throws IOException {
    Document doc = new Document();

    doc.add(new Field("id", "id" + docCount, Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("content", content, Field.Store.NO, Field.Index.ANALYZED));

    writer.addDocument(doc);
    docCount++;
  }

  // LUCENE-38
  public void testExclusiveLowerNull() throws Exception {
    Analyzer analyzer = new SingleCharAnalyzer();
    //http://issues.apache.org/jira/browse/LUCENE-38
    /*
    // wangxc 看这个issues的记录不错， 应该能从这里翻出些犄角旮旯的料。 有这么些好处：
      1. 这些料在实际工作中可以预料地应对一些表面上掩盖的问题。
      2. 以这些料出发，可以串起来深层次的研究。
      3. 面试中，如果能应对类似的料，说明够水平了。 
    */
    Query query = new TermRangeQuery("content", null, "C",
                                 false, false);
    initializeIndex(new String[] {"A", "B", "", "C", "D"}, analyzer); //wangxc 这个空文档算什么回事？在sort时的影响？
    IndexSearcher searcher = new IndexSearcher(dir, true);
    int numHits = searcher.search(query, null, 1000).totalHits;
    // When Lucene-38 is fixed, use the assert on the next line:
    assertEquals("A,B,<empty string>,C,D => A, B & <empty string> are in range", 3, numHits);
    // until Lucene-38 is fixed, use this assert:
    //assertEquals("A,B,<empty string>,C,D => A, B & <empty string> are in range", 2, hits.length());

    searcher.close();
    initializeIndex(new String[] {"A", "B", "", "D"}, analyzer);
    searcher = new IndexSearcher(dir, true);
    numHits = searcher.search(query, null, 1000).totalHits;
    // When Lucene-38 is fixed, use the assert on the next line:
    assertEquals("A,B,<empty string>,D => A, B & <empty string> are in range", 3, numHits);
    // until Lucene-38 is fixed, use this assert:
    //assertEquals("A,B,<empty string>,D => A, B & <empty string> are in range", 2, hits.length());
    searcher.close();
    addDoc("C");
    searcher = new IndexSearcher(dir, true);
    numHits = searcher.search(query, null, 1000).totalHits;
    // When Lucene-38 is fixed, use the assert on the next line:
    assertEquals("C added, still A, B & <empty string> are in range", 3, numHits);
    // until Lucene-38 is fixed, use this assert
    //assertEquals("C added, still A, B & <empty string> are in range", 2, hits.length());
    searcher.close();
  }

  // LUCENE-38
  public void testInclusiveLowerNull() throws Exception {
    //http://issues.apache.org/jira/browse/LUCENE-38
    Analyzer analyzer = new SingleCharAnalyzer();
    Query query = new TermRangeQuery("content", null, "C", true, true);
    initializeIndex(new String[]{"A", "B", "","C", "D"}, analyzer);
    IndexSearcher searcher = new IndexSearcher(dir, true);
    int numHits = searcher.search(query, null, 1000).totalHits;
    // When Lucene-38 is fixed, use the assert on the next line:
    assertEquals("A,B,<empty string>,C,D => A,B,<empty string>,C in range", 4, numHits);
    // until Lucene-38 is fixed, use this assert
    //assertEquals("A,B,<empty string>,C,D => A,B,<empty string>,C in range", 3, hits.length());
    searcher.close();
    initializeIndex(new String[]{"A", "B", "", "D"}, analyzer);
    searcher = new IndexSearcher(dir, true);
    numHits = searcher.search(query, null, 1000).totalHits;
    // When Lucene-38 is fixed, use the assert on the next line:
    assertEquals("A,B,<empty string>,D - A, B and <empty string> in range", 3, numHits);
    // until Lucene-38 is fixed, use this assert
    //assertEquals("A,B,<empty string>,D => A, B and <empty string> in range", 2, hits.length());
    searcher.close();
    addDoc("C");
    searcher = new IndexSearcher(dir, true);
    numHits = searcher.search(query, null, 1000).totalHits;
    // When Lucene-38 is fixed, use the assert on the next line:
    assertEquals("C added => A,B,<empty string>,C in range", 4, numHits);
    // until Lucene-38 is fixed, use this assert
    //assertEquals("C added => A,B,<empty string>,C in range", 3, hits.length());
     searcher.close();
  }
}
