#!/bin/bash
# Installing Atlas2 database

ORACLE_CONNECTION=Atlas2/atlas2@WINDON
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

mkdir Ctl
mkdir Data
mkdir Sql4csv
mkdir Schema
mkdir Expression

cd Sql4csv
svn co "svn://bar.ebi.ac.uk/branches/atlas-standalone-data/sql-data/Sql4csv" .
cd ..

cd Ctl
svn co "svn://bar.ebi.ac.uk/branches/atlas-standalone-data/sql-data/Ctl" .
cd ..

cd Schema
svn co "svn://bar.ebi.ac.uk/branches/atlas-standalone/sql/" .
cd ..

for TABLE_NAME in $TABLE_NAMES
do
	echo  "sqlplus -S $ORACLE_CONNECTION @Sql4csv/$TABLE_NAME.sql > Data/$TABLE_NAME.dat"
#       sqlplus -S $ORACLE_CONNECTION @Sql4csv/$TABLE_NAME.sql > Data/$TABLE_NAME.dat
done

# TODO: download Expressions

rm Schema.tar.Z
rm Data.tar.Z
# rm Expression.tar.Z

tar -zcvf Schema.tar.Z ./Schema/*.sql
tar -zcvf Data.tar.Z ./Data/*.dat ./Ctl/*.ctl
# tar -zcvf Expression.tar.Z ./Expression/*.dat

tar -zcvf AtlasData-10.3rel-2.14.tar.Z Schema.tar.Z Data.tar.Z Expression.tar.Z install.sh readme.txt

