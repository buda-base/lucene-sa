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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.lucene.sa.SkrtWordTokenizer;

/**
 * Unit tests for the sandhied combinations
 * 
 * Each combination must be tested independently since isSandhiedCombination() returns
 * at the first corresponding combination.
 */
public class SandhiedCombinationTests
{
	
	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
	}
	
    @Test
    public void testVowelSandhiCombination1() throws IOException
    {
    	char[] sandhiedString = "mAstu".toCharArray(); // mA astu => mAstu
    	int sandhiStartIdx = 1;
    	String sandhiedSubString = "A";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 1}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testVowelSandhiCombination2() throws IOException
    {
    	char[] sandhiedString = "naiti".toCharArray(); // na eti => naiti
    	int sandhiStartIdx = 1;
    	String sandhiedSubString = "ai";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 2}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
    public void testVowelSandhiCombination3() throws IOException
    {
    	char[] sandhiedString = "ta uvAca".toCharArray(); // te uvAca => ta uvAca
    	int sandhiStartIdx = 1;
    	String sandhiedSubString = "au";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 3}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testConsonantSandhi1Combination1() throws IOException
    {
    	char[] sandhiedString = "vAk".toCharArray(); // vAc => vAk 
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "k";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 1}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
    public void testConsonantSandhi1Combination2() throws IOException
    {
    	char[] sandhiedString = "tallokaH".toCharArray(); // tat lokaH => tallokaH 
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "ll";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 2}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
	public void testConsonantSandhi1VowelsCombination1() throws IOException
	{
    	char[] sandhiedString = "vAgBaqa".toCharArray(); // vAk Baqa => vAgBaqa 
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "AgB";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 2}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
	}
    
    @Test
	public void testConsonantSandhi1VowelsCombination2() throws IOException
	{
    	char[] sandhiedString = "atmannatman".toCharArray(); // atman atman => atmannatman 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "anna";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 3}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
	}

    @Test
	public void testConsonantSandhi1VowelsCombination3() throws IOException
	{
    	char[] sandhiedString = "atmann atman".toCharArray(); // atman atman => atmann atman 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "anna";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 4}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
	}

    @Test
    public void testConsonantSandhi2Combination1() throws IOException
    {
    	char[] sandhiedString = "vANmayaH".toCharArray(); // vAk mayaH => vANmayaH
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "Nm";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 2}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testConsonantSandhi2Combination2() throws IOException
    {
    	char[] sandhiedString = "vAN mayaH".toCharArray(); // vAk mayaH => vANmayaH
    	int sandhiStartIdx = 2;
    	String sandhiedSubString = "Nm";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 3}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
    public void testConsonantSandhi2Combination3() throws IOException
    {
    	char[] sandhiedString = "asmiMzwIkA".toCharArray(); // asmin wIkA => asmiMzwIkA
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "Mzw";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 3}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testConsonantSandhi2Combination4() throws IOException
    {
    	char[] sandhiedString = "asmiM zwIkA".toCharArray(); // asmin wIkA => asmiMzwIkA
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "Mzw";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 4}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi1Combination1() throws IOException
    {
    	char[] sandhiedString = "rAmastu".toCharArray(); // rAmaH tu => rAmastu 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "as";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 1}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi1Combination2() throws IOException
    {
    	char[] sandhiedString = "devAeva".toCharArray(); // devAH eva => devAeva 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "Ae";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 1}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi1Combination3() throws IOException
    {
    	char[] sandhiedString = "devA eva".toCharArray(); // devAH eva => devAeva 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "Ae";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 2}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi1Combination4() throws IOException
    {
    	char[] sandhiedString = "munir vadati".toCharArray(); // muniH vadati => munir vadati 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "irv";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 3}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
    public void testVisargaSandhi2Combination1() throws IOException
    {
    	char[] sandhiedString = "rAmogacCati".toCharArray(); // rAmaH gacCati => rAmogacCati 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "og";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 1}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi2Combination2() throws IOException
    {
    	char[] sandhiedString = "rAmo gacCati".toCharArray(); // rAmaH gacCati => rAmo gacCati 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "og";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 2}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi2Combination3() throws IOException
    {
    	char[] sandhiedString = "rAmorgacCati".toCharArray(); // rAmoH gacCati => rAmorgacCati 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "org";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 2}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }

    @Test
    public void testVisargaSandhi2Combination4() throws IOException
    {
    	char[] sandhiedString = "rAmor gacCati".toCharArray(); // rAmoH gacCati => rAmor gacCati 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "org";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 3}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
    public void testAbsoluteFinalSandhiCombination() throws IOException
    {
    	char[] sandhiedString = "suhRt".toCharArray(); // suhRd => suhRt
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "t";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 1}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
    public void testAbsoluteFinalConsonantClustersCombination() throws IOException
    {
    	char[] sandhiedString = "bhagavan".toCharArray(); // bhagavant => bhagavan
    	int sandhiStartIdx = 7;
    	String sandhiedSubString = "n";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 1}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
    public void testcCWordsCombination() throws IOException
    {
    	char[] sandhiedString = "rAmasya cCAtraH".toCharArray(); // rAmasya CAtraH => rAmasya cCAtraH
    	int sandhiStartIdx = 6;
    	String sandhiedSubString = "acC";
    	int[][] vowelSandhiCombinations1 = new int[][]{{0, 4}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
    @Test
    public void testPunarSandhiCombination() throws IOException
    {
    	char[] sandhiedString = "punaH punaH".toCharArray(); // punar punar => punaH punaH 
    	int sandhiStartIdx = 4;
    	String sandhiedSubString = "aHp";
    	int[][] vowelSandhiCombinations1 = new int[][]{{-1, 3}};
    	boolean res = SkrtWordTokenizer.isSandhiedCombination(sandhiedString, sandhiStartIdx, sandhiedSubString, vowelSandhiCombinations1); 
    	assertTrue(res);
    }
    
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}

}