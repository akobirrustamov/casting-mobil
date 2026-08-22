import { useMemo, useState } from 'react';
import CreatorQuickCreate from '../../components/CreatorQuickCreate';
import { PROFESSIONS } from './constants';
/**
 * Ijodkorlar va ularning kasblari (ТЗ §24).
 *
 * ⚠️ Bitta odam bir kinoda aktyor, boshqasida rejissyor bo'lishi mumkin —
 * shuning uchun kasb IJODKORDA emas, shu bog'lanishda saqlanadi.
 */
export default function CreditsTab({ form, set, t, locale, creators, onCreatorCreated }) {
  // ⚠️ Bu holat AYNAN shu bo'limga tegishli va u bilan birga ko'chirildi.
  // Ilgari u muharrirning umumiy holatida turardi — ya'ni ijodkor
  // qidiruvi boshqa bo'limlarni ham qayta chizishga majbur qilardi.
  const [creatorQuery, setCreatorQuery] = useState('');
  const [quickOpen, setQuickOpen] = useState(false);

  const filteredCreators = useMemo(() => {
    const q = creatorQuery.trim().toLowerCase();
    if (!q) return creators.slice(0, 12);
    return creators
      .filter((c) =>
        Object.values(c.translations || {}).some((tr) =>
          (tr.displayName || '').toLowerCase().includes(q)
        )
      )
      .slice(0, 12);
  }, [creators, creatorQuery]);

  const creatorName = (c) => {
    const tr = c.translations || {};
    return tr[locale]?.displayName || tr.UZ?.displayName || c.slug;
  };

  /**
   * Ijodkorni kontentga biriktiradi.
   *
   * ⚠️ Kasb IJODKORDA emas, shu BOG'LANISHDA saqlanadi: bitta odam bir
   * kinoda aktyor, boshqasida rejissyor bo'lishi mumkin (ТЗ §24).
   */
  const attach = (creatorId) => set({
    credits: [...form.credits, {
      creatorId,
      profession: 'ACTOR',
      characterName: '',
      sortOrder: form.credits.length,
    }],
  });

  // Allaqachon biriktirilganlar ro'yxatda takror chiqmasin.
  const attachedIds = new Set(form.credits.map((c) => c.creatorId));
  const suggestions = filteredCreators.filter((c) => !attachedIds.has(c.id));

  return (
      <>
        <input className="uz-input mb-4" placeholder={t('editor.searchCreator')}
               value={creatorQuery} onChange={(e) => setCreatorQuery(e.target.value)} />

        <div className="flex gap-2 flex-wrap mb-5">
          {suggestions.map((c) => (
            <button key={c.id} type="button" className="uz-chip"
                    onClick={() => attach(c.id)}>
              + {creatorName(c)}
            </button>
          ))}

          {/* ⚠️ Topilmasa — JOYIDA yaratish (ТЗ §54).
              Ilgari admin muharrirni tark etib, «Ijodkorlar» bo'limiga
              o'tib, yaratib, qaytishi kerak edi — va saqlanmagan
              o'zgarishlarini yo'qotardi. */}
          {suggestions.length === 0 && (
            <button type="button" className="uz-chip"
                    style={{ borderStyle: 'dashed' }}
                    onClick={() => setQuickOpen(true)}>
              + {creatorQuery.trim()
                  ? t('cr.createNamed', { name: creatorQuery.trim() })
                  : t('cr.quickCreate')}
            </button>
          )}
        </div>

        {form.credits.length === 0 ? (
          <p className="uz-muted" style={{ fontSize: 13 }}>{t('empty.body')}</p>
        ) : (
          form.credits.map((cr, i) => {
            const creator = creators.find((c) => c.id === cr.creatorId);
            return (
              <div key={i} className="uz-row mb-3 items-center">
                <div className="uz-col" style={{ flex: '0 0 180px', fontWeight: 600, fontSize: 14 }}>
                  {creator ? creatorName(creator) : `#${cr.creatorId}`}
                </div>
                <div className="uz-col">
                  <select className="uz-select" value={cr.profession} aria-label={t('editor.profession')}
                          onChange={(e) => {
                            const next = [...form.credits];
                            next[i] = { ...cr, profession: e.target.value };
                            set({ credits: next });
                          }}>
                    {PROFESSIONS.map((p) => <option key={p} value={p}>{p}</option>)}
                  </select>
                </div>
                <div className="uz-col">
                  <input className="uz-input" placeholder={t('editor.characterName')}
                         value={cr.characterName || ''}
                         onChange={(e) => {
                           const next = [...form.credits];
                           next[i] = { ...cr, characterName: e.target.value };
                           set({ credits: next });
                         }} />
                </div>
                <button type="button" className="uz-btn uz-btn-danger" style={{ minHeight: 40 }}
                        onClick={() => set({ credits: form.credits.filter((_, x) => x !== i) })}>
                  {t('common.remove')}
                </button>
              </div>
            );
          })
        )}

        <CreatorQuickCreate
          open={quickOpen}
          initialName={creatorQuery.trim()}
          onClose={() => setQuickOpen(false)}
          onCreated={(created) => {
            // Ro'yxat yangilanadi VA ijodkor darhol biriktiriladi.
            if (onCreatorCreated) onCreatorCreated(created);
            attach(created.id);
            setCreatorQuery('');
          }}
        />
      </>
  );
}
