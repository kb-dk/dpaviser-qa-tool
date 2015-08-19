package dk.statsbiblioteket.dpaviser.qatool;

import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.Item;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.medieplatform.autonomous.RunnableComponent;

public class LogNowComponent implements RunnableComponent<Batch> {
    private String message;
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

    public LogNowComponent(String message) {
        this.message = message;
    }

    @Override
    public String getComponentName() {
        return getClass().getName();
    }

    @Override
    public String getComponentVersion() {
        return "?";
    }

    @Override
    public String getEventID() {
        return "?";
    }

    @Override
    public void doWorkOnItem(Batch item, ResultCollector resultCollector) throws Exception {
        log.info(message + " - " + new java.util.Date());
    }
}
