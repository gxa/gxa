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

select ontology_id_key || chr(9) ||
       name            || chr(9) ||
       source_uri      || chr(9) ||
       description     || chr(9) ||
       version
from ontology_sources;

quit;
