package uk.ac.ebi.gxa.R.compute.rcommand;

import uk.ac.ebi.gxa.R.compute.AtlasComputeService;
import uk.ac.ebi.gxa.R.compute.ComputeException;
import uk.ac.ebi.gxa.R.compute.ComputeTask;
import uk.ac.ebi.gxa.R.compute.RUtil;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RDataFrame;

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
                rs.sourceFromBuffer(RUtil.getRCodeFromResource(rCodeResourcePath));
                return (RDataFrame) rs.getObject(call.toString());
            }
        }));
    }
}
