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
        long activeListings = productRepository.countByStockQuantityGreaterThan(0);
        long inventoryCount = productRepository.sumStockQuantity() == null ? 0 : productRepository.sumStockQuantity();

        return Map.of(
                "totalProducts", totalProducts,
                "totalLeads", totalLeads,
                "activeListings", activeListings,
                "inventoryCount", inventoryCount
        );
    }
}
