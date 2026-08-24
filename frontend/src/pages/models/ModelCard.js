// src/pages/models/ModelCard.js
import React, { useRef } from "react";
import Carousel from "react-bootstrap/Carousel";
import useOnScreen from "./useOnScreen";

function ModelCard({ model, openModal, t, getFirstName, SmartImage, open }) {
    const cardRef = useRef(null);
    const isVisible = useOnScreen(cardRef);

    // faqat isWebShow = true bo'lgan fotolar
    const visiblePhotos = (model.photoUrls || []).filter(p => p.isWebShow === true);

    return (
        <div
            className="group flex flex-col h-[340px] bg-[#1a2431] border border-[#2d3e53] rounded-[14px] overflow-hidden shadow-[0_12px_40px_rgba(0,0,0,0.45)] cursor-pointer transition-all duration-[180ms] hover:-translate-y-0.5 hover:bg-[#223043] hover:shadow-[0_16px_48px_rgba(0,0,0,0.55)]"
            ref={cardRef}
            role="button"
            onClick={() => openModal(model)}
        >
            <div className="flex-[1_0_auto] aspect-[3/4] bg-[#0f1722] overflow-hidden relative [&_img]:w-full [&_img]:h-full [&_img]:object-cover [&_img]:block [&_img]:[filter:saturate(1.02)_contrast(1.02)] [&_img]:transition-transform [&_img]:duration-[400ms] group-hover:[&_img]:scale-[1.02]">
                {visiblePhotos.length > 0 ? (
                    <Carousel
                        indicators={visiblePhotos.length > 1}
                        controls={visiblePhotos.length > 1}
                        interval={!open && isVisible ? 2500 : null}
                        className="relative w-full h-full [&_img]:w-full [&_img]:h-full [&_img]:object-cover [&_img]:object-center [&_.carousel-control-prev]:top-1/2 [&_.carousel-control-prev]:-translate-y-1/2 [&_.carousel-control-prev]:w-[10%] [&_.carousel-control-prev]:h-10 [&_.carousel-control-next]:top-1/2 [&_.carousel-control-next]:-translate-y-1/2 [&_.carousel-control-next]:w-[10%] [&_.carousel-control-next]:h-10 [&_.carousel-indicators]:bottom-2 [&_.carousel-indicators_[data-bs-target]]:w-2 [&_.carousel-indicators_[data-bs-target]]:h-2 [&_.carousel-indicators_[data-bs-target]]:rounded-full"
                    >
                        {visiblePhotos.map((p, index) => (
                            <Carousel.Item key={index}>
                                <SmartImage
                                    src={p.url}
                                    alt={`${model.name} ${index + 1}`}
                                    className="card-image"
                                />
                            </Carousel.Item>
                        ))}
                    </Carousel>
                ) : (
                    <img
                        src="https://via.placeholder.com/600x800?text=No+Photo"
                        alt={model.name}
                        loading="lazy"
                        className="card-placeholder"
                    />
                )}
            </div>

            <div className="relative text-center flex flex-col justify-center gap-1 flex-[0_0_72px] px-3 py-2.5 bg-transparent text-[#e5edf6]">
                <div className="flex flex-col items-center gap-0.5">
                    <div className="font-semibold text-base leading-[1.2] overflow-hidden text-ellipsis whitespace-nowrap text-[#e5edf6]">{getFirstName(model.name)}</div>
                    <div className="text-sm text-[#838282]">
                        {t("units.years", { count: model.age ?? 0 })}
                    </div>
                </div>
                <div className="absolute top-2 right-2 text-xs text-[#838282] bg-black/5 px-1.5 py-0.5 rounded">ID: {model.id}</div>
            </div>
        </div>
    );
}

export default ModelCard;
