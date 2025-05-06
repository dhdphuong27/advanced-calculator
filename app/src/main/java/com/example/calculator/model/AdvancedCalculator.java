package com.example.calculator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedCalculator {

    private static final Pattern DERIVATIVE_PATTERN = Pattern.compile("derivative\\(([^,]+),([^\\)]+)\\)");
    public interface IntegralFunction extends org.apache.commons.math3.analysis.UnivariateFunction {
        @Override
        double value(double x);
    }
    public static double roundIfAlmostInteger(double value) {
        final double EPSILON = 1e-1; // Adjust this threshold as needed
        double rounded = Math.round(value);
        return Math.abs(value - rounded) <= EPSILON ? rounded : value;
    }


    public static class CalculationResult {
        private final String latexExpression;
        private final double result;
        private final String error;


        public CalculationResult(String latexExpression, double result, String error) {
            this.latexExpression = latexExpression;
            this.result = roundIfAlmostInteger(result);
            this.error = error;
        }


        public CalculationResult(String latexExpression) {
            this.latexExpression = latexExpression;
            this.result = 0;
            this.error = "calculating";
        }

        public String getLatexExpression() {
            return latexExpression;
        }

        public double getResult() {
            return result;
        }

        public String getError() {
            return error;
        }

        public boolean hasError() {
            return error != null && !error.isEmpty();
        }
    }

    // Update your evaluateExpression method to use the new function
    public static CalculationResult evaluateExpression(String expression) {
        try {
            String processed = preProcessExpression(expression);
            System.out.println("Expression after preprocessing: " + processed);

            String expressionEvaluated = evaluateAndReplaceIntegralsAndDerivatives(processed);

            System.out.println("Expression after evaluation: " + expressionEvaluated);

            List<String> postfix = infixToPostfix(expressionEvaluated);
            System.out.println("Expression after infix to postfix: " + postfix);

            double result = Double.parseDouble(evaluatePostfix(postfix));
            System.out.println("result: " + result);

            //Because the approximate can be a litte it off, so i'm rounding it up here
            String latex = generateLatexForCombined(expression, roundIfAlmostInteger(result));
            System.out.println("latex: " + latex);
            return new CalculationResult(latex, roundIfAlmostInteger(result), null);
        } catch (Exception e) {
            return new CalculationResult("", 0, "Error: " + e.getMessage());
        }
    }
    // Modify evaluateAndReplaceIntegrals to also handle derivatives
    private static String evaluateAndReplaceIntegralsAndDerivatives(String expression) {
        // First handle integrals
        expression = evaluateAndReplaceIntegrals(expression);

        // Then handle derivatives
        Matcher matcher = DERIVATIVE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            try {
                double point = Double.parseDouble(matcher.group(1));
                String function = matcher.group(2);
                double derivative = numericalDerivative(function, point, 0.0001); // Using h=0.0001 for good accuracy
                matcher.appendReplacement(sb, String.valueOf(derivative));
            } catch (NumberFormatException e) {
                matcher.appendReplacement(sb, "NaN");
            } catch (Exception e) {
                matcher.appendReplacement(sb, "NaN");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String evaluateAndReplaceIntegrals(String expression) {
        Pattern integralPattern = Pattern.compile("integral\\(([^,]+),([^,]+),([^\\)]+)\\)");
        Matcher matcher = integralPattern.matcher(expression);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            try {
                double lowerBound = Double.parseDouble(matcher.group(1));
                double upperBound = Double.parseDouble(matcher.group(2));
                String integrand = matcher.group(3);
                double integralResult = riemannIntegral(integrand, lowerBound, upperBound, 1000);
                matcher.appendReplacement(sb, String.valueOf(integralResult));
            } catch (NumberFormatException e) {
                // Handle error in integral bounds
                matcher.appendReplacement(sb, "NaN"); // Or throw an exception
            } catch (Exception e) {
                // Handle error in integral evaluation
                matcher.appendReplacement(sb, "NaN"); // Or throw an exception
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    private static double riemannIntegral(String functionStr, double a, double b, int n) throws Exception {
        double h = (b - a) / n;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double x_i = a + (i + 0.5) * h; // Midpoint rule
            // Evaluate the function at x_i
            List<String> postfix = infixToPostfix(preProcessExpression(functionStr.replaceAll("x", String.valueOf(x_i))));
            sum += Double.parseDouble(evaluatePostfix(postfix));
        }
        return sum * h;
    }
    // Update generateLatexForCombined to handle derivatives
    private static String generateLatexForCombined(String originalExpression, double result) {
        String latex = originalExpression
                .replaceAll("\\^", "\\\\pow")
                .replaceAll("sqrt\\((.*?)\\)", "\\\\sqrt{$1}")
                .replaceAll("sin\\((.*?)\\)", "\\\\sin($1)")
                .replaceAll("cos\\((.*?)\\)", "\\\\cos($1)")
                .replaceAll("tan\\((.*?)\\)", "\\\\tan($1)")
                .replaceAll("sinh\\((.*?)\\)", "\\\\sinh($1)")
                .replaceAll("cosh\\((.*?)\\)", "\\\\cosh($1)")
                .replaceAll("tanh\\((.*?)\\)", "\\\\tanh($1)")

                .replaceAll("log\\(([^,]+),([^\\)]+)\\)", "\\\\log_{$1}{$2}")
                .replaceAll("ln\\((.*?)\\)", "\\\\ln($1)")
                .replaceAll("π", "\\\\pi")
                .replaceAll("!", "!\\\\")
                .replaceAll("e", "e")
                .replaceAll("integral\\(([^,]+),([^,]+),([^\\)]+)\\)", "\\\\int_{$1}^{$2}{$3} dx")
                .replaceAll("derivative\\(([^,]+),([^\\)]+)\\)", "\\\\frac{d}{dx}\\($2\\)\\|_{x=$1}")
                .replaceAll("root\\(([^,]+),([^\\)]+)\\)", "\\\\sqrt[$1]{$2}");

        latex = latex.replaceAll("\\\\pow(?![\\d\\-x])", "^{}");
        latex = latex.replaceAll("\\\\pow([\\d]+|x)", "^{$1}");

        return latex + " = " + result;
    }

    public static CalculationResult toLatexFormat(String expression) {
        try {
            String latex = generateLatex(expression);
            return new CalculationResult(latex);
        } catch (Exception e) {
            return new CalculationResult("", 0, "Error: " + e.getMessage());
        }
    }

    private static String preProcessExpression(String expression) {
        // Remove all whitespace
        expression = expression.replaceAll("\\s+", "");

        // Replace constants
        expression = expression.replaceAll("π", Double.toString(Math.PI));
        expression = expression.replaceAll("\\be\\b", Double.toString(Math.E));

        // Insert '*' between a number and a variable/function
        Pattern implicitMultiplication = Pattern.compile("(\\d)([a-zA-Z(])");
        Matcher matcher = implicitMultiplication.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + "*" + matcher.group(2));
        }
        matcher.appendTail(sb);
        expression = sb.toString();

        return expression;
    }

    private static List<String> infixToPostfix(String expression) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        List<String> tokens = tokenizeExpression(expression);
        int parenCount = 0; // Track parentheses nesting level

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (isNumeric(token) || token.equals("x")) {
                output.add(token);
            }
            else if (token.startsWith("integral(")) {
                // Handle integral function
                String content = token.substring("integral(".length(), token.length() - 1);
                String[] parts = content.split(",");
                if (parts.length == 3) {
                    List<String> integrandPostfix = infixToPostfix(parts[2]);
                    output.addAll(integrandPostfix);
                    output.add(parts[0]); // lower bound
                    output.add(parts[1]); // upper bound
                    output.add("integral_op");
                } else {
                    throw new IllegalArgumentException("Invalid integral format: " + token);
                }
            }else if (token.startsWith("root(")) {
                // Handle root function
                String content = token.substring("root(".length(), token.length() - 1);
                String[] parts = content.split(",");
                if (parts.length == 2) {
                    // First push the radicand (x), then the degree (n)
                    List<String> radicandPostfix = infixToPostfix(parts[1]);
                    output.addAll(radicandPostfix);
                    List<String> degreePostfix = infixToPostfix(parts[0]);
                    output.addAll(degreePostfix);
                    output.add("root");
                } else {
                    throw new IllegalArgumentException("Invalid root format: " + token);
                }
            }
            else if (isFunction(token)) {
                stack.push(token);
            }
            else if (token.equals("(")) {
                stack.push(token);
                parenCount++;
            }
            else if (token.equals(",")) {
                // Handle function argument separator
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty() || !stack.peek().equals("(")) {
                    throw new IllegalArgumentException("Mismatched parentheses or comma");
                }
            }
            else if (token.equals(")")) {
                // Handle closing parenthesis
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Mismatched parentheses");
                }
                stack.pop(); // Remove the '('
                parenCount--;

                if (!stack.isEmpty() && isFunction(stack.peek())) {
                    output.add(stack.pop());
                }
            }
            else if (token.equals("!")) {
                output.add(token);
            }
            else if (token.equals("unary-")) {
                stack.push(token);
            }
            else {
                // Handle operators with proper precedence
                while (!stack.isEmpty() &&
                        !stack.peek().equals("(") &&
                        getPrecedence(stack.peek()) >= getPrecedence(token)) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }

        // Check for balanced parentheses
        if (parenCount != 0) {
            throw new IllegalArgumentException("Mismatched parentheses");
        }

        // Pop all remaining operators
        while (!stack.isEmpty()) {
            String op = stack.pop();
            if (op.equals("(")) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            output.add(op);
        }

        return output;
    }

    private static List<String> tokenizeExpression(String expression) {
        List<String> tokens = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "(\\d+\\.?\\d*)|" +                 // Numbers
                        "([a-zA-Z]+\\()|" +                 // Functions with opening paren
                        "([a-zA-Z]+)|" +                    // Variables or constants
                        "([(),+\\-*/^!])|" +               // Operators and parentheses
                        "(integral\\([^\\)]+\\))|" +       // Integral function
                        "(root\\([^\\)]+\\))"              // Root function
        );

        Matcher matcher = pattern.matcher(expression);
        String previousToken = null;
        int pos = 0;

        while (matcher.find()) {
            if (matcher.start() != pos) {
                throw new IllegalArgumentException("Invalid character at position " + pos);
            }
            pos = matcher.end();

            String token = matcher.group();

            // Handle negative numbers and unary minus
            if (token.equals("-") &&
                    (previousToken == null ||
                            previousToken.equals("(") ||
                            isOperator(previousToken) ||
                            previousToken.equals(","))) {
                tokens.add("unary-");
            }
            else if (token.endsWith("(") && token.length() > 1) {
                // This is a function call - split into function name and '('
                String funcName = token.substring(0, token.length()-1);
                tokens.add(funcName);
                tokens.add("(");
            }
            else {
                tokens.add(token);
            }

            previousToken = token;
        }

        if (pos != expression.length()) {
            throw new IllegalArgumentException("Invalid character at position " + pos);
        }

        return tokens;
    }


    private static boolean isOperator(String token) {
        return "+-*/^".contains(token);
    }

    private static String evaluatePostfix(List<String> postfix) {
        Stack<String> stack = new Stack<>();

        for (String token : postfix) {
            System.out.println(stack);
            if (isNumeric(token)) {
                stack.push(token);
            } else if (token.equals("x")) {
                stack.push("x");
            } else if (token.equals("integral_op")) {
                double upperBound = Double.parseDouble(stack.pop());
                double lowerBound = Double.parseDouble(stack.pop());
                List<String> integrandPostfix = new ArrayList<>();
                while (!stack.isEmpty() && !(isNumeric(stack.peek()) || stack.peek().equals("x") || isOperator(stack.peek()) || isFunction(stack.peek()))) {
                    integrandPostfix.add(0, stack.pop());
                }

                try {
                    double result = riemannIntegralPostfix(integrandPostfix, lowerBound, upperBound, 1000);
                    stack.push(String.valueOf(result));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error evaluating integral: " + e.getMessage());
                }
            } else if (isFunction(token)) {
                double operand = Double.parseDouble(stack.pop());
                double result = evaluateFunction(token, operand);
                stack.push(String.valueOf(result));
            } else if (token.equals("log")) { // Handle two-operand logarithm
                double value = Double.parseDouble(stack.pop());
                double base = Double.parseDouble(stack.pop());
                stack.push(String.valueOf((Math.log(value) / Math.log(base))));
            }else if (token.equals("root")) { // Handle two-operand logarithm
                double value = Double.parseDouble(stack.pop());
                double base = Double.parseDouble(stack.pop());
                stack.push(String.valueOf(Math.pow(value, 1.0/base)));
            } else if (token.equals("!")) { // Handle factorial
                double operand = Double.parseDouble(stack.pop());
                stack.push(String.valueOf(factorial(operand)));
            } else if (token.equals("unary-")) {
                double operand = Double.parseDouble(stack.pop());
                stack.push(String.valueOf(-operand));
            } else {
                double operand2 = Double.parseDouble(stack.pop());
                double operand1 = Double.parseDouble(stack.pop());
                double result = evaluateOperator(token, operand1, operand2);
                stack.push(String.valueOf(result));
            }
        }
        return stack.pop();
    }

    private static double factorial(double n) {
        if (n == 0) {
            return 1;
        }
        if (n < 0 || n != Math.floor(n)) {
            throw new IllegalArgumentException("Factorial is only defined for non-negative integers.");
        }
        double result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isFunction(String str) {
        return str.equals("sin") || str.equals("cos") || str.equals("tan") ||
                str.equals("ln") || str.equals("sqrt")|| str.equals("sinh")||
                str.equals("cosh")|| str.equals("tanh"); // Removed "integral" here
    }

    private static int getPrecedence(String operator) {
        switch (operator) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
                return 2;
            case "^":
                return 3;
            default: // functions
                return 4;
        }
    }

    private static double evaluateOperator(String operator, double operand1, double operand2) {
        switch (operator) {
            case "+":
                return operand1 + operand2;
            case "-":
                return operand1 - operand2;
            case "*":
                return operand1 * operand2;
            case "/":
                if (operand2 == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                return operand1 / operand2;
            case "^":
                return Math.pow(operand1, operand2);
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    private static double evaluateFunction(String function, double operand) {
        switch (function) {
            case "sin":
                return Math.sin(operand);
            case "cos":
                return Math.cos(operand);
            case "tan":
                return Math.tan(operand);
            case "sinh":
                return Math.sinh(operand);
            case "cosh":
                return Math.cosh(operand);
            case "tanh":
                return Math.tanh(operand);
            case "log":
                return Math.log10(operand);
            case "ln":
                return Math.log(operand);
            case "sqrt":
                return Math.sqrt(operand);
            default:
                throw new IllegalArgumentException("Unknown function: " + function);
        }
    }


    private static String generateLatex(String originalExpression) {
        String latex = originalExpression
                .replaceAll("\\^", "\\\\pow")
                .replaceAll("sqrt\\((.*?)\\)", "\\\\sqrt{$1}")
                .replaceAll("sin\\((.*?)\\)", "\\\\sin($1)")
                .replaceAll("cos\\((.*?)\\)", "\\\\cos($1)")
                .replaceAll("tan\\((.*?)\\)", "\\\\tan($1)")
                .replaceAll("log\\(([^,]+),([^\\)]+)\\)", "\\\\log_{$1}{$2}")
                .replaceAll("ln\\((.*?)\\)", "\\\\ln($1)")
                .replaceAll("π", "\\\\pi")
                .replaceAll("!", "!\\\\")
                .replaceAll("e", "e")
                .replaceAll("integral\\(([^,]+),([^,]+),([^\\)]+)\\)", "\\\\int_{$1}^{$2}{$3} dx")
                .replaceAll("derivative\\(([^,]+),([^\\)]+)\\)", "\\\\frac{d}{dx}\\($2\\)\\|_{x=$1}")
                .replaceAll("root\\(([^,]+),([^\\)]+)\\)", "\\\\sqrt[$1]{$2}");

        latex = latex.replaceAll("\\\\pow(?![\\d\\-x])", "^{}");
        latex = latex.replaceAll("\\\\pow([\\d]+|x)", "^{$1}");

        return latex;
    }


    private static double riemannIntegralPostfix(List<String> integrandPostfix, double a, double b, int n) throws Exception {
        double h = (b - a) / n;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double x_i = a + (i + 0.5) * h;
            // Evaluate the postfix expression of the integrand with x = x_i
            List<String> substitutedPostfix = new ArrayList<>(integrandPostfix);
            for (int j = 0; j < substitutedPostfix.size(); j++) {
                if (substitutedPostfix.get(j).equals("x")) {
                    substitutedPostfix.set(j, String.valueOf(x_i));
                }
            }
            sum += Double.parseDouble(evaluatePostfix(substitutedPostfix)); // Recursive call
        }
        return sum * h;
    }

    // Add this method to evaluate derivatives numerically
    private static double numericalDerivative(String functionStr, double a, double h) throws Exception {
        // Central difference method for better accuracy
        List<String> postfixPlus = infixToPostfix(preProcessExpression(functionStr.replaceAll("x", String.valueOf(a + h))));
        List<String> postfixMinus = infixToPostfix(preProcessExpression(functionStr.replaceAll("x", String.valueOf(a - h))));

        double fPlus = Double.parseDouble(evaluatePostfix(postfixPlus));
        double fMinus = Double.parseDouble(evaluatePostfix(postfixMinus));

        return (fPlus - fMinus) / (2 * h);
    }
}