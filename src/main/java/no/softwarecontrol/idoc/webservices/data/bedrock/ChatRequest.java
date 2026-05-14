// ChatRequest.java
package no.softwarecontrol.idoc.webservices.data.bedrock;

import java.util.List;

public class ChatRequest {
    private List<ChatMessage> messages;
    private String systemPrompt;
    private String languageCode = "nb";
    private String countryCode = "NO";
    
    public ChatRequest() {}

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

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
}