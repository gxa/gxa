/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://ostolop.github.com/gxa/
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
