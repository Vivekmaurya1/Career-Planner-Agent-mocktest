package com.example.demo.Controller;

import com.example.demo.dtos.*;
import com.example.demo.Service.MockTestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/mocktest")
@RequiredArgsConstructor
public class MockTestController {

    private final MockTestService mockTestService;

    // ── Helper to resolve user from request attributes set by AuthForwardFilter ──
    private String resolveUser(HttpServletRequest request) {
        String email = (String) request.getAttribute("authenticatedUser");
        String name  = (String) request.getAttribute("authenticatedUserName");
        if (email != null) return name != null ? name + " (" + email + ")" : email;
        return "anonymous";
    }

    // ── POST /api/mocktest/start ───────────────────────────────────────────────
    @PostMapping("/start")
    public ResponseEntity<MockTestResponse> startMockTest(
            @RequestBody StartMockTestRequest request,
            HttpServletRequest httpRequest) {

        String user = resolveUser(httpRequest);

        log.info("══════════════════════════════════════════");
        log.info("► POST /api/mocktest/start");
        log.info("  user             = {}", user);
        log.info("  role             = {}", request.getRole());
        log.info("  experienceLevel  = {}", request.getExperienceLevel());
        log.info("  currentLevel     = {}", request.getCurrentLevel());
        log.info("  assessmentType   = {}", request.getAssessmentType());
        log.info("  difficulty       = {}", request.getDifficultyPreference());
        log.info("  focusTopics      = {}", request.getFocusTopics());
        log.info("  goal             = {}", request.getGoal());
        log.info("══════════════════════════════════════════");

        try {
            MockTestResponse response = mockTestService.startMockTest(request);
            log.info("✓ /start completed | testId='{}' questions={}",
                    response.getTestId(),
                    response.getQuestions() != null ? response.getQuestions().size() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("✗ /start failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ── POST /api/mocktest/submit ──────────────────────────────────────────────
    @PostMapping("/submit")
    public ResponseEntity<MockTestSubmitResponse> submitMockTest(
            @RequestBody SubmitMockTestRequest request,
            HttpServletRequest httpRequest) {

        String user       = resolveUser(httpRequest);
        int    answerCount = request.getAnswers() != null ? request.getAnswers().size() : 0;

        log.info("══════════════════════════════════════════");
        log.info("► POST /api/mocktest/submit");
        log.info("  user        = {}", user);
        log.info("  testId      = {}", request.getTestId());
        log.info("  role        = {}", request.getRole());
        log.info("  experience  = {}", request.getExperience());
        log.info("  statedLevel = {}", request.getStatedLevel());
        log.info("  questions   = {}", request.getQuestions() != null ? request.getQuestions().size() : 0);
        log.info("  answers     = {}", answerCount);
        log.info("══════════════════════════════════════════");

        if (request.getAnswers() != null) {
            request.getAnswers().forEach(a ->
                    log.debug("  answer → id={} type={} len={}",
                            a.getId(), a.getType(),
                            a.getAnswer() != null ? a.getAnswer().length() : 0)
            );
        }

        try {
            MockTestSubmitResponse response = mockTestService.submitMockTest(request);
            log.info("✓ /submit completed | detectedLevel='{}' overallScore='{}' readiness='{}'",
                    response.getDetectedLevel(),
                    response.getOverallScore(),
                    response.getInterviewReadiness());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("✗ /submit failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}