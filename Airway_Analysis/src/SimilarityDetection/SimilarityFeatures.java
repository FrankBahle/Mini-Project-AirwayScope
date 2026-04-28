package SimilarityDetection;

public class SimilarityFeatures {
    private final int branchCount;
    private final int branchPointCount;
    private final double averageTapering;
    private final double averageTortuosity;
    private final double abruptEndingRatio;

    public SimilarityFeatures(
            int branchCount,
            int branchPointCount,
            double averageTapering,
            double averageTortuosity,
            double abruptEndingRatio
    ) {
        this.branchCount = branchCount;
        this.branchPointCount = branchPointCount;
        this.averageTapering = averageTapering;
        this.averageTortuosity = averageTortuosity;
        this.abruptEndingRatio = abruptEndingRatio;
    }

    public int getBranchCount() {
        return branchCount;
    }

    public int getBranchPointCount() {
        return branchPointCount;
    }

    public double getAverageTapering() {
        return averageTapering;
    }

    public double getAverageTortuosity() {
        return averageTortuosity;
    }

    public double getAbruptEndingRatio() {
        return abruptEndingRatio;
    }

    public double[] toVector() {
        return new double[] {
                branchCount,
                branchPointCount,
                averageTapering,
                averageTortuosity,
                abruptEndingRatio
        };
    }
}
