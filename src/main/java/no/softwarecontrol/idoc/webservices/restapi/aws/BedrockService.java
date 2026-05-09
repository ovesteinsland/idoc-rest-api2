package no.softwarecontrol.idoc.webservices.restapi.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.webservices.data.bedrock.BedrockObservationTextResponse;
import no.softwarecontrol.idoc.webservices.data.bedrock.BedrockRequest;
import no.softwarecontrol.idoc.webservices.restapi.ProjectFacadeREST;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;

@Stateless
@Path("no.softwarecontrol.idoc.aws.bedrock")
@RolesAllowed({"ApplicationRole"})
public class BedrockService {
    private DefaultAwsRegionProviderChain regionProvider = DefaultAwsRegionProviderChain.builder().build();
    private BedrockRuntimeClient bedrockClient;
    private ObjectMapper objectMapper;

    private static final double HAIKU_INPUT_COST_PER_TOKEN = 0.25 / 1_000_000;   // $0.25 per 1M tokens
    private static final double HAIKU_OUTPUT_COST_PER_TOKEN = 1.25 / 1_000_000;   // $1.25 per 1M tokens

    public BedrockService() {
        bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        objectMapper = new ObjectMapper();
    }

    private InvokeModelRequest buildClaudeRequest(String prompt, int maxTokens, double temperature) throws JsonProcessingException {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", maxTokens,
                "temperature", temperature,
                "top_p", 0.9,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt
                ))
        ));

        return InvokeModelRequest.builder()
                .modelId("anthropic.claude-3-haiku-20240307-v1:0")
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();
    }

    @POST
    @Path("generateObservationDescription")
    @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public String generateObservationDescription(BedrockRequest bedrockRequest) {
        try {
            String keyWords = bedrockRequest.getContext();
            String languageCode = bedrockRequest.getLanguageCode();
            String countryCode = bedrockRequest.getCountryCode();

            String buildYear = bedrockRequest.getBuildYear() != null
                    ? bedrockRequest.getBuildYear()
                    : "UNKNOWN";

            BedrockRequest.ResponseTone tone = bedrockRequest.getResponseTone() != null
                    ? bedrockRequest.getResponseTone()
                    : BedrockRequest.ResponseTone.FORMAL;

            String prompt = String.format(
                    "You are an experienced electrical professional in the country with ISO 3166-1 alpha-2 code \"%s\" " +
                            "who performs inspections of electrical installations in accordance with that country's national regulations.\n\n" +

                            "TASK:\n" +
                            "Generate a precise and professional deviation description based on the following keywords: \"%s\"\n\n" +
                            "Build Year: \"%s\" " +
                            "Consider that the recipient of the generated text is property owners without detailed knowledge of electrical engineering. " +
                            "Limit the use of technical terminology.\n\n" +

                            "REQUIREMENTS FOR THE DESCRIPTION:\n" +
                            "- Write the entire response in the language identified by ISO 639-1 code \"%s\".\n" +
                            "- The tone of the response must be %s\n" +
                            "- Describe the deviation clearly and concisely.\n" +
                            "- Avoid terms like 'your home', 'your house' etc. It is uncertain what type of property is being inspected.\n" +
                            "- Include relevant references to applicable national regulations and standards for electrical installations " +
                            "in the country \"%s\". Cite specific sections, clauses, and paragraphs where possible.\n" +
                            "- Specify condition grade (DEVIATION_SEVERITY) ONLY as an integer where a higher number means higher severity.\n" +
                            "- Suggest specific remediation measures.\n\n" +

                            "FORMATTING:\n" +
                            "Respond in the following structure:\n" +
                            "OBSERVATION_DESCRIPTION:\n <description>\n" +
                            "OBSERVATION_REGULATION:\n <references to applicable national regulations and standards>\n" +
                            "DEVIATION_SEVERITY:\n <0-3>\n" +
                            "OBSERVATION_ACTION: <specific measure>\n\n" +

                            "IMPORTANT:\n" +
                            "- Refer ONLY to regulations and standards that actually apply in country \"%s\".\n" +
                            "- Be aware of the fact that the installation might have been acccording to regulations at the time the installation was built.\n " +
                            "- Be precise with section and clause references.\n" +
                            "- Do not claim illegal installation, unless you are absolutely sure.\n" +
                            "- Do not overestimate deviation severity.\n" +
                            "- Only use highest severity when instant risk of fire or death.\n " +
                            "- Only use second highest severity when breaking regulation\n" +
                            "- Do not invent regulation references that do not exist.\n" +
                            "- If you are unsure of the exact section, specify the regulation in general and note that " +
                            "the installer/inspector should verify the exact reference.\n",
                    countryCode, keyWords, buildYear, languageCode, tone.getPromptInstruction(), countryCode, countryCode
            );

            InvokeModelRequest request = buildClaudeRequest(prompt, 2000, 0.2);

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            String rawText = jsonResponse.get("content").get(0).get("text").asText();

            // Hent token-forbruk fra responsen
            JsonNode usage = jsonResponse.get("usage");
            int inputTokens = usage.get("input_tokens").asInt();
            int outputTokens = usage.get("output_tokens").asInt();

            // Beregn kostnadsestimat
            double estimatedCost = (inputTokens * HAIKU_INPUT_COST_PER_TOKEN)
                    + (outputTokens * HAIKU_OUTPUT_COST_PER_TOKEN);

            // Parse den strukturerte teksten inn i separate felter
            BedrockObservationTextResponse bedrockObservationTextResponse = parseObservationResponse(rawText);
            bedrockObservationTextResponse.setInputTokens(inputTokens);
            bedrockObservationTextResponse.setOutputTokens(outputTokens);
            bedrockObservationTextResponse.setEstimatedCostUSD(estimatedCost);

            return objectMapper.writeValueAsString(bedrockObservationTextResponse);

        } catch (Exception e) {
            throw new RuntimeException("Feil ved generering av avviksbeskrivelse: " + e.getMessage(), e);
        }
    }

    @POST
    @Path("generateObservationImprovementText")
    @Consumes(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public String generateObservationImprovementText(BedrockRequest bedrockRequest) {
        try {
            String deviationDescription = bedrockRequest.getContext();
            String languageCode = bedrockRequest.getLanguageCode();
            String countryCode = bedrockRequest.getCountryCode();
            BedrockRequest.ResponseTone tone = bedrockRequest.getResponseTone() != null
                    ? bedrockRequest.getResponseTone()
                    : BedrockRequest.ResponseTone.FORMAL;

            String prompt = String.format(
                    "Electrical technician/installer in the country with ISO 3166-1 alpha-2 code \"%s\" " +
                            "who performs remediation of deviations found during inspections of electrical installations.\n\n" +

                            "TASK:\n" +
                            "Based on the following deviation description, generate a professional remediation/improvement text " +
                            "that documents what has been done to rectify the deviation.\n\n" +

                            "DEVIATION DESCRIPTION:\n\"%s\"\n\n" +

                            "REQUIREMENTS:\n" +
                            "- Write the entire response in the language identified by ISO 639-1 code \"%s\".\n" +
                            "- The tone of the response must be %s\n" +
                            "- Describe concretely what remediation work has been performed to rectify the deviation.\n" +
                            "- The text should read as a confirmation that the work has been completed.\n" +
                            "- Do NOT repeat everything in the devation description, since it will be presented together with this response.\n" +
                            "- Keep it very short, concise and factual. Max 1000 characters in response.\n\n" +

                            "FORMATTING:\n" +
                            "Respond in the following structure:\n" +
                            "IMPROVEMENT_DESCRIPTION:\n <description of remediation work performed>\n" +

                            "- The text should be written from the perspective of the technician who performed the remediation.\n",
                    countryCode, deviationDescription, languageCode, tone.getPromptInstruction()
            );

            InvokeModelRequest request = buildClaudeRequest(prompt, 2000, 0.2);

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);

            String rawText = jsonResponse.get("content").get(0).get("text").asText();

            JsonNode usage = jsonResponse.get("usage");
            int inputTokens = usage.get("input_tokens").asInt();
            int outputTokens = usage.get("output_tokens").asInt();

            double estimatedCost = (inputTokens * HAIKU_INPUT_COST_PER_TOKEN)
                    + (outputTokens * HAIKU_OUTPUT_COST_PER_TOKEN);

            BedrockObservationTextResponse bedrockObservationTextResponse = new BedrockObservationTextResponse();
            bedrockObservationTextResponse.setDescription(extractSection(rawText, "IMPROVEMENT_DESCRIPTION:"));
            bedrockObservationTextResponse.setRegulation(extractSection("", "IMPROVEMENT_REGULATION:"));
            bedrockObservationTextResponse.setInputTokens(inputTokens);
            bedrockObservationTextResponse.setOutputTokens(outputTokens);
            bedrockObservationTextResponse.setEstimatedCostUSD(estimatedCost);

            return objectMapper.writeValueAsString(bedrockObservationTextResponse);

        } catch (Exception e) {
            throw new RuntimeException("Feil ved generering av utbedringsbeskrivelse: " + e.getMessage(), e);
        }
    }

    private BedrockObservationTextResponse parseObservationResponse(String rawText) {
        BedrockObservationTextResponse response = new BedrockObservationTextResponse();

        response.setDescription(extractSection(rawText, "OBSERVATION_DESCRIPTION:"));
        response.setRegulation(extractSection(rawText, "OBSERVATION_REGULATION:"));
        response.setAction(extractSection(rawText, "OBSERVATION_ACTION:"));

        String severityStr = extractSection(rawText, "DEVIATION_SEVERITY:");
        try {
            response.setDeviationSeverity(Integer.parseInt(severityStr.trim()));
        } catch (NumberFormatException e) {
            // Forsøk å finne første siffer i strengen
            severityStr.chars()
                    .filter(Character::isDigit)
                    .findFirst()
                    .ifPresentOrElse(
                            ch -> response.setDeviationSeverity(Character.getNumericValue(ch)),
                            () -> response.setDeviationSeverity(-1)
                    );
        }

        return response;
    }

    private String extractSection(String text, String sectionKey) {
        int startIndex = text.indexOf(sectionKey);
        if (startIndex == -1) {
            return "";
        }
        startIndex += sectionKey.length();

        // Finn neste seksjonsnøkkel for å avgrense slutten
        String[] sectionKeys = {
                "OBSERVATION_DESCRIPTION:", "OBSERVATION_REGULATION:",
                "DEVIATION_SEVERITY:", "OBSERVATION_ACTION:"
        };

        int endIndex = text.length();
        for (String key : sectionKeys) {
            if (key.equals(sectionKey)) continue;
            int idx = text.indexOf(key, startIndex);
            if (idx != -1 && idx < endIndex) {
                endIndex = idx;
            }
        }

        return text.substring(startIndex, endIndex).trim();
    }

    @GET
    @Path("generateProjectSummary/{languageCode}/{projectId}")
    public String createSummary(
            @PathParam("languageCode") String languageCode,
            @PathParam("projectId") String projectId) throws JsonProcessingException {
        StringBuilder projectText = new StringBuilder();
        Project project = ProjectFacadeREST.getInstance().findNative(projectId);

        projectText.append("Kontrolltype: ");
        projectText.append(project.getDisipline().getName()).append("\n");
        projectText.append(project.getProjectNumber().toString()).append("\n");

        if (!project.getReportList().isEmpty()) {
            Report report = project.getReportList().get(0);
            //String reportText = generateReportText(report);
            //projectText.append(reportText).append("\n");
        }

        if(project.getCustomer() != null) {
            projectText.append("Kunde: ").append(project.getCustomer().getName()).append("\n");
        }
        if(project.getAsset() != null) {
            projectText.append("Bygning: ").append(project.getAsset().getName()).append("\n");
        }


        List<Observation> observations = project.getObservationList().stream().filter(p -> p.isDeleted() == false).toList();
        String observationText = generateObservationText(observations);
        projectText.append(observationText);
        for(Project child: project.getProjectList()) {
            if(child.getAsset() != null) {
                projectText.append("Bygning: ").append(child.getAsset().getName()).append("\n");
            }
            List<Observation> childObservations = child.getObservationList().stream().filter(p -> p.isDeleted() == false).toList();
            String childObservationText = generateObservationText(childObservations);
            projectText.append(childObservationText);
        }
        return summarizeText(projectText.toString());
    }

    public String summarizeText(String inputText) {
        try {
            String prompt = String.format(
                    "Analyser følgende tekst og lag en presis konklusjon. Fokuser på alvorlige avvik. Ikke lag overskrift Konklusjon: \n\n" +
                            "INSTRUKSJONER:\n" +
                            "- Tilstandsgrader rangeres fra 0-3 der 3 er mest alvorlig\n" +
                            "- Avvik med tilstadsgrad 3 skal tydelig påpekes i konklusjonen" +
                            "- Avvik med tilstandsgrad 0 og 1 trenger ikke beskrives." +
                            "- Antall avvik i de forskjellige kategoriene man vises." +
                            "- Skriv klart og konsist\n" +
                            "- Ikke vær bastant og hold en høflig tone\n" +
                            "- Lag en fornuftig formattering av dato/periode basert på obserservasjonenes opprettet dato. Ikke angi klokkeslett. \n" +
                            "- Maksimalt 6-8 setninger\n\n" +
                            "TEKST SOM SKAL LAGES SAMMENDRAG AV:\n%s\n\n",
                    inputText
            );

            InvokeModelRequest request = buildClaudeRequest(prompt, 3000, 0.1);

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            return jsonResponse.get("content").get(0).get("text").asText();

        } catch (Exception e) {
            throw new RuntimeException("Feil ved tekstsammendrag: " + e.getMessage(), e);
        }
    }


    private String generateObservationText(List<Observation> observations) {
        StringBuilder builder = new StringBuilder();
        for (Observation observation : observations) {
            builder.append("Observation: ").append(observation.getObservationNo()).append("\n");
            Asset asset = observation.getProject().getAsset();
            if (asset != null && observation.getLocation() != null) {
                Location location = asset.findLocation(observation.getLocation());
                if (location != null) {
                    builder.append("Plassering: ");
                    builder.append(location.getFullName()).append("\n");
                }
            }
            if (asset != null && observation.getEquipment() != null) {
                builder.append("Utstyr: ");
                builder.append(observation.getEquipment().getFullName()).append("\n");
            }
            builder.append("Tilstandsgrad: ");
            builder.append(observation.getDeviation()).append("\n");
            builder.append(observation.getDescription()).append("\n");
            builder.append(observation.getAction()).append("\n");
            if(observation.getCreatedDate() != null) {
                builder.append("Opprettet dato: ").append(observation.getCreatedDate().toString()).append("\n");
            }
        }
        return builder.toString();
    }

    private String generateReportText(Report report) {
        StringBuilder builder = new StringBuilder();
        for (ReportSection reportSection : report.getReportSectionList().stream().filter(p -> !p.getIsIgnored()).toList()) {
            if(reportSection.getReportSectionType() == ReportSection.SectionType.DEFAULT) {
                builder.append(reportSection.getTitle()).append("\n");
                builder.append(reportSection.getBody()).append("\n");
            }
        }
        return builder.toString();
    }
}