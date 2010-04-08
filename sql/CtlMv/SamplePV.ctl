OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'DataMv/SamplePV.dat'
truncate into table a2_SamplePV
fields terminated by "\t" 
TRAILING NULLCOLS
(SAMPLEPVID 
,SAMPLEID
,PROPERTYID BOUNDFILLER  
,PROPERTYVALUEID "get_propertyvalueid_p (:PROPERTYID, :VALUE)"
,VALUE BOUNDFILLER
)

