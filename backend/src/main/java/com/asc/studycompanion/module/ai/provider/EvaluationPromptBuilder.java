package com.asc.studycompanion.module.ai.provider;

import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.study.entity.StudyTaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds deterministic, PII-free Vietnamese evaluation prompts constrained to ~2,048 context tokens.
 */
@Component
@RequiredArgsConstructor
public class EvaluationPromptBuilder {

    private static final int CHARS_PER_TOKEN_ESTIMATE = 3;

    private final AiProperties aiProperties;

    public String buildSystemPrompt() {
        return """
                Bạn là trợ lý học tập đồng hành cùng học sinh Việt Nam.
                Chỉ sử dụng tài liệu tham khảo được cung cấp (từ Wikipedia và các website bài học / SGK Kết nối tri thức với cuộc sống) để kiểm tra bài làm.
                TUYỆT ĐỐI không bịa kiến thức hoặc URL ngoài tài liệu; nếu nguồn thiếu hoặc không đủ đối chiếu, hãy đặt accuracy="UNVERIFIABLE", completeness="UNDETERMINED", overallStatus="INSUFFICIENT_EVIDENCE".
                Nhận xét bằng tiếng Việt tích cực, rõ ràng, chi tiết, có độ dài BẮT BUỘC từ 300 đến 600 ký tự (tính cả dấu câu và khoảng trắng), không viết ngắn dưới 300 ký tự và không làm bài hộ học sinh.
                Trả về DUY NHẤT một đối tượng JSON hợp lệ (không markdown) theo schema:
                {"accuracy":"CORRECT|PARTIALLY_CORRECT|INCORRECT|UNVERIFIABLE","completeness":"COMPLETE|INCOMPLETE|UNDETERMINED","overallStatus":"GOOD|NEEDS_IMPROVEMENT|INCORRECT|INSUFFICIENT_EVIDENCE","feedback":"string 300..600 chars","incorrectPoints":["string"],"missingPoints":["string"],"confidence":"HIGH|MEDIUM|LOW"}
                """.strip();
    }

    public String buildUserPrompt(AiEvaluationInput input) {
        String taskLabel = input.taskType() == StudyTaskType.REVIEW ? "Ôn bài cũ (Review)" : "Soạn bài mới (Preview)";
        int maxTotalChars = Math.max(2400, aiProperties.getMaxContextTokens() * CHARS_PER_TOKEN_ESTIMATE);

        StringBuilder sourcesSection = new StringBuilder();
        List<SearchSnippet> sources = input.sources() != null ? input.sources() : List.of();
        if (sources.isEmpty()) {
            sourcesSection.append("Không tìm thấy nguồn đối chiếu phù hợp.\n");
        } else {
            for (int i = 0; i < sources.size(); i++) {
                SearchSnippet s = sources.get(i);
                String block = String.format(
                        "[%d] %s (%s): %s%n",
                        i + 1,
                        safe(s.title()),
                        safe(s.url()),
                        safe(s.snippet())
                );
                if (sourcesSection.length() + block.length() > maxTotalChars / 2) {
                    break;
                }
                sourcesSection.append(block);
            }
        }

        String header = String.format(
                "Khối lớp: Lớp %d%nMôn học: %s (%s)%nLoại nhiệm vụ: %s%nTên bài học: %s%n%nNguồn đối chiếu:%n%s%nBài làm của học sinh:%n",
                input.grade(),
                safe(input.subjectName()),
                input.subjectCode().name(),
                taskLabel,
                safe(input.lessonTitle()),
                sourcesSection
        );

        int remainingBudget = Math.max(800, maxTotalChars - header.length());
        String studentText = safe(input.studentText());
        if (studentText.length() > remainingBudget) {
            studentText = studentText.substring(0, remainingBudget);
        }

        return header + studentText;
    }

    private static String safe(String val) {
        return val == null ? "" : val.trim();
    }
}
