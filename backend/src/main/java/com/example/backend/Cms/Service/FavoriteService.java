package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.UserFavorite;
import com.example.backend.Cms.Enums.FavoriteType;
import com.example.backend.Cms.Repository.UserFavoriteRepo;
import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Sevimlilar ro'yxati.
 *
 * <h2>Nima uchun server tomonda</h2>
 * Ilgari ro'yxat faqat telefonda saqlanardi: ilova qayta o'rnatilsa
 * yo'qolardi, ikkinchi qurilmada bo'sh bo'lardi. Foydalanuvchi buni
 * ma'lumot yo'qolishi deb his qiladi.
 *
 * <h2>⚠️ Barcha amallar IDEMPOTENT</h2>
 * Mobil tarmoq ishonchsiz: so'rov yuborilib, javob yo'qolishi mumkin
 * va klient uni takrorlaydi. Qo'shish ikki marta kelsa xato bermaydi,
 * o'chirish mavjud bo'lmagan yozuv uchun ham xato bermaydi.
 *
 * Aks holda «yurakcha» tugmasi tarmoq sifati yomon joyda xato
 * ko'rsatardi — foydalanuvchi uchun esa hech narsa buzilmagan.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteRepo repo;

    /**
     * Bitta foydalanuvchi bitta turda nechta sevimli saqlay oladi.
     *
     * <h2>⚠️ Nega chegara kerak</h2>
     * {@code targetId} ning mavjudligi TEKSHIRILMAYDI (sabab —
     * {@code UserFavorite} izohida). Ya'ni chegarasiz klient
     * xohlagancha qator yozib, jadvalni to'ldirib tashlashi mumkin
     * edi.
     *
     * 500 — haqiqiy foydalanishdan ancha yuqori: bu «yetarli emas»
     * degan shikoyat kelmaydigan, lekin suiiste'molni to'xtatadigan
     * chegara.
     */
    public static final int MAX_PER_TYPE = 500;

    /**
     * Bitta so'rovda nechta qo'shish mumkin.
     *
     * ⚠️ {@code public} — bu KLIENT shartnomasining bir qismi.
     * Kirishdan keyingi birlashtirishda telefondagi ro'yxat undan
     * uzun bo'lishi mumkin va klient uni bo'laklashi kerak.
     */
    public static final int MAX_BATCH = 200;

    @Transactional(readOnly = true)
    public List<Long> list(User user, FavoriteType type) {
        return repo.findTargetIds(user.getId(), type);
    }

    /**
     * Ro'yxatga qo'shadi va YANGILANGAN to'liq ro'yxatni qaytaradi.
     *
     * <h2>⚠️ Nega to'liq ro'yxat qaytariladi</h2>
     * Klient bitta so'rov bilan holatini serverdagisi bilan tenglaydi.
     * Faqat «ok» qaytarilsa, u o'z nusxasini o'zi yangilashi kerak
     * bo'lardi — va ikki tomon bir-biridan ajralib ketishi uchun
     * bitta yo'qolgan javob yetarli.
     *
     * <h2>Kirishdan keyin birlashtirish ham SHU metod</h2>
     * Odam tizimga kirmasdan ham yurakcha bosa oladi — ro'yxat
     * telefonda yig'iladi. Kirgach klient o'sha ro'yxatni shu yerga
     * yuboradi va u serverdagisi bilan BIRLASHADI.
     *
     * ⚠️ Almashtirilmaydi, birlashtiriladi. Almashtirilsa boshqa
     * qurilmada belgilangan sevimlilar jimgina yo'qolardi.
     */
    @Transactional
    public List<Long> add(User user, FavoriteType type, List<Long> targetIds) {
        List<Long> wanted = clean(targetIds);
        if (wanted.isEmpty()) {
            return list(user, type);
        }
        if (wanted.size() > MAX_BATCH) {
            throw BusinessException.validation(
                    "Bir so'rovda ko'pi bilan " + MAX_BATCH + " ta qo'shish mumkin");
        }

        Set<Long> existing = new LinkedHashSet<>(repo.findTargetIds(user.getId(), type));

        List<UserFavorite> fresh = new ArrayList<>();
        for (Long targetId : wanted) {
            // ⚠️ Allaqachon borini qayta yozmaymiz: `created_at`
            // o'zgarib, ro'yxat tartibi sababsiz aralashib ketardi.
            if (existing.contains(targetId)) {
                continue;
            }
            if (existing.size() + fresh.size() >= MAX_PER_TYPE) {
                throw BusinessException.validation(
                        "Sevimlilar ro'yxatida ko'pi bilan " + MAX_PER_TYPE + " ta element bo'ladi");
            }
            fresh.add(UserFavorite.builder()
                    .user(user)
                    .type(type)
                    .targetId(targetId)
                    .build());
        }

        if (!fresh.isEmpty()) {
            repo.saveAll(fresh);
        }
        return repo.findTargetIds(user.getId(), type);
    }

    /**
     * Ro'yxatdan olib tashlaydi va YANGILANGAN ro'yxatni qaytaradi.
     *
     * ⚠️ Mavjud bo'lmagan yozuv uchun xato BERILMAYDI. Klient
     * so'rovni takrorlagan bo'lishi mumkin va foydalanuvchi uchun
     * natija bir xil: element ro'yxatda yo'q.
     */
    @Transactional
    public List<Long> remove(User user, FavoriteType type, Long targetId) {
        if (targetId != null) {
            repo.deleteOne(user.getId(), type, targetId);
        }
        return repo.findTargetIds(user.getId(), type);
    }

    /**
     * Kiruvchi ro'yxatni tozalaydi.
     *
     * ⚠️ {@code null} va takrorlar olib tashlanadi, tartib SAQLANADI.
     * Takror kelsa unikal indeks xato berardi va butun so'rov
     * yiqilardi — birlashtirishda esa takror odatiy hol: telefondagi
     * ro'yxatning bir qismi serverda allaqachon bor.
     */
    private List<Long> clean(List<Long> targetIds) {
        if (targetIds == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(
                targetIds.stream().filter(Objects::nonNull).toList()));
    }
}
