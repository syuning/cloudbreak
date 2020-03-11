package com.sequenceiq.environment.telemetry.v1.controller;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.environment.api.v1.telemetry.endpoint.AccountTelemetryEndpoint;
import com.sequenceiq.environment.api.v1.telemetry.model.request.AccountTelemetryRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.request.TestAnonymizationRulesRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.response.AccountTelemetryResponse;
import com.sequenceiq.environment.api.v1.telemetry.model.response.TestAnonymizationRulesResponse;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.service.AccountTelemetryService;
import com.sequenceiq.environment.telemetry.v1.converter.AccountTelemetryConverter;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class AccountTelemetryController extends NotificationController implements AccountTelemetryEndpoint {

    private final AccountTelemetryService accountTelemetryService;

    private final AccountTelemetryConverter accountTelemetryConverter;

    public AccountTelemetryController(AccountTelemetryService accountTelemetryService,
            AccountTelemetryConverter accountTelemetryConverter) {
        this.accountTelemetryService = accountTelemetryService;
        this.accountTelemetryConverter = accountTelemetryConverter;
    }

    @Override
    public AccountTelemetryResponse get() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return accountTelemetryConverter.convert(accountTelemetryService.getOrDefault(accountId));
    }

    @Override
    public AccountTelemetryResponse update(AccountTelemetryRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        AccountTelemetry telemetry = accountTelemetryConverter.convert(request);
        return accountTelemetryConverter.convert(accountTelemetryService.create(telemetry, accountId));
    }

    @Override
    public AccountTelemetryResponse getDefault() {
        return accountTelemetryConverter.convert(accountTelemetryService.createDefaultAccuontTelemetry());
    }

    @Override
    public FeaturesResponse listFeatures() {
        AccountTelemetryResponse response = get();
        return response.getFeatures();
    }

    @Override
    public FeaturesResponse updateFeatures(FeaturesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return accountTelemetryConverter.convertFeatures(
                accountTelemetryService.updateFeatures(
                        accountId, accountTelemetryConverter.convertFeatures(request)));
    }

    @Override
    public List<AnonymizationRule> listRules() {
        AccountTelemetryResponse response = get();
        return response.getRules();
    }

    @Override
    public TestAnonymizationRulesResponse testRulePattern(TestAnonymizationRulesRequest request) {
        return null;
    }
}
