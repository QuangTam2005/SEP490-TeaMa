package com.asc.studycompanion.module.study.util;

/**
 * Shared text normalization and BR-12 length validation matching frontend study-utils.ts 1-to-1.
 */
public final class StudyTextUtils {

    public static final int MIN_SUBMISSION_CHARS = 850;
    public static final int MAX_SUBMISSION_CHARS = 2000;
    public static final int MIN_FEEDBACK_CHARS = 300;
    public static final int MAX_FEEDBACK_CHARS = 600;

    private StudyTextUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    public static int countNormalizedChars(String raw) {
        return normalize(raw).length();
    }

    public static boolean isValidLength(String raw) {
        int count = countNormalizedChars(raw);
        return count >= MIN_SUBMISSION_CHARS && count <= MAX_SUBMISSION_CHARS;
    }
}
