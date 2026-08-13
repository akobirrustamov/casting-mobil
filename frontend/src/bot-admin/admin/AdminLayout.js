import React from "react";
import Header from "./HeaderAdmin";

/**
 * Admin panelning yagona sahifa qobig'i.
 * Barcha admin sahifalari shu komponentdan foydalanadi, shuning uchun
 * fon, kenglik, chetki bo'shliqlar va sarlavha uslubi hamma joyda bir xil.
 * Uslublar faqat Tailwind klasslari bilan beriladi - yangi CSS yozilmaydi.
 */
export function AdminPage({ headerProps = "", children }) {
    return (
        <div className="min-h-screen bg-black text-white">
            <Header props={headerProps} />
            <div className="mx-auto w-full max-w-[1600px] px-4 pb-16 pt-6 sm:px-6 lg:px-8">
                {children}
            </div>
        </div>
    );
}

/**
 * Sahifa sarlavhasi. O'ngdagi qo'shimcha elementlar (tugma, statistika kartasi
 * va h.k.) children sifatida uzatiladi.
 */
export function AdminPageHeader({ title, children }) {
    return (
        <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
            <h1 className="m-0 bg-[linear-gradient(90deg,#3b82f6,#10b981)] bg-clip-text text-2xl font-extrabold text-transparent sm:text-3xl">
                {title}
            </h1>
            {children ? <div className="flex flex-wrap items-center gap-3">{children}</div> : null}
        </div>
    );
}

/** Bir xil ko'rinishdagi xatolik bloki. */
export function AdminError({ children }) {
    if (!children) return null;
    return (
        <div className="mx-auto mb-6 max-w-2xl rounded-lg bg-red-900/60 px-4 py-3 text-center text-red-200">
            {children}
        </div>
    );
}

/** Bir xil ko'rinishdagi yuklanish holati. */
export function AdminLoading({ text = "Yuklanmoqda..." }) {
    return (
        <div className="flex h-64 flex-col items-center justify-center gap-4">
            <div className="h-12 w-12 animate-spin rounded-full border-b-2 border-t-2 border-blue-500"></div>
            <span className="text-gray-400">{text}</span>
        </div>
    );
}

/** Ma'lumot yo'q holati. */
export function AdminEmpty({ children }) {
    return (
        <div className="flex h-64 items-center justify-center text-center text-gray-400">
            {children}
        </div>
    );
}
