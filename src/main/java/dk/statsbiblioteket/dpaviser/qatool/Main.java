package dk.statsbiblioteket.dpaviser.qatool;

import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.medieplatform.autonomous.RunnableComponent;
import dk.statsbiblioteket.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

public class Main {
    private Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        try {
            System.exit(new Main().doMain(args));
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    protected  int doMain(String... args) {
        if (args.length < 2) {
            System.err.println("Too few parameters");
            usage();
            return 2;
        }
        log.info("Entered " + getClass());
        Properties properties;
        Batch batch;
        try {
            //Get the batch (id) from the command line
            batch = getBatch(args[0]);
            //Create the properties that need to be passed into the components
            properties = createProperties(args[0], args[1]);
        } catch (Exception e) {
            usage();
            System.err.println(e.getMessage());
            return 2;
        }

        //This is the list of results so far
        ArrayList<ResultCollector> resultList = new ArrayList<>();
        try {
            //Make the components
            RunnableComponent batchStructureCheckerComponent = new dk.statsbiblioteket.dpaviser.BatchStructureCheckerComponent(properties);
            //Run the batch structure checker component, where the result is added to the resultlist
            runComponent(batch, resultList, batchStructureCheckerComponent);
            //Add more components as needed
        } catch (WorkException e) {
            // Ignore, already handled
        }
        ResultCollector mergedResult = NinestarsUtils.mergeResults(resultList);
        String result = NinestarsUtils.convertResult(mergedResult,batch.getFullID());
        System.out.println(result);
        if (!mergedResult.isSuccess()) {
            return 1;
        } else {
            return 0;
        }
    }


    /**
     * Create a properties construct with just one property, "scratch". Scratch denotes the folder where the batches
     * reside. It is takes as the parent of the first argument, which should be the path to the batch
     *
     * @param batchPath the path to the batch
     * @param sqlString the Sql connect string
     *
     * @return a properties construct
     * @throws RuntimeException on trouble parsing arguments.
     */
    private static Properties createProperties(String batchPath,String sqlString) throws IOException {
        Properties properties = new Properties(System.getProperties());
        File batchFile = new File(batchPath);
        setIfNotSet(properties, ConfigConstants.ITERATOR_FILESYSTEM_BATCHES_FOLDER, batchFile.getParent());
        setIfNotSet(properties, ConfigConstants.AT_NINESTARS, Boolean.TRUE.toString());
        setIfNotSet(properties, ConfigConstants.MFPAK_URL, sqlString);
        setIfNotSet(properties,
                ConfigConstants.AUTONOMOUS_BATCH_STRUCTURE_STORAGE_DIR,
                createTempDir().getAbsolutePath());
        setIfNotSet(properties, ConfigConstants.THREADS_PER_BATCH, Runtime.getRuntime().availableProcessors() + "");

        return properties;
    }

    private static File createTempDir() throws IOException {
        File temp = File.createTempFile("ninestarsQA", "");
        temp.delete();
        temp.mkdir();
        temp.deleteOnExit();
        return temp;
    }

    private static void setIfNotSet(Properties properties, String key, String value) {
        if (properties.getProperty(key) == null) {
            properties.setProperty(key, value);
        } else {
            System.out.println(properties.getProperty(key));
        }
    }

    private static void runComponent(Batch batch, ArrayList<ResultCollector> resultList,
                                     RunnableComponent component1) throws WorkException {
        //log.info("Preparing to run component {}", component1.getComponentName());
        ResultCollector result1 = new ResultCollector(component1.getComponentName(), component1.getComponentVersion());
        resultList.add(result1);
        doWork(batch, component1, result1);
        //log.info("Completed run of component {}", component1.getComponentName());
    }

    /**
     * Parse the batch and round trip id from the first argument to the script
     *
     * @param arg0 the first command line argument
     *
     * @return the batch id as a batch with no events
     */
    protected static Batch getBatch(String arg0) {
        File batchPath = new File(arg0);
        if (!batchPath.isDirectory()) {
            throw new RuntimeException("Must have first argument as existing directory");
        }
        String batchFullId = batchPath.getName();
        String[] splits = batchFullId.split(Pattern.quote("-RT"));
        Batch batch = new Batch(splits[0].replaceAll("[^0-9]", "").trim());
        batch.setRoundTripNumber(Integer.parseInt(splits[1].trim()));
        return batch;
    }

    /**
     * Call the doWork method on the runnable component, and add a failure to the result collector is the
     * method throws
     *
     * @param batch           the batch to work on
     * @param component       the component doing the work
     * @param resultCollector the result collector
     *
     * @return the resultcollector
     * @throws WorkException if the component threw an exception
     */
    protected static ResultCollector doWork(Batch batch, RunnableComponent component,
                                            ResultCollector resultCollector) throws WorkException {
        try {
            component.doWorkOnItem(batch, resultCollector);
        } catch (Exception e) {
            //log.error("Failed to do work on component {}", component.getComponentName(), e);
            resultCollector.addFailure(batch.getFullID(),
                    "exception",
                    component.getClass().getSimpleName(),
                    "Unexpected error in component: " + e.toString(),
                    Strings.getStackTrace(e));
            throw new WorkException(e);
        }
        return resultCollector;
    }

    /**
     * Print usage.
     */
    private static void usage() {
        System.err.print(
                "Usage: \n" + "java " + Main.class.getName() + " <batchdirectory>");
        System.err.println();
    }
}

