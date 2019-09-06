./build.sh
cd test
./big_sim.sh
cd ../
java -cp src Iris genome_in=test/base.fa reads_in=test/bigaln_sorted.bam vcf_in=test/bigsniffles.vcf vcf_out=test/bigngmlrrefined.vcf --ngmlr threads=2
java -cp src Iris genome_in=test/base.fa reads_in=test/bigaln_sorted.bam vcf_in=test/bigsniffles.vcf vcf_out=test/bigminimaprefined.vcf threads=2

echo 'Sniffles accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsim.insertions.fa iris_calls=test/bigsniffles.vcf
echo ''
echo 'ngmlr refined accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsim.insertions.fa iris_calls=test/bigngmlrrefined.vcf
echo ''
echo 'minimap2 refined accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsim.insertions.fa iris_calls=test/bigminimaprefined.vcf
echo ''

