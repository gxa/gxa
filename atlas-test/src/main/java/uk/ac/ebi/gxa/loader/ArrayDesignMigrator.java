package uk.ac.ebi.gxa.loader;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ArrayDesignMigrator {

    protected JdbcTemplate template;

    public ArrayDesignMigrator() {
        BeanFactory factory =
                new ClassPathXmlApplicationContext("benchmarksContext.xml");

        template = factory.getBean(JdbcTemplate.class);


    }

    public static void main(String[] args) throws Exception {

        if (args.length < 3)
            throw new IllegalArgumentException("Arguments: array_design_accession directory_name BioEntity_Type");

        String accession = args[0];
        File mappingFile = new File(args[1], accession + "_map.txt");
        File annFile = new File(args[1], accession + "_ann.txt");
        final String bioentitytype = args[2];

        BeanFactory factory =
                new ClassPathXmlApplicationContext("benchmarksContext.xml");

        JdbcTemplate template = factory.getBean(JdbcTemplate.class);

        ArrayDesignDAO adDao = new ArrayDesignDAO();
        adDao.setJdbcTemplate(template);


        final BufferedWriter mappingWriter = new BufferedWriter(new FileWriter(mappingFile));
        final BufferedWriter annotationWriter = new BufferedWriter(new FileWriter(annFile));

        final ArrayDesign arrayDesign = adDao.getArrayDesignShallowByAccession(accession);
        mappingWriter.write("Array Design Name" + "\t" + arrayDesign.getName() + "\n");
        mappingWriter.write("Array Design Accession" + "\t" + arrayDesign.getAccession() + "\n");
        mappingWriter.write("Array Design Type" + "\t" + (arrayDesign.getType() != null ? arrayDesign.getType() : " ") + "\n");
        mappingWriter.write("Array Design Provider" + "\t" + (arrayDesign.getProvider() != null ? arrayDesign.getProvider() : " ") + "\n");
        mappingWriter.write("Mapping Software Name" + "\t" + "Atlas" + "\n");
        mappingWriter.write("Mapping Software Version" + "\t" + "Atlas" + "\n");
        mappingWriter.write("Organism" + "\t" + "" + "\n");
        mappingWriter.write("BioEntity Type" + "\t" + bioentitytype + "\n");

        String query = "select  distinct de.accession, g.identifier, g.name, o.name\n" +
                "from a2_designelement de\n" +
                "join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid\n" +
                "join a2_gene g on g.geneid = de.geneid\n" +
                "join a2_organism o on o.organismid = g.organismid\n" +
                "where ad.accession = ?";


        template.query(query,
                new Object[]{accession},
                new RowMapper<Object>() {
                    boolean isAnnotationHeaderWritten = false;

                    public Object mapRow(ResultSet rs, int i) throws SQLException {

                        try {
                            mappingWriter.write(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(4) + "\n");

                            if (!isAnnotationHeaderWritten) {
                                writeAnnotationHeader(annotationWriter, rs.getString(4));
                                isAnnotationHeaderWritten = true;
                            }

                            annotationWriter.write(rs.getString(2) + "\t" + rs.getString(3) + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        return 1;
                    }

                    private void writeAnnotationHeader(BufferedWriter annotationFile, String organism) throws IOException {
                        annotationFile.write("organism" + "\t" + organism + "\n");
                        annotationFile.write("source" + "\t" + "Atlas" + "\n");
                        annotationFile.write("version" + "\t" + "Atlas" + "\n");
                        annotationFile.write("bioentity" + "\t" + bioentitytype + "\n");
                        annotationFile.write("gene" + "\t" + "" + "\n");
                        annotationFile.write("bioentitytype" + "\t" + "name" + "\n");
                    }
                }
        );

        System.out.println("arrayDesign = " + arrayDesign);

        mappingWriter.close();
        annotationWriter.close();
    }

}
