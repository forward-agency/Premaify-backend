package Premaify.com.example.web.controller;

import Premaify.com.example.web.repository.LeadRepository;
import Premaify.com.example.web.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;

    public DashboardController(LeadRepository leadRepository, ProductRepository productRepository) {
        this.leadRepository = leadRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        long totalProducts = productRepository.count();
        long totalLeads = leadRepository.count();
        long activeListings = productRepository.findAll().stream()
                .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() > 0)
                .count();
        long inventoryCount = productRepository.findAll().stream()
                .mapToLong(product -> product.getStockQuantity() == null ? 0 : product.getStockQuantity())
                .sum();

        return Map.of(
                "totalProducts", totalProducts,
                "totalLeads", totalLeads,
                "activeListings", activeListings,
                "inventoryCount", inventoryCount
        );
    }
}
