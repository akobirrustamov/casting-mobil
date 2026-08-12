import React from 'react';
import { useTranslation } from 'react-i18next';

export default function EmptyState({ children }) {
    const { t } = useTranslation();

    return (
        <div style={{ height: "59vh" }} className="empty-wrap">
            {/* Иллюстрация */}
            <svg
                className="empty-illustration"
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

            <h3 className="empty-title">{t('common.emptyTitle')}</h3>
            <p className="empty-subtitle">{t('common.emptySubtitle')}</p>
            {children}
        </div>
    );
}
