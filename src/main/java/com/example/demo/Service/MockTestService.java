package com.example.demo.Service;

import com.example.demo.dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockTestService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ignisflow.mocktest-url}")
    private String mockTestUrl;

    @Value("${ignisflow.api.key}")
    private String apiKey;

    // ─── START ────────────────────────────────────────────────────────────────
    public MockTestResponse startMockTest(StartMockTestRequest request) {
        log.info("━━━ [START] role='{}' experience='{}' level='{}' type='{}' difficulty='{}'",
                request.getRole(), request.getExperienceLevel(),
                request.getCurrentLevel(), request.getAssessmentType(),
                request.getDifficultyPreference());

        String prompt = buildStartPrompt(request);
        log.debug("[START] Prompt:\n{}", prompt);

        log.info("[START] Calling IgnisFlow...");
        String raw = callIgnisFlow(prompt);
        log.info("[START] Raw response received ({} chars)", raw != null ? raw.length() : 0);
        log.debug("[START] Raw:\n{}", raw);

        MockTestResponse response = parseMockTestResponse(raw, request);
        log.info("[START] ✓ testId='{}' questions={}",
                response.getTestId(),
                response.getQuestions() != null ? response.getQuestions().size() : 0);
        return response;
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────
    public MockTestSubmitResponse submitMockTest(SubmitMockTestRequest request) {
        log.info("━━━ [SUBMIT] testId='{}' role='{}' answers={}",
                request.getTestId(), request.getRole(),
                request.getAnswers() != null ? request.getAnswers().size() : 0);

        String prompt = buildSubmitPrompt(request);
        log.debug("[SUBMIT] Prompt ({} chars)", prompt.length());

        log.info("[SUBMIT] Calling IgnisFlow for evaluation...");
        String raw = callIgnisFlow(prompt);
        log.info("[SUBMIT] Raw response received ({} chars)", raw != null ? raw.length() : 0);

        MockTestSubmitResponse response = parseSubmitResponse(raw, request);
        log.info("[SUBMIT] ✓ detectedLevel='{}' overallScore='{}' readiness='{}'",
                response.getDetectedLevel(), response.getOverallScore(),
                response.getInterviewReadiness());
        return response;
    }

    // ─── CALL IGNISFLOW ───────────────────────────────────────────────────────
    private String callIgnisFlow(String prompt) {
        log.info("[IgnisFlow] Preparing request...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        String sessionId = UUID.randomUUID().toString();
        Map<String, Object> body = new HashMap<>();
        body.put("input_value", prompt);
        body.put("output_type", "chat");
        body.put("input_type", "chat");
        body.put("session_id", sessionId);

        log.info("[IgnisFlow] session_id={} url={}", sessionId, mockTestUrl);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            long start = System.currentTimeMillis();
            ResponseEntity<String> response =
                    restTemplate.postForEntity(mockTestUrl, entity, String.class);
            long elapsed = System.currentTimeMillis() - start;

            log.info("[IgnisFlow] ✓ status={} time={}ms", response.getStatusCode(), elapsed);
            log.debug("[IgnisFlow] Raw body:\n{}", response.getBody());

            if (response.getBody() == null || response.getBody().isBlank()) {
                log.error("[IgnisFlow] ✗ Empty response body");
                throw new RuntimeException("Empty response from IgnisFlow");
            }

            JsonNode root     = objectMapper.readTree(response.getBody());
            JsonNode textNode = root.path("outputs").path(0)
                    .path("outputs").path(0)
                    .path("results").path("message").path("text");

            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                log.error("[IgnisFlow] ✗ Could not find text node. Structure:\n{}",
                        root.toPrettyString());
                throw new RuntimeException("Could not extract text from IgnisFlow response");
            }

            String text = textNode.asText();
            log.info("[IgnisFlow] ✓ Text extracted ({} chars)", text.length());
            return text;

        } catch (Exception e) {
            log.error("[IgnisFlow] ✗ Call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI service failed: " + e.getMessage());
        }
    }

    // ─── BUILD START PROMPT ───────────────────────────────────────────────────
    private String buildStartPrompt(StartMockTestRequest req) {
        String type = Optional.ofNullable(req.getAssessmentType()).orElse("mixed");

        // Determine question counts based on assessment type
        int mcqCount = switch (type) {
            case "quiz"    -> 25;   // MCQ only
            case "writing" -> 0;    // Writing only
            default        -> 20;   // mixed: 20 MCQ + 5 writing
        };
        int writingCount = switch (type) {
            case "quiz"    -> 0;    // MCQ only
            case "writing" -> 25;   // Writing only
            default        -> 5;    // mixed
        };
        int totalPoints = (mcqCount * 3) + (writingCount * 8);

        log.info("[Prompt] assessmentType='{}' → mcqCount={} writingCount={} totalPoints={}",
                type, mcqCount, writingCount, totalPoints);

        return String.format("""
        {
          "role": "%s",
          "experience": "%s",
          "current_level": "%s",
          "goal": "%s",
          "assessment_type": "%s",
          "mcq_count": %d,
          "writing_count": %d,
          "total_points": %d,
          "difficulty_preference": "%s",
          "focus_topics": "%s"
        }
        """,
                req.getRole(),
                Optional.ofNullable(req.getExperienceLevel()).orElse("Not specified"),
                Optional.ofNullable(req.getCurrentLevel()).orElse("Intermediate"),
                Optional.ofNullable(req.getGoal()).orElse("Grow professionally"),
                type,
                mcqCount,
                writingCount,
                totalPoints,
                Optional.ofNullable(req.getDifficultyPreference()).orElse("medium"),
                Optional.ofNullable(req.getFocusTopics()).orElse("General")
        );
    }

    // ─── BUILD SUBMIT PROMPT ──────────────────────────────────────────────────
    private String buildSubmitPrompt(SubmitMockTestRequest req) {
        log.debug("[Prompt] Building submit prompt | questions={} answers={}",
                req.getQuestions() != null ? req.getQuestions().size() : 0,
                req.getAnswers() != null ? req.getAnswers().size() : 0);

        String questionsJson;
        try {
            questionsJson = objectMapper.writeValueAsString(req.getQuestions());
        } catch (Exception e) {
            log.warn("[Prompt] Failed to serialize questions: {}", e.getMessage());
            questionsJson = "[]";
        }

        StringBuilder answersBlock = new StringBuilder("[\n");
        List<UserAnswer> answers = req.getAnswers();
        for (int i = 0; i < answers.size(); i++) {
            UserAnswer ua = answers.get(i);
            String escapedAnswer = ua.getAnswer() != null
                    ? ua.getAnswer().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                    : "";
            answersBlock.append(String.format(
                    "    { \"id\": %d, \"type\": \"%s\", \"answer\": \"%s\" }%s\n",
                    ua.getId(),
                    ua.getType() != null ? ua.getType() : "mcq",
                    escapedAnswer,
                    i < answers.size() - 1 ? "," : ""
            ));
            log.debug("[Prompt] Answer[{}] id={} type={} len={}",
                    i, ua.getId(), ua.getType(),
                    ua.getAnswer() != null ? ua.getAnswer().length() : 0);
        }
        answersBlock.append("  ]");

        return String.format("""
            You are an AI Technical Assessment Engine.
            
            Candidate profile:
            - Role: %s
            - Experience: %s
            - Stated Level: %s
            - Goal: %s
            
            Original questions (includes correct_answer for MCQs and expectedKeyPoints for writing):
            %s
            
            Candidate's submitted answers:
            {
              "answers": %s
            }
            
            Execute Phase 4 — Evaluate all answers and generate the final report.
            
            ── EVALUATION RULES ────────────────────────────────────────────────
            For MCQ questions (3 points each):
            - Award 3 pts if answer matches correct_answer exactly. 0 for wrong/skipped.
            
            For Writing questions (8 points each):
            - Accuracy & Relevance  (3 pts)
            - Key Points Coverage   (2 pts): how many expectedKeyPoints are addressed
            - Clarity & Explanation (2 pts)
            - Depth of Knowledge    (1 pt)
            - Blank/skipped → 0 pts, "Not Attempted"
            
            ── DETECTION RULES ─────────────────────────────────────────────────
            Score >= 80%%  → detected_level = "ADVANCED"
            Score 50-79%%  → detected_level = "INTERMEDIATE"
            Score < 50%%   → detected_level = "BEGINNER"
            
            ── OUTPUT FORMAT ───────────────────────────────────────────────────
            Return ONLY valid JSON, no markdown, no explanation:
            
            {
              "role": "",
              "experience": "",
              "stated_level": "",
              "detected_level": "",
              "overall_score": "",
              "mcq_score": "",
              "writing_score": "",
              "technical_depth_score": "",
              "problem_solving_score": "",
              "strengths": [],
              "weak_areas": [],
              "must_learn_next": [],
              "recommended_topics": [],
              "interview_readiness": "",
              "confidence_level": "",
              "final_verdict": ""
            }
            """,
                req.getRole(),
                Optional.ofNullable(req.getExperience()).orElse("Not specified"),
                Optional.ofNullable(req.getStatedLevel()).orElse("Intermediate"),
                Optional.ofNullable(req.getGoal()).orElse("Grow professionally"),
                questionsJson,
                answersBlock.toString()
        );
    }

    // ─── PARSE START RESPONSE ─────────────────────────────────────────────────
    private MockTestResponse parseMockTestResponse(String raw, StartMockTestRequest req) {
        log.info("[Parse:Start] Extracting JSON...");
        try {
            String   json = extractJson(raw);
            JsonNode root = objectMapper.readTree(json);

            List<MockQuestion> questions    = new ArrayList<>();
            JsonNode           questionsNode = root.path("questions");
            log.info("[Parse:Start] Found {} questions", questionsNode.size());

            if (questionsNode.isArray()) {
                for (JsonNode q : questionsNode) {
                    String type = q.path("type").asText("mcq");

                    MockQuestion.MockQuestionBuilder builder = MockQuestion.builder()
                            .id(q.path("id").asInt())
                            .type(type)
                            .difficulty(q.path("difficulty").asText("medium"))
                            .topic(q.path("topic").asText("General"))
                            .points(q.path("points").asInt("mcq".equals(type) ? 3 : 8));

                    if ("mcq".equals(type)) {
                        builder.question(q.path("question").asText());
                        builder.correctAnswer(q.path("correct_answer").asText());
                        builder.options(jsonArrayToList(q.path("options")));
                        log.debug("[Parse:Start] MCQ id={} topic='{}' difficulty='{}'",
                                q.path("id").asInt(), q.path("topic").asText(),
                                q.path("difficulty").asText());
                    } else {
                        builder.question(q.path("question").asText());
                        builder.expectedKeyPoints(jsonArrayToList(q.path("expectedKeyPoints")));
                        log.debug("[Parse:Start] Writing id={} topic='{}' keyPoints={}",
                                q.path("id").asInt(), q.path("topic").asText(),
                                q.path("expectedKeyPoints").size());
                    }
                    questions.add(builder.build());
                }
            }

            long mcqCount     = questions.stream().filter(q -> "mcq".equals(q.getType())).count();
            long writingCount = questions.stream().filter(q -> "writing".equals(q.getType())).count();
            log.info("[Parse:Start] Breakdown → MCQ={} Writing={}", mcqCount, writingCount);

            JsonNode info   = root.path("assessment_info");
            String   testId = UUID.randomUUID().toString();
            log.info("[Parse:Start] ✓ testId='{}'", testId);

            return MockTestResponse.builder()
                    .testId(testId)
                    .timeMinutes(root.path("time_minutes").asInt(60))
                    .totalPoints(root.path("total_points").asInt(100))
                    .assessmentInfo(MockTestResponse.AssessmentInfo.builder()
                            .role(info.path("role").asText(req.getRole()))
                            .experience(info.path("experience").asText(req.getExperienceLevel()))
                            .level(info.path("level").asText(req.getCurrentLevel()))
                            .goal(info.path("goal").asText(req.getGoal()))
                            .build())
                    .questions(questions)
                    .build();

        } catch (Exception e) {
            log.error("[Parse:Start] ✗ {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse test generation response: " + e.getMessage());
        }
    }

    // ─── PARSE SUBMIT RESPONSE ────────────────────────────────────────────────
    private MockTestSubmitResponse parseSubmitResponse(String raw, SubmitMockTestRequest req) {
        log.info("[Parse:Submit] Extracting JSON...");
        try {
            String   json = extractJson(raw);
            JsonNode root = objectMapper.readTree(json);

            String detectedLevel  = root.path("detected_level").asText("INTERMEDIATE").toUpperCase();
            String overallScore   = root.path("overall_score").asText("0/100");
            String readiness      = root.path("interview_readiness").asText("Partially Ready");
            String confidence     = root.path("confidence_level").asText("Medium");

            log.info("[Parse:Submit] ✓ level='{}' score='{}' readiness='{}' confidence='{}'",
                    detectedLevel, overallScore, readiness, confidence);
            log.info("[Parse:Submit] strengths={} weakAreas={} mustLearn={}",
                    root.path("strengths").size(),
                    root.path("weak_areas").size(),
                    root.path("must_learn_next").size());

            return MockTestSubmitResponse.builder()
                    .role(root.path("role").asText(req.getRole()))
                    .experience(root.path("experience").asText(req.getExperience()))
                    .statedLevel(root.path("stated_level").asText(req.getStatedLevel()))
                    .detectedLevel(detectedLevel)
                    .overallScore(overallScore)
                    .mcqScore(root.path("mcq_score").asText("0/60"))
                    .writingScore(root.path("writing_score").asText("0/40"))
                    .technicalDepthScore(root.path("technical_depth_score").asText("0/10"))
                    .problemSolvingScore(root.path("problem_solving_score").asText("0/10"))
                    .strengths(jsonArrayToList(root.path("strengths")))
                    .weakAreas(jsonArrayToList(root.path("weak_areas")))
                    .mustLearnNext(jsonArrayToList(root.path("must_learn_next")))
                    .recommendedTopics(jsonArrayToList(root.path("recommended_topics")))
                    .interviewReadiness(readiness)
                    .confidenceLevel(confidence)
                    .finalVerdict(root.path("final_verdict").asText(""))
                    .build();

        } catch (Exception e) {
            log.error("[Parse:Submit] ✗ {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse evaluation response: " + e.getMessage());
        }
    }

    // ─── UTILS ────────────────────────────────────────────────────────────────
    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            log.error("[extractJson] Input is null or blank");
            throw new RuntimeException("Empty AI response");
        }
        raw = raw.replace("```json", "").replace("```", "").trim();
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            log.error("[extractJson] No valid JSON found. Preview: {}",
                    raw.substring(0, Math.min(300, raw.length())));
            throw new RuntimeException("No JSON found in AI response");
        }
        log.debug("[extractJson] Extracted JSON [{} → {}]", start, end);
        return raw.substring(start, end + 1);
    }

    private List<String> jsonArrayToList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray())
            node.forEach(n -> list.add(n.asText()));
        return list;
    }
}