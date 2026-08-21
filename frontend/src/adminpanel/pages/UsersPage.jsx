import { useEffect, useState } from 'react';
import { adminApi } from '../api/client';
import { useApi } from '../api/useApi';
import { useAuth } from '../auth/AuthContext';
import Modal from '../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { Badge, PageHeader, SearchInput, TableWrap } from '../components/Ui';
import { usePanelI18n } from '../i18n';

/**
 * Mobil ilova foydalanuvchilari.
 *
 * ⚠️ Xodimlar bu yerda EMAS — ular «Xodimlar» bo'limida. Backend ham
 * shu ro'yxatdan xodimlarni chiqarib tashlaydi.
 */
export default function UsersPage() {
  const { t } = usePanelI18n();
  const { can } = useAuth();
  const [q, setQ] = useState('');
  const { data, error, loading, reload } = useApi(
    () => adminApi.users({ q: q || undefined, limit: 100 }), [q]);

  const [premiumFor, setPremiumFor] = useState(null);
  const [months, setMonths] = useState(1);
  const [devicesFor, setDevicesFor] = useState(null);
  const [devices, setDevices] = useState([]);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    if (!devicesFor) return;
    adminApi.userDevices(devicesFor.id).then(setDevices).catch(() => setDevices([]));
  }, [devicesFor]);

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

  const money = (v) => Number(v || 0).toLocaleString();

  return (
    <>
      <PageHeader
        title={t('us.title')}
        subtitle={t('us.subtitle')}
        right={<SearchInput value={q} onChange={setQ} placeholder={t('us.search')} />}
      />
      <p className="uz-muted mb-4 text-sm">{t('us.noStaffHint')}</p>

      {actionError && (
        <div role="alert" className="mb-4 px-4 py-3"
             style={{ borderRadius: 'var(--p-radius)', background: 'rgba(248,113,113,.10)',
                      border: '1px solid rgba(248,113,113,.35)', color: 'var(--p-danger)', fontSize: 13 }}>
          {actionError.message}
        </div>
      )}

      <div className="uz-card overflow-hidden">
        {loading ? <LoadingState /> :
         error ? <ErrorState error={error} onRetry={reload} /> :
         !data?.length ? <EmptyState icon="👤" /> : (
          <TableWrap>
            <table className="uz-table">
              <thead>
                <tr>
                  <th>{t('staff.col.name')}</th>
                  <th>{t('staff.col.phone')}</th>
                  <th>{t('us.premium')}</th>
                  <th>{t('us.balance')}</th>
                  <th>{t('us.devices')}</th>
                  <th>{t('content.col.status')}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data.map((u) => (
                  <tr key={u.id}>
                    <td style={{ fontWeight: 600 }}>{u.name || '—'}</td>
                    <td className="uz-mono uz-muted" style={{ fontSize: 13 }}>{u.phone || u.email}</td>
                    <td>
                      {u.premiumActive
                        ? <Badge tone="gold">{u.premiumUntil?.slice(0, 10)}</Badge>
                        : <span className="uz-muted">—</span>}
                    </td>
                    <td className="uz-mono" style={{ fontSize: 12 }}>
                      <div>{money(u.moneyBalance)} {t('common.currency')}</div>
                      <div className="uz-muted">⭐ {u.starsBalance} · ◎ {u.coinBalance}</div>
                    </td>
                    <td>
                      <button type="button" className="uz-btn uz-btn-ghost"
                              style={{ minHeight: 30, padding: '0 10px', fontSize: 12 }}
                              onClick={() => setDevicesFor(u)}>
                        {u.activeDevices ?? 0}
                      </button>
                    </td>
                    <td>
                      <Badge tone={u.status === 'BLOCKED' ? 'blocked' : 'published'}>
                        {u.status}
                      </Badge>
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      {can('USER_PREMIUM_MANAGE') && (
                        <>
                          <button type="button" className="uz-btn uz-btn-ghost"
                                  style={{ minHeight: 30, padding: '0 10px', fontSize: 12 }}
                                  onClick={() => { setPremiumFor(u); setMonths(1); }}>
                            + {t('us.premium')}
                          </button>
                          {u.premiumActive && (
                            <button type="button" className="uz-btn uz-btn-ghost"
                                    style={{ minHeight: 30, padding: '0 10px', fontSize: 12, marginLeft: 6 }}
                                    disabled={busy}
                                    onClick={() => {
                                      if (!window.confirm(`${u.phone} — ${t('us.revokePremium')}?`)) return;
                                      run(() => adminApi.revokePremium(u.id));
                                    }}>
                              − {t('us.premium')}
                            </button>
                          )}
                        </>
                      )}
                      {can('USER_BLOCK') && (
                        u.status === 'BLOCKED' ? (
                          <button type="button" className="uz-btn uz-btn-ghost"
                                  style={{ minHeight: 30, padding: '0 10px', fontSize: 12, marginLeft: 6 }}
                                  disabled={busy}
                                  onClick={() => run(() => adminApi.unblockUser(u.id))}>
                            {t('us.unblock')}
                          </button>
                        ) : (
                          <button type="button" className="uz-btn uz-btn-danger"
                                  style={{ minHeight: 30, padding: '0 10px', fontSize: 12, marginLeft: 6 }}
                                  disabled={busy}
                                  onClick={() => {
                                    const reason = window.prompt(t('us.blockReason'));
                                    if (reason === null) return;
                                    run(() => adminApi.blockUser(u.id, reason));
                                  }}>
                            {t('us.block')}
                          </button>
                        )
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableWrap>
        )}
      </div>

      {/* Premium sovg'a qilish */}
      <Modal
        open={Boolean(premiumFor)}
        title={t('us.grantPremium')}
        onClose={() => setPremiumFor(null)}
        width={460}
        footer={
          <>
            <button type="button" className="uz-btn uz-btn-ghost" onClick={() => setPremiumFor(null)}>
              {t('common.cancel')}
            </button>
            <button type="button" className="uz-btn uz-btn-primary" disabled={busy}
                    onClick={() => run(async () => {
                      await adminApi.grantPremium(premiumFor.id, { months: Number(months) });
                      setPremiumFor(null);
                    })}>
              {busy ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <p className="uz-muted mb-4" style={{ fontSize: 13 }}>
          {premiumFor?.phone} · {premiumFor?.name}
        </p>
        <label className="uz-label" htmlFor="pm-months">{t('us.months')}</label>
        <input id="pm-months" className="uz-input" type="number" min="1" max="60" value={months}
               onChange={(e) => setMonths(e.target.value)} />
        {premiumFor?.premiumActive && (
          <p className="uz-muted mt-2" style={{ fontSize: 12 }}>
            {t('us.premiumUntil')}: {premiumFor.premiumUntil?.slice(0, 10)} — yangi muddat shunga qo'shiladi
          </p>
        )}
      </Modal>

      {/* Qurilmalar */}
      <Modal
        open={Boolean(devicesFor)}
        title={t('us.deviceList')}
        onClose={() => setDevicesFor(null)}
        width={640}
        footer={
          <button type="button" className="uz-btn uz-btn-ghost" onClick={() => setDevicesFor(null)}>
            {t('common.close')}
          </button>
        }
      >
        {devices.length === 0 ? (
          <p className="uz-muted" style={{ fontSize: 13 }}>{t('empty.body')}</p>
        ) : (
          devices.map((d) => (
            <div key={d.id} className="flex items-center gap-3 py-3 flex-wrap"
                 style={{ borderTop: '1px solid var(--p-border-soft)' }}>
              <div style={{ flex: '1 1 200px' }}>
                <div style={{ fontWeight: 600, fontSize: 13 }}>{d.deviceName || d.deviceId}</div>
                <div className="uz-muted uz-mono" style={{ fontSize: 11 }}>
                  {d.platform} · {d.lastActiveAt?.slice(0, 16).replace('T', ' ')}
                </div>
              </div>
              <Badge tone={d.active ? 'published' : 'draft'}>
                {d.active ? t('common.active') : t('common.inactive')}
              </Badge>
              {d.active && can('USER_DEVICE_MANAGE') && (
                <button type="button" className="uz-btn uz-btn-danger"
                        style={{ minHeight: 30, padding: '0 10px', fontSize: 12 }}
                        onClick={async () => {
                          await adminApi.revokeDevice(devicesFor.id, d.id);
                          setDevices(await adminApi.userDevices(devicesFor.id));
                          reload();
                        }}>
                  {t('us.revokeDevice')}
                </button>
              )}
            </div>
          ))
        )}
      </Modal>
    </>
  );
}
