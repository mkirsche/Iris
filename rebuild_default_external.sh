if [ "$(uname -s)" = 'Linux' ]; then
    BASEDIR=$(dirname "$(readlink -f "$0" || echo "$(echo "$0" | sed -e 's,\\,/,g')")")
else
    BASEDIR=$(dirname "$(readlink "$0" || echo "$(echo "$0" | sed -e 's,\\,/,g')")")
fi
cd $BASEDIR

if [ -d "rebuilt_external_scripts" ]; then
    rm -r rebuilt_external_scripts
fi
mkdir rebuilt_external_scripts
git submodule update --init --recursive

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

cd racon
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make
cp bin/racon ../../rebuilt_external_scripts
cd ../..

cp rebuilt_external_scripts/* external_scripts/

