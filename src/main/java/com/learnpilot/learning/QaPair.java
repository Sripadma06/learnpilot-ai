package com.learnpilot.learning;

/** A question that appears in the material together with the answer text that follows it. */
public record QaPair(String question, String answer, int page) {}
