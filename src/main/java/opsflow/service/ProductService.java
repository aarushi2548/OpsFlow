
package opsflow.service;

import opsflow.entity.Product;
import opsflow.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {

        if (productRepository.existsBySku(product.getSku())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product SKU already exists"
            );
        }

        product.setId(null);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {

        return productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Product not found"
            ));
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByStatus(Boolean active) {
        return productRepository.findByActive(active);
    }

    public Product updateProduct(Long id, Product updatedProduct) {

        Product existing = getProductById(id);

        if (!existing.getSku().equals(updatedProduct.getSku())
                && productRepository.existsBySku(updatedProduct.getSku())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product SKU already exists"
            );
        }

        existing.setName(updatedProduct.getName());
        existing.setSku(updatedProduct.getSku());
        existing.setDescription(updatedProduct.getDescription());
        existing.setCategory(updatedProduct.getCategory());
        existing.setPrice(updatedProduct.getPrice());
        existing.setActive(updatedProduct.getActive());

        return productRepository.save(existing);
    }

    public void deleteProduct(Long id) {

        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
