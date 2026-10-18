package com.learnpilot.learning;

import java.util.List;

/** A multiple choice question with exactly four options. */
public record GeneratedQuestion(String text, List<String> options, int correctIndex, String explanation, int page) {}
