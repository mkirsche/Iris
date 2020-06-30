if [ "$(uname -s)" = 'Linux' ]; then
    BINDIR=$(dirname "$(readlink -f "$0" || echo "$(echo "$0" | sed -e 's,\\,/,g')")")
else
    BINDIR=$(dirname "$(readlink "$0" || echo "$(echo "$0" | sed -e 's,\\,/,g')")")
fi
cd $BINDIR/src
javac *.java
jar -c -e Iris -f iris.jar *.class
mv iris.jar $BINDIR
cd $BINDIR
