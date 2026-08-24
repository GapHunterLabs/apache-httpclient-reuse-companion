package com.acmecorp.payments;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

/**
 * Demo data for Apache HttpClient Reuse Companion — used with
 * `./gradlew runIde` to capture the real Marketplace screenshot. Open
 * this file, the warning icon should appear on the call inside
 * `charge`.
 */
public class PaymentGateway {

    private final CloseableHttpClient sharedClient;

    public PaymentGateway() {
        // Built once, in the constructor -- NOT flagged.
        this.sharedClient = HttpClients.createDefault();
    }

    public Object charge(String payload) {
        // Built here on every call -- a fresh connection pool each
        // time. FLAGGED.
        CloseableHttpClient client = HttpClients.createDefault();
        return client.execute(request);
    }
}
