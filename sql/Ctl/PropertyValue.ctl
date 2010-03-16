OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/PropertyValue.dat'
truncate into table a2_PropertyValue
fields terminated by "\t" 
TRAILING NULLCOLS
(PROPERTYVALUEID,PROPERTYID,NAME)

