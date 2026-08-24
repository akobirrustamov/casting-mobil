import React from "react";
import { useTranslation } from "react-i18next";
import Logo from "./logo.jpg";

const Footer = () => {
    const { t } = useTranslation();

    return (
        <footer className="w-full text-white pt-16 px-5 pb-7 relative overflow-hidden border-t border-white/[0.07] bg-[radial-gradient(800px_400px_at_15%_-10%,rgba(16,185,129,0.12),transparent_40%),radial-gradient(800px_400px_at_85%_120%,rgba(245,158,11,0.1),transparent_40%),radial-gradient(600px_600px_at_50%_100%,rgba(139,92,246,0.08),transparent_50%),linear-gradient(180deg,#0a0a0b_0%,#0e1015_60%,#0a0a0b_100%)] before:content-[''] before:absolute before:top-0 before:left-0 before:right-0 before:h-px before:bg-[linear-gradient(90deg,transparent,rgba(255,255,255,0.38),transparent)]">
            <div className="max-w-[1200px] mx-auto relative z-[1]">
                <div className="grid grid-cols-[repeat(auto-fit,minmax(260px,1fr))] gap-7 mt-7 mb-9">
                    {/* Brand Section */}
                    <div className="py-1.5 min-[900px]:[&:not(:first-child)]:border-l min-[900px]:[&:not(:first-child)]:border-white/10 min-[900px]:[&:not(:first-child)]:pl-6">
                        <div className="flex items-center gap-4 mb-6 max-[560px]:flex-col max-[560px]:items-start">
                            <div className="w-[60px] h-[60px] rounded-[14px] overflow-hidden flex-shrink-0 bg-[linear-gradient(135deg,#10b981,#f59e0b)] shadow-[0_8px_24px_rgba(16,185,129,0.28)]">
                                <img src={Logo} alt="Logo" className="w-full h-full object-cover block" />
                            </div>
                            <div className="text-[2rem] font-extrabold bg-[linear-gradient(90deg,#e5fff6,#fef3c7)] bg-clip-text text-transparent max-[560px]:text-[1.6rem]">UzCasting</div>
                        </div>
                        <p className="text-[#cfd6e3] mt-1.5 mb-3.5 mx-0 leading-[1.65]">
                            {t("footer.brand.description")}
                        </p>
                        <div className="flex items-center gap-2 text-[#e5fff6] font-semibold">
                            <div className="relative w-3 h-3">
                                <span className="absolute inset-0 rounded-full bg-[#10b981] animate-pingOut [animation-duration:1.6s]"></span>
                                <span className="absolute inset-0 rounded-full bg-[#059669]"></span>
                            </div>
                            <span>{t("footer.brand.online")}</span>
                        </div>
                    </div>

                    {/* Contact Information */}
                    <div className="py-1.5 min-[900px]:[&:not(:first-child)]:border-l min-[900px]:[&:not(:first-child)]:border-white/10 min-[900px]:[&:not(:first-child)]:pl-6">
                        <h3 className="text-[1.2rem] font-extrabold mt-0 mb-3.5 mx-0 inline-flex items-center gap-2.5">
                            {t("footer.contact.title")}
                            <span className="w-11 h-[3px] rounded-sm bg-[linear-gradient(90deg,#10b981,#f59e0b)] shadow-[0_0_10px_rgba(16,185,129,0.35)]"></span>
                        </h3>
                        <div className="flex items-start gap-3 mt-0 mb-3.5 mx-0 text-[#e9f5ee]">
                            <div className="w-[26px] h-[26px] text-[#10b981] flex-shrink-0">
                                <LocationIcon />
                            </div>
                            <span>{t("footer.contact.address")}</span>
                        </div>
                        <div className="flex items-start gap-3 mt-0 mb-3.5 mx-0 text-[#e9f5ee]">
                            <div className="w-[26px] h-[26px] text-[#10b981] flex-shrink-0">
                                <PhoneIcon />
                            </div>
                            <a href="tel:+998916407314" className="text-[#e9f5ee] no-underline transition-[color,transform] duration-200 hover:text-[#fef3c7] hover:translate-x-0.5">
                                {t("footer.contact.phone")}
                            </a>
                        </div>
                        <div className="flex items-start gap-3 mt-0 mb-3.5 mx-0 text-[#e9f5ee]">
                            <div className="w-[26px] h-[26px] text-[#10b981] flex-shrink-0">
                                <EmailIcon />
                            </div>
                            <a href="mailto:uzcasting.org@gmail.com" className="text-[#e9f5ee] no-underline transition-[color,transform] duration-200 hover:text-[#fef3c7] hover:translate-x-0.5">
                                {t("footer.contact.email")}
                            </a>
                        </div>
                    </div>

                    {/* Social Media Links */}
                    <div className="py-1.5 min-[900px]:[&:not(:first-child)]:border-l min-[900px]:[&:not(:first-child)]:border-white/10 min-[900px]:[&:not(:first-child)]:pl-6">
                        <h3 className="text-[1.2rem] font-extrabold mt-0 mb-3.5 mx-0 inline-flex items-center gap-2.5">
                            {t("footer.social.title")}
                            <span className="w-11 h-[3px] rounded-sm bg-[linear-gradient(90deg,#10b981,#f59e0b)] shadow-[0_0_10px_rgba(16,185,129,0.35)]"></span>
                        </h3>
                        <a href="https://t.me/Uzcastinguz" target="_blank" rel="noopener noreferrer" className="flex items-center gap-3 text-[#e9f5ee] no-underline mt-0 mb-3 mx-0 py-1.5 transition-[color,transform] duration-200 hover:text-[blue] hover:-translate-y-px">
                            <div className="w-9 h-9 rounded-full flex items-center justify-center border border-white/[0.14] bg-[radial-gradient(circle_at_30%_30%,rgba(255,255,255,0.1),rgba(255,255,255,0.04))]">
                                <TelegramIcon />
                            </div>
                            <span>{t("footer.social.telegram")}</span>
                        </a>
                        <a href="https://www.instagram.com/uzcasting" target="_blank" rel="noopener noreferrer" className="flex items-center gap-3 text-[#e9f5ee] no-underline mt-0 mb-3 mx-0 py-1.5 transition-[color,transform] duration-200 hover:text-[blue] hover:-translate-y-px">
                            <div className="w-9 h-9 rounded-full flex items-center justify-center border border-white/[0.14] bg-[radial-gradient(circle_at_30%_30%,rgba(255,255,255,0.1),rgba(255,255,255,0.04))]">
                                <InstagramIcon />
                            </div>
                            <span>{t("footer.social.instagram")}</span>
                        </a>
                        <a href="http://www.youtube.com/@Jasmaxstar" target="_blank" rel="noopener noreferrer" className="flex items-center gap-3 text-[#e9f5ee] no-underline mt-0 mb-3 mx-0 py-1.5 transition-[color,transform] duration-200 hover:text-[blue] hover:-translate-y-px">
                            <div className="w-9 h-9 rounded-full flex items-center justify-center border border-white/[0.14] bg-[radial-gradient(circle_at_30%_30%,rgba(255,255,255,0.1),rgba(255,255,255,0.04))]">
                                <YouTubeIcon />
                            </div>
                            <span>{t("footer.social.youtube")}</span>
                        </a>
                        <a href="https://www.tiktok.com/@jasmaxstar" target="_blank" rel="noopener noreferrer" className="flex items-center gap-3 text-[#e9f5ee] no-underline mt-0 mb-3 mx-0 py-1.5 transition-[color,transform] duration-200 hover:text-[blue] hover:-translate-y-px">
                            <div className="w-9 h-9 rounded-full flex items-center justify-center border border-white/[0.14] bg-[radial-gradient(circle_at_30%_30%,rgba(255,255,255,0.1),rgba(255,255,255,0.04))]">
                                <TikTokIcon />
                            </div>
                            <span>{t("footer.social.tiktok")}</span>
                        </a>
                    </div>

                    {/* Quick Actions */}
                    <div className="py-1.5 min-[900px]:[&:not(:first-child)]:border-l min-[900px]:[&:not(:first-child)]:border-white/10 min-[900px]:[&:not(:first-child)]:pl-6">
                        <h3 className="text-[1.2rem] font-extrabold mt-0 mb-3.5 mx-0 inline-flex items-center gap-2.5">
                            {t("footer.quick.title")}
                            <span className="w-11 h-[3px] rounded-sm bg-[linear-gradient(90deg,#10b981,#f59e0b)] shadow-[0_0_10px_rgba(16,185,129,0.35)]"></span>
                        </h3>
                        <a href="https://t.me/JasMaxStar" target="_blank" rel="noopener noreferrer" className="flex items-center mb-2.5 gap-3 px-3.5 py-3 rounded-full no-underline text-white transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_8px_20px_rgba(0,0,0,0.35)] border border-[rgba(16,185,129,0.35)] bg-[linear-gradient(90deg,rgba(16,185,129,0.18),rgba(17,24,39,0.35))] hover:border-[rgba(16,67,185,0.55)] hover:bg-[linear-gradient(90deg,rgba(16,185,129,0.25),rgba(17,24,39,0.5))]">
                            <div className="w-9 h-9 rounded-full flex items-center justify-center">
                                <LinkIcon />
                            </div>
                            <div>
                                <div className="font-extrabold">{t("footer.quick.admin")}</div>
                                <div className="text-[#cfd6e3] text-[0.9rem]">{t("footer.quick.adminUser")}</div>
                            </div>
                        </a>
                        <a href="https://t.me/uzcastingbot" target="_blank" rel="noopener noreferrer" className="flex items-center mb-2.5 gap-3 px-3.5 py-3 rounded-full no-underline text-white transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_8px_20px_rgba(0,0,0,0.35)] border border-[rgba(245,158,11,0.35)] bg-[linear-gradient(90deg,rgba(245,158,11,0.18),rgba(17,24,39,0.35))] hover:border-[rgba(245,158,11,0.55)] hover:bg-[linear-gradient(90deg,rgba(245,158,11,0.25),rgba(17,24,39,0.5))]">
                            <div className="w-9 h-9 rounded-full flex items-center justify-center">
                                <BotIcon />
                            </div>
                            <div>
                                <div className="font-extrabold text-[#fde68a]">{t("footer.quick.register")}</div>
                                <div className="text-[#cfd6e3] text-[0.9rem]">{t("footer.quick.registerSub")}</div>
                            </div>
                        </a>
                    </div>
                </div>

                {/* Copyright Section */}
                <div className="text-[#cfd6e3] text-[0.95rem] mt-0 mb-4 mx-0">
                    <p>© {new Date().getFullYear()} UzCasting. {t("footer.copyright.text")}</p>
                    <div className="flex items-center justify-center gap-3.5 flex-wrap">
                        <span className="text-[#cfd6e3] font-bold">{t("footer.copyright.partners")}</span>
                        <div className="flex gap-3 flex-wrap justify-center">
                            <div className="px-3 py-1.5 rounded-full bg-white/[0.08] border border-white/[0.12] text-[#e5fff6] text-[0.85rem] transition-all duration-200 hover:bg-white/[0.14] hover:-translate-y-0.5">JasMax</div>
                            <div className="px-3 py-1.5 rounded-full bg-white/[0.08] border border-white/[0.12] text-[#e5fff6] text-[0.85rem] transition-all duration-200 hover:bg-white/[0.14] hover:-translate-y-0.5">UzContent</div>
                            <div className="px-3 py-1.5 rounded-full bg-white/[0.08] border border-white/[0.12] text-[#e5fff6] text-[0.85rem] transition-all duration-200 hover:bg-white/[0.14] hover:-translate-y-0.5">FilmUz</div>
                        </div>
                    </div>
                </div>
            </div>
        </footer>
    );
};

