package com.example.backend.Cms.Service;

import com.example.backend.Cms.Dto.CastingApplicationRequest;
import com.example.backend.Cms.Dto.MyCastingApplicationDto;
import com.example.backend.Entity.Attachment;
import com.example.backend.Entity.CastingUser;
import com.example.backend.Entity.User;
import com.example.backend.Repository.AttachmentRepo;
import com.example.backend.Repository.CastingUserRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Mobil ilovadan casting anketasi.
 *
 * <h2>Nega eski jadval, yangi jadval emas</h2>
 * Anketani ko'rib chiqish, narx qo'yish, to'lovni belgilash va saytga
 * chiqarish — hammasi eski admin saytida ({@code frontend/src/bot-admin})
 * va {@code casting_user} ustida ishlaydi. Ilova anketasi o'sha jadvalga
 * yozilsa, admin uni bot anketasi bilan BIR ro'yxatda ko'radi va hech
 * narsa qayta yozilmaydi. Farqi bitta ustunda: {@code app_user_id} (V40).
 *
 * <h2>⚠️ Eski {@code POST /casting-user} dan farqi</h2>
 * U klientdan {@code status}, {@code price}, {@code age} ni ham oladi va
 * mavjud bo'lmagan rasm id'ini jimgina tashlab yuboradi. Bu yerda holat va
 * narxni faqat server qo'yadi, yosh tug'ilgan kundan hisoblanadi, noto'g'ri
 * rasm esa aniq xato beradi — odam anketasi rasmsiz ketganini bilmay
 * qolmasin.
 */
@Service
@RequiredArgsConstructor
public class AppCastingService {

    /** Eski modulning raqamli holati: 0 — yangi, 1 — qabul, 2 — rad. */
    static final int STATUS_PENDING = 0;

    private final CastingUserRepo castingUserRepo;
    private final AttachmentRepo attachmentRepo;
    private final UserRepo userRepo;

    @Transactional
    public MyCastingApplicationDto submit(User user, CastingApplicationRequest req) {
        UUID userId = user.getId();

        // ⚠️ Foydalanuvchi qatori QULFLANADI. «Kutilayotgan ariza bormi»
        // tekshiruvi — o'qish, keyin yozish. Ikki marta tez bosilsa ikkala
        // so'rov ham «yo'q» ni ko'rib, ikkita anketa yozardi. Qisman unique
        // indeks ({@code where status = 0}) testlardagi H2 da yo'q —
        // `UserRepo.lockById` izohidagi bilan bir xil sabab.
        userRepo.lockById(userId);

        if (castingUserRepo.existsByAppUserIdAndStatus(userId, STATUS_PENDING)) {
            throw BusinessException.duplicate("CASTING_APPLICATION_PENDING",
                    "Sizda ko'rib chiqilayotgan ariza bor. Javobni kuting — "
                            + "keyin yangisini yuborish mumkin.");
        }

        LocalDate birthday = parseBirthday(req.getBirthday());
        List<Attachment> photos = resolvePhotos(req.getPhotos());

        CastingUser c = new CastingUser();
        c.setAppUserId(userId);
        // Ilova anketasi Telegram'dan kelmagan. Eski admin statusni
        // o'zgartirganda bot uchun xabar baribir yoziladi, lekin
        // `telegram_id` bo'sh bo'ladi — CastingUserController.parseTelegramId
        // buni ko'taradi.
        c.setTelegramId(null);
        c.setCastingType(req.getCastingType());
        c.setGender(req.getGender());
        c.setName(req.getName().trim());
        c.setRegion(req.getRegion());
        c.setNationality(req.getNationality());
        c.setBirthday(birthday.atStartOfDay());
        c.setAge(Period.between(birthday, LocalDate.now()).getYears());
        c.setHeight(req.getHeight());
        c.setHairColor(req.getHairColor());
        c.setEyeColor(req.getEyeColor());
        c.setClothSize(req.getClothSize());
        c.setShoeSize(req.getShoeSize());
        c.setBust(req.getBust());
        c.setWaist(req.getWaist());
        c.setSon(req.getSon());
        c.setEmail(req.getEmail());
        c.setPhone(req.getPhone().trim());
        c.setTelegram(req.getTelegram());
        c.setFacebook(req.getFacebook());
        c.setInstagram(req.getInstagram());
        // Narx va holatni faqat admin o'zgartiradi. Qiymatlar eski
        // `POST /casting-user` qo'yadigan boshlang'ich qiymatlar bilan
        // AYNAN bir xil — admin sayti ikkala manbani farqlamasin.
        c.setPrice(null);
        c.setStatus(STATUS_PENDING);
        c.setIsWebShow(Boolean.FALSE);
        c.setFirstChan(0);
        c.setSecondChan(0);
        c.setCreatedAt(LocalDateTime.now());
        c.setPhotos(photos);

        return MyCastingApplicationDto.from(castingUserRepo.save(c));
    }

