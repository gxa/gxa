
  CREATE OR REPLACE PROCEDURE "AEMART"."A2_EXPERIMENTDEL" (Accession varchar2)
as
begin
  delete a2_experiment e where e.accession = Accession;
  
  ---  RAISE_APPLICATION_ERROR (-20734,'Employee must not be 18 years old.');
  
exception
  
  when no_data_found then
  RAISE_APPLICATION_ERROR (-20734,'Employee must be 18 years old.');

end;
/
 