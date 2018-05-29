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

import java.io.FileNotFoundException;
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
	
	static private void printTokenStream(TokenStream tokenStream) {
		try {
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
			while (tokenStream.incrementToken()) {
				termList.add(charTermAttribute.toString());
				System.out.println(charTermAttribute.toString() + " tokenType: " + typeAttribute.type());
			}
			System.out.println(String.join(" ", termList) + "\n");
		} catch (IOException e) {
			assertTrue(false);
		}
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
        List<String> expected = Arrays.asList(
                "yad√", "kulyA√", "kulya√", "sva√", "at√", "a✓", "ya√", "yas√", "yad√", "puMs√", "va✓", "tra✓", "sPurat√", 
                "va✓", "M❌", "kza√", "sPuwa√", "sPuw√", "ut√", "DvaMs√", "pra✓", "vi√", "tan√", "ya√", "yas√", "yad√", 
                "prajYa√", "anu√", "saj√", "ucita✓", "suKa✓", "manas√", "manasA√", "SAstf√", "tattva√", "arTa✓", "ArTa✓", 
                "Bartf√", "stabDa√", "han√", "na√", "ut√", "Cf❌", "sad√", "kAvya✓", "SrI√", "viroDa√", "buDa✓", "guRita✓", 
                "guRa√", "AjYA√", "A√", "han√", "eva✓", "kftvan√", "vidvas√", "lok√", "loka√", "avinASin√", "sPuw√", "bahu✓", 
                "kU√", "kIrti✓", "rAjya√", "Bunakti✓", "Ara√", "Arya√", "hi√", "eha√", "iti√", "upagu✓", "hi√", "Bu√", "BU√", 
                "BA√", "Ba√", "piSuna√", "utkarRita√", "roman√", "saBya√", "ut√", "Svas√", "Svasita√", "tulya✓", "kula✓", 
                "ja✓", "mlAna√", "an✓", "A√", "ut√", "vi√", "Ikzita√", "Ikzitf√", "ij√", "zi❌", "ta✓", "H❌", "snih√", "vi√", "vi√", 
                "A√", "al✓", "Alu✓", "li❌", "te✓", "na✓", "bAzpa✓", "guru√", "tattva√", "ij√", "Ikz√", "cakzus√", "yad√", "pitf√", 
                "BI√", "aBi✓", "DA√", "hita√", "ni✓", "rE√", "rA√", "Ikz√", "Ikza√", "ij√", "an✓", "i✓", "Kila√", "pA√", "evam✓", 
                "uru√", "iti✓", "dfz√", "karman√", "aneka√", "amat√", "uj✓", "sadfSa√", "adButa√", "udBid√", "na✓", "harza√", 
                "hfz√", "BU√", "Bu√", "BAva√", "BA√", "Ba√", "r❌", "AsvAdayat√", "kim√", "cid√", "vIra√", "vIrya√", "t❌", "utta✓", 
                "tap✓", "tAS✓", "ca✓", "kim√", "cit√", "cid√", "SaraRa√", "upaga✓", "tA❌", "ya√", "yas√", "yad√", "vftti√", "vftta√", 
                "praRAma√", "api√", "arti✓"
                );
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
        String input = "SrIH. tattvasaNgrahaH. paYjikAsametaH. tattvasaMgrahasya";
        Reader reader = new StringReader(input);
        System.out.println("0 " + input);
        TokenStream words = tokenize(reader, skrtWordTokenizer);
        List<String> expected = Arrays.asList("SrI√", "tattva✓", "saNgraha√", "paYjikA√", "am√", "H❌", "tattva✓", "saNgraha√");
        assertTokenStream(words, expected);
    }
	
    @Test
    public void bug5FreeBeforeFails() throws Exception {
        System.out.println("bug5");
        String input = "prathamaṃ manobhir anvāgatās sa-suta-bandhu-janās sametya|| mattebha-gaṇḍa-taṭa-vicyuta-dāna-bindu-sikt-opalācala-sahasra-vibhūṣaṇāyāḥ puṣpāvanamra-taru-ṣaṇḍa-vataṃsakāyā bhūmef puran tilaka-bhūtam idaṃ krameṇa|| taṭottha-vṛkṣa-cyuta-naika-puṣpa-vicitra-tīrānta-jalāni bhānti| praphulla-padmābharaṇāni yatra sarāṃsi kāraṇḍava-saṃkulāni|| vilola-vīcī-calitāravinda-patad-rajaḥ-piñjaritaiś ca hamsaiḥ sva-kesarodāra-bharāvabhugnaiḥ kvacit sarāṃsy amburuhaiś ca bhānti| sva-puṣpa-bhārāvanatair nnagendrair mmada-pragalbhāli-kula-svanaiś ca| ajasra-gābhiś ca purāṅganābhir vvanāni yasmin samalaṃkṛtāni|| calat-patākāny abalā-sanāthāny atyarttha-śuklāny adhikonnatāni| taḍil-latā-citra-sitābbhra-kūṭa-tulyopamānāni gṛhāṇi yatra|| kailāsa-tuṅga-śikhara-pratimāni cānyāny ābhānti dīrggha-valabhīni sa-vedikāni| gāndharvva-śabda-mukharāṇi niviṣṭa-citra-karmmāṇi lola-kadalī-vana-śobhitāni|| prāsāda-mālābhir alaṃkṛtāni dharāṃ vidāryyeva samutthitāni| vimāna-mālā-sadṛśāni yattra gṛhāṇi pūrṇṇendu-karāmalāni|| yad bhāty abhiramya-sarid-dvayena capalormmiṇā samupagūḍhaṃ rahasi kuca-śālinībhyāṃ prīti-ratibhyāṃ smarāṅgam iva|| satya-kṣamādama-śama-vrata-śauca-dhairyya-svāddhyāya-vṛtta-vinaya-sthiti-buddhy-upetaiḥ";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        while (words.incrementToken()) {}
    }
    
    @Test
    public void bug6DandaTakenAsToken() throws Exception {
        System.out.println("bug5");
        String input = "upetaiḥ.";
        CharFilter roman = new Roman2SlpFilter(new StringReader(input));
        CharFilter siddham = new SiddhamFilter(roman);
        CharFilter geminates = new GeminateNormalizingFilter(siddham);
        TokenStream words = tokenize(geminates, skrtWordTokenizer);
        List<String> expected = Arrays.asList("upa√", "ita√", "i√");
        assertTokenStream(words, expected);
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
		System.out.println("Legend:\n0: input string\n1: expected output\n2: actual output\n");
	}
}