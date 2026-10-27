package com.Ledjr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.UnallocatedFunds;

public interface UnallocatedFundsRepository extends JpaRepository<UnallocatedFunds, Long> {

    /** each user only has one of these */
    Optional<UnallocatedFunds> findByUserId(Long userId);

}
