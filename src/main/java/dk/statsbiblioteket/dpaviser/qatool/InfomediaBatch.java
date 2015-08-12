package dk.statsbiblioteket.dpaviser.qatool;

import dk.statsbiblioteket.medieplatform.autonomous.Batch;

/** An infomedia batch is not quite the same as a Ninestars batch */

public class InfomediaBatch extends Batch {

    public InfomediaBatch(String batchID) {
        super(batchID);
    }

    @Override
    public String getFullID() {
        return super.getBatchID();
    }
}
