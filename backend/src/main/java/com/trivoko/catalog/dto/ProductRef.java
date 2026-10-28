package com.trivoko.catalog.dto;

import java.util.List;

/** A live product as other modules need it: reviews ask the order module "did they receive one of these variants?". */
public record ProductRef(Long id, String name, String slug, List<Long> variantIds) {
}
