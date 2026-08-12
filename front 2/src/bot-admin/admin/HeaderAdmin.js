import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import './headeradmin.css';
import logo from "../../pages/header/logo.jpg"

function Header() {
    const [isScrolled, setIsScrolled] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const [isLanguageOpen, setIsLanguageOpen] = useState(false);
    const location = useLocation();
    // Joriy til
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
    return (
        <>
            <header className={`header ${isScrolled ? 'scrolled' : ''}`}>
                <div className="header-container">
                    <div className="logo">
                        <Link to="/admin/home">
                            <img src={logo} alt="Logo" />
                            <span>Jasmaxstar</span>
                        </Link>
                    </div>

                     <nav className={`nav ${isMobileMenuOpen ? 'nav-open' : ''} pt-3`}>
                        <ul>
                            <li>
                                <Link to="/admin/home" className="nav-link">
                                    <span>🏠Bosh Sahifa</span>
                                    <div className="link-underline"></div>
                                </Link>
                            </li>
                            <li>
                                <Link to="/admin/news" className="nav-link">
                                    <span>📰Yangiliklar</span>
                                    <div className="link-underline"></div>
                                </Link>
                            </li>
                            <li>
                                <Link to="/admin/casting-users" className="nav-link">
                                    <span>🎭Kelib tushgan arizalar</span>
                                    <div className="link-underline"></div>
                                </Link>
                            </li>
                            <li>
                                <Link to="/admin/accepted" className="nav-link">
                                    <span>✔️Qabul qilingan arizalar</span>
                                    <div className="link-underline"></div>
                                </Link>
                            </li>

                        </ul>
                    </nav>
                    {/* мобильный блок */}
                    <div className="mobile-actions">
                        <button
                            className={`mobile-menu-toggle ${isMobileMenuOpen ? 'open' : ''}`}
                            onClick={toggleMobileMenu}
                            aria-label="Toggle menu"
                        >
                            <span></span><span></span><span></span>
                        </button>
                    </div>
                </div>
            </header >
             <div className="header-spacer"></div>
        </>
    );
}

export default Header;