package comparator;

import comparator.diffrence.ReportDifference;

import java.util.List;

public record DifferenceResult(List<ReportDifference> textDifferences, List<ReportDifference> attributeDifferences,
                               List<ReportDifference> orderDifferences, List<ReportDifference> otherDifferences) {
}
