package com.example.backend.Cms.Enums;

/**
 * Ichki virtual valyuta turi (ТЗ §39–41).
 */
public enum CurrencyKind {

    /** UZCASTING Stars — Telegramdagi kabi yulduzlar. */
    STARS,

    /** UZCASTING Coin — ichki tanga. */
    /**
     * ТЗ §39 dagi nom bilan bir xil.
     *
     * Ilgari shunchaki {@code COIN} edi. Farq kichik ko'rinadi, lekin u
     * doimiy tarjima qatlamini yaratardi: ТЗ va hisobotlarda bir nom,
     * kodda va API javobida boshqa nom.
     */
    UZCASTING_COIN
}
