/**
 * Panel karkasi: yon menyu + yuqori panel + kontent.
 *
 * Menyu rol va ruxsatga qarab filtrlanadi. Bu FAQAT qulaylik uchun -
 * yashirilgan bo'lim manzilini qo'lda kiritsa ham backend 403 qaytaradi.
 */
import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { LanguageSwitcher } from '../components/Ui';
import { usePanelI18n } from '../i18n';

const ROLE_TONE = {
  HYPER_ADMIN: 'gold',
  SUPER_ADMIN: 'info',
  ADMIN: 'published',
  WORKER: 'draft',
};

export default function AdminLayout() {
  const { t } = usePanelI18n();
  const { user, signOut, can, atLeast } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  // Mobil qurilmada bo'lim tanlangach menyu yopilsin
  useEffect(() => setMenuOpen(false), [location.pathname]);

  const sections = [
    {
      label: t('nav.main'),
      items: [
        { to: '/app/panel', end: true, icon: '▦', label: t('nav.dashboard'), show: true },
        { to: '/app/panel/reports', icon: '📊', label: t('nav.reports'), show: can('REPORT_VIEW') },
        // Hammaga ko'rinadi: yo'riqnoma xodimga o'z imkoniyatlarini aytadi.
        { to: '/app/panel/help', icon: '📖', label: t('nav.help'), show: true },
      ],
    },
    {
      label: t('nav.catalog'),
      items: [
        { to: '/app/panel/content', icon: '🎬', label: t('nav.content'), show: can('CONTENT_VIEW') },
        { to: '/app/panel/creators', icon: '★', label: t('nav.creators'), show: can('CREATOR_VIEW') },
        { to: '/app/panel/categories', icon: '▤', label: t('nav.categories'), show: can('CATEGORY_VIEW') },
        { to: '/app/panel/genres', icon: '#', label: t('nav.genres'), show: can('GENRE_VIEW') },
        { to: '/app/panel/media', icon: '🖼', label: t('nav.media'), show: can('MEDIA_VIEW') },
      ],
    },
    {
      label: t('nav.homepage'),
      items: [
        { to: '/app/panel/homepage', icon: '▦', label: t('nav.homepage'), show: can('HOMEPAGE_VIEW') },
        { to: '/app/panel/ads', icon: '📢', label: t('nav.ads'), show: can('ADVERTISEMENT_VIEW') },
        { to: '/app/panel/premieres', icon: '🎬', label: t('nav.premieres'), show: can('PREMIERE_VIEW') },
      ],
    },
    {
      label: t('nav.engagement'),
      items: [
        { to: '/app/panel/comments', icon: '💬', label: t('nav.comments'), show: can('COMMENT_VIEW') },
        { to: '/app/panel/notifications', icon: '🔔', label: t('nav.notifications'), show: can('NOTIFICATION_VIEW') },
      ],
    },
    {
      label: t('nav.users'),
      items: [
        { to: '/app/panel/users', icon: '👤', label: t('nav.users'), show: can('USER_VIEW') },
        { to: '/app/panel/tariffs', icon: '👑', label: t('nav.tariffs'), show: can('TARIFF_VIEW') },
        { to: '/app/panel/subscriptions', icon: '🎟️', label: t('nav.subscriptions'),
          show: can('SUBSCRIPTION_VIEW') },
        { to: '/app/panel/donations', icon: '✨', label: t('nav.donations'), show: can('DONATION_VIEW') },
      ],
    },
    {
      label: t('nav.casting'),
      items: [
        // Eski casting moduli — o'chirilmaydi va o'zgartirilmaydi (ТЗ §4).
        { to: '/app/panel/casting', icon: '🎭', label: t('nav.castingList'), show: can('CONTENT_VIEW') },
      ],
    },
    {
      label: t('nav.system'),
      items: [
        { to: '/app/panel/staff', icon: '👥', label: t('nav.staff'), show: atLeast('ADMIN') },
        { to: '/app/panel/settings', icon: '⚙️', label: t('nav.settings'), show: can('SETTINGS_VIEW') },
        { to: '/app/panel/audit', icon: '📜', label: t('nav.audit'), show: atLeast('ADMIN') },
      ],
    },
  ];

  return (
    <div className="uz-panel">
      {/* Mobil menyu ochiq bo'lganda orqa fon */}
      {menuOpen && (
        <div
          className="uz-scrim"
          onClick={() => setMenuOpen(false)}
          aria-hidden="true"
        />
      )}

      <aside className={`uz-sidebar ${menuOpen ? 'open' : ''}`}>
        <div className="flex items-center gap-3 px-6" style={{ height: 'var(--p-header)' }}>
          <div
            className="flex items-center justify-center font-bold"
            style={{
              width: 34, height: 34, borderRadius: 10,
              background: 'linear-gradient(135deg, var(--p-primary), var(--p-accent))',
              color: 'var(--on-brand)', fontSize: 15,
            }}
            aria-hidden="true"
          >
            UZ
          </div>
          <div>
            <div style={{ fontSize: 14, fontWeight: 700 }}>{t('app.title')}</div>
            <div className="uz-muted" style={{ fontSize: 11 }}>{t('app.subtitle')}</div>
          </div>
        </div>

        <nav className="pb-6">
          {sections.map((section) => {
            const visible = section.items.filter((i) => i.show);
            if (visible.length === 0) return null;
            return (
              <div key={section.label}>
                <div className="uz-nav-section">{section.label}</div>
                {visible.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) => `uz-nav-item ${isActive ? 'active' : ''}`}
                  >
                    <span aria-hidden="true" style={{ width: 18, textAlign: 'center' }}>{item.icon}</span>
                    <span>{item.label}</span>
                  </NavLink>
                ))}
              </div>
            );
          })}
        </nav>
      </aside>

      <div className="uz-content">
        <header
          className="flex items-center justify-between gap-4 px-4 sm:px-6 sticky top-0 z-40"
          style={{
            height: 'var(--p-header)',
            background: 'var(--scrim)',
            backdropFilter: 'blur(10px)',
            borderBottom: '1px solid var(--p-border-soft)',
          }}
        >
          <button
            type="button"
            className="uz-btn uz-btn-ghost uz-menu-btn"
            style={{ minHeight: 38, padding: '0 12px' }}
            onClick={() => setMenuOpen((v) => !v)}
            aria-label={t('nav.menu')}
            aria-expanded={menuOpen}
          >
            ☰
          </button>

          <div className="flex-1" />

          <LanguageSwitcher />

          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <div style={{ fontSize: 13, fontWeight: 600 }}>{user?.name || user?.phone}</div>
              <div className={`uz-badge uz-badge-${ROLE_TONE[user?.role] || 'draft'}`} style={{ marginTop: 2 }}>
                {user?.role}
              </div>
            </div>
            <button type="button" className="uz-btn uz-btn-ghost" style={{ minHeight: 38 }} onClick={signOut}>
              {t('nav.logout')}
            </button>
          </div>
        </header>

        <main className="p-4 sm:p-6 lg:p-8" style={{ maxWidth: 1600 }}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
