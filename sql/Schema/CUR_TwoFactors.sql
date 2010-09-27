/*******************************************************************************
select * from CUR_AllPropertyID
******************************************************************************/
create or replace view CUR_AllPropertyID as
select apv.AssayID
      ,NULL SampleID
      ,apv.PropertyValueID PropertyValueID
      ,NVL(apv.IsFactorValue,0) IsFactorValue
      ,1 IsAssay
from a2_AssayPV apv 
UNION ALL
select ass.AssayID
       ,spv.SampleID 
       ,spv.PropertyValueID PropertyValueID
       ,NVL(spv.IsFactorValue,0) IsFactorValue
       ,0 IsAssay
from a2_SamplePV spv
join a2_assaysample ass on ass.sampleid = spv.sampleid;
/
exit;
/

CREATE OR REPLACE TYPE CUR_TwoValues AS OBJECT
       (Value1  VARCHAR2(50),
        Value2  VARCHAR2(50));
/
CREATE OR REPLACE TYPE CUR_TwoValues_Table AS TABLE OF CUR_TwoValues;
/

/*******************************************************************************
select * from TABLE(CUR_ShowTwoValues('enee','menee'))
*******************************************************************************/
create or replace
FUNCTION CUR_ShowTwoValues(Experiment varchar2
                          ,Assay varchar2
                          ,property1 varchar2
                          ,property2 varchar2)
 RETURN CUR_TwoValues_Table 
 AS
r CUR_TwoValues_Table;
BEGIN
  SELECT CUR_TwoValues(Value1,Value2)
  BULK COLLECT INTO r
  FROM ( Select pv1.Value Value1
               ,pv2.Value Value1
         from CUR_AllPropertyID ap1
         join CUR_AllPropertyID ap2 on 1=1
         join a2_assay a on a.AssayID = ap1.AssayID = ap2.AssayID 
         join a2_experiment e on e.ExperimentID = a.experimentid
         join a2_PropertyValueID pv1 on pv1.PropertyValueID = ap1.PropertyValueID
         join a2_PropertyValueID pv2 on pv2.PropertyValueID = ap2.PropertyValueID
         join a2_property p1 on p1.PropertyID = pv1.PropertyID
         join a2_property p2 on p2.PropertyID = pv2.PropertyID
         where a.Accession = assay
         and e.Accession = experiment
         and p1.Name = property1
         and p2.Name = property2
  );  
  
  RETURN r;
END;


