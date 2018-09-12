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

package edu.stanford.dlss.search.suggest.analyzing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.spelling.suggest.LookupFactory;

/**
 * Factory for {@link AnalyzingInfixSuggester}
 * @lucene.experimental
 */
public class TokenizingLookupFactory extends LookupFactory {
    /**
     * The analyzer used at "query-time" and "build-time" to analyze suggestions.
     */
    protected static final String QUERY_ANALYZER = "suggestAnalyzerFieldType";

    /**
     * The analyzer used at "tokenizing-time" to do something.
     */
    protected static final String TOKENIZING_ANALYZER = "suggestTokenizingAnalyzerFieldType";

    /**
     * The path where the underlying index is stored
     * if no index is found, it will be generated by
     * the AnalyzingInfixSuggester
     */
    protected static final String INDEX_PATH = "indexPath";

    /**
     * Minimum number of leading characters before PrefixQuery is used (default 4).
     * Prefixes shorter than this are indexed as character ngrams
     * (increasing index size but making lookups faster)
     */
    protected static final String MIN_PREFIX_CHARS = "minPrefixChars";

    /**
     * Boolean clause matching option for multiple terms
     * Default is true - all terms required.
     */
    protected static final String ALL_TERMS_REQUIRED = "allTermsRequired";

    /** Highlight suggest terms  - default is true. */
    protected static final String HIGHLIGHT = "highlight";

    /**
     * Default path where the index for the suggester is stored/loaded from
     * */
    private static final String DEFAULT_INDEX_PATH = "analyzingInfixSuggesterIndexDir";

    /**
     * File name for the automaton.
     */
    private static final String FILENAME = "iwfsta.bin";

    /**
     * Clone of CONTEXTS_FIELD_NAME in TokenizingSuggester
     */
    public static final String CONTEXTS_FIELD_NAME = "contexts";


    @Override
    public Lookup create(NamedList params, SolrCore core) {
        // mandatory parameter
        Object fieldTypeName = params.get(QUERY_ANALYZER);
        if (fieldTypeName == null) {
            throw new IllegalArgumentException("Error in configuration: " + QUERY_ANALYZER + " parameter is mandatory");
        }
        FieldType ft = core.getLatestSchema().getFieldTypeByName(fieldTypeName.toString());
        if (ft == null) {
            throw new IllegalArgumentException("Error in configuration: " + fieldTypeName.toString() + " is not defined in the schema");
        }

        // mandatory parameter
        Object tokenizingFieldTypeName = params.get(TOKENIZING_ANALYZER);
        if (tokenizingFieldTypeName == null) {
            throw new IllegalArgumentException("Error in configuration: " + TOKENIZING_ANALYZER + " parameter is mandatory");
        }
        FieldType tft = core.getLatestSchema().getFieldTypeByName(tokenizingFieldTypeName.toString());
        if (tft == null) {
            throw new IllegalArgumentException("Error in configuration: " + tokenizingFieldTypeName.toString() + " is not defined in the schema");
        }

        Analyzer indexAnalyzer = ft.getIndexAnalyzer();
        Analyzer queryAnalyzer = ft.getQueryAnalyzer();
        Analyzer tokenizingAnalyzer = tft.getIndexAnalyzer();

        // optional parameters

        String indexPath = params.get(INDEX_PATH) != null
                ? params.get(INDEX_PATH).toString()
                : DEFAULT_INDEX_PATH;
        if (new File(indexPath).isAbsolute() == false) {
            indexPath = core.getDataDir() + File.separator + indexPath;
        }

        int minPrefixChars = params.get(MIN_PREFIX_CHARS) != null
                ? Integer.parseInt(params.get(MIN_PREFIX_CHARS).toString())
                : AnalyzingInfixSuggester.DEFAULT_MIN_PREFIX_CHARS;

        boolean allTermsRequired = params.get(ALL_TERMS_REQUIRED) != null
                ? Boolean.getBoolean(params.get(ALL_TERMS_REQUIRED).toString())
                : AnalyzingInfixSuggester.DEFAULT_ALL_TERMS_REQUIRED;

        boolean highlight = params.get(HIGHLIGHT) != null
                ? Boolean.getBoolean(params.get(HIGHLIGHT).toString())
                : AnalyzingInfixSuggester.DEFAULT_HIGHLIGHT;

        try {
            return new TokenizingSuggester(FSDirectory.open(new File(indexPath).toPath()), indexAnalyzer,
                    queryAnalyzer, tokenizingAnalyzer, minPrefixChars, true,
                    allTermsRequired, highlight) {
                @Override
                public List<LookupResult> lookup(CharSequence key, Set<BytesRef> contexts, int num, boolean allTermsRequired, boolean doHighlight) throws IOException {
                    List<LookupResult> res = super.lookup(key, contexts, num, allTermsRequired, doHighlight);
                    if (doHighlight) {
                        List<LookupResult> res2 = new ArrayList<>();
                        for(LookupResult hit : res) {
                            res2.add(new LookupResult(hit.highlightKey.toString(),
                                    hit.highlightKey,
                                    hit.value,
                                    hit.payload,
                                    hit.contexts));
                        }
                        res = res2;
                    }

                    return res;
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String storeFileName() {
        return FILENAME;
    }
}