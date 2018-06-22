# Lucene Analyzers for Sanskrit

## Building from source

### Build the lexical resources for the Trie:

 - make sure the submodules are initialized (`git submodule init`, then `git submodule update`), first from the root of the repo, then from `resources/sanskrit-stemming-data`
 - build lexical resources for the main trie: `cd resources/sanskrit-stemming-data/sandhify/ && python3 sandhifier.py`
 - build sandhi test tries: `cd resources/sanskrit-stemming-data/sandhify/ && python3 generate_test_tries.py`
 - update other test tries with lexical resources: `cd src/test/resources/tries && python3 update_tries.py`
 - compile the main trie: `io.bdrc.lucene.sa.BuildCompiledTrie.main()` (takes about 45mn on an average laptop)

The base command line to build a jar is:

```
mvn clean compile exec:java package
```

The following options alter the packaging:

- `-DincludeDeps=true` includes `io.bdrc.lucene:stemmer` in the produced jar file
- `-DperformRelease=true` signs the jar file with gpg

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

#### Parsing sample from Siddham Project data

Courtesy of Dániel Balogh, sample data from Siddham with the tokens produced.
`✓` is appended to found lemmas, `❌` where no match was found.

Limitations: 
 - project specific entries need to be fed in the lexical resources
 - the maximal-matching algorithm that is implemented makes it impossible to avoid wrong parsings such as `prajñānuṣaṅgocita` => `prajña✓ prajñā✓ | uṣa✓ | ṅ❌ ga✓`
 The reason being that `prajñān` is matched instead of `prajñā`, making it impossible to reconstruct `anuṣaṅga`.

```
yaḥ kulyaiḥ svai … #ātasa … yasya … … puṃva … tra … … sphuradvaṃ … kṣaḥ sphuṭoddhvaṃsita … pravitata
| yad✓ | kulyā✓ kulya✓ | sva✓ | at✓ | a❌ | ya✓ yas✓ yad✓ | puṁs✓ | va✓ | tra✓ | sphurat✓ | va✓ | ṁ❌ | kṣa✓ | sphuṭa✓ sphuṭ✓ | uddhvaṁs✓ | pravitan✓

 … yasya prajñānuṣaṅgocita-sukha-manasaḥ śāstra-tattvārttha-bharttuḥ … stabdho … hani … nocchṛ …
| ya✓ yas✓ yad✓ | prajña✓ prajñā✓ | uṣa✓ | ṅ❌ ga✓ | ucita✓ | sukha✓ | manas✓ manasā✓ | śāstṛ✓ | tattva✓ | artha✓ | bhartṛ✓ | stabdha✓ | han✓ | na✓ | uc✓ | chṛ✓ | 

sat-kāvya-śrī-virodhān budha-guṇita-guṇājñāhatān eva kṛtvā vidval-loke ’vināśi sphuṭa-bahu
sad✓ | kāvya✓ | śrī✓ | virodha✓ | budha✓ | guṇita✓ | guṇāj✓ | ñ❌ ah✓ | tad✓ | eva✓ | kṛtvan✓ kṛtvā✓ | vidvas✓ | lok✓ loka✓ | avināśin✓ | sphuṭ✓ | bahu✓

-kavitā-kīrtti rājyaṃ bhunakti āryyaihīty upaguhya bhāva-piśunair utkarṇṇitai romabhiḥ sabhyeṣūcchvasiteṣu
| kavitā✓ kū✓ | kīrti✓ | rājya✓ | bhunakti✓ | āra✓ ārya✓ | eha✓ | iti✓ | upagu✓ | hi✓ | bhū✓ bhu✓ bha✓ bhā✓ | piśuna✓ | utkarṇitai✓ | roman✓ | sabhya✓ | ut_śvas✓

tulya-kula-ja-mlānānanodvīkṣitaḥ sneha-vyāluḷitena bāṣpa-guruṇā tattvekṣiṇā cakṣuṣā yaḥ pitrābhihito nirīkṣya
| tulya✓ | kula✓ | ja✓ | mlāna✓ | an✓ | od❌ vī✓ | kṣita✓ kṣi✓ | snih✓ | vyālulita✓ | bāṣpa✓ | guru✓ | tattva✓ | ikṣin✓ | cakṣus✓ | yad✓ | pitṛ✓ | abhi_dhā✓ | niḥ_īkṣ✓

nikhilāṃ pāhy evam urvvīm iti dṛṣṭvā karmmāṇy anekāny amanuja-sadṛśāny adbhutodbhinna-harṣā bhāvair
| nikhila✓ | pā✓ pāhi✓ | evam✓ | urvī✓ uru✓ | iti✓ | dṛṣ✓ | karmāṇ✓ karman✓ | aneka✓ | amat✓ | uja❌ | sadṛśa✓ | adbhuta✓ | ut_bhid✓ | harṣa✓ hṛṣ✓ | bhu✓ bhū✓ bhāva✓ bhā✓ bha✓ | er❌ | 

āsvādayantaḥ … keciT vīryyottaptāś ca kecic charaṇam upagatā yasya
āsvādayat✓ ā_svād✓ | kim✓ | cid✓ | vīra✓ vīrya✓ | ut_tap✓ uttaptāḥ✓ | ca✓ | kim✓ | cit✓ cid✓ | śaraṇa✓ | upaga✓ | tā✓ | ya✓ yas✓ yad✓ | 

vṛtte praṇāme ’py artti##
vṛtti✓ vṛtta✓ | praṇāma✓ | api✓ | arti✓
```

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
