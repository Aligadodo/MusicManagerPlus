package com.filemanager.strategy.collection;

/**
 * TextSimilarityCalculator适配器
 * 将TextSimilarityCalculator适配为StringSimilarityCalculator接口
 * 
 * @author FileEditTools Team
 */
public class TextSimilarityCalculatorAdapter implements StringSimilarityCalculator {
    
    private final TextSimilarityCalculator calculator;
    
    public TextSimilarityCalculatorAdapter(TextSimilarityCalculator calculator) {
        this.calculator = calculator;
    }
    
    @Override
    public double calculateSimilarity(String s1, String s2) {
        return calculator.calculateSimilarity(s1, s2);
    }
}
