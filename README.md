# Lucene Analyzers for Sanskrit

## Components

### SanskritAnalyzer
The main analyzer.
It tokenizes the input text using *SkrtWordTokenizer*, then applies *StopFilter* (see below)

There are two constructors. The nullary constructor and 

```
    SanskritAnalyzer(boolean segmentInWords, int inputEncoding, String stopFilename)
		
    segmentInWords - if the segmentation is on words instead of syllables
    inputEncoding - 0 for SLP, 1 for devanagari, 2 for romanized sanskrit
    stopFilename - see below
```

The nullary constructor is equivalent to `SanskritAnalyzer(true, 0, "src/main/resources/skrt-stopwords.txt")`

### SkrtWordTokenizer

This tokenizer produces words through a Maximal Matching algorithm. It builds on top of [this Trie implementation](https://github.com/BuddhistDigitalResourceCenter/stemmer).

It undoes the sandhi to find the correct word boundaries and lemmatizes all the produced tokens.

Due to its design, this tokenizer doesn't deal with contextual ambiguities.
For example, "nagaraM" could either be a word of its own or "na" + "garaM", but will be parsed as a single word as long as "nagaraM" is present in the lexical resources. 

### SkrtSyllableTokenizer

Does not implement complex syllabation rules, but does the same thing as Peter Scharf's [script](http://www.sanskritlibrary.org/Sanskrit/SanskritTransliterate/syllabify.html).

### Stopword Filter

The [list of stopwords](src/main/resources/skrt-stopwords.txt) is [this list](https://gist.github.com/Akhilesh28/b012159a10a642ed5c34e551db76f236) encoded in SLP
The list must be formatted in the following way:

 - in SLP encoding
 - 1 word per line
 - empty lines (with and without comments), spaces and tabs are allowed
 - comments start with `#`
 - lines can end with a comment

### Roman2SlpFilter

Transcodes the romanized sanskrit input in SLP.

Following the naming convention used by Peter Scharf, we use "Roman" instead of "IAST" to show that, on top of supporting the full IAST character set, we support the extra distinctions within devanagari found in ISO 15919
In this filter, a list of non-Sanskrit and non-Devanagari characters are deleted.

See [here](src/main/java/io/bdrc/lucene/sa/Roman2SlpFilter.java) for the details.

### Deva2SlpFilter

Transcodes the devanagari sanskrit input in SLP.

This filter also normalizes non-Sanskrit Devanagari characters. Ex: क़ => क

## Resources

### Tries

SkrtWordTokenizer uses the data generated [here](https://github.com/BuddhistDigitalResourceCenter/sanskrit-stemming-data) as its lexical resources.

## Aknowledgements

 - https://gist.github.com/Akhilesh28/b012159a10a642ed5c34e551db76f236
 - http://sanskritlibrary.org/software/transcodeFile.zip (more specifically roman_slp1.xml)
 - https://en.wikipedia.org/wiki/ISO_15919#Comparison_with_UNRSGN_and_IAST
 - http://unicode.org/charts/PDF/U0900.pdf


## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
