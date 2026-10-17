package com.digitalfix.ms_digitalfix_catalog.repository;

import com.digitalfix.ms_digitalfix_catalog.model.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
}