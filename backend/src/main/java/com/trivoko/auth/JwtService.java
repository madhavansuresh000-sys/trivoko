package com.trivoko.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Makes and checks JWT login tokens (copied from EventHub).
 *
 * A JWT is like a stamped mall ID card: anyone can READ what is written on it (user id, email,
 * expiry), but only the server has the stamp (the secret key), so nobody can change it or make a
 * fake one. Signed with HMAC-SHA256.
 *
 * TriVoKo puts the roles on the card too (handy for the frontend), but the server does NOT trust
 * them: JwtCookieFilter reads the current roles from the database on every request.
 */
@Service
public class JwtService {

	private static final String ISSUER = "trivoko";

	private final JwtEncoder encoder;

	private final JwtDecoder decoder;

	private final Duration expiry;

	public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiry}") Duration expiry) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least 32 characters (256 bits) long");
		}
		SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		// checks the signature, the expiry time and that we made it
		nimbus.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
		this.decoder = nimbus;
		this.expiry = expiry;
	}

	/** A new token for this user, valid for app.jwt.expiry (8 hours). */
	public String issue(AuthUser user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.issuedAt(now)
			.expiresAt(now.plus(expiry))
			.subject(String.valueOf(user.id()))
			.claim("email", user.email())
			.claim("roles", user.roles().stream().map(Enum::name).sorted().toList())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/** The token's contents if the signature is right and it has not expired; empty otherwise. */
	public Optional<Jwt> read(String token) {
		try {
			return Optional.of(decoder.decode(token));
		}
		catch (JwtException ex) {
			return Optional.empty();
		}
	}

	public Duration expiry() {
		return expiry;
	}

}
