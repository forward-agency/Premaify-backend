package Premaify.com.example.web.repository;

import Premaify.com.example.web.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
