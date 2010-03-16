#!/bin/bash
# Installing Atlas2 database

INDEX_TABLESPACE= #make blank for database default
ORACLE_CONNECTION=
PARALLEL_LOAD=0 #zero for singlethreaded load
#scripts which must be executed first, in given order
CORE_SCRIPTS="Types.sql Tables.sql Views.sql list_to_table.sql PKG_ATLASMGR.sql PKG_ATLASAPI.sql PKG_ATLASLDR.sql"  
DATA_FOLDER="Data"
CTL_FOLDER="Ctl"
SCHEMA_FOLDER="Schema"
EXPRESSION_FOLDER="Expression"
TABLE_NAMES="Organism \
             Gene \
             ArrayDesign \
             Assay \
             AssayPV \
             AssayPVOntology \
             AssaySample \
             DesignElement \
             Experiment \
             ExpressionAnalytics \
             GeneProperty \
             GenePropertyValue \
             GeneGPV \
             Ontology \
             OntologyTerm \
             Property \
             PropertyValue \
             Sample \
             SamplePV \
             SamplePVOntology"
ARCHIVE_NAMES="Schema.tar.Z Data.tar.Z Expression.tar.Z"

if [ -z "$1" ]; then
   echo "usage: $0 Schema|ArrayDesign|Data|Expression user/password@tns_name"
   exit
fi

if [ -z "$ORACLE_CONNECTION"]; then
if [ -z "$2" ]; then
   echo "usage: $0 Schema|ArrayDesign|Data|Expression user/password@tns_name"
   exit
fi

  ORACLE_CONNECTION="$2"	
fi

# echo $ORACLE_CONNECTION
# exit

INSTALL_MODE=$1 

if [ "$INSTALL_MODE" == "ArrayDesign" ]; then
 INSTALL_MODE="Data"
 TABLE_NAMES="Organism Gene GeneProperty GenePropertyValue GeneGPV ArrayDesign DesignElement"
fi

case $INSTALL_MODE in
"Schema")
  ARCHIVE_NAMES="Schema.tar.Z";;
"Data")
 ARCHIVE_NAMES="Schema.tar.Z Data.tar.Z";;
"Expression")
 ARCHIVE_NAMES="Schema.tar.Z Data.tar.Z Expression.tar.Z"
esac 

#echo $ARCHIVE_NAMES
#exit

for ARCHIVE_NAME in $ARCHIVE_NAMES
do

if [ ! -r $ARCHIVE_NAME ]; then
        echo "file is not found or insufficient privilegies" $ARCHIVE_NAME; exit -1
fi

zcat $ARCHIVE_NAME | tar xvf -
if [ "$?" -ne "0" ]; then
    echo "can not extract:" $ARCHIVE_NAME; exit -1
fi
done

if [ ! -z "$INDEX_TABLESPACE" ]; then
sed -i '' "s/\/\*INDEX_TABLESPACE\*\//TABLESPACE $INDEX_TABLESPACE/" Schema/Tables.sql
fi

for SCRIPT_NAME in $CORE_SCRIPTS
do
	if [ ! -r Schema/$SCRIPT_NAME ]; then
        	echo "required script not found in Schema folder:" $SCRIPT_NAME; exit -1
	fi
	
	echo "executing " $SCRIPT_NAME

	sqlplus -L -S $ORACLE_CONNECTION @Schema/$SCRIPT_NAME
	if [ "$?" -ne "0" ]; then
    		echo "can not execute script" $SCRIPT_NAME ; exit -1
	fi
done

# for SCRIPT_NAME in Schema/* --TODO:execute all scripts except CORE_SCRIPTS 

#dos2unix $DataFolder/*.*
#if [ "$?" -ne "0" ]; then
#   echo "can not execute dos2unix" ; exit -1
#fi

#for DATA_FILE in $DATA_FOLDER/*.dat --TODO:figure out native method
#do
#	mv $DATA_FILE $DATA_FILE.old
#	tr -d \\r < $DATA_FILE.old > $DATA_FILE #remove CRLF
#	rm $DATA_FILE.old
#done

echo $INSTALL_MODE

if [ "$INSTALL_MODE" == "Schema" ]; then
	echo "Schema created, exit"
	exit 
fi

echo "call ATLASMGR.DisableConstraints();" | sqlplus -L -S $ORACLE_CONNECTION 

for LDR_CTL in $TABLE_NAMES 
do
	echo sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/$LDR_CTL.ctl data=$DATA_FOLDER/$LDR_CTL.dat 
	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/$LDR_CTL.ctl data=$DATA_FOLDER/$LDR_CTL.dat 

	LDR_RESULT="$?" 	

	if [ "$LDR_RESULT" -ne "0" ]; then
  		 echo "can not execute sqlldr:" $LDR_CTL $LDR_RESULT ; 
	fi

	cat $LDR_CTL.log >> install.log
	rm $LDR_CTL.log
done

if [ "$INSTALL_MODE" == "Data" ]; then
	echo "call ATLASMGR.EnableConstraints();" | sqlplus -L -S $ORACLE_CONNECTION
	echo "call ATLASMGR.RebuildSequence();" | sqlplus -L -S $ORACLE_CONNECTION

	echo "Data loaded, exit"
        exit  
