SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab off
set line 500
set wrap off
SET FEEDBACK OFF
SET VERIFY OFF

SELECT 1   || chr(9) || 'rnai'              || chr(9) || 'AE1__ASSAY_RNAI__DM' || chr(9) || '' FROM DUAL;
SELECT 2   || chr(9) || 'age'               || chr(9) || 'AE1__ASSAY_AGE__DM' || chr(9) || 'AE1__SAMPLE_AGE__DM' FROM DUAL;
SELECT 3   || chr(9) || 'cellline'          || chr(9) || 'AE1__ASSAY_CELLLINE__DM' || chr(9) || 'AE1__SAMPLE_CELLLINE__DM' FROM DUAL;
SELECT 4   || chr(9) || 'celltype'          || chr(9) || 'AE1__ASSAY_CELLTYPE__DM' || chr(9) || 'AE1__SAMPLE_CELLTYPE__DM' FROM DUAL;
SELECT 5   || chr(9) || 'clinhistory'       || chr(9) || 'AE1__ASSAY_CLINHISTORY__DM' || chr(9) || 'AE1__SAMPLE_CLINHISTORY__DM' FROM DUAL;
SELECT 6   || chr(9) || 'clininfo'          || chr(9) || 'AE1__ASSAY_CLININFO__DM' || chr(9) || 'AE1__SAMPLE_CLININFO__DM' FROM DUAL;
SELECT 7   || chr(9) || 'clintreatment'     || chr(9) || 'AE1__ASSAY_CLINTREATMENT__DM' || chr(9) || 'AE1__SAMPLE_CLINTREATMENT__DM' FROM DUAL;
SELECT 8   || chr(9) || 'compound'          || chr(9) || 'AE1__ASSAY_COMPOUND__DM' || chr(9) || '' FROM DUAL;
SELECT 9   || chr(9) || 'devstage'          || chr(9) || 'AE1__ASSAY_DEVSTAGE__DM' || chr(9) || 'AE1__SAMPLE_DEVSTAGE__DM' FROM DUAL;
SELECT 10  || chr(9) || 'diseaselocation'   || chr(9) || '' || chr(9) || '' FROM DUAL;
SELECT 11  || chr(9) || 'diseasestaging'    || chr(9) || 'AE1__ASSAY_DISEASESTAGING__DM' || chr(9) || 'AE1__SAMPLE_DISEASESTAGING__DM' FROM DUAL;
SELECT 12  || chr(9) || 'diseasestate'      || chr(9) || 'AE1__ASSAY_DISEASESTATE__DM' || chr(9) || 'AE1__SAMPLE_DISEASESTATE__DM' FROM DUAL;
SELECT 13  || chr(9) || 'dose'              || chr(9) || 'AE1__ASSAY_DOSE__DM' || chr(9) || '' FROM DUAL;
SELECT 14  || chr(9) || 'ecotype'           || chr(9) || 'AE1__ASSAY_ECOTYPE__DM' || chr(9) || 'AE1__SAMPLE_ECOTYPE__DM' FROM DUAL;
SELECT 15  || chr(9) || 'genmodif'          || chr(9) || 'AE1__ASSAY_GENMODIF__DM' || chr(9) || 'AE1__SAMPLE_GENMODIF__DM' FROM DUAL;
SELECT 16  || chr(9) || 'genotype'          || chr(9) || 'AE1__ASSAY_GENOTYPE__DM' || chr(9) || 'AE1__SAMPLE_GENOTYPE__DM' FROM DUAL;
SELECT 17  || chr(9) || 'growthcondition'   || chr(9) || 'AE1__ASSAY_GROWTHCONDITION__DM' || chr(9) || '' FROM DUAL;
SELECT 18  || chr(9) || 'histology'         || chr(9) || 'AE1__ASSAY_HISTOLOGY__DM' || chr(9) || 'AE1__SAMPLE_HISTOLOGY__DM' FROM DUAL;
SELECT 19  || chr(9) || 'indgeneticchar'    || chr(9) || 'AE1__ASSAY_INDGENETICCHAR__DM' || chr(9) || 'AE1__SAMPLE_INDGENETICCHAR__DM' FROM DUAL;
SELECT 20  || chr(9) || 'individual'        || chr(9) || 'AE1__ASSAY_INDIVIDUAL__DM' || chr(9) || 'AE1__SAMPLE_INDIVIDUAL__DM' FROM DUAL;
SELECT 21  || chr(9) || 'infect'            || chr(9) || 'AE1__ASSAY_INFECT__DM' || chr(9) || '' FROM DUAL;
SELECT 22  || chr(9) || 'injury'            || chr(9) || 'AE1__ASSAY_INJURY__DM' || chr(9) || '' FROM DUAL;
SELECT 23  || chr(9) || 'light'             || chr(9) || 'AE1__ASSAY_LIGHT__DM' || chr(9) || '' FROM DUAL;
SELECT 24  || chr(9) || 'materialtype'      || chr(9) || 'AE1__ASSAY_MATERIALTYPE__DM' || chr(9) || '' FROM DUAL;
SELECT 25  || chr(9) || 'media'             || chr(9) || 'AE1__ASSAY_MEDIA__DM' || chr(9) || '' FROM DUAL;
SELECT 26  || chr(9) || 'observation'       || chr(9) || 'AE1__ASSAY_OBSERVATION__DM' || chr(9) || 'AE1__SAMPLE_OBSERVATION__DM' FROM DUAL;
SELECT 27  || chr(9) || 'organism'          || chr(9) || 'AE1__ASSAY_ORGANISM__DM' || chr(9) || '' FROM DUAL;
SELECT 28  || chr(9) || 'organismpart'      || chr(9) || 'AE1__ASSAY_ORGANISMPART__DM' || chr(9) || 'AE1__SAMPLE_ORGANISMPART__DM' FROM DUAL;
SELECT 29  || chr(9) || 'phenotype'         || chr(9) || 'AE1__ASSAY_PHENOTYPE__DM' || chr(9) || 'AE1__SAMPLE_PHENOTYPE__DM' FROM DUAL;
SELECT 30  || chr(9) || 'protocoltype'      || chr(9) || 'AE1__ASSAY_PROTOCOLTYPE__DM' || chr(9) || '' FROM DUAL;
SELECT 31  || chr(9) || 'replicate'         || chr(9) || 'AE1__ASSAY_REPLICATE__DM' || chr(9) || '' FROM DUAL;
SELECT 32  || chr(9) || 'sex'               || chr(9) || 'AE1__ASSAY_SEX__DM' || chr(9) || 'AE1__SAMPLE_SEX__DM' FROM DUAL;
SELECT 33  || chr(9) || 'strainorline'      || chr(9) || 'AE1__ASSAY_STRAINORLINE__DM' || chr(9) || 'AE1__SAMPLE_STRAINORLINE__DM' FROM DUAL;
SELECT 34  || chr(9) || 'targetcelltype'    || chr(9) || 'AE1__ASSAY_TARGETCELLTYPE__DM' || chr(9) || 'AE1__SAMPLE_TARGETCELLTYPE__DM' FROM DUAL;
SELECT 35  || chr(9) || 'temperature'       || chr(9) || 'AE1__ASSAY_TEMPERATURE__DM' || chr(9) || '' FROM DUAL;
SELECT 36  || chr(9) || 'testresult'        || chr(9) || 'AE1__ASSAY_TESTRESULT__DM' || chr(9) || 'AE1__SAMPLE_TESTRESULT__DM' FROM DUAL;
SELECT 37  || chr(9) || 'time'              || chr(9) || 'AE1__ASSAY_TIME__DM' || chr(9) || '' FROM DUAL;
SELECT 38  || chr(9) || 'tumorgrading'      || chr(9) || 'AE1__ASSAY_TUMORGRADING__DM' || chr(9) || 'AE1__SAMPLE_TUMORGRADING__DM' FROM DUAL;
SELECT 39  || chr(9) || 'vehicle'           || chr(9) || 'AE1__ASSAY_VEHICLE__DM' || chr(9) || '' FROM DUAL;
SELECT 104 || chr(9) || 'biometric'         || chr(9) || 'AE1__ASSAY_BIOMETRIC__DM' || chr(9) || 'AE1__SAMPLE_BIOMETRIC__DM' FROM DUAL;
SELECT 105 || chr(9) || 'cultivar'          || chr(9) || 'AE1__ASSAY_CULTIVAR__DM' || chr(9) || 'AE1__SAMPLE_CULTIVAR__DM' FROM DUAL;
SELECT 106 || chr(9) || 'diseaseloc'        || chr(9) || 'AE1__ASSAY_DISEASELOC__DM' || chr(9) || 'AE1__SAMPLE_DISEASELOC__DM' FROM DUAL;
SELECT 107 || chr(9) || 'envhistory'        || chr(9) || 'AE1__ASSAY_ENVHISTORY__DM' || chr(9) || 'AE1__SAMPLE_ENVHISTORY__DM' FROM DUAL;
SELECT 108 || chr(9) || 'familyhistory'     || chr(9) || 'AE1__ASSAY_FAMILYHISTORY__DM' || chr(9) || 'AE1__SAMPLE_FAMILYHISTORY__DM' FROM DUAL;
SELECT 109 || chr(9) || 'generation'        || chr(9) || 'AE1__ASSAY_GENERATION__DM' || chr(9) || '' FROM DUAL;
SELECT 110 || chr(9) || 'initialtime'       || chr(9) || 'AE1__ASSAY_INITIALTIME__DM' || chr(9) || 'AE1__SAMPLE_INITIALTIME__DM' FROM DUAL;
SELECT 111 || chr(9) || 'organismstatus'    || chr(9) || 'AE1__ASSAY_ORGANISMSTATUS__DM' || chr(9) || 'AE1__SAMPLE_ORGANISMSTATUS__DM' FROM DUAL;
SELECT 112 || chr(9) || 'performer'         || chr(9) || 'AE1__ASSAY_PERFORMER__DM' || chr(9) || '' FROM DUAL;
SELECT 113 || chr(9) || 'population'        || chr(9) || 'AE1__ASSAY_POPULATION__DM' || chr(9) || '' FROM DUAL;
SELECT 114 || chr(9) || 'qcdescrtype'       || chr(9) || 'AE1__ASSAY_QCDESCRTYPE__DM' || chr(9) || '' FROM DUAL;
SELECT 115 || chr(9) || 'sourceprovider'    || chr(9) || 'AE1__ASSAY_SOURCEPROVIDER__DM' || chr(9) || 'AE1__SAMPLE_SOURCEPROVIDER__DM' FROM DUAL;
SELECT 116 || chr(9) || 'testtype'          || chr(9) || 'AE1__ASSAY_TESTTYPE__DM' || chr(9) || 'AE1__SAMPLE_TESTTYPE__DM' FROM DUAL;

quit;