// SVG ikonalar o‘zgarishsiz qoladi
const LocationIcon = () => (<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 0 1 0-5 2.5 2.5 0 0 1 0 5z" /></svg>);
const PhoneIcon = () => (<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z" /></svg>);
const EmailIcon = () => (<svg viewBox="0 0 24 24" fill="currentColor"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z" /></svg>);
const TelegramIcon = () => (<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M9.032 15.57 8.89 19.3c.318 0 .457-.136.622-.298l2.985-2.85 6.187 4.54c1.135.627 1.946.298 2.254-1.053l4.084-19.14-.001-.001c.363-1.69-.61-2.352-1.722-1.939L1.23 9.01c-1.657.643-1.633 1.565-.282 1.983l6.003 1.873L20.26 5.73c.595-.393 1.134-.175.69.218L9.032 15.57z" /></svg>);
const InstagramIcon = () => (<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M7 2h10a5 5 0 0 1 5 5v10a5 5 0 0 1-5 5H7a5 5 0 0 1-5-5V7a5 5 0 0 1 5-5Zm0 2a3 3 0 0 0-3 3v10a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3V7a3 3 0 0 0-3-3H7Zm5 3.5a5.5 5.5 0 1 1 0 11 5.5 5.5 0 0 1 0-11Zm0 2a3.5 3.5 0 1 0 0 7 3.5 3.5 0 1 0 0-7Zm6.25-.75a1.25 1.25 0 1 1-2.5 0 1.25 1.25 0 0 1 2.5 0Z" /></svg>);
const YouTubeIcon = () => (<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M23.5 6.2a3.02 3.02 0 0 0-2.12-2.13C19.44 3.5 12 3.5 12 3.5s-7.44 0-9.38.57A3.02 3.02 0 0 0 .5 6.2 31.2 31.2 0 0 0 0 12c0 1.98.2 3.94.5 5.8a3.02 3.02 0 0 0 2.12 2.13C4.56 20.5 12 20.5 12 20.5s7.44 0 9.38-.57A3.02 3.02 0 0 0 23.5 17.8c.33-1.86.5-3.82.5-5.8 0-1.98-.17-3.94-.5-5.8ZM9.75 15.5V8.5l6.5 3.5-6.5 3.5Z" /></svg>);
const TikTokIcon = () => (<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M21.5 8.1a8.3 8.3 0 0 1-5.3-2v7.46a6.56 6.56 0 1 1-6.56-6.56c.34 0 .67.03 1 .08v3.04a3.53 3.53 0 1 0 2.53 3.39V2.5h2.6a5.64 5.64 0 0 0 5.23 3.62v2Z" /></svg>);
const LinkIcon = () => (<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M10.59 13.41a1 1 0 0 0 1.41 1.41l4.24-4.24a3 3 0 1 0-4.24-4.24L9.66 5.68a1 1 0 1 0 1.41 1.41l2.34-2.34a1 1 0 1 1 1.41 1.41l-4.24 4.24ZM13.41 10.59a1 1 0 0 0-1.41-1.41L7.76 13.41a3 3 0 1 0 4.24 4.24l2.34-2.34a1 1 0 0 0-1.41-1.41l-2.34 2.34a1 1 0 1 1-1.41-1.41l4.24-4.24Z" /></svg>);
const BotIcon = () => (<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13.5h2v7h-2v-7zm4 3.5h2v4h-2v-4zm-8 0h2v4H7v-4z" /></svg>);

export default Footer;