fi

# load expression values - 40GB
if [ "$PARALLEL_LOAD" == "0" ]; then
	 sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat log=install.log
else 
 	echo "call ATLASLDR.A2_ASSAYSETBEGIN(NULL);" | sqlplus -L -S $ORACLE_CONNECTION

 	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat bad=EV1.bad multithreading=true parallel=true direct=true columnarrayrows=10000 streamsize=1048576 readsize=1048576 load=100000000 & 
 	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat bad=EV2.bad multithreading=true parallel=true direct=true columnarrayrows=10000 streamsize=1048576 readsize=1048576 load=100000000 skip=100000000 &
	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat bad=EV3.bad multithreading=true parallel=true direct=true columnarrayrows=10000 streamsize=1048576 readsize=1048576 load=100000000 skip=200000000 &
	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat bad=EV4.bad multithreading=true parallel=true direct=true columnarrayrows=10000 streamsize=1048576 readsize=1048576 load=100000000 skip=300000000 &
	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat bad=EV5.bad multithreading=true parallel=true direct=true columnarrayrows=10000 streamsize=1048576 readsize=1048576 load=100000000 skip=400000000 &
	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat bad=EV6.bad multithreading=true parallel=true direct=true columnarrayrows=10000 streamsize=1048576 readsize=1048576 load=100000000 skip=500000000 &
	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat bad=EV7.bad multithreading=true parallel=true direct=true columnarrayrows=10000 streamsize=1048576 readsize=1048576 load=100000000 skip=600000000 &
	sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/ExpressionValue.ctl data=$EXPRESSION_FOLDER/ExpressionValue.dat bad=EV8.bad multithreading=true parallel=true direct=true columnarrayrows=10000 streamsize=1048576 readsize=1048576 load=100000000 skip=700000000 &

	echo "call ATLASLDR.A2_ASSAYSETEND(NULL);" | sqlplus -L -S $ORACLE_CONNECTION
fi

if [ "$?" -ne "0" ]; then
        echo "can not execute sqlldr:" ExpressionValue $? ; 
fi

echo "call ATLASMGR.EnableConstraints();" | sqlplus -L -S $ORACLE_CONNECTION
echo "call ATLASMGR.RebuildSequence();" | sqlplus -L -S $ORACLE_CONNECTION

#sqlldr $ORACLE_CONNECTION control=ctl/Spec.ctl data=$DataFolder/Spec.dat
#sqlldr $ORACLE_CONNECTION control=ctl/Gene.ctl data=$DataFolder/Gene.dat
#sqlldr $ORACLE_CONNECTION control=ctl/ArrayDesign.ctl data=$DataFolder/ArrayDesign.dat
#sqlldr $ORACLE_CONNECTION control=ctl/Assay.ctl data=$DataFolder/Assay.dat
#sqlldr $ORACLE_CONNECTION control=ctl/AssayOntology.ctl data=$DataFolder/AssayOntology.dat
#sqlldr $ORACLE_CONNECTION control=ctl/AssayPropertyValue.ctl data=$DataFolder/AssayPropertyValue.dat
#sqlldr $ORACLE_CONNECTION control=ctl/AssaySample.ctl data=$DataFolder/AssaySample.dat
#sqlldr $ORACLE_CONNECTION control=ctl/DesignElement.ctl data=$DataFolder/DesignElement.dat
#sqlldr $ORACLE_CONNECTION control=ctl/Experiment.ctl data=$DataFolder/Experiment.dat
#sqlldr $ORACLE_CONNECTION control=ctl/ExpressionAnalytics.ctl data=$DataFolder/ExpressionAnalytics.dat
#sqlldr $ORACLE_CONNECTION control=ctl/GeneProperty.ctl data=$DataFolder/GeneProperty.dat
#sqlldr $ORACLE_CONNECTION control=ctl/GenePropertyValue.ctl data=$DataFolder/GenePropertyValue.dat
#sqlldr $ORACLE_CONNECTION control=ctl/Ontology.ctl data=$DataFolder/Ontology.dat
#sqlldr $ORACLE_CONNECTION control=ctl/OntologyTerm.ctl data=$DataFolder/OntologyTerm.dat
#sqlldr $ORACLE_CONNECTION control=ctl/Property.ctl data=$DataFolder/Property.dat
#sqlldr $ORACLE_CONNECTION control=ctl/PropertyValue.ctl data=$DataFolder/PropertyValue.dat
#sqlldr $ORACLE_CONNECTION control=ctl/Sample.ctl data=$DataFolder/Sample.dat
#sqlldr $ORACLE_CONNECTION control=ctl/SampleOntology.ctl data=$DataFolder/SampleOntology.dat
#sqlldr $ORACLE_CONNECTION control=ctl/SamplePropertyValue.ctl data=$DataFolder/SamplePropertyValue.dat
#sqlldr $ORACLE_CONNECTION control=ctl/ExpressionValue.ctl data=$DataFolder/ExpressionValue.dat

echo "installation complete"
