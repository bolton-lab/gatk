package org.broadinstitute.sting.gatk.refdata;

import net.sf.samtools.util.SequenceUtil;
import org.broadinstitute.sting.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Example format:
 * 585 chr1 433 433 rs56289060  0  +  - - -/C  genomic  insertion unknown 0  0  unknown  between  1
 * 585 chr1 491 492 rs55998931  0  +  C C C/T  genomic  single   unknown 0 0 unknown exact 1
 * <p/>
 * User: mdepristo
 * Date: Feb 27, 2009
 * Time: 10:47:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class rodDbSNP extends BasicReferenceOrderedDatum implements VariationRod {

    public static final String STANDARD_DBSNP_TRACK_NAME = "dbsnp";

    public GenomeLoc loc;       // genome location of SNP
    // Reference sequence chromosome or scaffold
    // Start and stop positions in chrom

    public String RS_ID;        // Reference SNP identifier or Affy SNP name
    public String strand;      // Which DNA strand contains the observed alleles

    public String refBases;        // the reference base according to NCBI, in the dbSNP file
    public String observed;    // The sequences of the observed alleles from rs-fasta files

    public String molType;     // Sample type from exemplar ss
    public String varType;     // The class of variant (simple, insertion, deletion, range, etc.)
    // Can be 'unknown','single','in-del','het','microsatellite','named','mixed','mnp','insertion','deletion'
    public String validationStatus;    // The validation status of the SNP
    // one of set('unknown','by-cluster','by-frequency','by-submitter','by-2hit-2allele','by-hapmap')

    public double avHet;        // The average heterozygosity from all observations
    public double avHetSE;      // The Standard Error for the average heterozygosity

    public String func;         // The functional category of the SNP (coding-synon, coding-nonsynon, intron, etc.)
    // set('unknown','coding-synon','intron','cds-reference','near-gene-3','near-gene-5',
    // 'nonsense','missense','frameshift','untranslated-3','untranslated-5','splice-3','splice-5')
    public String locType;      // How the variant affects the reference sequence
    // enum('range','exact','between','rangeInsertion','rangeSubstitution','rangeDeletion')

    public int weight;          // The quality of the alignment

    // cache the allele list so it doesn't need to get recomputed each time
    private List<String> alleleList = null;


    // ----------------------------------------------------------------------
    //
    // Constructors
    //
    // ----------------------------------------------------------------------
    public rodDbSNP(final String name) {
        super(name);
    }

    // ----------------------------------------------------------------------
    //
    // manipulating the SNP information
    //
    // ----------------------------------------------------------------------
    public GenomeLoc getLocation() {
        return loc;
    }

    /**
     * get the reference base(s) at this position
     *
     * @return the reference base or bases, as a string
     */
    public String getReference() {
        return refBases;
    }

    /**
     * get the -1 * (log 10 of the error value)
     *
     * @return the log based error estimate
     */
    public double getNegLog10PError() {
        return 4; // -log10(0.0001)
    }

    /**
     * gets the alternate alleles.  This method should return all the alleles present at the location,
     * NOT including the reference base.  This is returned as a string list with no guarantee ordering
     * of alleles (i.e. the first alternate allele is not always going to be the allele with the greatest
     * frequency).
     *
     * @return an alternate allele list
     */
    public List<String> getAlternateAlleleList() {
        List<String> ret = new ArrayList<String>();
        for (String allele : getAlleleList())
            if (!allele.equals(getReference())) ret.add(allele);
        return ret;
    }

    /**
     * gets the alleles.  This method should return all the alleles present at the location,
     * including the reference base.  The first allele should always be the reference allele, followed
     * by an unordered list of alternate alleles.
     *
     * @return an alternate allele list
     */
    public List<String> getAlleleList() {
        if ( alleleList == null ) {
            // add ref first
            if ( onFwdStrand() )
                alleleList = Arrays.asList(observed.split("/"));
            else
                alleleList = Arrays.asList(SequenceUtil.reverseComplement(observed).split("/"));
            if ( alleleList.size() > 0 && alleleList.contains(getReference()) && !alleleList.get(0).equals(this.getReference()) )
                Collections.swap(alleleList, alleleList.indexOf(getReference()), 0);
        }

        return alleleList;
    }

    public boolean onFwdStrand() {
        return strand.equals("+");
    }

    /**
     * get the frequency of this variant
     *
     * @return VariantFrequency with the stored frequency
     */
    public double getNonRefAlleleFrequency() {
        return 0;  // dbSNP doesn't know the allele frequency
    }

    /** @return the VARIANT_TYPE of the current variant */
    public VARIANT_TYPE getType() {
        return VARIANT_TYPE.SNP;
    }// ----------------------------------------------------------------------

    //
    // What kind of variant are we?
    //
    // ----------------------------------------------------------------------
    public boolean isSNP() {
        return varType.contains("single") && locType.contains("exact");
    }

    public boolean isInsertion() {
        return varType.contains("insertion");
    }

    public boolean isDeletion() {
        return varType.contains("deletion");
    }

    public boolean isIndel() {
        return isInsertion() || isDeletion() || varType.contains("in-del");
    }

    /**
     * gets the alternate base is the case of a SNP.  Throws a StingException when we can't parse out a
     * single base to return, the site isn't a snp, or the site isn't biallelic
     *
     * @return a char, representing the alternate base
     */
    public char getAlternativeBaseForSNP() {
        if (!isSNP())  throw new StingException("We're not a SNP; called in DbSNP rod at position " + this.loc);
        if (!isBiallelic()) throw new StingException("We're not biallelic; at position " + this.loc);
        List<String> ret = this.getAlternateAlleleList();
        if (ret.size() == 1 && ret.get(0).length() == 1)
            return ret.get(0).charAt(0);
        throw new StingException("getAlternativeBaseForSNP failed for DbSNP rod " + this.loc);
    }

    /**
     * gets the reference base is the case of a SNP.  Throws a StingException if we're not a SNP.
     *
     * @return a char, representing the alternate base
     */
    public char getReferenceForSNP() {
        if (!isSNP())  throw new StingException("We're not a SNP; called in DbSNP rod at position " + this.loc);
        if (refBases.length() != 1) throw new StingException("The reference base in DbSNP must be zero, at position " + this.loc + " was " + refBases);
        return refBases.charAt(0); // we know it's length 1, this is ok
    }

    public boolean isReference() {
        return false; // snp locations are never "reference", there's always a variant
    }

    public boolean isHapmap() {
        return validationStatus.contains("by-hapmap");
    }

    public boolean is2Hit2Allele() {
        return validationStatus.contains("by-2hit-2allele");
    }

    // ----------------------------------------------------------------------
    //
    // formatting
    //
    // ----------------------------------------------------------------------

    public String getRS_ID() { return RS_ID; }    

    public String toString() {
        return String.format("%s\t%d\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%f\t%f\t%s\t%s\t%d",
                             getLocation().getContig(), getLocation().getStart(), getLocation().getStop() + 1,
                             RS_ID, strand, refBases, observed, molType,
                             varType, validationStatus, avHet, avHetSE, func, locType, weight);
    }

    public String toSimpleString() {
        return String.format("%s:%s:%s", RS_ID, observed, strand);
    }

    public String toMediumString() {
        String s = String.format("%s:%s:%s", getLocation().toString(), RS_ID, Utils.join("",this.getAlleleList()));
        if (isSNP()) s += ":SNP";
        if (isIndel()) s += ":Indel";
        if (isHapmap()) s += ":Hapmap";
        if (is2Hit2Allele()) s += ":2Hit";
        return s;
    }

    public String repl() {
        return String.format("%d\t%s\t%d\t%d\t%s\t0\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%f\t%f\t%s\t%s\t%d",
                             585, getLocation().getContig(), getLocation().getStart() - 1, getLocation().getStop(),
                             RS_ID, strand, refBases, refBases, observed, molType,
                             varType, validationStatus, avHet, avHetSE, func, locType, weight);
    }

    public boolean parseLine(final Object header, final String[] parts) {
        try {
            String contig = parts[1];
            long start = Long.parseLong(parts[2]) + 1; // The final is 0 based
            long stop = Long.parseLong(parts[3]) + 1;  // The final is 0 based
            loc = GenomeLocParser.parseGenomeLoc(contig, start, Math.max(start, stop - 1));

            RS_ID = parts[4];
            strand = parts[6];
            refBases = parts[7];
            //if (strand.equals("-")) // this is just wrong lol
            //    refBases = BaseUtils.simpleReverseComplement(refBases);
            observed = parts[9];
            molType = parts[10];
            varType = parts[11];
            validationStatus = parts[12];
            avHet = Double.parseDouble(parts[13]);
            avHetSE = Double.parseDouble(parts[14]);
            func = parts[15];
            locType = parts[16];
            weight = Integer.parseInt(parts[17]);
            //System.out.printf("Parsed %s%n", toString());
            return true;
        } catch (MalformedGenomeLocException ex) {
            // Just rethrow malformed genome locs; the ROD system itself will deal with these.
            throw ex;
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Just rethrow malformed genome locs; the ROD system itself will deal with these.
            throw new RuntimeException("Badly formed dbSNP line: " + ex);
        } catch (RuntimeException e) {
            System.out.printf("  Exception caught during parsing DBSNP line %s%n", Utils.join(" <=> ", parts));
            throw e;
        }
    }

    public double getHeterozygosity() {
        return avHet;
    }

    public int getPloidy() throws IllegalStateException {
        return 2; // our DbSNP assumes a diploid human
    }

    public boolean isBiallelic() {
        return getAlternateAlleleList().size() == 1;
    }

    public static rodDbSNP getFirstRealSNP(List<Object> dbsnpList) {
        if (dbsnpList == null)
            return null;

        rodDbSNP dbsnp = null;
        for (Object d : dbsnpList) {
            if (d instanceof rodDbSNP && ((rodDbSNP) d).isSNP()) {
                dbsnp = (rodDbSNP) d;
                break;
            }
        }

        return dbsnp;
    }
}
