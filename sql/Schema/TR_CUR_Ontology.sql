/*******************************************************************************

Insert Into CUR_Ontology (Accession) values ('EFO_FIFO');

select * from a2_OntologyTerm where Accession = 'EFO_FIFO'

*******************************************************************************/
create or replace
trigger TR_CUR_Ontology instead of INSERT on CUR_Ontology
--referencing old as old1 new as new1
for each row 
declare 
  mOntologyID int;
begin

  select OntologyID into mOntologyID from a2_Ontology where name = 'EFO';
  
MERGE INTO a2_OntologyTerm ot 
USING (select :new.Accession Accession from dual) t
   ON (ot.Accession = t.Accession)
   WHEN NOT MATCHED THEN INSERT (OntologyID,Accession) 
                         VALUES (mOntologyID,:new.Accession);
 
end;
/
exit;
/
