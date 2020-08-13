# Lucene Analyzers for Sanskrit 

This repository contains bricks to implement a full analyzer pipeline in Lucene:

- filters to normalize and convert SLP1, Devanagari and IAST into SLP1
- indexation in SLP1 or simplified IAST with no diacritics (for lenient search)
- stopwords filter
- a syllable-based tokenizer
- a word tokenizer (that doesn't break compounds)

## Installation through maven:

```xml
    <dependency>
      <groupId>io.bdrc.lucene</groupId>
      <artifactId>lucene-sa</artifactId>
      <version>1.1.1</version>
    </dependency>
```

## Components

### SanskritAnalyzer

#### Constructors

```
    SanskritAnalyzer(String mode, String inputEncoding)
```
 - `mode`: `space`(tokenize at spaces), `syl`(tokenize in syllables) or `word`(tokenize in words)
 - `inputEncoding`: `SLP`(SLP1 encoding), `deva`(devanagari script) or `roman`(IAST)
 

```
    SanskritAnalyzer(String mode, String inputEncoding, String stopFilename)
    
```
 - `stopFilename`: path to the file, empty string (default list) or `null` (no stopwords)

```
    SanskritAnalyzer(String mode, String inputEncoding, boolean mergePrepositions, boolean filterGeminates, boolean normalizeAnusvara)
```
 - `mergePrepositions`: concatenates the token containing a preposition with the next one if true.
 - `filterGeminates`: normalize geminates (see [below](#geminatenormalizingfilter)) if `true`, else keep them as-is (default behavior)
 - `normalizeAnusvara`: normalize anusvara (see [below](#anusvaranormalizer)) if `true`, else keep them as-is (default behavior) 
 
```
    SanskritAnalyzer(String mode, String inputEncoding, boolean mergePrepositions, boolean filterGeminates, String lenient)
```
 - `lenient`: `index` or `query` (requires this information to select the correct filter pipeline) 

In all configurations except when lenient is activated, the output tokens of the analyzers are always encoded in SLP1.
Lenient analyzers output a drastically simplified IAST (see below for details).

#### Usecases
Three usecases are given as examples of possible configurations

##### 1. Regular search
A text in IAST can be tokenized in syllables for indexing. The queries are in SLP and tokenized in words. The default stopwords list is applied.
- Indexing:  `SanskritAnalyzer("syl", "roman")`
- Querying:  `SanskritAnalyzer("syl", "SLP")`

##### 2. Lenient search (syllables)
A text in IAST is indexed in syllables and the queries are split into syllables in the same way. Geminates and anusvara are normalized. The lenient search is enabled by indicating either "index" or "query", thereby selecting the appropriate pipeline of filters.
- Indexing:  `SanskritAnalyzer("syl", "roman", false, true, "index")` or simpler: `SanskritAnalyzer.IndexLenientSyl()`
- Querying:  `SanskritAnalyzer("syl", "roman", false, false, "query")` or simpler: `SanskritAnalyzer.QueryLenientSyl()`

### SkrtWordTokenizer (deprecated)

This tokenizer produces words through a Maximal Matching algorithm. It builds on top of [this Trie implementation](https://github.com/BuddhistDigitalResourceCenter/stemmer).

### SkrtSyllableTokenizer

Produces syllable tokens using the same syllabation rules found in Peter Scharf's [script](http://www.sanskritlibrary.org/Sanskrit/SanskritTransliterate/syllabify.html). 

### Stopword Filter

The [list of stopwords](src/main/resources/skrt-stopwords.txt) is [this list](https://gist.github.com/Akhilesh28/b012159a10a642ed5c34e551db76f236) encoded in SLP. The list must be formatted in the following way:

 - in SLP encoding
 - 1 word per line
 - empty lines (with and without comments), spaces and tabs are allowed
 - comments start with `#`
 - lines can end with a comment

### GeminateNormalizingFilter

Geminates of consonants around a `r` or `y` is commonly found in old documents. These can be normalized in order to be found more easily.

This filter applies the following simplification rules:

```  
    CCr   →  Cr 
    rCC   →  rC
    hCC   →  rC
    ṛCC   →  ṛC
    CCy   →  Cy
```

`C` is any consonant in the following list: [k g c j ṭ ḍ ṇ t d n p b m y v l s ś ṣ]
The second consonant can be the aspirated counterpart(ex: `rtth`), in which case the consonant that is kept is the aspirated one.
Thus, "arttha" is normalized to "artha",  "dharmma" to "dharma".

### AnusvaraNormalizer

Anusvara get normalized to:
- `n` before dentals
- `ṇ` before retroflex
- `ñ` before palatals
- `ṅ` before velars
- `m` otherwise
 
### Roman2SlpFilter

Transcodes the romanized sanskrit input in SLP.

Following the naming convention used by Peter Scharf, we use "Roman" instead of "IAST" to show that, on top of supporting the full IAST character set, we support the extra distinctions within devanagari found in ISO 15919
In this filter, a list of non-Sanskrit and non-Devanagari characters are deleted.

See [here](src/main/java/io/bdrc/lucene/sa/Roman2SlpFilter.java) for the details.

### Slp2RomanFilter

Transcodes the SLP input in IAST.

Outputs fully composed forms(single Unicode codepoints) instead of relying on extra codepoints for diacritics.

### Deva2SlpFilter

Transcodes the devanagari sanskrit input in SLP.

This filter also normalizes non-Sanskrit Devanagari characters. Ex: क़ => क

### Lenient Search Mode
`SanskritAnalyzer` in lenient mode outputs tokens encoded in simplified sanskrit instead of SLP.
 
This following transformations are applied to the IAST transcription:
 - all long vowels become short
 - all aspirated consonants become unaspirated
 - all remaining diacritics are removed
 - all geminates (or consonnant + aspirate) become simple consonnants

Keeping in the same spirit, these informal conventions are modified: 
 - `sh` (for `ś` or `ṣ`) becomes `s`
 - `v` becomes `b`
 - anusvaras are transformed into their equivalent

In terms of implementation, the input normalization happening in `Roman2SlpFilter` and `Deva2SlpFilter` is leveraged by always applying them first, then transforming SLP into *lenient Sanskrit*.  
Relying on `Roman2SlpFilter` has the additional benefit of correctly dealing with capital letters by lower-casing the input.

#### LenientCharFilter
Used at query time.

Expects SLP as input.
Applies the modifications listed above.

#### LenientTokenFilter
Used at index time.

Expects IAST as input. (`Slp2RomanFilter` can be used to achieve that)
Applies the modifications listed above. 

## Building from source

### Build the lexical resources for the Trie:

These steps need only be done once for a fresh clone of the repo; or simply run the `initialize.sh` script

 - make sure the submodules are initialized (`git submodule init`, then `git submodule update`), first from the root of the repo, then from `resources/sanskrit-stemming-data`
 - build lexical resources for the main trie: `cd resources/sanskrit-stemming-data/sandhify/ && python3 sandhifier.py`
 - build sandhi test tries: `cd resources/sanskrit-stemming-data/sandhify/ && python3 generate_test_tries.py`
     if you encounter a `ModuleNotFoundError: No module named 'click'` you may need to `python3 -m pip install click`
 - update other test tries with lexical resources: `cd src/test/resources/tries && python3 update_tries.py`
 - compile the main trie: `mvn exec:java -Dexec.mainClass="io.bdrc.lucene.sa.BuildCompiledTrie"` 
       (takes about 45mn on an average laptop). This step generally need only be run once 
       unless there are changes to the lexical resources for the main trie.
       If this step is run initially then it is sufficient to use the second base command 
       line form below.

The base command line to build a jar is either:

```
mvn clean compile exec:java package
```

which will build the main trie if it has not been built as indicated above, or:

```
mvn clean compile package
```

if the main trie has already been built.

The following options modify the package step:

- `-DincludeDeps=true` includes `io.bdrc.lucene:stemmer` in the produced jar file
- `-DperformRelease=true` signs the jar file with gpg

be aware that only one analyzer jar should have the `io.bdrc.lucene:stemmer` included when more 
than one of the BDRC analyzers are used together.

## Aknowledgements

 - https://gist.github.com/Akhilesh28/b012159a10a642ed5c34e551db76f236
 - http://sanskritlibrary.org/software/transcodeFile.zip (more specifically roman_slp1.xml)
 - https://en.wikipedia.org/wiki/ISO_15919#Comparison_with_UNRSGN_and_IAST
 - http://unicode.org/charts/PDF/U0900.pdf

## License

The code is Copyright 2017-2020 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
