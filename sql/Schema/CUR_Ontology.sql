/*******************************************************************************
select * from CUR_Ontology 
*******************************************************************************/
create or replace view CUR_Ontology as 
SELECT DISTINCT ACCESSION from a2_ontologyterm
/
exit;
/