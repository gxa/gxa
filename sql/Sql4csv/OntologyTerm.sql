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
ONTOLOGYTERMID || chr(9) ||                 
ONTOLOGYID || chr(9) ||                     
TERM || chr(9) ||                           
ACCESSION || chr(9) ||                      
DESCRIPTION                     
from A2_ONTOLOGYTERM

/
quit;
