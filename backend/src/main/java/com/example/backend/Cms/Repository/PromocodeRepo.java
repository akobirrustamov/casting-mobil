package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Promocode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromocodeRepo extends JpaRepository<Promocode, Long> {

    Optional<Promocode> findByCode(String code);

    boolean existsByCode(String code);

    List<Promocode> findAllByOrderByCreatedAtDesc();

    /**
     * Kodni QULFLAB oladi — {@code select ... for update}.
     *
     * <h2>⚠️ Nima uchun oddiy {@code findByCode} yetmaydi</h2>
     * Umumiy limit ({@code maxRedemptions}) shunday tekshiriladi: sanash,
     * solishtirish, yozish. Ikkita so'rov oxirgi o'ringa bir vaqtda kelsa,
     * ikkalasi ham «9 dan 10» ni ko'radi va ikkalasi ham o'tadi — limit
     * 11 bo'lardi. Qulf ikkinchisini birinchisi tugaguncha kuttiradi va u
     * endi «10 dan 10» ni ko'radi.
     *
     * Bu tranzaksiya ichida chaqirilishi SHART — qulf tranzaksiya bilan
     * birga bo'shaydi.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promocode p where p.code = :code")
    Optional<Promocode> lockByCode(@Param("code") String code);
}
