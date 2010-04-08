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
SAMPLEID|| chr(9) ||                       
ACCESSION|| chr(9) ||                      
SPECIES|| chr(9) ||                        
CHANNEL
from A2_SAMPLE

/
quit;
