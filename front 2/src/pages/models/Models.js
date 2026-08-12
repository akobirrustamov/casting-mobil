import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import ApiCall, { baseUrl } from '../../config';
import './Models.css';
import Header from '../header/Header';
import Loader from './Loader';
import EmptyState from './EmptyState';
import Footer from "../footer/Footer";
import "bootstrap/dist/css/bootstrap.css";
import Carousel from "react-bootstrap/Carousel";
import ModelCard from "./ModelCard";


function Models() {
    const { t } = useTranslation();

    const [items, setItems] = useState([]);
    const [open, setOpen] = useState(false);
    const [current, setCurrent] = useState(null);
    const [loading, setLoading] = useState(true);
    const [zoomPhoto, setZoomPhoto] = useState(null);
    const [contactLock, setContactLock] = useState(false);
    console.log("weasdrghjkhgfdsa");

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
                console.log(res.data);

                const list = Array.isArray(res) ? res : Array.isArray(res?.data) ? res.data : [];
                console.log(list);

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
            <div className="card-carousel-image" style={style}>
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
        <div className="models-page">
            <Header props="" />
            <section className="container">
                <div className="toolbar">
                    <h1 className="title">{t('models.title', 'Models / Actors')}</h1>

                    <div className="filters gap-4">
                        <div className="filter-item">
                            <label>{t('filters.search', 'Поиск')}</label>
                            <input
                                type="text"
                                placeholder={t('filters.searchPlaceholder', 'Имя, фамилия…')}
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                            />
                        </div>

                        <div className="filter-item">
                            <label>{t('filters.gender', 'Пол')}</label>
                            <select value={gender} onChange={(e) => setGender(e.target.value)}>
                                <option value="all">{t('filters.genderAny', 'Любой')}</option>
                                <option value="male">{t('filters.genderMale', 'Мужской')}</option>
                                <option value="female">{t('filters.genderFemale', 'Женский')}</option>
                            </select>
                        </div>

                        <div className="filter-item ">
                            <label>{t('filters.castingType.label', 'Casting type')}</label>
                            <select value={ctype} onChange={(e) => setCtype(e.target.value)}>
                                {castingTypeOptions.map(o => (
                                    <option key={o.value} value={o.value}>{o.label}</option>
                                ))}
                            </select>
                        </div>

                        <div className="filter-item age-block  ">
                            <label>{t('filters.age', 'Возраст')}</label>
                            <div className="dual-range">
                                <div
                                    className="dual-range__track"
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
                                    min={rangeMin}
                                    max={rangeMax}
                                    value={minAge}
                                    onChange={(e) => setMinAge(Math.min(clampAge(e.target.value), maxAge))}
                                />
                                <input
                                    type="range"
                                    min={rangeMin}
                                    max={rangeMax}
                                    value={maxAge}
                                    onChange={(e) => setMaxAge(Math.max(clampAge(e.target.value), minAge))}
                                />
                                <span className="dual-range__badge" style={{ left: `calc(${pct(minAge)}% - 12px)` }}>{minAge}</span>
                                <span className="dual-range__badge dual-range__badge--right" style={{ left: `calc(${pct(maxAge)}% - 12px)` }}>{maxAge}</span>
                                <div className="dual-range__ticks">
                                    {[0, 25, 50, 75, 100].map(tick => (
                                        <span key={tick} style={{ left: `${tick}%` }}>{tick}</span>
                                    ))}
                                </div>
                            </div>
                        </div>

                        <div className="filter-item ">
                            <label>{t('filters.heightFrom', 'Рост от')}</label>
                            <select value={heightFrom} onChange={(e) => setHeightFrom(e.target.value)}>
                                <option value="">{t('filters.any', 'Любой')}</option>
                                {heightOptions.map(h => (
                                    <option key={h} value={h}>{h} {t('units.cm', 'см')}</option>
                                ))}
                            </select>
                        </div>

                        <div className="filter-item">
                            <label>&nbsp;</label>
                            <button
                                type="button"
                                className="btn-reset"
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

                    <div className="result-count">
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

                    <div className="grid">
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
                <div className="cast-backdrop" onClick={closeModal}>
                    <div className="cast-modal" onClick={(e) => e.stopPropagation()}>
                        <button className="cast-modal-close" onClick={closeModal}>✕</button>

                        <div className="cast-modal-body">
                            <div className="profile-photo">
                                {current.photoUrls && current.photoUrls.length > 0 ? (
                                    <Carousel
                                        indicators={current.photoUrls.length > 1}
                                        controls={current.photoUrls.length > 1}
                                        interval={null}
                                        className="model-carousel"
                                    >
                                        {current.photoUrls.map((p, index) => (
                                            <Carousel.Item key={index}>
                                                <div className="carousel-image-container">
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

                            <div className="profile-info">
                                <div className="profile-head">
                                    <div className="profile-center">
                                        <h2 className="profile-name">{getFirstName(current.name)}</h2>
                                    </div>
                                    <div className="profile-id">ID: {current.id}</div>
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

                        <div className="profile-gallery">
                            <h3>
                                {t('models.photos', 'ФОТО')} (
                                {(current.photos || []).filter(p => p.isWebShow).length}
                                )
                            </h3>

                            <div className="gallery-row">
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
                            <div className="contact-row">
                                <button
                                    type="button"
                                    className="btn-primary"
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
                <div className="lightbox" onClick={() => setZoomPhoto(null)}>
                    <img src={zoomPhoto} alt="zoom" />
                </div>
            )}

            <Footer />
        </div>
    );
}

export default Models;