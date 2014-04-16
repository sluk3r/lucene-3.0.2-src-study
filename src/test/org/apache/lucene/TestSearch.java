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

import java.util.GregorianCalendar;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.Version;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;

/** JUnit adaptation of an older test case SearchTest. */
public class TestSearch extends LuceneTestCase {

    /** Main for running test case by itself. */
    public static void main(String args[]) {
        TestRunner.run (new TestSuite(TestSearch.class));
    }

    /** This test performs a number of searches. It also compares output
     *  of searches using multi-file index segments with single-file
     *  index segments.
     *
     *  TODO: someone should check that the results of the searches are
     *        still correct by adding assert statements. Right now, the test
     *        passes if the results are the same between multi-file and
     *        single-file formats, even if the results are wrong.
     */
    public void testSearch() throws Exception {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      doTestSearch(pw, false);
      pw.close();
      sw.close();
      String multiFileOutput = sw.getBuffer().toString();
      System.out.println("multiFileOutput:\n" + multiFileOutput);

//      sw = new StringWriter();
//      pw = new PrintWriter(sw, true);
//      doTestSearch(pw, true);
//      pw.close();
//      sw.close();
//      String singleFileOutput = sw.getBuffer().toString();
//
//      assertEquals(multiFileOutput, singleFileOutput);
//
//      System.out.println("========================================================");
//      System.out.println("singleFileOutput: \n" + singleFileOutput);

        //wangxc question 从结果没看出来是singleFileOutput与否的区别
    }


    private void doTestSearch(PrintWriter out, boolean useCompoundFile)
    throws Exception
    {
      Directory directory = new RAMDirectory();
      Analyzer analyzer = new SimpleAnalyzer();
      IndexWriter writer = new IndexWriter(directory, analyzer, true, 
                                           IndexWriter.MaxFieldLength.LIMITED);

      writer.setUseCompoundFile(useCompoundFile);

      String[] docs = {
        "a b c d e",
        "a b c d e a b c d e",
        "a b c d e f g h i j",
        "a c e",
        "e c a",
        "a c e a c e",
        "a c e a b c",
        "a c e a f b c",//wangxc 新加了这个， 想试下slop的影响。 从结果看， 这个slop是4时，还能查询出来“a c e a f b c”， 感觉这里的slop已经大于4了。 再加一个试试？
        "a c e a f g h i j k b c"//wangxc, 看到效果了，这里a与b之间距离远大于4了， 结果没查询出来。  在Phrase的专门测试里应该有更精确的表示。
      };
      for (int j = 0; j < docs.length; j++) {
        Document d = new Document();
        d.add(new Field("contents", docs[j], Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(d);
      }

      out.println("docs indexed  size:" + docs.length);
      writer.close();

      Searcher searcher = new IndexSearcher(directory, true);

      String[] queries = {
        "a b",
        "\"a b\"",
        "\"a b c\"",
        "a c",
        "\"a c\"",
        "\"a c e\"", //wangxc 这几种是针对Phrase的测试？
      };
      //wangxc 看了测试打印出来的结果， 发现这个正是理解Phrase的好材料
      ScoreDoc[] hits = null;

      QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "contents", analyzer);
      parser.setPhraseSlop(4); //wangxc 这个4是小于等于4的意思？
      for (int j = 0; j < queries.length; j++) {
        Query query = parser.parse(queries[j]);
        out.println("\n\n\n Query: " + query.toString("contents")); //wangxc

      //DateFilter filter =
      //  new DateFilter("modified", Time(1997,0,1), Time(1998,0,1));
      //DateFilter filter = DateFilter.Before("modified", Time(1997,00,01));
      //System.out.println(filter);

        hits = searcher.search(query, null, 1000).scoreDocs;

        out.println(hits.length + " total results");
        for (int i = 0 ; i < hits.length && i < 10; i++) {
          Document d = searcher.doc(hits[i].doc);
          out.println(i + " " + hits[i].score
// 			   + " " + DateField.stringToDate(d.get("modified"))
                             + " " + d.get("contents"));
        }
      }

      searcher.close();
  }

  static long Time(int year, int month, int day) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.set(year, month, day);
    return calendar.getTime().getTime();
  }
}


/**
 *


 docs indexed  size:9



 Query: a b
 9 total results
 0 0.67981035 a b c d e a b c d e
 1 0.6729779 a b c d e
 2 0.6576601 a c e a b c
 3 0.6576601 a c e a f b c
 4 0.48069853 a b c d e f g h i j
 5 0.43844008 a c e a f g h i j k b c
 6 0.1379716 a c e a c e
 7 0.13008086 a c e
 8 0.13008086 e c a



 Query: "a b"~4
 5 total results
 0 0.94838655 a b c d e a b c d e
 1 0.9388548 a b c d e
 2 0.8047327 a c e a b c
 3 0.6706106 a b c d e f g h i j
 4 0.56903195 a c e a f b c



 Query: "a b c"~4
 5 total results
 0 1.3437651 a b c d e a b c d e
 1 1.3302596 a b c d e
 2 1.2490513 a c e a b c
 3 0.9501854 a b c d e f g h i j
 4 0.80625904 a c e a f b c



 Query: a c
 9 total results
 0 0.67097956 a c e a c e
 1 0.67097956 a c e a b c
 2 0.67097956 a c e a f b c
 3 0.6326056 a c e
 4 0.6326056 e c a
 5 0.5591496 a b c d e a b c d e
 6 0.5535299 a b c d e
 7 0.44731972 a c e a f g h i j k b c
 8 0.3953785 a b c d e f g h i j



 Query: "a c"~4
 9 total results
 0 0.94890845 a c e a c e
 1 0.8946395 a c e
 2 0.88762254 a c e a b c
 3 0.8442975 a c e a f b c
 4 0.61251783 a b c d e a b c d e
 5 0.5535299 a b c d e
 6 0.5165203 e c a //wangxc 这个反而有高分了？ 背后的原理？
 7 0.5001187 a c e a f g h i j k b c //wangxc 这个为什么没有高分？ 是因为后面还有离的很远的a和c？
 8 0.3953785 a b c d e f g h i j //wangxc 这个分低是因为文档长？a和c之间又没有紧挨着？



 Query: "a c e"~4
 9 total results
 0 1.5097042 a c e a c e //wangxc 在Lucene内部是怎么区分连续着两个ace的？   第一次见打分高于1的！！！
 1 1.3419592 a c e
 2 1.2119497 a c e a b c
 3 1.125267 a c e a f b c
 4 0.86623096 a b c d e a b c d e
 5 0.75017804 a c e a f g h i j k b c
 6 0.677933 a b c d e
 7 0.6001424 e c a
 8 0.48423782 a b c d e f g h i j
 *
 */





