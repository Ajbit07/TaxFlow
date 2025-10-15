package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.SearchResult;
import com.taxflow.application.service.SearchService;
import com.taxflow.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    public ApiResponse<List<SearchResult>> global(@PathVariable UUID businessId, @RequestParam("q") String query) {
        return ApiResponse.ok(searchService.global(businessId, query));
    }
}
