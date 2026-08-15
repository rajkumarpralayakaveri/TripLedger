package com.rkdevstudios.tripledger.contribution.persistence;

import com.rkdevstudios.tripledger.contribution.domain.PaymentProof;
import com.rkdevstudios.tripledger.contribution.domain.PaymentProofRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPaymentProofRepository extends JpaRepository<PaymentProof, String>, PaymentProofRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentProof p where p.id = :id")
    Optional<PaymentProof> findByIdWithLock(@Param("id") String id);
}
