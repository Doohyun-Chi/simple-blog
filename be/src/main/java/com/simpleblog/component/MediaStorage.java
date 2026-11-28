package com.simpleblog.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Keeps uploaded files in one directory. The stored name is generated here and the client's name never reaches the
 * file system, so a path cannot be steered out of the directory.
 */
@Component
public class MediaStorage {

	private static final Logger LOG = LoggerFactory.getLogger(MediaStorage.class);

	private final Path directory;

	public MediaStorage(@Value("${simpleblog.media.directory:./media}") String directory) {
		this.directory = Path.of(directory).toAbsolutePath().normalize();
	}

	/** Writes the bytes under a new name and returns that name. */
	public String save(byte[] content, String extension) {
		String name = UUID.randomUUID() + "." + extension;
		write(name, content);
		return name;
	}

	/**
	 * Writes the bytes beside a stored file: under its name with this extension instead of its own (see {@link #besideName}).
	 * Returns that name.
	 */
	public String saveBeside(String stored, String extension, byte[] content) {
		String name = besideName(stored, extension);
		write(name, content);
		return name;
	}

	/** The name of the file kept beside a stored one: {@code 3f2a….png} with the extension drawio is {@code 3f2a….drawio}. */
	public static String besideName(String stored, String extension) {
		int dot = stored.lastIndexOf('.');
		return (dot < 0 ? stored : stored.substring(0, dot)) + "." + extension;
	}

	private void write(String name, byte[] content) {
		try {
			Files.createDirectories(this.directory);
			Files.write(this.directory.resolve(name), content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to store the media file", ex);
		}
	}

	public Resource load(String name) {
		return new FileSystemResource(this.directory.resolve(name));
	}

	/**
	 * Removes the file once the current transaction commits, so a rolled back deletion keeps it. The row is gone by
	 * then, so a file that cannot be removed is only logged: it is left behind, unreachable, rather than failing a save
	 * that has already happened.
	 */
	public void removeAfterCommit(String name) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					remove(name);
				}
				catch (UncheckedIOException ex) {
					LOG.warn("Left the media file {} behind", name, ex);
				}
			}
		});
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
