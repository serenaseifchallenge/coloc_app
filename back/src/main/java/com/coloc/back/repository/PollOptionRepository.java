package com.coloc.back.repository;

import com.coloc.back.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByNote_IdOrderByIdAsc(Long noteId);
}