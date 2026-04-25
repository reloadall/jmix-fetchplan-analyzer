package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContractReadService {

    private final CustomerReadService customerReadService;

    @Autowired
    public ContractReadService(CustomerReadService customerReadService) {
        this.customerReadService = customerReadService;
    }

    public void readContract(Contract contract) {
        contract.getNumber();
        customerReadService.readCustomer(contract.getCustomer());
    }
}