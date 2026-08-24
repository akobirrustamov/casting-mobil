import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import ConfirmDialog, { useConfirm } from '../components/ConfirmDialog';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';
import { count, money } from '../utils/format';

const at = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : null);

/**
 * ⚠️ Obuna tarixi uchun necha qator skanerlanadi.
 *
 * Backendda «foydalanuvchi bo'yicha obunalar» endpointi YO'Q — qidiruv
 * faqat telefon va ism bo'yicha. Shuning uchun bu yerda telefon bo'yicha
 * qidirib, keyin aynan shu hisobga tegishlilari ajratib olinadi.
 *
 * Chegara ochiq aytiladi: mos keluvchi yozuvlar shundan ko'p bo'lsa,
 * sahifada ogohlantirish chiqadi va «Obunalar» bo'limiga havola
 * beriladi. Jim qolib qisqartirish «bu odamning boshqa obunasi yo'q»
 * degan yolg'on xulosaga olib borardi (§45).
 */
const SUBS_SCAN_LIMIT = 100;

function Field({ label, value, note, mono = false }) {
  return (
    <div className="mb-3">
      <div className="uz-muted" style={{ fontSize: 11 }}>{label}</div>
      <div className={mono ? 'uz-mono' : ''} style={{ fontSize: 14, wordBreak: 'break-all' }}>
        {value}
      </div>
      {note && <div className="uz-muted" style={{ fontSize: 11, marginTop: 2 }}>{note}</div>}
    </div>
  );
}

function Money({ label, value, suffix, accent }) {
  return (
    <div className="uz-card p-4">
      <div className="uz-muted" style={{ fontSize: 12, fontWeight: 600 }}>{label}</div>
      <div className="uz-mono" style={{
        fontSize: 24, fontWeight: 700, marginTop: 6, color: accent || 'var(--p-text)',
      }}>
        {value}
        {suffix && <span style={{ fontSize: 13, marginLeft: 4 }}>{suffix}</span>}
      </div>
    </div>
  );
}

/**
 * Bitta foydalanuvchi sahifasi (ТЗ §35 — BOSQICH F6).
 *
 * <h2>Nega alohida sahifa</h2>
 * Ro'yxatdagi qator savolga javob bera olmaydi: «bu odam nimaga
 * shikoyat qilyapti?» degan murojaatda balans, qurilmalar va obuna
 * tarixi BIR VAQTDA kerak bo'ladi. Ilgari ularning har biri alohida
 * modal yoki alohida bo'limda edi va admin ular orasida yurib,
 * kontekstni boshida saqlab turishga majbur bo'lardi.
 */
