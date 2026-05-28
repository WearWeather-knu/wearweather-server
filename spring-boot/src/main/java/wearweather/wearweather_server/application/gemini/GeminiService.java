package wearweather.wearweather_server.application.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import wearweather.wearweather_server.application.user.dto.UserResult;
import wearweather.wearweather_server.presentation.gemini.dto.OutfitImageRecommendationRequest;
import wearweather.wearweather_server.presentation.gemini.dto.OutfitImageRecommendationResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String imageModel;

    public GeminiService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.5-flash}") String model,
            @Value("${gemini.image-model:gemini-2.5-flash-image}") String imageModel
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
        this.apiKey = apiKey;
        this.model = model;
        this.imageModel = imageModel;
    }

    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프롬프트를 입력해주세요.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gemini API key 설정이 필요합니다.");
        }

        GeminiRequest request = new GeminiRequest(
                List.of(new Content(List.of(new Part(prompt))))
        );

        GeminiResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini API 호출에 실패했습니다.", exception);
        }

        String text = extractText(response);
        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini API 응답이 비어있습니다.");
        }

        return text;
    }

    public OutfitImageRecommendationResponse recommendOutfitImages(
            UserResult user,
            OutfitImageRecommendationRequest request
    ) {
        validateApiKey();
        if (request.weather() == null || request.weather().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "날씨 정보를 입력해주세요.");
        }

        String prompt = buildOutfitImagePrompt(user, request);
        GeminiImageRequest geminiRequest = new GeminiImageRequest(
                List.of(new Content(List.of(new Part(prompt)))),
                new GenerationConfig(List.of("TEXT", "IMAGE"))
        );

        GeminiResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", imageModel)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(geminiRequest)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini 이미지 생성 API 호출에 실패했습니다.", exception);
        }

        List<OutfitImageRecommendationResponse.GeneratedImage> images = extractImages(response);
        if (images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini 이미지 생성 응답이 비어있습니다.");
        }

        return new OutfitImageRecommendationResponse(extractText(response), images);
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gemini API key 설정이 필요합니다.");
        }
    }

    private String buildOutfitImagePrompt(UserResult user, OutfitImageRecommendationRequest request) {
        int outfitCount = clampImageCount(request.outfitCount());
        String style = request.style() == null || request.style().isBlank() ? "realistic Korean street fashion photo" : request.style();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a realistic fashion recommendation image for today's weather.\n");
        prompt.append("The image should show ").append(outfitCount).append(" complete outfit option");
        if (outfitCount > 1) {
            prompt.append("s");
        }
        prompt.append(" as a clean lookbook collage.\n");
        prompt.append("Do not include faces, logos, brand names, or readable text.\n");
        prompt.append("Use neatly arranged clothing items on a simple studio background.\n");
        prompt.append("Prioritize clothes that are practical for the weather.\n");
        prompt.append("Visual style: ").append(style).append(".\n\n");

        prompt.append("Weather data:\n");
        prompt.append(formatValue(request.weather())).append("\n\n");

        prompt.append("User profile:\n");
        appendPromptLine(prompt, "nickname", user.nickname());
        appendPromptLine(prompt, "age", user.age() == null ? null : user.age() + "");
        appendPromptLine(prompt, "gender", user.gender() == null ? null : user.gender().name());

        prompt.append("\nImportant constraints:\n");
        prompt.append("- Generate only safe daily clothing recommendations.\n");
        prompt.append("- Match temperature, rain, wind, humidity, and current condition if present.\n");
        prompt.append("- If weather data is in Korean, understand it naturally.\n");
        prompt.append("- Make the outfits easy to inspect as product-like photos.\n");

        return prompt.toString();
    }

    private int clampImageCount(Integer outfitCount) {
        if (outfitCount == null) {
            return 3;
        }
        return Math.max(1, Math.min(outfitCount, 4));
    }

    private void appendPromptLine(StringBuilder prompt, String label, String value) {
        if (value != null && !value.isBlank()) {
            prompt.append("- ").append(label).append(": ").append(value).append("\n");
        }
    }

    private List<OutfitImageRecommendationResponse.GeneratedImage> extractImages(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return List.of();
        }

        List<OutfitImageRecommendationResponse.GeneratedImage> images = new ArrayList<>();
        for (Candidate candidate : response.candidates()) {
            if (candidate.content() == null || candidate.content().parts() == null) {
                continue;
            }
            for (Part part : candidate.content().parts()) {
                InlineData inlineData = part.imageData();
                if (inlineData == null || inlineData.data() == null || inlineData.data().isBlank()) {
                    continue;
                }

                String mimeType = inlineData.mimeType() == null || inlineData.mimeType().isBlank()
                        ? "image/png"
                        : inlineData.mimeType();
                String dataUrl = "data:" + mimeType + ";base64," + inlineData.data();
                images.add(new OutfitImageRecommendationResponse.GeneratedImage(mimeType, dataUrl));
            }
        }
        return images;
    }

    private String formatValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> normalized.put(String.valueOf(key), mapValue));
            return normalized.entrySet().stream()
                    .map(entry -> "- " + entry.getKey() + ": " + formatValue(entry.getValue()))
                    .collect(Collectors.joining("\n"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::formatValue)
                    .collect(Collectors.joining(", "));
        }
        return String.valueOf(value);
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return "";
        }

        Content content = response.candidates().getFirst().content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            return "";
        }

        return content.parts().stream()
                .map(Part::text)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining());
    }

    private record GeminiRequest(List<Content> contents) {
    }

    private record GeminiImageRequest(List<Content> contents, GenerationConfig generationConfig) {
    }

    private record GenerationConfig(List<String> responseModalities) {
    }

    private record GeminiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text, InlineData inlineData, InlineData inline_data) {
        private Part(String text) {
            this(text, null, null);
        }

        private InlineData imageData() {
            return inlineData != null ? inlineData : inline_data;
        }
    }

    private record InlineData(String mimeType, String data) {
    }
}
