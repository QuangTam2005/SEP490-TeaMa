package com.asc.studycompanion.module.ai;

import com.asc.studycompanion.module.ai.entity.KnowledgeCache;
import com.asc.studycompanion.module.ai.provider.SearchSnippet;
import com.asc.studycompanion.module.ai.repository.KnowledgeCacheRepository;
import com.asc.studycompanion.module.ai.service.KnowledgeCacheService;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeCacheServiceTest {

    @Mock
    private KnowledgeCacheRepository repository;

    private KnowledgeCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new KnowledgeCacheService(repository);
    }

    @Test
    void lookupHitsValidCacheAndRejectsExpiredOrDisallowedEntries() {
        String expectedKey = cacheService.buildCacheKey(7, SubjectCode.MATH, "  Ước   Chung Lớn Nhất ");
        assertEquals("v2:7:MATH:ước chung lớn nhất", expectedKey);

        SearchSnippet snippet = new SearchSnippet("KNTT", "https://loigiaihay.com", "Nội dung", Instant.now());
        KnowledgeCache valid = KnowledgeCache.builder()
                .cacheKey(expectedKey)
                .cacheAllowed(true)
                .expiresAt(Instant.now().plus(2, ChronoUnit.DAYS))
                .sourcesJson(List.of(snippet))
                .build();
        when(repository.findByCacheKey(expectedKey)).thenReturn(Optional.of(valid));

        Optional<List<SearchSnippet>> hit = cacheService.lookup(7, SubjectCode.MATH, "Ước chung lớn nhất");
        assertTrue(hit.isPresent());
        assertEquals(1, hit.get().size());

        valid.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        assertTrue(cacheService.lookup(7, SubjectCode.MATH, "Ước chung lớn nhất").isEmpty());

        valid.setExpiresAt(Instant.now().plus(2, ChronoUnit.DAYS));
        valid.setCacheAllowed(false);
        assertTrue(cacheService.lookup(7, SubjectCode.MATH, "Ước chung lớn nhất").isEmpty());
    }

    @Test
    void storeRespectsCacheAllowedFlagAndSavesWithTtl() {
        SearchSnippet snippet = new SearchSnippet("Wikipedia", "https://vi.wikipedia.org", "Định nghĩa", Instant.now());

        cacheService.store(8, SubjectCode.SCI, "Áp suất", List.of(snippet), false);
        verify(repository, never()).save(any());

        when(repository.findByCacheKey("v2:8:SCI:áp suất")).thenReturn(Optional.empty());
        cacheService.store(8, SubjectCode.SCI, "Áp suất", List.of(snippet), true);

        ArgumentCaptor<KnowledgeCache> captor = ArgumentCaptor.forClass(KnowledgeCache.class);
        verify(repository).save(captor.capture());
        assertEquals(8, captor.getValue().getGrade());
        assertTrue(captor.getValue().getExpiresAt().isAfter(Instant.now().plus(6, ChronoUnit.DAYS)));
    }
}
