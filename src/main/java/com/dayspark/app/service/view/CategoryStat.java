package com.dayspark.app.service.view;

/** Completions of one category this month; {@code percentage} is relative to the largest category. */
public record CategoryStat(String category, long count, int percentage) {}
