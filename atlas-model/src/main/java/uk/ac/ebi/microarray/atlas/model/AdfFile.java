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
