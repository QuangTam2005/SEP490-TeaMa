package com.asc.studycompanion.module.ai.provider;

import java.util.List;

/**
 * Port interface for grounded curriculum web search (e.g., Brave Search).
 */
public interface SearchProvider {

    List<SearchSnippet> search(SearchQuery query);
}
