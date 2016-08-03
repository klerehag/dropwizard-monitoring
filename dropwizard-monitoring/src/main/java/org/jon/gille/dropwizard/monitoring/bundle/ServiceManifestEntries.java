package org.jon.gille.dropwizard.monitoring.bundle;

import com.jcabi.manifests.Manifests;

public final class ServiceManifestEntries {

    private static final String SERVICE_ID = "Service-Id";
    private static final String SERVICE_VERSION = "Service-Version";
    private static final String NOT_AVAILABLE = "N/A";

    private ServiceManifestEntries() {
    }

    private static String value(String name, String defaultValue) {
        if (Manifests.exists(name)) {
            return Manifests.read(name);
        } else {
            return defaultValue;
        }
    }

    public static String serviceId(String defaultId) {
        return value(SERVICE_ID, defaultId);
    }

    public static String serviceVersion() {
        return value(SERVICE_VERSION, NOT_AVAILABLE);
    }
}
