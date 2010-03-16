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
ONTOLOGYID|| chr(9) ||                     
NAME|| chr(9) ||                           
SOURCE_URI|| chr(9) ||                     
DESCRIPTION|| chr(9) ||                    
VERSION     
from A2_ONTOLOGY

/
quit;
