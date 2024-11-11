package com.example.sample.flow.files;

public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    public int calculateSum(int x, int y) {
        return add(x, y);
    }

    public int calculateDifference(int x, int y) {
        return subtract(x, y);
    }

    public static void main(String[] args) {
        Calculator calc = new Calculator();
        int sum = calc.calculateSum(10, 20);
        int diff = calc.calculateDifference(20, 10);
        System.out.println("Sum: " + sum);
        System.out.println("Difference: " + diff);
    }
}
