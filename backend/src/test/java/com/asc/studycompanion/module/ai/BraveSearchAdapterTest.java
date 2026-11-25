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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        properties.setMaxSources(2);
        properties.getBrave().setBaseUrl("https://api.search.brave.com/res/v1/web/search");
        properties.getBrave().setApiKey("test-brave-key");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BraveSearchAdapter(builder.build(), properties);
    }

    @Test
    void searchesWithExtraSnippetsFiltersNonEduAndCapsRequestsAndSourcesToTwo() {
        String extra1 = "Phần 1: Khái niệm Ước chung lớn nhất (ƯCLN) của hai hay nhiều số tự nhiên là số lớn nhất trong tập hợp các ước chung của các số đó. ".repeat(6).trim();
        String extra2 = "Phần 2: Để tìm ƯCLN bằng cách phân tích các số ra thừa số nguyên tố, ta chọn các thừa số nguyên tố chung với số mũ nhỏ nhất rồi nhân lại. ".repeat(6).trim();
        String webJson = """
                {
                  "web": {
                    "results": [
                      { "title": "Video Facebook giải nhanh", "url": "https://www.facebook.com/groups/math7", "description": "Không dùng nguồn mạng xã hội" },
                      { "title": "Hành trang số SGK", "url": "https://hanhtrangso.nxbgd.vn/sach-dien-tu/toan-7", "description": "Vue SPA không có nội dung văn bản" },
                      { "title": "Cổng Bộ GD", "url": "https://moet.gov.vn/tintuc/Pages/thong-bao.aspx", "description": "Thông tư hành chính" },
                      { "title": "OLM Toán 7", "url": "https://olm.vn/hoi-dap/detail/123", "description": "Cloudflare blocked" },
                      { "title": "HayLamDo Toán 7", "url": "https://haylamdo.com/toan-7/bai-11", "description": "Timeout host" },
                      { "title": "Blog chung", "url": "https://example.com/1", "description": "Mô tả chung" },
                      { "title": "Toán 7 Kết nối tri thức - Bài 11", "url": "https://loigiaihay.com/ket-noi-tri-thuc-bai-11", "description": "Lý thuyết trọng tâm Toán 7.", "extra_snippets": ["%s", "%s"] },
                      { "title": "VietJack Toán 7 - Ước chung lớn nhất", "url": "https://vietjack.com/toan-7/ucln", "description": "Tóm tắt bài học chuẩn SGK" }
                    ]
                  }
                }
                """.formatted(extra1, extra2);

        String wikiJson = """
                {
                  "web": {
                    "results": [
                      { "title": "Wikipedia - Ước chung lớn nhất", "url": "https://vi.wikipedia.org/wiki/UCLN", "description": "Định nghĩa Wikipedia chuẩn xác" }
                    ]
                  }
                }
                """;

        // Request 1: educational lesson websites with extra_snippets=true
        server.expect(requestTo(containsString("api.search.brave.com")))
                .andExpect(requestTo(containsString("country=ALL")))
                .andExpect(requestTo(containsString("search_lang=vi")))
                .andExpect(requestTo(containsString("extra_snippets=true")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Subscription-Token", "test-brave-key"))
                .andRespond(withSuccess(webJson, MediaType.APPLICATION_JSON));

        // Request 2: Wikipedia directly for conceptual MATH topic (1 web + 1 wiki = 2 sources max)
        server.expect(requestTo(containsString("vi.wikipedia.org")))
                .andExpect(requestTo(containsString("extra_snippets=true")))
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
        assertEquals(2, results.size());
        assertFalse(results.stream().anyMatch(s -> s.url().contains("facebook.com")));
        assertFalse(results.stream().anyMatch(s -> s.url().contains("hanhtrangso.nxbgd.vn")));
        assertFalse(results.stream().anyMatch(s -> s.url().contains("moet.gov.vn")));
        assertFalse(results.stream().anyMatch(s -> s.url().contains("olm.vn")));
        assertFalse(results.stream().anyMatch(s -> s.url().contains("haylamdo.com")));
        assertTrue(results.get(0).title().contains("Kết nối tri thức"));
        assertTrue(results.get(0).snippet().contains("thừa số nguyên tố"));
        assertTrue(results.get(0).snippet().length() > 1000 && results.get(0).snippet().length() <= 2000,
                "Expected snippet > 1000 and <= 2000 chars but got " + results.get(0).snippet().length());
        assertTrue(results.get(1).url().contains("vi.wikipedia.org"));
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
    void routesEnglishToGlobalSuccessAndLiteratureToVietnameseTextbookPortals() {
        SearchQuery english = new SearchQuery(6, SubjectCode.ENG, "Tiếng Anh", "Unit 5: Natural Wonders");
        assertTrue(adapter.buildLessonWebQuery(english).contains("Global Success"));
        String engSecondary = adapter.buildSecondarySourceQuery(english);
        assertTrue(engSecondary.contains("Global Success"));
        assertFalse(engSecondary.contains("vi.wikipedia.org"));

        // Ensure Request 2 for ENG does NOT query vi.wikipedia.org
        String engWeb1 = """
                {"web":{"results":[{"title":"Global Success Unit 5","url":"https://vietjack.com/eng6-unit5","description":"Nature vocab"}]}}
                """;
        String engWeb2 = """
                {"web":{"results":[{"title":"Loigiaihay Unit 5 Natural Wonders","url":"https://loigiaihay.com/eng6-unit5","description":"Travel items vocab: plaster, backpack, sleeping bag"}]}}
                """;
        server.expect(requestTo(containsString("Global%20Success")))
                .andExpect(requestTo(not(containsString("vi.wikipedia.org"))))
                .andRespond(withSuccess(engWeb1, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("Global%20Success")))
                .andExpect(requestTo(not(containsString("vi.wikipedia.org"))))
                .andRespond(withSuccess(engWeb2, MediaType.APPLICATION_JSON));

        List<SearchSnippet> engResults = adapter.search(english);
        server.verify();
        assertEquals(2, engResults.size());

        SearchQuery lit = new SearchQuery(7, SubjectCode.LIT, "Ngữ văn", "Bài 1: Bầy chim chìa vôi");
        assertTrue(adapter.buildLessonWebQuery(lit).contains("Kết nối tri thức"));
        String litSecondary = adapter.buildSecondarySourceQuery(lit);
        assertTrue(litSecondary.contains("Ngữ văn Lớp 7"));
        assertTrue(litSecondary.contains("Bầy chim chìa vôi"));
        assertFalse(litSecondary.contains("vi.wikipedia.org"));

        SearchQuery tech = new SearchQuery(8, SubjectCode.TECH, "Công nghệ", "Bài 4: Vật liệu cơ khí");
        assertTrue(adapter.buildLessonWebQuery(tech).contains("Lớp 8 Công nghệ Bài 4: Vật liệu cơ khí Kết nối tri thức"));
        assertEquals("site:vi.wikipedia.org Vật liệu cơ khí", adapter.buildWikipediaQuery(tech));
    }

    @Test
    void safelyEncodesCurlyBracesInLessonTitlesWithoutUriTemplateCrash() {
        server.reset();
        String mathJson = """
                {"web":{"results":[{"title":"Toán 6 Tập hợp","url":"https://vietjack.com/toan-6-tap-hop","description":"Tập hợp A gồm các phần tử 1, 2, 3"}]}}
                """;
        server.expect(requestTo(containsString("api.search.brave.com")))
                .andExpect(requestTo(containsString("%7B1")))
                .andRespond(withSuccess(mathJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("vi.wikipedia.org")))
                .andExpect(requestTo(containsString("%7B1")))
                .andRespond(withSuccess("{\"web\":{\"results\":[]}}", MediaType.APPLICATION_JSON));

        List<SearchSnippet> results = adapter.search(
                new SearchQuery(6, SubjectCode.MATH, "Toán", "Tập hợp A = {1; 2; 3}")
        );
        server.verify();
        assertEquals(1, results.size());
    }
}
