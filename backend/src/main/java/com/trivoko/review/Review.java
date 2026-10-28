package com.trivoko.review;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "4 stars - Good grip, runs a little small." ONE per customer per product (V12 unique key); writing again edits it.
 * Product and user ids are plain numbers: this module never touches the catalog or user tables (ArchUnit rule).
 */
@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(nullable = false)
	private int rating;

	@Column(length = 80)
	private String title;

	@Column(nullable = false, length = 1000)
	private String body;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public Review(Long productId, Long userId) {
		this.productId = productId;
		this.userId = userId;
	}

	public void write(int rating, String title, String body) {
		this.rating = rating;
		this.title = title;
		this.body = body;
	}

}
