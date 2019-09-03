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

cd htslib
autoheader
autoconf
./configure --prefix=$BASEDIR/htslib
make
make install
cd ..

cd samtools
autoheader
conf
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
