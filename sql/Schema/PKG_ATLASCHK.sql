CREATE OR REPLACE PACKAGE ATLASCHK
IS

PROCEDURE ValidateData;

END;
/

/*******************************************************************************
BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY
*******************************************************************************/

CREATE OR REPLACE PACKAGE BODY ATLASCHK
AS
  LastName varchar2(8000);

function add_error(name varchar2, val varchar2, err varchar2)
  return varchar2
AS  
  errr varchar2(8000);
begin
  errr := err;

if(name = LastName) then
  null;
else
  LastName = name;
end if 

if(LENGTH(NVL(err,'')) < 50) then 
    if(LENGTH(NVL(err,''))<5) then
      errr := 'Experiments w/o assay:';
    else  
      errr := errr || ',';
    end if; 
    errr :=  errr || val; 
  else 
    if(err LIKE '%,...') then
      null;
    else  
     errr := errr || ',...';
    end if; 
  end if;
  
  return errr;
end;

--------------------------------------------------------------------------------

PROCEDURE ValidateData
AS
  err varchar2(8000) := '';    
  errr varchar2(8000) := ''; 
BEGIN 

--experiments w/o assays
for r in (select Accession 
            from a2_Experiment 
           where not exists (select 1 
                             from a2_Assay 
                             where a2_Assay.ExperimentID = a2_Experiment.ExperimentID 
                             and 1=0)) 
loop
  err := add_error('Experiments w/o assays:',r.Accession, err);                                                                  
end loop;

--assays w/o samples
for r in (select Accession 
            from a2_Assay
            where not exists (select 1 
                              from a2_AssaySample
                              where a2_AssaySample.AssayID = a2_Assay.AssayID 
                              and 1=0)) 
loop
  err := add_error('Assays w/o samples:',r.Accession, err);
end loop;

--samples w/o assays
for r in (select Accession
            from a2_Sample
            where not exists (select 1 
                              from a2_AssaySample
                              where a2_AssaySample.SampleID = a2_Sample.SampleID 
                              and 1=0)) 
loop
  err := add_error('Samples w/o assays:',r.Accession, err);
end loop;

--experiment w/o factors
for r in (select Accession
            from a2_Experiment
            where not exists (select 1 
                              from vwExperimentFactors
                              where vwExperimentFactors.ExperimentID = a2_Experiment.ExperimentID 
                              and 1=0)) 
loop
  err := add_error('Experiments w/o factors:',r.Accession, err);
end loop;

--assays w/o propertyvalues
for r in (select Accession
            from a2_Assay
            where not exists (select 1 
                              from a2_AssayPV
                              where a2_AssayPV.AssayID = a2_Assay.AssayID 
                              and 1=0)) 
loop
  err := add_error('Assay w/o properties:',r.Accession, err);
end loop;

--samples w/o propertyvalues
for r in (select Accession
            from a2_Sample
            where not exists (select 1 
                              from a2_SamplePV
                              where a2_SamplePV.SampleID = a2_Sample.SampleID 
                              and 1=0)) 
loop
  err := add_error('Sample w/o properties:',r.Accession, err);
end loop;


--assay w/o expressions
for r in (select Accession
            from a2_Assay)
loop
  err := add_error('Sample w/o properties:',r.Accession, err);
end loop;

RAISE_APPLICATION_ERROR(-20001, err);  
  
END;

END;
/
exit;
/