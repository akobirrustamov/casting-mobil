import { useEffect, useState } from 'react';
import { adminApi } from '../../api/client';
import { useFieldErrors } from '../../api/useFieldErrors';
import Modal from '../../components/Modal';
import { usePanelI18n } from '../../i18n';
import PermissionPicker from './PermissionPicker';
import Select from '../../components/Select';

/** Backenddagi `StaffCreateRequest.phone` bilan bir xil (ТЗ formati). */
const PHONE_RE = /^\+998\s?\d{2}\s?\d{3}\s?\d{2}\s?\d{2}$/;

/** Backenddagi `@Size(min = 8)` + «harf va raqam» qoidasi. */
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

const empty = () => ({
  name: '',
  phone: '+998 ',
  email: '',
  avatarUrl: '',
  password: '',
  role: '',
  permissions: [],
});

/**
 * Xodim yaratish va tahrirlash (BOSQICH F1).
 *
 * <h2>Nega yaratish va tahrirlash bitta formada, rol/parol esa alohida</h2>
 * Backend ataylab shunday bo'lingan: `PUT /staff/{id}` faqat ism, telefon,
 * email va avatarni o'zgartiradi. Rol, ruxsat va parolning har birida
 * o'z xavfsizlik qoidasi bor — masalan «o'zida bo'lmagan ruxsatni bera
 * olmaydi» yoki «yangi rol ham yaratish doirasida bo'lsin». Ularni oddiy
 * tahrirlash formasiga qo'shib yuborish shu tekshiruvlarni foydalanuvchi
 * uchun ko'rinmas qilardi: bitta «Saqlash» tugmasi ba'zan o'tib, ba'zan
 * tushunarsiz 403 qaytarardi.
 *
 * Shuning uchun forma tahrirlashda rol va parol maydonlarini UMUMAN
 * ko'rsatmaydi — ular ro'yxatdagi alohida amallar.
 *
 * <h2>Ruxsatlar bloki</h2>
 * Faqat WORKER tanlanganda chiziladi. ADMIN va undan yuqori rollarda
 * ruxsatlar jadvali umuman ishlatilmaydi (backendda ham shunday), ya'ni
 * u yerda qutichalarni ko'rsatish yolg'on tanlov bo'lardi.
 */
