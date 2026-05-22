package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import java.time.LocalDate;
import java.util.List;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Contract;
import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Currency;

public interface RecordRepository {

    List<RecordView> find(LocalDate dateStart,
                          LocalDate dateFinish,
                          IdLike<Contract> contractId,
                          IdLike<Currency> currencyId,
                          LoadPlanLike loadPlanLike);
}