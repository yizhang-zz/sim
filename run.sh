#!/bin/sh

export CLASSPATH=./bin:./lib/Jama-1.0.2.jar:./lib/commons-collections-3.2.jar:./lib/commons-configuration-1.5.jar:lib/commons-lang-2.3.jar:lib/commons-logging-1.1.1.jar:lib/log4j-1.2.15.jar

java sim.Main $1
