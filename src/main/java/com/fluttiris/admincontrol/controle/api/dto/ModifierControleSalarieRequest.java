package com.fluttiris.admincontrol.controle.api.dto;

import java.util.UUID;

public record ModifierControleSalarieRequest(boolean accorde, UUID actionCorrectiveId) {
}
