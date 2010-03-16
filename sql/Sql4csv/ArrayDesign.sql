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


Select ARRAYDESIGNID || chr(9) ||
ACCESSION || chr(9) ||
TYPE || chr(9) ||
NAME || chr(9) ||
PROVIDER 
from A2_ARRAYDESIGN

/
quit;
