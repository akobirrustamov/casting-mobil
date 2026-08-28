import { useMemo, useState } from 'react';

import { PageHeader } from '../components/Ui';
import { useAuth } from '../auth/AuthContext';
import { usePanelI18n } from '../i18n';
import { GUIDE } from './help/guide';

/**
 * Yo'riqnoma — «men nima qila olaman va qanday».
 *
 * <h2>⚠️ Nega umumiy qo'llanma emas</h2>
 * Xodimlarning ruxsatlari har xil. Hammaga bir xil matn ko'rsatilsa,
 * kontent muharriri o'zi kira olmaydigan bo'limlar haqida o'qib,
 * keyin ularni menyudan izlab yurardi.
 *
 * Shuning uchun sahifa AVVAL foydalanuvchi bajara oladigan ishlarni
 * ko'rsatadi. Qolganlari pastda, alohida — ular «bu ish bor, lekin
 * senda ruxsat yo'q» degan ma'noda va admindan nima so'rashni
 * aytadi.
 *
 * <h2>Ruxsat qoidasi panel bilan BIR XIL</h2>
 * {@code can()} va {@code atLeast()} — o'sha ilgaklar. Yo'riqnoma o'z
 * qoidasini yozsa, u menyudan ajralib ketardi: bu yerda «qila
 * olasan» deb yozilib, bo'lim menyuda ko'rinmasdi.
 */
export default function HelpPage() {
  const { t, locale } = usePanelI18n();
  const { user, can, atLeast } = useAuth();
  const [openId, setOpenId] = useState(null);

  /** Shu mavzuni joriy xodim bajara oladimi. */
  const allowed = useMemo(() => {
    return (topic) => {
      if (topic.role) return atLeast(topic.role);
      if (topic.perm) return can(topic.perm);
      return true;
    };
  }, [can, atLeast]);

  const groups = useMemo(
    () =>
      GUIDE.map((group) => ({
        label: group.group[locale] || group.group.uz,
        mine: group.topics.filter(allowed),
        others: group.topics.filter((topic) => !allowed(topic)),
      })).filter((group) => group.mine.length > 0 || group.others.length > 0),
    [locale, allowed]
  );

  const mineCount = groups.reduce((n, g) => n + g.mine.length, 0);
  const otherCount = groups.reduce((n, g) => n + g.others.length, 0);

  return (
    <>
      <PageHeader title={t('help.title')} subtitle={t('help.subtitle')} />

      {/* Xodim o'z rolini bilishi kerak — «nega bu bo'lim yo'q» degan
          savolning javobi ko'pincha shu. */}
      <div className="uz-card mb-4">
        <div className="uz-label">{t('help.yourRole')}</div>
        <div className="flex items-center gap-2 flex-wrap">
          <span className="uz-badge uz-badge-gold">{user?.role || '—'}</span>
          <span className="uz-muted" style={{ fontSize: 13 }}>
            {t('help.canDo', { n: mineCount })}
          </span>
        </div>
      </div>

      {groups.map((group) => (
        <section key={group.label} className="mb-5">
          <div className="uz-label" style={{ fontSize: 14 }}>{group.label}</div>

          {group.mine.length === 0 ? (
            <p className="uz-muted" style={{ fontSize: 13 }}>{t('help.noneHere')}</p>
          ) : (
            <div className="grid gap-2">
              {group.mine.map((topic) => (
                <Topic
                  key={topic.id}
                  topic={topic}
                  locale={locale}
                  open={openId === topic.id}
                  onToggle={() => setOpenId(openId === topic.id ? null : topic.id)}
                />
              ))}
            </div>
          )}
        </section>
      ))}

      {otherCount > 0 && <Locked groups={groups} locale={locale} count={otherCount} />}
    </>
  );
}

/** Bitta mavzu — yopiq holda sarlavha, ochilganda qadamlar. */
function Topic({ topic, locale, open, onToggle }) {
  const { t } = usePanelI18n();
  const pick = (field) => topic[field][locale] || topic[field].uz;

  return (
    <div className="uz-card" style={{ padding: 0, overflow: 'hidden' }}>
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={open}
        className="flex items-center gap-3 w-full text-left"
        style={{ padding: '14px 16px', background: 'none', border: 0, cursor: 'pointer' }}
      >
        <span aria-hidden="true" style={{ fontSize: 20 }}>{topic.icon}</span>
        <span className="flex-1">
          <span className="uz-h2" style={{ fontSize: 15, display: 'block' }}>{pick('title')}</span>
          <span className="uz-muted" style={{ fontSize: 12 }}>{pick('what')}</span>
        </span>
        <span className="uz-muted" aria-hidden="true">{open ? '▴' : '▾'}</span>
      </button>

      {open && (
        <div style={{ padding: '0 16px 16px 52px' }}>
          <ol className="uz-steps">
            {(topic.steps[locale] || topic.steps.uz).map((step, i) => (
              <li key={i}>{step}</li>
            ))}
          </ol>

          {(topic.perm || topic.role) && (
            <p className="uz-muted mt-2" style={{ fontSize: 11 }}>
              {t('help.needs')}: <code>{topic.perm || topic.role}</code>
            </p>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * Ruxsat yetmaydigan ishlar.
 *
 * ⚠️ Yashirilmaydi va ATAYLAB. Xodim «bunday ish bormi?» degan
 * savolga javob topishi va admindan aynan qaysi ruxsatni so'rashni
 * bilishi kerak. Qadamlar ko'rsatilmaydi — ular baribir kerak emas.
 */
function Locked({ groups, locale, count }) {
  const { t } = usePanelI18n();
  const [open, setOpen] = useState(false);

  return (
    <section className="uz-card">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        className="flex items-center gap-2 w-full text-left"
        style={{ background: 'none', border: 0, cursor: 'pointer', padding: 0 }}
      >
        <span className="uz-h2" style={{ fontSize: 15 }}>{t('help.lockedTitle')}</span>
        <span className="uz-muted" style={{ fontSize: 13 }}>({count})</span>
        <span className="uz-muted ml-auto" aria-hidden="true">{open ? '▴' : '▾'}</span>
      </button>

      {open && (
        <>
          <p className="uz-muted mt-2 mb-3" style={{ fontSize: 12 }}>
            {t('help.lockedHint')}
          </p>
          <ul className="grid gap-1">
            {groups.flatMap((g) => g.others).map((topic) => (
              <li key={topic.id} className="flex items-center gap-2" style={{ fontSize: 13 }}>
                <span aria-hidden="true">{topic.icon}</span>
                <span>{topic.title[locale] || topic.title.uz}</span>
                <code className="uz-muted" style={{ fontSize: 11 }}>
                  {topic.perm || topic.role}
                </code>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
