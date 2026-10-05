package com.trivoko.catalog;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trivoko.catalog.dto.CategoryNode;
import com.trivoko.common.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categories;

	CategoryService(CategoryRepository categories) {
		this.categories = categories;
	}

	/** The whole tree for the menu (40 rows - small enough to load every time). */
	public List<CategoryNode> tree() {
		return categories.findByParentIsNullOrderBySortOrder().stream().map(CatalogMapper::toNode).toList();
	}

	/** "mobiles-accessories" -> its own id + the ids of every category below it. */
	public List<Long> idsWithChildren(String slug) {
		Category category = categories.findBySlug(slug)
			.orElseThrow(() -> new ResourceNotFoundException("Category", slug));
		List<Long> ids = new ArrayList<>();
		collect(category, ids);
		return ids;
	}

	/**
	 * Like idsWithChildren, but an unknown slug gives an empty list instead of a 404. (Throwing inside this
	 * @Transactional class would mark the caller's transaction "rollback only", even if the caller catches it.)
	 */
	public List<Long> idsWithChildrenOrEmpty(String slug) {
		return categories.findBySlug(slug).map(category -> {
			List<Long> ids = new ArrayList<>();
			collect(category, ids);
			return ids;
		}).orElse(List.of());
	}

	private void collect(Category category, List<Long> ids) {
		ids.add(category.getId());
		category.getChildren().forEach(child -> collect(child, ids));
	}

}
