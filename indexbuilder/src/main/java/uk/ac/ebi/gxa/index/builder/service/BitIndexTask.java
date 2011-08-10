package uk.ac.ebi.gxa.index.builder.service;

import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.List;

class BitIndexTask {
    private final List<Experiment> experiments;
    private final long totalSize;

    private int totalStatCount = 0;
    private int processedCount = 0;
    private long processedSize = 0;
    private final long start = System.currentTimeMillis();

    public BitIndexTask(List<Experiment> experiments) {
        this.experiments = experiments;
        totalSize = size(experiments);
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    private long size(List<Experiment> experiments) {
        long result = 0;
        for (Experiment e : experiments) {
            result += e.getAssays().size();
        }
        return result;
    }

    public void processedStats(int car) {
        totalStatCount += car;
    }

    public String progress() {
        return String.format("%d/%d (%d/%d assays)",
                processedCount, experiments.size(),
                processedSize, totalSize);
    }

    public int getTotalExperiments() {
        return experiments.size();
    }

    public void done(Experiment e) {
        processedCount++;
        processedSize += e.getAssays().size();
    }

    public int getTotalStatCount() {
        return totalStatCount;
    }
}
