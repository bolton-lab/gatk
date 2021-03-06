/*
* Copyright 2012-2016 Broad Institute, Inc.
* 
* Permission is hereby granted, free of charge, to any person
* obtaining a copy of this software and associated documentation
* files (the "Software"), to deal in the Software without
* restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the
* Software is furnished to do so, subject to the following
* conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
* OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
* THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.broadinstitute.gatk.tools.walkers.indels;

import org.broadinstitute.gatk.engine.walkers.WalkerTest;
import org.broadinstitute.gatk.utils.exceptions.UserException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IndelRealignerIntegrationTest extends WalkerTest {

    private static final String mainTestBam = validationDataLocation + "indelRealignerTest.pilot1.ceu.fixed.fixmates.bam";
    private static final String mainTestIntervals = validationDataLocation + "indelRealignerTest.pilot1.ceu.intervals";
    private static final String knownIndels = validationDataLocation + "indelRealignerTest.pilot1.ceu.vcf";
    private static final String baseCommandPrefix = "-T IndelRealigner -noPG -R " + b36KGReference + " -I " + mainTestBam + " -targetIntervals " + mainTestIntervals + " -compress 0 -L 20:49,500-55,500 ";
    private static final String baseCommand = baseCommandPrefix + "-o %s ";
    private static final String base_md5 = "12e7c9fd7af4fc9184c5f58a1660eac5";
    private static final String base_md5_with_SW_or_VCF = "2d3f79298687da007da52286b5c7261d";

    @Test
    public void testDefaults() {

        WalkerTestSpec spec1 = new WalkerTestSpec(
                baseCommand,
                1,
                Arrays.asList(base_md5));
        executeTest("test realigner defaults", spec1);

        WalkerTestSpec spec2 = new WalkerTestSpec(
                baseCommand + "-known " + knownIndels,
                1,
                Arrays.asList(base_md5_with_SW_or_VCF));
        executeTest("test realigner defaults with VCF", spec2);
    }

    @Test
    public void testKnownsOnly() {
        WalkerTestSpec spec1 = new WalkerTestSpec(
                baseCommand + "--consensusDeterminationModel KNOWNS_ONLY -known " + knownIndels,
                1,
                Arrays.asList("3d028025dcb8d268262274d8ffc42635"));
        executeTest("realigner known indels only from VCF", spec1);
    }

    @Test
    public void testUseSW() {
        WalkerTestSpec spec1 = new WalkerTestSpec(
                baseCommand + "--consensusDeterminationModel USE_SW -known " + knownIndels,
                1,
                Arrays.asList(base_md5_with_SW_or_VCF));
        executeTest("realigner use SW from VCF", spec1);
    }

    @Test
    public void testLods() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put("-LOD 60", base_md5);
        e.put( "-LOD 1 --consensusDeterminationModel USE_SW",  "44868da9b026201572cbfaaedacc57eb" );

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                    baseCommand + entry.getKey(),
                    1,
                    Arrays.asList(entry.getValue()));
            executeTest(String.format("realigner [%s]", entry.getKey()), spec);
        }
    }

    @Test
    public void testLongRun() {
        WalkerTestSpec spec = new WalkerTestSpec(
                "-T IndelRealigner -noPG -R " + b36KGReference + " -I " + validationDataLocation + "NA12878.chrom1.SLX.SRP000032.2009_06.bam -L 1:10,000,000-11,000,000 -targetIntervals " + validationDataLocation + "indelRealignerTest.NA12878.chrom1.intervals -compress 0 -o %s",
                1,
                Arrays.asList("c40aa32bca520015acb175fde52b4ed4"));
        executeTest("realigner long run", spec);
    }

    @Test
    public void testNoTags() {
        WalkerTestSpec spec = new WalkerTestSpec(
                baseCommand + "--noOriginalAlignmentTags --consensusDeterminationModel USE_SW",
                1,
                Arrays.asList("3adc7711a163a65a570a47fe28eb4d24"));
        executeTest("realigner no output tags", spec);
    }

    @Test
    public void testStats() {
        WalkerTestSpec spec1 = new WalkerTestSpec(
                baseCommandPrefix + "-stats %s -o /dev/null",
                1,
                Arrays.asList("7ed8d4eed635613fd031598a5c9ef5a3"));
        executeTest("realigner stats", spec1);

        WalkerTestSpec spec2 = new WalkerTestSpec(
                baseCommandPrefix + "-LOD 60 -stats %s -o /dev/null",
                1,
                Arrays.asList("e8b02bfc5debec55fe936a38c59463cc"));
        executeTest("realigner stats", spec2);
    }

    @Test
    public void testMaxReadsInMemory() {
        HashMap<String, String> e = new HashMap<String, String>();
        e.put("--maxReadsInMemory 10000", "b8a4491506303dc96cf105ba069dd928");
        e.put( "--maxReadsInMemory 40000", base_md5 );

        for ( Map.Entry<String, String> entry : e.entrySet() ) {
            WalkerTest.WalkerTestSpec spec = new WalkerTest.WalkerTestSpec(
                    baseCommand + entry.getKey(),
                    1,
                    Arrays.asList(entry.getValue()));
            executeTest(String.format("realigner [%s]", entry.getKey()), spec);
        }
    }

    @Test
    public void testNWayOut() {
        WalkerTestSpec spec1 = new WalkerTestSpec(
                baseCommandPrefix + " -nWayOut .clean.bam ",
                1,
                Arrays.asList("d41d8cd98f00b204e9800998ecf8427e"));
        executeTest("test realigner nWayOut", spec1);
    }

    @Test
    public void testBadCigarStringDoesNotFail() {
        // Just making sure the test runs without an error, don't care about the MD5 value
        WalkerTestSpec spec = new WalkerTestSpec(
                "-T IndelRealigner -R " + b37KGReference + " -I " + privateTestDir + "Realigner.error.bam -L 19:5787200-5787300 -targetIntervals 19:5787205-5787300 -o %s",
                1,
                Arrays.asList(""));
        executeTest("test bad cigar string does not fail", spec);
    }
}
