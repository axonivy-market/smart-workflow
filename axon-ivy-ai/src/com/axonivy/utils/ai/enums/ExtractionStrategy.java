package com.axonivy.utils.ai.enums;

/**
 * Defines the different strategies for extracting structured data from AI responses.
 */
public enum ExtractionStrategy {
    /**
     * Uses wrapper delimiters (e.g., << >>) around the result.
     * AI is instructed to wrap the response in markers for easy extraction.
     */
    WRAPPER,
    
    /**
     * Uses JSON schema instructions to guide the AI response format.
     * AI is provided with a JSON schema and asked to return compliant JSON directly.
     */
    INSTRUCTIONS
} 