package com.apexmetrics.telemetry.repository;

import com.apexmetrics.telemetry.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
