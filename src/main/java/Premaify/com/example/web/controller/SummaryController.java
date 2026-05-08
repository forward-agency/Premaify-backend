package Premaify.com.example.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SummaryController {
    private final DashboardController dashboardController;

    public SummaryController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return dashboardController.summary();
    }
}
