package com.notarize.repository;

import com.notarize.model.Notary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotaryRepository extends JpaRepository<Notary, Long> {
    boolean existsByNotaryAddress(String notaryAddress);
    Optional<Notary> findByNotaryAddress(String notaryAddress);
    List<Notary> findByActiveTrue(); // Add this method
}