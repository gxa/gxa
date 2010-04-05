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

select rownum           || chr(9) ||
       ontology_id_key  || chr(9) ||
       accession        || chr(9) ||
       description
from (
  select distinct ontology_id_key, accession, description
  from ontology_annotation);

quit;
