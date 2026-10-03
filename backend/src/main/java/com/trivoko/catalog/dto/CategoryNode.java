package com.trivoko.catalog.dto;

import java.util.List;

/** The category tree for the menu: top categories with their sub-categories. */
public record CategoryNode(Long id, String name, String slug, List<CategoryNode> children) {
}
