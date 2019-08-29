# RESOLVASE: RESOLving VAriant SEquences
A module used primarily within CrossStitch which corrects the sequences of structural variant calls (currently only insertions).  It uses FalconSense to obtain consensus sequences of the reads surrounding each variant and aligns these sequences back to the reference at the insertion site, resulting in an insertion which takes into account the aggregate information of all supporting reads.

## Building

```./build.sh```

Note: The external tools samtools, ngmlr, and falcon_sense are provided pre-built in the external_scripts folder.  
However, to rebuild them from scratch, fetch and build the included submodules according to their README files.
There is an option when running CrossStitch to use custom paths for these programs, but this is not recommended for falcon_sense 
because the developers have changed the interface for running it.

## Testing

```./run_test.sh```

This runs an example on a simulated dataset with an insertion 
(length 70, sequence (GGGGGGGGCCCCCCCC)x4 + GGGGGG) at position 6930 
and a deletion (length 70) at position 13790, and outputs the refined variant calls.

```./run_sample.sh```

This runs another example on a simulated dataset with 29 insertions and a number of deletions.
This is more used for assessing the speed of different settings and for testing during development.

## Running 

```
java -cp src CrossStitch [args]
  Example: java -cp src CrossStitch genome_in=genome.fa vcf_in=sniffles.vcf 
      reads_in=reads.bam vcf_out=refined.vcf

Required args:
  genome_in (String) - the FASTA file containing the reference genome
  vcf_in    (String) - the VCF file with variant calls/supporting reads determined by Sniffles
  reads_in  (String) - the BAM file containing the reads (must be indexed with samtools)
  vcf_out   (String) - the name of the refined VCF file to be produced

Optional args:
  threads          (int) [4]    - the number of threads to use when running CrossStitch
  padding_before   (int) [1]    - the number of bases to output before the variant in REF/ALT fields
  padding_after    (int) [0]    - the number of bases to output after the variant in REF/ALT fields
  samtools_path    (String)     - the path to samtools if not using included binary
  ngmlr_path       (String)     - the path to ngmlr if not using included binary
  falconsense_path (String)     - the path to falconsense if not using included binary
  log_out          (String)     - the name of the log file to be produced
  genome_buffer    (int) [100k] - the genome region on each side of the SV to align assembled reads to
  min_ins_length   (int) [30]   - the min length allowed for a refined insertion sequence
  max_ins_dist     (int) [5k]   - the max distance a refined insertion call can be from its old position
  ```
  
