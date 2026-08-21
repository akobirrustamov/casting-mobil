package com.example.backend.support;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hibernate yuborayotgan SQL'ni yozib boradi.
 *
 * <h2>Nega kerak</h2>
 * «Sahifalash bazada bo'lyaptimi yoki xotiradami» degan savolga statistika
 * hisoblagichlari JAVOB BERMAYDI: agar yozuvlar o'sha tranzaksiyada
 * yaratilgan bo'lsa, ular allaqachon kontekstda turadi va "yuklash"
 * hisoblanmaydi.
 *
 * Yagona ishonchli dalil — yuborilgan SQL'ning o'zi: unda {@code limit}
 * bormi. Bor bo'lsa — sahifani baza kesyapti; yo'q bo'lsa — Hibernate butun
 * jadvalni tortib, sahifani xotirada kesyapti.
 *
 * Faqat testlarda ishlatiladi ({@code application-test.properties}).
 */
public class CapturingStatementInspector implements StatementInspector {

    private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

    public static void clear() {
        STATEMENTS.clear();
    }

    public static List<String> captured() {
        return List.copyOf(STATEMENTS);
    }

    /** Berilgan jadvalga tegishli select'lar. */
    public static List<String> selectsFrom(String table) {
        return STATEMENTS.stream()
                .filter(sql -> sql.toLowerCase().startsWith("select"))
                .filter(sql -> sql.toLowerCase().contains(table.toLowerCase()))
                .toList();
    }

    @Override
    public String inspect(String sql) {
        STATEMENTS.add(sql);
        return sql;
    }
}
