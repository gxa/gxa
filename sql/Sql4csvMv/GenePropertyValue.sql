SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab off
set line 500
set wrap off
SET FEEDBACK OFF 
SET VERIFY OFF 

select ROWNUM         || chr(9) ||
       GenePropertyID || chr(9) ||
       value
from (
select distinct genepropertyid, TRIM(REPLACE(Value,'  ',' ')) value
from (
select distinct 2 as GenePropertyID,  VALUE from AE2__GENE_ENSGENE__DM UNION ALL
select distinct 3 as GenePropertyID,  VALUE from AE2__GENE_UNIPROT__DM UNION ALL
select distinct 4 as GenePropertyID,  VALUE from AE2__GENE_CAS__DM UNION ALL
select distinct 5 as GenePropertyID,  VALUE from AE2__GENE_CHEBI__DM UNION ALL
select distinct 6 as GenePropertyID,  VALUE from AE2__GENE_CHEMFORMULA__DM UNION ALL
select distinct 7 as GenePropertyID,  VALUE from AE2__GENE_DBXREF__DM UNION ALL
select distinct 9 as GenePropertyID,  VALUE from AE2__GENE_DISEASE__DM UNION ALL
select distinct 10 as GenePropertyID, VALUE from AE2__GENE_EMBL__DM UNION ALL
select distinct 11 as GenePropertyID, VALUE from AE2__GENE_ENSFAMILY__DM UNION ALL
select distinct 12 as GenePropertyID, VALUE from AE2__GENE_ENSPROTEIN__DM UNION ALL
select distinct 13 as GenePropertyID, VALUE from AE2__GENE_ENSTRANSCRIPT__DM UNION ALL
select distinct 14 as GenePropertyID, VALUE from AE2__GENE_GOTERM__DM UNION ALL
select distinct 15 as GenePropertyID, VALUE from AE2__GENE_GO__DM UNION ALL
select distinct 16 as GenePropertyID, VALUE from AE2__GENE_HMDB__DM UNION ALL
select distinct 18 as GenePropertyID, VALUE from AE2__GENE_IMAGE__DM UNION ALL
select distinct 19 as GenePropertyID, VALUE from AE2__GENE_INTERPROTERM__DM UNION ALL
select distinct 20 as GenePropertyID, VALUE from AE2__GENE_INTERPRO__DM UNION ALL
select distinct 21 as GenePropertyID, VALUE from AE2__GENE_KEYWORD__DM UNION ALL
select distinct 22 as GenePropertyID, VALUE from AE2__GENE_LOCUSLINK__DM UNION ALL
select distinct 23 as GenePropertyID, VALUE from AE2__GENE_OMIM__DM UNION ALL
select distinct 24 as GenePropertyID, VALUE from AE2__GENE_ORF__DM UNION ALL
select distinct 25 as GenePropertyID, VALUE from AE2__GENE_ORTHOLOG__DM UNION ALL
select distinct 26 as GenePropertyID, VALUE from AE2__GENE_PATHWAY__DM UNION ALL
select distinct 27 as GenePropertyID, VALUE from AE2__GENE_PROTEINNAME__DM UNION ALL
select distinct 28 as GenePropertyID, VALUE from AE2__GENE_REFSEQ__DM UNION ALL
select distinct 29 as GenePropertyID, VALUE from AE2__GENE_SYNONYM__DM UNION ALL
select distinct 30 as GenePropertyID, VALUE from AE2__GENE_UNIGENE__DM UNION ALL
select distinct 31 as GenePropertyID, VALUE from AE2__GENE_UNIPROTMETENZ__DM)
WHERE Value is not null);

quit;
