package org.apache.lucene.index;

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


/** This is the base class for an in-memory posting list, //wangxc 这个基于内存的PostingList最终是怎么跟buffer和物理文件关联起来的？
 *  keyed by a Token.  {@link TermsHash} maintains a hash//wangxc Keyed by a Token的体现是？
 *  table holding one instance of this per unique Token.
 *  Consumers of TermsHash ({@link TermsHashConsumer}) must
 *  subclass this class with its own concrete class.
 *  FreqProxTermsWriter.PostingList is a private inner class used 
 *  for the freq/prox postings, and 
 *  TermVectorsTermsWriter.PostingList is a private inner class
 *  used to hold TermVectors postings. */

abstract class RawPostingList {
  final static int BYTES_SIZE = DocumentsWriter.OBJECT_HEADER_BYTES + 3*DocumentsWriter.INT_NUM_BYTE;
  int textStart;
  int intStart;
  int byteStart;
}
