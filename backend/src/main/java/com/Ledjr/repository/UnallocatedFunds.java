package com.Ledjr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UnallocatedFunds extends JpaRepository<UnallocatedFunds, Long>{
    Optional<UnallocatedFunds> findByUserId(Long userId);
}
