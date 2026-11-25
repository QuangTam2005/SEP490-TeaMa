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
    void queryTargetsKnttTextbookAndWikipediaAndCapsResultsToThree() {
        String longDesc = "Kiến thức trọng tâm ".repeat(40);
        String mockJson = """
                {
                  "web": {
                    "results": [
                      { "title": "Blog chung", "url": "https://example.com/1", "description": "Mô tả 1" },
                      { "title": "Wikipedia - Ước chung lớn nhất", "url": "https://vi.wikipedia.org/wiki/UCLN", "description": "Định nghĩa Wikipedia" },
                      { "title": "Toán 7 Kết nối tri thức - Bài 11", "url": "https://loigiaihay.com/ket-noi-tri-thuc-bai-11", "description": "%s" },
                      { "title": "Trang 4", "url": "https://example.com/4", "description": "Mô tả 4" }
                    ]
                  }
                }
                """.formatted(longDesc);

        server.expect(requestTo(containsString("api.search.brave.com")))
                .andExpect(requestTo(containsString("country=ALL")))
                .andExpect(requestTo(containsString("search_lang=vi")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Subscription-Token", "test-brave-key"))
                .andRespond(withSuccess(mockJson, MediaType.APPLICATION_JSON));

        SearchQuery query = new SearchQuery(7, SubjectCode.MATH, "Toán", "Ước chung lớn nhất");
        String q = adapter.buildKnttWikipediaQuery(query);
        assertTrue(q.contains("Lớp 7"));
        assertTrue(q.contains("Kết nối tri thức với cuộc sống"));
        assertTrue(q.contains("vi.wikipedia.org"));

        List<SearchSnippet> results = adapter.search(query);
        server.verify();

        assertEquals(3, results.size());
        assertTrue(results.get(0).title().contains("Kết nối tri thức"));
        assertTrue(results.get(1).url().contains("wikipedia.org"));
        assertTrue(results.get(0).snippet().length() <= 350);
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
}
