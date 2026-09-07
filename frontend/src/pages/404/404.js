import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import Header from '../header/Header';

/**
 * Topilmagan sahifa.
 *
 * <h2>⚠️ Bu sahifa kutilganidan KO'PROQ ko'rinadi</h2>
 * Spring noma'lum yo'llarni `index.html` ga yo'naltiradi, ya'ni har
 * qanday xato manzil — eski havola, terishdagi xato, qidiruv
 * tizimidagi eskirgan yozuv — shu yerga tushadi. Shuning uchun bu
 * boshi berk ko'cha emas, chiqish yo'li bo'lishi kerak.
 *
 * <h2>⚠️ Nega bu yerda `primary`/`secondary` ranglari YO'Q</h2>
 * Ilgari sarlavha `from-primary to-secondary` gradienti bilan
 * `bg-clip-text text-transparent` qilib berilgandi. Bu ikki rang
 * `tailwind.config.js` da UMUMAN aniqlanmagan — natijada brauzer
 * `linear-gradient(transparent, transparent)` chizardi va matn
 * `rgba(0,0,0,0)` bo'lardi.
 *
 * Ya'ni eng katta element — «404» yozuvining o'zi — butunlay
 * ko'rinmasdi. Sahifa shu sababli bo'sh va buzuq ko'rinardi, va
 * xato hech qanday ogohlantirish bermasdi: Tailwind noma'lum
 * sinfni shunchaki chiqarib tashlaydi.
 *
 * Endi faqat standart ranglar ishlatiladi.
 */
export default function PageNotFound() {
    const { t } = useTranslation();
    const navigate = useNavigate();

    /**
     * ⚠️ Tarix bo'sh bo'lsa «orqaga» hech qayerga olib bormaydi.
     *
     * Odam havolani to'g'ridan-to'g'ri ochgan bo'lsa (yangi oyna,
     * xabardagi havola) `navigate(-1)` jimgina hech narsa qilmasdi —
     * tugma buzuq bo'lib ko'rinardi.
     */
    const canGoBack = window.history.length > 1;

    return (
        <div className="relative flex min-h-screen flex-col overflow-hidden bg-slate-50 dark:bg-slate-900">
            <Header />

            {/* Yumshoq fon dog'lari. Matn ortida, bosishni to'smaydi. */}
            <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden">
                <div className="absolute -left-24 top-32 h-72 w-72 rounded-full bg-blue-400/25 blur-[120px] dark:bg-blue-600/20" />
                <div className="absolute -right-16 top-56 h-64 w-64 rounded-full bg-indigo-400/25 blur-[120px] dark:bg-indigo-600/20" />
            </div>

            <main className="relative z-10 mx-auto flex w-full max-w-2xl flex-1 flex-col items-center justify-center px-6 py-20 text-center">
                {/*
                  ⚠️ QATTIQ rang, gradient EMAS.

                  Avval bu yerda `bg-clip-text text-transparent` bilan
                  gradient turgandi va u ikki marta ko'rinmay qoldi:
                  birinchi safar ranglar (`primary`/`secondary`)
                  aniqlanmagani uchun, ikkinchi safar esa `via-*`
                  yordamchisi `--tw-gradient-to` ni shaffofga
                  qaytargani uchun (sahifada to'qqizta uslub varaqasi
                  bir-birini bosadi).

                  `text-transparent` ning butun xavfi shunda: gradient
                  bir sabab bilan yo'qolsa, matn ham YO'QOLADI —
                  noto'g'ri rangdek ko'zga tashlanmaydi. Qattiq rang
                  esa eng yomon holatda boshqacha rangda chiqadi.
                */}
                <p className="font-mono text-[5.5rem] font-black leading-none tracking-tight text-blue-600 dark:text-blue-400 sm:text-[7rem]">
                    {t('notFound.code')}
                </p>

                <h1 className="mt-6 text-2xl font-bold tracking-tight text-slate-800 dark:text-white sm:text-3xl">
                    {t('notFound.title')}
                </h1>

                <p className="mt-3 max-w-md text-[15px] leading-relaxed text-slate-500 dark:text-slate-400">
                    {t('notFound.text')}
                </p>

                <div className="mt-9 flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
                {/*
                  ⚠️ Ranglar `!` bilan MAJBURIY berilgan.

                  Sahifada to'qqizta uslub varaqasi bor va ulardan biri
                  (chegara rangi `#dee2e6` — Bootstrap qiymati) barcha
                  `button` elementlarining foniga o'z qoidasini qo'yadi.
                  Natijasi ko'rilgan: ikkinchi tugma OQ fonda qoldi,
                  matni esa `dark:text-slate-200` bo'yicha OCH kulrang
                  bo'ldi — ya'ni oq ustida och kulrang, o'qib
                  bo'lmaydigan holat.

                  `dark:text-*` o'tib, `dark:bg-*` o'tmagani ayni
                  shundan: begona varaq faqat fonni bosadi. Shuning
                  uchun fon ham, matn ham, chegara ham majburiy —
                  ikkalasi BIRGA o'zgarsin, aks holda yana shunday
                  yarim holat chiqadi.
                */}
                    <button
                        type="button"
                        onClick={() => navigate('/')}
                        className="rounded-xl !bg-blue-600 px-7 py-3 text-sm font-semibold !text-white shadow-sm transition hover:!bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 active:scale-[.98] dark:focus-visible:ring-offset-slate-900"
                    >
                        {t('notFound.home')}
                    </button>

                    {canGoBack && (
                        <button
                            type="button"
                            onClick={() => navigate(-1)}
                            className="rounded-xl border !border-slate-300 !bg-slate-100 px-7 py-3 text-sm font-semibold !text-slate-700 transition hover:!bg-slate-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-400 focus-visible:ring-offset-2 active:scale-[.98] dark:!border-slate-600 dark:!bg-slate-700 dark:!text-slate-100 dark:hover:!bg-slate-600 dark:focus-visible:ring-offset-slate-900"
                        >
                            {t('notFound.back')}
                        </button>
                    )}
                </div>
            </main>
        </div>
    );
}
