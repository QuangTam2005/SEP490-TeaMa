package com.asc.studycompanion.module.timetable;

import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.timetable.controller.SubjectController;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubjectControllerTest {

    @Mock
    private SubjectRepository subjectRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SubjectController(subjectRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAllActiveSubjectsAndFiltersByGradeWhenRequested() throws Exception {
        List<Subject> seeded = List.of(
                Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").minGrade(1).maxGrade(12).isActive(true).build(),
                Subject.builder().id(4L).code(SubjectCode.SCI).name("Khoa học tự nhiên").minGrade(1).maxGrade(9).isActive(true).build(),
                Subject.builder().id(6L).code(SubjectCode.PHY).name("Vật lý").minGrade(10).maxGrade(12).isActive(true).build(),
                Subject.builder().id(11L).code(SubjectCode.INFO).name("Tin học").minGrade(3).maxGrade(12).isActive(true).build(),
                Subject.builder().id(13L).code(SubjectCode.TECH).name("Công nghệ").minGrade(3).maxGrade(12).isActive(true).build()
        );
        when(subjectRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(seeded);

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].code").value("MATH"))
                .andExpect(jsonPath("$.data[0].minGrade").value(1))
                .andExpect(jsonPath("$.data[0].maxGrade").value(12))
                .andExpect(jsonPath("$.data[2].code").value("PHY"))
                .andExpect(jsonPath("$.data[4].code").value("TECH"));

        mockMvc.perform(get("/api/subjects").param("grade", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[1].code").value("SCI"))
                .andExpect(jsonPath("$.data[3].code").value("TECH"));

        mockMvc.perform(get("/api/subjects").param("grade", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[1].code").value("PHY"))
                .andExpect(jsonPath("$.data[3].code").value("TECH"));

        mockMvc.perform(get("/api/subjects").param("grade", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"));
    }
}
