#!/bin/bash
# Note: Set JAVA_HOME environment variable to specify which java to use
# Note: Modify OPTS variable below to set custom java options

# Disable IPv6 because it breaks Java networking on Ubuntu and Debian
# http://code.google.com/p/esmska/issues/detail?id=252
# http://code.google.com/p/esmska/issues/detail?id=233
OPTS="-Djava.net.preferIPv4Stack=true"

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

# check that java is present
if ! which ${JAVA_HOME}java &>/dev/null; then
    echo "Java executable not found, exiting. Please install Java first." 1>&2
    exit 1
fi

# run program
exec "$JAVA_HOME"java $OPTS -jar esmska.jar $*
