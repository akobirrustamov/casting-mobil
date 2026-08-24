import { useState } from 'react';
import { adminApi } from '../../api/client';
import { useApi } from '../../api/useApi';
import { useFieldErrors } from '../../api/useFieldErrors';
import { useAuth } from '../../auth/AuthContext';
import ConfirmDialog, { useConfirm } from '../../components/ConfirmDialog';
import Modal from '../../components/Modal';
import { EmptyState, ErrorState, LoadingState } from '../../components/States';
import { Badge, PageHeader, Pagination, SearchInput, TableWrap } from '../../components/Ui';
import { usePanelI18n } from '../../i18n';
import PermissionPicker from './PermissionPicker';
import StaffForm from './StaffForm';

const ROLE_TONE = {
  HYPER_ADMIN: 'gold',
  SUPER_ADMIN: 'info',
  ADMIN: 'published',
  WORKER: 'draft',
};

const STATUS_TONE = {
  ACTIVE: 'published',
  INACTIVE: 'draft',
  BLOCKED: 'blocked',
};

const ROLES = ['HYPER_ADMIN', 'SUPER_ADMIN', 'ADMIN', 'WORKER'];
const STATUSES = ['ACTIVE', 'INACTIVE', 'BLOCKED'];

/** ISO sanani qisqa, o'qiladigan ko'rinishga keltiradi. */
const at = (value) => (value ? value.replace('T', ' ').slice(0, 16) : null);

/** Kichkina amal tugmasi — jadval kengaymasligi uchun belgi bilan. */
function Action({ label, icon, onClick, danger = false, disabled = false }) {
  return (
    <button
      type="button"
      className={`uz-btn ${danger ? 'uz-btn-danger' : 'uz-btn-ghost'}`}
      style={{ minHeight: 30, padding: '0 9px', fontSize: 12, marginLeft: 6 }}
      onClick={onClick}
      disabled={disabled}
      title={label}
      aria-label={label}
    >
      <span aria-hidden="true">{icon}</span>
    </button>
  );
}

/**
 * Xodimlar boshqaruvi (ТЗ §12, §78 — BOSQICH F1).
 *
 * <h2>Nega bu birinchi bosqich</h2>
 * Backend xodim yaratishni to'liq qo'llab-quvvatlardi (ierarxiya,
 * ruxsatlar, audit), lekin panelda tugma YO'Q edi. Ya'ni yangi admin
 * yoki worker faqat baza orqali qo'shilardi — RBAC butun tizimning
 * asosi bo'la turib, o'zi paneldan boshqarilmasdi.
 *
 * <h2>Frontend nimani qiladi va nimani QILMAYDI</h2>
 * Bu yerdagi barcha yashirish va faolsizlantirish — QULAYLIK. Xodim
 * manzilni qo'lda kiritsa ham, tugmani DevTools bilan yoqsa ham
 * backend `canManageUser` va `canCreateRole` ni qaytadan tekshiradi va
 * 403 qaytaradi. Panel faqat bajarib bo'lmaydigan amalni ko'rsatmaslik
 * bilan cheklanadi.
 */
