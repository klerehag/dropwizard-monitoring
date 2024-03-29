package org.jon.gille.dropwizard.monitoring.health.domain

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheckRegistry
import org.jon.gille.dropwizard.monitoring.health.HealthCheckSettingsExtractor
import spock.lang.Specification

import java.util.concurrent.ExecutorService

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class DelegatingHealthCheckServiceSpec extends Specification {

    def "Running a health check delegates to the Dropwizard registry and gets the correct settings"() {
        given:

        def name = "some_check"
        def message = "It worked"

        def healthCheckRegistry = mock(HealthCheckRegistry.class)

        when(healthCheckRegistry.runHealthCheck(name)).thenReturn(HealthCheck.Result.healthy(message))

        def service = new DelegatingHealthCheckService(healthCheckRegistry)

        def check = mock(HealthCheck.class)

        def settings = HealthCheckSettings.withLevel(Level.WARNING).withType("EXTERNAL_DEPENDENCY").build()
        service.registerHealthCheck(name, check, settings)

        when:
        def result = service.runHealthCheck(name)

        then:
        result.name() == name

        and:
        result.healthy

        and:
        !result.unhealthy

        and:
        result.message() == Optional.of(message)

        and:
        !result.error().isPresent()

        and:
        result.status() == Status.HEALTHY

        and:
        result.type() == settings.type()

        and:
        result.dependentOn() == settings.dependentOn()

        and:
        result.description() == settings.description()

        and:
        result.link() == settings.link()

    }

    def "Running a health check delegates to the Dropwizard registry and falls back to default settings"() {
        given:

        def name = "some_check"
        def message = "It worked"

        def healthCheckRegistry = mock(HealthCheckRegistry.class)
        when(healthCheckRegistry.runHealthCheck(name)).thenReturn(HealthCheck.Result.healthy(message))

        def service = new DelegatingHealthCheckService(healthCheckRegistry)

        def defaultSettings = HealthCheckSettings.DEFAULT_SETTINGS

        when:
        def result = service.runHealthCheck(name)

        then:
        result.name() == name

        and:
        result.healthy

        and:
        !result.unhealthy

        and:
        result.message() == Optional.of(message)

        and:
        !result.error().isPresent()

        and:
        result.status() == Status.HEALTHY

        and:
        result.type() == defaultSettings.type()

        and:
        result.dependentOn() == defaultSettings.dependentOn()

        and:
        result.description() == defaultSettings.description()

        and:
        result.link() == defaultSettings.link()

    }

    def "Running a health check that is unhealthy results in the correct status"() {
        given:

        def name = "some_check"
        def message = "It didn't work"

        def healthCheckRegistry = mock(HealthCheckRegistry.class)
        when(healthCheckRegistry.runHealthCheck(name)).thenReturn(HealthCheck.Result.unhealthy(message))

        def settingsRegistry = new HealthCheckSettingsRegistry()
        def settings = HealthCheckSettings.withLevel(Level.CRITICAL).withType("EXTERNAL_DEPENDENCY").build()
        settingsRegistry.register(name, settings)

        def service = new DelegatingHealthCheckService(healthCheckRegistry)

        when:
        def result = service.runHealthCheck(name)

        then:
        result.name() == name

        and:
        !result.healthy

        and:
        result.unhealthy

        and:
        result.status() == Status.CRITICAL
    }

    def "Running all health checks delegates to the Dropwizard registry and gets the correct settings"() {
        given:

        def checkOne = "a_check"
        def checkTwo = "some_check"
        def messageOne = "It worked"
        def messageTwo = "yeah!"

        def healthCheckRegistry = mock(HealthCheckRegistry.class)
        def healthCheckResults = new TreeMap()
        healthCheckResults.put(checkOne, HealthCheck.Result.healthy(messageOne))
        healthCheckResults.put(checkTwo, HealthCheck.Result.healthy(messageTwo))
        when(healthCheckRegistry.runHealthChecks()).thenReturn(healthCheckResults)

        def settings = HealthCheckSettings.withLevel(Level.WARNING).withType("EXTERNAL_DEPENDENCY").build()

        def check = mock(HealthCheck.class)

        def service = new DelegatingHealthCheckService(healthCheckRegistry)
        service.registerHealthCheck(checkOne, check, settings)
        service.registerHealthCheck(checkTwo, check)

        when:
        def serviceHealth = service.runHealthChecks()

        then:
        serviceHealth.status() == Status.HEALTHY

        and:
        def executedChecks = serviceHealth.executedChecks()
        executedChecks.size() == 2

        and:
        executedChecks[0] == new HealthCheckResult(checkOne, settings, HealthCheck.Result.healthy(messageOne))

        and:
        executedChecks[1] == new HealthCheckResult(checkTwo, HealthCheckSettings.DEFAULT_SETTINGS,
                HealthCheck.Result.healthy(messageTwo))
    }

    def "Running all health checks concurrently delegates to the Dropwizard registry and gets the correct settings"() {
        given:

        def checkOne = "a_check"
        def checkTwo = "some_check"
        def messageOne = "It worked"
        def messageTwo = "yeah!"

        ExecutorService executor = mock(ExecutorService.class)

        def healthCheckRegistry = mock(HealthCheckRegistry.class)
        def healthCheckResults = new TreeMap()
        healthCheckResults.put(checkOne, HealthCheck.Result.healthy(messageOne))
        healthCheckResults.put(checkTwo, HealthCheck.Result.healthy(messageTwo))
        when(healthCheckRegistry.runHealthChecks(executor)).thenReturn(healthCheckResults)

        def settings = HealthCheckSettings.withLevel(Level.WARNING).withType("EXTERNAL_DEPENDENCY").build()

        def check = mock(HealthCheck.class)

        def service = new DelegatingHealthCheckService(healthCheckRegistry)
        service.registerHealthCheck(checkOne, check, settings)
        service.registerHealthCheck(checkTwo, check)

        when:
        def serviceHealth = service.runHealthChecksConcurrently(executor)

        then:
        serviceHealth.status() == Status.HEALTHY

        and:
        def executedChecks = serviceHealth.executedChecks()
        executedChecks[0] == new HealthCheckResult(checkOne, settings, HealthCheck.Result.healthy(messageOne))

        and:
        executedChecks[1] == new HealthCheckResult(checkTwo, HealthCheckSettings.DEFAULT_SETTINGS,
                HealthCheck.Result.healthy(messageTwo))
    }

    def "Registering a health check with settings registers it with both the health check and settings registries"() {
        given:
        def healthCheckRegistry = mock(HealthCheckRegistry.class)
        def settingsRegistry = mock(HealthCheckSettingsRegistry.class)
        def settingsExtractor = mock(HealthCheckSettingsExtractor.class)
        def healthCheckDecorator = { c -> c }
        def service = new DelegatingHealthCheckService(healthCheckRegistry, settingsRegistry,
                settingsExtractor, healthCheckDecorator)

        def name = "some name"
        def check = mock(HealthCheck.class)
        def settings = HealthCheckSettings.withLevel(Level.CRITICAL).withType("EXTERNAL_DEPENDENCY").build()
        when(settingsExtractor.extractSettings(check)).thenReturn(HealthCheckSettings.DEFAULT_SETTINGS)

        when:

        service.registerHealthCheck(name, check, settings)

        then:
        verify(healthCheckRegistry).register(name, check)
        verify(settingsRegistry).register(name, settings)
    }

    def "Registering a health check without explicit settings extracts the settings from the health check"() {
        given:
        def healthCheckRegistry = mock(HealthCheckRegistry.class)
        def settingsRegistry = mock(HealthCheckSettingsRegistry.class)
        def settingsExtractor = mock(HealthCheckSettingsExtractor.class)
        def settings = HealthCheckSettings.withLevel(Level.CRITICAL).withType("EXTERNAL_DEPENDENCY").build()
        def check = mock(HealthCheck.class)
        when(settingsExtractor.extractSettings(check)).thenReturn(settings)

        def healthCheckDecorator = { c -> c }

        def service = new DelegatingHealthCheckService(healthCheckRegistry, settingsRegistry,
                settingsExtractor, healthCheckDecorator)

        when:
        def name = "some name"
        service.registerHealthCheck(name, check)

        then:
        verify(healthCheckRegistry).register(name, check)
        verify(settingsRegistry).register(name, settings)
    }

    def "Registering a health check with settings overrides annotation based settings"() {
        given:
        def check = mock(HealthCheck.class)

        def healthCheckRegistry = mock(HealthCheckRegistry.class)
        def settingsRegistry = mock(HealthCheckSettingsRegistry.class)
        def settingsExtractor = mock(HealthCheckSettingsExtractor.class)

        def annotationSettings = HealthCheckSettings
                .withLevel(Level.CRITICAL)
                .withType("SELF")
                .withDescription("Some description")
                .withDependentOn("some_service")
                .withLink("www.me.com")
                .build()
        when(settingsExtractor.extractSettings(check)).thenReturn(annotationSettings)

        def healthCheckDecorator = { c -> c }
        def service = new DelegatingHealthCheckService(healthCheckRegistry, settingsRegistry,
                settingsExtractor, healthCheckDecorator)

        def name = "some name"
        def settings = HealthCheckSettings.withLevel(Level.WARNING).withType("EXTERNAL_DEPENDENCY").build()

        when:

        service.registerHealthCheck(name, check, settings)

        then:
        verify(healthCheckRegistry).register(name, check)
        verify(settingsRegistry).register(name, HealthCheckSettings
                .withLevel(Level.WARNING)
                .withType("EXTERNAL_DEPENDENCY")
                .withDescription("Some description")
                .withDependentOn("some_service")
                .withLink("www.me.com")
                .build())
    }

    def "A health check is decorated when registered"() {
        given:
        def healthCheckRegistry = mock(HealthCheckRegistry.class)
        def settingsRegistry = mock(HealthCheckSettingsRegistry.class)
        def settingsExtractor = mock(HealthCheckSettingsExtractor.class)
        def settings = HealthCheckSettings.DEFAULT_SETTINGS
        def rawCheck = mock(HealthCheck.class)
        when(settingsExtractor.extractSettings(rawCheck)).thenReturn(settings)

        def decoratedCheck = mock(HealthCheck.class)
        def healthCheckDecorator = mock(HealthCheckDecorator.class)
        when(healthCheckDecorator.decorate(rawCheck)).thenReturn(decoratedCheck)

        def service = new DelegatingHealthCheckService(healthCheckRegistry, settingsRegistry,
                settingsExtractor, healthCheckDecorator)

        when:
        def name = "some name"
        service.registerHealthCheck(name, rawCheck)

        then:
        verify(healthCheckRegistry).register(name, decoratedCheck)
    }
}
