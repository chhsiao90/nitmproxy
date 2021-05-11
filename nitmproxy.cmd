#mvn compile exec:java@inprocess --mode %JAVA_OPTS% -Dmain.args="%*"
#mvn exec:java@inprocess -Dmain.args="-h 192.168.1.112"
mvn exec:java@inprocess -Dmain.args="-m TRANSPARENT -h 192.168.1.112"
