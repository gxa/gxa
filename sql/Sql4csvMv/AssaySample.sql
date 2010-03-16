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
ASSAYSAMPLEID || chr(9) ||                  
ASSAYID || chr(9) ||                        
SAMPLEID     
from A2_ASSAYSAMPLE

/
quit;
