OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'DataMv/AssayPV.dat'
truncate into table a2_AssayPV
fields terminated by "\t" 
TRAILING NULLCOLS
(ASSAYPVID 
,ASSAYID
,PROPERTYID BOUNDFILLER  
,PROPERTYVALUEID "get_propertyvalueid_p (:PROPERTYID, :VALUE)"
,VALUE BOUNDFILLER
)

