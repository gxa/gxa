begin
 dbms_stats.gather_schema_stats('ATLAS2');
end;

select TABLE_NAME, NUM_rows from dba_tables
where owner = 'ATLAS2' and
TABLE_NAME like 'A2_%'
order by Num_ROWS desc

select * from 

11696436

select * 
from a2_propertyvalue pv
join a2_property p on p.propertyid = pv.propertyid
WHERE pv.name='Cape Verde Islands'

select PropertyID, Name, count(1) from a2_propertyvalue
where name is not null
group by PropertyID, Name
having count(1) > 1
