package com.example.backend.Admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arxitektura testi: har bir admin endpointi qo'riqlangan bo'lishi kerak.
 *
 * <h2>Qanday nosozlikni ushlaydi</h2>
 * Kimdir yangi endpoint qo'shib, ruxsat tekshiruvini yozishni unutadi.
 * {@code SecurityConfig} uni tokensiz kirishdan himoya qiladi, lekin
 * ISTALGAN tokenli xodim — hatto eng cheklangan WORKER ham — unga kira
 * olardi. Ya'ni autentifikatsiya bor, avtorizatsiya yo'q.
 *
 * <h2>Nima qo'riqlash hisoblanadi</h2>
 * Loyihada tekshiruv bir necha shaklda yozilgan va hammasi to'g'ri:
 *   • {@code @RequirePermission(...)} annotatsiyasi;
 *   • {@code require(...)} yordamchi chaqiruvi;
 *   • {@code permissionService.hasPermission/canCreateRole/canManageUser};
 *   • media fayllari uchun {@code readable(id)} — u entitlement tekshiradi
 *     ({@code AccessService.canReadMedia}): rasm ochiq, video esa faqat
 *     haqli foydalanuvchiga;
 *   • bo'laklab yuklashda {@code requireUploader()} / {@code ownSession()} —
 *     birinchisi MEDIA_UPLOAD ruxsatini, ikkinchisi sessiya egaligini
 *     tekshiradi;
 *   • rol tekshiruvi ({@code roleOf} + {@code isAtLeast}).
 *
 * Test aynan shu shakllarni taniydi. Yangi shakl paydo bo'lsa — uni shu
 * yerga qo'shish kerak, aks holda test noto'g'ri ogohlantiradi.
 *
 * Manba fayllar o'qiladi, chunki metod TANASINI refleksiya bilan ko'rib
 * bo'lmaydi.
 */
class AdminEndpointGuardTest {

    private static final Path CONTROLLER_DIR =
            Paths.get("src/main/java/com/example/backend/Admin/Controller");

    /**
     * Ataylab qo'riqlanmaydigan endpointlar.
     *
     * Har biri uchun sabab yozilishi SHART — aks holda ro'yxat vaqt o'tib
     * "hamma narsa mumkin" ga aylanadi.
     */
    private static final Map<String, String> INTENTIONALLY_OPEN = Map.of(
            "login", "Kirish nuqtasi - token hali yo'q. Rol tekshiruvi metod ichida: "
                    + "USER admin panelga kira olmaydi.",
            "me", "O'z profilini o'qish. Rol tekshiruvi metod ichida.",
            "refresh", "Access token muddati tugagach chaqiriladi - ta'rifi bo'yicha "
                    + "yaroqli access token YO'Q. Huquq refresh token orqali tekshiriladi "
                    + "va metod ichida rol hamda bloklanish qayta ko'riladi (§61).",
            "logout", "Chiqish hech qachon rad etilmasligi kerak: aks holda yaroqsiz "
                    + "token bilan qolgan foydalanuvchi sessiyasini yopa olmasdi."
    );

    private static final Pattern MAPPING =
            Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping");

    private static final Pattern METHOD_NAME =
            Pattern.compile("public\\s+[\\w<>,\\[\\].? ]+\\s+(\\w+)\\s*\\(");

    @Test
    @DisplayName("Har bir admin endpointi ruxsat yoki rol tekshiruviga ega")
    void everyAdminEndpointIsGuarded() throws IOException {
        assertThat(Files.isDirectory(CONTROLLER_DIR))
                .as("Controller papkasi topilmadi: %s", CONTROLLER_DIR.toAbsolutePath())
                .isTrue();

        List<String> unguarded = new ArrayList<>();
        int checked = 0;

        List<Path> files;
        try (Stream<Path> stream = Files.list(CONTROLLER_DIR)) {
            files = stream.filter(f -> f.toString().endsWith(".java")).toList();
        }

        for (Path file : files) {
            String source = Files.readString(file);
            String fileName = file.getFileName().toString();

            for (Endpoint e : endpointsOf(source)) {
                checked++;
                if (INTENTIONALLY_OPEN.containsKey(e.name())) {
                    continue;
                }
                if (!e.isGuarded()) {
                    unguarded.add(fileName + "#" + e.name());
                }
            }
        }

        assertThat(checked)
                .as("Endpointlar topilmadi - test o'zi buzilgan bo'lishi mumkin")
                .isGreaterThan(30);

        assertThat(unguarded)
                .as("Bu endpointlarda ruxsat tekshiruvi yo'q. Har qanday tokenli xodim, "
                        + "hatto cheklangan WORKER ham ularga kira oladi. "
                        + "@RequirePermission qo'shing yoki require(...) chaqiring.")
                .isEmpty();
    }

    /** Manbadan endpoint metodlarini va ularning bo'lagini ajratadi. */
    private List<Endpoint> endpointsOf(String source) {
        List<Endpoint> result = new ArrayList<>();

        List<Integer> starts = new ArrayList<>();
        Matcher m = MAPPING.matcher(source);
        while (m.find()) {
            starts.add(m.start());
        }

        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i);
            int to = (i + 1 < starts.size()) ? starts.get(i + 1) : source.length();
            String chunk = source.substring(from, to);

            Matcher name = METHOD_NAME.matcher(chunk);
            if (name.find()) {
                result.add(new Endpoint(name.group(1), chunk));
            }
        }
        return result;
    }

    private record Endpoint(String name, String body) {

        boolean isGuarded() {
            return body.contains("@RequirePermission")
                    || body.contains("require(")
                    // Ba'zi endpointlar tekshiruvni to'g'ridan-to'g'ri chaqiradi,
                    // chunki natijaga qarab boshqacha xabar beradi
                    || body.contains("permissionService.hasPermission(")
                    // Media fayllari entitlement bilan himoyalangan.
                    || body.contains("readable(")
                    // Bo'laklab yuklash: ikkala yordamchi ham MEDIA_UPLOAD
                    // tekshiradi, ownSession qo'shimcha ravishda egalikni.
                    || body.contains("requireUploader(")
                    || body.contains("ownSession(")
                    || body.contains("permissionService.canCreateRole(")
                    || body.contains("permissionService.canManageUser(")
                    || (body.contains("roleOf(") && body.contains("isAtLeast("));
        }
    }
}
