package com.simpleblog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores a {@link SourceType} as its code, and no source as null, in TB_MEDIA.SOURCE_TYPE. */
@Converter(autoApply = true)
public class SourceTypeConverter implements AttributeConverter<SourceType, String> {

	@Override
	public String convertToDatabaseColumn(SourceType type) {
		return type == null ? null : type.code();
	}

	@Override
	public SourceType convertToEntityAttribute(String code) {
		return code == null ? null : SourceType.of(code);
	}

}
