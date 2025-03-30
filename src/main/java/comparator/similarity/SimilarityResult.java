package comparator.similarity;

public record SimilarityResult(int controlXmlNodeCount, int correctNodeCount, int mismatchCount, double similarity) {
}
