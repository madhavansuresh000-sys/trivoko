package com.trivoko.flashsale;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trivoko.flashsale.dto.FlashDtos.SaleRequest;
import com.trivoko.flashsale.dto.FlashDtos.SaleSummary;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** The admin schedules flash sales (ADMIN only, SecurityConfig). */
@RestController
@RequestMapping("/api/admin/flash-sales")
@RequiredArgsConstructor
@Tag(name = "Admin - flash sales")
public class AdminFlashSaleController {

	private final FlashSaleService saleService;

	@GetMapping
	public List<SaleSummary> all() {
		return saleService.all();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SaleSummary create(@Valid @RequestBody SaleRequest request) {
		return saleService.create(request);
	}

}
