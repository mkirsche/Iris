./SURVIVOR simreads inserts.fa HG002_Pac_error_profile_bwa.txt 50 reads.fastq
./bwa index genome.fa
./bwa mem genome.fa reads.fastq > aln.sam
../external_scripts/samtools view -S -b aln.sam > aln.bam
../external_scripts/samtools sort aln.bam > aln_sorted.bam
../external_scripts/samtools index aln_sorted.bam
./sniffles -m aln_sorted.bam -n -1 -v sniffles.vcf
rm genome.fa.*
rm aln.sam
rm aln.bam
rm *genotype
