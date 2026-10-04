package com.trivoko.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.trivoko.catalog.dto.UploadSignature;

/**
 * The REAL upload (step 10 "tried with one real photo"). It talks to the internet, so it only runs when asked:
 *
 * <pre>
 *   mvnw test -Dtest=CloudinaryLiveUploadTest -Dcloudinary.live=true -Dcloudinary.photo=C:\path\to\photo.jpg
 * </pre>
 *
 * The keys come from .env (CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET).
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "cloudinary.live", matches = "true")
class CloudinaryLiveUploadTest {

	@Autowired
	private ImageUploadService uploadService;

	@Test
	void photoGoesStraightToCloudinaryWithOurSignature() throws Exception {
		Path photo = Path.of(System.getProperty("cloudinary.photo"));
		UploadSignature sig = uploadService.signProductPhotoUpload();

		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("api_key", sig.apiKey());
		fields.put("timestamp", String.valueOf(sig.timestamp()));
		fields.put("signature", sig.signature());
		fields.put("folder", sig.folder());
		fields.put("allowed_formats", sig.allowedFormats());

		String boundary = "----trivoko" + UUID.randomUUID();
		HttpRequest request = HttpRequest.newBuilder(URI.create(sig.uploadUrl()))
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.POST(HttpRequest.BodyPublishers.ofByteArray(multipart(boundary, fields, photo)))
			.build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		System.out.println("CLOUDINARY " + response.statusCode() + ": " + response.body());
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"secure_url\":\"https://res.cloudinary.com/" + sig.cloudName());
		assertThat(response.body()).contains("trivoko/products/");
	}

	private static byte[] multipart(String boundary, Map<String, String> fields, Path file) throws Exception {
		var out = new java.io.ByteArrayOutputStream();
		for (var field : fields.entrySet()) {
			out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n"
					+ field.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
		}
		out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
				+ file.getFileName() + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		out.write(Files.readAllBytes(file));
		out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
		return out.toByteArray();
	}

}
