/*******************************************************************************
Table-Value function, which returns all values for given two properties
*******************************************************************************/
create or replace
FUNCTION CUR_ShowTwoValues(Experiment varchar2
                          ,property1 varchar2
                          ,property2 varchar2)
 RETURN CUR_TwoValues_Table 
 AS
r CUR_TwoValues_Table;
BEGIN
  SELECT CUR_TwoValues(Assay,Value1,Value2)
  BULK COLLECT INTO r
  FROM ( Select distinct a.Accession Assay
                ,pv1.Name Value1
                ,pv2.Name Value2
         from CUR_AllPropertyID ap1
         join CUR_AllPropertyID ap2 on 1=1
         join a2_assay a on a.AssayID = ap1.AssayID and a.AssayID = ap2.AssayID 
         join a2_experiment e on e.ExperimentID = a.experimentid
         join a2_PropertyValue pv1 on pv1.PropertyValueID = ap1.PropertyValueID
         join a2_PropertyValue pv2 on pv2.PropertyValueID = ap2.PropertyValueID
         join a2_property p1 on p1.PropertyID = pv1.PropertyID
         join a2_property p2 on p2.PropertyID = pv2.PropertyID
         where e.Accession = experiment
         and p1.Name = property1
         and p2.Name = property2
  );  
  
  RETURN r;
END;


