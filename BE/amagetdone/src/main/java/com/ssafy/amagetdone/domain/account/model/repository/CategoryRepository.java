package com.ssafy.amagetdone.domain.account.model.repository;

import com.ssafy.amagetdone.domain.account.model.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
