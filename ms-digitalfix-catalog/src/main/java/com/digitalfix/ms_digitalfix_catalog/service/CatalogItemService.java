package com.digitalfix.ms_digitalfix_catalog.service;

import com.digitalfix.ms_digitalfix_catalog.dto.CatalogItemRequest;
import com.digitalfix.ms_digitalfix_catalog.dto.StockUpdateRequest;
import com.digitalfix.ms_digitalfix_catalog.model.CatalogItem;
import com.digitalfix.ms_digitalfix_catalog.repository.CatalogItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lógica de negocio del catálogo de servicios técnicos y repuestos.
 * Se encarga del CRUD y de validar que el stock nunca quede negativo al descontar.
 */
@Service
public class CatalogItemService {

    private final CatalogItemRepository repository;

    public CatalogItemService(CatalogItemRepository repository) {
        this.repository = repository;
    }

    public CatalogItem create(CatalogItemRequest request) {
        CatalogItem item = new CatalogItem();
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setStock(request.getStock());
        return repository.save(item);
    }

    public List<CatalogItem> findAll() {
        return repository.findAll();
    }

    public CatalogItem findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item no encontrado: " + id));
    }

    // Actualización parcial: solo pisa tarifa o stock si realmente vienen en el request
    public CatalogItem updateTarifaStock(Long id, StockUpdateRequest request) {
        CatalogItem item = findById(id);
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            item.setStock(request.getStock());
        }
        return repository.save(item);
    }

    // Descuenta stock validando que haya unidades suficientes antes de guardar
    public void decreaseStock(Long id, int amount) {
        CatalogItem item = findById(id);
        int newStock = item.getStock() - amount;
        if (newStock < 0) {
            throw new IllegalStateException("Stock insuficiente para el item: " + id);
        }
        item.setStock(newStock);
        repository.save(item);
    }
}