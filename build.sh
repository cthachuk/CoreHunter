#!/bin/sh

# This purpose of this script is to build CoreHunter and CoreAnalyser.
# The jar file for each will be place in a 'bin' subdirectory of this
# project.
#
# We assume the availability of mavan, java, and a JDK
# installation for this to work.
#
# Author: Chris Thachuk
# Date: Feb 3, 2011

ROOT=`dirname $0`

function check_requirements {
 hash mvn 2>&- || { echo >&2 "Maven must be installed first.  Aborting."; exit 1; }
 hash javac 2>&- || { echo >&2 "The Java JDK must be installed first.  Aborting."; exit 1; }
 hash java 2>&- || { echo >&2 "The Java Runtime must be installed first.  Aborting."; exit 1; }
}

function build_corehunter {
    check_requirements
    
    echo "Rebuilding Corehunter"

    echo "  - building corehunter"
    mvn package
    
    echo "  - copying to ROOT of current project"
    if [ ! -d $ROOT/bin ]; then
	mkdir $ROOT/bin
    fi

    cp -f $ROOT/corehunter-cli/target/corehunter-cli-1.0-SNAPSHOT-jar-with-dependencies.jar $ROOT/bin/corehunter-cli.jar
    cp -f $ROOT/coreanalyser-cli/target/coreanalyser-cli-1.0-SNAPSHOT-jar-with-dependencies.jar $ROOT/bin/coreanalyser-cli.jar
}

if [ ! -f $ROOT/bin/corehunter-cli.jar ] || [ ! -f $ROOT/bin/coreanalyser-cli.jar ]; then
    build_corehunter
else
    echo "CoreHunter suite is already built."
fi

