SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab on
set line 500
set wrap off
SET FEEDBACK OFF
SET VERIFY OFF

Select ROWNUM || chr(9) || 1 || chr(9) || 1 || chr(9) || SAMPLE_ID_KEY ||chr(9) || ACCESSION||chr(9) || ONTOLOGY_ID_KEY||chr(9) || ORIG_VALUE||chr(9) || ORIG_VALUE_SRC
from ontology_annotation
where SAMPLE_ID_KEY is not null

/
quit;


