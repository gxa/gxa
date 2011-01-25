package uk.ac.ebi.gxa.netcdf.reader;

import java.io.File;
import java.io.IOException;

/**
 * NetCDF File descriptor. Does not possess any resources, though will contain knowledge about repository
 * structure and the details one can extract from the file name.
 */
public class NetCDFDescriptor {
    private File file;

    public NetCDFDescriptor(File file) {
        this.file = file;
    }

    public NetCDFProxy createProxy() throws IOException {
        return new NetCDFProxy(file);
    }

    public String getPathForR() {
        return file.getAbsolutePath();
    }

    public String getProxyId() {
        return file.getName();
    }
}
