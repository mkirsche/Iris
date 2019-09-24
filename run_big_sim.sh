./build.sh
cd test/bigsimtest
./big_sim.sh
cd ../../
java -cp src Iris genome_in=test/base.fa reads_in=test/bigsimtest/bigaln_sorted.bam vcf_in=test/bigsimtest/bigsniffles.vcf vcf_out=test/bigsimtest/bigngmlrraconrefined.vcf --ngmlr threads=2
java -cp src Iris genome_in=test/base.fa reads_in=test/bigsimtest/bigaln_sorted.bam vcf_in=test/bigsimtest/bigsniffles.vcf vcf_out=test/bigsimtest/bigminimapraconrefined.vcf threads=2
java -cp src Iris genome_in=test/base.fa reads_in=test/bigsimtest/bigaln_sorted.bam vcf_in=test/bigsimtest/bigsniffles.vcf vcf_out=test/bigsimtest/bigngmlrfalconsenserefined.vcf --ngmlr --falconsense threads=2
java -cp src Iris genome_in=test/base.fa reads_in=test/bigsimtest/bigaln_sorted.bam vcf_in=test/bigsimtest/bigsniffles.vcf vcf_out=test/bigsimtest/bigminimapfalconsenserefined.vcf --falconsense threads=2

echo 'Sniffles accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsimtest/bigsim.insertions.fa iris_calls=test/bigsimtest/bigsniffles.vcf
echo ''
echo 'ngmlr/racon refined accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsimtest/bigsim.insertions.fa iris_calls=test/bigsimtest/bigngmlrraconrefined.vcf
echo ''
echo 'minimap2/racon refined accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsimtest/bigsim.insertions.fa iris_calls=test/bigsimtest/bigminimapraconrefined.vcf
echo ''
echo 'ngmlr/falconsense refined accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsimtest/bigsim.insertions.fa iris_calls=test/bigsimtest/bigngmlrfalconsenserefined.vcf
echo ''
echo 'minimap2/falconsense refined accuracy results'
echo '-----------------------------------'
java -cp src EvaluateSimulatedAccuracy ground_truth=test/bigsimtest/bigsim.insertions.fa iris_calls=test/bigsimtest/bigminimapfalconsenserefined.vcf
echo ''
