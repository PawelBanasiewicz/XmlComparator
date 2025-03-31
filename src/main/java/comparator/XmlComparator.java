package comparator;

import comparator.similarity.SimilarityCalculator;
import comparator.similarity.SimilarityResult;
import org.w3c.dom.Document;
import org.xmlunit.diff.Difference;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class XmlComparator {

    public static void main(String[] args) {
        compare("old_format.xml", "new_format.xml", "output.txt");
    }

    public static void compare(final String controlXmlPath, final String testXmlPath, final String outputPath) {
        try {
            final Document controlXmlDocument = XmlUtils.parseXML(new File(controlXmlPath));
            final Document testXmlDocument = XmlUtils.parseXML(new File(testXmlPath));

            final List<Difference> differences = DifferenceFinder.findDifferences(controlXmlDocument, testXmlDocument);
            final DifferenceResult differenceResult = DifferenceAnalyzer.analyzeDifferences(differences);

            final SimilarityResult similarityResult = SimilarityCalculator.calculate(controlXmlDocument, differenceResult.textDifferences().size());
            ReportGenerator.generateReport(differenceResult, similarityResult, outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Something went wrong with reading xml file", e);
        }
    }
}
