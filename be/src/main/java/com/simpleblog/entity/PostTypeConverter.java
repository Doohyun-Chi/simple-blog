package com.simpleblog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores a {@link PostType} as its code, so TB_POST.TYPE keeps the values WordPress used. */
@Converter(autoApply = true)
public class PostTypeConverter implements AttributeConverter<PostType, String> {

	@Override
	public String convertToDatabaseColumn(PostType type) {
		return type == null ? null : type.code();
	}

	@Override
	public PostType convertToEntityAttribute(String code) {
		return code == null ? null : PostType.of(code);
	}

}
