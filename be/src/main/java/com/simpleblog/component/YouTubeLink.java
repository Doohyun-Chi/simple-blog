package com.simpleblog.component;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/** Recognises a link to a YouTube video and derives its canonical link and thumbnail. Nothing is fetched. */
@Component
public class YouTubeLink {

	private static final Set<String> HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be");

	private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

	private static final Pattern VIDEO_PATH = Pattern.compile("^/(?:shorts/|embed/)?([A-Za-z0-9_-]{11})/?$");

	private static final Pattern WATCH_QUERY = Pattern.compile("(?:^|&)v=([A-Za-z0-9_-]{11})(?:&|$)");

	/** The video ID of a https or http link, or empty when it is not a link to one YouTube video. */
	public Optional<String> videoIdOf(String url) {
		try {
			URI uri = new URI(url.strip());
			boolean web = "https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme());
			if (!web || uri.getHost() == null || !HOSTS.contains(uri.getHost().toLowerCase())) {
				return Optional.empty();
			}
			return "youtu.be".equalsIgnoreCase(uri.getHost()) ? match(VIDEO_PATH, uri.getRawPath()) : fromYouTubeHost(uri);
		}
		catch (java.net.URISyntaxException | RuntimeException ex) {
			return Optional.empty();
		}
	}

	public String canonicalOf(String videoId) {
		return "https://youtu.be/" + videoId;
	}

	public String thumbnailOf(String videoId) {
		return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
	}

	private Optional<String> fromYouTubeHost(URI uri) {
		if ("/watch".equals(uri.getRawPath()) && uri.getRawQuery() != null) {
			return match(WATCH_QUERY, uri.getRawQuery());
		}
		return uri.getRawPath() != null && uri.getRawPath().matches("^/(?:shorts|embed)/.*")
				? match(VIDEO_PATH, uri.getRawPath()) : Optional.empty();
	}

	private Optional<String> match(Pattern pattern, String text) {
		Matcher matcher = pattern.matcher(text == null ? "" : text);
		return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
	}

}
