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
 * http://gxa.github.com/gxa
 */

package ae3.service;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Jul 15, 2009
 * Time: 2:05:16 PM
 * To change this template use File | Settings | File Templates.
 * just for test
 */
public class XML4dbDumps  {
    public static class Document{

        public static class Entry  implements Serializable{
            public static class Reference{

                private String dbName = null;
                public void setDbName(String Value){
                    this.dbName = Value;
                }
                public String getDbName(){
                    return this.dbName;
                }

                private String dbKey;
                public void setDbKey(String Value){
                    this.dbKey = Value;
                }

                public String getDbKey(){
                    return this.dbKey;
                }
            }
            public static class AdditionalField{

                private String name;
                public String getName(){
                    return this.name;
                }
                public void setName(String Value){
                    this.name = Value;
                }

                private String value;
                public String getValue(){
                    return this.value;
                }
                public void setValue(String Value){
                   this.value = Value;
                }
            }

            private String id;
            public String getId(){
                return this.id;
            }
            public void setId(String Value){
                this.id = Value;
            }

            private String accessionNumber = null;
            public String getAccessionNumber(){
                return this.accessionNumber;
            }
            public void setAccessionNumber(String Value){
                this.accessionNumber = Value;
            }

            private List<Reference> references = null;

            public List<Reference> getReferences(){
                    if(null == this.references)
                               this.references = new ArrayList<Reference>();

                    return references;
            }


            List<AdditionalField> additionalFields = null;

            public List<AdditionalField> getAdditionalFields(){
                    if(null == this.additionalFields)
                               this.additionalFields = new ArrayList<AdditionalField>();

                    return additionalFields;
            }

            private String name = null;
            public String getName(){
               return this.name;
            }
            public void setName(String Value){
                this.name = Value;
            }

            private String description = null;
            public String getDescription(){
                return this.description;
            }
            public void setDescription(String Value){
                this.description = Value;
            }

            private String authors;
            public String getAuthors(){
                return this.authors;
            }
            public void setAuthors(String Value){
                this.authors = Value;
            }

            private String keywords = null;
            public String getKeywords(){
                return this.keywords;
            }
            public void setKeywords(String Value){
                this.keywords = Value;    
            }
            

            private String dateCreated = null;
            public String getDateCreated(){
                return this.dateCreated;
            }
            public void setDateCreated(String Value){
                this.dateCreated = Value;
            }

            private String dateModified = null;
            public String getDateModified(){
                return this.dateModified;
            }
            public void setDateModified(String Value){
                this.dateModified = Value;  
            }

        }


        private String name = null;
        public String getName(){
            return this.name;
        }
        public void setName(String Value){
            this.name = Value;
        }

        private String description;
        public String getDescription(){
            return this.description;
        }
        public void setDescription(String Value){
           this.description = Value;
        }

        private String release = null;
        public String getRelease(){
           return this.release;
        }
        public void setRelease(String Value){
           this.release = Value;
        }

        private String releaseDate = null;
        public String getReleaseDate(){
            return this.releaseDate;
        }
        public void setReleaseDate(String Value){
            this.releaseDate = Value;
        }

        List<Entry> entries = null;
        public List<Entry> getEntries(){
            if(null == entries)
                  entries = new ArrayList<Entry>();

            return entries;
        }
    }


    public static String Serialize(Document doc) throws Exception 
    {
        String result = null;

        org.dom4j.Document document = DocumentHelper.createDocument();
        Element root = document.addElement( "database" );

        root.addElement("name").addText(doc.getName());
        root.addElement("description").addText(doc.getDescription());
        root.addElement("release").addText(doc.getRelease());
        root.addElement("release_date").addText(doc.getReleaseDate());
        root.addElement("entry_count").addText(Integer.toString(doc.getEntries().size()));

        Element entries = root.addElement("entries");

        for(Document.Entry e : doc.getEntries())
        {
            Element e1 = entries.addElement("entry")
                    .addAttribute("id",e.getId())
                    .addAttribute("acc",e.getAccessionNumber());

            e1.addElement("name").addText(e.getName());
            e1.addElement("description").addText(e.getDescription());
            e1.addElement("authors").addText(e.getAuthors());
            e1.addElement("keywords").addText(e.getKeywords());

            Element dates = e1.addElement("dates");

            dates.addElement("date")
                    .addAttribute("type","creation")
                    .addAttribute("value",e.getDateCreated());

            dates.addElement("date")
                    .addAttribute("type","last_modification")
                    .addAttribute("value",e.getDateModified());


            if(e.getReferences().size()>0)
            {
                Element crossReferences = e1.addElement("cross_references");

                for(Document.Entry.Reference ref : e.getReferences())
                {
                crossReferences.addElement("ref")
                        .addAttribute("dbname",ref.getDbName())
                        .addAttribute("dbkey",ref.getDbKey());
                }
            }

            if(e.getAdditionalFields().size()>0)
            {
                Element additionalFields = e1.addElement("additional_fields");

                for(Document.Entry.AdditionalField f : e.getAdditionalFields())
                {
                additionalFields.addElement("ref")
                        .addAttribute("name",f.getName())
                        .addText(f.getValue());
                }
            }
        }


        /*
        Element author1 = root.addElement( "author" )
            .addAttribute( "name", "James" )
            .addAttribute( "location", "UK" )
            .addText( "James Strachan" );

        Element author2 = root.addElement( "author" )
            .addAttribute( "name", "Bob" )
            .addAttribute( "location", "US" )
            .addText( "Bob McWhirter" );

        result = document.toString();
        */

        ByteArrayOutputStream i = new ByteArrayOutputStream();

        // Pretty print the document to System.out
        OutputFormat format = OutputFormat.createPrettyPrint(); //createCompactFormat();
        XMLWriter writer = new XMLWriter(i , format );
        writer.write( document );

        result = i.toString();

        return result;
    }
}
