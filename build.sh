BINDIR=`dirname $(readlink -f "$0")`
javac -cp $BINDIR/src/junit.jar:BINDIR/src $BINDIR/src/*.java
