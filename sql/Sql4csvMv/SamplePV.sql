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

select ROWNUM || chr(9) || v from (
select distinct Sample_ID_KEY || chr(9) || 2 || chr(9) || Null || chr(9) || Value v from AE1__SAMPLE_AGE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                                    
select distinct Sample_ID_KEY || chr(9) || 3 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_CELLLINE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                               
select distinct Sample_ID_KEY || chr(9) || 4 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_CELLTYPE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                               
select distinct Sample_ID_KEY || chr(9) || 5 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_CLINHISTORY__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                            
select distinct Sample_ID_KEY || chr(9) || 6 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_CLININFO__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                               
select distinct Sample_ID_KEY || chr(9) || 7 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_CLINTREATMENT__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                          
select distinct Sample_ID_KEY || chr(9) || 9 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_DEVSTAGE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                               
select distinct Sample_ID_KEY || chr(9) || 11 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_DISEASESTAGING__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                        
select distinct Sample_ID_KEY || chr(9) || 12 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_DISEASESTATE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                          
select distinct Sample_ID_KEY || chr(9) || 14 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_ECOTYPE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                               
select distinct Sample_ID_KEY || chr(9) || 15 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_GENMODIF__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                              
select distinct Sample_ID_KEY || chr(9) || 16 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_GENOTYPE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                              
select distinct Sample_ID_KEY || chr(9) || 18 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_HISTOLOGY__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                             
select distinct Sample_ID_KEY || chr(9) || 19 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_INDGENETICCHAR__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                        
select distinct Sample_ID_KEY || chr(9) || 20 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_INDIVIDUAL__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                            
select distinct Sample_ID_KEY || chr(9) || 26 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_OBSERVATION__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                           
select distinct Sample_ID_KEY || chr(9) || 28 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_ORGANISMPART__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                          
select distinct Sample_ID_KEY || chr(9) || 29 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_PHENOTYPE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                             
select distinct Sample_ID_KEY || chr(9) || 32 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_SEX__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                                   
select distinct Sample_ID_KEY || chr(9) || 33 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_STRAINORLINE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                          
select distinct Sample_ID_KEY || chr(9) || 34 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_TARGETCELLTYPE__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                        
select distinct Sample_ID_KEY || chr(9) || 36 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_TESTRESULT__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                            
select distinct Sample_ID_KEY || chr(9) || 38 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_TUMORGRADING__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                          
select distinct Sample_ID_KEY || chr(9) || 104 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_BIOMETRIC__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                            
select distinct Sample_ID_KEY || chr(9) || 105 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_CULTIVAR__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                             
select distinct Sample_ID_KEY || chr(9) || 106 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_DISEASELOC__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                           
select distinct Sample_ID_KEY || chr(9) || 107 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_ENVHISTORY__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                           
select distinct Sample_ID_KEY || chr(9) || 108 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_FAMILYHISTORY__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                        
select distinct Sample_ID_KEY || chr(9) || 110 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_INITIALTIME__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                          
select distinct Sample_ID_KEY || chr(9) || 111 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_ORGANISMSTATUS__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                       
select distinct Sample_ID_KEY || chr(9) || 115 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_SOURCEPROVIDER__DM where value is not null UNION ALL                                                                                                                                                                                                                                                                       
select distinct Sample_ID_KEY || chr(9) || 116 || chr(9) || Null || chr(9) || Value from AE1__SAMPLE_TESTTYPE__DM where value is not null)     

/
quit;


