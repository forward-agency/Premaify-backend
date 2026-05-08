package Premaify.com.example.web.controller;

import Premaify.com.example.web.model.OrderLog;
import Premaify.com.example.web.repository.OrderLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderLogRepository orderLogRepository;

    public OrderController(OrderLogRepository orderLogRepository) {
        this.orderLogRepository = orderLogRepository;
    }

    @GetMapping
    public List<OrderLog> listOrders() {
        return orderLogRepository.findAllByOrderByIdDesc();
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void deleteAllOrders() {
        orderLogRepository.deleteAll();
    }
}
