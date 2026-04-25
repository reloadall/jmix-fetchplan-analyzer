package io.github.reloadall.fetchplan.analyzer.scenario.document.service;

import io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Manager;
import org.springframework.stereotype.Service;

@Service
public class ManagerReadService {

    public void readManager(Manager manager) {
        manager.getEmail();
        manager.getDepartment().getName();
    }
}