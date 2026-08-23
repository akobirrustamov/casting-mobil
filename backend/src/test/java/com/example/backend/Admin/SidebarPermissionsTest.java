package com.example.backend.Admin;

import com.example.backend.Enums.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §49 — admin panel menyusi.
 *
 * <h2>Nima uchun backend testi frontend faylini o'qiydi</h2>
 * ТЗ: «Role/permissionga qarab menu yashirilsin. Lekin backend
 * authorization baribir majburiy.»
 *
 * Menyu ruxsat NOMLARIGA tayanadi va bu nomlar backend enum'idan keladi.
 * Nom xato yozilsa — masalan {@code ADVERTISMENT_VIEW} — hech qanday
 * xato chiqmaydi:
 *
 * <ul>
 *   <li>Worker uchun {@code can()} har doim {@code false} qaytaradi va
 *       menyu bandi <b>hech qachon ko'rinmaydi</b>;</li>
 *   <li>Admin uchun esa rol qisqa yo'li ishlaydi va band <b>har doim
 *       ko'rinadi</b>.</li>
 * </ul>
 *
 * Ya'ni admin sinov qilganda hammasi joyida ko'rinadi, xato faqat
 * Worker shikoyat qilganda bilinadi — va sabab menyuda ekanligi hech
 * kimning xayoliga kelmaydi.
 *
 * ⚠️ Bu test menyu YASHIRILISHINI xavfsizlik deb hisoblamaydi. Backend
 * himoyasi {@code AdminEndpointGuardTest} da alohida tekshiriladi.
 */
class SidebarPermissionsTest {

    private static final Path SIDEBAR = Path.of(
            "../frontend/src/adminpanel/layout/AdminLayout.jsx");

    /** {@code can('CONTENT_VIEW')} ko'rinishidagi chaqiruvlar. */
    private static final Pattern CAN_CALL =
            Pattern.compile("can\\(\\s*'([A-Z_]+)'\\s*\\)");

    private Set<String> permissionsUsedInMenu() throws IOException {
        String source = Files.readString(SIDEBAR);
        Set<String> found = new LinkedHashSet<>();
        Matcher m = CAN_CALL.matcher(source);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    @Nested
    @DisplayName("Menyu va backend bir xil ruxsat nomlarini ishlatadi")
    class NamesMatch {

        @Test
        @DisplayName("⚠️ Menyudagi har bir ruxsat backend enum'ida BOR")
        void everyMenuPermissionExistsInBackend() throws IOException {
            Set<String> backend = Arrays.stream(Permission.values())
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            List<String> unknown = new ArrayList<>();
            for (String used : permissionsUsedInMenu()) {
                if (!backend.contains(used)) {
                    unknown.add(used);
                }
            }

            assertThat(unknown)
                    .as("Menyuda backend bilmaydigan ruxsat ishlatilgan. "
                            + "Bu xato JIMGINA yashiradi: Worker menyu bandini "
                            + "hech qachon ko'rmaydi, Admin esa har doim "
                            + "ko'radi (rol qisqa yo'li) — ya'ni admin sinovida "
                            + "hammasi joyida ko'rinadi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Ijobiy nazorat: detektor haqiqatan topyapti")
        void detectorFindsPermissions() throws IOException {
            // Yuqoridagi test BO'SH ro'yxat kutadi. Naqsh buzilsa yoki
            // yo'l noto'g'ri bo'lsa u ham bo'sh qaytaradi va test abadiy
            // yashil turadi — hech narsani tekshirmasdan.
            assertThat(permissionsUsedInMenu())
                    .as("Menyudan bitta ham ruxsat topilmadi — demak "
                            + "detektor noto'g'ri joyga qarayapti")
                    .isNotEmpty()
                    .contains("CONTENT_VIEW");
        }
    }

    @Nested
    @DisplayName("ТЗ §49 dagi modullar qamrovi")
    class Coverage {

        @Test
        @DisplayName("Har bir asosiy modul menyuda bor")
        void allRequestedModulesAreInTheMenu() throws IOException {
            String source = Files.readString(SIDEBAR);

            // ТЗ §49 ro'yxati. Yo'l nomlari mavjud loyihaga moslashtirilgan
            // («Actual route/module mavjud projectga moslashtirilsin»).
            List<String> routes = List.of(
                    "/app/panel/content",
                    "/app/panel/categories",
                    "/app/panel/genres",
                    "/app/panel/creators",
                    "/app/panel/media",
                    "/app/panel/homepage",
                    "/app/panel/ads",
                    "/app/panel/premieres",
                    "/app/panel/notifications",
                    "/app/panel/comments",
                    "/app/panel/users",
                    "/app/panel/tariffs",
                    "/app/panel/donations",
                    "/app/panel/reports",
                    "/app/panel/staff",
                    "/app/panel/casting",
                    "/app/panel/audit",
                    "/app/panel/settings");

            List<String> missing = routes.stream()
                    .filter(route -> !source.contains(route))
                    .toList();

            assertThat(missing)
                    .as("ТЗ §49 da so'ralgan modul menyuda yo'q")
                    .isEmpty();
        }
    }
}
