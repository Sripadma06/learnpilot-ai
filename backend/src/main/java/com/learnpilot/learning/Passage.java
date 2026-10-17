package com.learnpilot.learning;

/** A piece of source text with the PDF page it came from. */
public record Passage(int page, String text) {}
