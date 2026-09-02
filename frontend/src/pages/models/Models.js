import React, { useState, useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import ApiCall, { baseUrl } from '../../config';
import Header from '../header/Header';
import Loader from './Loader';
import EmptyState from './EmptyState';
import Footer from "../footer/Footer";
import "bootstrap/dist/css/bootstrap.css";
import Carousel from "react-bootstrap/Carousel";
import ModelCard from "./ModelCard";



// Avval Models.css dagi `.dual-range input[type="range"]` uslublari.
const RANGE_INPUT =
    "absolute pointer-events-none appearance-none w-full h-[38px] bg-none m-0 " +
    "[&::-webkit-slider-runnable-track]:h-1.5 [&::-moz-range-track]:h-1.5 " +
    "[&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:pointer-events-auto " +
    "[&::-webkit-slider-thumb]:w-[18px] [&::-webkit-slider-thumb]:h-[18px] " +
    "[&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#4da3ff] " +
    "[&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-[#0b1220] " +
    "[&::-webkit-slider-thumb]:shadow-[0_0_0_2px_rgba(77,163,255,0.55)] [&::-webkit-slider-thumb]:cursor-pointer " +
    "[&::-moz-range-thumb]:pointer-events-auto [&::-moz-range-thumb]:w-[18px] [&::-moz-range-thumb]:h-[18px] " +
    "[&::-moz-range-thumb]:rounded-full [&::-moz-range-thumb]:bg-[#4da3ff] " +
    "[&::-moz-range-thumb]:border-2 [&::-moz-range-thumb]:border-[#0b1220] " +
    "[&::-moz-range-thumb]:shadow-[0_0_0_2px_rgba(77,163,255,0.55)] [&::-moz-range-thumb]:cursor-pointer";

function Models() {
    const { t } = useTranslation();

    const [items, setItems] = useState([]);
    const [open, setOpen] = useState(false);
    const [current, setCurrent] = useState(null);
    const [loading, setLoading] = useState(true);
    const [zoomPhoto, setZoomPhoto] = useState(null);
    const [contactLock, setContactLock] = useState(false);

    // --- Фильтры ---
    const [query, setQuery] = useState('');
    const [gender, setGender] = useState('all');
    const [ctype, setCtype] = useState('all');
    const [minAge, setMinAge] = useState(0);
    const [maxAge, setMaxAge] = useState(100);
    const [heightFrom, setHeightFrom] = useState('');

    const TELEGRAM_USERNAME = 'JasMaxStar';

    const calcAge = (birthday) => {
        if (!birthday) return null;
        try {
            const b = new Date(birthday);
            const now = new Date();
            let age = now.getFullYear() - b.getFullYear();
            const m = now.getMonth() - b.getMonth();
            if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age--;
            return age;
        } catch { return null; }
    };

    useEffect(() => {
        if (open) document.body.style.overflow = 'hidden';
        else document.body.style.overflow = '';
        return () => (document.body.style.overflow = '');
    }, [open]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const res = await ApiCall('/api/v1/casting-user/web', 'GET');

                const list = Array.isArray(res) ? res : Array.isArray(res?.data) ? res.data : [];

                const mapped = list.map((u) => {
                    const photos = Array.isArray(u.photos) ? u.photos : [];
                    const photoUrls = photos
                        .filter(p => p?.id && p.isWebShow === true) // faqat ID mavjud va isWebShow true bo‘lsa
                        .map(p => ({
                            url: `${baseUrl}/api/v1/file/getFile/${p.id}`,
                            isWebShow: true
                        }));



                    const ageRaw = u.age ?? calcAge(u.birthday);
                    const ageNum = Number(ageRaw);
                    const age = Number.isFinite(ageNum) ? Math.max(0, Math.min(100, ageNum)) : null;

                    return {
                        ...u,
                        photoUrls,
                        age,
                        castingType: (u.castingType || '').toLowerCase(),
                    };
                });


                setItems(mapped);
            } catch (e) {
                console.error('Failed to load cards', e);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const getFirstName = (fullName = "") => {
        if (!fullName) return "";

        const parts = fullName.trim().split(/\s+/);
        if (parts.length === 1) return parts[0]; // только имя

        // Список типичных окончаний фамилий и отчеств
        const lastNameEndings = [
            "ov", "ova", "ev", "eva", "yev", "yeva",
            "ovich", "ovna", "ovna", "ovna", "ovna",
            "qizi", "bekqizi", "ов", "ова", "ев", "ева", "овна", "қызы"
        ];

        // Проверка по окончаниям (латиница/кириллица)
        const looksLikeLastName = (word) => {
            const w = word.toLowerCase();
            return lastNameEndings.some(end => w.endsWith(end));
        };

        // Найти первое слово, которое НЕ похоже на фамилию/отчество
        for (let p of parts) {
            if (!looksLikeLastName(p)) return p;
        }

        // fallback → если все слова выглядят как фамилия, возвращаем второе
        return parts[1] || parts[0];
    };

    const openModal = (item) => {
        setCurrent(item);
        setOpen(true);
    };

    const closeModal = () => {
        setOpen(false);
        setCurrent(null);
    };
    const SmartImage = ({ src, alt, className }) => {
        const [style, setStyle] = useState({});

        const handleLoad = (e) => {
            const { naturalWidth, naturalHeight } = e.target;
            if (naturalWidth >= naturalHeight) {
                setStyle({ width: "133%" }); // квадратное или горизонтальное фото
            } else {
                setStyle({ width: "105%" }); // вертикальное фото
            }
        };

        return (
            <div className="w-full h-full" style={style}>
                <img
                    src={src}
                    alt={alt}
                    loading="lazy"
                    className={className}
                    onLoad={handleLoad}
                />
            </div>
        );
    };


    const fmt = (v) => (v === null || v === undefined || v === '' ? '—' : v);

    const heightOptions = useMemo(() => {
        const list = [];
        for (let h = 145; h <= 220; h += 5) list.push(h);
        return list;
    }, []);

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        return items.filter((i) => {
            // faqat isWebShow = true bo‘lsin
            // if (!i.isWebShow) return false;

            if (q && !(i.name || '').toLowerCase().includes(q)) return false;
            if (gender !== 'all' && String(i.gender || '').toLowerCase() !== gender) return false;
            if (ctype !== 'all' && String(i.castingType || '').toLowerCase() !== ctype) return false;

            const a = Number(i.age);
            if (Number.isFinite(a)) {
                if (a < Number(minAge)) return false;
                if (a > Number(maxAge)) return false;
            }

            if (heightFrom !== '') {
                const h = Number(i.height);
                if (!Number.isFinite(h) || h < Number(heightFrom)) return false;
            }

            return true;
        });
    }, [items, query, gender, ctype, minAge, maxAge, heightFrom]);

    const rangeMin = 0, rangeMax = 100;
    const clampAge = (v) => Math.max(rangeMin, Math.min(rangeMax, Number(v) || 0));
    const pct = (val) => ((val - rangeMin) * 100) / (rangeMax - rangeMin);

    useEffect(() => {
        const minC = clampAge(minAge), maxC = clampAge(maxAge);
        if (minC !== minAge) setMinAge(minC);
        if (maxC !== maxAge) setMaxAge(maxC);
        if (minC > maxC) setMinAge(maxC);
    }, [maxAge, minAge]); // eslint-disable-line

    const buildContactMessage = (m) => {
        const lblRequest = t('models.contact.requestTitle', 'Заявка на модель');
        const lblId = t('models.contact.id', 'ID');
        const lblName = t('models.contact.name', 'Имя');
        return [lblRequest, `${lblId}: ${m.id}`, `${lblName}: ${(m.name || '').trim()}`].join('\n');
    };

    const DRAFT_FLAG_PREFIX = 'tg_draft_sent'; // ключ в localStorage: tg_draft_sent:<username>:<modelId>

    const handleContact = async () => {
        if (!current || contactLock) return;
        setContactLock(true);

        const msg = buildContactMessage(current);
        const draftKey = `${DRAFT_FLAG_PREFIX}:${TELEGRAM_USERNAME}:${current.id}`;

        try {
            const alreadySent = localStorage.getItem(draftKey) === '1';

            if (!alreadySent) {
                // первый клик по этой модели: откроем с ?text=...
                const encoded = encodeURIComponent(msg);
                window.open(`https://t.me/${TELEGRAM_USERNAME}?text=${encoded}`, '_blank', 'noopener,noreferrer');

                // пометим как «отправлено», чтобы в следующий раз не подставлялось снова
                localStorage.setItem(draftKey, '1');
            } else {
                // последующие клики по этой же модели:
                // открываем чат без ?text=..., чтобы Телеграм не подставлял черновик повторно
                // (на всякий — скопируем текст в буфер, чтобы пользователю было удобно вставить самому)
                if (navigator.clipboard?.writeText) {
                    try { await navigator.clipboard.writeText(msg); } catch { }
                }
                window.open(`https://t.me/${TELEGRAM_USERNAME}`, '_blank', 'noopener,noreferrer');
            }
        } finally {
            setTimeout(() => setContactLock(false), 600);
        }
    };

    const castingTypeOptions = [
        { value: 'all', label: t('filters.castingType.options.all') },
        { value: 'model', label: t('filters.castingType.options.model') },
        { value: 'euromodel', label: t('filters.castingType.options.euromodel') },
        { value: 'bloger', label: t('filters.castingType.options.bloger') },
        { value: 'actor', label: t('filters.castingType.options.actor') },
        { value: 'extra', label: t('filters.castingType.options.extra') },
        { value: 'influencer', label: t('filters.castingType.options.influencer') },
    ];

    return (
        <div className="[color-scheme:dark] bg-black pt-20 [&_*::-webkit-scrollbar]:h-[10px] [&_*::-webkit-scrollbar]:w-[10px] [&_*::-webkit-scrollbar-track]:bg-[#0b111a] [&_*::-webkit-scrollbar-thumb]:bg-[#2a3a50] [&_*::-webkit-scrollbar-thumb]:rounded-full [&_*::-webkit-scrollbar-thumb]:border-2 [&_*::-webkit-scrollbar-thumb]:border-[#0b111a]">
            <Header props="" />
            <section className="max-w-[1200px] mx-auto p-6 bg-[#0b0f14] text-[#e5edf6]">
                <div className="flex flex-col gap-2.5 mb-3.5">
                    <h1 className="text-2xl font-bold mt-2 mb-4 mx-0 text-[#e5edf6] tracking-[0.2px]">{t('models.title', 'Models / Actors')}</h1>

                    <div className="grid grid-cols-[repeat(auto-fit,minmax(190px,1fr))] items-end gap-4">
                        <div className="[&_label]:block [&_label]:text-xs [&_label]:uppercase [&_label]:text-[#8a98ac] [&_label]:tracking-[0.06em] [&_label]:mb-1.5 [&_input]:w-full [&_input]:px-3 [&_input]:py-2.5 [&_input]:rounded-xl [&_input]:border [&_input]:border-[#243244] [&_input]:outline-none [&_input]:bg-[#121923] [&_input]:text-[#e5edf6] [&_input]:transition-all [&_input::placeholder]:text-[#94a3b8] [&_input:focus]:border-[#4da3ff] [&_input:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)] [&_select]:w-full [&_select]:px-3 [&_select]:py-2.5 [&_select]:rounded-xl [&_select]:border [&_select]:border-[#243244] [&_select]:outline-none [&_select]:bg-[#121923] [&_select]:text-[#e5edf6] [&_select]:transition-all [&_select:focus]:border-[#4da3ff] [&_select:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)]">
                            <label>{t('filters.search', 'Поиск')}</label>
                            <input
                                type="text"
                                placeholder={t('filters.searchPlaceholder', 'Имя, фамилия…')}
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                            />
                        </div>

                        <div className="[&_label]:block [&_label]:text-xs [&_label]:uppercase [&_label]:text-[#8a98ac] [&_label]:tracking-[0.06em] [&_label]:mb-1.5 [&_input]:w-full [&_input]:px-3 [&_input]:py-2.5 [&_input]:rounded-xl [&_input]:border [&_input]:border-[#243244] [&_input]:outline-none [&_input]:bg-[#121923] [&_input]:text-[#e5edf6] [&_input]:transition-all [&_input::placeholder]:text-[#94a3b8] [&_input:focus]:border-[#4da3ff] [&_input:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)] [&_select]:w-full [&_select]:px-3 [&_select]:py-2.5 [&_select]:rounded-xl [&_select]:border [&_select]:border-[#243244] [&_select]:outline-none [&_select]:bg-[#121923] [&_select]:text-[#e5edf6] [&_select]:transition-all [&_select:focus]:border-[#4da3ff] [&_select:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)]">
                            <label>{t('filters.gender', 'Пол')}</label>
                            <select value={gender} onChange={(e) => setGender(e.target.value)}>
                                <option value="all">{t('filters.genderAny', 'Любой')}</option>
                                <option value="male">{t('filters.genderMale', 'Мужской')}</option>
                                <option value="female">{t('filters.genderFemale', 'Женский')}</option>
                            </select>
                        </div>

                        <div className="[&_label]:block [&_label]:text-xs [&_label]:uppercase [&_label]:text-[#8a98ac] [&_label]:tracking-[0.06em] [&_label]:mb-1.5 [&_input]:w-full [&_input]:px-3 [&_input]:py-2.5 [&_input]:rounded-xl [&_input]:border [&_input]:border-[#243244] [&_input]:outline-none [&_input]:bg-[#121923] [&_input]:text-[#e5edf6] [&_input]:transition-all [&_input::placeholder]:text-[#94a3b8] [&_input:focus]:border-[#4da3ff] [&_input:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)] [&_select]:w-full [&_select]:px-3 [&_select]:py-2.5 [&_select]:rounded-xl [&_select]:border [&_select]:border-[#243244] [&_select]:outline-none [&_select]:bg-[#121923] [&_select]:text-[#e5edf6] [&_select]:transition-all [&_select:focus]:border-[#4da3ff] [&_select:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)]">
                            <label>{t('filters.castingType.label', 'Casting type')}</label>
                            <select value={ctype} onChange={(e) => setCtype(e.target.value)}>
                                {castingTypeOptions.map(o => (
                                    <option key={o.value} value={o.value}>{o.label}</option>
                                ))}
                            </select>
                        </div>

                        <div className="[&_label]:block [&_label]:text-xs [&_label]:uppercase [&_label]:text-[#8a98ac] [&_label]:tracking-[0.06em] [&_label]:mb-1.5">
                            <label>{t('filters.age', 'Возраст')}</label>
                            <div className="relative h-[38px] mt-0.5 [--range-bg:#253347] [--range-fill:#4da3ff]">
                                <div
                                    className="absolute left-0 right-0 top-1/2 h-1.5 rounded-full bg-[var(--range-bg)] shadow-[inset_0_0_0_1px_rgba(255,255,255,0.04)]"
                                    style={{
                                        background: `linear-gradient(to right,
                      var(--range-bg) 0%,
                      var(--range-bg) ${pct(minAge)}%,
                      var(--range-fill) ${pct(minAge)}%,
                      var(--range-fill) ${pct(maxAge)}%,
                      var(--range-bg) ${pct(maxAge)}%,
                      var(--range-bg) 100%)`,
                                    }}
                                />
                                <input
                                    type="range"
                                    className={RANGE_INPUT}
                                    min={rangeMin}
                                    max={rangeMax}
                                    value={minAge}
                                    onChange={(e) => setMinAge(Math.min(clampAge(e.target.value), maxAge))}
                                />
                                <input
                                    type="range"
                                    className={RANGE_INPUT}
                                    min={rangeMin}
                                    max={rangeMax}
                                    value={maxAge}
                                    onChange={(e) => setMaxAge(Math.max(clampAge(e.target.value), minAge))}
                                />
                                <span className="absolute -top-[18px] -translate-x-1/2 bg-[#4b5563] text-white text-[10px] leading-none rounded px-[5px] py-[3px] pointer-events-none" style={{ left: `calc(${pct(minAge)}% - 12px)` }}>{minAge}</span>
                                <span className="absolute -top-[18px] -translate-x-1/2 bg-[#4b5563] text-white text-[10px] leading-none rounded px-[5px] py-[3px] pointer-events-none" style={{ left: `calc(${pct(maxAge)}% - 12px)` }}>{maxAge}</span>
                                <div className="absolute left-0 right-0 -bottom-4 h-3 text-[10px] text-[#8a98ac] [&_span]:absolute [&_span]:-translate-x-1/2">
                                    {[0, 25, 50, 75, 100].map(tick => (
                                        <span key={tick} style={{ left: `${tick}%` }}>{tick}</span>
                                    ))}
                                </div>
                            </div>
                        </div>

                        <div className="[&_label]:block [&_label]:text-xs [&_label]:uppercase [&_label]:text-[#8a98ac] [&_label]:tracking-[0.06em] [&_label]:mb-1.5 [&_input]:w-full [&_input]:px-3 [&_input]:py-2.5 [&_input]:rounded-xl [&_input]:border [&_input]:border-[#243244] [&_input]:outline-none [&_input]:bg-[#121923] [&_input]:text-[#e5edf6] [&_input]:transition-all [&_input::placeholder]:text-[#94a3b8] [&_input:focus]:border-[#4da3ff] [&_input:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)] [&_select]:w-full [&_select]:px-3 [&_select]:py-2.5 [&_select]:rounded-xl [&_select]:border [&_select]:border-[#243244] [&_select]:outline-none [&_select]:bg-[#121923] [&_select]:text-[#e5edf6] [&_select]:transition-all [&_select:focus]:border-[#4da3ff] [&_select:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)]">
                            <label>{t('filters.heightFrom', 'Рост от')}</label>
                            <select value={heightFrom} onChange={(e) => setHeightFrom(e.target.value)}>
                                <option value="">{t('filters.any', 'Любой')}</option>
                                {heightOptions.map(h => (
                                    <option key={h} value={h}>{h} {t('units.cm', 'см')}</option>
                                ))}
                            </select>
                        </div>

                        <div className="[&_label]:block [&_label]:text-xs [&_label]:uppercase [&_label]:text-[#8a98ac] [&_label]:tracking-[0.06em] [&_label]:mb-1.5 [&_input]:w-full [&_input]:px-3 [&_input]:py-2.5 [&_input]:rounded-xl [&_input]:border [&_input]:border-[#243244] [&_input]:outline-none [&_input]:bg-[#121923] [&_input]:text-[#e5edf6] [&_input]:transition-all [&_input::placeholder]:text-[#94a3b8] [&_input:focus]:border-[#4da3ff] [&_input:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)] [&_select]:w-full [&_select]:px-3 [&_select]:py-2.5 [&_select]:rounded-xl [&_select]:border [&_select]:border-[#243244] [&_select]:outline-none [&_select]:bg-[#121923] [&_select]:text-[#e5edf6] [&_select]:transition-all [&_select:focus]:border-[#4da3ff] [&_select:focus]:shadow-[0_0_0_2px_rgba(77,163,255,0.28)]">
                            <label>&nbsp;</label>
                            <button
                                type="button"
                                className="w-full px-3 py-2.5 rounded-xl border border-[#243244] bg-[#121923] text-[#e5edf6] cursor-pointer transition-all hover:bg-[#15202b] hover:border-[#2b3a4f] active:translate-y-px"
                                onClick={() => {
                                    setQuery('');
                                    setGender('all');
                                    setCtype('all');
                                    setMinAge(rangeMin);
                                    setMaxAge(rangeMax);
                                    setHeightFrom('');
                                }}
                            >
                                {t('actions.reset', 'Сбросить')}
                            </button>
                        </div>
                    </div>

                    <div className="text-[13px] text-[#8a98ac]">
                        {t('models.found', 'Найдено')}: {filtered.length}
                    </div>
                </div>

                {loading ? (
                    <Loader label={t('common.loading', 'Loading...')} />
                ) : filtered.length === 0 ? (
                    <EmptyState
                        title={t('common.emptyTitle', 'Подходящих моделей не найдено')}
                        subtitle={t('common.emptySubtitle', 'Измените фильтры или попробуйте другой запрос')}
                    />
                ) : (

                    <div className="grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-4">
                        {filtered.map((m) => (
                            <ModelCard
                                key={m.id}
                                model={m}
                                openModal={openModal}
                                t={t}
                                getFirstName={getFirstName}
                                SmartImage={SmartImage}
                                open={open}
                            />
                        ))}
                    </div>


                )}
            </section>

            {open && current && (
                <div className="fixed inset-0 bg-[rgba(7,10,14,0.7)] backdrop-blur-[2px] flex items-center justify-center p-4 z-[1000]" onClick={closeModal}>
                    <div className="bg-[#161f2a] border border-[#243244] rounded-2xl p-5 max-w-[980px] w-[92%] max-h-[90vh] overflow-y-auto relative shadow-[0_24px_64px_rgba(0,0,0,0.6)] text-[#e5edf6] animate-modalIn" onClick={(e) => e.stopPropagation()}>
                        <button className="absolute z-[100] top-2.5 right-2.5 border border-[#243244] bg-[#121923] text-[#e5edf6] rounded-full w-9 h-9 cursor-pointer text-base grid place-items-center transition-all hover:bg-[#192433] hover:border-[#31465f] active:scale-[0.98]" onClick={closeModal}>✕</button>

                        <div className="flex gap-5 mb-5 max-[860px]:flex-col">
                            <div className="flex-[0_0_300px] rounded-xl overflow-hidden bg-[#0f1722] border border-[#243244] [&_img]:w-full [&_img]:h-[315px] [&_img]:block [&_img]:object-cover">
                                {current.photoUrls && current.photoUrls.length > 0 ? (
                                    <Carousel
                                        indicators={current.photoUrls.length > 1}
                                        controls={current.photoUrls.length > 1}
                                        interval={null}
                                        className="relative w-full h-full [&_img]:w-full [&_img]:h-full [&_img]:object-cover [&_img]:object-center [&_.carousel-control-prev]:top-1/2 [&_.carousel-control-prev]:-translate-y-1/2 [&_.carousel-control-prev]:w-[10%] [&_.carousel-control-prev]:h-10 [&_.carousel-control-next]:top-1/2 [&_.carousel-control-next]:-translate-y-1/2 [&_.carousel-control-next]:w-[10%] [&_.carousel-control-next]:h-10 [&_.carousel-indicators]:bottom-2 [&_.carousel-indicators_[data-bs-target]]:w-2 [&_.carousel-indicators_[data-bs-target]]:h-2 [&_.carousel-indicators_[data-bs-target]]:rounded-full"
                                    >
                                        {current.photoUrls.map((p, index) => (
                                            <Carousel.Item key={index}>
                                                <div className="w-full h-full">
                                                    <img
                                                        className="d-block w-100"
                                                        src={p.url}
                                                        alt={`${current.name} ${index + 1}`}
                                                    />
                                                </div>
                                            </Carousel.Item>
                                        ))}
                                    </Carousel>

                                ) : (
                                    <img
                                        src="https://via.placeholder.com/400x500?text=No+Photo"
                                        alt={current.name}
                                        className="no-photo-placeholder"
                                    />
                                )}
                            </div>

                            <div className="flex-1 [&_dl]:grid [&_dl]:grid-cols-[160px_1fr] [&_dl]:gap-x-[14px] [&_dl]:gap-y-2 [&_dl]:m-0 [&_dt]:text-xs [&_dt]:uppercase [&_dt]:text-[#8a98ac] [&_dt]:tracking-[0.05em] [&_dd]:m-0 [&_dd]:font-medium [&_dd]:text-[#c3cfde]">
                                <div className="relative flex items-center text-center gap-2.5">
                                    <div className="flex flex-col items-center gap-1">
                                        <h2 className="text-xl font-bold mt-0 mb-2 mx-0 text-[#e5edf6]">{getFirstName(current.name)}</h2>
                                    </div>
                                    <div className="text-sm text-[#838282] bg-black/5 px-2 py-0.5 rounded mb-[5px]">ID: {current.id}</div>
                                </div>

                                <dl>
                                    <dt>{t('modal.age', 'Возраст')}</dt>
                                    <dd>{t('units.years', { count: current.age ?? 0 })}</dd>
                                    <dt>{t('modal.height', 'Рост')}</dt>
                                    <dd>{fmt(current.height)} {t('units.cm', 'см')}</dd>
                                    <dt>{t('modal.appearanceType', 'Тип внешности')}</dt>
                                    <dd>{t(`filters.castingType.options.${current.castingType}`, current.castingType || '—')}</dd>
                                    <dt>{t('modal.hairColor', 'Цвет волос')}</dt>
                                    <dd>{fmt(current.hairColor) || '—'}</dd>
                                    <dt>{t('modal.eyeColor', 'Цвет глаз')}</dt>
                                    <dd>{fmt(current.eyeColor) || '—'}</dd>
                                    <dt>{t('modal.gender', 'Пол')}</dt>
                                    <dd>
                                        {fmt(
                                            current.gender === "female"
                                                ? t("modal.gender_female")
                                                : t("modal.gender_male")
                                        )}
                                    </dd>
                                </dl>
                            </div>
                        </div>

                        <div className="mt-[18px] [&_h3]:text-[15px] [&_h3]:text-[#e5edf6] [&_h3]:mt-0 [&_h3]:mb-2 [&_h3]:mx-0 [&_h3]:tracking-[0.02em]">
                            <h3>
                                {t('models.photos', 'ФОТО')} (
                                {(current.photos || []).filter(p => p.isWebShow).length}
                                )
                            </h3>

                            <div className="flex gap-2.5 overflow-x-auto pb-1 [scroll-snap-type:x_proximity] [&_img]:w-[120px] [&_img]:h-[160px] [&_img]:object-cover [&_img]:rounded-[10px] [&_img]:cursor-pointer [&_img]:flex-shrink-0 [&_img]:border [&_img]:border-[#243244] [&_img]:bg-[#0f1722] [&_img]:[scroll-snap-align:start] [&_img]:transition-all [&_img:hover]:-translate-y-0.5 [&_img:hover]:border-[#335174] [&_img:hover]:shadow-[0_12px_28px_rgba(0,0,0,0.35)]">
                                {(current.photos || [])
                                    .filter(p => p.isWebShow === true)
                                    .map((p, idx) => (
                                        <img
                                            key={idx}
                                            src={`${baseUrl}/api/v1/file/getFile/${p.id}`}
                                            alt={`${current.name}-${idx}`}
                                            onClick={() =>
                                                setZoomPhoto(`${baseUrl}/api/v1/file/getFile/${p.id}`)
                                            }
                                        />
                                    ))}
                            </div>
                            <div className="mt-3 flex items-center gap-3">
                                <button
                                    type="button"
                                    className="bg-[#4da3ff] text-[#0b1220] border border-[#2e5fa1] px-3.5 py-2.5 rounded-xl font-semibold cursor-pointer transition-all shadow-[0_6px_18px_rgba(77,163,255,0.25)] hover:bg-[#2e8fff] hover:shadow-[0_10px_28px_rgba(77,163,255,0.35)] active:translate-y-px"
                                    onClick={handleContact}
                                    disabled={contactLock}
                                    aria-disabled={contactLock}
                                    title={contactLock ? t('common.loading', 'Loading...') : undefined}
                                >
                                    {t('actions.contact', 'Связаться')}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {zoomPhoto && (
                <div className="fixed inset-0 bg-[rgba(5,8,12,0.92)] flex items-center justify-center z-[2000] animate-fadeInOpacity [&_img]:max-w-[92%] [&_img]:max-h-[92%] [&_img]:rounded-[14px] [&_img]:border [&_img]:border-[#243244] [&_img]:shadow-[0_24px_64px_rgba(0,0,0,0.65)]" onClick={() => setZoomPhoto(null)}>
                    <img src={zoomPhoto} alt="zoom" />
                </div>
            )}

            <Footer />
        </div>
    );
}

export default Models;