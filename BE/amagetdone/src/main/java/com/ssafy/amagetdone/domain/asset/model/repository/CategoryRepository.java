package com.ssafy.amagetdone.domain.asset.model.repository;

import com.ssafy.amagetdone.domain.asset.model.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
