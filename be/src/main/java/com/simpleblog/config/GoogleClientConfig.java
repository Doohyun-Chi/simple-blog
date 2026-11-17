package com.simpleblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * The Google OAuth client, built from {@code simpleblog.auth.google.client-id} and {@code client-secret}. The
 * secret belongs in the gitignored {@code be/config/application-local.properties} (or the deployment's
 * environment), never in a tracked file. Without a client id there is no registration, and the Google sign-in is
 * simply not offered. The redirect URI is the one registered for the client in Google Cloud Console.
 */
@Configuration
@ConditionalOnExpression("!'${simpleblog.auth.google.client-id:}'.isBlank()")
public class GoogleClientConfig {

	static final String REDIRECT_URI = "{baseUrl}/public/auth/{registrationId}/callback";

	@Bean
	public ClientRegistrationRepository googleClientRegistrations(@Value("${simpleblog.auth.google.client-id}") String clientId,
			@Value("${simpleblog.auth.google.client-secret}") String clientSecret) {
		ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
			.clientId(clientId)
			.clientSecret(clientSecret)
			.redirectUri(REDIRECT_URI)
			.build();
		return new InMemoryClientRegistrationRepository(google);
	}

}
