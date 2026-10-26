package com.Ledjr.controller;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Ledjr.dto.CreateTransactionRequest;
import com.Ledjr.dto.TransactionResponse;
import com.Ledjr.entity.AppUser;
import com.Ledjr.entity.Transaction;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.TransactionService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AppUserService appUserService;

    public TransactionController(TransactionService transactionService, AppUserService appUserService){
        this.transactionService = transactionService;
        this.appUserService = appUserService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateTransactionRequest request){

        if (request.amount() == null || request.amount().signum() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount must be greater than 0.");
        }
        if (!"income".equals(request.type()) && !"expense".equals(request.type())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Type must be income or expense.");
        }

        Optional<AppUser> user = appUserService.findById(request.userId());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        LocalDate date = request.txnDate() != null ? request.txnDate() : LocalDate.now();

        Transaction saved = transactionService.newTransaction(user.get(), null, null, request.type(), request.amount(), date, request.description());

        TransactionResponse response = new TransactionResponse(saved.getId(), saved.getType(), saved.getAmount(), saved.getTxnDate(), saved.getDescription());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
