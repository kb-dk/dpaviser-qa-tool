package dk.statsbiblioteket.dpaviser.qatool;

import dk.statsbiblioteket.dpaviser.BatchStructureCheckerComponent;
import dk.statsbiblioteket.dpaviser.metadatachecker.BatchMetadataCheckerComponent;
import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.medieplatform.autonomous.RunnableComponent;
import dk.statsbiblioteket.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import static dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants.AT_NINESTARS;
import static dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants.AUTONOMOUS_BATCH_STRUCTURE_STORAGE_DIR;
import static dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants.ITERATOR_FILESYSTEM_BATCHES_FOLDER;
import static dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants.THREADS_PER_BATCH;

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

    /**
     * Create a properties construct with just one property, "scratch". Scratch denotes the folder where the batches
     * reside. It is takes as the parent of the first argument, which should be the path to the batch
     *
     * @param batchPath the path to the batch
     * @return a properties construct
     * @throws RuntimeException on trouble parsing arguments.
     */
    private static Properties createProperties(String batchPath) throws IOException {
        Properties properties = new Properties(System.getProperties());
        File batchFile = new File(batchPath);
        setIfNotSet(properties, ITERATOR_FILESYSTEM_BATCHES_FOLDER, batchFile.getParent());
        setIfNotSet(properties, AT_NINESTARS, Boolean.TRUE.toString());
        setIfNotSet(properties, AUTONOMOUS_BATCH_STRUCTURE_STORAGE_DIR, createTempDir().getAbsolutePath());
        setIfNotSet(properties, THREADS_PER_BATCH, Runtime.getRuntime().availableProcessors() + "");

        setIfNotSet(properties, ConfigConstants.ITERATOR_FILESYSTEM_GROUPINGCHAR, ".");
        setIfNotSet(properties, ConfigConstants.ITERATOR_DATAFILEPATTERN, ".*\\.pdf$");
        setIfNotSet(properties, ConfigConstants.ITERATOR_FILESYSTEM_CHECKSUMPOSTFIX, ".md5");
        setIfNotSet(properties, ConfigConstants.ITERATOR_FILESYSTEM_IGNOREDFILES, "");
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

    private static ResultCollector runComponent(Batch batch,
                                                RunnableComponent component1) {
        //log.info("Preparing to run component {}", component1.getComponentName());
        ResultCollector result1 = new ResultCollector(component1.getComponentName(), component1.getComponentVersion());
        try {
            component1.doWorkOnItem(batch, result1);
        } catch (Exception e) {
            //log.error("Failed to do work on component {}", component.getComponentName(), e);
            result1.addFailure(batch.getFullID(),
                    "exception",
                    component1.getClass().getSimpleName(),
                    "Unexpected error in component: " + e.toString(),
                    Strings.getStackTrace(e));
        }
        return result1;
    }

    /**
     * Parse the batch and round trip id from the first argument to the script
     *
     * @param batchDirPath the first command line argument
     * @return the batch id as a batch with no events
     */
    protected static Batch getBatch(String batchDirPath) {
        File batchDirFile = new File(batchDirPath);
        System.out.println("Looking at: " + batchDirFile.getAbsolutePath());
        if (!batchDirFile.isDirectory()) {
            throw new RuntimeException("Must have first argument as existing directory");
        }
        return new InfomediaBatch(batchDirFile.getName());
    }

    /**
     * Print usage.
     */
    private static void usage() {
        System.err.print(
                "Usage: \n" + "java " + Main.class.getName() + " <batchdirectory>");
        System.err.println();
    }

    protected int doMain(String... args) {
        if (args.length < 1) {
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
            properties = createProperties(args[0]);
        } catch (Exception e) {
            usage();
            e.printStackTrace(System.err);
            return 2;
        }

        ResultCollector finalresult = processBatch(batch, properties);
        System.out.println(finalresult.toReport());

        if (!finalresult.isSuccess()) {
            return 1;
        } else {
            return 0;
        }
    }

    protected ResultCollector processBatch(Batch batch, Properties properties) {
        ResultCollector result = new ResultCollector("batch", getClass().getPackage().getImplementationVersion());

        Arrays.<RunnableComponent<Batch>>asList(
                new LogNowComponent("Start"),
                new BatchStructureCheckerComponent(properties),
                new BatchMetadataCheckerComponent(properties),
                new LogNowComponent("Stop")
        ).stream().map(component -> runComponent(batch, component)).reduce(result, (a, next) -> next.mergeInto(a));

        return result;
    }
}

