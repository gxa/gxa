OPTIONS(ROWS=100000,DIRECT=TRUE)
load data
infile 'Data/DesignElement.dat'
truncate into table a2_DesignElement
fields terminated by "\t" 
TRAILING NULLCOLS
(DESIGNELEMENTID,ARRAYDESIGNID,GENEID,ACCESSION,NAME,TYPE,ISCONTROL)

