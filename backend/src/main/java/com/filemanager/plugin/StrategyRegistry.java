package com.filemanager.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrategyRegistry {

    private static StrategyRegistry instance;
    private final Map<String, StrategyConfigurable> strategyMap;

    private StrategyRegistry() {
        this.strategyMap = new HashMap<>();
    }

    public static synchronized StrategyRegistry getInstance() {
        if (instance == null) {
            instance = new StrategyRegistry();
        }
        return instance;
    }

    public void registerStrategy(StrategyConfigurable strategy) {
        if (strategy != null) {
            strategyMap.put(strategy.getId(), strategy);
        }
    }

    public StrategyConfigurable getStrategy(String strategyId) {
        return strategyMap.get(strategyId);
    }

    public List<StrategyConfigurable> getStrategies() {
        return new ArrayList<>(strategyMap.values());
    }

    public boolean isStrategyRegistered(String strategyId) {
        return strategyMap.containsKey(strategyId);
    }

    public void unregisterStrategy(String strategyId) {
        strategyMap.remove(strategyId);
    }

    public int getStrategyCount() {
        return strategyMap.size();
    }
}