export default function UserDetailPage() {
  const { t } = usePanelI18n();
  const { can } = useAuth();
  const { userId } = useParams();
  const navigate = useNavigate();

  const { data: user, error, loading, reload } = useApi(
    () => adminApi.userById(userId),
    [userId]
  );

  const devices = useApi(() => adminApi.userDevices(userId), [userId]);

  const confirmer = useConfirm(() => { reload(); devices.reload(); });

  const [premiumOpen, setPremiumOpen] = useState(false);
  const [months, setMonths] = useState(1);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);

  // Obuna tarixi: telefon (yoki ism) bo'yicha qidirib, keyin aynan shu
  // hisobga tegishlilari ajratiladi — yuqoridagi izohga qarang.
  const subsKey = user?.phone || user?.name || null;
  const subs = useApi(
    () => (subsKey
      ? adminApi.subscriptions({ q: subsKey, page: 0, size: SUBS_SCAN_LIMIT })
      : Promise.resolve(null)),
    [subsKey]
  );

  const run = async (fn) => {
    setBusy(true);
    setActionError(null);
    try {
      await fn();
      reload();
    } catch (err) {
      setActionError(err);
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <LoadingState rows={4} />;
  if (error) return <ErrorState error={error} onRetry={reload} />;

  const mine = (subs.data?.items || []).filter((s) => s.userId === user.id);
  const subsTruncated = (subs.data?.totalItems || 0) > SUBS_SCAN_LIMIT;

  return (
    <>
      <PageHeader
        title={user.name || user.phone || t('ud.title')}
        subtitle={user.phone || user.email || ''}
        right={(
          <div className="flex items-center gap-3 flex-wrap">
            <button type="button" className="uz-btn uz-btn-ghost"
                    onClick={() => navigate('/app/panel/users')}>
              {t('ud.back')}
            </button>

            {can('USER_PREMIUM_MANAGE') && (
              <>
                <button type="button" className="uz-btn uz-btn-ghost" disabled={busy}
                        onClick={() => { setPremiumOpen(true); setMonths(1); }}>
                  + {t('us.premium')}
                </button>
                {user.premiumActive && (
                  <button type="button" className="uz-btn uz-btn-ghost" disabled={busy}
                          onClick={() => confirmer.ask({
                            message: `${user.phone || user.name} — ${t('us.revokePremium')}?`,
                            confirmLabel: t('us.revokePremium'),
                            run: () => adminApi.revokePremium(user.id),
                          })}>
                    − {t('us.premium')}
                  </button>
                )}
              </>
            )}

            {can('USER_BLOCK') && (
              user.status === 'BLOCKED' ? (
                <button type="button" className="uz-btn uz-btn-ghost" disabled={busy}
                        onClick={() => run(() => adminApi.unblockUser(user.id))}>
                  {t('us.unblock')}
                </button>
              ) : (
                <button type="button" className="uz-btn uz-btn-danger" disabled={busy}
                        onClick={() => confirmer.ask({
                          message: `${user.phone || user.name} — ${t('confirm.blockUser')}`,
                          confirmLabel: t('us.block'),
                          input: { label: t('us.blockReason'), required: false },
                          run: (reason) => adminApi.blockUser(user.id, reason),
                        })}>
                  {t('us.block')}
                </button>
              )
            )}
          </div>
        )}
      />

      {actionError && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
                      border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13 }}>
          {actionError.message}
        </div>
      )}

      <div className="grid gap-4 mb-6" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))' }}>
        <div className="uz-card p-5">
          <div className="uz-h2 mb-4" style={{ fontSize: 15 }}>{t('ud.profile')}</div>

          <Field label={t('staff.col.name')} value={user.name || '—'} />
          <Field label={t('staff.col.phone')} value={user.phone || '—'} mono />
          <Field label={t('ud.email')} value={user.email || '—'} />

          <div className="flex gap-2 flex-wrap mb-3">
            <Badge tone={user.status === 'BLOCKED' ? 'blocked' : 'published'}>{user.status}</Badge>
            {/* ⚠️ `null` — «hali til tanlamagan», UZ emas. Uni UZ deb
                ko'rsatish taxminni fakt sifatida ko'rsatish bo'lardi:
                bu odam hali ilovani ochmagan. */}
            {user.language
              ? <Badge tone="info">{user.language}</Badge>
              : <span className="uz-muted" style={{ fontSize: 12 }}>{t('us.languageUnknown')}</span>}
          </div>

          {user.status === 'BLOCKED' && user.blockedReason && (
            <Field label={t('ud.blockedReason')} value={user.blockedReason} />
          )}

          <Field
            label={t('ud.registeredAt')}
            value={at(user.createdAt) || '—'}
            note={user.createdAt ? null : t('ud.registeredUnknown')}
            mono
          />
          <Field label={t('ud.lastActive')} value={at(user.lastActiveAt) || '—'} mono />
        </div>

        <div>
          <div className="uz-h2 mb-3" style={{ fontSize: 15 }}>{t('ud.balance')}</div>
          <div className="grid gap-3 mb-5"
               style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))' }}>
            <Money label={t('ud.money')} value={money(user.moneyBalance)}
                   suffix={t('common.currency')} />
            <Money label={t('ud.stars')} value={count(user.starsBalance)} accent="var(--p-gold)" />
            <Money label={t('ud.coins')} value={count(user.coinBalance)} accent="var(--p-accent)" />
          </div>

          <div className="uz-h2 mb-3" style={{ fontSize: 15 }}>{t('ud.premium')}</div>
          <div className="uz-card p-4">
            {user.premiumActive ? (
              <>
                <Badge tone="gold">{t('common.active')}</Badge>
                <div className="uz-mono mt-2" style={{ fontSize: 18, fontWeight: 700 }}>
                  {at(user.premiumUntil)?.slice(0, 10) || '—'}
                </div>
                <div className="uz-muted" style={{ fontSize: 11 }}>{t('ud.premiumUntil')}</div>
              </>
            ) : (
              <span className="uz-muted" style={{ fontSize: 13 }}>{t('ud.premiumNone')}</span>
            )}
          </div>
        </div>
      </div>

      {/* ───────────────────────────── Qurilmalar */}
      <div className="uz-h2 mb-3" style={{ fontSize: 15 }}>{t('ud.devices')}</div>
      <div className="uz-card p-4 mb-6">
        {devices.loading ? <LoadingState rows={2} /> :
         devices.error ? <ErrorState error={devices.error} onRetry={devices.reload} /> :
         !devices.data?.length ? (
           <EmptyState compact icon="📱" title={t('ud.devicesEmpty')} />
         ) : (
           devices.data.map((d, i) => (
             <div key={d.id} className="flex items-center gap-3 py-3 flex-wrap"
                  style={{ borderTop: i === 0 ? 'none' : '1px solid var(--p-border-soft)' }}>
               <div style={{ flex: '1 1 200px' }}>
                 <div style={{ fontWeight: 600, fontSize: 13 }}>{d.deviceName || d.deviceId}</div>
                 <div className="uz-muted uz-mono" style={{ fontSize: 11 }}>
                   {d.platform} · {at(d.lastActiveAt) || '—'}
                 </div>
               </div>
               <Badge tone={d.active ? 'published' : 'draft'}>
                 {d.active ? t('common.active') : t('common.inactive')}
               </Badge>
               {d.active && can('USER_DEVICE_MANAGE') && (
                 <button type="button" className="uz-btn uz-btn-danger"
                         style={{ minHeight: 30, padding: '0 10px', fontSize: 12 }}
                         onClick={() => confirmer.ask({
                           message: `${d.deviceName || d.platform || d.deviceId} — `
                             + t('confirm.revokeDevice'),
                           confirmLabel: t('us.revokeDevice'),
                           run: () => adminApi.revokeDevice(user.id, d.id),
                         })}>
                   {t('us.revokeDevice')}
                 </button>
               )}
             </div>
           ))
         )}
      </div>

      {/* ───────────────────────────── Obuna tarixi */}
      <div className="flex items-center justify-between gap-3 mb-3 flex-wrap">
        <div className="uz-h2" style={{ fontSize: 15 }}>{t('ud.subscriptions')}</div>
        {can('SUBSCRIPTION_VIEW') && subsKey && (
          <Link to={`/app/panel/subscriptions?q=${encodeURIComponent(subsKey)}`}
                className="uz-btn uz-btn-ghost"
                style={{ minHeight: 32, fontSize: 12, textDecoration: 'none' }}>
            {t('ud.allSubscriptions')}
          </Link>
        )}
      </div>

      {!can('SUBSCRIPTION_VIEW') ? (
        // Obuna ro'yxati KIM qancha to'laganini ochadi va shu sababli
        // `TARIFF_VIEW` dan alohida ruxsat talab qiladi (§71, §107).
        <p className="uz-muted" style={{ fontSize: 13 }}>{t('error.forbiddenBody')}</p>
      ) : !subsKey ? (
        <p className="uz-muted" style={{ fontSize: 13 }}>{t('ud.subsNoKey')}</p>
      ) : (
        <>
          <p className="uz-muted mb-3" style={{ fontSize: 12, lineHeight: 1.6 }}>
            {t('ud.subsScopeNote')}
          </p>
          {subsTruncated && (
            <div className="mb-3 px-4 py-3"
                 style={{ borderRadius: 'var(--p-radius)', background: 'var(--warning-soft)',
                          border: '1px solid var(--warning-border)',
                          color: 'var(--p-warning)', fontSize: 13 }}>
              {t('ud.subsTruncated')}
            </div>
          )}
          <div className="uz-card overflow-hidden">
            {subs.loading ? <LoadingState rows={2} /> :
             subs.error ? <ErrorState error={subs.error} onRetry={subs.reload} /> :
             mine.length === 0 ? (
               <EmptyState compact icon="🎟️" title={t('ud.subsEmpty')} />
             ) : (
               <TableWrap>
                 <table className="uz-table">
                   <thead>
                     <tr>
                       <th>{t('sub.tariff')}</th>
                       <th>{t('sub.period')}</th>
                       <th>{t('sub.source')}</th>
                       <th style={{ textAlign: 'right' }}>{t('sub.paid')}</th>
                       <th>{t('sub.status')}</th>
                     </tr>
                   </thead>
                   <tbody>
                     {mine.map((s) => (
                       <tr key={s.id}>
                         <td style={{ fontWeight: 600, fontSize: 13 }}>{s.tariffName || '—'}</td>
                         <td className="uz-mono uz-muted" style={{ fontSize: 12 }}>
                           {at(s.startAt)?.slice(0, 10)} → {at(s.endAt)?.slice(0, 10) || '∞'}
                         </td>
                         <td className="uz-muted" style={{ fontSize: 12 }}>
                           {s.source === 'ADMIN_GIFT' ? t('sub.gift') : t('sub.purchase')}
                         </td>
                         {/* ⚠️ Sovg'a obunada `paidAmount` — `null`, ya'ni
                             «sotilmagan». `money()` uni «—» qiladi:
                             «0 so'm» deb ko'rsatish «bepul sotildi» degan
                             boshqa ma'noni berardi (§45, §103). */}
                         <td className="uz-mono" style={{ textAlign: 'right' }}>
                           {money(s.paidAmount)}
                         </td>
                         <td>
                           <Badge tone={s.revokedAt ? 'blocked' : s.active ? 'published' : 'draft'}>
                             {s.revokedAt ? t('sub.revoked') : s.active ? t('sub.active') : t('sub.ended')}
                           </Badge>
                         </td>
                       </tr>
                     ))}
                   </tbody>
                 </table>
               </TableWrap>
             )}
          </div>
        </>
      )}

      {/* Premium sovg'a qilish */}
      <Modal
        open={premiumOpen}
        title={t('us.grantPremium')}
        onClose={() => setPremiumOpen(false)}
        width={460}
        footer={
          <>
            <button type="button" className="uz-btn uz-btn-ghost" onClick={() => setPremiumOpen(false)}>
              {t('common.cancel')}
            </button>
            <button type="button" className="uz-btn uz-btn-primary" disabled={busy}
                    onClick={() => run(async () => {
                      await adminApi.grantPremium(user.id, { months: Number(months) });
                      setPremiumOpen(false);
                    })}>
              {busy ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <p className="uz-muted mb-4" style={{ fontSize: 13 }}>
          {user.phone} · {user.name}
        </p>
        <label className="uz-label" htmlFor="ud-months">{t('us.months')}</label>
        <input id="ud-months" className="uz-input" type="number" min="1" max="60" value={months}
               onChange={(e) => setMonths(e.target.value)} />
        {user.premiumActive && (
          <p className="uz-muted mt-2" style={{ fontSize: 12 }}>
            {t('us.premiumUntil')}: {at(user.premiumUntil)?.slice(0, 10)} — yangi muddat shunga qo'shiladi
          </p>
        )}
      </Modal>

      <ConfirmDialog {...confirmer.props} />
    </>
  );
}
