package com.rkdevstudios.tripledger.settlement.persistence;

import com.rkdevstudios.tripledger.settlement.domain.SettlementSession;
import com.rkdevstudios.tripledger.settlement.domain.SettlementSessionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaSettlementSessionRepository extends JpaRepository<SettlementSession, String>, SettlementSessionRepository {
}
