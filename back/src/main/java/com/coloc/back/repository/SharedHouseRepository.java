package com.coloc.back.repository;

import com.coloc.back.entity.SharedHouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedHouseRepository extends JpaRepository<SharedHouse, Long> {

    Optional<SharedHouse> findByInvitationCode(String invitationCode);

    boolean existsByInvitationCode(String invitationCode);
}