package com.asc.studycompanion.module.ai.provider;

/**
 * Port interface for LLM evaluation providers (MiMo v2.6 Flash via CKEY).
 */
public interface AiEvaluationProvider {

    AiEvaluationResult evaluate(AiEvaluationInput input);
}
