package uk.ac.ebi.gxa.loader;

import com.sun.javaws.exceptions.InvalidArgumentException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 28/02/2011
 * Time: 11:39
 * To change this template use File | Settings | File Templates.
 */
public class ArrayDesignMigrator {

    protected JdbcTemplate template;

    public ArrayDesignMigrator() {
        BeanFactory factory =
                new ClassPathXmlApplicationContext("benchmarksContext.xml");

        template = (JdbcTemplate) factory.getBean("template");


    }

    public static void main(String[] args) throws Exception {

        if (args.length < 3)
            throw new InvalidArgumentException(new String[]{"Arguments: array_design_accession file_name BioEntity_Type"});

        String accession = args[0];
        String fileName = args[1];
        String bioentitytype = args[2];

        BeanFactory factory =
                new ClassPathXmlApplicationContext("benchmarksContext.xml");

        JdbcTemplate template = (JdbcTemplate) factory.getBean("template");

        ArrayDesignDAO adDao = new ArrayDesignDAO();
        adDao.setJdbcTemplate(template);


        final BufferedWriter mappingFile = new BufferedWriter(new FileWriter(fileName));

        ArrayDesign arrayDesign = adDao.getArrayDesignShallowByAccession(accession);
        mappingFile.write("Array Design Name" + "\t" + arrayDesign.getName() + "\n");
        mappingFile.write("Array Design Accession" + "\t" + arrayDesign.getAccession() + "\n");
        mappingFile.write("Array Design Type" + "\t" + (arrayDesign.getType() != null ? arrayDesign.getType() : " ") + "\n");
        mappingFile.write("Array Design Provider" + "\t" + arrayDesign.getProvider() + "\n");
        mappingFile.write("Mapping Software Name" + "\t" + "Atlas" + "\n");
        mappingFile.write("Mapping Software Version" + "\t" + "2" + "\n");
        mappingFile.write("Organism" + "\t" + "" + "\n");
        mappingFile.write("BioEntity Type" + "\t" + bioentitytype + "\n");


        String query = "select  distinct de.accession, g.identifier, g.name, o.name\n" +
                "from a2_designelement de\n" +
                "join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid\n" +
                "join a2_gene g on g.geneid = de.geneid\n" +
                "join a2_organism o on o.organismid = g.organismid\n" +
                "where ad.accession = ?";


        ArrayDesignElementMapper mapper = new ArrayDesignElementMapper(arrayDesign, mappingFile);

        template.query(query,
                new Object[]{accession},
                new RowMapper<Object>() {
                    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
                        try {
                            mappingFile.write(resultSet.getString(1) + "\t" + resultSet.getString(2) + "\t" + resultSet.getString(4) + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        return 1;
                    }
                }
        );

        System.out.println("arrayDesign = " + arrayDesign);

        mappingFile.close();
    }

    private static class ArrayDesignElementMapper implements RowMapper {
        private ArrayDesign arrayDesign;
        private BufferedWriter out;

        public ArrayDesignElementMapper(ArrayDesign arrayDesign, BufferedWriter out) {
            this.arrayDesign = arrayDesign;
            this.out = out;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {

            try {
                out.write(resultSet.getString(1) + "\t" + resultSet.getString(2) + "\t" + resultSet.getString(4));
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


            long deid = resultSet.getLong(1);
            String acc = resultSet.getString(2);
            String name = resultSet.getString(4);
            long geneId = resultSet.getLong(3);

            arrayDesign.addDesignElement(acc, deid);
            arrayDesign.addDesignElement(name, deid);
            arrayDesign.addGene(deid, geneId);

            return resultSet.getString(1);
        }

    }

}
