package Premaify.com.example.web.repository;

import Premaify.com.example.web.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    List<Lead> findAllByOrderByIdDesc();
}
