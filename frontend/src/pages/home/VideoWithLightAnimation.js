import React, { useState, useEffect, useRef } from 'react';

// Foydalanuvchi animatsiyani kamaytirishni yoki trafikni tejashni so'ragan bo'lsa,
// videoni umuman yuklamaymiz — faqat poster rasm ko'rsatiladi.
const prefersLightMode = () => {
    if (typeof window === 'undefined') return false;
    const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
    const saveData = navigator.connection?.saveData;
    return Boolean(reducedMotion || saveData);
};

const VideoWithLightAnimation = ({
    src,
    poster,
    alt = "Video with light animation",
    animationSpeed = 1000 // Har bir yo'nalish 1 soniya davom etadi
}) => {
    const [animationPhase, setAnimationPhase] = useState(0);
    const [inView, setInView] = useState(false);
    const [lightMode] = useState(prefersLightMode);
    const containerRef = useRef(null);
    const videoRef = useRef(null);

    // Ekranda ko'rinmayotgan bo'lsa — video ham, yorug'lik animatsiyasi ham to'xtaydi
    useEffect(() => {
        const node = containerRef.current;
        if (!node || typeof IntersectionObserver === 'undefined') {
            setInView(true);
            return;
        }

        const observer = new IntersectionObserver(
            ([entry]) => setInView(entry.isIntersecting),
            { threshold: 0.1 }
        );
        observer.observe(node);
        return () => observer.disconnect();
    }, []);

    useEffect(() => {
        if (!inView || lightMode) return;

        const interval = setInterval(() => {
            setAnimationPhase(prev => (prev + 1) % 4);
        }, animationSpeed);

        return () => clearInterval(interval);
    }, [animationSpeed, inView, lightMode]);

    useEffect(() => {
        const video = videoRef.current;
        if (!video) return;

        if (inView) {
            const play = video.play();
            if (play?.catch) play.catch(() => { }); // avtoplay bloklansa — jim o'tkazamiz
        } else {
            video.pause();
        }
    }, [inView]);

    return (
        <div
            ref={containerRef}
            className=" w-full h-1/2 relative overflow-hidden rounded-lg shadow-xl bg-gray-900"
        >
            {/* Asosiy video */}
            {lightMode ? (
                <img src={poster} alt={alt} className="w-full h-full object-cover opacity-90" />
            ) : (
                <video
                    ref={videoRef}
                    src={src}
                    poster={poster}
                    aria-label={alt}
                    className="w-full h-full object-cover opacity-90"
                    autoPlay
                    muted
                    loop
                    playsInline
                    preload="metadata"
                    disablePictureInPicture
                    disableRemotePlayback
                />
            )}



        </div >
    );
};

export default VideoWithLightAnimation;
