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
SAMPLEONTOLOGYID|| chr(9) ||               
SAMPLEID|| chr(9) ||                       
ONTOLOGYTERMID|| chr(9) ||                 
SOURCEMAPPING
from A2_SAMPLEONTOLOGY

/
quit;
