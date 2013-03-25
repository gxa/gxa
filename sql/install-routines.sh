#!/bin/bash
TABLE_NAMES_DATA="Assay \
             AssayPV \
             AssayPVOntology \
             AssaySample \
             Experiment \
             ExperimentAsset \
             Sample \
             SamplePV \
             SamplePVOntology"

TABLE_NAMES_SCHEMA="ArrayDesign \
             AnnotationSrc \
             AnnSrc_BioEntityType \
             BioEntity \
             BioEntitybepv \
             BioEntityproperty \
             BioEntitypropertyvalue \
             BioEntitytype \
             BIOMART_ANNSRC \
             DesignElement \
             DesignEltBioentity \
             EXTERNAL_ARRAYDESIGN \
             EXTERNAL_BEPROPERTY \
             Gene \
             GeneGPV \
             GeneProperty \
             GenePropertyValue \
             Ontology \
             OntologyTerm \
             Organism \
             Property \
             PropertyValue \
             SchemaVersion \
             Software"

create_schema() {
    ATLAS_CONNECTION=$1

    # scripts which must be executed first, in given order
    CORE_SCRIPTS="Types.sql Tables.sql Views.sql list_to_table.sql list_to_table_str.sql PKG_ATLASMGR.sql \
    PKG_ATLASLDR.sql CUR_AssayProperty.sql CUR_MergePropertyValue.sql CUR_PropertyValue.sql CUR_SampleProperty.sql \
    CUR_AllPropertyID.sql CUR_TwoValues.sql CUR_TwoFactors.sql CUR_MergeFactors.sql \
    TR_CUR_AssayProperty.sql TR_CUR_PropertyValue.sql TR_CUR_SampleProperty.sql CUR_OntologyMapping.sql \
    TR_CUR_OntologyMapping.sql"
    SCHEMA_FOLDER=Schema

    for SCRIPT_NAME in $CORE_SCRIPTS
      do
      if [ ! -r Schema/$SCRIPT_NAME ]; then
	     echo "required script not found in Schema folder:" $SCRIPT_NAME; exit -1
      fi
      echo "executing " $SCRIPT_NAME
      
      sqlplus -L -S $ATLAS_CONNECTION @Schema/$SCRIPT_NAME
      if [ "$?" -ne "0" ]; then
	  echo "can not execute script" $SCRIPT_NAME ; exit -1
      fi
    done
}

load_data() {
    ATLAS_CONNECTION=$1
    DATA_FOLDER=$2
    CTL_FOLDER=$3
    INSTALL_MODE=$4
    ATLAS_INDEX_TABLESPACE=$5

    echo "call ATLASMGR.DisableConstraints();" | sqlplus -L -S $ATLAS_CONNECTION

    TABLE_NAMES_SET="${TABLE_NAMES_SCHEMA} ${TABLE_NAMES_DATA}"
    if [ "$INSTALL_MODE" == "Schema" ]; then
      TABLE_NAMES_SET=$TABLE_NAMES_SCHEMA
    fi

    TARGET_ATLAS_USER=`echo ${ATLAS_INDEX_TABLESPACE} | awk -F'_' '{print $1}'`
    echo "Creating table synonyms for user: ${TARGET_ATLAS_USER}_ro"
    for TABLE in $TABLE_NAMES_SET
       do
         echo "create or replace synonym ${TARGET_ATLAS_USER}_ro.A2_${TABLE} for ${TARGET_ATLAS_USER}.A2_${TABLE};" | sqlplus -L -S $ATLAS_CONNECTION
         echo "create or replace synonym ${TARGET_ATLAS_USER}_ro.A2_${TABLE} for ${TARGET_ATLAS_USER}.A2_${TABLE};"
       done
    echo "create or replace synonym ${TARGET_ATLAS_USER}_ro.A2_CONFIG_PROPERTY for ${TARGET_ATLAS_USER}.A2_CONFIG_PROPERTY;" | sqlplus -L -S $ATLAS_CONNECTION
    echo "create or replace synonym ${TARGET_ATLAS_USER}_ro.A2_CONFIG_PROPERTY for ${TARGET_ATLAS_USER}.A2_CONFIG_PROPERTY;"
    echo "Done creating table synonyms for user: ${TARGET_ATLAS_USER}_ro"


    echo "Granting permissions to ${TARGET_ATLAS_USER}_select_role"
    for TABLE in $TABLE_NAMES_SET
      do
        echo "grant select on A2_${TABLE} to ${TARGET_ATLAS_USER}_select_role;" | sqlplus -L -S $ATLAS_CONNECTION
        echo "grant select on A2_${TABLE} to ${TARGET_ATLAS_USER}_select_role"
      done
    echo "grant select on A2_CONFIG_PROPERTY to ${TARGET_ATLAS_USER}_select_role;" | sqlplus -L -S $ATLAS_CONNECTION
    echo "grant select on A2_CONFIG_PROPERTY to ${TARGET_ATLAS_USER}_select_role"
    echo "Done granting select permissions to ${TARGET_ATLAS_USER}_select_role"

    for LDR_CTL in $TABLE_NAMES_SET 
      do
      echo "... $LDR_CTL"
      sqlldr $ATLAS_CONNECTION control=$CTL_FOLDER/$LDR_CTL.ctl data=$DATA_FOLDER/$LDR_CTL.dat 
      
      LDR_RESULT="$?" 	
      
      if [ "$LDR_RESULT" -ne "0" ]; then
	  echo "can not execute sqlldr:" $LDR_CTL $LDR_RESULT ; 
      fi
      cat $LDR_CTL.log >> install.log
      rm $LDR_CTL.log
    done

    echo "Creating indexes and constraints..."
    SCRIPT_NAME=Indexes.sql
    if [ ! -z "${ATLAS_INDEX_TABLESPACE}" ]; then
        sed "s/\/\*PK_TABLESPACE\*\//USING INDEX TABLESPACE ${ATLAS_INDEX_TABLESPACE}/" Schema/Indexes.sql | \
        sed "s/\/\*INDEX_TABLESPACE\*\//TABLESPACE ${ATLAS_INDEX_TABLESPACE}/" > Schema/IndexesTablespace.sql
	    SCRIPT_NAME=IndexesTablespace.sql
    fi

    sqlplus -L -S $ATLAS_CONNECTION @Schema/${SCRIPT_NAME}
    if [ "$?" -ne "0" ]; then
	   echo "can not execute script" Indexes.sql ; exit -1
    fi

    echo "Enabling constraints and rebuilding sequences..."
    echo "call ATLASMGR.EnableConstraints();" | sqlplus -L -S $ATLAS_CONNECTION
    echo "call ATLASMGR.RebuildSequences();" | sqlplus -L -S $ATLAS_CONNECTION
}
