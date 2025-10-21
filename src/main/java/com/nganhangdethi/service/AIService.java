package com.nganhangdethi.service;

import com.nganhangdethi.model.Question;
import com.nganhangdethi.util.AppConfig;
import com.nganhangdethi.util.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String DEFAULT_GEMINI_TEXT_MODEL = "gemini-1.5-flash-latest";
    private static final String DEFAULT_GEMINI_VISION_MODEL = "gemini-1.5-flash-latest";

    private final OkHttpClient httpClient;
    private String apiKey;

    public AIService() {
        this.apiKey = AppConfig.getGeminiApiKey();
        if (this.apiKey == null || this.apiKey.trim().isEmpty() ||
            this.apiKey.startsWith("YOUR_") || "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE".equalsIgnoreCase(this.apiKey.trim())) {
            logger.warn("AIService: Google Gemini API Key CHƯA ĐƯỢC CẤU HÌNH hoặc đang sử dụng giá trị placeholder. Các chức năng AI sẽ không hoạt động.");
            this.apiKey = null;
        }

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .build();
    }

    public String getAiSuggestion(String userPrompt) {
        if (apiKey == null) {
            return "Lỗi: API Key của Google AI (Gemini) chưa được cấu hình trong Cài đặt.";
        }
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            logger.warn("User prompt rỗng, không gửi yêu cầu đến AI.");
            return "Lỗi: Nội dung yêu cầu AI không được để trống.";
        }
        return executeGeminiRequest(userPrompt, null, null, DEFAULT_GEMINI_TEXT_MODEL, "gợi ý dựa trên văn bản");
    }

    public String getAiSuggestionFromImage(String textPromptForImage, String base64Image, String imageMimeType) {
        if (apiKey == null) { return "Lỗi: API Key của Google AI (Gemini) chưa được cấu hình."; }
        if (base64Image == null || base64Image.trim().isEmpty()) { return "Lỗi: Dữ liệu hình ảnh rỗng.";}
        if (imageMimeType == null || imageMimeType.trim().isEmpty()) {
            logger.warn("MIME type của ảnh không được cung cấp, mặc định là image/jpeg cho Vision API call.");
            imageMimeType = "image/jpeg";
        }
        return executeGeminiRequest(textPromptForImage, base64Image, imageMimeType, DEFAULT_GEMINI_VISION_MODEL, "gợi ý dựa trên hình ảnh");
    }

    private String executeGeminiRequest(String textPrompt, String base64Image, String imageMimeType, String modelToUse, String requestTypeDescription) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("API Key cho Gemini chưa được cấu hình. Không thể gửi yêu cầu '{}'.", requestTypeDescription);
            return "Lỗi: API Key của Google AI (Gemini) chưa được cấu hình.";
        }

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        JsonObject payload = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        JsonArray partsArray = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", textPrompt != null ? textPrompt : "");
        partsArray.add(textPart);

        if (base64Image != null && !base64Image.trim().isEmpty()) {
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mime_type", imageMimeType != null ? imageMimeType : "image/jpeg");
            inlineData.addProperty("data", base64Image);
            JsonObject imagePart = new JsonObject();
            imagePart.add("inline_data", inlineData);
            partsArray.add(imagePart);
        }

        contentObject.add("parts", partsArray);
        contentsArray.add(contentObject);
        payload.add("contents", contentsArray);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.5);
        generationConfig.addProperty("maxOutputTokens", 8192);
        payload.add("generationConfig", generationConfig);

        JsonArray safetySettingsArray = new JsonArray();
        String[] categories = {"HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT"};
        for (String category : categories) {
            JsonObject setting = new JsonObject();
            setting.addProperty("category", category);
            setting.addProperty("threshold", "BLOCK_NONE");
            safetySettingsArray.add(setting);
        }
        payload.add("safetySettings", safetySettingsArray);

        String apiUrl = GEMINI_API_BASE_URL + modelToUse + ":generateContent?key=" + apiKey;
        String payloadString = payload.toString();

        RequestBody body = RequestBody.create(payloadString, mediaType);
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        logger.debug("Đang gửi yêu cầu '{}' đến Gemini. Model: {}. Prompt (70 chars): '{}'",
                     requestTypeDescription, modelToUse,
                     (textPrompt != null ? textPrompt.substring(0, Math.min(textPrompt.length(), 70)) : "[NO TEXT PROMPT]"));
        logger.trace("Gemini Request Payload ({}):\n{}", requestTypeDescription, payloadString);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBodyString = null;
            if (response.body() != null) {
                responseBodyString = response.body().string();
            }

            if (responseBodyString != null) {
                logger.trace("Toàn bộ phản hồi thô từ Gemini API ({}), Length {}:\n{}",
                             requestTypeDescription, responseBodyString.length(), responseBodyString.substring(0, Math.min(responseBodyString.length(), 1000)));
            } else {
                logger.warn("Phản hồi từ API Gemini ({}) là null.", requestTypeDescription);
            }

            if (!response.isSuccessful()) {
                String errorDetails = responseBodyString != null ? responseBodyString : "(Không có nội dung lỗi từ body)";
                logger.error("Yêu cầu API Gemini ({}) thất bại với mã HTTP {}: {}",
                             requestTypeDescription, response.code(), errorDetails.substring(0, Math.min(errorDetails.length(), 500)));

                if (response.code() == 400) {
                     if (responseBodyString != null && responseBodyString.contains("User location is not supported")) {
                         return "Lỗi từ API Gemini: Vị trí của bạn không được hỗ trợ để sử dụng API này.";
                     }
                     return "Lỗi 400 từ API Gemini (" + requestTypeDescription + "): Yêu cầu không hợp lệ. Chi tiết: " + errorDetails.substring(0, Math.min(errorDetails.length(), 200));
                }
                return "Lỗi từ API Gemini (" + requestTypeDescription + "): " + response.code() + " - " + errorDetails.substring(0, Math.min(errorDetails.length(), 200));
            }

            if (responseBodyString == null || responseBodyString.trim().isEmpty()) {
                logger.error("Phản hồi từ API Gemini ({}) rỗng (mặc dù response code là successful).", requestTypeDescription);
                return "Lỗi: Phản hồi từ API Gemini (" + requestTypeDescription + ") rỗng.";
            }
            return parseGeminiResponse(responseBodyString, requestTypeDescription);

        } catch (IOException e) {
            logger.error("IOException trong khi gọi API Gemini ({}): {}", requestTypeDescription, e.getMessage(), e);
            return "Lỗi kết nối mạng đến dịch vụ AI Gemini (" + requestTypeDescription + "): " + e.getMessage();
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn trong khi xử lý yêu cầu {} từ AI Gemini: {}", requestTypeDescription, e.getMessage(), e);
            return "Lỗi không mong muốn khi xử lý yêu cầu " + requestTypeDescription + " từ AI Gemini.";
        }
    }

    private String parseGeminiResponse(String responseBodyString, String requestType) {
        String jsonToParse = extractJsonBlock(responseBodyString);
        if (jsonToParse == null || jsonToParse.isEmpty()) {
             logger.warn("Không thể trích xuất khối JSON từ phản hồi AI (type: {}). Phản hồi gốc (nếu có): {}", requestType, responseBodyString != null ? responseBodyString.substring(0, Math.min(responseBodyString.length(), 200)) : "null");
             return "Lỗi: Phản hồi từ AI không hợp lệ (null, trống, hoặc không trích xuất được JSON) để phân tích (type: " + requestType + ").";
        }

        try (JsonReader reader = new JsonReader(new StringReader(jsonToParse))) {
            reader.setLenient(true);
            JsonElement parsedElement = JsonParser.parseReader(reader);

            if (parsedElement == null || parsedElement.isJsonNull()) {
                 logger.warn("Sau khi parse lenient, kết quả là JsonNull hoặc null (type: {})", requestType);
                 return "Lỗi: Phản hồi từ AI không thể phân tích thành đối tượng JSON hợp lệ (type: " + requestType + ").";
            }

            if ("trích xuất nhiều câu hỏi từ ảnh".equals(requestType)) {
                if (parsedElement.isJsonArray()) {
                    logger.info("Đã nhận và phân tích thành công phản hồi Gemini (JsonArray trực tiếp) cho {}.", requestType);
                    return jsonToParse; // jsonToParse was the string that formed this array
                } else if (parsedElement.isJsonObject()) {
                    // AI trả về JsonObject, cố gắng trích xuất mảng JSON từ text content
                    JsonObject jsonResponse = parsedElement.getAsJsonObject();
                    if (jsonResponse.has("candidates")) {
                        JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                            if (firstCandidate.has("content") && firstCandidate.get("content").isJsonObject()) {
                                JsonObject contentObj = firstCandidate.getAsJsonObject("content");
                                if (contentObj.has("parts") && contentObj.get("parts").isJsonArray()) {
                                    JsonArray parts = contentObj.getAsJsonArray("parts");
                                    if (!parts.isEmpty() && parts.get(0).isJsonObject() &&
                                        parts.get(0).getAsJsonObject().has("text")) {
                                        String textContentWithPotentialJsonArray = parts.get(0).getAsJsonObject().get("text").getAsString();
                                        // textContentWithPotentialJsonArray có thể là "```json\n[\n...\n]\n```"
                                        // Sử dụng extractJsonBlock để lấy JSON sạch
                                        String extractedArrayString = extractJsonBlock(textContentWithPotentialJsonArray);
                                        if (extractedArrayString != null) {
                                            try {
                                                JsonElement checkParse = JsonParser.parseString(extractedArrayString);
                                                if (checkParse.isJsonArray()) {
                                                    logger.info("Đã trích xuất thành công chuỗi JSON Array từ JsonObject cho '{}'.", requestType);
                                                    return extractedArrayString;
                                                } else {
                                                    logger.warn("Nội dung text trích xuất cho '{}' ('{}') không phải là JSON Array hợp lệ sau khi extractJsonBlock. Trả về lỗi.", requestType, extractedArrayString.substring(0, Math.min(extractedArrayString.length(), 200)));
                                                    return "Lỗi: Nội dung AI trả về cho trích xuất câu hỏi không phải là mảng JSON hợp lệ.";
                                                }
                                            } catch (JsonSyntaxException e) {
                                                logger.warn("Lỗi cú pháp khi kiểm tra chuỗi JSON Array trích xuất cho '{}'. Text: '{}'. Trả về lỗi.", requestType, extractedArrayString.substring(0, Math.min(extractedArrayString.length(), 200)));
                                                return "Lỗi: Lỗi cú pháp trong mảng JSON nội dung AI trả về cho trích xuất câu hỏi.";
                                            }
                                        } else {
                                             logger.warn("Không thể trích xuất khối JSON (mảng) từ textContent ('{}') cho '{}'. Trả về lỗi.", textContentWithPotentialJsonArray.substring(0, Math.min(textContentWithPotentialJsonArray.length(), 200)), requestType);
                                             return "Lỗi: Không thể trích xuất mảng JSON câu hỏi từ phản hồi AI.";
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Nếu không trích xuất được từ JsonObject theo cấu trúc mong đợi
                    logger.warn("Phản hồi AI cho '{}' là JsonObject nhưng không thể trích xuất JSON Array từ cấu trúc dự kiến. Phản hồi: {}", requestType, jsonToParse.substring(0, Math.min(jsonToParse.length(),300)));
                    return "Lỗi: Phản hồi AI cho trích xuất nhiều câu hỏi có cấu trúc không mong đợi (không tìm thấy mảng nội dung).";
                } else {
                    // Không phải JsonArray cũng không phải JsonObject (hoặc type không mong đợi khác)
                    logger.warn("Phản hồi AI cho '{}' mong đợi là JsonArray (hoặc JsonObject chứa Array) nhưng nhận được {}. Chuỗi đã parse: {}", requestType, parsedElement.getClass().getSimpleName(), jsonToParse.substring(0, Math.min(jsonToParse.length(),300)));
                    return "Lỗi: Phản hồi AI cho trích xuất nhiều câu hỏi không phải định dạng mảng JSON mong đợi.";
                }
            }
            
            // Xử lý cho các loại request khác (mong đợi JsonObject và lấy text từ dalamnya)
            if (!parsedElement.isJsonObject()){
                logger.warn("Sau khi parse lenient, kết quả không phải là JsonObject (type: {}). Actual type: {}. Chuỗi đã parse: {}", requestType, parsedElement.getClass().getSimpleName(), jsonToParse.substring(0, Math.min(jsonToParse.length(), 300)));
                return "Lỗi: Phản hồi từ AI không phải là một đối tượng JSON gốc hợp lệ (type: " + requestType + ").";
            }
            JsonObject jsonResponse = parsedElement.getAsJsonObject();

            if (jsonResponse.has("candidates")) {
                JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                    if (firstCandidate.has("content") && firstCandidate.get("content").isJsonObject()) {
                        JsonObject contentObj = firstCandidate.getAsJsonObject("content");
                        if (contentObj.has("parts") && contentObj.get("parts").isJsonArray()) {
                            JsonArray parts = contentObj.getAsJsonArray("parts");
                            if (!parts.isEmpty() && parts.get(0).isJsonObject() &&
                                parts.get(0).getAsJsonObject().has("text")) {
                                String textContent = parts.get(0).getAsJsonObject().get("text").getAsString();
                                logger.info("Đã nhận và phân tích thành công phản hồi Gemini cho {}.", requestType);
                                return textContent.trim(); 
                            }
                        }
                    }
                    if (firstCandidate.has("finishReason") && "SAFETY".equals(firstCandidate.get("finishReason").getAsString())) {
                        logger.warn("Phản hồi Gemini cho {} bị chặn bởi SAFETY. Safety Ratings: {}", requestType, firstCandidate.has("safetyRatings") ? firstCandidate.get("safetyRatings").toString() : "N/A");
                        return "Lỗi: Phản hồi từ AI bị chặn vì lý do an toàn.";
                    }
                }
            }

            if (jsonResponse.has("promptFeedback")) {
                JsonObject promptFeedback = jsonResponse.getAsJsonObject("promptFeedback");
                if (promptFeedback.has("blockReason")) {
                    String blockReason = promptFeedback.get("blockReason").getAsString();
                    logger.warn("Prompt API Gemini bị chặn. Lý do: {}. Feedback: {}", blockReason, promptFeedback.toString());
                    return "Lỗi: Yêu cầu của bạn đã bị chặn bởi bộ lọc an toàn của Gemini (Lý do: " + blockReason + ").";
                }
            }

            logger.warn("Phản hồi API Gemini (type: {}) không chứa cấu trúc nội dung text mong đợi. JSON (500 chars): {}", requestType, jsonResponse.toString().substring(0, Math.min(jsonResponse.toString().length(), 500)));
            return "Lỗi: Không thể trích xuất nội dung mong muốn từ API Gemini (type: " + requestType + ").";

        } catch (JsonSyntaxException e) {
            logger.error("JsonSyntaxException khi phân tích phản hồi API Gemini (type: {}): \"{}\". Chuỗi (500 chars): '{}'", requestType, e.getMessage(), jsonToParse.substring(0, Math.min(jsonToParse.length(), 500)), e);
            return "Lỗi: Không thể phân tích phản hồi JSON từ AI (type: " + requestType + "). Chi tiết: " + e.getMessage();
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException khi phân tích phản hồi API Gemini (type: {}): \"{}\". Chuỗi (500 chars): '{}'", requestType, e.getMessage(), jsonToParse.substring(0, Math.min(jsonToParse.length(), 500)), e);
            return "Lỗi: Định dạng phản hồi JSON không hợp lệ từ API Gemini (type: " + requestType + "). Chi tiết: " + e.getMessage();
        } catch (IOException e) {
            logger.error("IOException với JsonReader cho phản hồi API Gemini (type: {}): {}", requestType, e.getMessage(), e);
            return "Lỗi đọc phản hồi JSON từ AI (type: " + requestType + ").";
        }
    }

    private String extractJsonBlock(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            logger.warn("Phản hồi thô từ AI là null hoặc rỗng.");
            return null;
        }
        String textToSearch = rawResponse.trim();

        // Ưu tiên trích xuất từ khối Markdown trước
        Pattern patternMarkdown = Pattern.compile("```(?:json)?\\s*([\\[\\{][\\s\\S]*?[\\]\\}])\\s*```", Pattern.DOTALL);
        Matcher matcherMarkdown = patternMarkdown.matcher(textToSearch);
        if (matcherMarkdown.find()) {
            String jsonFromMarkdown = matcherMarkdown.group(1).trim();
            // Kiểm tra xem có phải JSON hợp lệ không trước khi trả về
             try {
                JsonParser.parseString(jsonFromMarkdown);
                logger.debug("Đã trích xuất JSON hợp lệ từ khối Markdown.");
                return jsonFromMarkdown;
            } catch (JsonSyntaxException e) {
                logger.warn("JSON trích xuất từ Markdown lỗi cú pháp: {}. Sẽ thử tìm JSON không qua Markdown.", e.getMessage());
                // Không trả về ở đây, để thử các cách khác bên dưới
            }
        }

        // Nếu không có Markdown hoặc Markdown không hợp lệ, thử tìm JSON array hoặc object trực tiếp
        // Thử tìm Json Array trước ([...])
        int firstBracket = textToSearch.indexOf('[');
        int lastBracket = textToSearch.lastIndexOf(']');
        if (firstBracket != -1 && lastBracket > firstBracket) {
            String potentialJsonArray = textToSearch.substring(firstBracket, lastBracket + 1);
            try {
                JsonElement parsed = JsonParser.parseString(potentialJsonArray);
                if (parsed.isJsonArray()) {
                    logger.debug("Đã trích xuất JsonArray tiềm năng (không qua Markdown).");
                    return potentialJsonArray;
                }
            } catch (JsonSyntaxException e) {
                // Bỏ qua, thử tìm Json Object
            }
        }
        
        // Thử tìm Json Object ({...})
        int firstBrace = textToSearch.indexOf('{');
        int lastBrace = textToSearch.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace > firstBrace) {
            String potentialJsonObject = textToSearch.substring(firstBrace, lastBrace + 1);
            try {
                JsonElement parsed = JsonParser.parseString(potentialJsonObject);
                if (parsed.isJsonObject()) {
                    logger.debug("Đã trích xuất JsonObject tiềm năng (không qua Markdown).");
                    return potentialJsonObject;
                }
            } catch (JsonSyntaxException e) {
                 logger.warn("Khối JSON Object tiềm năng (không qua Markdown, từ {{ đến }}) không hợp lệ: {}.", e.getMessage());
            }
        }
        
        // Nếu không tìm thấy JSON hợp lệ nào
        logger.warn("Không tìm thấy khối JSON rõ ràng (Array hoặc Object) trong phản hồi: '{}'. Trả về toàn bộ chuỗi đã trim nếu không có Markdown, hoặc null nếu có Markdown lỗi.",
            textToSearch.substring(0, Math.min(textToSearch.length(), 200)));
        // Nếu có markdown nhưng lỗi, có thể không nên trả về raw textToSearch.
        // Nếu không có markdown và không tìm thấy JSON, trả về textToSearch để try-catch bên ngoài xử lý.
        return matcherMarkdown.find() ? null : textToSearch; // Trả về null nếu có markdown lỗi, ngược lại trả về text đã trim.
    }

    public List<Question> getMultipleQuestionsFromImage(String userProvidedInstructions, String base64Image, String imageMimeType, int numQuestionsToExtract) {
        if (apiKey == null) {
            logger.warn("API Key cho Gemini chưa được cấu hình. Không thể trích xuất nhiều câu hỏi.");
            return new ArrayList<>();
        }
        if (base64Image == null || base64Image.trim().isEmpty()) {
            logger.warn("Dữ liệu hình ảnh rỗng, không thể trích xuất.");
            return new ArrayList<>();
        }
        if (numQuestionsToExtract <= 0) {
            logger.warn("Số lượng câu hỏi yêu cầu trích xuất không hợp lệ: {}", numQuestionsToExtract);
            return new ArrayList<>();
        }

        String promptTemplate = Constants.AI_PROMPT_EXTRACT_MULTIPLE_QUESTIONS_FROM_IMAGE;
        String finalPrompt = promptTemplate.replace("{NUM_QUESTIONS}", String.valueOf(numQuestionsToExtract));
        finalPrompt = finalPrompt.replace("{USER_INSTRUCTIONS}", userProvidedInstructions != null && !userProvidedInstructions.isEmpty() ? userProvidedInstructions : "Không có hướng dẫn thêm.");

        String rawJsonResponseString = executeGeminiRequest(finalPrompt, base64Image, imageMimeType, DEFAULT_GEMINI_VISION_MODEL, "trích xuất nhiều câu hỏi từ ảnh");

        List<Question> extractedQuestions = new ArrayList<>();
        if (rawJsonResponseString == null || rawJsonResponseString.trim().isEmpty() || rawJsonResponseString.startsWith("Lỗi:")) {
            logger.error("Không nhận được phản hồi hợp lệ từ AI hoặc có lỗi khi trích xuất nhiều câu hỏi: {}", rawJsonResponseString);
            // rawJsonResponseString ở đây đã chứa thông báo lỗi từ executeGeminiRequest/parseGeminiResponse
            // Không cần thêm thông báo lỗi mới, phương thức gọi sẽ hiển thị lỗi này.
            return extractedQuestions; 
        }

        try {
            JsonElement parsedElement = JsonParser.parseString(rawJsonResponseString);
            if (parsedElement.isJsonArray()) {
                JsonArray jsonArray = parsedElement.getAsJsonArray();
                logger.info("AI trả về {} đối tượng câu hỏi tiềm năng.", jsonArray.size());

                for (JsonElement qElement : jsonArray) {
                    if (qElement.isJsonObject()) {
                        JsonObject qObject = qElement.getAsJsonObject();
                        Question question = new Question();

                        question.setQuestionText(getStringOrEmpty(qObject, "question_text"));
                        question.setOptionA(getStringOrEmpty(qObject, "option_a"));
                        question.setOptionB(getStringOrEmpty(qObject, "option_b"));
                        question.setOptionC(getStringOrEmpty(qObject, "option_c"));
                        question.setOptionD(getStringOrEmpty(qObject, "option_d"));
                        
                        String correctAnswer = getStringOrEmpty(qObject, "correct_answer").toUpperCase();
                        if (!Arrays.asList("A", "B", "C", "D").contains(correctAnswer)) {
                            logger.warn("Đáp án từ AI không hợp lệ ('{}'), đặt là '-' cho câu hỏi: {}", correctAnswer, question.getQuestionText().substring(0, Math.min(question.getQuestionText().length(), 30)));
                            correctAnswer = "-"; 
                        }
                        question.setCorrectAnswer(correctAnswer);
                        question.setExplanation(getStringOrEmpty(qObject, "explanation"));

                        String level = getStringOrEmpty(qObject, "level");
                        if (level.isEmpty() || !Arrays.asList(Constants.QUESTION_LEVELS_NO_ALL).contains(level)) {
                             logger.warn("Level từ AI không hợp lệ ('{}') hoặc rỗng, đặt là mặc định '{}' cho câu hỏi: {}", level, Constants.DEFAULT_QUESTION_LEVEL, question.getQuestionText().substring(0, Math.min(question.getQuestionText().length(), 30)));
                            level = Constants.DEFAULT_QUESTION_LEVEL;
                        }
                        question.setLevel(level);

                        String type = getStringOrEmpty(qObject, "type");
                        if (type.isEmpty() || !Arrays.asList(Constants.QUESTION_TYPES_NO_ALL).contains(type)) {
                            logger.warn("Type từ AI không hợp lệ ('{}') hoặc rỗng, đặt là mặc định '{}' cho câu hỏi: {}", type, Constants.DEFAULT_QUESTION_TYPE, question.getQuestionText().substring(0, Math.min(question.getQuestionText().length(), 30)));
                            type = Constants.DEFAULT_QUESTION_TYPE;
                        }
                        question.setType(type);
                        
                        if (!question.getQuestionText().isEmpty() && (!question.getOptionA().isEmpty() || !question.getOptionB().isEmpty())) { // Chỉ cần A hoặc B
                           extractedQuestions.add(question);
                        } else {
                            logger.warn("Câu hỏi trích xuất từ AI không đủ thông tin tối thiểu (question_text, và ít nhất option_a hoặc option_b): {}", qObject.toString().substring(0, Math.min(qObject.toString().length(), 100)));
                        }
                    }
                }
                logger.info("Đã phân tích và thêm {} câu hỏi hợp lệ vào danh sách.", extractedQuestions.size());
            } else {
                // Điều này không nên xảy ra nếu parseGeminiResponse đã trả về lỗi khi không phải là array
                logger.error("Phản hồi AI cho việc trích xuất nhiều câu hỏi không phải là một JSON array như mong đợi (sau khi parseGeminiResponse). Phản hồi: {}", rawJsonResponseString.substring(0, Math.min(rawJsonResponseString.length(), 500)));
            }
        } catch (JsonSyntaxException e) {
            logger.error("Lỗi cú pháp JSON khi phân tích phản hồi trích xuất nhiều câu hỏi: {}. Phản hồi (first 500 chars): '{}'", e.getMessage(), rawJsonResponseString.substring(0, Math.min(rawJsonResponseString.length(), 500)));
        } catch (Exception e) {
            logger.error("Lỗi không mong muốn khi phân tích phản hồi trích xuất nhiều câu hỏi: {}", e.getMessage(), e);
        }
        return extractedQuestions;
    }

    private String getStringOrEmpty(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() && obj.get(key).getAsJsonPrimitive().isString() && !obj.get(key).getAsString().equalsIgnoreCase("null")
               ? obj.get(key).getAsString()
               : "";
    }
}