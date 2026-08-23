// Header va HeaderAdmin uchun umumiy Tailwind sinf to'plamlari.
// Avval bu uslublar src/pages/header/header.css da edi va headeradmin.css
// uni @import qilardi — endi bitta manba shu fayl.

export const headerClass = (isScrolled) =>
    "fixed top-0 left-0 w-full z-[1000] flex justify-center bg-[#111827] " +
    "border-b border-[#334155] transition-all duration-300 animate-slideDown" +
    (isScrolled
        ? " shadow-[0_4px_6px_-1px_rgba(0,0,0,0.1),0_2px_4px_-1px_rgba(0,0,0,0.06)] backdrop-blur-[10px] animate-headerScroll"
        : "");

export const headerContainerClass =
    "max-w-[1200px] w-full px-6 max-md:px-4 flex justify-between items-center h-20";

export const logoLinkClass =
    "flex items-center no-underline text-[#3b82f6] font-bold text-2xl relative";

export const logoImgClass =
    "h-10 w-10 rounded-full object-cover mr-3 border-2 border-[#2563eb] shadow-[0_0_10px_rgba(37,99,235,0.3)]";

export const navClass = (isOpen) =>
    "pt-3 max-md:fixed max-md:top-0 max-md:w-full max-md:h-screen max-md:bg-[#111827] " +
    "max-md:shadow-[-5px_0_15px_rgba(0,0,0,0.1)] max-md:transition-[right] max-md:duration-500 " +
    "max-md:ease-[cubic-bezier(0.65,0,0.35,1)] max-md:pt-20 max-md:px-6 max-md:pb-8 " +
    "max-md:z-[999] max-md:flex max-md:flex-col max-md:items-center" +
    (isOpen ? " max-md:right-0 max-md:animate-menuSlideIn" : " max-md:right-[-100%]");

export const navListClass =
    "flex items-center gap-8 list-none max-md:flex-col max-md:w-full";

// Rangsiz asos — rang chaqiruvchi tomonidan qo'shiladi
// (public header: #1f2937, admin header: #f3f4f6).
export const navLinkClass =
    "relative no-underline font-medium transition-all duration-300 flex flex-col items-center py-2 overflow-hidden " +
    "after:content-[''] after:absolute after:bottom-0 after:left-0 after:w-full after:h-0.5 " +
    "after:bg-gradient-to-r after:from-[#2563eb] after:to-[#1d4ed8] after:scale-x-0 after:origin-right " +
    "after:transition-transform after:duration-[400ms] after:ease-[cubic-bezier(0.65,0,0.35,1)] " +
    "hover:after:scale-x-100 hover:after:origin-left " +
    "max-md:text-lg max-md:py-4 max-md:w-full max-md:text-center";

export const navLinkSpanClass = "relative z-[2] transition-all duration-300";

export const registerBtnClass =
    "relative z-[1] inline-block overflow-hidden cursor-pointer border-none rounded-lg " +
    "px-[1.4rem] py-[0.7rem] text-[1.05rem] font-semibold text-white no-underline " +
    "bg-gradient-to-br from-[#2563eb] to-[#3b82f6] shadow-[0_4px_15px_rgba(37,99,235,0.5)] " +
    "[text-shadow:0_1px_2px_rgba(0,0,0,0.2)] animate-registerBtn " +
    "transition-all duration-[400ms] ease-[cubic-bezier(0.68,-0.55,0.27,1.55)] " +
    "before:content-[''] before:absolute before:top-0 before:left-[-100%] before:w-full before:h-full before:-z-[1] " +
    "before:bg-[linear-gradient(90deg,transparent,rgba(255,255,255,0.4),transparent)] " +
    "before:transition-[left] before:duration-700 before:animate-shineEffect " +
    "hover:bg-gradient-to-br hover:from-[#1d4ed8] hover:to-[#2563eb] hover:-translate-y-[3px] hover:scale-105 " +
    "hover:shadow-[0_12px_25px_rgba(37,99,235,0.6),0_8px_10px_rgba(37,99,235,0.4)] hover:animate-none " +
    "hover:before:left-full hover:before:animate-none " +
    "active:-translate-y-[1px] active:scale-[1.02] active:shadow-[0_6px_15px_rgba(37,99,235,0.5)]";

export const languageToggleClass =
    "group flex items-center gap-2 bg-transparent border-2 border-[#e5e7eb] rounded-md px-3 py-2 " +
    "font-semibold text-white cursor-pointer transition-all duration-300 " +
    "hover:border-[#2563eb] hover:text-[#2563eb]";

export const languageChevronClass =
    "w-[14px] h-[14px] transition-transform duration-300 group-hover:rotate-180";

export const languageMenuClass =
    "absolute top-full right-0 bg-black rounded-lg shadow-[0_4px_15px_rgba(0,0,0,0.1)] " +
    "mt-[0.2rem] min-w-[80px] z-[1001] animate-fadeIn";

export const langButtonClass = (isActive) =>
    "block w-[90%] bg-none border-none px-[0.2rem] py-[0.1rem] text-center cursor-pointer " +
    "rounded transition-all duration-200 font-normal text-white hover:bg-[#f3f4f6] hover:text-[#2563eb]" +
    (isActive ? " bg-[#2563eb]" : "");

export const mobileToggleClass =
    "hidden max-md:flex flex-col justify-around w-[30px] h-[30px] bg-transparent border-none cursor-pointer p-0 z-[10000] relative";

// Hamburger chiziqlari: 1-, 2- va 3-chiziq uchun ochiq holatdagi transformlar
const barBase =
    "w-[30px] h-[3px] rounded-[10px] origin-center relative transition-all duration-[400ms] ease-[cubic-bezier(0.68,-0.55,0.27,1.55)] ";

export const mobileBarClass = (index, isOpen) => {
    if (!isOpen) return barBase + "bg-[#f3f4f6]";
    if (index === 0) return barBase + "bg-[#2563eb] rotate-45 translate-x-2 translate-y-2";
    if (index === 1) return barBase + "bg-[#2563eb] opacity-0 -translate-x-5";
    return barBase + "bg-[#2563eb] -rotate-45 translate-x-[7px] -translate-y-[7px]";
};

export const headerSpacerClass = "h-20";
