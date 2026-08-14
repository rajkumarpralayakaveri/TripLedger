package com.rkdevstudios.tripledger.expense.api;

import com.rkdevstudios.tripledger.common.ApiResponse;
import com.rkdevstudios.tripledger.common.ApiRoutes;
import com.rkdevstudios.tripledger.expense.application.CategoryService;
import com.rkdevstudios.tripledger.expense.domain.ExpenseCategory;
import com.rkdevstudios.tripledger.identity.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/workspaces/{id}/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new SecurityException("User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(@PathVariable("id") String id) {
        getAuthenticatedUser();
        List<ExpenseCategory> list = categoryService.getAvailableCategories(id);
        List<CategoryResponse> responses = list.stream().map(c -> new CategoryResponse(
                c.getId(),
                c.getWorkspaceId(),
                c.getName(),
                c.getIcon(),
                c.getColor(),
                c.isActive(),
                c.isSystemCategory()
        )).toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseCategory>> createCustomCategory(
            @PathVariable("id") String id,
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        getAuthenticatedUser();
        ExpenseCategory cat = categoryService.createCustomCategory(
                id,
                request.name(),
                request.icon(),
                request.color()
        );
        return ResponseEntity.ok(ApiResponse.success(cat));
    }

    @PutMapping("/{categoryId}/toggle")
    public ResponseEntity<ApiResponse<ExpenseCategory>> toggleCategory(
            @PathVariable("id") String id,
            @PathVariable("categoryId") String categoryId,
            @RequestParam boolean active
    ) {
        getAuthenticatedUser();
        ExpenseCategory cat = categoryService.toggleCategoryActive(categoryId, active);
        return ResponseEntity.ok(ApiResponse.success(cat));
    }
}
