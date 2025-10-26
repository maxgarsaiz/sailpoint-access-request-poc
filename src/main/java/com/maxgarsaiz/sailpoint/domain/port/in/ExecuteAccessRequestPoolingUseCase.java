package com.maxgarsaiz.sailpoint.domain.port.in;

import java.util.UUID;

/**
 * Puerto de entrada - Caso de uso para ejecutar pooling
 */
public interface ExecuteAccessRequestPoolingUseCase {
    void executePooling(UUID accessRequestId);
}