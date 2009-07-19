#!/bin/sh
# Note: Set $JAVA_HOME environment variable to specify which java to use

# traverse symlinks
SCRIPT="$0"
if [ -L "${SCRIPT}" ]; then
    SCRIPT="`readlink -f "${SCRIPT}"`"
fi
# go to the program directory
cd "`dirname "$SCRIPT"`"

# setup some additional parameters for Mac OS X
if [ `uname` == "Darwin" ]; then
    OPS="-Xdock:name=Esmska -Xdock:icon=icons/esmska.png -Dfile.encoding=UTF8 -Dapple.laf.useScreenMenuBar=true"
fi

# run program
if [ "$JAVA_HOME" ]; then
    "$JAVA_HOME"/bin/java $OPS -jar esmska.jar $*
                     else
    java $OPS -jar esmska.jar $*
fi

# return exitcode
EXITCODE="$?"
exit ${EXITCODE}

