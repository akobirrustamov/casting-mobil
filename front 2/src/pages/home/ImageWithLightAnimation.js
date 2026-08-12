import React, { useState, useEffect } from 'react';

const ImageWithLightAnimation = ({
    src,
    alt = "Image with light animation",
    animationSpeed = 1000 // Har bir yo'nalish 3 soniya davom etadi
}) => {
    const [animationPhase, setAnimationPhase] = useState(0);

    useEffect(() => {
        const interval = setInterval(() => {
            setAnimationPhase(prev => (prev + 1) % 4);
        }, animationSpeed);

        return () => clearInterval(interval);
    }, [animationSpeed]);

    return (
        <div
            className=" w-full h-1/2 relative overflow-hidden rounded-lg shadow-xl bg-gray-900"
        >
            {/* Asosiy rasm */}
            <img
                src={src}
                alt={alt}
                className={`w-full h-full opacity-90`}
            />

            {/* Aylana shaklidagi yorug'lik effektlari */}
            <div className="absolute inset-0 pointer-events-none">
                {/* 1. Yuqori chap - aylana */}
                <div
                    className={`absolute top-14 left-14 w-24 h-24 bg-white/20 transition-all duration-1000 ${animationPhase === 0 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 2. Yuqori markaz - aylana */}
                <div
                    className={`absolute top-14 left-1/2 transform -translate-x-1/2 w-28 h-28 bg-white/20 transition-all duration-1000 ${animationPhase === 0 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 3. Yuqori o'ng - aylana */}
                <div
                    className={`absolute top-14 right-14 w-24 h-24 bg-white/20 transition-all duration-1000 ${animationPhase === 0 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 4. O'ng yuqori - aylana */}
                <div
                    className={`absolute top-1/4 right-14 w-24 h-24 bg-white/20 transition-all duration-1000 ${animationPhase === 1 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 5. O'ng markaz - aylana */}
                <div
                    className={`absolute top-1/2 right-14 transform -translate-y-1/2 w-28 h-28 bg-white/20 transition-all duration-1000 ${animationPhase === 1 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 6. O'ng past - aylana */}
                <div
                    className={`absolute bottom-1/4 right-14 w-24 h-24 bg-white/20 transition-all duration-1000 ${animationPhase === 1 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 7. Pastki o'ng - aylana */}
                <div
                    className={`absolute bottom-14 right-14 w-24 h-24 bg-white/20 transition-all duration-1000 ${animationPhase === 2 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 8. Pastki markaz - aylana */}
                <div
                    className={`absolute bottom-14 left-1/2 transform -translate-x-1/2 w-28 h-28 bg-white/20 transition-all duration-1000 ${animationPhase === 2 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 9. Pastki chap - aylana */}
                <div
                    className={`absolute bottom-14 left-14 w-24 h-24 bg-white/20 transition-all duration-1000 ${animationPhase === 2 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 10. Chap past - aylana */}
                <div
                    className={`absolute bottom-1/4 left-14 w-24 h-24 bg-white/20 transition-all duration-1000 ${animationPhase === 3 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 11. Chap markaz - aylana */}
                <div
                    className={`absolute top-1/2 left-14 transform -translate-y-1/2 w-28 h-28 bg-white/20 transition-all duration-1000 ${animationPhase === 3 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />

                {/* 12. Chap yuqori - aylana */}
                <div
                    className={`absolute top-1/4 left-14 w-24 h-24 bg-white/20 transition-all duration-1000 ${animationPhase === 3 ? 'opacity-100 scale-100' : 'opacity-0 scale-50'}`}
                    style={{
                        filter: 'blur(15px)',
                        borderRadius: '50%'
                    }}
                />
            </div>

        </div >
    );
};

export default ImageWithLightAnimation;
