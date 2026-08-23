import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import logo from "../../pages/header/logo1.jpg"
import {
    headerClass,
    headerContainerClass,
    logoLinkClass,
    logoImgClass,
    mobileToggleClass,
    mobileBarClass,
    headerSpacerClass,
} from "../../shared/headerStyles";

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
                        <Link to="/aadmin/casting-users/web" className={logoLinkClass}>
                            <img src={logo} alt="Logo" className={logoImgClass} />
                            <span className="max-md:hidden">Uzcasting</span>
                        </Link>
                    </div>

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
