package com.axonivy.utils.ai.function.strategy;

/**
 * Interface for processing AI results specific to different extraction strategies.
 */
public interface ResultProcessor {
    /**
     * Processes the raw result from AI based on the extraction strategy.
     * 
     * @param rawResult The raw result string from the AI
     * @return The processed result string
     */
    String processResult(String rawResult);
} 