    /** Faqat shu foydalanuvchining anketalari — boshqa hech kimniki emas. */
    @Transactional(readOnly = true)
    public List<MyCastingApplicationDto> mine(User user) {
        return castingUserRepo.findAllByAppUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(MyCastingApplicationDto::from)
                .toList();
    }

    /**
     * Sana qismi olinadi: {@code 2000-05-17} ham, {@code 2000-05-17T00:00:00Z}
     * ham bir xil kun.
     *
     * ⚠️ Vaqt mintaqasi bo'yicha O'GIRILMAYDI. Telefon tug'ilgan kunni
     * UTC yarim tuni qilib yuborsa, Toshkent vaqtiga o'girish uni boshqa
     * kunga surib yuborishi mumkin edi — tug'ilgan kun esa vaqt emas, sana.
     */
    LocalDate parseBirthday(String raw) {
        LocalDate date;
        try {
            date = LocalDate.parse(raw.trim().substring(0, 10));
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            throw BusinessException.validation("Tug'ilgan sana noto'g'ri: " + raw);
        }
        LocalDate today = LocalDate.now();
        if (!date.isBefore(today)) {
            throw BusinessException.validation("Tug'ilgan sana kelajakda bo'lishi mumkin emas");
        }
        if (Period.between(date, today).getYears() > 100) {
            throw BusinessException.validation("Tug'ilgan sana noto'g'ri: 100 yoshdan katta");
        }
        return date;
    }

    /**
     * Rasm id'larini tekshiradi.
     *
     * Takrorlangan id bitta deb olinadi: {@code casting_user_photos.photos_id}
     * UNIQUE va bir rasmni ikki marta qo'shish INSERT'ni yiqitardi.
     */
    private List<Attachment> resolvePhotos(List<UUID> requested) {
        Set<UUID> ids = new LinkedHashSet<>(requested);

        List<Attachment> found = attachmentRepo.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new BusinessException("CASTING_PHOTO_NOT_FOUND",
                    "Rasmlardan biri topilmadi. Rasmni qaytadan yuklang.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // ⚠️ Boshqa anketaga biriktirilgan rasm qabul qilinmaydi. Sabab
        // ikkita: jadvaldagi UNIQUE cheklov (aks holda 500), va begona
        // anketaning rasmini o'ziniki qilib olish imkoni bo'lmasin.
        if (castingUserRepo.countLinkedPhotos(ids) > 0) {
            throw new BusinessException("CASTING_PHOTO_IN_USE",
                    "Rasmlardan biri boshqa arizada ishlatilgan. Rasmni qaytadan yuklang.",
                    HttpStatus.CONFLICT);
        }

        // So'rovdagi tartib saqlanadi: birinchi rasm — admin ro'yxatidagi
        // muqova (`CastingUserDetail.js` `photos[0]` ni ko'rsatadi).
        List<Attachment> ordered = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            found.stream().filter(a -> id.equals(a.getId())).findFirst().ifPresent(ordered::add);
        }
        return ordered;
    }
}
