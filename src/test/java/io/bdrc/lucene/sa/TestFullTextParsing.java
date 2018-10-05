/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear 
 * below; otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the 
 * License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package io.bdrc.lucene.sa;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * Unit tests for the Sanskrit tokenizers and filters.
 */
public class TestFullTextParsing
{
	
	static SkrtWordTokenizer skrtWordTokenizer = fillWordTokenizer();
	
	static private SkrtWordTokenizer fillWordTokenizer() {
		try {
			skrtWordTokenizer = new SkrtWordTokenizer(true);
		} catch (Exception e) {
            e.printStackTrace();
        }
		return skrtWordTokenizer;
	}
	
	static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
		tokenizer.close();
		tokenizer.end();
		tokenizer.setReader(reader);
		tokenizer.reset();
		return tokenizer;
	}
	
	static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
		try {
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
			PartOfSpeechAttribute posAttribute= tokenStream.addAttribute(PartOfSpeechAttribute.class);
			while (tokenStream.incrementToken()) {
                if (typeAttribute.type().equals("non-word")) {
                    termList.add(charTermAttribute.toString()+"❌");
                } else if (typeAttribute.type().equals("word")) {
                    termList.add(charTermAttribute.toString()+"✓");
                } else if (typeAttribute.type().equals("lemma")) {
                    termList.add(charTermAttribute.toString()+"√");
                } 
				System.out.println(charTermAttribute.toString() + ", tokenType: " + typeAttribute.type()+ ", POS: " + posAttribute.getPartOfSpeech());
			}
			System.out.println("1 " + String.join(" ", expected));
			System.out.println("2 " + String.join(" ", termList) + "\n");
			assertThat(termList, is(expected));
		} catch (IOException e) {
			assertTrue(false);
		}
	}
	
    @Test
    public void withHyphens() throws Exception {
        System.out.println("bug1");
        String input = "yaḥ kulyaiḥ svai … #ātasa … yasya … … puṃva … tra … … sphuradvaṃ … kṣaḥ sphuṭoddhvaṃsita … pravitata … "
                + "yasya prajñānuṣaṅgocita-sukha-manasaḥ śāstra-tattvārttha-bharttuḥ … stabdho … hani … nocchṛ … sat-kāvya-śrī-virodhān "
                + "budha-guṇita-guṇājñāhatān eva kṛtvā vidval-loke ’vināśi sphuṭa-bahu-kavitā-kīrtti rājyaṃ bhunakti āryyaihīty upaguhya "
                + "bhāva-piśunair utkarṇṇitai romabhiḥ sabhyeṣūcchvasiteṣu tulya-kula-ja-mlānānanodvīkṣitaḥ sneha-vyāluḷitena bāṣpa-guruṇā "
                + "tattvekṣiṇā cakṣuṣā yaḥ pitrābhihito nirīkṣya nikhilāṃ pāhy evam urvvīm iti dṛṣṭvā karmmāṇy anekāny amanuja-sadṛśāny "
                + "adbhutodbhinna-harṣā bhāvair āsvādayantaḥ … keciT vīryyottaptāś ca kecic charaṇam upagatā yasya vṛtte praṇāme ’py artti##";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> expected = Arrays.asList("yad√", "kulyA√", "kulya√", "sva√", "sva√", "at√", "ya√", "yad√", "yas√", "puMs√", "va✓", "tra✓", 
                "sPurat√", "va✓", "M❌", "kza√", "sPuw√", "sPuwa√", "DvaMs√", "ut√", "pra√", "vi√", "tan√", "ya√", "yad√", "yas√", "prajYa√", 
                "anu√", "saj√", "ta✓", "ucita✓", "cita✓", "suKa√", "manasA√", "manas√", "SAstf√", "SAstra√", "ad√", "tattva√", "ArTa√", 
                "arTa√", "Bartf√", "stabDa√", "u❌", "han√", "na√", "ut√", "sad√", "sat√", "kAvya√", "SrI√", "irA√", "viroDa√", "Da✓", 
                "buDa√", "guRita√", "guRa√", "AjYA√", "A√", "han√", "eva√", "kftvan√", "va✓", "vidvas√", "lok√", "loka√", "avinASin√", "sPuw√", 
                "sPuwa√", "bahu√", "kU√", "a✓", "kIrti√", "rAjya√", "Bunakti√", "Ara√", "Arya√", "hi√", "eha√", "iti√", "upagu√", "hi√", "av√", 
                "BA√", "BA√", "BAva√", "BU√", "Ba√", "Bu√", "Suna√", "Svan√", "a✓", "Suna√", "Svan√", "utkarRita√", "roman√", "saBya√", 
                "vasita√", "ut√", "tulya✓", "ya✓", "kula√", "ja✓", "mlAna√", "an✓", "A√", "vi√", "ut√", "dU√", "dva√", "Ikzita√", "Ikzitf√", 
                "ij√", "kzi√", "kzita√", "nah√", "vi√", "A√", "vi√", "Alu√", "al✓", "lul√", "lulita√", "bAzpa√", "guru√", "tattva√", "Ikz√", "ij√", 
                "a✓", "cakzus√", "yad√", "pitf√", "BI√", "aBi√", "aBi√", "DA√", "hita√", "ni√", "rA√", "rE√", "Ikz√", "Ikza√", "ij√", "ya✓", 
                "Kila√", "niKila√", "pA√", "evam√", "uru√", "iti√", "iti√", "dfz√", "karman√", "aneka√", "amat√", "uj✓", "a✓", "sadfSa√", "sadfSa√", 
                "adButa√", "Bid√", "udBid√", "harza√", "harza√", "hfz√", "na√", "a✓", "BA√", "Ba√", "fz√", "av√", "BA√", "BA√", "BAva√", "BAva√", 
                "BU√", "Ba√", "Bu√", "AsvAdayat√", "kim√", "cid√", "cit√", "cit√", "vIra√", "vIrya√", "vIrya√", "tap√", "utta✓", "ca√", "kim√", 
                "cid√", "cit√", "aRa✓", "SaraRa√", "upaga✓", "tA✓", "ya√", "yad√", "yas√", "vftta√", "vftti√", "IraRa√", "ira✓", "praRAma√", "pA√", 
                "api√", "arti√");
        assertTokenStream(words, expected);
    }
    
	@Test
	public void bug1ExtraNonwordToken() throws Exception {
		System.out.println("bug1");
		String input = "ametaH";
		Reader reader = new StringReader(input);
    	System.out.println("0 " + input);
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		List<String> expected = Arrays.asList("am√", "H❌");
		assertTokenStream(words, expected);
	}
	
	@Test
	public void bug2missingNonWord() throws Exception {
		System.out.println("bug2");
		String input = ". tattvasaNgrahaH";
		Reader reader = new StringReader(input);
    	System.out.println("0 " + input);
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		List<String> expected = Arrays.asList("tattva✓", "saNgraha√");
		assertTokenStream(words, expected);
	}
	
	@Test
	public void bug3WrongTokenSize() throws Exception {
		System.out.println("bug3");
		String input = "sAtmIBUtam";
		Reader reader = new StringReader(input);
    	System.out.println("0 " + input);
		TokenStream words = tokenize(reader, skrtWordTokenizer);
		List<String> expected = Arrays.asList("sAtma√", "BU√", "BUta√");
		assertTokenStream(words, expected);
	}
	
    @Test
    public void bug4AlternativeSpelling() throws Exception {
        System.out.println("bug4");
        // TODO: we would expect that the non-word "H" after the token "ameta" is kept, 
        // but it seems to be deleted...
        String input = "SrIH. tattvasaNgrahaH. paYjikAsametaH. tattvasaMgrahasya";
        Reader reader = new StringReader(input);
        System.out.println("0 " + input);
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        List<String> expected = Arrays.asList("SrI√", "tattva✓", "saNgraha√", "paYjikA√", "am√", "tattva✓", "saNgraha√");
        assertTokenStream(words, expected);
    }
	
    @Test
    public void bug5FreeBeforeFails() throws Exception {
        System.out.println("bug5");
        String input = "vidāryyeva samutthitāni prathamaṃ manobhir anvāgatās sa-suta-bandhu-janās sametya|| mattebha-gaṇḍa-taṭa-"
                + "vicyuta-dāna-bindu-sikt-opalācala-sahasra-vibhūṣaṇāyāḥ puṣpāvanamra-taru-ṣaṇḍa-vataṃsakāyā bhūmef puran tilaka-"
                + "bhūtam idaṃ krameṇa|| taṭottha-vṛkṣa-cyuta-naika-puṣpa-vicitra-tīrānta-jalāni bhānti| praphulla-padmābharaṇāni "
                + "yatra sarāṃsi kāraṇḍava-saṃkulāni|| vilola-vīcī-calitāravinda-patad-rajaḥ-piñjaritaiś ca hamsaiḥ sva-kesarodāra-"
                + "bharāvabhugnaiḥ kvacit sarāṃsy amburuhaiś ca bhānti| sva-puṣpa-bhārāvanatair nnagendrair mmada-pragalbhāli-kula-"
                + "svanaiś ca| ajasra-gābhiś ca purāṅganābhir vvanāni yasmin samalaṃkṛtāni|| calat-patākāny abalā-sanāthāny atyarttha-"
                + "śuklāny adhikonnatāni| taḍil-latā-citra-sitābbhra-kūṭa-tulyopamānāni gṛhāṇi yatra|| kailāsa-tuṅga-śikhara-pratimāni "
                + "cānyāny ābhānti dīrggha-valabhīni sa-vedikāni| gāndharvva-śabda-mukharāṇi niviṣṭa-citra-karmmāṇi lola-kadalī-vana-"
                + "śobhitāni|| prāsāda-mālābhir alaṃkṛtāni dharāṃ vidāryyeva samutthitāni| vimāna-mālā-sadṛśāni yattra gṛhāṇi pūrṇṇendu-"
                + "karāmalāni|| yad bhāty abhiramya-sarid-dvayena capalormmiṇā samupagūḍhaṃ rahasi kuca-śālinībhyāṃ prīti-ratibhyāṃ "
                + "smarāṅgam iva|| satya-kṣamādama-śama-vrata-śauca-dhairyya-svāddhyāya-vṛtta-vinaya-sthiti-buddhy-upetaiḥ";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        while (words.incrementToken()) {}
    }    
    
    @Test
    public void bug6DandaTakenAsToken() throws Exception {
        System.out.println("bug6");
        String input = "upetaiḥ.";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> expected = Arrays.asList("upa√", "i√", "ita√", "tad√", "H❌");
        assertTokenStream(words, expected);
    }
    
    @Test
    public void bug7() throws Exception {
        System.out.println("bug7");
        String input = "tayorbhedo—viśeṣaḥ .";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> expected = Arrays.asList("tad√", "Urj√", "Beda√", "viSeza√");
        assertTokenStream(words, expected);
    }

    @Test
    public void bug8() throws Exception {
        System.out.println("bug8");
        String input = "grahaṇam—anyāpohasya—anyavyavacchedasya";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> expected = Arrays.asList("grahaRa√", "an√", "anya√", "anya√", "Ap√", "Apa√", "apoha√", "pA√", 
                "pa√", "an√", "anya√", "han√", "vyavacCeda√", "avI√", "av√", "avi√", "avacCeda√", "va✓", "c❌", "Ceda√");
        assertTokenStream(words, expected);
    }

    @Test
    public void bug9bodhi() throws Exception {
        System.out.println("bug9");
        String input = "bodhisattvacaryāvatāra - "
                + "Śāntideva - "
                + "mañjuśrī nāma saṃgīti - "
                + "mañjuśrījñānasattvasya paramārtha nāma saṃgīti - "
                + "Nāmasaṃgīti - "
                + "bodhicaryāvatāra"
                + "Prajñāpāramitā";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        // "apapāramita√" found because second non-max token apa is a Preposition, which is then prepended to "pāramita√" token.
        List<String> expected = Arrays.asList("bodhisattva√", "caryā√", "avatāra√", "śāntideva√", "mañjuśrī√", "āma√", "am√", 
                "nāman√", "samgīti√", "mañjuśrī√", "āna√", "an√", "jñāna√", "sattva√", "ara√", "parama√", "ārtha√", "artha√", 
                "nāman√", "samgīti√", "nāman√", "samgīti√", "bodhi√", "bodhin√", "caryā√", "avatāra√", "prajñā√", "prajña√", "āp√", 
                "āpa√", "apa√", "apapāramita√");
        assertTokenStream(words, expected);
    }
    
    @Test
    public void bug10Shri() throws Exception {
        System.out.println("bug10");
        String input = "śrījñāna"; // "mañjuśrījñāna";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> expected = Arrays.asList("SrI√", "IjY❌", "Ana√", "an√", "jYAna√");
        assertTokenStream(words, expected);
    }

    @Test
    public void bug11SameInputDifferingOutput() throws Exception {
        System.out.println("bug11");
        String input = "nirīkṣya nirīkṣya";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> expected = Arrays.asList("ni√", "rA√", "rE√", "Ikz√", "Ikza√", "ij√", "ya✓", "ir❌", "Ikz√", 
                "Ikza√", "ni√", "rA√", "rE√", "akza√", "akzan√", "Ikz√", "Ikza√", "ij√", "ya✓");
        assertTokenStream(words, expected);
    }
    
    @Test
    public void bug12MissingTokenAndRollingBufferError() throws Exception {
        System.out.println("bug12");
        String input = "nāma saṃgīti paramārtha nāma saṃgīti -";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> expected = Arrays.asList("nAman√", "sam√", "gIti√", "parama√", "ArTa√", "arTa√", 
                "nAman√", "sam√", "gIti√");
        assertTokenStream(words, expected);
    }
    
    @Test
    public void bug13InfiniteLoopAtDash() throws Exception {
        System.out.println("bug13");
        String input = "caryāvatāra - Śāntideva";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        List<String> expected = Arrays.asList("caryā√", "avatāra√", "śāntideva√");
        assertTokenStream(words, expected);
    }

    @Test
    public void bug14InfiniteLoop() throws Exception {
        System.out.println("bug14");
        String input = "padmapāṇidhāraṇī.";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        List<String> expected = Arrays.asList("padma√", "pāṇi√", "dhāraṇa√");
        assertTokenStream(words, expected);
    }
    
    @Test
    public void bug15() throws Exception {
        System.out.println("bug14");
        String input = "sāmānyadharmacaryā.";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        List<String> expected = Arrays.asList("sāmānya√", "sāman√", "adha√", "dharma√", "dharman√", 
                "ṛc√", "mara√", "maryā√", "marya√", "car√", "cara√", "caryā√", "carya√");
        assertTokenStream(words, expected);
    }
    
    @Test
    public void bug16() throws Exception {
        System.out.println("bug14");
        String input = "guhyāpannapattrikā.";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        List<String> expected = Arrays.asList("guh√", "guha√", "guhya√", "āp√", "apa√", 
                "apapanna✓", "atri√", "kim√");  // apapanna = apa(Preposition) + panna(Verb)
        assertTokenStream(words, expected);
    }

    @Test
    public void bug17() throws Exception {
        System.out.println("bug wrong offsets");
        String input = "vajrapāṇinīlāmbara tantra śrī mahākālīdevīstotrāṣṭaka-nāma";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        words = new PrepositionMergingFilter(words);
        words = new Slp2RomanFilter(words);
        List<String> expected = Arrays.asList("vajra√", "pāṇi√", "pāṇini√", "ilā√", "ban❌", "tra✓", 
                "śrī√", "ahan√", "mah√", "mahat√", "kāla√", "īdādeva√", "i√", "ita√", "vā√", "rā√", 
                "vaś√", "aṣṭaka√", "ma✓", "āāma√", "am√");
        assertTokenStream(words, expected);
    }    
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
}