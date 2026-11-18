package com.simpleblog.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Keeps uploaded files in one directory. The stored name is generated here and the client's name never reaches the
 * file system, so a path cannot be steered out of the directory.
 */
@Component
public class MediaStorage {

	private final Path directory;

	public MediaStorage(@Value("${simpleblog.media.directory:./media}") String directory) {
		this.directory = Path.of(directory).toAbsolutePath().normalize();
	}

	/** Writes the bytes under a new name and returns that name. */
	public String save(byte[] content, String extension) {
		String name = UUID.randomUUID() + "." + extension;
		try {
			Files.createDirectories(this.directory);
			Files.write(this.directory.resolve(name), content);
			return name;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to store the media file", ex);
		}
	}

	public Resource load(String name) {
		return new FileSystemResource(this.directory.resolve(name));
	}

	public void remove(String name) {
		try {
			Files.deleteIfExists(this.directory.resolve(name));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to remove the media file", ex);
		}
	}

}
