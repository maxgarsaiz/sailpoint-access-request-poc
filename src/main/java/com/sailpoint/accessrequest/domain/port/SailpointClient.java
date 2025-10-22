package com.sailpoint.accessrequest.domain.port;

import com.sailpoint.accessrequest.domain.model.AccessRequest;

public interface SailpointClient {
    String submitAccessRequest(AccessRequest accessRequest);
    String checkRequestStatus(String sailpointRequestId);
}
