package com.example.policyassistant.fragment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DocumentFragmentRepository extends JpaRepository<DocumentFragment, Long> {

    List<DocumentFragment> findByFileIdIn(Collection<String> fileIds);
}