export default function StaffForm({ open, staff, creatableRoles, onClose, onSaved }) {
  const { t } = usePanelI18n();
  const isEdit = Boolean(staff?.id);

  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [touched, setTouched] = useState(false);

  const fields = useFieldErrors();

  useEffect(() => {
    if (!open) return;
    setError(null);
    setTouched(false);
    fields.clear();
    setForm(staff
      ? {
          ...empty(),
          name: staff.name || '',
          phone: staff.phone || '',
          email: staff.email || '',
          avatarUrl: staff.avatarUrl || '',
          role: staff.role || '',
          permissions: staff.permissions || [],
        }
      // Bitta variant qolsa uni oldindan tanlaymiz: ADMIN uchun ro'yxatda
      // baribir faqat WORKER bor va uni qo'lda tanlash ortiqcha qadam.
      : { ...empty(), role: creatableRoles?.length === 1 ? creatableRoles[0] : '' });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, staff]);

  const set = (patch) => setForm((prev) => ({ ...prev, ...patch }));

  const nameBad = !form.name.trim() || form.name.trim().length < 2;
  const phoneBad = !PHONE_RE.test(form.phone.trim());
  const passwordBad = !isEdit && !PASSWORD_RE.test(form.password);
  const roleBad = !isEdit && !form.role;
  const invalid = nameBad || phoneBad || passwordBad || roleBad;

  async function save() {
    setTouched(true);
    if (invalid) return;

    setSaving(true);
    setError(null);
    fields.clear();
    try {
      if (isEdit) {
        await adminApi.updateStaff(staff.id, {
          name: form.name.trim(),
          phone: form.phone.trim(),
          // Bo'sh matn `@Email` tekshiruvidan o'tadi, lekin bazada bo'sh
          // satr qoldirardi — «kiritilmagan» uchun null to'g'riroq.
          email: form.email.trim() || null,
          avatarUrl: form.avatarUrl.trim() || null,
        });
      } else {
        await adminApi.createStaff({
          name: form.name.trim(),
          phone: form.phone.trim(),
          password: form.password,
          role: form.role,
          permissions: form.role === 'WORKER' ? form.permissions : [],
        });
      }
      onSaved();
      onClose();
    } catch (err) {
      // Backend AYNAN qaysi maydon noto'g'ri ekanini aytadi (§52).
      fields.apply(err);
      setError(err);
    } finally {
      setSaving(false);
    }
  }

  /** Backend xatosi klient tekshiruvidan ustun: u haqiqiy rad etish sababi. */
  const errorOf = (field, clientBad, clientMessage) => {
    const fromServer = fields.errorOf(field);
    if (fromServer) return fromServer;
    return touched && clientBad ? clientMessage : null;
  };

  const nameError = errorOf('name', nameBad, t('common.required'));
  const phoneError = errorOf('phone', phoneBad, t('staff.phoneHint'));
  const passwordError = errorOf('password', passwordBad, t('staff.passwordHint'));
  const roleError = errorOf('role', roleBad, t('common.required'));
  const emailError = fields.errorOf('email');

  const roles = creatableRoles || [];

  return (
    <Modal
      open={open}
      title={isEdit ? t('staff.edit') : t('staff.create')}
      onClose={saving ? () => {} : onClose}
      width={isEdit || form.role !== 'WORKER' ? 620 : 900}
      footer={
        <>
          {/* Maydonga bog'langan xatolar allaqachon o'z joyida ko'rinadi —
              bu yerda faqat qolganlari (masalan DUPLICATE_PHONE). */}
          {error && fields.formError && (
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
      {!isEdit && roles.length === 0 && (
        <div
          role="alert"
          className="mb-4 px-4 py-3"
          style={{
            borderRadius: 'var(--p-radius)', background: 'var(--danger-soft)',
            border: '1px solid var(--danger-border)', color: 'var(--p-danger)', fontSize: 13,
          }}
        >
          {t('staff.noCreatableRoles')}
        </div>
      )}

      <div className="uz-row">
        <div className="uz-col">
          <label className="uz-label" htmlFor="st-name">
            {t('staff.name')}<span style={{ color: 'var(--p-danger)' }}> *</span>
          </label>
          <input
            id="st-name" className="uz-input" value={form.name}
            aria-invalid={Boolean(nameError)}
            onChange={(e) => set({ name: e.target.value })}
          />
          {nameError && <div className="uz-field-error">{nameError}</div>}
        </div>

        <div className="uz-col">
          <label className="uz-label" htmlFor="st-phone">
            {t('staff.phone')}<span style={{ color: 'var(--p-danger)' }}> *</span>
          </label>
          <input
            id="st-phone" className="uz-input uz-mono" value={form.phone}
            placeholder="+998 90 123 45 67" inputMode="tel"
            aria-invalid={Boolean(phoneError)}
            onChange={(e) => set({ phone: e.target.value })}
          />
          <div className={phoneError ? 'uz-field-error' : 'uz-muted'} style={{ fontSize: 12, marginTop: 6 }}>
            {phoneError || t('staff.phoneHint')}
          </div>
        </div>
      </div>

      {isEdit && (
        <div className="uz-row mt-4">
          <div className="uz-col">
            <label className="uz-label" htmlFor="st-email">{t('staff.email')}</label>
            <input
              id="st-email" className="uz-input" type="email" value={form.email}
              aria-invalid={Boolean(emailError)}
              onChange={(e) => set({ email: e.target.value })}
            />
            {emailError && <div className="uz-field-error">{emailError}</div>}
          </div>
          <div className="uz-col">
            <label className="uz-label" htmlFor="st-avatar">{t('staff.avatarUrl')}</label>
            <input
              id="st-avatar" className="uz-input" value={form.avatarUrl}
              onChange={(e) => set({ avatarUrl: e.target.value })}
            />
          </div>
        </div>
      )}

      {!isEdit && (
        <>
          <div className="uz-row mt-4">
            <div className="uz-col">
              <label className="uz-label" htmlFor="st-password">
                {t('staff.password')}<span style={{ color: 'var(--p-danger)' }}> *</span>
              </label>
              <input
                id="st-password" className="uz-input" type="password"
                autoComplete="new-password" value={form.password}
                aria-invalid={Boolean(passwordError)}
                onChange={(e) => set({ password: e.target.value })}
              />
              <div className={passwordError ? 'uz-field-error' : 'uz-muted'}
                   style={{ fontSize: 12, marginTop: 6 }}>
                {passwordError || t('staff.passwordHint')}
              </div>
            </div>

            <div className="uz-col">
              <label className="uz-label" htmlFor="st-role">
                {t('staff.role')}<span style={{ color: 'var(--p-danger)' }}> *</span>
              </label>
              {/* ⚠️ Ro'yxatda FAQAT yaratuvchi bera oladigan rollar bor
                  (`/auth/me` dagi `creatableRoles`). Bu qulaylik uchun —
                  backend baribir `canCreateRole` ni tekshiradi va
                  403 qaytaradi. */}
              <Select
                id="st-role" className="uz-select" value={form.role}
                aria-invalid={Boolean(roleError)}
                onChange={(e) => set({ role: e.target.value })}
              >
                <option value="">{t('common.selectPlaceholder')}</option>
                {roles.map((r) => <option key={r} value={r}>{r}</option>)}
              </Select>
              {roleError && <div className="uz-field-error">{roleError}</div>}
            </div>
          </div>

          {form.role === 'WORKER' && (
            <div className="mt-5">
              <div className="uz-label">{t('staff.permissions')}</div>
              <p className="uz-muted mb-3" style={{ fontSize: 12 }}>
                {t('staff.permissionsHint')}
              </p>
              <PermissionPicker
                value={form.permissions}
                onChange={(permissions) => set({ permissions })}
                disabled={saving}
              />
            </div>
          )}
        </>
      )}
    </Modal>
  );
}
