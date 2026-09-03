package com.incidentmanager.agent.web;

import com.incidentmanager.agent.reporting.IncidentSummary;
import com.incidentmanager.agent.reporting.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/reports/today")
    public IncidentSummary today() {
        return reportService.summarize(LocalDate.now());
    }

    @GetMapping("/api/reports/{date}")
    public IncidentSummary byDate(@PathVariable LocalDate date) {
        return reportService.summarize(date);
    }
}
