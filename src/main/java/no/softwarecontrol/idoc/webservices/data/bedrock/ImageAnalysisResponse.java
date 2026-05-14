package no.softwarecontrol.idoc.webservices.data.bedrock;

public class ImageAnalysisResponse {
    private String analysis;
    private int inputTokens;
    private int outputTokens;
    private double estimatedCostUSD;

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
    public double getEstimatedCostUSD() { return estimatedCostUSD; }
    public void setEstimatedCostUSD(double estimatedCostUSD) { this.estimatedCostUSD = estimatedCostUSD; }
}