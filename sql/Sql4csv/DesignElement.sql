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
DESIGNELEMENTID || chr(9) ||                
ARRAYDESIGNID || chr(9) ||                  
GENEID || chr(9) ||                         
ACCESSION || chr(9) ||                      
NAME || chr(9) ||                           
TYPE || chr(9) ||                           
ISCONTROL  
from A2_DESIGNELEMENT

/
quit;
