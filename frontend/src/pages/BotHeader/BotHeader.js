import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import logo from "./logo.jpg";
import { FaInstagram, FaTelegramPlane } from 'react-icons/fa';

const socialIconClass = "text-[1.4rem] text-[#f3f4f6] transition-all duration-300 hover:text-[#3b82f6]";

const navLinkBase =
    "no-underline font-medium relative transition-colors duration-300 hover:text-[#2563eb] " +
    "max-lg:text-[1.2rem] max-lg:text-center max-lg:block max-lg:p-4 max-lg:w-full";

const navLinkClass = (isActive) =>
    navLinkBase + (isActive ? " text-[#2563eb]" : " text-[#f3f4f6]");

const langBtnClass = (isActive) =>
    "px-4 py-2 border rounded-md cursor-pointer transition-colors duration-300 " +
    (isActive
        ? "bg-[#2563eb] text-white border-[#2563eb]"
        : "bg-transparent text-[#f3f4f6] border-[#e5e7eb]");

const barBase = "w-full h-[3px] bg-[#f3f4f6] rounded-[10px] transition-all duration-300 ";

const barClass = (index, isOpen) => {
    if (!isOpen) return barBase;
    if (index === 0) return barBase + "rotate-45 translate-x-[5px] translate-y-[5px]";
    if (index === 1) return barBase + "opacity-0";
    return barBase + "-rotate-45 translate-x-[6px] -translate-y-[6px]";
};

function Header({ activeTab }) {
    const { userId } = useParams();
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [scrolled, setScrolled] = useState(false);
    const [isMobile, setIsMobile] = useState(window.innerWidth < 1024); // 768 emas, 1024 bo‘ldi

    useEffect(() => {
        const handleResize = () => {
            const mobile = window.innerWidth < 1024; // ⚡ endi tablet ham mobile hisoblanadi
            setIsMobile(mobile);
            if (!mobile) {
                setIsMenuOpen(false);
                document.body.style.overflow = 'auto';
            }
        };

        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const [language, setLanguage] = useState('uz');

    const toggleMenu = () => {
        const newState = !isMenuOpen;
        setIsMenuOpen(newState);
        document.body.style.overflow = newState ? 'hidden' : 'auto';
    };

    const closeMenu = () => {
        setIsMenuOpen(false);
        document.body.style.overflow = 'auto';
    };

    const changeLanguage = (lang) => {
        setLanguage(lang);
        localStorage.setItem('selectedLanguage', lang);
        window.location.reload();
    };

    const translations = {
        uz: { home: "Bosh Sahifa", casting: "Casting", my: "Tarix", language: "Til", uzbek: "UZ", russian: "RU" },
        ru: { home: "Главная", casting: "Кастинг", my: "История", language: "Язык", uzbek: "УЗ", russian: "РУ" }
    };

    return (
        <>
            <header
                className={
                    "fixed top-0 left-0 w-full z-[1000] bg-[rgba(15,15,15,0.95)] border-b border-[#334155] " +
                    "transition-all duration-300 animate-slideDown" +
                    (scrolled ? " shadow-[0_4px_6px_rgba(0,0,0,0.3)] backdrop-blur-[10px]" : "")
                }
            >
                <div className="max-w-[1200px] mx-auto px-6 h-20 flex justify-between items-center">
                    <div>
                        <Link
                            to={`/bot/${userId}`}
                            onClick={closeMenu}
                            className="flex items-center no-underline text-[#3b82f6] font-bold text-2xl"
                        >
                            <img
                                src={logo}
                                alt="Logo"
                                className="h-10 w-10 rounded-full object-cover mr-3 border-2 border-[#2563eb]"
                            />
                            <span>UzCasting</span>
                        </Link>
                    </div>

                    <div className="hidden lg:flex items-center gap-4">
                        <div className="relative">
                            <select
                                value={language}
                                onChange={(e) => changeLanguage(e.target.value)}
                                className="bg-[#111827] text-[#f3f4f6] border-[0.5px] border-[#374151] rounded-md px-[0.2rem] py-[0.1rem] text-[0.6rem] cursor-pointer transition-all duration-300 hover:border-[#2563eb]"
                            >
                                <option value="uz">{translations[language].uzbek}</option>
                                <option value="ru">{translations[language].russian}</option>
                            </select>
                        </div>
                        <div className="flex gap-4">
                            <a href="https://www.instagram.com/uzcasting?igsh=c2M2ZHVoMWI1YzVi" target="_blank" rel="noopener noreferrer">
                                <FaInstagram className={socialIconClass} />
                            </a>
                            <a href="https://t.me/Uzcastinguz" target="_blank" rel="noopener noreferrer">
                                <FaTelegramPlane className={socialIconClass} />
                            </a>
                        </div>
                    </div>

                    {/* Mobile toggle */}
                    <button
                        className="flex lg:hidden flex-col justify-around w-6 h-[22px] bg-transparent border-none text-sm cursor-pointer z-[10000]"
                        onClick={toggleMenu}
                        aria-label={isMenuOpen ? "Close menu" : "Open menu"}
                    >
                        <span className={barClass(0, isMenuOpen)}></span>
                        <span className={barClass(1, isMenuOpen)}></span>
                        <span className={barClass(2, isMenuOpen)}></span>
                    </button>
                </div>
            </header>

            {/* Mobile Nav */}
            <nav
                className={
                    "lg:hidden fixed top-0 flex flex-col w-full h-screen bg-[#111827] pt-20 px-6 pb-8 " +
                    "transition-[right] duration-500 ease-in-out z-[999] justify-start items-center" +
                    (isMenuOpen ? " right-0" : " right-[-100%]")
                }
            >
                <ul className="flex flex-col items-center gap-8 w-full list-none">
                    <li className="w-full">
                        <Link
                            to={`/bot/${userId}`}
                            className={navLinkClass(activeTab === '')}
                            onClick={closeMenu}
                        >
                            <span>{translations[language].home}</span>
                        </Link>
                    </li>
                    <li className="w-full">
                        <Link
                            to={`/data-form/${userId}`}
                            className={navLinkClass(activeTab === 'data-form')}
                            onClick={closeMenu}
                        >
                            <span>{translations[language].casting}</span>
                        </Link>
                    </li>
                    <li className="w-full">
                        <Link
                            to={`/history/${userId}`}
                            className={navLinkClass(activeTab === 'history')}
                            onClick={closeMenu}
                        >
                            <span>{translations[language].my}</span>
                        </Link>
                    </li>
                </ul>

                <div className="flex justify-center mt-8 gap-4">
                    <button
                        className={langBtnClass(language === "uz")}
                        onClick={() => changeLanguage("uz")}
                    >
                        UZ
                    </button>
                    <button
                        className={langBtnClass(language === "ru")}
                        onClick={() => changeLanguage("ru")}
                    >
                        RU
                    </button>
                </div>

                <div className="flex flex-col mt-8 gap-4 w-full">
                    <a
                        href="https://www.instagram.com/uzcasting"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-2 text-[#f3f4f6] no-underline p-2 justify-center"
                    >
                        <FaInstagram className={socialIconClass} /> Instagram
                    </a>
                    <a
                        href="https://t.me/Uzcastinguz"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-2 text-[#f3f4f6] no-underline p-2 justify-center"
                    >
                        <FaTelegramPlane className={socialIconClass} /> Telegram
                    </a>
                </div>
            </nav>
        </>
    );
}

export default Header;
