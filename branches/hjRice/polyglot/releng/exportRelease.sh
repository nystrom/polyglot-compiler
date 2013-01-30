#! /bin/bash

# Dave Grove
# Nate Nystrom

while [ $# != 0 ]; do

  case $1 in
    -version)
	export POLYGLOT_VERSION=$2
	shift
    ;;

    -tag)
	export POLYGLOT_TAG=$2
	shift
    ;;
   esac
   shift
done

if [[ -z "$POLYGLOT_VERSION" ]]; then
    echo "usage: $0 must give Polyglot version as -version <version>"
    exit 1
fi

if [[ -z "$POLYGLOT_TAG" ]]; then
    echo "usage: $0 must give polyglot tag as -tag <svn tag>"
    exit 1
fi

date

workdir=$PWD/scratch
distdir=$workdir/x10-$POLYGLOT_VERSION
tarfile=polyglot-$POLYGLOT_VERSION-src.tar.gz

echo
echo cleaning $workdir
rm -rf $workdir
mkdir -p $workdir || exit 1
mkdir -p $workdir/polyglot-$POLYGLOT_VERSION-src


(
echo
echo getting polyglot
cd $workdir/polyglot-$POLYGLOT_VERSION-src
svn export http://polyglot-compiler.googlecode.com/svn/tags/$POLYGLOT_TAG/polyglot
)

echo "The distribution is now exported to the directory $workdir"
