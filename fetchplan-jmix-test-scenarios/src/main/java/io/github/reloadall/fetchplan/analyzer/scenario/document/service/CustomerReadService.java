package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerReadService {

    private final ManagerReadService managerReadService;

    @Autowired
    public CustomerReadService(ManagerReadService managerReadService) {
        this.managerReadService = managerReadService;
    }

    public void readCustomer(Customer customer) {
        customer.getName();
        customer.getTier().getCode();
        managerReadService.readManager(customer.getManager());
    }
}