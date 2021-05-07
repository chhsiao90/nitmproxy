#mvn exec:java@inprocess -Dmain.args="$*" $JAVA_OPTS
mvn exec:java@inprocess -Dmain.args="-m TRANSPARENT"
