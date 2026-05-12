package Premaify.com.example.web.repository;

import Premaify.com.example.web.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, String> {
    long countByStockQuantityGreaterThan(int stockQuantity);

    @Query("SELECT COALESCE(SUM(p.stockQuantity), 0) FROM Product p")
    Long sumStockQuantity();
}
