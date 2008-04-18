#!/bin/sh
# Note: Set $JAVA_HOME environment variable to specify which java to use

# traverse symlinks
SCRIPT="$0"
if [ -L "${SCRIPT}" ]; then
    SCRIPT=`readlink -f "${SCRIPT}"`
fi
# go to the program directory
cd `dirname "$SCRIPT"`

# run program
if [ "$JAVA_HOME" ]; then
    $JAVA_HOME/bin/java -jar esmska.jar $*
		     else
    java -jar esmska.jar $*
fi

# return exitcode
EXITCODE="$?"
exit ${EXITCODE}
