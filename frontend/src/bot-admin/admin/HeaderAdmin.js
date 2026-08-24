import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import logo from "../../pages/header/logo.jpg"
import {
    headerClass,
    headerContainerClass,
    logoLinkClass,
    logoImgClass,
    navClass,
    navListClass,
    navLinkClass,
    navLinkSpanClass,
    mobileToggleClass,
    mobileBarClass,
    headerSpacerClass,
} from "../../shared/headerStyles";

// Admin sahifalarining foni qora bo'lgani uchun menyu rangi ochiq qilinadi
const adminNavLink = navLinkClass + " text-[#f3f4f6] hover:text-[#93c5fd]";

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
            <header className={headerClass(isScrolled)}>
                <div className={headerContainerClass}>
                    <div>
                        <Link to="/admin/home" className={logoLinkClass}>
                            <img src={logo} alt="Logo" className={logoImgClass} />
                            <span className="max-md:hidden">Jasmaxstar</span>
                        </Link>
                    </div>

                    <nav className={navClass(isMobileMenuOpen)}>
                        <ul className={navListClass}>
                            <li>
                                <Link to="/admin/home" className={adminNavLink}>
                                    <span className={navLinkSpanClass}>🏠Bosh Sahifa</span>
                                </Link>
                            </li>
                            <li>
                                <Link to="/admin/news" className={adminNavLink}>
                                    <span className={navLinkSpanClass}>📰Yangiliklar</span>
                                </Link>
                            </li>
                            <li>
                                <Link to="/admin/casting-users" className={adminNavLink}>
                                    <span className={navLinkSpanClass}>🎭Kelib tushgan arizalar</span>
                                </Link>
                            </li>
                            <li>
                                <Link to="/admin/accepted" className={adminNavLink}>
                                    <span className={navLinkSpanClass}>✔️Qabul qilingan arizalar</span>
                                </Link>
                            </li>

                        </ul>
                    </nav>
                    {/* mobil blok */}
                    <div className="flex items-center gap-4">
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
            </header >
            <div className={headerSpacerClass}></div>
        </>
    );
}

export default Header;
