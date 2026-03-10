package com.ssafy.amagetdone.domain.member.model.repository;

import com.ssafy.amagetdone.domain.member.model.entity.Setting;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, Integer> {
}
