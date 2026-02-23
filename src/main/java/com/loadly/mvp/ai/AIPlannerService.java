package com.loadly.mvp.ai;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.loadly.mvp.engine.FreeTimeCalculator;
import com.loadly.mvp.engine.FreeWindow;
import com.loadly.mvp.engine.WeeklyMetrics;
import com.loadly.mvp.engine.WorkloadEngine;
import com.loadly.mvp.model.CalendarEvent;
import com.loadly.mvp.service.EventService;
import org.springframework.web.reactive.function.client.WebClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AIPlannerService {

    private final EventService eventService;
    private final WorkloadEngine workloadEngine;
    private final FreeTimeCalculator freeTimeCalculator;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public AIPlannerService(EventService eventService,
            WorkloadEngine workloadEngine,
            FreeTimeCalculator freeTimeCalculator,
            ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.workloadEngine = workloadEngine;
        this.freeTimeCalculator = freeTimeCalculator;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public AIWeeklyPlan generatWeeklyPlan(int userId, LocalDateTime weekStart, LocalDateTime weekEnd) {

        // 1. Fetch user's calendar events for the week
        List<CalendarEvent> events = eventService.getEventsForWeek(userId, weekStart, weekEnd);

        // 2. Compute weekly metrics
        WeeklyMetrics metrics = workloadEngine.computeWeeklyMetrics(events);

        List<FreeWindow> freeWindows = freeTimeCalculator.calculateFreeWindows(events, weekStart);

        // 3) Build prompt
        String prompt = buildPrompt(userId, weekStart, weekEnd, metrics, events, freeWindows);

        // 4) Call AI API
        try {
            String content = callOpenAi(prompt);
            AIWeeklyPlan plan = parsePlanJson(content);
            // basic safety: never return null lists
            if (plan.getWarnings() == null)
                plan.setWarnings(List.of());
            if (plan.getStudyBlocks() == null)
                plan.setStudyBlocks(List.of());
            if (plan.getTips() == null)
                plan.setTips(List.of());
            return plan;
        } catch (Exception e) {
            // In case of any error, return something usable
            return fallbackPlan(metrics);
        }

    }

    private String buildPrompt(int userId, LocalDateTime weekStart, LocalDateTime weekEnd, WeeklyMetrics metrics,
            List<CalendarEvent> events, List<FreeWindow> freeWindows) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an academic productivity planner.\n");
        sb.append("Return ONLY valid JSON with this exact shape:\n");
        sb.append("{\n");
        sb.append("  \"warnings\": [\"...\"],\n");
        sb.append(
                "  \"studyBlocks\": [{\"day\":\"MONDAY\",\"startTime\":\"18:00\",\"durationHours\":2.0,\"description\":\"...\"}],\n");
        sb.append("  \"tips\": [\"...\"]\n");
        sb.append("}\n\n");

        sb.append("UserId: ").append(userId).append("\n");
        sb.append("Week: ").append(weekStart).append(" to ").append(weekEnd).append("\n\n");

        sb.append("Weekly Metrics:\n");
        sb.append("- workloadScore: ").append(metrics.getWorkloadScore()).append("\n");
        sb.append("- freeHours: ").append(metrics.getFreeHours()).append("\n");
        sb.append("- busiestDay: ").append(metrics.getBusiestDay()).append("\n");
        sb.append("- pressureLevel: ").append(metrics.getPressureLevel()).append("\n\n");

        sb.append("\nAvailable Free Time Windows (YOU MUST schedule studyBlocks ONLY inside these):\n");
        for (FreeWindow w : freeWindows) {
            sb.append("- ")
                    .append(w.getDay())
                    .append(" ")
                    .append(w.getStartTime())
                    .append("–")
                    .append(w.getEndTime())
                    .append("\n");
        }

        sb.append("Events (fixed commitments + assignments):\n");

        if (events.isEmpty()) {
            sb.append("- (no events)\n");
        } else {
            for (CalendarEvent e : events) {
                sb.append("- ")
                        .append(e.getTitle())
                        .append(" (")
                        .append(e.getType())
                        .append(") ")
                        .append(e.getStartTime())
                        .append(" to ")
                        .append(e.getEndTime());

                if (e.getType() != null && e.getType().name().equals("ASSIGNMENT")) {
                    sb.append(" | effortHours=").append(e.getEffortHours());
                }
                sb.append("\n");
            }
        }

        sb.append("\nHard Rules:\n");
        sb.append("- Return ONLY valid JSON (no markdown, no explanations).\n");
        sb.append("- Schedule studyBlocks ONLY inside the provided free windows.\n");
        sb.append("- Do NOT create blocks outside free windows.\n");
        sb.append("- Do NOT overlap blocks.\n");
        sb.append("- Each block duration must fit entirely within its window.\n");
        sb.append("- Prefer blocks of 1 to 2 hours.\n");
        sb.append("- Create 5 to 10 blocks total.\n");

        return sb.toString();
    }

    private String callOpenAi(String prompt) {

        // Minimal Chat Completions request body
        String requestJson = """
                {
                  "model": "%s",
                  "messages": [
                    { "role": "system", "content": "You output strictly valid JSON and nothing else." },
                    { "role": "user", "content": %s }
                  ],
                  "temperature": 0.2
                }
                """.formatted(model, objectMapper.valueToTree(prompt).toString());

        String rawResponse = webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + openAiApiKey)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new RuntimeException("Empty OpenAI response");
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response wrapper", e);
        }
    }

    private AIWeeklyPlan parsePlanJson(String content) {
        // Sometimes models wrap JSON in ```json ... ```
        String cleaned = content.trim();
        cleaned = cleaned.replaceAll("^```json\\s*", "");
        cleaned = cleaned.replaceAll("^```\\s*", "");
        cleaned = cleaned.replaceAll("\\s*```$", "");

        try {
            return objectMapper.readValue(cleaned, AIWeeklyPlan.class);
        } catch (Exception e) {
            throw new RuntimeException("Model did not return valid AIWeeklyPlan JSON. Content was: " + cleaned, e);
        }
    }

    private AIWeeklyPlan fallbackPlan(WeeklyMetrics metrics) {
        AIWeeklyPlan plan = new AIWeeklyPlan();

        // warnings
        if ("HIGH".equalsIgnoreCase(metrics.getPressureLevel())) {
            plan.getWarnings().add("High workload week detected — start assignments early and protect focus time.");
            plan.getWarnings()
                    .add("Your busiest day is " + metrics.getBusiestDay() + ". Avoid adding extra commitments there.");
        } else if ("MEDIUM".equalsIgnoreCase(metrics.getPressureLevel())) {
            plan.getWarnings().add("Moderate workload week — plan consistent daily study blocks.");
        } else {
            plan.getWarnings().add("Light workload week — use the extra time to get ahead or rest.");
        }

        // simple study blocks (generic)
        plan.getStudyBlocks()
                .add(new StudyBlock("MONDAY", "18:00", 1.5, "Review week tasks and start highest-effort assignment"));
        plan.getStudyBlocks().add(new StudyBlock("WEDNESDAY", "18:00", 1.5, "Deep work: assignments / project"));
        plan.getStudyBlocks().add(new StudyBlock("SATURDAY", "11:00", 2.0, "Catch-up + finalize pending tasks"));

        // tips
        plan.getTips().add("Do 1 hard task first (60–90 minutes) before switching contexts.");
        plan.getTips().add("Keep your busiest day lighter: meals + short breaks + no extra meetings.");

        return plan;
    }
}
