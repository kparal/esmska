#!/bin/bash
# Note: Set JAVA_HOME environment variable to specify which java to use
# Note: Modify OPTS variable below to set custom java options

OPTS=""

# traverse symlinks
SCRIPT="$0"
if [ -L "${SCRIPT}" ]; then
    SCRIPT="`readlink -f "${SCRIPT}"`"
fi

# go to the program directory
cd "`dirname "$SCRIPT"`"

# set some additional java options for Mac OS X
if [ "`uname`" = "Darwin" ]; then
    OPTS="$OPTS -Xdock:name=Esmska -Xdock:icon=icons/esmska.png -Dfile.encoding=UTF8 -Dapple.laf.useScreenMenuBar=true"
fi

# decide where to find java
if [ "$JAVA_HOME" ]; then
    JAVA_HOME="$JAVA_HOME/bin/"
fi

# run program
exec "$JAVA_HOME"java $OPTS -jar esmska.jar $*
