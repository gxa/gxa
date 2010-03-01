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

package uk.ac.ebi.microarray.atlas.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Feb 18, 2010
 * Time: 4:46:09 PM
 * To change this template use File | Settings | File Templates.
 *
 * Class represents ADF file in form it can be loaded into Atlas database
 *
 */
public class AdfFile {
       public String arrayDesignName;
       public String arrayDesignType;
       public String arrayDesignProvider;
       public String[] entryPriorityList;
       public class CompositeElement{
           public String name;
           public List<DatabaseEntry> entries;
           public DatabaseEntry createDatabaseEntry(String name, String value){
               if(entries==null){
                   entries = new ArrayList<DatabaseEntry>();
               }

               DatabaseEntry entry = new DatabaseEntry();
               entry.name = name;
               entry.value = value;

               return entries.add(entry) ? entry : null;
           }
       }
       public class DatabaseEntry{
           public String name;
           public String value;
       }
      public List<CompositeElement> elements;

      public CompositeElement createCompositeElement(String name){
        if(elements==null){
            elements = new ArrayList<CompositeElement>();
        }

        CompositeElement element = new CompositeElement();
        element.name = name;

        return elements.add(element) ? element : null;
    }
}
