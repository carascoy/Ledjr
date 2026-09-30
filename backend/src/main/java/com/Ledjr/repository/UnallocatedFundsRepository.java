package com.Ledjr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.UnallocatedFunds;

/**
 * JpaRepository<UnallocatedFunds, Long> gives these for free, no code needed:
 * save(unallocatedFunds) — insert or update
 * findById(id)           — get one row by its id
 * findAll()                — get every row
 * deleteById(id)           — delete one row
 * count()                  — how many rows exist
 */
public interface UnallocatedFundsRepository extends JpaRepository<UnallocatedFunds, Long> {

    /** The one unallocated-funds pool belonging to a specific user. */
    Optional<UnallocatedFunds> findByUserId(Long userId);

}
