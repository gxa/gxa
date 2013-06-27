package uk.ac.ebi.gxa.index.builder.service.mirbase;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MirbaseFastaParser {

    private String miRnaFastaLocation;

    public static final String NEW_ENTRY_SYMBOL = ">";

    public void setMiRnaFastaLocation(String miRnaFastaLocation) {
        this.miRnaFastaLocation = miRnaFastaLocation;
    }


    public List<MiRNAEntity> parse() {
        File file = new File(miRnaFastaLocation);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            return readData(br);

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Cannot find miRBase fasta file in " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find miRBase fasta file in " + file.getAbsolutePath(), e);
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e) {
                throw new IllegalStateException("Cannot close miRBase fasta file in " + file.getAbsolutePath(), e);
            }
        }
    }


    protected List<MiRNAEntity> readData(BufferedReader br) throws IOException {
        List<MiRNAEntity> entries = Lists.newArrayList();
        String line;

        MiRNAEntity entity = null;

        while ((line = br.readLine()) != null) {
            if (line.startsWith(NEW_ENTRY_SYMBOL)) {

                ArrayList<String> words = splitLine(line);

                entity = new MiRNAEntity(parseIdentifier(words), parseAccession(words), parseOrganism(words), parseName(words));
            } else {
                entity.setSequence(line);
                entries.add(entity);
            }

        }
        return entries;
    }

    protected ArrayList<String> splitLine(String line) {
        return Lists.newArrayList(Splitter.on(" ").omitEmptyStrings().trimResults().split(line));
    }

    protected String parseName(ArrayList<String> words) {
        return words.get(words.size() - 1);
    }

    protected String parseIdentifier(ArrayList<String> words) {
        return words.get(0).replaceFirst(NEW_ENTRY_SYMBOL, "");
    }

    protected String parseAccession(ArrayList<String> words) {
        return words.get(1);
    }

    protected String parseOrganism(ArrayList<String> words) {
        StringBuffer stringBuffer = new StringBuffer();
        for (String s : words.subList(2, words.size() - 1)) {
            stringBuffer.append(s).append(" ");
        }
        return stringBuffer.toString().trim();
    }


}
