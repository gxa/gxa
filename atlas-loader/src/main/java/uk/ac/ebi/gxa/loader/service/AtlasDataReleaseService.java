package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.loader.DataReleaseCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.Date;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class AtlasDataReleaseService {
    private final ExperimentDAO experimentDAO;
    private final AtlasNetCDFDAO atlasNetCDFDAO;

    public AtlasDataReleaseService(ExperimentDAO experimentDAO, AtlasNetCDFDAO atlasNetCDFDAO) {
        this.experimentDAO = experimentDAO;
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void process(DataReleaseCommand command) {
        final String accession = command.getAccession();
        try {
            final Experiment experiment = experimentDAO.getExperimentByAccession(accession);
            atlasNetCDFDAO.releaseExperiment(experiment);
            experiment.setReleaseDate(new Date());
            experimentDAO.save(experiment);
        } catch (Exception ex) {
            throw createUnexpected("Can not release data for experiment " + accession, ex);
        }
    }
}
