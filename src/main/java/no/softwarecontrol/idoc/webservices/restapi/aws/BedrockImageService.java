package no.softwarecontrol.idoc.webservices.restapi.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.webservices.data.bedrock.ImageAnalysisRequest;
import no.softwarecontrol.idoc.webservices.data.bedrock.ImageAnalysisResponse;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.Base64;

@Stateless
@Path("no.softwarecontrol.idoc.aws.bedrock.image")
@RolesAllowed({"ApplicationRole"})
public class BedrockImageService {

    private static final String MODEL_ID = "eu.anthropic.claude-sonnet-4-6";

    private static final double SONNET_INPUT_COST_PER_TOKEN  = 3.0  / 1_000_000;
    private static final double SONNET_OUTPUT_COST_PER_TOKEN = 15.0 / 1_000_000;

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;

    public BedrockImageService() {
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @POST
    @Path("analyzeImage")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String analyzeImage(ImageAnalysisRequest imageRequest) {
        try {
            String prompt = imageRequest.getPrompt() != null ? imageRequest.getPrompt()
                    : "Analyser dette bildet og beskriv hva du ser.";

            String mediaType = imageRequest.getMediaType() != null ? imageRequest.getMediaType() : "image/jpeg";

            ImageSource imageSource = ImageSource.builder()
                    .bytes(SdkBytes.fromByteArray(Base64.getDecoder().decode(imageRequest.getBase64Image())))
                    .build();

            ImageBlock imageBlock = ImageBlock.builder()
                    .format(mediaType.contains("png") ? ImageFormat.PNG : ImageFormat.JPEG)
                    .source(imageSource)
                    .build();

            Message message = Message.builder()
                    .role(ConversationRole.USER)
                    .content(
                            ContentBlock.fromText(prompt),
                            ContentBlock.fromImage(imageBlock)
                    )
                    .build();

            ConverseRequest converseRequest = ConverseRequest.builder()
                    .modelId(MODEL_ID)
                    .messages(message)
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(1024)
                            .temperature(0.2f)
                            .build())
                    .build();

            ConverseResponse converseResponse = bedrockClient.converse(converseRequest);

            String analysisResult = converseResponse.output().message().content().get(0).text();

            int inputTokens  = converseResponse.usage().inputTokens();
            int outputTokens = converseResponse.usage().outputTokens();
            double estimatedCost = (inputTokens  * SONNET_INPUT_COST_PER_TOKEN)
                    + (outputTokens * SONNET_OUTPUT_COST_PER_TOKEN);

            ImageAnalysisResponse response = new ImageAnalysisResponse();
            response.setAnalysis(analysisResult);
            response.setInputTokens(inputTokens);
            response.setOutputTokens(outputTokens);
            response.setEstimatedCostUSD(estimatedCost);

            return objectMapper.writeValueAsString(response);

        } catch (Exception e) {
            throw new RuntimeException("Feil ved bildeanalyse: " + e.getMessage(), e);
        }
    }
}