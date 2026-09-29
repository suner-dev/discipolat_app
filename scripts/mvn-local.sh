#!/bin/bash
# Agent A — recette de build locale (machine saturée : heap bornée, mode hors-ligne)
export JAVA_HOME=~/.sdkman/candidates/java/21.0.12+1.1-tem
export PATH=$JAVA_HOME/bin:$HOME/.sdkman/candidates/maven/current/bin:$PATH
export MAVEN_OPTS="-Xmx600m -XX:MaxMetaspaceSize=350m"
cd "$(dirname "$0")/../backend" || exit 1
exec mvn -B -o "$@"
