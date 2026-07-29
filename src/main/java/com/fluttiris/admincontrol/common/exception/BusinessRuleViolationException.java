package com.fluttiris.admincontrol.common.exception;

/**
 * Levée quand une opération est structurellement valide (données bien formées)
 * mais viole une règle métier (ex : rôle STT2 sans parent STT1, entreprise
 * doublement affectée à un même chantier...).
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
