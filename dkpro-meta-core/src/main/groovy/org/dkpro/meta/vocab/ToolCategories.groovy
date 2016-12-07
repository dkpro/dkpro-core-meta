/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dkpro.meta.vocab;

public class ToolCategories
{
    public static def roleNames = [
         coref:          'Coreference resolver',
         tagger:         'Part-of-speech tagger',
         parser:         'Parser',
         chunker:        'Chunker',
         segmenter:      'Segmenter',
         checker:        'Checker',
         lemmatizer:     'Lemmatizer',
         srl:            'Semantic role labeler',
         morph:          'Morphological analyzer',
         transformer:    'Transformer',
         stem:           'Stemmer',
         ner:            'Named Entity Recognizer',
         langdetect:     'Language Identifier',
         transcriptor:   'Phonetic Transcriptor',
         topicmodel:     'Topic Model',
         embeddings:     'Embeddings',
         gazeteer:       'Gazeteer',
         other:          'Other' ];
    
    public static String COREF = "coref";
    public static String NER = "ner";
    public static String CHECKER = "checker";
    public static String MORPH = "morph";
    public static String SRL = "srl";
    public static String STEM = "stem";
    public static String TRANSFORMER = "transformer";
    public static String CHUNKER = "chunker";
    public static String LANGDETECT = "langdetect";
    public static String TAGGER = "tagger";
    public static String PARSER = "parser";
    public static String SEGMENTER = "segmenter";
    public static String LEMMATIZER = "lemmatizer";
    public static String TRANSCRIPTOR = "transcriptor";
    public static String TOPICMODEL = "topicmodel";
    public static String GAZETEER = "gazeteer";
    public static String EMBEDDINGS = "embeddings";
    public static String OTHER = "other";
}
