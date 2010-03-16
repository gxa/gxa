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
PROPERTYID|| chr(9) ||                     
NAME|| chr(9) ||                           
AE1TABLENAME_ASSAY|| chr(9) ||             
AE1TABLENAME_SAMPLE|| chr(9) ||            
ASSAYPROPERTYID|| chr(9) ||                
SAMPLEPROPERTYID 
from A2_PROPERTY

/
quit;
