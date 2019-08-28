./build.sh
cd test
./align.sh
cd ../
java -cp src CrossStitch genome_in=test/genome.fa reads_in=test/aln_sorted.bam vcf_in=test/sniffles.vcf vcf_out=test/refined.vcf
cat test/refined.vcf

