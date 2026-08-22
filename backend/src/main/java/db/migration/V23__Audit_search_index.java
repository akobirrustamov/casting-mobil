package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * V23 — AUDIT JURNALI QIDIRUV INDEKSI (ТЗ §59).
 *
 * <h2>Nega alohida migratsiya</h2>
 * V21 qidiruv indekslarini yaratgan, lekin o'sha paytda audit jurnali
 * {@code action} bo'yicha ANIQ TENGLIK bilan izlanardi. §59 da u
 * {@code like '%q%'} ga o'zgardi: panelda bu maydon qidiruv darchasi va
 * admin «content» deb yozadi, «CONTENT_PUBLISHED» deb emas.
 *
 * V21 ni tahrirlash yordam bermasdi — u ishlab turgan bazada allaqachon
 * bajarilgan va qayta ishga tushmaydi.
 *
 * <h2>Nega audit jadvali uchun bu ayniqsa muhim</h2>
 * Audit jurnali eng tez o'sadigan jadvallardan biri: har bir admin
 * amali bitta qator. U hech qachon tozalanmaydi (§59 — o'chirish
 * taqiqlangan), ya'ni yillar davomida faqat kattalashadi. Indekssiz
 * har bir filtr butun tarixni skanerlaydi.
 */
public class V23__Audit_search_index extends BaseJavaMigration {

    private static final List<String[]> SEARCHED = List.<String[]>of(
            new String[]{"audit_log", "action"});

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

                // `lower(...)` so'rovdagi ifoda bilan bir xil bo'lishi shart.
                st.execute("create index if not exists " + name
                        + " on " + table + " using gin (lower(" + column + ") gin_trgm_ops)");
            }
        }
    }
}
