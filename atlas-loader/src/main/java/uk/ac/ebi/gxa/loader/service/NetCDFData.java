package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixStorage;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.ArrayList;
import java.util.List;

public class NetCDFData {
    EfvTree<AtlasNetCDFUpdaterService.CPair<String, String>> matchedEfvs = null;
    List<Assay> assays = new ArrayList<Assay>();
    DataMatrixStorage storage;
    String[] uEFVs;
}
