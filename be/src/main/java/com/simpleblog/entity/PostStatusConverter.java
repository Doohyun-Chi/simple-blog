package com.simpleblog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores a {@link PostStatus} as its code, so TB_POST.STATUS keeps the values WordPress used. */
@Converter(autoApply = true)
public class PostStatusConverter implements AttributeConverter<PostStatus, String> {

	@Override
	public String convertToDatabaseColumn(PostStatus status) {
		return status == null ? null : status.code();
	}

	@Override
	public PostStatus convertToEntityAttribute(String code) {
		return code == null ? null : PostStatus.of(code);
	}

}
