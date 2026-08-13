package com.rkdevstudios.tripledger.expense.application;

import com.rkdevstudios.tripledger.expense.domain.ExpenseCategory;
import com.rkdevstudios.tripledger.expense.domain.ExpenseCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final ExpenseCategoryRepository categoryRepository;

    public CategoryService(ExpenseCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void initializeSystemCategories() {
        List<ExpenseCategory> systems = categoryRepository.findByIsSystemCategoryTrue();
        if (systems.isEmpty()) {
            createSystemCategory("Food", "restaurant", "#FF9800");
            createSystemCategory("Lodging", "hotel", "#9C27B0");
            createSystemCategory("Transport", "directions_car", "#2196F3");
            createSystemCategory("Shopping", "shopping_bag", "#E91E63");
            createSystemCategory("Leisure", "sports_bar", "#4CAF50");
            createSystemCategory("Fuel", "local_gas_station", "#795548");
            createSystemCategory("Others", "more_horiz", "#607D8B");
        }
    }

    private void createSystemCategory(String name, String icon, String color) {
        ExpenseCategory cat = new ExpenseCategory(
                UUID.randomUUID().toString(),
                null,
                name,
                icon,
                color,
                true
        );
        categoryRepository.save(cat);
    }

    @Transactional
    public ExpenseCategory createCustomCategory(
            String workspaceId,
            String name,
            String icon,
            String color
    ) {
        ExpenseCategory cat = new ExpenseCategory(
                UUID.randomUUID().toString(),
                workspaceId,
                name,
                icon,
                color,
                false
        );
        return categoryRepository.save(cat);
    }

    @Transactional
    public ExpenseCategory toggleCategoryActive(String categoryId, boolean active) {
        ExpenseCategory cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        cat.setActive(active);
        return categoryRepository.save(cat);
    }

    public List<ExpenseCategory> getAvailableCategories(String workspaceId) {
        // Fetch all system categories + custom workspace categories
        List<ExpenseCategory> list = categoryRepository.findByIsSystemCategoryTrue();
        if (workspaceId != null) {
            list.addAll(categoryRepository.findByWorkspaceId(workspaceId));
        }
        return list;
    }
}
