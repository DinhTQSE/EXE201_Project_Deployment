package com.vsign.backend.dictionary.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntryEntity, Integer> {
    List<DictionaryEntryEntity> findByPublishedTrue();
}
