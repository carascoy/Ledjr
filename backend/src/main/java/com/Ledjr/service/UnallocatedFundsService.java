package com.Ledjr.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.UnallocatedFunds;
import com.Ledjr.repository.UnallocatedFundsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service 
public class UnallocatedFundsService {

    private final UnallocatedFundsRepository unallocatedFundsRepository;
    private static final Logger log = LoggerFactory.getLogger(UnallocatedFundsService.class);

    public UnallocatedFundsService(UnallocatedFundsRepository unallocatedFundsRepository){
        this.unallocatedFundsRepository = unallocatedFundsRepository;
    }

    public UnallocatedFunds createPool(AppUser user, BigDecimal startingBalance){
        UnallocatedFunds pool = new UnallocatedFunds();
        pool.setUser(user);
        pool.setBalance(startingBalance);
        unallocatedFundsRepository.save(pool);
        log.info("Created unallocated funds pool for user id# {} with starting balance {}", user.getId(), startingBalance);
        return pool;
    }

    public Optional<UnallocatedFunds> findByUserId(Long userId){
        return unallocatedFundsRepository.findByUserId(userId);
    }


}
