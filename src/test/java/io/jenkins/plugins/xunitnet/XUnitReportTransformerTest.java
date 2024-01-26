package io.jenkins.plugins.xunitnet;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class XUnitReportTransformerTest extends AbstractWorkspaceTest implements FilenameFilter {

    private XUnitReportTransformer transformer;
    private File tempFilePath;

    @Before
    public void setup() throws Exception {
        super.createWorkspace();
        transformer = new XUnitReportTransformer();
        tempFilePath = parentFile;
    }

    @After
    public void teardown() throws Exception {
        super.deleteWorkspace();
    }

    @Test
    public void testDeleteOutputFiles() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("xunit-simple.xml"), tempFilePath);
        File[] listFiles = tempFilePath.listFiles(this);
        for (File file : listFiles) {
            Assert.assertTrue("Could not delete the transformed files", file.delete());
        }
    }

    @Test
    public void testTransform() throws Exception {
        transformer.transform(this.getClass().getResourceAsStream("xunit-simple.xml"), tempFilePath);
        assertJunitFiles(1);
    }

    private void assertJunitFiles(int expectedJunitFilesCount) throws DocumentException {
        File[] listFiles = tempFilePath.listFiles(this);
        Assert.assertEquals("The number of junit files are incorrect.", expectedJunitFilesCount, listFiles.length);
        for (File file : listFiles) {
            Document result = new SAXReader().read(file);
            Assert.assertNotNull("The XML wasn't parsed", result);
            org.dom4j.Element root = result.getRootElement();
            Assert.assertNotNull("There is no root in the XML", root);
            Assert.assertEquals("The name is not correct", "testsuite", root.getName());
        }
    }

    @Test(expected = TransformerException.class)
    public void testPreventXXEWithFile() throws Exception {
        File tempFile = new File(tempFilePath, "dummy.txt");

        try (FileWriter output = new FileWriter(tempFile)) {
            output.write("You should never see this");
        }

        InputStream input = getClass().getResourceAsStream("xunit-sec-file.xml");
        String content =
                IOUtils.toString(input, Charset.defaultCharset()).replace("__FILEPATH__", tempFile.getAbsolutePath());

        try (InputStream transformStream = IOUtils.toInputStream(content, StandardCharsets.UTF_8)) {
            transformer.transform(transformStream, tempFilePath);
        }
        assertJunitFiles(0);
    }

    @Test(expected = TransformerException.class)
    public void testPreventXXEWithHttps() throws Exception {
        transformer.transform(getClass().getResourceAsStream("xunit-sec-https.xml"), tempFilePath);
        assertJunitFiles(0);
    }

    public boolean accept(File dir, String name) {
        return name.startsWith(XUnitReportTransformer.JUNIT_FILE_PREFIX);
    }
}
