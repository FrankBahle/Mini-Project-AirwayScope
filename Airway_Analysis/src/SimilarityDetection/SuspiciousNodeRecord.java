package SimilarityDetection;

import DataStructure.ArrayList;
import java.util.Collections;
import java.util.List;

import GraphConstruction.AirwayNode;

public class SuspiciousNodeRecord {

    private final AirwayNode node;
    private final List<String> reasons;

    private double suspicionScore;

    private boolean abruptEnding;
    private boolean highTortuosity;
    private boolean strongTapering;
    private boolean branchAbnormality;

    private double localTortuosity;
    private double localTapering;
    private double widthDropRatio;
    private double terminalSegmentLength;

    public SuspiciousNodeRecord(AirwayNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null.");
        }

        this.node = node;
        this.reasons = new ArrayList<>();
        this.suspicionScore = 0.0;

        this.abruptEnding = false;
        this.highTortuosity = false;
        this.strongTapering = false;
        this.branchAbnormality = false;

        this.localTortuosity = 0.0;
        this.localTapering = 0.0;
        this.widthDropRatio = 0.0;
        this.terminalSegmentLength = 0.0;
    }

    public AirwayNode getNode() {
        return node;
    }

    public int getRow() {
        return node.getRow();
    }

    public int getCol() {
        return node.getCol();
    }

    public String getNodeType() {
        return node.getType().name();
    }

    public double getSuspicionScore() {
        return suspicionScore;
    }

    public boolean isAbruptEnding() {
        return abruptEnding;
    }

    public boolean isHighTortuosity() {
        return highTortuosity;
    }

    public boolean isStrongTapering() {
        return strongTapering;
    }

    public boolean isBranchAbnormality() {
        return branchAbnormality;
    }

    public double getLocalTortuosity() {
        return localTortuosity;
    }

    public double getLocalTapering() {
        return localTapering;
    }

    public double getWidthDropRatio() {
        return widthDropRatio;
    }

    public double getTerminalSegmentLength() {
        return terminalSegmentLength;
    }

    public List<String> getReasons() {
        return Collections.unmodifiableList(reasons);
    }

    public boolean isSuspicious() {
        return abruptEnding || highTortuosity || strongTapering || branchAbnormality || !reasons.isEmpty();
    }

    public void addScore(double value) {
        suspicionScore += value;
    }

    public void addReason(String reason) {
        if (reason == null) {
            return;
        }

        String cleaned = reason.trim();
        if (cleaned.isEmpty()) {
            return;
        }

        if (!reasons.contains(cleaned)) {
            reasons.add(cleaned);
        }
    }

    public void markAbruptEnding(double terminalSegmentLength) {
        this.abruptEnding = true;
        this.terminalSegmentLength = terminalSegmentLength;
        addReason("Abrupt ending");
    }

    public void markHighTortuosity(double localTortuosity) {
        this.highTortuosity = true;
        this.localTortuosity = localTortuosity;
        addReason("High tortuosity");
    }

    public void markStrongTapering(double localTapering, double widthDropRatio) {
        this.strongTapering = true;
        this.localTapering = localTapering;
        this.widthDropRatio = widthDropRatio;
        addReason("Strong tapering");
    }

    public void markBranchAbnormality() {
        this.branchAbnormality = true;
        addReason("Branch abnormality");
    }

    public String getNodeKey() {
        return "(" + getRow() + "," + getCol() + ")";
    }

    public String buildSummary() {
        StringBuilder builder = new StringBuilder();

        builder.append("Node ");
        builder.append(getNodeKey());
        builder.append(" [");
        builder.append(getNodeType());
        builder.append("]");

        if (reasons.isEmpty()) {
            builder.append(" has no suspicious reasons recorded.");
            return builder.toString();
        }

        builder.append(" suspicious because: ");

        for (int i = 0; i < reasons.size(); i++) {
            builder.append(reasons.get(i));
            if (i < reasons.size() - 1) {
                builder.append(", ");
            }
        }

        builder.append(". Score = ");
        builder.append(String.format("%.3f", suspicionScore));

        if (highTortuosity) {
            builder.append(", tortuosity = ");
            builder.append(String.format("%.3f", localTortuosity));
        }

        if (strongTapering) {
            builder.append(", tapering = ");
            builder.append(String.format("%.3f", localTapering));
            builder.append(", width drop ratio = ");
            builder.append(String.format("%.3f", widthDropRatio));
        }

        if (abruptEnding) {
            builder.append(", terminal length = ");
            builder.append(String.format("%.3f", terminalSegmentLength));
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return buildSummary();
    }
}