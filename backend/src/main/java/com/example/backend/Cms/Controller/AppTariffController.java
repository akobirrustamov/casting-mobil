package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Cms.Service.TariffCatalogService;
import com.example.backend.Entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Premium tariflari — ilova uchun.
 *
 * <h2>⚠️ Nima uchun bu OCHIQ</h2>
 * Narx — hisobga kirishdan oldin ko'riladigan narsa. Mehmon «Premium'ga
 * o'tish» tugmasini bosganda unga avval kirishni majburlash, keyin narxni
 * ko'rsatish teskari tartib: odam nimaga pul to'lashini bilmasdan hisob
 * ochishi kerak bo'lardi.
 *
 * Bosh sahifa va katalog ham shu sababdan ochiq
 * ({@code SecurityConfig}).
 *
 * <h2>Til</h2>
 * Tartib: {@code ?locale=RU} → profildagi til → UZ. Aynan shu ketma-ketlik
 * {@code HomeFeedService.resolveLanguage} da yozilgan va u shu yerda
 * QAYTA ISHLATILADI — ikkinchi nusxa ikki xil tilni tanlashga olib
 * kelardi: bosh sahifa ruscha, tariflar o'zbekcha.
 *
 * ⚠️ {@code locale} parametrining sukut qiymati YO'Q. {@code defaultValue =
 * "UZ"} qo'yilsa, parametr yuborilmagan holat «o'zbekcha so'raldi» dan
 * farq qilmasdi va profildagi til hech qachon o'qilmasdi.
 */
@RestController
@RequestMapping("/api/v1/app/tariffs")
@RequiredArgsConstructor
public class AppTariffController {

    private final TariffCatalogService tariffCatalogService;
    private final HomeFeedService homeFeedService;

    @GetMapping
    public ResponseEntity<TariffsResponse> tariffs(@RequestParam(required = false) Locale locale) {
        // Token bo'lmasligi mumkin — bu ochiq endpoint. `getOrNull`
        // aynan shuning uchun: mehmonga 401 emas, o'zbekcha ro'yxat.
        User user = CurrentUser.getOrNull();
        Locale resolved = homeFeedService.resolveLanguage(user, locale);

        return ResponseEntity.ok(new TariffsResponse(
                resolved, tariffCatalogService.active(resolved)));
    }

    /**
     * @param locale  qaysi tilda javob berildi — klient buni tekshira
     *                oladi va o'zining tanlovi bilan solishtira oladi
     * @param tariffs sotib olish mumkin bo'lganlari, admin bergan tartibda
     */
    public record TariffsResponse(Locale locale,
                                  List<TariffCatalogService.TariffView> tariffs) {
    }
}
