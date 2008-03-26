#!/bin/sh
SCRIPT="$0"
if [ -L "${SCRIPT}" ]; then
    SCRIPT=`readlink -f "${SCRIPT}"`
fi
cd `dirname "$SCRIPT"`

java -jar esmska.jar $*

EXITCODE="$?"
exit ${EXITCODE}
