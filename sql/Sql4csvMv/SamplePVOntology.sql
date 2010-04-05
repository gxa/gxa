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
       SAMPLE_ID_KEY  || chr(9) ||
       ACCESSION      || chr(9) ||
       ONTOLOGY_ID_KEY|| chr(9) ||
       ORIG_VALUE     || chr(9) ||
       ORIG_VALUE_SRC
from ontology_annotation
where SAMPLE_ID_KEY is not null and
    exists(select 1 from AE1__SAMPLE__MAIN
    where AE1__SAMPLE__MAIN.SAMPLE_ID_KEY = ontology_annotation.SAMPLE_ID_KEY
    and   AE1__SAMPLE__MAIN.arraydesign_id is not null);

quit;


