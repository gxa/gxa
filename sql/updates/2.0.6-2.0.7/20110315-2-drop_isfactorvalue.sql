alter table A2_ASSAYPV drop column ISFACTORVALUE;
alter table A2_SAMPLEPV drop column ISFACTORVALUE;

CREATE OR REPLACE VIEW "VWEXPERIMENTFACTORS" ("EXPERIMENTID", "PROPERTYID", "PROPERTYVALUEID") AS
select distinct a. ExperimentID, pv.PropertyID, pv.PropertyValueID
from a2_assayPV apv
join a2_propertyvalue pv on apv.propertyvalueid = pv.propertyvalueid
join a2_assay a on a.assayid = apv.assayid;
