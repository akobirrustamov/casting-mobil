import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import logo from "./logo1.jpg"
import {
    headerClass,
    headerContainerClass,
    logoLinkClass,
    logoImgClass,
    navClass,
    navListClass,
    navLinkClass,
    navLinkSpanClass,
    registerBtnClass,
    languageToggleClass,
    languageChevronClass,
    languageMenuClass,
    langButtonClass,
    mobileToggleClass,
    mobileBarClass,
} from "../../shared/headerStyles";

// Public header menyusi rangi (avval header.css dagi .nav-link)
const publicNavLink = navLinkClass + " text-[#1f2937] hover:text-[#2563eb]";

function Header() {
    const [isScrolled, setIsScrolled] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const [isLanguageOpen, setIsLanguageOpen] = useState(false);
    const location = useLocation();
    const { t, i18n } = useTranslation();

    // Joriy til
    const [currentLanguage, setCurrentLanguage] = useState(i18n.language);

    useEffect(() => {
        // LocalStorage dan tilni olish
        const savedLang = localStorage.getItem("appLanguage");
        if (savedLang && savedLang !== i18n.language) {
            i18n.changeLanguage(savedLang);
            setCurrentLanguage(savedLang);
        }
    }, [i18n]);

    useEffect(() => {
        const handleScroll = () => {
            setIsScrolled(window.scrollY > 10);
        };

        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    // Sahifa o'zgarganda mobil menyu yopilsin
    useEffect(() => {
        if (isMobileMenuOpen) {
            setIsMobileMenuOpen(false);
        }
        setIsLanguageOpen(false);
    }, [location.pathname]);

    const toggleMobileMenu = () => {
        setIsMobileMenuOpen(!isMobileMenuOpen);
        setIsLanguageOpen(false);
    };

    const toggleLanguageMenu = () => {
        setIsLanguageOpen(!isLanguageOpen);
    };

    const changeLanguage = (lng) => {
        i18n.changeLanguage(lng);
        setCurrentLanguage(lng);
        localStorage.setItem("appLanguage", lng); // 👉 LocalStorage ga yozish
        setIsLanguageOpen(false);
    };

    const languageMenu = (
        <div className={languageMenuClass}>
            <button className={langButtonClass(currentLanguage === 'uz')} onClick={() => changeLanguage('uz')}>O'Z</button>
            <button className={langButtonClass(currentLanguage === 'ru')} onClick={() => changeLanguage('ru')}>RU</button>
            <button className={langButtonClass(currentLanguage === 'en')} onClick={() => changeLanguage('en')}>EN</button>
        </div>
    );

    const languageChevron = (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className={languageChevronClass}>
            <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
        </svg>
    );

    return (
        <>
            <header className={headerClass(isScrolled)}>
                <div className={headerContainerClass}>
                    <div>
                        <Link to="/" className={logoLinkClass}>
                            <img src={logo} alt="Logo" className={logoImgClass} />
                            <span className="max-md:hidden">{t('header.siteTitle')}</span>
                        </Link>
                    </div>

                    <div className="flex items-center gap-8">
                        <nav className={navClass(isMobileMenuOpen)}>
                            <ul className={navListClass}>
                                <li>
                                    <Link to="/" className={publicNavLink}>
                                        <span className={navLinkSpanClass}>{t('header.home')}</span>
                                    </Link>
                                </li>
                                <li>
                                    <Link to="/models" className={publicNavLink}>
                                        <span className={navLinkSpanClass}>{t('header.models')}</span>
                                    </Link>
                                </li>
                                <li className="md:hidden">
                                    <Link to="https://t.me/uzcastingbot" className={publicNavLink}>
                                        {t('header.register')}
                                    </Link>
                                </li>
                            </ul>
                        </nav>

                        {/* ...o'ngdagi desktop paneli */}
                        <div className="flex items-center gap-4 max-md:hidden">
                            <div className="relative hidden md:flex">
                                <button
                                    className={languageToggleClass}
                                    onClick={toggleLanguageMenu}
                                    aria-label="Change language"
                                >
                                    {currentLanguage.toUpperCase()}
                                    {languageChevron}
                                </button>

                                {isLanguageOpen && languageMenu}
                            </div>

                            <div className="flex items-center">
                                <Link to="https://t.me/uzcastingbot" className={registerBtnClass}>{t('header.register')}</Link>
                            </div>
                        </div>

                        {/* mobil blok */}
                        <div className="flex items-center gap-4">
                            <div className="relative flex md:hidden">
                                <button
                                    className={languageToggleClass}
                                    onClick={toggleLanguageMenu}
                                    aria-label="Change language"
                                >
                                    {currentLanguage.toUpperCase()}
                                    {languageChevron}
                                </button>

                                {isLanguageOpen && languageMenu}
                            </div>

                            <button
                                className={mobileToggleClass}
                                onClick={toggleMobileMenu}
                                aria-label="Toggle menu"
                            >
                                <span className={mobileBarClass(0, isMobileMenuOpen)}></span>
                                <span className={mobileBarClass(1, isMobileMenuOpen)}></span>
                                <span className={mobileBarClass(2, isMobileMenuOpen)}></span>
                            </button>
                        </div>
                    </div>

                </div>
            </header >
        </>
    );
}

export default Header;
