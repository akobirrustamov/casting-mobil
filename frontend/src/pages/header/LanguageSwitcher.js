import React from "react";
import { useTranslation } from "react-i18next";

export default function LanguageSwitcher() {
    const { i18n } = useTranslation();

    const change = (lng) => {
        i18n.changeLanguage(lng);
        localStorage.setItem("selectedLanguage", lng);
    };

    return (
        <div style={{ display: "flex", gap: 8 }}>
            <button onClick={() => change("uz")}>UZ</button>
            <button onClick={() => change("ru")}>RU</button>
            <button onClick={() => change("en")}>EN</button>
        </div>
    );
}
