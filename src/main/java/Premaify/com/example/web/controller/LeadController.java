package Premaify.com.example.web.controller;

import Premaify.com.example.web.model.Lead;
import Premaify.com.example.web.repository.LeadRepository;
import Premaify.com.example.web.service.LeadService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {
    private final LeadRepository leadRepository;
    private final LeadService leadService;

    public LeadController(LeadRepository leadRepository, LeadService leadService) {
        this.leadRepository = leadRepository;
        this.leadService = leadService;
    }

    @GetMapping
    public List<Lead> listLeads(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String datePreset,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return leadService.listLeads(status, datePreset, startDate, endDate);
    }

    @PostMapping
    public Lead createLead(@RequestBody Lead lead) {
        return leadService.saveLead(lead);
    }

    @PatchMapping("/{id}")
    public Lead patchLead(@PathVariable Long id, @RequestBody Lead lead) {
        return leadService.updateLead(id, lead);
    }

    @PutMapping("/{id}/convert")
    public Lead convertLead(@PathVariable Long id) {
        return leadService.convertLead(id);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void deleteAllLeads() {
        leadRepository.deleteAll();
    }
}
