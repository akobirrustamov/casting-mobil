package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * V21 — QIDIRUV INDEKSLARI (ТЗ §55).
 *
 * <h2>Muammo</h2>
 * Panel qidiruvlari {@code lower(x) like '%q%'} ko'rinishida ishlaydi.
 * Oldingi joker belgi ({@code %}) tufayli B-tree indeksi ISHLATILMAYDI:
 * baza har qidiruvda butun jadvalni skanerlaydi.
 *
 * Kichik bazada bu sezilmaydi. 100 000 kontent va 500 000 foydalanuvchida
 * esa har bir qidiruv sekundlarga cho'ziladi — va aynan o'sha paytda
 * qidiruv eng ko'p kerak bo'ladi.
 *
 * <h2>Yechim: trigram indekslari</h2>
 * {@code pg_trgm} kengaytmasi satrni uch harfli bo'laklarga ajratadi va
 * GIN indeksi orqali {@code like '%...%'} ni indekslanadigan qiladi.
 *
 * <h2>⚠️ Nima uchun Java migratsiya</h2>
 * {@code pg_trgm} — PostgreSQL kengaytmasi. H2 da u YO'Q va oddiy SQL
 * migratsiya dev hamda test muhitida yiqilardi.
 *
 * H2 da indekslar umuman yaratilmaydi va bu TO'G'RI: u faqat dev va
 * testda ishlatiladi, u yerda jadvallar kichik va indeks hech narsani
 * tezlashtirmaydi.
 *
 * <h2>⚠️ CONCURRENTLY ishlatilmadi</h2>
 * {@code create index concurrently} jadvalni bloklamaydi, lekin
 * tranzaksiya ichida ishlamaydi — Flyway esa migratsiyani tranzaksiyada
 * bajaradi. Indekslar hozir yaratilmoqda, jadvallar hali kichik.
 */
public class V21__Search_indexes extends BaseJavaMigration {

    /** {@code jadval(ustun)} — panel qidiradigan har bir ustun. */
    private static final List<String[]> SEARCHED = List.of(
            // Foydalanuvchi qidiruvi (§35, §38): telefon, email, ism.
            new String[]{"users", "phone"},
            new String[]{"users", "email"},
            new String[]{"users", "name"},

            // Kontent sarlavhasi — uch tilda saqlanadi.
            new String[]{"cms_content_translation", "title"},

            // Ijodkor ismi.
            new String[]{"cms_creator_translation", "display_name"},
            new String[]{"cms_creator_translation", "first_name"},
            new String[]{"cms_creator_translation", "last_name"},

            // Kategoriya va janr nomi.
            new String[]{"cms_category_translation", "name"},
            new String[]{"cms_genre_translation", "name"},

            // Izoh matni (§34) va bildirishnoma (§51).
            new String[]{"cms_comment", "text"},
            new String[]{"cms_notification_translation", "title"},
            new String[]{"cms_notification_translation", "body"},

            // Media fayl nomi (§26).
            new String[]{"media_asset", "original_filename"});

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase();

        if (!product.contains("postgres")) {
            // H2 — dev va test. Jadvallar kichik, indeks kerak emas.
            return;
        }

        try (Statement st = connection.createStatement()) {
            st.execute("create extension if not exists pg_trgm");

            for (String[] target : SEARCHED) {
                String table = target[0];
                String column = target[1];
                String name = "idx_trgm_" + table + "_" + column;

                // ⚠️ `lower(...)` indeksda ham bo'lishi SHART: so'rov
                // `lower(x) like lower(...)` qiladi va indeks aynan shu
                // ifodaga mos kelishi kerak. Aks holda u ishlatilmaydi.
                st.execute("create index if not exists " + name
                        + " on " + table + " using gin (lower(" + column + ") gin_trgm_ops)");
            }
        }
    }
}
