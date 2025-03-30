package comparator;

import comparator.diffrence.ReportDifference;
import comparator.similarity.SimilarityResult;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ReportGenerator {

    public static void generateReport(final DifferenceResult differenceResult, final SimilarityResult similarityResult, final String outputFilePath) {
        try (final PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            writer.println("============ XML COMPARISON ============");
            writer.println("Control nodes: " + similarityResult.controlXmlNodeCount());
            writer.println("Mismatch count: " + similarityResult.mismatchCount());
            writer.printf("\nSimilarity: %.2f%%\n", similarityResult.similarity());

            printDifferences(writer, "\n=== Text differences ===", differenceResult.textDifferences());
            printDifferences(writer, "\n=== Attribute differences ===", differenceResult.attributeDifferences());
            printDifferences(writer, "\n=== Node order change ===", differenceResult.orderDifferences());
            printDifferences(writer, "\n=== Other ===", differenceResult.otherDifferences());
        } catch (IOException e) {
            throw new RuntimeException("Error during generating report");
        }
    }

    private static void printDifferences(final PrintWriter writer, final String header, final List<? extends ReportDifference> differences) {
        if (!differences.isEmpty()) {
            writer.println(header);
            differences.forEach(writer::println);
        }
    }
}
