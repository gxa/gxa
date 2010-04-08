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

Select ASSAYID || chr(9) ||                        
ACCESSION || chr(9) ||                      
EXPERIMENTID || chr(9) ||                   
ARRAYDESIGNID 
from A2_ASSAY

/
quit;
