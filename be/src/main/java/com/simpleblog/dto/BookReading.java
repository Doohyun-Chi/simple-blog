package com.simpleblog.dto;

import java.time.LocalDate;

/** The request body of checking in to a book. */
public record BookReading(LocalDate readingStartDate, LocalDate readingCompleteDate) {
}
