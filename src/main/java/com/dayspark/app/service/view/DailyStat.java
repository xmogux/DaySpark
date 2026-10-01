package com.dayspark.app.service.view;

/** One bar of the weekly chart on the statistics page. */
public record DailyStat(String label, long completed, long planned, int percentage) {}
