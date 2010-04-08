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

select ROWNUM || chr(9) || GenePropertyID || chr(9) || Value from (
select distinct GenePropertyID, Value from (
--ensgene
select 2 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_ENSGENE__DM 
UNION ALL
--uniprot
select 3 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_UNIPROT__DM 
UNION ALL
--cas
select 4 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_CAS__DM 
UNION ALL
--chebi
select 5 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_CHEBI__DM 
UNION ALL
--chemformula
select 6 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_CHEMFORMULA__DM 
UNION ALL
--dbxref
select 7 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_DBXREF__DM 
UNION ALL
--desc
select 8 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_DESC__DM 
UNION ALL
--disease
select 9 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_DISEASE__DM 
UNION ALL
--embl
select 10 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_EMBL__DM 
UNION ALL
--ensfamily
select 11 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_ENSFAMILY__DM 
UNION ALL
--ensprotein
select 12 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_ENSPROTEIN__DM 
UNION ALL
--enstranscript
select 13 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_ENSTRANSCRIPT__DM 
UNION ALL
--goterm
select 14 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_GOTERM__DM 
UNION ALL
--go
select 15 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_GO__DM 
UNION ALL
--hmdb
select 16 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_HMDB__DM 
UNION ALL
--image
select 18 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_IMAGE__DM 
UNION ALL
--interproterm
select 19 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_INTERPROTERM__DM 
UNION ALL
--interpro
select 20 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_INTERPRO__DM 
UNION ALL
--keyword
select 21 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_KEYWORD__DM 
UNION ALL
--locuslink
select 22 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_LOCUSLINK__DM 
UNION ALL
--omim
select 23 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_OMIM__DM 
UNION ALL
--orf
select 24 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_ORF__DM 
UNION ALL
--ortholog
select 25 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_ORTHOLOG__DM 
UNION ALL
--pathway
select 26 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_PATHWAY__DM 
UNION ALL
--proteinname
select 27 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_PROTEINNAME__DM 
UNION ALL
--refseq
select 28 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_REFSEQ__DM 
UNION ALL
--synonym
select 29 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_SYNONYM__DM 
UNION ALL
--unigene
select 30 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_UNIGENE__DM 
UNION ALL
--uniprotmetenz
select 31 as GenePropertyID, GENE_ID_KEY, VALUE from AE2__GENE_UNIPROTMETENZ__DM 
) t where GENE_ID_KEY > 0 and length(VALUE) > 0 order by GenePropertyID, Value ) t1

/
quit;
