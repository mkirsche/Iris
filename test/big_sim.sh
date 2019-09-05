./SURVIVOR simSV base.fa simul.param 0 0 bigsim
./SURVIVOR simreads bigsim.fasta HG002_Pac_error_profile_bwa.txt 20 bigreads.fastq
../external_scripts/ngmlr -t 4 -r base.fa -q bigreads.fastq -o bigaln.sam
../external_scripts/samtools view -S -b bigaln.sam > bigaln.bam
../external_scripts/samtools sort bigaln.bam > bigaln_sorted.bam
../external_scripts/samtools index bigaln_sorted.bam
./sniffles -m bigaln_sorted.bam -n -1 -v bigsniffles.vcf
rm base.fa.*
rm base.fa-*
rm bigaln.sam
rm bigaln.bam
rm *genotype
