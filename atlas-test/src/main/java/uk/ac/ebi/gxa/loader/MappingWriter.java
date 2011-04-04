package uk.ac.ebi.gxa.loader;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 07/03/2011
 * Time: 12:36
 * To change this template use File | Settings | File Templates.
 */
public class MappingWriter {

    private static String mappingHeaderTemplate =
            "Array Design Name\t\n" +
                    "Array Design Accession\t$acc\n" +
                    "Array Design Type\t\n" +
                    "Array Design Provider\t\n" +
                    "Mapping Software Name\t$swname\n" +
                    "Mapping Software Version\t$swversion\n" +
                    "Organism\t$organism\n" +
                    "BioEntity Type\tenstranscript\n";

    private String swname = "Ensembl";
    private String swversion = "61";

    private static String accMappingFile = "/Users/nsklyar/Data/annotations/Ens61/fn_accession.txt";
    private static String ensMappingDir = "/Users/nsklyar/Data/annotations/Ens61/ensemblAnnotation/mappings";

    private Map<String, FileNameAccMapping> fileNameToAccMapping;

    private File outDir;

    public static void main(String[] args) throws Exception {


        MappingWriter mappingWriter = new MappingWriter();
        mappingWriter.generateMappingFiles(accMappingFile, ensMappingDir);
    }

    private void generateMappingFiles (String confFileName, String mappingsDirectory) throws IOException {

        readFileNameAccMapping(confFileName);

        outDir = new File(mappingsDirectory, "result");
        outDir.mkdir();

        traverseDirectory(mappingsDirectory, "");
    }

    private void traverseDirectory(String fname, String organism) throws IOException {

        File dir = new File(fname);
        String[] chld = dir.list();
        if (dir.isFile()) {
            System.out.println(organism + "\t" + dir.getName());

            addHeaderToFile(dir);
            return;

        } else if (dir.isDirectory()) {
            organism = dir.getName();
            for (int i = 0; i < chld.length; i++) {
                traverseDirectory(fname + "/" + chld[i], organism);
            }
        }
    }

    private void readFileNameAccMapping(String confFileName) throws IOException {
        fileNameToAccMapping = new HashMap<String, FileNameAccMapping>();

            File file = new File(confFileName);
            CSVReader csvReader = new CSVReader(new FileReader(file), '\t', '"');
            String[] line;

            while ((line = csvReader.readNext()) != null) {
                if (line.length > 3) {

                    FileNameAccMapping mapping = new FileNameAccMapping(line[0], line[1], line[2], line[3]);
                    if (StringUtils.isNotEmpty(mapping.getAccession())) {
                        fileNameToAccMapping.put(mapping.getFileName(), mapping);
                    }
                }
            }


    }

    private void addHeaderToFile(File inputFile) throws IOException {
        FileNameAccMapping accMapping = fileNameToAccMapping.get(inputFile.getName());

        if (accMapping == null) return;

        File outFile = new File(outDir, accMapping.getOrganism() + "_" + inputFile.getName() + ".txt");

        FileWriter writer = new FileWriter(outFile);

        String header = mappingHeaderTemplate.replace("$acc", accMapping.getAccession())
                .replace("$organism", accMapping.getOrganismFull())
                .replace("$swname", swname)
                .replace("$swversion", swversion);

        writer.write(header, 0, header.length());


        List list = FileUtils.readLines(inputFile);
        for (Object s : list) {
            writer.write(s.toString() + "\n");
        }

        writer.flush();
        writer.close();

    }

    static class FileNameAccMapping {
        private String organism;
        private String organismFull;
        private String fileName;
        private String accession;

        FileNameAccMapping(String organism, String fileName, String accession, String organismFull) {
            this.organism = organism;
            this.fileName = fileName;
            this.accession = accession;
            this.organismFull = organismFull;
        }

        public String getOrganism() {
            return organism;
        }

        public String getFileName() {
            return fileName;
        }

        public String getAccession() {
            return accession;
        }

        public String getOrganismFull() {
            return organismFull;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FileNameAccMapping mapping = (FileNameAccMapping) o;

            if (accession != null ? !accession.equals(mapping.accession) : mapping.accession != null) return false;
            if (!fileName.equals(mapping.fileName)) return false;
            if (!organism.equals(mapping.organism)) return false;
            if (!organismFull.equals(mapping.organismFull)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = organism.hashCode();
            result = 31 * result + organismFull.hashCode();
            result = 31 * result + fileName.hashCode();
            result = 31 * result + (accession != null ? accession.hashCode() : 0);
            return result;
        }
    }
}
