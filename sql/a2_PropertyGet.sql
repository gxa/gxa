/*
*/
create or replace
procedure a2_propertyGet(
   propertyid_ int
  ,keyword varchar
  ,properties IN OUT sys_refcursor
)
as
begin

    open properties for 
        select pv.PropertyValueID
        , p.PropertyID
    from a2_Property p
    join a2_PropertyValue pv on p.PropertyID = pv.PropertyID
    where (propertyid_ is null) or (p.propertyID = propertyid_)
    and (p.Name = keyword);
  
end;
