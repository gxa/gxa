/*******************************************************************************
merge two factors 'time_period:hour' and 'time_value:12', 
by creating new 'time:12 hour'

call CUR_MergeFactors('E-MEXP-123', 'time_value', 'time_period', 'time');
*******************************************************************************/
create or replace
PROCEDURE CUR_MergeFactors(Experiment varchar2, Property1 varchar2, Property2 varchar2, NewProperty varchar2)
AS
tValues CUR_TwoValues_Table;
NewPropertyID INTEGER;
OldProperty1ID INTEGER;
OldProperty2ID INTEGER; 
BEGIN
 SELECT CUR_TwoValues(Assay,Value1,Value2,SampleID)
  BULK COLLECT INTO tValues
 FROM TABLE(CUR_ShowTwoValues(Experiment,Property1,Property2)); 
 
 Select a2_Property_Seq.nextval 
 into NewPropertyID 
 from dual; 
 
 Insert into a2_Property(PropertyID,Name) values (NewPropertyID,NewProperty);

 Insert into a2_PropertyValue(PropertyID,Name)
 Select distinct NewPropertyID
      ,t.Value1 || ' ' || t.Value2
 From TABLE(tValues) t;     

 Select PropertyID INTO OldProperty1ID 
 from a2_property
 where Name = Property1;

 Select PropertyID INTO OldProperty2ID 
 from a2_property
 where Name = Property2;

 Insert into a2_AssayPV(AssayID,PropertyValueID)
 Select a.AssayID,pv.PropertyValueID 
 FROM TABLE(tValues) t
 JOIN a2_PropertyValue pv on pv.name = t.Value1 || ' ' || t.Value2
 JOIN a2_Assay a on a.accession = t.Assay 
                 and a.experimentid = (select ExperimentID from a2_Experiment where Accession = Experiment);

 Insert into a2_SamplePV(SampleID,PropertyValueID)
 Select ass.SampleID,pv.PropertyValueID 
 FROM TABLE(tValues) t
 JOIN a2_PropertyValue pv on pv.name = t.Value1 || ' ' || t.Value2
 JOIN a2_Assay a on a.accession = t.Assay 
                 and a.experimentid = (select ExperimentID from a2_Experiment where Accession = Experiment)
 JOIN a2_AssaySample ass on ass.AssayID = a.AssayID and ass.SampleID = t.SampleID;
 
 /* do not delete
 Delete From a2_AssayPV
 where AssayID in (select AssayID 
                   from a2_Assay  
                   where accession in (select Assay from TABLE(tValues))
                   and Experimentid = (select ExperimentID from a2_Experiment where Accession = Experiment)
                  )
 and PropertyValueID in (Select PropertyValueID from a2_PropertyValue where PropertyID in (OldProperty1ID,OldProperty2ID));
 */
  
END;
