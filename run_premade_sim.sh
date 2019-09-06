./build.sh
java -cp src Iris genome_in=test/base.fa vcf_in=test/pbAll.sniffles.vcf reads_in=test/pbAll.bam vcf_out=out.vcf threads=2

