package com.project.demo.rest.product;

import com.project.demo.logic.entity.category.Category;
import com.project.demo.logic.entity.category.CategoryRepository;
import com.project.demo.logic.entity.http.GlobalResponseHandler;
import com.project.demo.logic.entity.http.Meta;
import com.project.demo.logic.entity.product.Product;
import com.project.demo.logic.entity.product.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductRestController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER')")
    public ResponseEntity<?> getAllProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Product> productPage = productRepository.findAll(pageable);
        Meta meta = new Meta(request.getMethod(), request.getRequestURL().toString());
        meta.setTotalPages(productPage.getTotalPages());
        meta.setTotalElements(productPage.getTotalElements());
        meta.setPageNumber(productPage.getNumber() + 1);
        meta.setPageSize(productPage.getSize());

        return new GlobalResponseHandler().handleResponse("Products retrieved successfully",
                productPage.getContent(), HttpStatus.OK, meta);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody Product product, HttpServletRequest request) {
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Optional<Category> foundCategory = categoryRepository.findById(product.getCategory().getId());
            if (foundCategory.isEmpty()) {
                return new GlobalResponseHandler().handleResponse("Category id " + product.getCategory().getId() + " not found",
                        HttpStatus.NOT_FOUND, request);
            }
            product.setCategory(foundCategory.get());
        } else {
            product.setCategory(null); // Permitir productos sin categoría
        }

        Product savedProduct = productRepository.save(product);
        return new GlobalResponseHandler().handleResponse("Product created successfully",
                savedProduct, HttpStatus.CREATED, request);
    }


    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId, @RequestBody Product product, HttpServletRequest request) {
        Optional<Product> foundProduct = productRepository.findById(productId);

        if (foundProduct.isPresent()) {
            Product existingProduct = foundProduct.get();

            existingProduct.setName(product.getName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setStockQuantity(product.getStockQuantity());

            if (product.getCategory() != null && product.getCategory().getId() != null) {
                Optional<Category> categoryOptional = categoryRepository.findById(product.getCategory().getId());
                if (categoryOptional.isEmpty()) {
                    return new GlobalResponseHandler().handleResponse("Category id " + product.getCategory().getId() + " not found",
                            HttpStatus.BAD_REQUEST, request);
                }
                existingProduct.setCategory(categoryOptional.get());
            } else {
                existingProduct.setCategory(null); // Quitar categoría si no se incluye
            }

            Product updatedProduct = productRepository.save(existingProduct);
            return new GlobalResponseHandler().handleResponse("Product updated successfully",
                    updatedProduct, HttpStatus.OK, request);

        } else {
            return new GlobalResponseHandler().handleResponse("Product id " + productId + " not found",
                    HttpStatus.NOT_FOUND, request);
        }
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId, HttpServletRequest request) {
        Optional<Product> foundProduct = productRepository.findById(productId);

        if (foundProduct.isPresent()) {
            productRepository.deleteById(productId);
            return new GlobalResponseHandler().handleResponse("Product deleted successfully",
                    foundProduct.get(), HttpStatus.OK, request);
        } else {
            return new GlobalResponseHandler().handleResponse("Product id " + productId + " not found",
                    HttpStatus.NOT_FOUND, request);
        }
    }

}
