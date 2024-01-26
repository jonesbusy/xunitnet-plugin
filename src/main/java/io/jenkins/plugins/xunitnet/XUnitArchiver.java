package io.jenkins.plugins.xunitnet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.TaskListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import jenkins.security.MasterToSlaveCallable;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.xml.sax.SAXException;

/**
 * Class responsible for transforming XUnit to JUnit files and then run them all through the JUnit result archiver.
 *
 */
public class XUnitArchiver extends MasterToSlaveCallable<Boolean, IOException> {

    private static final long serialVersionUID = 1L;

    private final String root;
    private final String junitDirectoryName;
    private final TaskListener listener;
    private final String testResultsPattern;
    private final TestReportTransformer unitReportTransformer;
    private final boolean failIfNoResults;

    private int fileCount;

    public XUnitArchiver(
            String root,
            String junitDirectoryName,
            TaskListener listener,
            String testResultsPattern,
            TestReportTransformer unitReportTransformer,
            boolean failIfNoResults) {
        this.root = root;
        this.junitDirectoryName = junitDirectoryName;
        this.listener = listener;
        this.testResultsPattern = testResultsPattern;
        this.unitReportTransformer = unitReportTransformer;
        this.failIfNoResults = failIfNoResults;
    }

    /** {@inheritDoc} */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public Boolean call() throws IOException {
        boolean retValue = true;
        String[] xunitFiles = findXUnitReports(new File(root));
        if (xunitFiles.length > 0) {
            File junitOutputPath = new File(root, junitDirectoryName);
            junitOutputPath.mkdirs();

            for (String xunitFileName : xunitFiles) {
                try (FileInputStream fileStream = new FileInputStream(new File(root, xunitFileName))) {
                    unitReportTransformer.transform(fileStream, junitOutputPath);
                    fileCount++;
                } catch (TransformerException te) {
                    throw new IOException(
                            "Could not transform the XUnit report. Please report this issue to the plugin author", te);
                } catch (SAXException se) {
                    throw new IOException(
                            "Could not transform the XUnit report. Please report this issue to the plugin author", se);
                } catch (ParserConfigurationException pce) {
                    throw new IOException(
                            "Could not initialize the XML parser. Please report this issue to the plugin author", pce);
                }
            }
        } else {
            retValue = false;
        }

        return retValue;
    }

    int getFileCount() {
        return fileCount;
    }

    /**
     * Return all XUnit report files
     *
     * @param parentPath parent
     * @return an array of strings
     */
    private String[] findXUnitReports(File parentPath) {
        FileSet fs = Util.createFileSet(parentPath, testResultsPattern);
        DirectoryScanner ds = fs.getDirectoryScanner();

        String[] xunitFiles = ds.getIncludedFiles();
        if (xunitFiles.length == 0) {
            if (this.failIfNoResults) {
                // no test result. Most likely a configuration error or fatal problem
                listener.fatalError("No XUnit test report files were found. Configuration error?");
            } else {
                listener.getLogger().println("No XUnit test report files were found.");
            }
        }
        return xunitFiles;
    }
}
