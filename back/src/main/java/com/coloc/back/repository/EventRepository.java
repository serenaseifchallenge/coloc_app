package com.coloc.back.repository;

import com.coloc.back.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findBySharedHouseIdOrderByStartDateAscStartTimeAsc(Long sharedHouseId);
}