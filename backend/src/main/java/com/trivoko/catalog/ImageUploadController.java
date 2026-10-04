package com.trivoko.catalog;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.catalog.dto.UploadSignature;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Logged-in users only (SecurityConfig); from Phase 2 limited to sellers and admins. */
@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads", description = "Signed photo uploads to Cloudinary")
public class ImageUploadController {

	private final ImageUploadService uploadService;

	ImageUploadController(ImageUploadService uploadService) {
		this.uploadService = uploadService;
	}

	@PostMapping("/signature")
	@Operation(summary = "Get a one-time signature to upload one product photo straight to Cloudinary")
	public UploadSignature signature() {
		return uploadService.signProductPhotoUpload();
	}

}
