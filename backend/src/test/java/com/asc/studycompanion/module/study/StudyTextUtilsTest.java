package com.asc.studycompanion.module.study;

import com.asc.studycompanion.module.study.util.StudyTextUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyTextUtilsTest {

    @Test
    void normalizesCrlfAndTrimsLeadingTrailingWhitespace() {
        String raw = "   \r\nDòng 1\r\nDòng 2\r\n  ";
        assertEquals("Dòng 1\nDòng 2", StudyTextUtils.normalize(raw));
        assertEquals(13, StudyTextUtils.countNormalizedChars(raw));
    }

    @Test
    void enforcesStrictInclusiveBounds400To3000() {
        assertFalse(StudyTextUtils.isValidLength("a".repeat(399)));
        assertFalse(StudyTextUtils.isValidLength("  " + "a".repeat(399) + "\r\n"));
        assertTrue(StudyTextUtils.isValidLength("a".repeat(400)));
        assertTrue(StudyTextUtils.isValidLength("a".repeat(3000)));
        assertFalse(StudyTextUtils.isValidLength("a".repeat(3001)));
    }
}
