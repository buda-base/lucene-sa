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

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.util.RollingCharBuffer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.lucene.sa.SkrtWordTokenizer;

/**
 * Unit tests for the sandhied combinations
 * 
 * Each combination must be tested as the only possible one since isSandhiedCombination() 
 * returns at the first corresponding combination.
 */
public class TestSandhiedCombinations
{
	
	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
	}
	
    @Test
    public void testVowelSandhiCombination1() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("mAstu")); // mA astu => mAstu
    	sandhied.get(0);
    	int sandhiStartIdx = 1;
    	String sandhiedSubString = "A";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0);
    	assertTrue(res);
    }

    @Test
    public void testVowelSandhiCombination2() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("naiti")); // na eti => naiti
    	sandhied.get(0);
    	int sandhiStartIdx = 1;
    	String sandhiedSubString = "ai";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }
    
    @Test
    public void testVowelSandhiCombination3() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("ta uvAca")); // te uvAca => ta uvAca
    	sandhied.get(0);
    	sandhied.get(0);
    	int sandhiStartIdx = 1;
    	String sandhiedSubString = "au";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }

    @Test
    public void testConsonantSandhi1Combination1() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("vAk")); // vAc => vAk 
    	sandhied.get(0);
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "k";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }
    
    @Test
    public void testConsonantSandhi1Combination2() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("tallokaH")); // tat lokaH => tallokaH 
    	sandhied.get(0);
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "ll";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }
    
    @Test
	public void testConsonantSandhi1VowelsCombination1() throws IOException
	{
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("vAgBaqa")); // vAk Baqa => vAgBaqa 
    	sandhied.get(0);
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "AgB";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
	}
    
    @Test
	public void testConsonantSandhi1VowelsCombination2() throws IOException
	{
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("atmannatman")); // atman atman => atmannatman 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "anna";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
	}

    @Test
	public void testConsonantSandhi1VowelsCombination3() throws IOException
	{
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("atmann atman")); // atman atman => atmann atman 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "anna";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
	}

    @Test
    public void testConsonantSandhi2Combination1() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("vANmayaH")); // vAk mayaH => vANmayaH
    	sandhied.get(0);
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "Nm";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }

    @Test
    public void testConsonantSandhi2Combination2() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("vAN mayaH")); // vAk mayaH => vANmayaH
    	sandhied.get(0);
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "Nm";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }
    
    @Test
    public void testConsonantSandhi2Combination3() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("asmiMzwIkA")); // asmin wIkA => asmiMzwIkA
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "Mzw";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }

    @Test
    public void testConsonantSandhi2Combination4() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("asmiM zwIkA")); // asmin wIkA => asmiMzwIkA
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "Mzw";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi1Combination1() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("rAmastu")); // rAmaH tu => rAmastu 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "as";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi1Combination2() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("devAeva")); // devAH eva => devAeva 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "Ae";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi1Combination3() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("devA eva")); // devAH eva => devAeva 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "Ae";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi1Combination4() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("munir vadati")); // muniH vadati => munir vadati 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "irv";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
    }
    
    @Test
    public void testVisargaSandhi2Combination1() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("rAmogacCati")); // rAmaH gacCati => rAmogacCati 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "og";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi2Combination2() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("rAmo gacCati")); // rAmaH gacCati => rAmo gacCati 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "og";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi2Combination3() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("rAmorgacCati")); // rAmoH gacCati => rAmorgacCati 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "org";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1);
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi2Combination4() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("rAmor gacCati")); // rAmoH gacCati => rAmor gacCati 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "org";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -1); 
    	assertTrue(res);
    }
    
    @Test
    public void testAbsoluteFinalSandhiCombination() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("suhRt")); // suhRd => suhRt
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "t";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }
    
    @Test
    public void testAbsoluteFinalConsonantClustersCombination() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("bhagavan")); // bhagavant => bhagavan
    	sandhied.get(0);
    	int sandhiStartIdx = 7;
    	String sandhiedSubString = "n";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }
    
    @Test
    public void testcCWordsCombination1() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("rAmasyacCAtraH")); // rAmasya CAtraH => rAmasyacCAtraH
    	sandhied.get(0);
    	int sandhiStartIdx = 6;
    	String sandhiedSubString = "acC";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }
    
    @Test
    public void testcCWordsCombination2() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("rAmasya cCAtraH")); // rAmasya CAtraH => rAmasya cCAtraH
    	sandhied.get(0);
    	int sandhiStartIdx = 6;
    	String sandhiedSubString = "acC";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, 0); 
    	assertTrue(res);
    }
    
    @Test
    public void testPunarSandhiCombination1() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("punaHpunaH")); // punar punar => punaHpunaH 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "punaHp";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -4); 
    	assertTrue(res);
    }
    
    @Test
    public void testPunarSandhiCombination2() throws IOException
    {
    	RollingCharBuffer sandhied = new RollingCharBuffer();
    	sandhied.reset(new StringReader("punaH punaH")); // punar punar => punaH punaH 
    	sandhied.get(0);
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "punaHp";
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhied, sandhiStartIdx, sandhiedSubString, -4); 
    	assertTrue(res);
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}

}