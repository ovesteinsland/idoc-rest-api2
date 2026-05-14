package no.softwarecontrol.idoc.webservices.data.bedrock;

public class ImageAnalysisRequest {
    private String base64Image;
    private String mediaType = "image/jpeg";
    private String prompt;

    public String getBase64Image() { return base64Image; }
    public void setBase64Image(String base64Image) { this.base64Image = base64Image; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}