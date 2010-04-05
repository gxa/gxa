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

select ROWNUM        || chr(9) ||
       ASSAY_ID_KEY  || chr(9) ||
       PropertyID    || chr(9) ||
       VALUE
from (
select distinct assay_id_key, propertyid, trim(replace(value,'  ',' ')) value
from (                                      
select distinct ASSAY_ID_KEY, 1 as PropertyID, Value from AE1__ASSAY_RNAI__DM UNION ALL                                                                                                                                                                                                                                                                                     
select distinct ASSAY_ID_KEY, 2 as PropertyID, Value from AE1__ASSAY_AGE__DM UNION ALL                                                                                                                                                                                                                                                                                      
select distinct ASSAY_ID_KEY, 3 as PropertyID, Value from AE1__ASSAY_CELLLINE__DM UNION ALL                                                                                                                                                                                                                                                                                 
select distinct ASSAY_ID_KEY, 4 as PropertyID, Value from AE1__ASSAY_CELLTYPE__DM UNION ALL                                                                                                                                                                                                                                                                                 
select distinct ASSAY_ID_KEY, 5 as PropertyID, Value from AE1__ASSAY_CLINHISTORY__DM UNION ALL                                                                                                                                                                                                                                                                              
select distinct ASSAY_ID_KEY, 6 as PropertyID, Value from AE1__ASSAY_CLININFO__DM UNION ALL                                                                                                                                                                                                                                                                                 
select distinct ASSAY_ID_KEY, 7 as PropertyID, Value from AE1__ASSAY_CLINTREATMENT__DM UNION ALL                                                                                                                                                                                                                                                                            
select distinct ASSAY_ID_KEY, 8 as PropertyID, Value from AE1__ASSAY_COMPOUND__DM UNION ALL                                                                                                                                                                                                                                                                                 
select distinct ASSAY_ID_KEY, 9 as PropertyID, Value from AE1__ASSAY_DEVSTAGE__DM UNION ALL                                                                                                                                                                                                                                                                                 
select distinct ASSAY_ID_KEY, 11 as PropertyID, Value from AE1__ASSAY_DISEASESTAGING__DM UNION ALL                                                                                                                                                                                                                                                                          
select distinct ASSAY_ID_KEY, 12 as PropertyID, Value from AE1__ASSAY_DISEASESTATE__DM UNION ALL                                                                                                                                                                                                                                                                            
select distinct ASSAY_ID_KEY, 13 as PropertyID, Value from AE1__ASSAY_DOSE__DM UNION ALL                                                                                                                                                                                                                                                                                    
select distinct ASSAY_ID_KEY, 14 as PropertyID, Value from AE1__ASSAY_ECOTYPE__DM UNION ALL                                                                                                                                                                                                                                                                                 
select distinct ASSAY_ID_KEY, 15 as PropertyID, Value from AE1__ASSAY_GENMODIF__DM UNION ALL                                                                                                                                                                                                                                                                                
select distinct ASSAY_ID_KEY, 16 as PropertyID, Value from AE1__ASSAY_GENOTYPE__DM UNION ALL                                                                                                                                                                                                                                                                                
select distinct ASSAY_ID_KEY, 17 as PropertyID, Value from AE1__ASSAY_GROWTHCONDITION__DM UNION ALL                                                                                                                                                                                                                                                                         
select distinct ASSAY_ID_KEY, 18 as PropertyID, Value from AE1__ASSAY_HISTOLOGY__DM UNION ALL                                                                                                                                                                                                                                                                               
select distinct ASSAY_ID_KEY, 19 as PropertyID, Value from AE1__ASSAY_INDGENETICCHAR__DM UNION ALL                                                                                                                                                                                                                                                                          
select distinct ASSAY_ID_KEY, 20 as PropertyID, Value from AE1__ASSAY_INDIVIDUAL__DM UNION ALL                                                                                                                                                                                                                                                                              
select distinct ASSAY_ID_KEY, 21 as PropertyID, Value from AE1__ASSAY_INFECT__DM UNION ALL                                                                                                                                                                                                                                                                                  
select distinct ASSAY_ID_KEY, 22 as PropertyID, Value from AE1__ASSAY_INJURY__DM UNION ALL                                                                                                                                                                                                                                                                                  
select distinct ASSAY_ID_KEY, 23 as PropertyID, Value from AE1__ASSAY_LIGHT__DM UNION ALL                                                                                                                                                                                                                                                                                   
select distinct ASSAY_ID_KEY, 24 as PropertyID, Value from AE1__ASSAY_MATERIALTYPE__DM UNION ALL                                                                                                                                                                                                                                                                            
select distinct ASSAY_ID_KEY, 25 as PropertyID, Value from AE1__ASSAY_MEDIA__DM UNION ALL                                                                                                                                                                                                                                                                                   
select distinct ASSAY_ID_KEY, 26 as PropertyID, Value from AE1__ASSAY_OBSERVATION__DM UNION ALL                                                                                                                                                                                                                                                                             
select distinct ASSAY_ID_KEY, 27 as PropertyID, Value from AE1__ASSAY_ORGANISM__DM UNION ALL                                                                                                                                                                                                                                                                                
select distinct ASSAY_ID_KEY, 28 as PropertyID, Value from AE1__ASSAY_ORGANISMPART__DM UNION ALL                                                                                                                                                                                                                                                                            
select distinct ASSAY_ID_KEY, 29 as PropertyID, Value from AE1__ASSAY_PHENOTYPE__DM UNION ALL                                                                                                                                                                                                                                                                               
select distinct ASSAY_ID_KEY, 30 as PropertyID, Value from AE1__ASSAY_PROTOCOLTYPE__DM UNION ALL                                                                                                                                                                                                                                                                            
select distinct ASSAY_ID_KEY, 31 as PropertyID, Value from AE1__ASSAY_REPLICATE__DM UNION ALL                                                                                                                                                                                                                                                                               
select distinct ASSAY_ID_KEY, 32 as PropertyID, Value from AE1__ASSAY_SEX__DM UNION ALL                                                                                                                                                                                                                                                                                     
select distinct ASSAY_ID_KEY, 33 as PropertyID, Value from AE1__ASSAY_STRAINORLINE__DM UNION ALL                                                                                                                                                                                                                                                                            
select distinct ASSAY_ID_KEY, 34 as PropertyID, Value from AE1__ASSAY_TARGETCELLTYPE__DM UNION ALL                                                                                                                                                                                                                                                                          
select distinct ASSAY_ID_KEY, 35 as PropertyID, Value from AE1__ASSAY_TEMPERATURE__DM UNION ALL                                                                                                                                                                                                                                                                             
select distinct ASSAY_ID_KEY, 36 as PropertyID, Value from AE1__ASSAY_TESTRESULT__DM UNION ALL
select distinct ASSAY_ID_KEY, 37 as PropertyID, Value from AE1__ASSAY_TIME__DM UNION ALL
select distinct ASSAY_ID_KEY, 38 as PropertyID, Value from AE1__ASSAY_TUMORGRADING__DM UNION ALL
select distinct ASSAY_ID_KEY, 39 as PropertyID, Value from AE1__ASSAY_VEHICLE__DM UNION ALL
select distinct ASSAY_ID_KEY, 104 as PropertyID, Value from AE1__ASSAY_BIOMETRIC__DM UNION ALL
select distinct ASSAY_ID_KEY, 105 as PropertyID, Value from AE1__ASSAY_CULTIVAR__DM UNION ALL
select distinct ASSAY_ID_KEY, 106 as PropertyID, Value from AE1__ASSAY_DISEASELOC__DM UNION ALL
select distinct ASSAY_ID_KEY, 107 as PropertyID, Value from AE1__ASSAY_ENVHISTORY__DM UNION ALL
select distinct ASSAY_ID_KEY, 108 as PropertyID, Value from AE1__ASSAY_FAMILYHISTORY__DM UNION ALL
select distinct ASSAY_ID_KEY, 109 as PropertyID, Value from AE1__ASSAY_GENERATION__DM UNION ALL
select distinct ASSAY_ID_KEY, 110 as PropertyID, Value from AE1__ASSAY_INITIALTIME__DM UNION ALL
select distinct ASSAY_ID_KEY, 111 as PropertyID, Value from AE1__ASSAY_ORGANISMSTATUS__DM UNION ALL
select distinct ASSAY_ID_KEY, 112 as PropertyID, Value from AE1__ASSAY_PERFORMER__DM UNION ALL
select distinct ASSAY_ID_KEY, 113 as PropertyID, Value from AE1__ASSAY_POPULATION__DM UNION ALL
select distinct ASSAY_ID_KEY, 114 as PropertyID, Value from AE1__ASSAY_QCDESCRTYPE__DM UNION ALL
select distinct ASSAY_ID_KEY, 115 as PropertyID, Value from AE1__ASSAY_SOURCEPROVIDER__DM UNION ALL
select distinct ASSAY_ID_KEY, 116 as PropertyID, Value from AE1__ASSAY_TESTTYPE__DM ) t
WHERE Value is not null
AND EXISTS
  (select 1 from AE1__ASSAY__MAIN
    where AE1__ASSAY__MAIN.ASSAY_ID_KEY = t.ASSAY_ID_KEY
      AND AE1__ASSAY__MAIN.ARRAYDESIGN_ID is not null));

quit;
