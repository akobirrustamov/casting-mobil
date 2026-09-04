package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.PromocodeRedemption;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromocodeRedemptionRepo extends JpaRepository<PromocodeRedemption, Long> {

    long countByPromocodeId(Long promocodeId);

    boolean existsByPromocodeIdAndUserId(Long promocodeId, UUID userId);

    /** Foydalanuvchining o'zi ishlatgan kodlari — ilovadagi «Promokodlarim». */
    @EntityGraph(attributePaths = {"promocode", "subscription"})
    List<PromocodeRedemption> findAllByUserIdOrderByRedeemedAtDesc(UUID userId);

    /** Admin: kim ishlatgan. */
    @EntityGraph(attributePaths = {"user", "subscription"})
    List<PromocodeRedemption> findAllByPromocodeIdOrderByRedeemedAtDesc(Long promocodeId);
}
