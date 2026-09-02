// src/pages/home/Home.jsx
import React, { useEffect, useState } from "react";
import Header from "../header/Header";
import { registerBtnClass } from "../../shared/headerStyles";
import "react-responsive-modal/styles.css";
import { useNavigate } from "react-router-dom";
import { FaArrowDown, FaCheckCircle } from "react-icons/fa";
import { useTranslation } from "react-i18next";
import bg from "../../images/bg.jpg"
import VideoWithLightAnimation from "./VideoWithLightAnimation"
import face from "../../images/bashara.png"
import banner from "../../images/banner.jpg"
import one from "../../images/1.jpg"
import two from "../../images/2.jpg"
import three from "../../images/3.jpg"
import Footer from "../footer/Footer"
import { motion } from "framer-motion";
import { fadeIn } from "../framerMotion/variants";


function Home() {
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();

    const [isMobile, setIsMobile] = useState(false);

    useEffect(() => {
        const savedLanguage = localStorage.getItem("selectedLanguage") || "uz";
        if (savedLanguage !== i18n.language) i18n.changeLanguage(savedLanguage);

        const checkMobile = () => setIsMobile(window.innerWidth < 768);
        checkMobile();
        window.addEventListener("resize", checkMobile);
        return () => window.removeEventListener("resize", checkMobile);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const goClient = () => navigate(`/models`);

    // Берём строки из i18n; подстраховка от undefined/null
    const heroSubtitle = t("hero.subtitle") ?? "";

    return (
        <div className="min-h-screen flex flex-col bg-[#0b1220] text-[#f8f9fa] font-lora">
            <Header />
            <VideoWithLightAnimation
                src={`${process.env.PUBLIC_URL}/videos/sahna.mp4`}
                poster={bg}
                alt="Sahna ortidagi lavhalar"
            />

            {/* ===== HERO ===== */}
            <section

                className="w-full py-[clamp(32px,5vw,72px)] px-4 bg-[radial-gradient(1200px_600px_at_20%_-10%,rgba(255,255,255,0.06),transparent),linear-gradient(180deg,rgba(255,255,255,0.02),rgba(255,255,255,0))] border-b border-white/[0.06]">
                <div className="max-w-[1200px] mx-auto text-center">

                    <motion.h1
                        variants={fadeIn("up", 0.5)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}
                        className="text-[clamp(28px,3.6vw,44px)] leading-[1.2] font-extrabold tracking-[-0.02em] m-0 mb-4">
                        {t("hero.title")}
                    </motion.h1>
                    <motion.p
                        variants={fadeIn("up", 0.7)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}

                        className="text-[clamp(14px,1.6vw,18px)] text-[#bfc7d6] max-w-[800px] mt-0 mx-auto mb-7 leading-[1.6]">{heroSubtitle}</motion.p>

                    <motion.div
                        variants={fadeIn("up", 0.9)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}
                        className="grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-4 mt-[22px] mb-8 max-[900px]:grid-cols-1">
                        <div className="grid grid-cols-[24px_1fr] gap-3 items-start bg-white/[0.04] border border-white/[0.08] rounded-2xl px-4 py-3.5 backdrop-blur-[4px] [&_h4]:mt-0 [&_h4]:mb-1 [&_h4]:mx-0 [&_h4]:text-[15px] [&_h4]:font-bold [&_h4]:text-white [&_p]:m-0 [&_p]:text-[13px] [&_p]:text-[#cbd3e1]">
                            <FaCheckCircle className="text-[#4cf04f] text-xl mt-1" />
                            <div>
                                <h4>{t("hero.feature1Title")}</h4>
                                <p>{t("hero.feature1Text")}</p>
                            </div>
                        </div>
                        <div className="grid grid-cols-[24px_1fr] gap-3 items-start bg-white/[0.04] border border-white/[0.08] rounded-2xl px-4 py-3.5 backdrop-blur-[4px] [&_h4]:mt-0 [&_h4]:mb-1 [&_h4]:mx-0 [&_h4]:text-[15px] [&_h4]:font-bold [&_h4]:text-white [&_p]:m-0 [&_p]:text-[13px] [&_p]:text-[#cbd3e1]">
                            <FaCheckCircle className="text-[#4cf04f] text-xl mt-1" />
                            <div>
                                <h4>{t("hero.feature2Title")}</h4>
                                <p>{t("hero.feature2Text")}</p>
                            </div>
                        </div>
                        <div className="grid grid-cols-[24px_1fr] gap-3 items-start bg-white/[0.04] border border-white/[0.08] rounded-2xl px-4 py-3.5 backdrop-blur-[4px] [&_h4]:mt-0 [&_h4]:mb-1 [&_h4]:mx-0 [&_h4]:text-[15px] [&_h4]:font-bold [&_h4]:text-white [&_p]:m-0 [&_p]:text-[13px] [&_p]:text-[#cbd3e1]">
                            <FaCheckCircle className="text-[#4cf04f] text-xl mt-1" />
                            <div>
                                <h4>{t("hero.feature3Title")}</h4>
                                <p>{t("hero.feature3Text")}</p>
                            </div>
                        </div>
                    </motion.div>

                    <div className="flex justify-center gap-4 flex-wrap mt-2" role="group" aria-label="Casting choices">
                        <a href="https://t.me/uzcastingbot" target="_blank" className={registerBtnClass}>
                            {t("hero.btnApplicant")}
                        </a>
                        <button className={registerBtnClass} onClick={goClient}>
                            {t("hero.btnClient")}
                        </button>
                    </div>
                </div>
            </section>

            {/* ===== DIRECTOR ===== */}
            <motion.section
                variants={fadeIn("up", 0.5)}
                initial="hidden"
                whileInView="show"
                viewport={{ once: false, amount: 0 }}
                className="px-4 pt-[clamp(20px,3vw,36px)] pb-2">
                <div className="max-w-[1200px] mx-auto grid gap-10 grid-cols-[360px_1fr] max-[900px]:grid-cols-1 bg-white/[0.04] border border-white/[0.08] rounded-[18px] p-4">
                    <div className="[&_img]:w-full [&_img]:h-auto [&_img]:block [&_img]:rounded-2xl [&_img]:border [&_img]:border-white/[0.08] [&_img]:shadow-[0_18px_40px_rgba(0,0,0,0.35)] [&_img]:object-cover">
                        <img src={face} alt="Sattorov Jasur — Producer / Director" />
                    </div>
                    <div className="[&_h2]:mt-2 [&_h2]:mb-2.5 [&_h2]:mx-0 [&_h2]:text-[clamp(22px,2.6vw,28px)] [&_h2]:font-extrabold [&_p]:m-0 [&_p]:text-[#cfd6e6] [&_p]:leading-[1.65] [&_p]:text-justify">
                        <h2>{t("director.heading")}</h2>
                        <p>{t("director.about")}</p>
                    </div>
                </div>
            </motion.section>

            {/* ===== SHOWCASE: Films ===== */}
            <section className="px-4 pt-3 pb-7">
                <div className="[&_h2]:max-w-[1200px] [&_h2]:mt-0 [&_h2]:mb-6 [&_h2]:mx-auto [&_h2]:text-[clamp(22px,2.6vw,28px)] [&_h2]:font-extrabold">
                    <h2>{t("showcase.films.title")}</h2>
                </div>
                <div className="max-w-[1200px] mx-auto grid gap-6 grid-cols-2 items-start max-[900px]:grid-cols-1">
                    {/* Левая колонка: постер + трейлер */}
                    <motion.div
                        variants={fadeIn("down", 0.5)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}
                        className="flex flex-col gap-[18px]"
                    >
                        <div className="bg-white/[0.04] border border-white/[0.08] rounded-[18px] p-3 [&_img]:w-full [&_img]:rounded-[14px] [&_img]:object-cover [&_figcaption]:text-[13px] [&_figcaption]:text-[#bfc7d6] [&_figcaption]:mt-2 [&_figcaption]:text-center">
                            <img src={banner} alt="Maxsus Bo‘lim — poster" />
                            <figcaption>{t("showcase.films.posterCaption")}</figcaption>
                        </div>

                        <div className="[&_iframe]:rounded-[14px] [&_iframe]:border [&_iframe]:border-white/[0.08] [&_iframe]:shadow-[0_8px_20px_rgba(0,0,0,0.35)]">
                            <iframe
                                width="100%"
                                height="315"
                                src="https://www.youtube.com/embed/gF6kaevugtk?si=gWYT4VdZw32Cvxlf"
                                title="YouTube video player"
                                frameBorder="0"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                referrerPolicy="strict-origin-when-cross-origin"
                                allowFullScreen
                            ></iframe>
                        </div>

                        <div className="flex gap-1 w-full">
                            <img className="w-1/3" src={one} alt="one" />
                            <img className="w-1/3" src={two} alt="two" />
                            <img className="w-1/3" src={three} alt="three" />
                        </div>
                    </motion.div>

                    {/* Правая колонка: текст */}
                    <motion.div
                        variants={fadeIn("down", 0.5)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}
                        className="bg-white/[0.04] border border-white/[0.08] rounded-[18px] p-[clamp(14px,2.2vw,18px)]"
                    >
                        <ul className="list-none m-0 p-0 grid gap-3 [&_li]:bg-white/[0.03] [&_li]:border [&_li]:border-white/[0.06] [&_li]:rounded-xl [&_li]:px-3.5 [&_li]:py-3 [&_li]:text-[#d7deee] [&_li]:leading-[1.6] [&_strong]:text-white">
                            <li>
                                <strong>{t("showcase.films.ftitle")}</strong>
                                <br />
                                {t("showcase.films.fname")}
                            </li>
                            <li>
                                <strong>{t("showcase.films.genre")}</strong>
                                <br />
                                {t("showcase.films.genreVal")}
                            </li>
                            <li>
                                <strong>{t("showcase.films.producer")}</strong>
                                <br />
                                {t("showcase.films.producerVal")}
                            </li>
                            <li>
                                <strong>{t("showcase.films.places")}</strong>
                                <br />
                                <span style={{ whiteSpace: "pre-line" }}>
                                    {t("showcase.films.placesVal")}
                                </span>
                            </li>
                            <li>
                                <strong>{t("showcase.films.synopsis")}</strong>
                                <br />
                                {t("showcase.films.synopsisVal")}
                            </li>
                            <li>
                                <strong>{t("showcase.films.facts")}</strong>
                                <br />
                                <span style={{ whiteSpace: "pre-line" }}>
                                    {t("showcase.films.factsVal")}
                                </span>
                            </li>
                            <li>
                                <strong>{t("showcase.films.goal")}</strong>
                                <br />
                                <span style={{ whiteSpace: "pre-line" }}>
                                    {t("showcase.films.goalVal")}
                                </span>
                            </li>
                        </ul>
                    </motion.div>
                </div>
                <br />
                <hr />
            </section>

            {/* ===== SHOWCASE: Clips ===== */}
            <section className="px-4 pt-6 pb-10">
                <div className="[&_h2]:max-w-[1200px] [&_h2]:mt-0 [&_h2]:mb-6 [&_h2]:mx-auto [&_h2]:text-[clamp(22px,2.6vw,28px)] [&_h2]:font-extrabold">
                    <h2>{t("showcase.clips.title")}</h2>
                </div>

                <div className="max-w-[1200px] mx-auto grid gap-5 grid-cols-[repeat(auto-fit,minmax(300px,1fr))]">
                    {[
                        "https://www.youtube.com/embed/G650mrCmNWM?si=BEXo0vfEoU93_n3K",
                        "https://www.youtube.com/embed/w_ZOD_y68w0?si=ETVpry5UL02ocOJJ",
                        "https://www.youtube.com/embed/6chd2yev_Ug?si=KFRsU4NaMZ4eKq-d",
                        "https://www.youtube.com/embed/jGnlnNCW_WA?si=7z0xUgPCqe1uTInb",
                        "https://www.youtube.com/embed/_Ns_0M_1F3g?si=2yU5ZJN3Y32QhuwO",
                        "https://www.youtube.com/embed/npTIpW3IFHI?si=rSYmZ00uXQ_LTjS_",
                    ].map((src, index) => (
                        <motion.div
                            key={index}
                            variants={fadeIn("up", 0.2 * index)}
                            initial="hidden"
                            whileInView="show"
                            viewport={{ once: false, amount: 0 }}
                            className="[&_iframe]:w-full [&_iframe]:aspect-video [&_iframe]:rounded-[14px] [&_iframe]:border [&_iframe]:border-white/[0.08] [&_iframe]:shadow-[0_8px_20px_rgba(0,0,0,0.35)]"
                        >
                            <iframe
                                src={src}
                                title={`YouTube video player ${index + 1}`}
                                frameBorder="0"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                referrerPolicy="strict-origin-when-cross-origin"
                                allowFullScreen
                            ></iframe>
                        </motion.div>
                    ))}
                </div>

                <p className="mt-4 text-center text-[#cfd6e6] text-[15px]">{t("showcase.clips.more")}</p>
            </section>

            {/* Floating button */}
            <a
                href="https://t.me/uzcastingbot"
                className="group fixed bottom-5 right-5 z-[1000] cursor-pointer border-none p-0 overflow-hidden rounded-full w-[90px] h-[90px] max-md:w-[65px] max-md:h-[65px] flex items-center justify-center bg-[radial-gradient(60%_60%_at_50%_50%,rgba(55,48,163),rgba(67,56,202))] max-md:bg-[radial-gradient(60%_60%_at_50%_50%,rgba(255,255,255,0.06),rgba(255,255,255,0.02)),#4895ef] shadow-[0_4px_15px_rgba(37,99,235,0.5)] transition-all duration-300 hover:scale-[1.15] hover:shadow-[0_6px_20px_rgba(37,99,235,0.7)] after:content-[''] after:absolute after:-top-[10px] after:-left-[10px] after:-right-[10px] after:-bottom-[10px] after:rounded-full after:animate-pulseRing2 after:pointer-events-none"
                aria-label="Ro'yhatdan o'tish"
            >
                <svg className="absolute inset-0 w-full h-full pointer-events-none animate-textRotate [animation-duration:10s] origin-center group-hover:[animation-play-state:paused]" viewBox="0 0 100 100" aria-hidden="true">
                    <defs>
                        <path
                            id="textcircle"
                            d="M50,50 m-36,0 a36,36 0 1,1 72,0 a36,36 0 1,1 -72,0"
                        />
                    </defs>
                    <text className="text-[7px] tracking-[2.2px] uppercase fill-white">
                        <textPath href="#textcircle" startOffset="0%">
                            {"ro'yhatdan o'tish • ro'yhatdan o'tish • ro'yhatdan o'tish • ro'yhatdan o'tish • "}
                        </textPath>
                    </text>
                </svg>

                <span className="absolute left-1/2 top-1/2 w-[60px] h-[60px] -translate-x-1/2 -translate-y-1/2 flex items-center justify-center bg-transparent border-none pointer-events-none">
                    <FaArrowDown className="text-[28px] max-md:text-[15px] text-white animate-bounceY [animation-duration:1.5s]" aria-hidden="true" />
                </span>
            </a>

            <Footer />
        </div>

    );
}

export default Home;
