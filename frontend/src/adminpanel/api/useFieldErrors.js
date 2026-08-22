import { useCallback, useState } from 'react';

/**
 * Backend qaytargan maydon xatolarini formaga bog'laydi (ТЗ §52).
 *
 * <h2>Nima uchun kerak</h2>
 * Backend xatolarni `{code, message, errors: [{field, message}]}`
 * ko'rinishida qaytaradi va u yerda AYNAN qaysi maydon noto'g'ri
 * ekanligi yozilgan. Lekin formalar faqat umumiy xabarni ko'rsatardi:
 *
 *   «Validatsiya xatosi»
 *
 * Foydalanuvchi o'nlab maydonli formada qaysi birini tuzatishni
 * bilmasdi va ularni birma-bir sinab ko'rishga majbur bo'lardi.
 *
 * <h2>Nima uchun alohida hook</h2>
 * Har bir formada shu bir xil «xatoni yoyish» kodi takrorlanardi — va
 * bitta joyda unutilsa, o'sha forma jimgina umumiy xabarga qaytardi.
 */
export function useFieldErrors() {
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState(null);

  /** Ushlangan xatoni maydonlarga yoyadi. */
  const apply = useCallback((err) => {
    setFormError(err);
    const map = {};
    (err?.errors || []).forEach((fe) => {
      if (fe?.field) {
        map[fe.field] = fe.message;
      }
    });
    setFieldErrors(map);
  }, []);

  const clear = useCallback(() => {
    setFieldErrors({});
    setFormError(null);
  }, []);

  /**
   * Maydon xatosi.
   *
   * ⚠️ Ichma-ich maydonlar uchun ham ishlaydi: backend
   * `translations[UZ].title` ko'rinishida yuboradi.
   */
  const errorOf = useCallback((field) => fieldErrors[field], [fieldErrors]);

  return {
    fieldErrors,
    /**
     * Umumiy xato — FAQAT maydonga bog'lanmagan xatolar uchun.
     *
     * Maydon xatolari allaqachon o'z joyida ko'rsatilgan bo'lsa, tepada
     * yana bir xabar chiqarish takror bo'lardi.
     */
    formError: Object.keys(fieldErrors).length ? null : formError,
    apply,
    clear,
    errorOf,
  };
}
