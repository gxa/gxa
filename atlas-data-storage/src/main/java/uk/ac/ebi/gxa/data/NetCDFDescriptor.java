package uk.ac.ebi.gxa.data;

import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.io.IOException;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * NetCDF File descriptor. Does not possess any resources, though will contain knowledge about repository
 * structure and the details one can extract from the file name.
 */
@Immutable
class NetCDFDescriptor {
    private final ArrayDesign arrayDesign;
    private final File file;

    NetCDFDescriptor(AtlasDataDAO atlasDataDao, Experiment experiment, ArrayDesign arrayDesign) {
        this.arrayDesign = arrayDesign;
        this.file = atlasDataDao.getNetCDFLocation(experiment, arrayDesign);
    }

    NetCDFProxy createProxy() throws AtlasDataException {
        try {
            return new NetCDFProxy(file);
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    String getFileName() {
        return file.getName();
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
