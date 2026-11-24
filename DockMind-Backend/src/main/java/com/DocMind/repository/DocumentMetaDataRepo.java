package com.DocMind.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.DocMind.dto.DocumentMetaDataDto;
import com.DocMind.entity.DocumentMetaData;
import com.DocMind.entity.DocumentStatus;
import com.DocMind.entity.User;

@Repository
public interface DocumentMetaDataRepo extends JpaRepository<DocumentMetaData,String>{

	List<DocumentMetaData> findByStatus(DocumentStatus status);

    List<DocumentMetaData> findAllByOrderByCreatedAtDesc();


    List<DocumentMetaData> findByUserOrderByCreatedAtDesc(User user);

    Optional<DocumentMetaData> findByIdAndUser(String id, User user);

}
