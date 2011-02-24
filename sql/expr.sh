#!/bin/bash
# Installing Atlas2 database

INDEX_TABLESPACE=ATLAS2_INDX #make blank for database default
ORACLE_CONNECTION=
PARALLEL_LOAD=1 #zero for singlethreaded load
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

echo "installation complete"
