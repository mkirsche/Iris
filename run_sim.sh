./build.sh
cd test/bigsimtest
./big_sim.sh
cd ../../
java -cp src Iris genome_in=test/base.fa reads_in=test/bigsimtest/bigaln_sorted.bam vcf_in=test/bigsimtest/bigsniffles.vcf vcf_out=test/bigsimtest/bigngmlrrefined.vcf --ngmlr threads=2
java -cp src Iris genome_in=test/base.fa reads_in=test/bigsimtest/bigaln_sorted.bam vcf_in=test/bigsimtest/bigsniffles.vcf vcf_out=test/bigsimtest/bigminimaprefined.vcf threads=2

echo 'Sniffles accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsimtest/bigsim.insertions.fa iris_calls=test/bigsimtest/bigsniffles.vcf
echo ''
echo 'ngmlr refined accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsimtest/bigsim.insertions.fa iris_calls=test/bigsimtest/bigngmlrrefined.vcf
echo ''
echo 'minimap2 refined accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsimtest/bigsim.insertions.fa iris_calls=test/bigsimtest/bigminimaprefined.vcf
echo ''

