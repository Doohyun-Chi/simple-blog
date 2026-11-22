package com.simpleblog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The {@code TB_MEDIA} table of DBSCHEMA.md. An internal row is an image file kept on the server ({@code MIME_TYPE} is
 * its mime type, {@code LOCATION} its stored file name); an external row is a reference ({@code MIME_TYPE} and
 * {@code LOCATION} are the URL, {@code THUMBNAIL} the YouTube thumbnail). {@code DATE} is UTC.
 */
@Entity
@Table(name = "TB_MEDIA")
public class Media {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "SLUG")
	private String slug;

	@Column(name = "PROVIDER")
	private MediaProvider provider;

	@Column(name = "MIME_TYPE")
	private String mimeType;

	@Column(name = "LOCATION")
	private String location;

	@Column(name = "THUMBNAIL")
	private String thumbnail;

	@Column(name = "DATE")
	private LocalDateTime date;

	@Column(name = "OWNER_ID")
	private Long ownerId;

	protected Media() {
	}

	private Media(String slug, MediaProvider provider, String mimeType, String location, String thumbnail, LocalDateTime date,
			long ownerId) {
		this.slug = slug;
		this.provider = provider;
		this.mimeType = mimeType;
		this.location = location;
		this.thumbnail = thumbnail;
		this.date = date;
		this.ownerId = ownerId;
	}

	public static Media internalImage(String slug, String mimeType, String storedName, LocalDateTime date, long ownerId) {
		return new Media(slug, MediaProvider.INTERNAL, mimeType, storedName, null, date, ownerId);
	}

	public static Media externalVideo(String slug, String url, String thumbnail, LocalDateTime date, long ownerId) {
		return new Media(slug, MediaProvider.EXTERNAL, url, url, thumbnail, date, ownerId);
	}

	public boolean ownedBy(long userId) {
		return this.ownerId != null && this.ownerId == userId;
	}

	public boolean storesFile() {
		return this.provider.storesFile();
	}

	public String slug() {
		return this.slug;
	}

	public MediaProvider provider() {
		return this.provider;
	}

	public String mimeType() {
		return this.mimeType;
	}

	public String location() {
		return this.location;
	}

	public String thumbnail() {
		return this.thumbnail;
	}

	public LocalDateTime date() {
		return this.date;
	}

}
