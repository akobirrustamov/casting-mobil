package com.example.backend.Repository;

import com.example.backend.Entity.CastingUser;
import com.example.backend.Entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface MessageRepo extends JpaRepository<Message, Long> {

    @Query(value = "select * from message where casting_user_id=:castingUserId", nativeQuery = true)
    Optional<Message> findByCastingUserId(Integer castingUserId);

    @Modifying
    @Transactional
    @Query(value = "delete from message where casting_user_id=:castingUserId", nativeQuery = true)
    void deleteByCastinguserId(Integer castingUserId);
}
