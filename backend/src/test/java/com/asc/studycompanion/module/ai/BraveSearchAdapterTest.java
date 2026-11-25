package com.asc.studycompanion.module.ai;

import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.module.ai.config.AiProperties;
import com.asc.studycompanion.module.ai.provider.BraveSearchAdapter;
import com.asc.studycompanion.module.ai.provider.SearchQuery;
import com.asc.studycompanion.module.ai.provider.SearchSnippet;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BraveSearchAdapterTest {

    private MockRestServiceServer server;
    private BraveSearchAdapter adapter;
    private AiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setMaxSources(3);
        properties.getBrave().setBaseUrl("https://api.search.brave.com/res/v1/web/search");
        properties.getBrave().setApiKey("test-brave-key");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BraveSearchAdapter(builder.build(), properties);
    }

    @Test
    void searchesLessonWebsitesAndWikipediaAndCapsRequestsAndSourcesToThree() {
        String longDesc = "Kiến thức trọng tâm ".repeat(40);
        String webJson = """
                {
                  "web": {
                    "results": [
                      { "title": "Blog chung", "url": "https://example.com/1", "description": "Mô tả 1" },
                      { "title": "Toán 7 Kết nối tri thức - Bài 11", "url": "https://loigiaihay.com/ket-noi-tri-thuc-bai-11", "description": "%s" },
                      { "title": "VietJack Toán 7 - Ước chung lớn nhất", "url": "https://vietjack.com/toan-7/ucln", "description": "Lý thuyết trọng tâm" }
                    ]
                  }
                }
                """.formatted(longDesc);

        String wikiJson = """
                {
                  "web": {
                    "results": [
                      { "title": "Wikipedia - Ước chung lớn nhất", "url": "https://vi.wikipedia.org/wiki/UCLN", "description": "Định nghĩa Wikipedia chuẩn xác" }
                    ]
                  }
                }
                """;

        // Request 1: educational lesson websites
        server.expect(requestTo(containsString("api.search.brave.com")))
                .andExpect(requestTo(containsString("country=ALL")))
                .andExpect(requestTo(containsString("search_lang=vi")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Subscription-Token", "test-brave-key"))
                .andRespond(withSuccess(webJson, MediaType.APPLICATION_JSON));

        // Request 2: Wikipedia directly for the lesson title (Request 3 skipped since 2+1 = 3 sources collected)
        server.expect(requestTo(containsString("vi.wikipedia.org")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(wikiJson, MediaType.APPLICATION_JSON));

        SearchQuery query = new SearchQuery(
                7,
                SubjectCode.MATH,
                "Toán",
                "Bài 11: Ước chung lớn nhất",
                "Ước chung lớn nhất của hai hay nhiều số là số lớn nhất trong tập hợp các ước chung của các số đó."
        );
        assertTrue(adapter.buildLessonWebQuery(query).contains("Lớp 7 Toán Bài 11: Ước chung lớn nhất"));
        assertEquals("site:vi.wikipedia.org Ước chung lớn nhất", adapter.buildWikipediaQuery(query));

        List<SearchSnippet> results = adapter.search(query);
        server.verify();

        assertEquals(2, adapter.consumeLastRequestCount());
        assertEquals(3, results.size());
        assertTrue(results.get(0).title().contains("Kết nối tri thức"));
        assertTrue(results.get(2).url().contains("vi.wikipedia.org"));
        assertTrue(results.get(0).snippet().length() <= 350);
    }

    @Test
    void usesSubmissionMainContentAsThirdFallbackRequestWhenPrimarySearchNeedsMoreSources() {
        String emptyJson = "{\"web\":{\"results\":[]}}";
        String contentJson = """
                {
                  "web": {
                    "results": [
                      { "title": "Số hữu tỉ trên Wikipedia", "url": "https://vi.wikipedia.org/wiki/So_huu_ti", "description": "Số viết dưới dạng a/b với b khác 0" }
                    ]
                  }
                }
                """;

        server.expect(requestTo(containsString("api.search.brave.com")))
                .andRespond(withSuccess(emptyJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("vi.wikipedia.org")))
                .andRespond(withSuccess(emptyJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("api.search.brave.com")))
                .andRespond(withSuccess(contentJson, MediaType.APPLICATION_JSON));

        SearchQuery query = new SearchQuery(
                7,
                SubjectCode.MATH,
                "Toán",
                "Bài tập tự chọn",
                "Số hữu tỉ là số được viết dưới dạng phân số a trên b với a, b thuộc tập số nguyên và b khác 0."
        );
        List<SearchSnippet> results = adapter.search(query);
        server.verify();

        assertEquals(3, adapter.consumeLastRequestCount());
        assertEquals(1, results.size());
        assertTrue(results.get(0).url().contains("vi.wikipedia.org"));
    }

    @Test
    void mapsHttp429OrMissingKeyToProviderUnavailable() {
        server.expect(requestTo(containsString("api.search.brave.com")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AppException rateEx = assertThrows(
                AppException.class,
                () -> adapter.search(new SearchQuery(6, SubjectCode.SCI, "KHTN", "Các phép đo"))
        );
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, rateEx.getErrorCode());

        properties.getBrave().setApiKey("");
        AppException missingKeyEx = assertThrows(
                AppException.class,
                () -> adapter.search(new SearchQuery(6, SubjectCode.SCI, "KHTN", "Các phép đo"))
        );
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, missingKeyEx.getErrorCode());
    }

    @Test
    void buildsSubjectSpecificQueriesForEnglishAndTechnology() {
        SearchQuery english = new SearchQuery(7, SubjectCode.ENG, "Tiếng Anh", "Unit 3: Teenagers Present Simple");
        assertTrue(adapter.buildLessonWebQuery(english).contains("Global Success"));
        assertEquals("site:vi.wikipedia.org Teenagers Present Simple", adapter.buildWikipediaQuery(english));

        SearchQuery tech = new SearchQuery(8, SubjectCode.TECH, "Công nghệ", "Bài 4: Vật liệu cơ khí");
        assertTrue(adapter.buildLessonWebQuery(tech).contains("Lớp 8 Công nghệ Bài 4: Vật liệu cơ khí Kết nối tri thức"));
        assertEquals("site:vi.wikipedia.org Vật liệu cơ khí", adapter.buildWikipediaQuery(tech));
    }
}
