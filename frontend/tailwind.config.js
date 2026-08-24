/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      screens: {
        // CastingUserAccepted.css dagi @media (min-width: 1440px)
        wide: "1440px",
      },
      fontFamily: {
        lora: ["Lora", "serif"],
      },
      backgroundImage: {
        // DataForm.css dagi <select> uchun pastga qaragan strelka
        "select-arrow":
          "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='%23e9ecef' viewBox='0 0 16 16'%3E%3Cpath d='M7.247 11.14 2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z'/%3E%3C/svg%3E\")",
      },
      keyframes: {
        bbl: {
          "0%, 80%, 100%": { opacity: ".6", transform: "scale(.6)" },
          "40%": { opacity: "1", transform: "scale(1)" },
        },
        bounceY: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-12px)" },
        },
        cardEnter: {
          "0%": { opacity: "0", transform: "translateY(20px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        colorShift: {
          "0%": {
            background: "linear-gradient(135deg,#2563eb,#3b82f6)",
            boxShadow: "0 4px 15px #2563eb80",
          },
          "25%": {
            background: "linear-gradient(135deg,#1d4ed8,#3b82f6)",
            boxShadow: "0 4px 15px #1d4ed880",
          },
          "50%": {
            background: "linear-gradient(135deg,#4338ca,#4f46e5)",
            boxShadow: "0 4px 15px #4338ca80",
          },
          "75%": {
            background: "linear-gradient(135deg,#3730a3,#4338ca)",
            boxShadow: "0 4px 15px #3730a380",
          },
          "100%": {
            background: "linear-gradient(135deg,#312e81,#3730a3)",
            boxShadow: "0 4px 15px #312e8180",
          },
        },
        colorShiftDark: {
          "0%": {
            background: "linear-gradient(135deg,#3b82f6,#60a5fa)",
            boxShadow: "0 4px 15px #3b82f699",
          },
          "25%": {
            background: "linear-gradient(135deg,#2563eb,#3b82f6)",
            boxShadow: "0 4px 15px #2563eb99",
          },
          "50%": {
            background: "linear-gradient(135deg,#4f46e5,#6366f1)",
            boxShadow: "0 4px 15px #4f46e599",
          },
          "75%": {
            background: "linear-gradient(135deg,#4338ca,#4f46e5)",
            boxShadow: "0 4px 15px #4338ca99",
          },
          "100%": {
            background: "linear-gradient(135deg,#3730a3,#4338ca)",
            boxShadow: "0 4px 15px #3730a399",
          },
        },
        fadeIn: {
          "0%": { opacity: "0", transform: "translateY(20px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        fadeInOpacity: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        formFadeIn: {
          "0%": { opacity: "0", transform: "translateY(10px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        fadeInUp: {
          "0%": { opacity: "0", transform: "translateY(20px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        floatY: {
          "0%, 100%": { transform: "translateY(0) rotate(0deg)" },
          "50%": { transform: "translateY(-18px) rotate(8deg)" },
        },
        headerScroll: {
          "0%": { opacity: ".8", transform: "translateY(-10px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        menuSlideIn: {
          "0%": { opacity: "0", transform: "translateX(50px)" },
          "100%": { opacity: "1", transform: "translateX(0)" },
        },
        modalScale: {
          "0%": { opacity: "0", transform: "scale(.9)" },
          "100%": { opacity: "1", transform: "scale(1)" },
        },
        modalIn: {
          "0%": { opacity: ".9", transform: "translateY(4px) scale(.98)" },
          "100%": { opacity: "1", transform: "translateY(0) scale(1)" },
        },
        pingOut: {
          "75%, 100%": { opacity: "0", transform: "scale(1.8)" },
        },
        glowPulse: {
          "0%": { boxShadow: "0 0 10px #3b82f666" },
          "100%": { boxShadow: "0 0 20px #3b82f6cc" },
        },
        pulseRing2: {
          "0%": { boxShadow: "0 0 0 0 rgba(37,99,235,0.7)" },
          "70%": { boxShadow: "0 0 0 15px rgba(37,99,235,0)" },
          "100%": { boxShadow: "0 0 0 0 rgba(37,99,235,0)" },
        },
        pulseRing: {
          "0%": { boxShadow: "0 0 0 0 #3b82f6b3" },
          "70%": { boxShadow: "0 0 0 15px #3b82f600" },
          "100%": { boxShadow: "0 0 0 0 #3b82f600" },
        },
        shake: {
          "0%, 100%": { transform: "translateX(0)" },
          "20%, 60%": { transform: "translateX(-5px)" },
          "40%, 80%": { transform: "translateX(5px)" },
        },
        shineEffect: {
          "0%": { left: "-100%" },
          "20%": { left: "100%" },
          "100%": { left: "100%" },
        },
        slideDown: {
          "0%": { opacity: "0", transform: "translateY(-100%)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        slideIn: {
          "0%": { opacity: "0", transform: "translateY(30px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        textRotate: {
          "100%": { transform: "rotate(1turn)" },
        },
        rotate360: {
          "100%": { transform: "rotate(360deg)" },
        },
      },
      animation: {
        bbl: "bbl 1.4s ease-in-out infinite",
        bounceY: "bounceY 2s ease-in-out infinite",
        cardEnter: "cardEnter .5s ease forwards",
        colorShift: "colorShift 8s ease-in-out infinite alternate",
        colorShiftDark: "colorShiftDark 8s ease-in-out infinite alternate",
        fadeIn: "fadeIn .6s ease forwards",
        fadeInUp: "fadeInUp .6s ease forwards",
        fadeInOpacity: "fadeInOpacity .6s ease-out",
        formFadeIn: "formFadeIn .4s ease forwards",
        floatY: "floatY 6s ease-in-out infinite",
        headerScroll: "headerScroll .4s ease forwards",
        menuSlideIn: "menuSlideIn .3s ease forwards",
        modalIn: "modalIn .2s ease-out forwards",
        modalScale: "modalScale .3s ease",
        pingOut: "pingOut 1.5s cubic-bezier(0,0,.2,1) infinite",
        glowPulse: "glowPulse 2s ease-in-out infinite alternate",
        spinGlow: "rotate360 1s linear infinite, glowPulse 2s ease-in-out infinite alternate",
        pulseRing: "pulseRing 2s infinite",
        pulseRing2: "pulseRing2 2s infinite",
        registerBtn: "pulseRing 2.5s infinite, colorShift 8s infinite alternate",
        shake: "shake .4s ease-in-out",
        shineEffect: "shineEffect 3s ease-in-out infinite",
        slideDown: "slideDown .4s ease forwards",
        slideIn: "slideIn .5s ease forwards",
        textRotate: "textRotate 20s linear infinite",
      },
    },
  },
  plugins: [],
}
