package no.softwarecontrol.idoc.webservices.data.bedrock;

public class BedrockRequest {

    public enum ResponseTone {
        FORMAL("formal and objective. Use professional language with a neutral, impersonal style."),
        FRIENDLY("friendly and approachable. Use a warm, easy-to-understand style while remaining professional."),
        AUTHORITATIVE("authoritative and direct. Use firm, clear language that conveys urgency and importance."),
        SIMPLIFIED("simplified and educational. Use plain language, explain concepts, and avoid jargon as much as possible.");

        private final String promptInstruction;

        ResponseTone(String promptInstruction) {
            this.promptInstruction = promptInstruction;
        }

        public String getPromptInstruction() {
            return promptInstruction;
        }
    }

    private String languageCode;
    private String countryCode;
    private String prompt;
    private String context;
    private String buildYear;
    private ResponseTone responseTone;

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public ResponseTone getResponseTone() {
        return responseTone;
    }

    public void setResponseTone(ResponseTone responseTone) {
        this.responseTone = responseTone;
    }

    public String getBuildYear() {
        return buildYear;
    }

    public void setBuildYear(String buildYear) {
        this.buildYear = buildYear;
    }
}