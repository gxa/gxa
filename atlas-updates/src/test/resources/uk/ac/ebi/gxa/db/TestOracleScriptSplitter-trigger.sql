CREATE SEQUENCE  "A2_ONTOLOGY_SEQ";

CREATE OR REPLACE
TRIGGER A2_ONTOLOGY_INSERT
before insert on A2_Ontology
for each row
begin
-- test
if( :new.OntologyID is null) then
select A2_Ontology_seq.nextval into :new.OntologyID from dual;
end if;
end;
/

ALTER TRIGGER A2_ONTOLOGY_INSERT ENABLE;

call atlasmgr.RebuildSequences();
