./build.sh
cd test/smallsimtest
./align.sh
cd ../../
java -cp src Iris genome_in=test/smallsimtest/genome.fa reads_in=test/smallsimtest/aln_sorted.bam vcf_in=test/smallsimtest/sniffles.vcf vcf_out=test/smallsimtest/refined.vcf
cat test/smallsimtest/refined.vcf

