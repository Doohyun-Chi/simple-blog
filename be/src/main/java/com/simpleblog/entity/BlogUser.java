package com.simpleblog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The {@code TB_USER} table of DBSCHEMA.md: who has signed in. IDs 1 to 100 are reserved and handed out from 101 by the
 * database. {@code EMAIL} is stored trimmed and lower case; {@code REGISTERED_DATE} is UTC.
 */
@Entity
@Table(name = "TB_USER")
public class BlogUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "EMAIL")
	private String email;

	@Column(name = "PROVIDER")
	private String provider;

	@Column(name = "REGISTERED_DATE")
	private LocalDateTime registeredDate;

	protected BlogUser() {
	}

	public BlogUser(String email, String provider, LocalDateTime registeredDate) {
		this.email = email;
		this.provider = provider;
		this.registeredDate = registeredDate;
	}

	public Long id() {
		return this.id;
	}

	public String email() {
		return this.email;
	}

	public String provider() {
		return this.provider;
	}

}
