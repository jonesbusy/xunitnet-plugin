package io.jenkins.plugins.xunitnet;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Transform;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

/**
 * Unit test for the XSL transformation
 *
 */
public class XUnitToJUnitXslTest {
    @Before
    public void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
    }

    @Test
    public void testTransformation() throws Exception {

        Transform myTransform = new Transform(
                new InputSource(this.getClass().getResourceAsStream("xunit-simple.xml")),
                new InputSource(
                        this.getClass().getResourceAsStream(XUnitReportTransformer.XUNIT_TO_JUNIT_XSLFILE_STR)));

        Diff myDiff = new Diff(readXmlAsString("junit-simple.xml"), myTransform);
        assertTrue("XSL transformation did not work" + myDiff, myDiff.similar());
    }

    private String readXmlAsString(String resourceName) throws IOException {
        String xmlString = "";

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(resourceName)));
        String line = reader.readLine();
        while (line != null) {
            xmlString += line + "\n";
            line = reader.readLine();
        }
        reader.close();

        return xmlString;
    }
}
