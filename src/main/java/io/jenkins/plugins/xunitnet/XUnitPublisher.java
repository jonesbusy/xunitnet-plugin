package io.jenkins.plugins.xunitnet;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultProjectAction;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.security.MasterToSlaveCallable;
import jenkins.tasks.SimpleBuildStep;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Class that records XUnit test reports into Jenkins.
 *
 */
public class XUnitPublisher extends Recorder implements Serializable, SimpleBuildStep {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(XUnitPublisher.class.getName());

    private static final transient String PLUGIN_XUNIT = "/plugin/xunitnet/";

    private String testResultsPattern;

    /**
     * <p>Flag that when set, <strong>marks the build as failed if there are
     * no test results</strong>.</p>
     *
     * <p>Defaults to <code>true</code>.</p>
     */
    private boolean failIfNoResults;

    /**
     * <p>Flag that when set, <strong>marks the build as failed if any tests
     * fail</strong>.</p>
     *
     * <p>Defaults to <code>false</code>.</p>
     */
    private boolean failedTestsFailBuild;

    @DataBoundConstructor
    public XUnitPublisher(String testResultsPattern) {
        this.testResultsPattern = testResultsPattern;
        this.failIfNoResults = true;
    }

    public String getTestResultsPattern() {
        return testResultsPattern;
    }

    @DataBoundSetter
    public void setTestResultsPattern(String testResultsPattern) {
        this.testResultsPattern = testResultsPattern;
    }

    public boolean getFailIfNoResults() {
        return failIfNoResults;
    }

    @DataBoundSetter
    public void setFailIfNoResults(boolean failIfNoResults) {
        this.failIfNoResults = failIfNoResults;
    }

    public boolean getFailedTestsFailBuild() {
        return failedTestsFailBuild;
    }

    @DataBoundSetter
    public void setFailedTestsFailBuild(boolean failedTestsFailBuild) {
        this.failedTestsFailBuild = failedTestsFailBuild;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Action getProjectAction(AbstractProject<?, ?> project) {
        TestResultProjectAction action = project.getAction(TestResultProjectAction.class);
        if (action == null) {
            return new TestResultProjectAction((Job) project);
        } else {
            return action;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Record the test results into the current build.
     * @param junitFilePattern JUnit file pattern
     * @param build The current build
     * @param listener Task listner
     * @return True or false
     * @throws InterruptedException InterruptedException
     * @throws IOException IOException
     */
    private boolean recordTestResult(String junitFilePattern, Run<?, ?> build, TaskListener listener, FilePath filePath)
            throws InterruptedException, IOException {
        synchronized (build) {
            TestResultAction existingAction = build.getAction(TestResultAction.class);
            TestResultAction action;

            final long buildTime = build.getTimestamp().getTimeInMillis();

            TestResult existingTestResults = null;
            if (existingAction != null) {
                existingTestResults = existingAction.getResult();
            }
            TestResult result = getTestResult(junitFilePattern, build, existingTestResults, buildTime, filePath);

            if (existingAction == null) {
                action = new TestResultAction(build, result, listener);
            } else {
                action = existingAction;
                action.setResult(result, listener);
            }

            if (this.failIfNoResults
                    && result.getPassCount() == 0
                    && result.getFailCount() == 0
                    && result.getSkipCount() == 0) {
                listener.getLogger().println("None of the test reports contained any result");
                build.setResult(Result.FAILURE);
                return true;
            }

            if (existingAction == null) {
                build.addAction(action);
            }

            if (action.getResult().getFailCount() > 0) {
                if (failedTestsFailBuild) {
                    build.setResult(Result.FAILURE);
                } else {
                    build.setResult(Result.UNSTABLE);
                }
            }

            return true;
        }
    }

    /**
     * Collect the test results from the files
     * @param junitFilePattern JUnit file pattern
     * @param build The current build
     * @param existingTestResults existing test results to add results to
     * @param buildTime
     * @return a test result
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */
    private TestResult getTestResult(
            final String junitFilePattern,
            Run<?, ?> build,
            final TestResult existingTestResults,
            final long buildTime,
            final FilePath filePath)
            throws IOException, InterruptedException {
        TestResult result = filePath.act(new MasterToSlaveCallable<TestResult, IOException>() {
            private static final long serialVersionUID = -8917897415838795523L;

            public TestResult call() throws IOException {
                FileSet fs = Util.createFileSet(new File(filePath.getRemote()), junitFilePattern);
                DirectoryScanner ds = fs.getDirectoryScanner();

                String[] files = ds.getIncludedFiles();
                if (files.length == 0) {
                    if (failIfNoResults) {
                        // no test result. Most likely a configuration error or fatal problem
                        throw new AbortException(
                                "No test report files were found or the XUnit input XML file contained no tests.");
                    } else {
                        return new TestResult();
                    }
                }
                if (existingTestResults == null) {
                    return new TestResult(buildTime, ds, true, false, null, false);
                } else {
                    existingTestResults.parse(buildTime, ds, null);
                    return existingTestResults;
                }
            }
        });
        return result;
    }

    @Override
    public void perform(
            @Nonnull Run<?, ?> run, @Nonnull FilePath ws, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        boolean result;
        try {
            EnvVars env = run.getEnvironment(listener);
            String resolvedTestResultsPattern = env.expand(testResultsPattern);

            listener.getLogger().println("Recording XUnit tests results");
            String junitTempReportsDirectoryName =
                    "tempJunitReports" + UUID.randomUUID().toString();
            XUnitArchiver transformer = new XUnitArchiver(
                    ws.getRemote(),
                    junitTempReportsDirectoryName,
                    listener,
                    resolvedTestResultsPattern,
                    new XUnitReportTransformer(),
                    failIfNoResults);
            result = ws.act(transformer);

            if (result) {
                recordTestResult(junitTempReportsDirectoryName + "/TEST-*.xml", run, listener, ws);
                ws.child(junitTempReportsDirectoryName).deleteRecursive();
            } else {
                if (this.getFailIfNoResults()) {
                    // this should only happen if failIfNoResults is true and there are no result files, see
                    // XUnitArchiver.
                    run.setResult(Result.FAILURE);
                }
            }
        } catch (AbortException e) {
            // this is used internally to signal issues, so we just rethrow instead of letting the IOException
            // catch it below.
            throw e;
        } catch (IOException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Could not read the XSL XML file.", e);
            listener.getLogger().println("Could not read the XSL XML file." + e.getMessage());
            throw new AbortException("Could not read the XSL XML file.");
        }
    }

    @Extension
    @Symbol("xunitnet")
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(XUnitPublisher.class);
        }

        @Override
        public String getDisplayName() {
            return "Publish XUnit test result report";
        }

        @Override
        public String getHelpFile() {
            return PLUGIN_XUNIT + "help.html";
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
