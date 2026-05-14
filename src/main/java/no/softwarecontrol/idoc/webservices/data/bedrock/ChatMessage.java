// ChatMessage.java
package no.softwarecontrol.idoc.webservices.data.bedrock;

public class ChatMessage {
    private long id = System.currentTimeMillis();
    private String role;
    private String content;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}