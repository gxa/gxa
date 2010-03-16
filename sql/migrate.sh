#!/bin/bash
# Installing Atlas2 database

ORACLE_CONNECTION=AEMART/marte@AEDWP_SMITHERS
TABLE_NAMES="Organism \
  Gene \
  ArrayDesign \
  Assay \
  AssayOntology \
  AssayPropertyValue \
  AssaySample \
  DesignElement \
  Experiment \
  ExpressionAnalytics \
  GeneProperty \
  GenePropertyValue \
  Ontology \
  OntologyTerm \
  Property \
  PropertyValue \
  Sample \
  SampleOntology \
  SamplePropertyValue"

sqlplus -S AEMART/marte@AEDWP_SMITHERS @Sql4csvMoo/SamplePV.sql > DataMoo/SamplePV.dat

exit 0;

for TABLE_NAME in $TABLE_NAMES
do
	echo  "sqlplus -S $ORACLE_CONNECTION @Sql4csvMoo/$TABLE_NAME.sql > DataMoo/$TABLE_NAME.dat"
        sqlplus -S $ORACLE_CONNECTION @Sql4csvMoo/$TABLE_NAME.sql > DataMoo/$TABLE_NAME.dat

        echo sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/$LDR_CTL.ctl data=$DATA_FOLDER/$LDR_CTL.dat
        sqlldr $ORACLE_CONNECTION control=$CTL_FOLDER/$LDR_CTL.ctl data=$DATA_FOLDER/$LDR_CTL.dat

        if [ "$?" -ne "0" ]; then
                 echo "can not execute sqlldr:" $LDR_CTL $? ; exit -1
        fi

        cat $LDR_CTL.log >> install.log
        rm $LDR_CTL.log
done


