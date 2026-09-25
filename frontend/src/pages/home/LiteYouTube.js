import React, { useState } from "react";

// YouTube iframe'ni darhol qo'ymaymiz: har biri ~1 MB JS yuklaydi, bosh
// sahifada esa ular 7 ta. Avval faqat muqova rasmi (bir necha KB)
// ko'rsatiladi, bosilgandagina haqiqiy pleer ochiladi va o'zi boshlanadi.
function LiteYouTube({ id, title, className = "" }) {
    const [active, setActive] = useState(false);

    if (active) {
        return (
            <iframe
                className={`w-full aspect-video ${className}`}
                src={`https://www.youtube.com/embed/${id}?autoplay=1`}
                title={title}
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                referrerPolicy="strict-origin-when-cross-origin"
                allowFullScreen
            ></iframe>
        );
    }

    return (
        <button
            type="button"
            onClick={() => setActive(true)}
            aria-label={`${title} — ijro etish`}
            className={`group relative block w-full aspect-video overflow-hidden bg-black p-0 cursor-pointer ${className}`}
        >
            <img
                src={`https://i.ytimg.com/vi/${id}/hqdefault.jpg`}
                alt={title}
                loading="lazy"
                decoding="async"
                width="480"
                height="360"
                className="absolute inset-0 w-full h-full object-cover"
            />
            <span className="absolute inset-0 flex items-center justify-center">
                <svg viewBox="0 0 68 48" width="68" height="48" aria-hidden="true" className="opacity-90 group-hover:opacity-100 transition-opacity">
                    <path d="M66.5 7.7a8.5 8.5 0 0 0-6-6C55.3.3 34 .3 34 .3s-21.3 0-26.5 1.4a8.5 8.5 0 0 0-6 6C.1 13 .1 24 .1 24s0 11 1.4 16.3a8.5 8.5 0 0 0 6 6C12.7 47.7 34 47.7 34 47.7s21.3 0 26.5-1.4a8.5 8.5 0 0 0 6-6C67.9 35 67.9 24 67.9 24s0-11-1.4-16.3z" fill="#f00" />
                    <path d="M45 24 27 14v20z" fill="#fff" />
                </svg>
            </span>
        </button>
    );
}

export default LiteYouTube;
