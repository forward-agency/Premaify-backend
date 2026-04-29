package Premaify.com.example.web.service;

import Premaify.com.example.web.model.Lead;
import Premaify.com.example.web.model.OrderLog;
import Premaify.com.example.web.model.Product;
import Premaify.com.example.web.repository.LeadRepository;
import Premaify.com.example.web.repository.OrderLogRepository;
import Premaify.com.example.web.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;
    private final OrderLogRepository orderLogRepository;

    public LeadService(
            LeadRepository leadRepository,
            ProductRepository productRepository,
            OrderLogRepository orderLogRepository
    ) {
        this.leadRepository = leadRepository;
        this.productRepository = productRepository;
        this.orderLogRepository = orderLogRepository;
    }

    @Transactional
    public Lead saveLead(Lead lead) {
        if (lead.getStatus() == null || lead.getStatus().isBlank()) {
            lead.setStatus("New");
        }
        if (lead.getPreviousPeriod() == null) {
            lead.setPreviousPeriod(false);
        }
        return leadRepository.save(lead);
    }

    @Transactional
    public Lead updateLead(Long leadId, Lead patch) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));

        if (patch.getName() != null) lead.setName(patch.getName());
        if (patch.getPhone() != null) lead.setPhone(patch.getPhone());
        if (patch.getLocation() != null) lead.setLocation(patch.getLocation());
        if (patch.getLaptop() != null) lead.setLaptop(patch.getLaptop());
        if (patch.getProductId() != null) lead.setProductId(patch.getProductId());
        if (patch.getCategory() != null) lead.setCategory(patch.getCategory());
        if (patch.getPaymentType() != null) lead.setPaymentType(patch.getPaymentType());
        if (patch.getSource() != null) lead.setSource(patch.getSource());
        if (patch.getStatus() != null) lead.setStatus(patch.getStatus());
        if (patch.getNotes() != null) lead.setNotes(patch.getNotes());
        if (patch.getPreviousPeriod() != null) lead.setPreviousPeriod(patch.getPreviousPeriod());

        return leadRepository.save(lead);
    }

    @Transactional
    public Lead convertLead(Long leadId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found"));

        if (!"Converted".equalsIgnoreCase(lead.getStatus())) {
            productRepository.findById(lead.getProductId()).ifPresent(product -> {
                Integer currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                product.setStockQuantity(Math.max(0, currentStock - 1));
                productRepository.save(product);
            });
        }

        lead.setStatus("Converted");
        Lead savedLead = leadRepository.save(lead);

        if (!orderLogRepository.existsByLeadId(leadId)) {
            OrderLog orderLog = new OrderLog();
            orderLog.setLeadId(savedLead.getId());
            orderLog.setProduct(savedLead.getLaptop());
            orderLog.setCustomer(savedLead.getName() == null || savedLead.getName().isBlank() ? "Not shared" : savedLead.getName());
            orderLog.setPaymentType(savedLead.getPaymentType());
            orderLogRepository.save(orderLog);
        }

        return savedLead;
    }

    @Transactional
    public Product updateStock(String productId, Integer stockQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setStockQuantity(Math.max(0, stockQuantity));
        return productRepository.save(product);
    }
}
