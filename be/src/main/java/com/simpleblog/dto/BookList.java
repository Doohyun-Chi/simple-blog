package com.simpleblog.dto;

import java.util.List;

/** The shelf: the books asked for and the years that have books. */
public record BookList(List<Integer> years, List<BookInfo> books) {
}
