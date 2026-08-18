package com.example.amazon.repository;

import com.example.amazon.entity.PcSpec;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PcSpecRepository extends JpaRepository<PcSpec, String> {
}