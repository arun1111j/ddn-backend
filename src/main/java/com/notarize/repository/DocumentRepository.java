package com.notarize.repository;

import com.notarize.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByDocumentHash(String documentHash);
    boolean existsByDocumentHash(String documentHash);
    List<Document> findByOwnerAddress(String ownerAddress);
    List<Document> findByNotarized(boolean notarized);
    List<Document> findByNotaryAddressesContaining(String notaryAddress);
    List<Document> findByLastVerifiedBeforeOrLastVerifiedIsNull(LocalDateTime dateTime);

    Optional<Document> findByIpfsHash(String ipfsCid);
}