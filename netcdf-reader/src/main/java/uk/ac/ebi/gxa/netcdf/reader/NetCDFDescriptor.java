package uk.ac.ebi.gxa.netcdf.reader;

import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.io.IOException;

/**
 * NetCDF File descriptor. Does not possess any resources, though will contain knowledge about repository
 * structure and the details one can extract from the file name.
 */
@Immutable
public class NetCDFDescriptor {
    private final File file;

    public NetCDFDescriptor(File file) {
        this.file = file;
    }

    public NetCDFProxy createProxy() throws IOException {
        return new NetCDFProxy(file);
    }

    public String getPathForR() {
        return file.getAbsolutePath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetCDFDescriptor that = (NetCDFDescriptor) o;
        return file == null ? that.file == null : file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "NetCDFDescriptor{" +
                "file=" + file +
                '}';
    }
}
