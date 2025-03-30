package comparator;

import comparator.similarity.SimilarityCalculator;
import comparator.similarity.SimilarityResult;
import org.xmlunit.diff.Difference;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class XmlComparator {

    public static void main(String[] args) {
        compare("old_format.xml", "new_format.xml", "output.txt");
    }

    public static void compare(final String controlXmlPath, final String testXmlPath, final String outputPath) {
        final File controlXmlFile = new File(controlXmlPath);
        final File testXmlFile = new File(testXmlPath);

        try {
            final String controlXmlContent = new String(Files.readAllBytes(controlXmlFile.toPath()));
            final String testXmlContent = new String(Files.readAllBytes(testXmlFile.toPath()));
            final List<Difference> differences = DifferenceFinder.findDifferences(controlXmlContent, testXmlContent);
            final DifferenceResult differenceResult = DifferenceAnalyzer.analyzeDifferences(differences);

            final SimilarityResult similarityResult = SimilarityCalculator.calculate(controlXmlContent, differenceResult.textDifferences().size());
            ReportGenerator.generateReport(differenceResult, similarityResult, outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Something went wrong with reading xml file", e);
        }
    }
}
