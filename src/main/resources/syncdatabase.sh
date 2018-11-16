#!/bin/sh
file="indexdata"
if [ ! -f "$file" ]
then
    echo "$file not found in this directly and create it with default value '-1,-1'"
	echo "-1,-1,-1" >> $file
fi


sourceDBUrl="jdbc:oracle:thin:amdb_200@localhost:1521/xe"
sourceDBUser="amdb_200"
sourceDBPass="amdb_200"
sourceDBDriver="oracle.jdbc.OracleDriver"

destDBUrl="jdbc:oracle:thin:amdb_250@localhost:1521/xe"
destDBUser="amdb_250"
destDBPass="amdb_250"
destDBDriver="oracle.jdbc.OracleDriver"

java -jar org.wso2.carbon.apimgt.dbsync-0.0.1-jar-with-dependencies.jar \
$sourceDBUrl  $sourceDBUser  $sourceDBPass  $sourceDBDriver  $destDBUrl  $destDBUser  $destDBPass  $destDBDriver