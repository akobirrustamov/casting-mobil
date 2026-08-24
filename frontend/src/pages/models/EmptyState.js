import React from 'react';
import { useTranslation } from 'react-i18next';

export default function EmptyState({ children }) {
    const { t } = useTranslation();

    return (
        <div style={{ height: "59vh" }} className="px-4 pt-9 pb-11 border border-dashed border-[#243244] rounded-[14px] text-center bg-[#121923] text-[#e5edf6]">
            {/* Иллюстрация */}
            <svg
                className="block mx-auto mb-3 [filter:drop-shadow(0_10px_24px_rgba(0,0,0,0.25))]"
                width="160"
                height="120"
                viewBox="0 0 160 120"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                aria-hidden
            >
                <rect x="10" y="18" rx="10" ry="10" width="140" height="90" fill="#0f1722" stroke="#243244" />
                <rect x="20" y="28" rx="8" ry="8" width="60" height="70" fill="#121923" stroke="#243244" />
                <rect x="85" y="28" rx="6" ry="6" width="55" height="12" fill="#121923" stroke="#243244" />
                <rect x="85" y="48" rx="6" ry="6" width="55" height="8" fill="#121923" stroke="#243244" />
                <rect x="85" y="62" rx="6" ry="6" width="40" height="8" fill="#121923" stroke="#243244" />
                <circle cx="50" cy="63" r="18" fill="#162233" stroke="#2d3e53" />
                <path d="M106 88 L120 102" stroke="#2e8fff" strokeWidth="4" strokeLinecap="round" />
                <circle cx="126" cy="108" r="6" fill="#2e8fff" />
            </svg>

            <h3 className="mt-1.5 mb-1 mx-0 text-lg text-[#e5edf6]">{t('common.emptyTitle')}</h3>
            <p className="m-0 text-[#8a98ac] text-sm">{t('common.emptySubtitle')}</p>
            {children}
        </div>
    );
}
