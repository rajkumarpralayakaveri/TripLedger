package com.rkdevstudios.tripledger.settlement.persistence;

import com.rkdevstudios.tripledger.settlement.domain.SettlementTransaction;
import com.rkdevstudios.tripledger.settlement.domain.SettlementTransactionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaSettlementTransactionRepository extends JpaRepository<SettlementTransaction, String>, SettlementTransactionRepository {
}
