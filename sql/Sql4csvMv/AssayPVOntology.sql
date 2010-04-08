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

Select ROWNUM         || chr(9) ||
       ASSAY_ID_KEY   || chr(9) ||
       ACCESSION      || chr(9) || 
       ONTOLOGY_ID_KEY|| chr(9) ||
       ORIG_VALUE     || chr(9) ||
       ORIG_VALUE_SRC
from ontology_annotation
where ASSAY_ID_KEY is not null
AND EXISTS
  (select 1 from AE1__ASSAY__MAIN
    where AE1__ASSAY__MAIN.ASSAY_ID_KEY = ONTOLOGY_ANNOTATION.ASSAY_ID_KEY
      AND AE1__ASSAY__MAIN.ARRAYDESIGN_ID is not null);

quit;


