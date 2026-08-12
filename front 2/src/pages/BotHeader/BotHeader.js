import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import logo from "./logo.jpg";
import './BotHeader.css'; // ⚡ Header.css ishlatiladi
import { FaInstagram, FaTelegramPlane } from 'react-icons/fa';

function Header({ activeTab }) {
    const { userId } = useParams();
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [scrolled, setScrolled] = useState(false);
    const [language, setLanguage] = useState('uz');
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
            <header className={`header ${scrolled ? 'scrolled' : ''}`}>
                <div className="header-container">
                    <div className="logo">
                        <Link to={`/bot/${userId}`} onClick={closeMenu}>
                            <img src={logo} alt="Logo" />
                            <span>UzCasting</span>
                        </Link>
                    </div>

                    <div className="header-right only-desktop">
                        <div className="language-selector">
                            <select
                                value={language}
                                onChange={(e) => changeLanguage(e.target.value)}
                                className="language-toggle"
                            >
                                <option value="uz">{translations[language].uzbek}</option>
                                <option value="ru">{translations[language].russian}</option>
                            </select>
                        </div>
                        <div className="social-icons">
                            <a href="https://www.instagram.com/uzcasting?igsh=c2M2ZHVoMWI1YzVi" target="_blank" rel="noopener noreferrer">
                                <FaInstagram className="social-icon" />
                            </a>
                            <a href="https://t.me/Uzcastinguz" target="_blank" rel="noopener noreferrer">
                                <FaTelegramPlane className="social-icon" />
                            </a>
                        </div>
                    </div>

                    {/* Mobile toggle */}
                    <button
                        className={`mobile-menu-toggle only-mobile ${isMenuOpen ? 'open' : ''}`}
                        onClick={toggleMenu}
                        aria-label={isMenuOpen ? "Close menu" : "Open menu"}
                    >
                        <span></span>
                        <span></span>
                        <span></span>
                    </button>
                </div>
            </header>

            {/* Mobile Nav */}
            <nav className={`nav ${isMenuOpen ? 'nav-open' : ''}`}>
                <ul>
                    <li>
                        <Link
                            to={`/bot/${userId}`}
                            className={`nav-link ${activeTab === '' ? 'active' : ''}`}
                            onClick={closeMenu}
                        >
                            <span>{translations[language].home}</span>
                        </Link>
                    </li>
                    <li>
                        <Link
                            to={`/data-form/${userId}`}
                            className={`nav-link ${activeTab === 'data-form' ? 'active' : ''}`}
                            onClick={closeMenu}
                        >
                            <span>{translations[language].casting}</span>
                        </Link>
                    </li>
                    <li>
                        <Link
                            to={`/history/${userId}`}
                            className={`nav-link ${activeTab === 'history' ? 'active' : ''}`}
                            onClick={closeMenu}
                        >
                            <span>{translations[language].my}</span>
                        </Link>
                    </li>
                </ul>

                <div className="mobile-language-selector">
                    <button
                        className={language === "uz" ? "active" : ""}
                        onClick={() => changeLanguage("uz")}
                    >
                        UZ
                    </button>
                    <button
                        className={language === "ru" ? "active" : ""}
                        onClick={() => changeLanguage("ru")}
                    >
                        RU
                    </button>
                </div>

                <div className="social-icons-mobile">
                    <a href="https://www.instagram.com/uzcasting" target="_blank" rel="noopener noreferrer">
                        <FaInstagram className="social-icon" /> Instagram
                    </a>
                    <a href="https://t.me/Uzcastinguz" target="_blank" rel="noopener noreferrer">
                        <FaTelegramPlane className="social-icon" /> Telegram
                    </a>
                </div>
            </nav>
        </>
    );
}

export default Header;
