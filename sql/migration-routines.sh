source install-routines.sh

export_data_for_migration() {
    AEW_CONNECTION=$1
    DATA_FOLDER=$2

    for TABLE_NAME in $TABLE_NAMES
      do
      echo "... $TABLE_NAME"
      if [ -x ./flat_array ]; then
	  SQL=`egrep -iv '^(set|quit)' Sql4csvMv/$TABLE_NAME.sql | sed 's/|| chr(9) ||/,/; s/;//'`
	  ./flat_array userid=$AEW_CONNECTION sqlstmt="$SQL" arraysize=100 > $DATA_FOLDER/$TABLE_NAME.dat
      else
	  sqlplus -S $AEW_CONNECTION @Sql4csvMv/$TABLE_NAME.sql > $DATA_FOLDER/$TABLE_NAME.dat
      fi
    done
    
    sqlplus -S $AEW_CONNECTION @Sql4csvMv/Property.sql > $DATA_FOLDER/Property.dat
    sqlplus -S $AEW_CONNECTION @Sql4csvMv/GeneProperty.sql > $DATA_FOLDER/GeneProperty.dat
    
    echo "Mapping identifiers..."
    
    echo "... AssayPVOntology"
    perl Sql4csvMv/map-ontology.pl \
	$DATA_FOLDER/Property.dat \
	$DATA_FOLDER/PropertyValue.dat \
	$DATA_FOLDER/AssayPV.dat \
	$DATA_FOLDER/AssayPVOntology.dat \
	$DATA_FOLDER/OntologyTerm.dat \
	> $DATA_FOLDER/AssayPVOntology.mapped.dat \
	2>APVO.errors
    
    echo "... SamplePVOntology"
    perl Sql4csvMv/map-ontology.pl \
	$DATA_FOLDER/Property.dat \
	$DATA_FOLDER/PropertyValue.dat \
	$DATA_FOLDER/SamplePV.dat \
	$DATA_FOLDER/SamplePVOntology.dat \
	$DATA_FOLDER/OntologyTerm.dat \
	> $DATA_FOLDER/SamplePVOntology.mapped.dat \
	2> SPVO.errors
    
    echo "... GeneGPV"
    perl Sql4csvMv/map-pvs.pl \
	$DATA_FOLDER/GenePropertyValue.dat \
	$DATA_FOLDER/GeneGPV.dat \
	> $DATA_FOLDER/GeneGPV.mapped.dat \
	2> GPVS.errors
    
    echo "... AssayPV"
    perl Sql4csvMv/map-pvs.pl \
	$DATA_FOLDER/PropertyValue.dat \
	$DATA_FOLDER/AssayPV.dat \
	> $DATA_FOLDER/AssayPV.mapped.dat \
	2> APVS.errors
    
    echo "... SamplePV"
    perl Sql4csvMv/map-pvs.pl \
	$DATA_FOLDER/PropertyValue.dat \
	$DATA_FOLDER/SamplePV.dat \
	> $DATA_FOLDER/SamplePV.mapped.dat \
	2> SPVS.errors
    
    echo "... ExpressionAnalytics"
    perl Sql4csvMv/map-eas.pl $DATA_FOLDER/Property.dat \
	$DATA_FOLDER/PropertyValue.dat \
	$DATA_FOLDER/ExpressionAnalytics.dat \
	> $DATA_FOLDER/ExpressionAnalytics.mapped.dat \
	2> EA.errors
    
    for TABLE_NAME in AssayPVOntology SamplePVOntology GeneGPV AssayPV SamplePV ExpressionAnalytics
      do
      mv $DATA_FOLDER/$TABLE_NAME.dat $DATA_FOLDER/$TABLE_NAME.orig.dat
      mv $DATA_FOLDER/$TABLE_NAME.mapped.dat $DATA_FOLDER/$TABLE_NAME.dat
    done
}
