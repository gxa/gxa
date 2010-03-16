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

Select
EXPERIMENTID || chr(9) ||
ACCESSION || chr(9) ||
DESCRIPTION || chr(9) ||
PERFORMER || chr(9) ||
LAB || chr(9) ||
LOADDATE  
from A2_EXPERIMENT

/
quit;
