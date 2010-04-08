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

SELECT ROWNUM || chr(9) || NULL || chr(9) || t.GENE_ID_KEY || chr(9) || t.GenePropertyID || chr(9) || t.value
FROM(
select GENE_ID_KEY,2 GenePropertyID, value from AE2__GENE_ENSGENE__DM                                                                                                                                                                                                                                                                                              
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,3 GenePropertyID, value from AE2__GENE_UNIPROT__DM                                                                                                                                                                                                                                                                                              
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,4 GenePropertyID, value from AE2__GENE_CAS__DM                                                                                                                                                                                                                                                                                                  
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,5 GenePropertyID, value from AE2__GENE_CHEBI__DM                                                                                                                                                                                                                                                                                                
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,6 GenePropertyID, value from AE2__GENE_CHEMFORMULA__DM                                                                                                                                                                                                                                                                                          
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,7 GenePropertyID, value from AE2__GENE_DBXREF__DM                                                                                                                                                                                                                                                                                               
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,8 GenePropertyID, value from AE2__GENE_DESC__DM                                                                                                                                                                                                                                                                                                 
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,9 GenePropertyID, value from AE2__GENE_DISEASE__DM                                                                                                                                                                                                                                                                                              
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,10 GenePropertyID, value from AE2__GENE_EMBL__DM                                                                                                                                                                                                                                                                                                
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,11 GenePropertyID, value from AE2__GENE_ENSFAMILY__DM                                                                                                                                                                                                                                                                                           
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,12 GenePropertyID, value from AE2__GENE_ENSPROTEIN__DM                                                                                                                                                                                                                                                                                          
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,13 GenePropertyID, value from AE2__GENE_ENSTRANSCRIPT__DM                                                                                                                                                                                                                                                                                       
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,14 GenePropertyID, value from AE2__GENE_GOTERM__DM                                                                                                                                                                                                                                                                                              
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,15 GenePropertyID, value from AE2__GENE_GO__DM                                                                                                                                                                                                                                                                                                  
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,16 GenePropertyID, value from AE2__GENE_HMDB__DM                                                                                                                                                                                                                                                                                                
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,18 GenePropertyID, value from AE2__GENE_IMAGE__DM                                                                                                                                                                                                                                                                                               
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,19 GenePropertyID, value from AE2__GENE_INTERPROTERM__DM                                                                                                                                                                                                                                                                                        
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,20 GenePropertyID, value from AE2__GENE_INTERPRO__DM                                                                                                                                                                                                                                                                                            
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,21 GenePropertyID, value from AE2__GENE_KEYWORD__DM                                                                                                                                                                                                                                                                                             
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,22 GenePropertyID, value from AE2__GENE_LOCUSLINK__DM                                                                                                                                                                                                                                                                                           
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,23 GenePropertyID, value from AE2__GENE_OMIM__DM                                                                                                                                                                                                                                                                                                
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,24 GenePropertyID, value from AE2__GENE_ORF__DM                                                                                                                                                                                                                                                                                                 
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,25 GenePropertyID, value from AE2__GENE_ORTHOLOG__DM                                                                                                                                                                                                                                                                                            
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,26 GenePropertyID, value from AE2__GENE_PATHWAY__DM                                                                                                                                                                                                                                                                                             
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,27 GenePropertyID, value from AE2__GENE_PROTEINNAME__DM                                                                                                                                                                                                                                                                                         
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,28 GenePropertyID, value from AE2__GENE_REFSEQ__DM                                                                                                                                                                                                                                                                                              
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,29 GenePropertyID, value from AE2__GENE_SYNONYM__DM                                                                                                                                                                                                                                                                                             
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,30 GenePropertyID, value from AE2__GENE_UNIGENE__DM                                                                                                                                                                                                                                                                                             
 UNION ALL                                                                                                                                                                                                                                                                                                                                                        
select GENE_ID_KEY,31 GenePropertyID, value from AE2__GENE_UNIPROTMETENZ__DM                                                                                                                                                                                                                                                                                       
) t 
where t.Gene_ID_KEY is not null
and t.value is not null
order by GenePropertyID, Gene_ID_Key, value
/
quit;
