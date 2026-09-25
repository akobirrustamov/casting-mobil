-- V43 — bildirishnomalar: «o'qilgan» belgisi.
--
-- Buyurtmachi (25.09.2026): push keladi, lekin ilovadagi qo'ng'iroqchada
-- «yangi xabar bor» degan qizil belgi chiqmaydi; «Xabarlar» ochilganda
-- xabarlar o'qilgan bo'lishi kerak.
--
-- ⚠️ Belgi FOYDALANUVCHIGA tegishli, qurilmaga emas: telefonda o'qigan
-- xabar planshetda ham o'qilgan bo'lishi kerak. Shuning uchun serverda.
--
-- Qator bor = o'qilgan. Qator yo'q = o'qilmagan. Xabar yoki odam
-- o'chirilsa, belgilar ham ketadi.
create table if not exists cms_notification_read (
    user_id          uuid         not null references users (id) on delete cascade,
    notification_id  bigint       not null references cms_notification (id) on delete cascade,
    read_at          timestamp(6) not null default now(),

    -- Bitta odam bitta xabarni bir marta o'qiydi: so'rov takrorlansa
    -- ham ikkinchi qator paydo bo'lmaydi.
    primary key (user_id, notification_id)
);
