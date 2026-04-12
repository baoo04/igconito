package com.cinema.payment.repository;

import com.cinema.payment.entity.PaymentTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {}
