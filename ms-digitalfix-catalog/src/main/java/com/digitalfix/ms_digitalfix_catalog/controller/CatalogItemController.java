package com.digitalfix.ms_digitalfix_catalog.controller;

import com.digitalfix.ms_digitalfix_catalog.dto.CatalogItemRequest;
import com.digitalfix.ms_digitalfix_catalog.dto.StockUpdateRequest;
import com.digitalfix.ms_digitalfix_catalog.model.CatalogItem;
import com.digitalfix.ms_digitalfix_catalog.service.CatalogItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/services")
@RequiredArgsConstructor
public class CatalogItemController {

    private final CatalogItemService catalogItemService;

    @PostMapping
    public ResponseEntity<CatalogItem> create(@Valid @RequestBody CatalogItemRequest request) {
        CatalogItem created = catalogItemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CatalogItem>> findAll() {
        return ResponseEntity.ok(catalogItemService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogItem> findById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogItemService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CatalogItem> updateTarifaStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request) {
        CatalogItem updated = catalogItemService.updateTarifaStock(id, request);
        return ResponseEntity.ok(updated);
    }
}