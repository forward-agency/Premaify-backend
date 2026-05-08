package Premaify.com.example.web.service;

import Premaify.com.example.web.model.Lead;
import Premaify.com.example.web.model.OrderLog;
import Premaify.com.example.web.model.Product;
import Premaify.com.example.web.repository.LeadRepository;
import Premaify.com.example.web.repository.OrderLogRepository;
import Premaify.com.example.web.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeadService {
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;
    private final OrderLogRepository orderLogRepository;
    private final EmailService emailService;

    public LeadService(
            LeadRepository leadRepository,
            ProductRepository productRepository,
            OrderLogRepository orderLogRepository,
            EmailService emailService
    ) {
        this.leadRepository = leadRepository;
        this.productRepository = productRepository;
        this.orderLogRepository = orderLogRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<Lead> listLeads(String status, String datePreset, LocalDate startDate, LocalDate endDate) {
        Specification<Lead> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (status != null && !status.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("status")), status.toLowerCase())
            );
        }
        if (startDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDate)
            );
        }
        if (endDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("date"), endDate)
            );
        }
        if (startDate == null && endDate == null && (datePreset == null || datePreset.isBlank() || "recent".equalsIgnoreCase(datePreset))) {
            LocalDateTime threshold = LocalDateTime.now().minusHours(48);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), threshold),
                            criteriaBuilder.and(
                                    criteriaBuilder.isNull(root.get("createdAt")),
                                    criteriaBuilder.greaterThanOrEqualTo(root.get("date"), threshold.toLocalDate())
                            )
                    )
            );
        }

        return leadRepository.findAll(
                specification,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "date"))
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    @Transactional
    public Lead saveLead(Lead lead) {
        if (lead.getStatus() == null || lead.getStatus().isBlank()) {
            lead.setStatus("New");
        }
        if (lead.getType() == null || lead.getType().isBlank()) {
            lead.setType("Order".equalsIgnoreCase(lead.getActionType()) ? "Order" : "Enquiry");
        }
        if (lead.getPreviousPeriod() == null) {
            lead.setPreviousPeriod(false);
        }
        if (lead.getCreatedAt() == null) {
            lead.setCreatedAt(LocalDateTime.now());
        }
        if (lead.getDate() == null) {
            lead.setDate(lead.getCreatedAt().toLocalDate());
        }
        Lead savedLead = leadRepository.save(lead);
        if ("Order".equalsIgnoreCase(savedLead.getType()) && !orderLogRepository.existsByLeadId(savedLead.getId())) {
            OrderLog orderLog = new OrderLog();
            orderLog.setLeadId(savedLead.getId());
            orderLog.setProduct(savedLead.getLaptop());
            orderLog.setCustomer(savedLead.getName() == null || savedLead.getName().isBlank() ? "Not shared" : savedLead.getName());
            orderLog.setType("Order");
            orderLogRepository.save(orderLog);
        }
        if ("Order".equalsIgnoreCase(savedLead.getType())) {
            emailService.sendOrderNotification(savedLead);
        } else {
            emailService.sendEnquiryNotification(savedLead);
        }
        return savedLead;
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
        if (patch.getSource() != null) lead.setSource(patch.getSource());
        if (patch.getActionType() != null) lead.setActionType(patch.getActionType());
        if (patch.getType() != null) lead.setType(patch.getType());
        if (patch.getFullAddress() != null) lead.setFullAddress(patch.getFullAddress());
        if (patch.getDistrict() != null) lead.setDistrict(patch.getDistrict());
        if (patch.getPincode() != null) lead.setPincode(patch.getPincode());
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
            orderLog.setType("Order");
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
