# IRIS: Implement for Refining Insertion Sequences
A module which corrects the sequences of structural variant calls (currently only insertions).  It uses FalconSense to obtain consensus sequences of the reads surrounding each variant and aligns these sequences back to the reference at the insertion site, resulting in an insertion which takes into account the aggregate information of all supporting reads.

## Building

```./build.sh```

Note: The external tools samtools, ngmlr, minimap2, racon, and falcon_sense are provided pre-built in the external_scripts folder.  
However, to rebuild them from scratch, fetch and build the included submodules according to their README files by running the script rebuild_external.sh.  Note: gcc >= 6.4.0 is required to rebuild dependencies in this way.
Note that either minimap2 (default) or ngmlr will be used, but not both.
Also, racon (default) or falcon_sense will be used, but not both, and racon depends on minimap2.
There is an option when running IRIS to use custom paths for these programs if you already have them installed, but this is not recommended for falcon_sense 
because the developers have significantly changed the interface for running it and it is not possible to run Falconsense in isolation with recent builds of Canu.

## Testing

```./run_premade_sim.sh```

This runs an example on a simulated dataset with 29 insertions and a number of deletions with no included ground truth.
This is primarily used for assessing the speed of different settings and for testing during development,
but also works well as a quick test to ensure that your build is working.

```./run_small_sim.sh```

This runs an example on a simulated dataset with an insertion 
(length 70, sequence (GGGGGGGGCCCCCCCC)x4 + GGGGGG) at position 6930 
and a deletion (length 70) at position 13790, and outputs the refined variant calls.
This test runs the simulation itself in addition to the refinement and is good for ensuring that variant calls are precise and match the desired format.
Note that this test depends on included binaries for BWA, SURVIVOR, and Sniffles,
so it may be necessary to update test/smallsimtest/align.sh to point to your own local executables.

```./run_big_sim.sh```

This creates and refines a simulated dataset with 50 indels of lengths in [50, 200].  It then
runs IRIS on the variant calls output from Sniffles twice (once with ngmlr as the aligner and once with minimap2).
It compares the refined insertion sequences as well as the original calls to the ground truth and outputs some simple accuracy metrics for each.
As with run_test, it may be necessary to modify bigsimtest/big_sim.sh to point to your own installations of required software (ngmlr, SURVIVOR, and Sniffles).

## Running 

```
java -cp src Iris [args]
  Example: java -cp src Iris genome_in=genome.fa vcf_in=sniffles.vcf 
      reads_in=reads.bam vcf_out=refined.vcf

Required args:
  genome_in (String) - the FASTA file containing the reference genome
  vcf_in    (String) - the VCF file with variant calls/supporting reads determined by Sniffles
  reads_in  (String) - the BAM file containing the reads (must be indexed with samtools)
  vcf_out   (String) - the name of the refined VCF file to be produced

Optional args:
  threads          (int) [4]    - the number of threads to use when running Iris
  padding_before   (int) [1]    - the number of bases to output before the variant in REF/ALT fields
  padding_after    (int) [0]    - the number of bases to output after the variant in REF/ALT fields
  samtools_path    (String)     - the path to samtools if not using included binary
  ngmlr_path       (String)     - the path to ngmlr if using ngmlr and not using included binary
  minimap_path     (String)     - the path to minimap if using minimap and not using included binary
  falconsense_path (String)     - the path to falconsense if using falconsense and not using included binary
  racon_path       (String)           - the path to racon if not using included binary
  log_out          (String)     - the name of the log file to be produced
  genome_buffer    (int) [100k] - the genome region on each side of the SV to align assembled reads to
  min_ins_length   (int) [30]   - the min length allowed for a refined insertion sequence
  max_ins_dist     (int) [100]  - the max distance a refined insertion call can be from its old position
  --ngmlr                       - align with ngmlr instead of minimap
  --falconsense                 - compute consensus with falconsense instead of racon
  --keep_files                  - don't remove intermediate files - used for debugging
  --also_deletions              - also try to refine deletion positions/lengths
  --resume                      - use the results already computed from a previously terminated run
  --pacbio                      - if using minimap as the aligner, run in pacbio mode
  --rerunracon                  - if using racon for consensus, run it twice
  --keep_long_variants          - output original VCF line for very long variants instead of ignoring them
  ```

