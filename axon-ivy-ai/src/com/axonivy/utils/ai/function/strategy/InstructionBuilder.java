package com.axonivy.utils.ai.function.strategy;

/**
 * Interface for building instructions specific to different extraction strategies.
 */
public interface InstructionBuilder {
    /**
     * Builds the instruction string for the AI based on the provided context.
     * 
     * @param context The context object (e.g., target object, schema) to build instructions from
     * @return The formatted instruction string
     */
    String buildInstructions(Object context);
} 