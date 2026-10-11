package com.coloc.back.repository;

import com.coloc.back.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    List<PollVote> findByOption_Note_Id(Long noteId);

    List<PollVote> findByRoommate_Id(Long roommateId);

    boolean existsByOption_IdAndRoommate_Id(Long optionId, Long roommateId);

    long countByOption_Id(Long optionId);

    void deleteByRoommate_IdAndOption_Note_Id(
        Long roommateId,
        Long noteId
    );
}