/**
multiFileOutput:
Query: a b
9 total results
0 0.67981035 a b c d e a b c d e
1 0.6729779 a b c d e
2 0.6576601 a c e a b c
3 0.6576601 a c e a f b c
4 0.48069853 a b c d e f g h i j
5 0.43844008 a c e a f g h i j k b c
6 0.1379716 a c e a c e
7 0.13008086 a c e
8 0.13008086 e c a
Query: "a b"~4
5 total results
0 0.94838655 a b c d e a b c d e
1 0.9388548 a b c d e
2 0.8047327 a c e a b c
3 0.6706106 a b c d e f g h i j
4 0.56903195 a c e a f b c
Query: "a b c"~4
5 total results
0 1.3437651 a b c d e a b c d e
1 1.3302596 a b c d e
2 1.2490513 a c e a b c
3 0.9501854 a b c d e f g h i j
4 0.80625904 a c e a f b c
Query: a c
9 total results
0 0.67097956 a c e a c e
1 0.67097956 a c e a b c
2 0.67097956 a c e a f b c
3 0.6326056 a c e
4 0.6326056 e c a
5 0.5591496 a b c d e a b c d e
6 0.5535299 a b c d e
7 0.44731972 a c e a f g h i j k b c
8 0.3953785 a b c d e f g h i j
Query: "a c"~4
9 total results
0 0.94890845 a c e a c e
1 0.8946395 a c e
2 0.88762254 a c e a b c
3 0.8442975 a c e a f b c
4 0.61251783 a b c d e a b c d e
5 0.5535299 a b c d e
6 0.5165203 e c a
7 0.5001187 a c e a f g h i j k b c
8 0.3953785 a b c d e f g h i j
Query: "a c e"~4
9 total results
0 1.5097042 a c e a c e
1 1.3419592 a c e
2 1.2119497 a c e a b c
3 1.125267 a c e a f b c
4 0.86623096 a b c d e a b c d e
5 0.75017804 a c e a f g h i j k b c
6 0.677933 a b c d e
7 0.6001424 e c a
8 0.48423782 a b c d e f g h i j

========================================================
singleFileOutput:
Query: a b
9 total results
0 0.67981035 a b c d e a b c d e
1 0.6729779 a b c d e
2 0.6576601 a c e a b c
3 0.6576601 a c e a f b c
4 0.48069853 a b c d e f g h i j
5 0.43844008 a c e a f g h i j k b c
6 0.1379716 a c e a c e
7 0.13008086 a c e
8 0.13008086 e c a
Query: "a b"~4
5 total results
0 0.94838655 a b c d e a b c d e
1 0.9388548 a b c d e
2 0.8047327 a c e a b c
3 0.6706106 a b c d e f g h i j
4 0.56903195 a c e a f b c
Query: "a b c"~4
5 total results
0 1.3437651 a b c d e a b c d e
1 1.3302596 a b c d e
2 1.2490513 a c e a b c
3 0.9501854 a b c d e f g h i j
4 0.80625904 a c e a f b c
Query: a c
9 total results
0 0.67097956 a c e a c e
1 0.67097956 a c e a b c
2 0.67097956 a c e a f b c
3 0.6326056 a c e
4 0.6326056 e c a
5 0.5591496 a b c d e a b c d e
6 0.5535299 a b c d e
7 0.44731972 a c e a f g h i j k b c
8 0.3953785 a b c d e f g h i j
Query: "a c"~4
9 total results
0 0.94890845 a c e a c e
1 0.8946395 a c e
2 0.88762254 a c e a b c
3 0.8442975 a c e a f b c
4 0.61251783 a b c d e a b c d e
5 0.5535299 a b c d e
6 0.5165203 e c a
7 0.5001187 a c e a f g h i j k b c
8 0.3953785 a b c d e f g h i j
Query: "a c e"~4
9 total results
0 1.5097042 a c e a c e
1 1.3419592 a c e
2 1.2119497 a c e a b c
3 1.125267 a c e a f b c
4 0.86623096 a b c d e a b c d e
5 0.75017804 a c e a f g h i j k b c
6 0.677933 a b c d e
7 0.6001424 e c a
8 0.48423782 a b c d e f g h i j
 */