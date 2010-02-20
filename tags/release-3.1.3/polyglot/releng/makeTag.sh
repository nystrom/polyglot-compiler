#! /bin/bash

# Author: Dave Grove
# Author: Nate Nystrom
#
# Simple script to use svn copy to tag a X10 release
# 
# Usage: makeTag -rev <svn revision number> -tag <revision name>
#

while [ $# != 0 ]; do

  case $1 in
    -rev)
	export REVISION=$2
	shift
    ;;

    -tag)
	export TAG=$2
	shift
    ;;
   esac
   shift
done

if [[ -z "$REVISION" ]]; then
    echo "usage: $0 must give svn revision number as -rev <rev>"
    exit 1
fi

if [[ -z "$TAG" ]]; then
    echo "usage: $0 must give tag name as -tag <tag>"
    exit 1
fi

svn copy -r $REVISION \
        https://polyglot-compiler.googlecode.com/svn/trunk \
        https://polyglot-compiler.googlecode.com/svn/tags/$TAG \
        -m "Tagging trunk revision $REVISION as $TAG"
