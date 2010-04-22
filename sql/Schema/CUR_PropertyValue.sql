/*******************************************************************************
ediatable property values 
*******************************************************************************/
create or replace view CUR_PropertyValue as
select p.PropertyID, pv.PropertyValueID, P.Name Property, PV.Name Value
,(select count(1) from a2_assaypv apv where apv.propertyvalueid = pv.propertyvalueid) assays
,(select count(1) from a2_samplepv apv where apv.propertyvalueid = pv.propertyvalueid) samples
from a2_Property p
join a2_PropertyValue pv on pv.propertyid = p.propertyid;
/
exit;
/