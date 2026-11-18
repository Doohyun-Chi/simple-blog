package com.simpleblog.dto;

import java.util.List;

/** All media items as in be/apispec.yaml ({@code MediaList}). */
public record MediaList(List<MediaInfo> medias) {
}
