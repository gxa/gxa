package uk.ac.ebi.gxa.loader;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 07/03/2011
 * Time: 12:36
 * To change this template use File | Settings | File Templates.
 */
public class MappingWriter {
    public static void main(String[] args) {
        dirlist("/Users/nsklyar/Data/annotations/Ens61/ensemblAnnotation/mappings", "");
    }

    private static void dirlist(String fname, String organism) {

        File dir = new File(fname);
        String[] chld = dir.list();
        if (dir.isFile()) {
            System.out.println(organism +  "\t" + dir.getName());
            return;

        } else if (dir.isDirectory()) {
            organism = dir.getName();
            for (int i = 0; i < chld.length; i++) {
                dirlist(fname + "/" + chld[i], organism);
            }
        }
    }

}
