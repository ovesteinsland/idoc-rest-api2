package no.softwarecontrol.idoc.webservices.data.bedrock;
// ... existing code ...
public class BedrockObservationTextResponse {
    private String description;
    private String regulation;
    private int deviationSeverity;
    private String action;
    private int inputTokens;
    private int outputTokens;
    private double estimatedCostUSD;

    public BedrockObservationTextResponse() {}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRegulation() { return regulation; }
    public void setRegulation(String regulation) { this.regulation = regulation; }

    public int getDeviationSeverity() { return deviationSeverity; }
    public void setDeviationSeverity(int deviationSeverity) { this.deviationSeverity = deviationSeverity; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }

    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }

    public double getEstimatedCostUSD() { return estimatedCostUSD; }
    public void setEstimatedCostUSD(double estimatedCostUSD) { this.estimatedCostUSD = estimatedCostUSD; }
}