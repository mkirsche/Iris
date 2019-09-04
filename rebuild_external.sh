BASEDIR=`dirname $(readlink -f "$0")`

mkdir rebuilt_external_scripts
git submodule update --init --recursive
cd ngmlr
mkdir -p build
cd build
cmake ..
make
cd ../bin/ngmlr-*
cp ngmlr ../../../rebuilt_external_scripts
cd ../../..

cd minimap2
make
cp minimap2 ../rebuilt_external_scripts
cd ..

cd htslib
autoheader
autoconf
./configure --prefix=$BASEDIR/htslib
make
make install
cd ..

cd samtools
autoheader
autoconf
./configure --with-htslib=../htslib --prefix=$BASEDIR/samtools
make
make install
cd bin
cp samtools ../../rebuilt_external_scripts
cd ../..

cd canu
cd src
make -j 4
cd ../Linux-amd64/bin
cp falcon_sense ../../../rebuilt_external_scripts
cd ../../..

sed -i 's/external_scripts/rebuilt_external_scripts/g' src/Settings.java
sed -i 's/external_scripts/rebuilt_external_scripts/g' test/align.sh

