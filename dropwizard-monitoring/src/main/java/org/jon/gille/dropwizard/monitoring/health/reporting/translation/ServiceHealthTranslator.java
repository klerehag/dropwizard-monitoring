package org.jon.gille.dropwizard.monitoring.health.reporting.translation;

import org.jon.gille.dropwizard.monitoring.api.health.HealthCheckResultDto;
import org.jon.gille.dropwizard.monitoring.api.health.ServiceInstanceHealthDto;
import org.jon.gille.dropwizard.monitoring.api.metadata.InstanceMetadataDto;
import org.jon.gille.dropwizard.monitoring.api.metadata.MetadataDto;
import org.jon.gille.dropwizard.monitoring.api.metadata.ServiceMetadataDto;
import org.jon.gille.dropwizard.monitoring.api.reporting.ServiceHealthReportDto;
import org.jon.gille.dropwizard.monitoring.health.domain.HealthCheckResult;
import org.jon.gille.dropwizard.monitoring.health.domain.ServiceHealth;
import org.jon.gille.dropwizard.monitoring.health.translation.api.HealthCheckResultTranslator;
import org.jon.gille.dropwizard.monitoring.metadata.domain.ServiceMetadata;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ServiceHealthTranslator {

    public static ServiceHealthReportDto createReport(ServiceMetadata serviceMetadata, ServiceHealth serviceHealth) {
        MetadataDto metadataDto = translate(serviceMetadata);
        ServiceInstanceHealthDto serviceInstanceHealthDto = translate(serviceHealth);

        return new ServiceHealthReportDto(
                serviceHealth.id().toString(),
                serviceHealth.timestamp().atOffset(ZoneOffset.UTC),
                metadataDto,
                serviceInstanceHealthDto);
    }

    private static MetadataDto translate(ServiceMetadata serviceMetadata) {
        return new MetadataDto(
                new ServiceMetadataDto(serviceMetadata.serviceName().name(),
                        serviceMetadata.serviceVersion().version()),
                new InstanceMetadataDto(serviceMetadata.instanceMetadata().instanceId().id(),
                        serviceMetadata.instanceMetadata().hostAddress())
        );
    }

    private static ServiceInstanceHealthDto translate(ServiceHealth serviceHealth) {
        return new ServiceInstanceHealthDto(serviceHealth.status().name(),
                translate(serviceHealth.executedChecks().stream()));
    }

    private static List<HealthCheckResultDto> translate(Stream<HealthCheckResult> checks) {
        return checks.map(HealthCheckResultTranslator::mapToDto).collect(toList());
    }
}
