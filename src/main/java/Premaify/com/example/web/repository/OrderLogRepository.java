package Premaify.com.example.web.repository;

import Premaify.com.example.web.model.OrderLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderLogRepository extends JpaRepository<OrderLog, Long> {
    boolean existsByLeadId(Long leadId);

    List<OrderLog> findAllByOrderByIdDesc();
}
