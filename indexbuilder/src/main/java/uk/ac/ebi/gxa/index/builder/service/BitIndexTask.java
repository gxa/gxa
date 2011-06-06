package uk.ac.ebi.gxa.index.builder.service;

import java.io.File;
import java.util.List;

class BitIndexTask {
    private static final double GiB = 1024.0 * 1024.0 * 1024.0;

    private final List<File> files;
    private final long totalSize;

    private int totalStatCount = 0;
    private int processedCount = 0;
    private long processedSize = 0;
    // Count of ncdfs in which no efvs were found
    private int emptyCount = 0;
    private final long start = System.currentTimeMillis();

    public BitIndexTask(List<File> files) {
        this.files = files;
        totalSize = size(files);
    }

    public List<File> getFiles() {
        return files;
    }

    private long size(List<File> files) {
        long result = 0;
        for (File f : files) {
            result += f.length();
        }
        return result;
    }

    public void processedStats(int car) {
        totalStatCount += car;
    }

    public String progress() {
        return String.format("%d/%d (%.1f/%.1fG)",
                processedCount, files.size(),
                processedSize / GiB, totalSize / GiB);
    }

    public int getTotalFiles() {
        return files.size();
    }

    public void skipEmpty(File f) {
        emptyCount++;
        done(f);
    }

    public void done(File f) {
        processedCount++;
        processedSize += f.length();
    }

    public int getTotalStatCount() {
        return totalStatCount;
    }
}
