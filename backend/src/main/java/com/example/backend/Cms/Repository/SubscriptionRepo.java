package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByUserIdOrderByEndAtDesc(UUID userId);

    /**
     * Obunalar ro'yxati (ТЗ §71, §107).
     *
     * <h2>Nega bu kerak bo'ldi</h2>
     * Dashboard obuna daromadini ko'rsatardi, lekin admin QAYSI obunalar
     * bu raqamni bergani ko'ra olmasdi — endpoint ham, sahifa ham yo'q
     * edi. Ya'ni raqamni tekshirib bo'lmasdi.
     *
     * <h2>Filtrlar birga ishlaydi</h2>
     * Har bir shart {@code :param is null or ...} ko'rinishida — biri
     * ikkinchisini bekor qilmaydi (§34 va §59 dagi xato takrorlanmasin).
     *
     * {@code active} — hozir amal qilayotganlar: bekor qilinmagan va
     * muddati tugamagan.
     */
    /*
     * ⚠️ `cast(:param as ...)` — bezak emas, MAJBURIY.
     *
     * `:param is null` shartida parametr boshqa hech qayerda
     * ishlatilmaydi: Hibernate har bir nomlangan parametr uchun
     * ALOHIDA pozitsion `?` yasaydi, ya'ni `$N` faqat `is null`
     * kontekstida qoladi. PostgreSQL 18 uchun bunday parametrning
     * turi noma'lum va u so'rovni butunlay rad etadi:
     *
     *   ERROR: could not determine data type of parameter $N
     *
     * Xato FAQAT filtr bo'sh bo'lganda chiqadi — ya'ni sahifa
     * birinchi marta, filtrsiz ochilganda. Bu esa eng ko'p
     * uchraydigan holat.
     *
     * `String` ga o'girish `is null` ma'nosini o'zgartirmaydi
     * (`cast(null as ...)` baribir null), lekin parametrga tur
     * beradi. Haqiqiy solishtirishda esa parametr o'z turida
     * qoladi — u alohida `?`.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            select s from Subscription s
            left join fetch s.user u
            left join fetch s.tariff t
            where (:source is null or s.source = :source)
              and (:tariffId is null or t.id = :tariffId)
              and (cast(:from as LocalDateTime) is null or s.startAt >= :from)
              and (cast(:to as LocalDateTime) is null or s.startAt <= :to)
              and (cast(:active as String) is null
                   or (:active = true  and s.revokedAt is null and s.endAt > :now)
                   or (:active = false and (s.revokedAt is not null or s.endAt <= :now)))
              and (:q is null
                   or lower(u.phone) like lower(concat('%', :q, '%'))
                   or lower(u.name) like lower(concat('%', :q, '%')))
            order by s.startAt desc
            """,
            countQuery = """
            select count(s) from Subscription s
            left join s.user u
            left join s.tariff t
            where (:source is null or s.source = :source)
              and (:tariffId is null or t.id = :tariffId)
              and (cast(:from as LocalDateTime) is null or s.startAt >= :from)
              and (cast(:to as LocalDateTime) is null or s.startAt <= :to)
              and (cast(:active as String) is null
                   or (:active = true  and s.revokedAt is null and s.endAt > :now)
                   or (:active = false and (s.revokedAt is not null or s.endAt <= :now)))
              and (:q is null
                   or lower(u.phone) like lower(concat('%', :q, '%'))
                   or lower(u.name) like lower(concat('%', :q, '%')))
            """)
    org.springframework.data.domain.Page<Subscription> search(
            @org.springframework.data.repository.query.Param("source")
            com.example.backend.Cms.Enums.SubscriptionSource source,
            @org.springframework.data.repository.query.Param("tariffId") Long tariffId,
            @org.springframework.data.repository.query.Param("active") Boolean active,
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to,
            @org.springframework.data.repository.query.Param("q") String q,
            @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Obuna daromadi (§45).
     *
     * ⚠️ FAQAT haqiqiy xaridlar: {@code ADMIN_GIFT} obunalarida
     * {@code paidAmount} bo'sh va ular hisobga kirmaydi — sovg'a daromad
     * emas.
     *
     * Ilgari bu jamlanma {@code findAll()} bilan Java'da hisoblanardi:
     * har bir dashboard ochilishida BUTUN obunalar jadvali xotiraga
     * tortilardi.
     */
    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(s.paidAmount), 0) from Subscription s "
                    + "where s.paidAmount is not null and s.revokedAt is null")
    java.math.BigDecimal totalPaidAmount();

    /** Bitta tarif bo'yicha daromad (ТЗ §47 filtri). */
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(s.paidAmount), 0) from Subscription s
            where s.paidAmount is not null and s.revokedAt is null
              and s.tariff.id = :tariffId
            """)
    java.math.BigDecimal totalPaidAmountByTariff(
            @org.springframework.data.repository.query.Param("tariffId") Long tariffId);

    /**
     * Obuna daromadi — kunlik (ТЗ §48 grafigi).
     *
     * ⚠️ Sovg'a obunalar ({@code paidAmount is null}) kirmaydi: ular
     * grafikni ko'tarib ko'rsatardi, lekin hech qanday pul kelmagan.
     */
    @org.springframework.data.jpa.repository.Query("""
            select cast(s.startAt as date) as day, coalesce(sum(s.paidAmount), 0) as value
            from Subscription s
            where s.paidAmount is not null and s.revokedAt is null
              and s.startAt >= :from
            group by cast(s.startAt as date)
            order by cast(s.startAt as date)
            """)
    List<DayMoney> revenueByDay(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from);

    /** Kunlik pul — grafik uchun proyeksiya. */
    interface DayMoney {
        java.time.LocalDate getDay();
        java.math.BigDecimal getValue();
    }
}
