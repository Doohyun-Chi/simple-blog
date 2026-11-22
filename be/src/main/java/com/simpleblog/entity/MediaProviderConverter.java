package com.simpleblog.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores a {@link MediaProvider} as its code, so TB_MEDIA.PROVIDER keeps the values it has. */
@Converter(autoApply = true)
public class MediaProviderConverter implements AttributeConverter<MediaProvider, String> {

	@Override
	public String convertToDatabaseColumn(MediaProvider provider) {
		return provider == null ? null : provider.code();
	}

	@Override
	public MediaProvider convertToEntityAttribute(String code) {
		return code == null ? null : MediaProvider.of(code);
	}

}
