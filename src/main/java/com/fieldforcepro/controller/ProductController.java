package com.fieldforcepro.controller;

import com.fieldforcepro.model.Product;
import com.fieldforcepro.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    @Operation(summary = "List products with optional search")
    public List<Product> listProducts(@RequestParam(value = "search", required = false) String search) {
        List<Product> all = productRepository.findAll();
        if (search == null || search.isBlank()) {
            return all;
        }
        String q = search.toLowerCase();
        return all.stream()
                .filter(p ->
                        (p.getName() != null && p.getName().toLowerCase().contains(q)) ||
                        (p.getSku() != null && p.getSku().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id")
    public ResponseEntity<Product> getProduct(@PathVariable("id") Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    public record ProductRequest(String name, BigDecimal price, String description) { }

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) {
        String sku = request.name() != null ? request.name().toUpperCase().replaceAll("\\s+", "-") : null;
        Product toSave = Product.builder()
                .name(request.name())
                .sku(sku)
                .category("GENERAL")
                .price(request.price())
                .description(request.description())
                .active(true)
                .build();
        Product saved = productRepository.save(toSave);
        return ResponseEntity.created(URI.create("/products/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<Product> updateProduct(@PathVariable("id") Long id, @RequestBody ProductRequest request) {
        Optional<Product> existingOpt = productRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Product existing = existingOpt.get();
        existing.setName(request.name());
        existing.setPrice(request.price());
        existing.setDescription(request.description());
        Product saved = productRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
