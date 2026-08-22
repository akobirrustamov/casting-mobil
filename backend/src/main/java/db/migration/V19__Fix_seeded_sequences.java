package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * V19 — SEED QILINGAN JADVALLARNING KETMA-KETLIGINI TO'G'RILAYDI.
 *
 * <h2>Xato</h2>
 * V5 tariflarni, ularning tarjimalarini va valyuta paketlarini ANIQ ID
 * bilan qo'shadi ({@code insert into cms_tariff (id, ...) values (1, ...)}).
 * Lekin ketma-ketlik (sequence) oldinga surilmagan — u hamon 1 dan
 * boshlaydi.
 *
 * Natijada admin panelida BIRINCHI yangi tarif yaratishga urinish
 * {@code duplicate key} xatosi bilan yiqiladi: baza id = 1 berishga
 * urinadi, u esa allaqachon band.
 *
 * Bu ТЗ §36 ning asosiy talabini buzadi: «Admin panel orqali
 * o'zgartirilishi shart».
 *
 * <h2>Nima uchun Java migratsiya, SQL emas</h2>
 * Ketma-ketlikni surish sintaksisi portativ EMAS:
 * <ul>
 *   <li>PostgreSQL ({@code bigserial}) — {@code setval(...)};</li>
 *   <li>H2 ({@code identity}) — {@code alter table ... alter column ...
 *       restart with ...}.</li>
 * </ul>
 * Loyihada ikkalasi ham ishlatiladi: PostgreSQL — ishlab chiqarishda,
 * H2 — dev va testda. Bitta SQL fayl ikkalasida ham ishlamaydi, shuning
 * uchun bazani aniqlab, mos buyruq yuboriladi.
 *
 * <h2>Nima uchun V5 tuzatilmadi</h2>
 * U allaqachon bajarilgan. Tahrirlash Flyway nazorat summasini buzadi va
 * mavjud bazalar ishga tushmay qoladi — ma'lumot saqlanishi asosiy qoida.
 */
public class V19__Fix_seeded_sequences extends BaseJavaMigration {

    /** V5 da aniq ID bilan to'ldirilgan jadvallar. */
    private static final List<String> SEEDED_TABLES = List.of(
            "cms_tariff",
            "cms_tariff_translation",
            "cms_currency_package");

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
        boolean postgres = product.contains("postgres");

        for (String table : SEEDED_TABLES) {
            long next = maxId(connection, table) + 1;
            try (Statement st = connection.createStatement()) {
                if (postgres) {
                    // pg_get_serial_sequence ketma-ketlik nomini o'zi topadi —
                    // uni qo'lda yozish nom qoidasiga bog'lanib qolardi.
                    st.execute("select setval(pg_get_serial_sequence('" + table
                            + "', 'id'), " + next + ", false)");
                } else {
                    st.execute("alter table " + table
                            + " alter column id restart with " + next);
                }
            }
        }
    }

    private long maxId(Connection connection, String table) throws Exception {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select coalesce(max(id), 0) from " + table)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
