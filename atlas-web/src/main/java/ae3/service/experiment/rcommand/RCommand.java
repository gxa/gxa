package ae3.service.experiment.rcommand;

import com.google.common.io.Resources;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RDataFrame;

import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;

/**
 * @author Olga Melnichuk
 *         Date: 25/03/2011
 */
public class RCommand {

    private final AtlasComputeService computeService;
    private final String rCodeResourcePath;

    public RCommand(AtlasComputeService computeService, String rCodeResourcePath) {
        this.computeService = computeService;
        this.rCodeResourcePath = rCodeResourcePath;
    }

    public RCommandResult execute(final RCommandStatement call) throws ComputeException {
        return new RCommandResult(computeService.computeTask(new ComputeTask<RDataFrame>() {
            public RDataFrame compute(RServices rs) throws RemoteException {
                rs.sourceFromBuffer(
                        getRCodeFromResource(rCodeResourcePath)
                );
                return (RDataFrame) rs.getObject(call.toString());
            }
        }));
    }

    private String getRCodeFromResource(String resourcePath) throws ComputeException {
        try {
            return Resources.toString(getClass().getClassLoader().getResource(resourcePath), Charset.defaultCharset());
        } catch (IOException e) {
            throw new ComputeException("Error while reading in R code from " + resourcePath, e);
        }
    }
}
