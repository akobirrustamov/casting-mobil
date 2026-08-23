import { useState } from 'react';
import Modal from './Modal';
import { usePanelI18n } from '../i18n';

/**
 * Tasdiqlash oynasi (ТЗ §51).
 *
 * <h2>Nima uchun `window.confirm` emas</h2>
 * <ul>
 *   <li>Brauzer oynasi TARJIMA QILINMAYDI — «OK / Cancel» foydalanuvchi
 *       tilida emas, brauzer tilida chiqadi;</li>
 *   <li>u panel dizayniga umuman mos kelmaydi (§50 «professional admin
 *       dashboard»);</li>
 *   <li>xavfli amalni oddiy amaldan farqlay olmaydi — o'chirish ham,
 *       saqlash ham bir xil ko'rinadi.</li>
 * </ul>
 *
 * Bu oyna esa xavfli amalda tugmani QIZIL qiladi va nima o'chirilayotganini
 * nomi bilan ko'rsatadi: «Ha» tugmasini bosishdan oldin odam AYNAN nimani
 * tasdiqlayotganini ko'rishi kerak.
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  /** Ixtiyoriy tushuntirish — amal aslida nima qilishini aniqlashtiradi. */
  note,
  confirmLabel,
  danger = true,
  busy = false,
  /** Ixtiyoriy matn maydoni — masalan bloklash sababi. */
  input,
  inputValue = '',
  onInputChange,
  onConfirm,
  onCancel,
}) {
  const { t } = usePanelI18n();
  const blocked = Boolean(input?.required) && !inputValue.trim();

  return (
    <Modal
      open={open}
      title={title || t('confirm.title')}
      onClose={busy ? () => {} : onCancel}
      width={460}
      footer={
        <>
          <button
            type="button"
            className="uz-btn uz-btn-ghost"
            onClick={onCancel}
            disabled={busy}
          >
            {t('common.cancel')}
          </button>
          <button
            type="button"
            className={`uz-btn ${danger ? 'uz-btn-danger' : 'uz-btn-primary'}`}
            onClick={onConfirm}
            disabled={busy || blocked}
            /* Fokus bekor qilishda turadi: xavfli amal tasodifan
               Enter bilan tasdiqlanmasin. */
          >
            {busy ? t('common.saving') : (confirmLabel || t('confirm.yes'))}
          </button>
        </>
      }
    >
      <p style={{ margin: 0, lineHeight: 1.6 }}>{message}</p>

      {note && (
        <p style={{ margin: '8px 0 0', lineHeight: 1.6, fontSize: 13,
                    color: 'var(--p-muted)' }}>
          {note}
        </p>
      )}

      {input && (
        <label style={{ marginTop: 14, display: 'block' }}>
          <span className="uz-label">{input.label}</span>
          <input
            className="uz-input"
            value={inputValue}
            autoFocus
            disabled={busy}
            onChange={(e) => onInputChange(e.target.value)}
            placeholder={input.placeholder}
          />
        </label>
      )}
    </Modal>
  );
}

/**
 * Tasdiqlash oqimini boshqaradigan hook.
 *
 * <h2>Nima uchun hook</h2>
 * Har bir sahifada «qaysi element tasdiqlanmoqda» degan holatni qo'lda
 * saqlash takrorlanish edi — va bitta joyda unutilsa, amal tasdiqsiz
 * bajarilib ketardi.
 *
 * Ishlatilishi:
 * <pre>
 *   const confirm = useConfirm();
 *   ...
 *   onClick={() => confirm.ask({
 *     message: `${row.name} — o'chirilsinmi?`,
 *     run: () => adminApi.deleteAd(row.id),
 *   })}
 *   ...
 *   &lt;ConfirmDialog {...confirm.props} /&gt;
 * </pre>
 */
export function useConfirm(onDone) {
  const [state, setState] = useState(null);
  const [value, setValue] = useState('');
  const [busy, setBusy] = useState(false);

  const close = () => {
    if (busy) return;
    setState(null);
    setValue('');
  };

  return {
    ask: (options) => {
      setValue('');
      setState(options);
    },
    props: {
      open: Boolean(state),
      title: state?.title,
      message: state?.message,
      note: state?.note,
      confirmLabel: state?.confirmLabel,
      danger: state?.danger !== false,
      input: state?.input,
      inputValue: value,
      onInputChange: setValue,
      busy,
      onCancel: close,
      onConfirm: async () => {
        if (!state?.run) return;
        setBusy(true);
        try {
          await state.run(value);
          setState(null);
          setValue('');
          if (onDone) onDone();
        } finally {
          // Xato bo'lsa ham oyna qulflanib qolmasin.
          setBusy(false);
        }
      },
    },
  };
}
