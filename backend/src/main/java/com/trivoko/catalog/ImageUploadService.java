package com.trivoko.catalog;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.trivoko.catalog.dto.UploadSignature;
import com.trivoko.common.ServiceNotConfiguredException;

/**
 * Signed uploads to Cloudinary.
 *
 * <pre>
 *  Browser --1. POST /api/uploads/signature--> TriVoKo server (knows the secret, signs)
 *  Browser <--2. signature + timestamp-------- TriVoKo server
 *  Browser --3. photo + signature-----------> Cloudinary (checks the signature, stores the photo)
 * </pre>
 *
 * Like a gate pass: the office (our server) stamps the pass, the guard (Cloudinary) checks the stamp.
 * Cloudinary refuses the upload if anyone changes a signed value (folder, formats) or uses it after one hour.
 */
@Service
public class ImageUploadService {

	static final String FOLDER = "trivoko/products";
	static final String ALLOWED_FORMATS = "jpg,jpeg,png,webp";
	/** 5 MB. Cloudinary cannot enforce this from a signed parameter, so the upload page checks it before sending. */
	static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

	private final String cloudName;
	private final String apiKey;
	private final String apiSecret;
	private final Clock clock;

	ImageUploadService(@Value("${app.cloudinary.cloud-name:}") String cloudName,
			@Value("${app.cloudinary.api-key:}") String apiKey,
			@Value("${app.cloudinary.api-secret:}") String apiSecret) {
		this.cloudName = cloudName;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.clock = Clock.systemUTC();
	}

	public UploadSignature signProductPhotoUpload() {
		if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
			throw new ServiceNotConfiguredException("Photo upload is not set up yet (Cloudinary keys missing in .env)");
		}
		long timestamp = clock.instant().getEpochSecond();
		// must be a mutable map (the library adds to it) - see docs/decisions.md D1
		Map<String, Object> signed = new HashMap<>(Map.of(
				"timestamp", timestamp,
				"folder", FOLDER,
				"allowed_formats", ALLOWED_FORMATS));
		Cloudinary cloudinary = new Cloudinary(Map.of("cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret));
		String signature = cloudinary.apiSignRequest(signed, apiSecret, 2);

		return new UploadSignature(cloudName, apiKey, timestamp, signature, FOLDER, ALLOWED_FORMATS, MAX_FILE_SIZE,
				"https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");
	}

}
