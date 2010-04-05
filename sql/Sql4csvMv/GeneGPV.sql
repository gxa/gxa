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

SELECT ROWNUM         || chr(9) ||
       GENE_ID_KEY    || chr(9) ||
       GenePropertyID || chr(9) ||
       value
FROM (
       select distinct gene_id_key, genepropertyid, TRIM(REPLACE(value,'  ',' ')) value
FROM (
select distinct GENE_ID_KEY, 2 GenePropertyID, value from AE2__GENE_ENSGENE__DM UNION ALL
select distinct GENE_ID_KEY, 3 GenePropertyID, value from AE2__GENE_UNIPROT__DM UNION ALL
select distinct GENE_ID_KEY, 4 GenePropertyID, value from AE2__GENE_CAS__DM UNION ALL
select distinct GENE_ID_KEY, 5 GenePropertyID, value from AE2__GENE_CHEBI__DM UNION ALL
select distinct GENE_ID_KEY, 6 GenePropertyID, value from AE2__GENE_CHEMFORMULA__DM UNION ALL
select distinct GENE_ID_KEY, 7 GenePropertyID, value from AE2__GENE_DBXREF__DM UNION ALL
select distinct GENE_ID_KEY, 9 GenePropertyID, value from AE2__GENE_DISEASE__DM UNION ALL
select distinct GENE_ID_KEY, 10 GenePropertyID, value from AE2__GENE_EMBL__DM UNION ALL
select distinct GENE_ID_KEY, 11 GenePropertyID, value from AE2__GENE_ENSFAMILY__DM UNION ALL
select distinct GENE_ID_KEY, 12 GenePropertyID, value from AE2__GENE_ENSPROTEIN__DM UNION ALL
select distinct GENE_ID_KEY, 13 GenePropertyID, value from AE2__GENE_ENSTRANSCRIPT__DM UNION ALL
select distinct GENE_ID_KEY, 14 GenePropertyID, value from AE2__GENE_GOTERM__DM UNION ALL
select distinct GENE_ID_KEY, 15 GenePropertyID, value from AE2__GENE_GO__DM UNION ALL
select distinct GENE_ID_KEY, 16 GenePropertyID, value from AE2__GENE_HMDB__DM UNION ALL
select distinct GENE_ID_KEY, 18 GenePropertyID, value from AE2__GENE_IMAGE__DM UNION ALL
select distinct GENE_ID_KEY, 19 GenePropertyID, value from AE2__GENE_INTERPROTERM__DM UNION ALL
select distinct GENE_ID_KEY, 20 GenePropertyID, value from AE2__GENE_INTERPRO__DM UNION ALL
select distinct GENE_ID_KEY, 21 GenePropertyID, value from AE2__GENE_KEYWORD__DM UNION ALL
select distinct GENE_ID_KEY, 22 GenePropertyID, value from AE2__GENE_LOCUSLINK__DM UNION ALL
select distinct GENE_ID_KEY, 23 GenePropertyID, value from AE2__GENE_OMIM__DM UNION ALL
select distinct GENE_ID_KEY, 24 GenePropertyID, value from AE2__GENE_ORF__DM UNION ALL
select distinct GENE_ID_KEY, 25 GenePropertyID, value from AE2__GENE_ORTHOLOG__DM UNION ALL
select distinct GENE_ID_KEY, 26 GenePropertyID, value from AE2__GENE_PATHWAY__DM UNION ALL
select distinct GENE_ID_KEY, 27 GenePropertyID, value from AE2__GENE_PROTEINNAME__DM UNION ALL
select distinct GENE_ID_KEY, 28 GenePropertyID, value from AE2__GENE_REFSEQ__DM UNION ALL
select distinct GENE_ID_KEY, 29 GenePropertyID, value from AE2__GENE_SYNONYM__DM UNION ALL
select distinct GENE_ID_KEY, 30 GenePropertyID, value from AE2__GENE_UNIGENE__DM UNION ALL
select distinct GENE_ID_KEY, 31 GenePropertyID, value from AE2__GENE_UNIPROTMETENZ__DM) t
where gene_id_key > 0 and value is not null);

quit;
