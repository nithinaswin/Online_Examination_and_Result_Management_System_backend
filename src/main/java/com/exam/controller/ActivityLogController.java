package com.exam.controller;

import com.exam.dto.ApiResponse;
import com.exam.dto.PageResponse;
import com.exam.entity.ActivityLog;
import com.exam.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ActivityLog>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLog> logsPage = activityLogService.getLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Activity logs fetched", new PageResponse<>(logsPage)));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<ActivityLog>>> getRecentLogs() {
        List<ActivityLog> logs = activityLogService.getRecentLogs();
        return ResponseEntity.ok(ApiResponse.success("Recent activity logs fetched", logs));
    }
}
