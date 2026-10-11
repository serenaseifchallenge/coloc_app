package com.coloc.back.repository;

import com.coloc.back.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findBySharedHouse_IdOrderByCreatedAtDesc(Long sharedHouseId);

    List<Note> findByCreator_Id(Long creatorId);
}