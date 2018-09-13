# Solr Tokenizing Suggester
[![Build Status](https://travis-ci.com/sul-dlss/solr-tokenizing-suggester.svg?branch=master)](https://travis-ci.com/sul-dlss/solr-tokenizing-suggester)

Used in Stanford Library's [IIIF Content Search](https://iiif.io/api/search/1.0/) implementation.

## Usage

1. Clone the project

 ```sh
  $ git clone git://github.com/sul-dlss/solr-tokenizing-suggester.git
 ```

1. Run the maven installation

 ```sh
 $ mvn clean install
 ```

1. Include the generated jar (`target/tokenizing-suggest-v1.0.jar`) in your `solrconfig.xml` and configure

 ```xml
 ...
 <lib dir="${solr.install.dir:../../../..}/contrib" regex="tokenizing-suggest-.*\.jar" />
 ...
 <searchComponent name="suggest" class="solr.SuggestComponent">
  <lst name="suggester">
    <str name="name">mySuggester</str>
    <str name="lookupImpl">edu.stanford.dlss.search.suggest.analyzing.TokenizingLookupFactory</str>
    <str name="dictionaryImpl">DocumentDictionaryFactory</str>
    <str name="suggestAnalyzerFieldType">textSuggest</str>
    <str name="suggestTokenizingAnalyzerFieldType">textSuggestTokenizer</str>
    <str name="contextField">druid</str>
    <str name="field">ocrtext_suggest</str>
  </lst>
</searchComponent>

 ```
