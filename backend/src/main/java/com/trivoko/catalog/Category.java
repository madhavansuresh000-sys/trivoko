package com.trivoko.catalog;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A node in the category tree, e.g. "Mobiles & Accessories" (top, no parent)
 * with children "Mobiles", "Cases & covers", "Chargers & cables".
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(nullable = false, unique = true, length = 100)
	private String slug;

	/** null = a top category. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Category parent;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@OneToMany(mappedBy = "parent")
	@OrderBy("sortOrder")
	private List<Category> children = new ArrayList<>();

	public Category(String name, String slug, Category parent, int sortOrder) {
		this.name = name;
		this.slug = slug;
		this.parent = parent;
		this.sortOrder = sortOrder;
	}

}
