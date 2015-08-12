package dk.statsbiblioteket.dpaviser.qatool;

import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.util.xml.XSLT;

import javax.xml.transform.TransformerException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NinestarsUtils {
    /**
     * Converts the result from the resultCollector to the ninestars qa format
     *
     * @param result the result to convert
     *
     * @param batchFullID The batch ID to put in the report
     * @return the ninestars xml
     */
    protected static String convertResult(ResultCollector result, String batchFullID) {
        Map<String, String> params = new HashMap<>();
        params.put("batchID",batchFullID);
        try {
            return XSLT.transform(Thread.currentThread().getContextClassLoader().getResource("converter.xslt"),
                                  result.toReport(),params);
        } catch (TransformerException e) {
            throw new RuntimeException("Failed to transform");
        }
    }

    /**
     * Merge the list of resultcollecots into one resultcollector
     *
     * @param resultCollectors the result collectors
     *
     * @return a single merged resultcollector
     */
    protected static ResultCollector mergeResults(List<ResultCollector> resultCollectors) {
        ResultCollector finalresult = new ResultCollector("batch", getVersion());
        for (ResultCollector resultCollector : resultCollectors) {
            finalresult = resultCollector.mergeInto(finalresult);
        }
        return finalresult;
    }

    /**
     * Get the version of this Suite
     *
     * @return the version
     */
    public static String getVersion() {
        return NinestarsUtils.class.getPackage().getImplementationVersion();
    }
}
