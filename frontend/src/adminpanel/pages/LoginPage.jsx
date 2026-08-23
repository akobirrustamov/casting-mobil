import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { LanguageSwitcher } from '../components/Ui';
import { usePanelI18n } from '../i18n';

export default function LoginPage() {
  const { t } = usePanelI18n();
  const { signIn, isAuthed } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  if (isAuthed) {
    navigate('/app/panel', { replace: true });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setFieldErrors({});
    try {
      await signIn(phone.trim(), password);
      navigate(location.state?.from || '/app/panel', { replace: true });
    } catch (err) {
      setError(err);
      // Backend maydon bo'yicha xatolarni {field, message} ko'rinishida beradi
      const map = {};
      (err.errors || []).forEach((fe) => {
        map[fe.field] = fe.message;
      });
      setFieldErrors(map);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="uz-panel flex items-center justify-center p-4" style={{ minHeight: '100vh' }}>
      <div className="w-full" style={{ maxWidth: 420 }}>
        <div className="flex justify-end mb-4">
          <LanguageSwitcher />
        </div>

        <div className="uz-card p-7 sm:p-8" style={{ boxShadow: 'var(--p-shadow)' }}>
          <div className="flex items-center gap-3 mb-6">
            <div
              className="flex items-center justify-center font-bold"
              style={{
                width: 42, height: 42, borderRadius: 12,
                background: 'linear-gradient(135deg, var(--p-primary), var(--p-accent))',
                color: 'var(--on-brand)',
              }}
              aria-hidden="true"
            >
              UZ
            </div>
            <div>
              <div style={{ fontSize: 16, fontWeight: 700 }}>{t('app.title')}</div>
              <div className="uz-muted" style={{ fontSize: 12 }}>{t('app.subtitle')}</div>
            </div>
          </div>

          <h1 className="uz-h2 mb-5">{t('login.title')}</h1>

          <form onSubmit={handleSubmit} noValidate>
            <div className="mb-4">
              <label className="uz-label" htmlFor="phone">{t('login.phone')}</label>
              <input
                id="phone"
                className="uz-input"
                type="tel"
                inputMode="tel"
                autoComplete="username"
                placeholder="+998 90 111 00 01"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                aria-invalid={Boolean(fieldErrors.phone)}
                required
              />
              {fieldErrors.phone && <div className="uz-field-error">{fieldErrors.phone}</div>}
            </div>

            <div className="mb-5">
              <label className="uz-label" htmlFor="password">{t('login.password')}</label>
              <div style={{ position: 'relative' }}>
                <input
                  id="password"
                  className="uz-input"
                  style={{ paddingRight: 46 }}
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  aria-invalid={Boolean(fieldErrors.password)}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? t('login.hidePassword') : t('login.showPassword')}
                  style={{
                    position: 'absolute', right: 6, top: '50%', transform: 'translateY(-50%)',
                    width: 34, height: 34, borderRadius: 8, background: 'transparent',
                    border: 'none', color: 'var(--p-muted)', cursor: 'pointer', fontSize: 15,
                  }}
                >
                  {showPassword ? '🙈' : '👁'}
                </button>
              </div>
              {fieldErrors.password && <div className="uz-field-error">{fieldErrors.password}</div>}
            </div>

            {error && !Object.keys(fieldErrors).length && (
              <div
                role="alert"
                className="mb-4 px-4 py-3"
                style={{
                  borderRadius: 'var(--p-radius)',
                  background: 'var(--danger-soft)',
                  border: '1px solid var(--danger-border)',
                  color: 'var(--p-danger)',
                  fontSize: 13,
                }}
              >
                {error.code === 'NETWORK_ERROR' ? t('error.network') : error.message}
              </div>
            )}

            <button type="submit" className="uz-btn uz-btn-primary w-full" disabled={loading}>
              {loading ? t('login.loading') : t('login.submit')}
            </button>
          </form>

          <p className="uz-muted mt-5 text-center" style={{ fontSize: 12 }}>
            {t('login.hint')}
          </p>
        </div>
      </div>
    </div>
  );
}