export default function StaffPage() {
  const { t } = usePanelI18n();
  const { user } = useAuth();

  const [q, setQ] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [createdFrom, setCreatedFrom] = useState('');
  const [createdTo, setCreatedTo] = useState('');
  const [page, setPage] = useState(0);

  const { data, error, loading, reload } = useApi(
    () => adminApi.staff({
      q: q || undefined,
      role: role || undefined,
      status: status || undefined,
      createdFrom: createdFrom || undefined,
      createdTo: createdTo || undefined,
      page,
      size: 20,
    }),
    [q, role, status, createdFrom, createdTo, page]
  );

  // Har qanday filtr o'zgarganda birinchi sahifaga qaytamiz: aks holda
  // «3-sahifa» da turib filtrlasa, natija bo'sh ko'rinardi va buni
  // foydalanuvchi «hech narsa topilmadi» deb tushunardi.
  const onFilter = (setter) => (value) => { setter(value); setPage(0); };

  const clearFilters = () => {
    setQ('');
    setRole('');
    setStatus('');
    setCreatedFrom('');
    setCreatedTo('');
    setPage(0);
  };

  const hasFilters = Boolean(q || role || status || createdFrom || createdTo);

  const confirmer = useConfirm(() => reload());

  const [formFor, setFormFor] = useState(undefined); // undefined = yopiq, null = yaratish
  const [permissionsFor, setPermissionsFor] = useState(null);
  const [roleFor, setRoleFor] = useState(null);
  const [passwordFor, setPasswordFor] = useState(null);

  const creatableRoles = user?.creatableRoles || [];

  return (
    <>
      <PageHeader
        title={t('staff.title')}
        subtitle={t('staff.subtitle')}
        right={(
          <div className="flex items-center gap-3 flex-wrap">
            <SearchInput value={q} onChange={onFilter(setQ)}
                         placeholder={t('staff.search')} />
            <select className="uz-select" value={role} aria-label={t('staff.role')}
                    onChange={(e) => onFilter(setRole)(e.target.value)}>
              <option value="">{t('staff.allRoles')}</option>
              {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
            <select className="uz-select" value={status} aria-label={t('staff.col.status')}
                    onChange={(e) => onFilter(setStatus)(e.target.value)}>
              <option value="">{t('staff.allStatuses')}</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>{t(`staff.status.${s}`)}</option>
              ))}
            </select>
            <input type="date" className="uz-input" style={{ maxWidth: 165 }}
                   value={createdFrom} aria-label={t('staff.createdFrom')}
                   title={t('staff.createdFrom')}
                   onChange={(e) => onFilter(setCreatedFrom)(e.target.value)} />
            <input type="date" className="uz-input" style={{ maxWidth: 165 }}
                   value={createdTo} aria-label={t('staff.createdTo')}
                   title={t('staff.createdTo')}
                   onChange={(e) => onFilter(setCreatedTo)(e.target.value)} />
            {hasFilters && (
              <button type="button" className="uz-btn uz-btn-ghost" onClick={clearFilters}>
                {t('staff.clearFilters')}
              </button>
            )}
            {creatableRoles.length > 0 && (
              <button type="button" className="uz-btn uz-btn-primary"
                      onClick={() => setFormFor(null)}>
                {t('staff.add')}
              </button>
            )}
          </div>
        )}
      />

      <div className="uz-card overflow-hidden">
        {loading ? (
          <LoadingState />
        ) : error ? (
          <ErrorState error={error} onRetry={reload} />
        ) : !data?.items?.length ? (
          <EmptyState icon="👥" />
        ) : (
          <>
            <TableWrap>
              <table className="uz-table">
                <thead>
                  <tr>
                    <th>{t('staff.col.name')}</th>
                    <th>{t('staff.col.phone')}</th>
                    <th>{t('staff.col.role')}</th>
                    <th>{t('staff.col.status')}</th>
                    <th>{t('staff.col.permissions')}</th>
                    <th>{t('staff.col.createdBy')}</th>
                    <th>{t('staff.col.lastLogin')}</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((u) => {
                    const isSelf = u.id === user?.id;
                    const can = Boolean(u.manageable);
                    const name = u.name || u.phone;
                    return (
                      <tr key={u.id}>
                        <td>
                          <div className="flex items-center gap-3">
                            {u.avatarUrl ? (
                              <img src={u.avatarUrl} alt="" loading="lazy"
                                   style={{ width: 30, height: 30, borderRadius: '50%',
                                            objectFit: 'cover', flex: '0 0 auto' }} />
                            ) : (
                              <span
                                aria-hidden="true"
                                className="flex items-center justify-center"
                                style={{ width: 30, height: 30, borderRadius: '50%',
                                         background: 'var(--p-surface-2)', fontSize: 12,
                                         fontWeight: 700, flex: '0 0 auto' }}
                              >
                                {(u.name || '?').trim().charAt(0).toUpperCase()}
                              </span>
                            )}
                            <div style={{ minWidth: 0 }}>
                              <div style={{ fontWeight: 600 }}>{u.name || '—'}</div>
                              {u.email && (
                                <div className="uz-muted" style={{ fontSize: 11 }}>{u.email}</div>
                              )}
                              {isSelf && (
                                <div className="uz-muted" style={{ fontSize: 11 }}>
                                  {t('staff.self')}
                                </div>
                              )}
                            </div>
                          </div>
                        </td>
                        <td className="uz-muted uz-mono" style={{ fontSize: 13 }}>{u.phone}</td>
                        <td><Badge tone={ROLE_TONE[u.role] || 'draft'}>{u.role}</Badge></td>
                        <td>
                          <Badge tone={STATUS_TONE[u.status] || 'draft'}>
                            {t(`staff.status.${u.status || 'ACTIVE'}`)}
                          </Badge>
                          {/* Sabab ko'rsatilmasa, admin «nega bu odam kira
                              olmayapti?» degan savolga javob topa olmasdi. */}
                          {u.statusReason && (
                            <div className="uz-muted" style={{ fontSize: 11, marginTop: 4 }}>
                              {u.statusReason}
                            </div>
                          )}
                        </td>
                        <td className="uz-muted" style={{ fontSize: 12 }}>
                          {u.role === 'WORKER'
                            ? t('staff.permissionCount', { n: (u.permissions || []).length })
                            : t('staff.permissionsAll')}
                        </td>
                        <td className="uz-muted" style={{ fontSize: 12 }}>
                          {/* createdBy null — bu AutoRun yoki seeder yaratgan
                              hisob. «—» qo'yish «noma'lum» degan boshqa
                              ma'noni berardi. */}
                          <div>{u.createdByName || (u.createdBy ? '—' : t('staff.system'))}</div>
                          {at(u.createdAt) && (
                            <div style={{ fontSize: 11 }}>{at(u.createdAt)}</div>
                          )}
                        </td>
                        <td className="uz-muted" style={{ fontSize: 12 }}>
                          {/* Hech qachon kirmagan xodim — bo'sh katak emas,
                              aniq belgi. */}
                          {at(u.lastLoginAt)
                            || <span title={t('staff.never')}>—</span>}
                        </td>
                        <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                          {!can ? (
                            <span className="uz-muted" style={{ fontSize: 11 }}
                                  title={isSelf ? t('staff.selfNote') : t('staff.notManageable')}>
                              {isSelf ? t('staff.self') : t('staff.notManageable')}
                            </span>
                          ) : (
                            <>
                              <Action label={t('common.edit')} icon="✎"
                                      onClick={() => setFormFor(u)} />
                              {u.role === 'WORKER' && (
                                <Action label={t('staff.editPermissions')} icon="🛡"
                                        onClick={() => setPermissionsFor(u)} />
                              )}
                              <Action label={t('staff.changeRole')} icon="⇅"
                                      onClick={() => setRoleFor(u)} />
                              <Action label={t('staff.resetPassword')} icon="🔑"
                                      onClick={() => setPasswordFor(u)} />

                              {u.status === 'BLOCKED' ? (
                                <Action label={t('staff.unblock')} icon="⏼"
                                        onClick={() => confirmer.ask({
                                          title: t('staff.unblock'),
                                          message: t('staff.confirmUnblock', { name }),
                                          confirmLabel: t('staff.unblock'),
                                          danger: false,
                                          run: () => adminApi.unblockStaff(u.id),
                                        })} />
                              ) : u.status === 'INACTIVE' ? (
                                <Action label={t('staff.activate')} icon="⏼"
                                        onClick={() => confirmer.ask({
                                          title: t('staff.activate'),
                                          message: t('staff.confirmActivate', { name }),
                                          confirmLabel: t('staff.activate'),
                                          danger: false,
                                          run: () => adminApi.activateStaff(u.id),
                                        })} />
                              ) : (
                                <Action label={t('staff.deactivate')} icon="⏻" danger
                                        onClick={() => confirmer.ask({
                                          title: t('staff.deactivate'),
                                          message: t('staff.confirmDeactivate', { name }),
                                          note: t('staff.deactivateNote'),
                                          confirmLabel: t('staff.deactivate'),
                                          input: { label: t('staff.reasonOptional') },
                                          run: (reason) => adminApi.deactivateStaff(u.id, reason),
                                        })} />
                              )}

                              {u.status !== 'BLOCKED' && (
                                <Action label={t('staff.block')} icon="⛔" danger
                                        onClick={() => confirmer.ask({
                                          title: t('staff.block'),
                                          message: t('staff.confirmBlock', { name }),
                                          note: t('staff.blockNote'),
                                          confirmLabel: t('staff.block'),
                                          input: { label: t('staff.reasonOptional') },
                                          run: (reason) => adminApi.blockStaff(u.id, reason),
                                        })} />
                              )}
                            </>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </TableWrap>

            <Pagination page={page} totalPages={data.totalPages} onPage={setPage} />
          </>
        )}
      </div>

      <StaffForm
        open={formFor !== undefined}
        staff={formFor}
        creatableRoles={creatableRoles}
        onClose={() => setFormFor(undefined)}
        onSaved={reload}
      />

      {/* ⚠️ Bu uchtasi ATAYLAB shartli chiziladi.
          Ular boshlang'ich holatini `staff` dan oladi. Doim chizilib
          faqat `open` bilan yashirilsa, ikkinchi marta OCHILGANDA holat
          eskisidan qolardi: parol oynasida oldingi urinishdagi matn,
          ruxsatlar oynasida esa boshqa xodimning qutichalari. Shartli
          chizishda har ochilish yangi mount bo'ladi. */}
      {permissionsFor && (
        <PermissionsModal
          staff={permissionsFor}
          onClose={() => setPermissionsFor(null)}
          onSaved={reload}
        />
      )}

      {roleFor && (
        <RoleModal
          staff={roleFor}
          creatableRoles={creatableRoles}
          onClose={() => setRoleFor(null)}
          onSaved={reload}
        />
      )}

      {passwordFor && (
        <PasswordModal
          staff={passwordFor}
          onClose={() => setPasswordFor(null)}
        />
      )}

      <ConfirmDialog {...confirmer.props} />
    </>
  );
}

/**
 * WORKER ruxsatlarini o'zgartirish.
 *
 * Alohida oyna, chunki backend ham alohida endpoint beradi: bu yerda
 * «o'zida bo'lmagan ruxsatni bera olmaydi» qoidasi ishlaydi va uni
 * oddiy tahrirlash bilan aralashtirish xatoni tushunarsiz qilardi.
 */
function PermissionsModal({ staff, onClose, onSaved }) {
  const { t } = usePanelI18n();
  // Komponent har ochilishda yangidan mount bo'ladi (yuqoridagi izohga
  // qarang), shuning uchun boshlang'ich qiymat to'g'ridan-to'g'ri
  // prop'dan olinadi — sinxronlashtiruvchi effekt kerak emas.
  const [value, setValue] = useState(staff.permissions || []);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      await adminApi.setStaffPermissions(staff.id, value);
      onSaved();
      onClose();
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open
      title={`${t('staff.editPermissions')} — ${staff.name || staff.phone}`}
      onClose={saving ? () => {} : onClose}
      width={900}
      footer={
        <>
          {error && (
            <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
              {error.message}
            </span>
          )}
          <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose} disabled={saving}>
            {t('common.cancel')}
          </button>
          <button type="button" className="uz-btn uz-btn-primary" onClick={save} disabled={saving}>
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <p className="uz-muted mb-3" style={{ fontSize: 12 }}>{t('staff.permissionsHint')}</p>
      <PermissionPicker value={value} onChange={setValue} disabled={saving} />
    </Modal>
  );
}

/**
 * Rolni o'zgartirish.
 *
 * ⚠️ Ro'yxatda faqat aktor TAYINLAY oladigan rollar bor. Backend ikki
 * tomonlama tekshiradi: nishonni boshqara olishi VA yangi rolni yarata
 * olishi. Aks holda ADMIN o'z xodimini SUPER_ADMIN qilib, keyin o'sha
 * hisob orqali huquqini oshirib olardi.
 */
function RoleModal({ staff, creatableRoles, onClose, onSaved }) {
  const { t } = usePanelI18n();
  const [value, setValue] = useState(staff.role || '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const changed = Boolean(value) && value !== staff.role;

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      await adminApi.changeStaffRole(staff.id, value);
      onSaved();
      onClose();
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open
      title={`${t('staff.changeRole')} — ${staff.name || staff.phone}`}
      onClose={saving ? () => {} : onClose}
      width={480}
      footer={
        <>
          {error && (
            <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
              {error.message}
            </span>
          )}
          <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose} disabled={saving}>
            {t('common.cancel')}
          </button>
          <button type="button" className="uz-btn uz-btn-primary" onClick={save}
                  disabled={saving || !changed}>
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <label className="uz-label" htmlFor="st-role-change">{t('staff.role')}</label>
      <select id="st-role-change" className="uz-select" value={value}
              disabled={saving} onChange={(e) => setValue(e.target.value)}>
        {/* Joriy rol ham ro'yxatda — aks holda tanlangan qiymat bo'sh
            ko'rinardi va admin nima o'zgarayotganini bilmasdi. */}
        {staff.role && !creatableRoles.includes(staff.role) && (
          <option value={staff.role}>{staff.role}</option>
        )}
        {creatableRoles.map((r) => <option key={r} value={r}>{r}</option>)}
      </select>
      <p className="uz-muted mt-3" style={{ fontSize: 12, lineHeight: 1.6 }}>
        {t('staff.roleNote')}
      </p>
      {changed && (
        <p className="mt-2" style={{ fontSize: 13, color: 'var(--p-warning)' }}>
          {t('staff.confirmRole', { name: staff.name || staff.phone, role: value })}
        </p>
      )}
    </Modal>
  );
}

/**
 * Parolni tiklash.
 *
 * ⚠️ Parol tasodifiy generatsiya QILINMAYDI — uni admin kiritadi.
 * Generatsiya qilingan parolni foydalanuvchiga yetkazish kerak bo'lardi,
 * ya'ni u javob tanasida, keyin esa ehtimol logda yoki chatda paydo
 * bo'lardi. Backend ham uni javobda qaytarmaydi va auditga yozmaydi.
 */
function PasswordModal({ staff, onClose }) {
  const { t } = usePanelI18n();
  const [value, setValue] = useState('');
  const [saving, setSaving] = useState(false);
  const [touched, setTouched] = useState(false);

  const fields = useFieldErrors();

  const bad = !/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(value);
  const fieldError = fields.errorOf('password') || (touched && bad ? t('staff.passwordHint') : null);

  const save = async () => {
    setTouched(true);
    if (bad) return;
    setSaving(true);
    fields.clear();
    try {
      await adminApi.resetStaffPassword(staff.id, value);
      // Parol javobda qaytmaydi — ko'rsatadigan narsa yo'q, oyna yopiladi.
      onClose();
    } catch (err) {
      fields.apply(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open
      title={`${t('staff.resetPassword')} — ${staff.name || staff.phone}`}
      onClose={saving ? () => {} : onClose}
      width={460}
      footer={
        <>
          {fields.formError && (
            <span style={{ color: 'var(--p-danger)', fontSize: 13, marginRight: 'auto' }} role="alert">
              {fields.formError.message}
            </span>
          )}
          <button type="button" className="uz-btn uz-btn-ghost" onClick={onClose} disabled={saving}>
            {t('common.cancel')}
          </button>
          <button type="button" className="uz-btn uz-btn-primary" onClick={save} disabled={saving}>
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <label className="uz-label" htmlFor="st-new-password">{t('staff.newPassword')}</label>
      <input id="st-new-password" className="uz-input" type="password"
             autoComplete="new-password" value={value} disabled={saving}
             aria-invalid={Boolean(fieldError)}
             onChange={(e) => setValue(e.target.value)} />
      <div className={fieldError ? 'uz-field-error' : 'uz-muted'} style={{ fontSize: 12, marginTop: 6 }}>
        {fieldError || t('staff.passwordHint')}
      </div>
    </Modal>
  );
}
