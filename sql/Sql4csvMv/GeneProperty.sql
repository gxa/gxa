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

SELECT 1  || chr(9) || 'ensembl'       || chr(9) || ''                            || chr(9) || 1  FROM DUAL;
SELECT 2  || chr(9) || 'ensgene'       || chr(9) || 'AE2__GENE_ENSGENE__DM'       || chr(9) || 2  FROM DUAL;
SELECT 3  || chr(9) || 'uniprot'       || chr(9) || 'AE2__GENE_UNIPROT__DM'       || chr(9) || 3  FROM DUAL;
SELECT 4  || chr(9) || 'cas'           || chr(9) || 'AE2__GENE_CAS__DM'           || chr(9) || '' FROM DUAL;
SELECT 5  || chr(9) || 'chebi'         || chr(9) || 'AE2__GENE_CHEBI__DM'         || chr(9) || '' FROM DUAL;
SELECT 6  || chr(9) || 'chemformula'   || chr(9) || 'AE2__GENE_CHEMFORMULA__DM'   || chr(9) || '' FROM DUAL;
SELECT 7  || chr(9) || 'dbxref'        || chr(9) || 'AE2__GENE_DBXREF__DM'        || chr(9) || '' FROM DUAL;
SELECT 8  || chr(9) || 'desc'          || chr(9) || 'AE2__GENE_DESC__DM'          || chr(9) || '' FROM DUAL;
SELECT 9  || chr(9) || 'disease'       || chr(9) || 'AE2__GENE_DISEASE__DM'       || chr(9) || '' FROM DUAL;
SELECT 10 || chr(9) || 'embl'          || chr(9) || 'AE2__GENE_EMBL__DM'          || chr(9) || '' FROM DUAL;
SELECT 11 || chr(9) || 'ensfamily'     || chr(9) || 'AE2__GENE_ENSFAMILY__DM'     || chr(9) || '' FROM DUAL;
SELECT 12 || chr(9) || 'ensprotein'    || chr(9) || 'AE2__GENE_ENSPROTEIN__DM'    || chr(9) || '' FROM DUAL;
SELECT 13 || chr(9) || 'enstranscript' || chr(9) || 'AE2__GENE_ENSTRANSCRIPT__DM' || chr(9) || '' FROM DUAL;
SELECT 14 || chr(9) || 'goterm'        || chr(9) || 'AE2__GENE_GOTERM__DM'        || chr(9) || '' FROM DUAL;
SELECT 15 || chr(9) || 'go'            || chr(9) || 'AE2__GENE_GO__DM'            || chr(9) || '' FROM DUAL;
SELECT 16 || chr(9) || 'hmdb'          || chr(9) || 'AE2__GENE_HMDB__DM'          || chr(9) || '' FROM DUAL;
SELECT 17 || chr(9) || 'ids'           || chr(9) || 'AE2__GENE_IDS__DM'           || chr(9) || '' FROM DUAL;
SELECT 18 || chr(9) || 'image'         || chr(9) || 'AE2__GENE_IMAGE__DM'         || chr(9) || '' FROM DUAL;
SELECT 19 || chr(9) || 'interproterm'  || chr(9) || 'AE2__GENE_INTERPROTERM__DM'  || chr(9) || '' FROM DUAL;
SELECT 20 || chr(9) || 'interpro'      || chr(9) || 'AE2__GENE_INTERPRO__DM'      || chr(9) || '' FROM DUAL;
SELECT 21 || chr(9) || 'keyword'       || chr(9) || 'AE2__GENE_KEYWORD__DM'       || chr(9) || '' FROM DUAL;
SELECT 22 || chr(9) || 'locuslink'     || chr(9) || 'AE2__GENE_LOCUSLINK__DM'     || chr(9) || '' FROM DUAL;
SELECT 23 || chr(9) || 'omim'          || chr(9) || 'AE2__GENE_OMIM__DM'          || chr(9) || '' FROM DUAL;
SELECT 24 || chr(9) || 'orf'           || chr(9) || 'AE2__GENE_ORF__DM'           || chr(9) || '' FROM DUAL;
SELECT 25 || chr(9) || 'ortholog'      || chr(9) || 'AE2__GENE_ORTHOLOG__DM'      || chr(9) || '' FROM DUAL;
SELECT 26 || chr(9) || 'pathway'       || chr(9) || 'AE2__GENE_PATHWAY__DM'       || chr(9) || '' FROM DUAL;
SELECT 27 || chr(9) || 'proteinname'   || chr(9) || 'AE2__GENE_PROTEINNAME__DM'   || chr(9) || '' FROM DUAL;
SELECT 28 || chr(9) || 'refseq'        || chr(9) || 'AE2__GENE_REFSEQ__DM'        || chr(9) || '' FROM DUAL;
SELECT 29 || chr(9) || 'synonym'       || chr(9) || 'AE2__GENE_SYNONYM__DM'       || chr(9) || '' FROM DUAL;
SELECT 30 || chr(9) || 'unigene'       || chr(9) || 'AE2__GENE_UNIGENE__DM'       || chr(9) || '' FROM DUAL;
SELECT 31 || chr(9) || 'uniprotmetenz' || chr(9) || 'AE2__GENE_UNIPROTMETENZ__DM' || chr(9) || '' FROM DUAL;

quit;
