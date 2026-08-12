// src/pages/home/Home.jsx
import React, { useEffect, useState } from "react";
import Header from "../header/Header";
import "react-responsive-modal/styles.css";
import "./home.css";
import { useNavigate } from "react-router-dom";
import { FaArrowDown, FaCheckCircle } from "react-icons/fa";
import { useTranslation } from "react-i18next";
import bg from "../../images/bg.jpg"
import ImageWithLightAnimation from "./ImageWithLightAnimation"
import face from "../../images/bashara.png"
import banner from "../../images/banner.jpg"
import one from "../../images/1.jpg"
import two from "../../images/2.jpg"
import three from "../../images/3.jpg"
import Footer from "../footer/Footer"
import { motion } from "framer-motion";
import { fadeIn } from "../framerMotion/variants";

import useTypingEffect from "./useTypingEffect";

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
    const heroTitle = t("hero.title") ?? "";
    const heroSubtitle = t("hero.subtitle") ?? "";

    return (
        <div className="home-container">
            <Header />
            <ImageWithLightAnimation src={bg} alt="Tog' manzarasi" />

            {/* ===== HERO ===== */}
            <section

                className="hero">
                <div className="hero-content">

                    <motion.h1
                        variants={fadeIn("up", 0.5)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}
                        className="hero-title">
                        {t("hero.title")}
                    </motion.h1>
                    <motion.p
                        variants={fadeIn("up", 0.7)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}

                        className="hero-subtitle">{heroSubtitle}</motion.p>

                    <motion.div
                        variants={fadeIn("up", 0.9)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}
                        className="hero-features">
                        <div className="hf-card">
                            <FaCheckCircle className="hf-icon" />
                            <div>
                                <h4>{t("hero.feature1Title")}</h4>
                                <p>{t("hero.feature1Text")}</p>
                            </div>
                        </div>
                        <div className="hf-card">
                            <FaCheckCircle className="hf-icon" />
                            <div>
                                <h4>{t("hero.feature2Title")}</h4>
                                <p>{t("hero.feature2Text")}</p>
                            </div>
                        </div>
                        <div className="hf-card">
                            <FaCheckCircle className="hf-icon" />
                            <div>
                                <h4>{t("hero.feature3Title")}</h4>
                                <p>{t("hero.feature3Text")}</p>
                            </div>
                        </div>
                    </motion.div>

                    <div className="cta-buttons" role="group" aria-label="Casting choices">
                        <a href="https://t.me/uzcastingbot" target="_blank" className="register-btn">
                            {t("hero.btnApplicant")}
                        </a>
                        <button className="register-btn" onClick={goClient}>
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
                className="director">
                <div className="director-card">
                    <div className="director-media">
                        <img src={face} alt="Sattorov Jasur — Producer / Director" />
                    </div>
                    <div className="director-info">
                        <h2>{t("director.heading")}</h2>
                        <p>{t("director.about")}</p>
                    </div>
                </div>
            </motion.section>

            {/* ===== SHOWCASE: Films ===== */}
            <section className="showcase">
                <div className="showcase-head">
                    <h2>{t("showcase.films.title")}</h2>
                </div>
                <div className="showcase-grid">
                    {/* Левая колонка: постер + трейлер */}
                    <motion.div
                        variants={fadeIn("down", 0.5)}
                        initial="hidden"
                        whileInView="show"
                        viewport={{ once: false, amount: 0 }}
                        className="showcase-media"
                    >
                        <div className="showcase-img">
                            <img src={banner} alt="Maxsus Bo‘lim — poster" />
                            <figcaption>{t("showcase.films.posterCaption")}</figcaption>
                        </div>

                        <div className="showcase-video">
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
                        className="showcase-body"
                    >
                        <ul className="showcase-list">
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
            <section className="showcase showcase-clips">
                <div className="showcase-head">
                    <h2>{t("showcase.clips.title")}</h2>
                </div>

                <div className="clips-grid">
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
                            className="clip-item"
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

                <p className="clips-note">{t("showcase.clips.more")}</p>
            </section>

            {/* Floating button */}
            <a
                href="https://t.me/uzcastingbot"
                className="fixed-navigate-btn circle-marquee"
                aria-label="Ro'yhatdan o'tish"
            >
                <svg className="marquee-svg" viewBox="0 0 100 100" aria-hidden="true">
                    <defs>
                        <path
                            id="textcircle"
                            d="M50,50 m-36,0 a36,36 0 1,1 72,0 a36,36 0 1,1 -72,0"
                        />
                    </defs>
                    <text className="circle-text">
                        <textPath href="#textcircle" startOffset="0%">
                            {"ro'yhatdan o'tish • ro'yhatdan o'tish • ro'yhatdan o'tish • ro'yhatdan o'tish • "}
                        </textPath>
                    </text>
                </svg>

                <span className="circle-center">
                    <FaArrowDown className="arrow-bounce" aria-hidden="true" />
                </span>
            </a>

            <Footer />
        </div>

    );
}

export default Home;
