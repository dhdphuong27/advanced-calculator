package com.example.calculator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.example.calculator.model.AdvancedCalculator;


import java.util.Arrays;
import java.util.List;

import ru.noties.jlatexmath.JLatexMathView;

public class MainActivity extends AppCompatActivity {


    private EditText inputField;
    private LinearLayout resultsContainer;
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "theme";
    private boolean isColorblindTheme = false;

    List<String> functionNames = Arrays.asList("ln", "sin", "cos", "tan",
                                                "integral","derivative","sinh",
                                                "cosh","tanh");
    private double memory = 0.0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Load the saved theme preference
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isColorblindTheme = preferences.getBoolean(KEY_THEME, false);

        // Apply the theme before setting the content view
        if (isColorblindTheme) {
            setTheme(R.style.Theme_Calculator_Colorblind);
        } else {
            setTheme(R.style.Theme_Calculator_Default);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        inputField = findViewById(R.id.inputField);
        inputField.setShowSoftInputOnFocus(false);
        inputField.requestFocus();
        //resultsContainer = findViewById(R.id.resultsContainer);
        // Turn off keyboard popup on InputView



        // Set up all numeric and operator buttons
        setNumericOnClickListener();
        setOperatorOnClickListener();
        setAdvancedOperatorOnClickListener();
        setMemoryOnClickListener();
        setOtherOperatorOnClickListener();



        // Setup the equals button listener
        Button btnEquals = findViewById(R.id.btn_equals);

        inputField.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                displayLatexCalculation();
            }
        });

        btnEquals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //replace m with memory value

                String existingText = inputField.getText().toString();
                String mem = String.valueOf(memory);
                String updatedText = existingText.replace("m", mem);
                inputField.setText(updatedText);
                inputField.setSelection(updatedText.length());

                calculateAndDisplayResult();
            }
        });

        // Clear button listener
        Button btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputField.setText("");
                //resultsContainer.
            }
        });

        Button btnLeft = findViewById(R.id.btn_left);
        btnLeft.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int cursorPosition = inputField.getSelectionStart();
                if (cursorPosition>0) {
                    cursorPosition-=1;
                    inputField.setSelection(cursorPosition);
                }
                //Toast.makeText(MainActivity.this, "Left button clicked", Toast.LENGTH_SHORT).show();
            }

        });

        Button btnRight = findViewById(R.id.btn_right);
        btnRight.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int cursorPosition = inputField.getSelectionStart();
                if (cursorPosition<inputField.getText().length())
                {
                    cursorPosition+=1;
                    inputField.setSelection(cursorPosition);
                    //Toast.makeText(MainActivity.this, "Right button clicked", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button btnSwap = findViewById(R.id.btn_swap_mode);
        btnSwap.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                isColorblindTheme = !isColorblindTheme;

                // Save the preference
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(KEY_THEME, isColorblindTheme);
                editor.apply();

                // Restart activity to apply new theme
                recreate();
            }
        });


        // Delete button listener (removes the last character)
        Button btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cursorPosition = inputField.getSelectionStart();
                String existingText = inputField.getText().toString();

                if (cursorPosition > 0) {
                    // Check if the character before the cursor is a ')'
                    if (existingText.charAt(cursorPosition - 1) == ')') {
                        // Try to find the matching '(' and the function name before it
                        int openCount = 0;
                        int closeCount = 1;
                        int startIndex = cursorPosition - 2; // Start checking before the ')'

                        while (startIndex >= 0) {
                            if (existingText.charAt(startIndex) == ')') {
                                closeCount++;
                            } else if (existingText.charAt(startIndex) == '(') {
                                openCount++;
                                if (openCount == closeCount) {
                                    // Found the matching '(', now check for a function name before it
                                    for (String functionName : functionNames) {
                                        int functionIndex = startIndex - functionName.length();
                                        if (functionIndex >= 0 && existingText.substring(functionIndex, startIndex).equals(functionName)) {
                                            // Found the whole function to delete
                                            String updatedText = existingText.substring(0, functionIndex) + existingText.substring(cursorPosition);
                                            inputField.setText(updatedText);
                                            inputField.setSelection(functionIndex);
                                            return; // Exit
                                        }
                                    }
                                    // If no function name found, just delete the ')'
                                    String updatedText = existingText.substring(0, cursorPosition - 1) + existingText.substring(cursorPosition);
                                    inputField.setText(updatedText);
                                    inputField.setSelection(cursorPosition - 1);
                                    return; // Exit
                                }
                            }
                            startIndex--;
                        }
                        // If no matching '(' found, just delete the ')'
                        String updatedText = existingText.substring(0, cursorPosition - 1) + existingText.substring(cursorPosition);
                        inputField.setText(updatedText);
                        inputField.setSelection(cursorPosition - 1);
                        return; // Exit
                    } else if (existingText.charAt(cursorPosition - 1) == '(') {
                        // Check for a function name immediately before the '('
                        for (String functionName : functionNames) {
                            int functionIndex = cursorPosition - 1 - functionName.length();
                            if (functionIndex >= 0 && existingText.substring(functionIndex, cursorPosition - 1).equals(functionName)) {
                                // Found a function, try to find the matching ')'
                                int openCount = 1;
                                int closeCount = 0;
                                int endIndex = cursorPosition;

                                while (endIndex < existingText.length()) {
                                    if (existingText.charAt(endIndex) == '(') {
                                        openCount++;
                                    } else if (existingText.charAt(endIndex) == ')') {
                                        closeCount++;
                                    }

                                    if (openCount == closeCount && openCount > 0) {
                                        // Found the matching ')'
                                        String updatedText = existingText.substring(0, functionIndex) + existingText.substring(endIndex + 1);
                                        inputField.setText(updatedText);
                                        inputField.setSelection(functionIndex);
                                        return; // Exit
                                    }
                                    endIndex++;
                                }
                                // If no matching ')' is found, just delete the '('
                                String updatedText = existingText.substring(0, cursorPosition - 1) + existingText.substring(cursorPosition);
                                inputField.setText(updatedText);
                                inputField.setSelection(cursorPosition - 1);
                                return; // Exit
                            }
                        }
                        // If no function name found before '(', just delete the '('
                        String updatedText = existingText.substring(0, cursorPosition - 1) + existingText.substring(cursorPosition);
                        inputField.setText(updatedText);
                        inputField.setSelection(cursorPosition - 1);
                        return; // Exit
                    } else {
                        // Default delete if not at a '(' or ')'
                        String updatedText = existingText.substring(0, cursorPosition - 1) + existingText.substring(cursorPosition);
                        inputField.setText(updatedText);
                        inputField.setSelection(cursorPosition - 1);
                    }
                }
            }
        });
    }
    private void displayLatexCalculation() {
        String expression = inputField.getText().toString().trim();

        if (expression.isEmpty()) {
            displayError("Please enter an expression");
            return;
        }
        // Evaluate the expression using our AdvancedCalculator
        AdvancedCalculator.CalculationResult result = AdvancedCalculator.toLatexFormat(expression);

        // Display the result
        JLatexMathView mathView = findViewById(R.id.j_latex_math_view);
        //Toast.makeText(MainActivity.this, result.getLatexExpression(), Toast.LENGTH_SHORT).show();

        if (result.getError().equals("calculating")) {
            String latex = result.getLatexExpression();
            //Toast.makeText(MainActivity.this, latex, Toast.LENGTH_SHORT).show();
            mathView.setLatex(latex);
        }
    }
    private double calculateAndDisplayResult() {
        String expression = inputField.getText().toString().trim();

        if (expression.isEmpty()) {
            displayError("Please enter an expression");
            return 0.0;
        }
        // Evaluate the expression using our AdvancedCalculator
        AdvancedCalculator.CalculationResult result = AdvancedCalculator.evaluateExpression(expression);

        // Display the result
        JLatexMathView mathView = findViewById(R.id.j_latex_math_view);

        if (result.getError()!=null) {
            mathView.setLatex("\\textcolor{red}{\\text{" + result.getError() + "}}");
        } else {
            // Display both the expression and result in LaTeX format
            String latex = result.getLatexExpression();
            mathView.setLatex(latex);
            System.out.println("Calculate result: "+ result.getResult());
            return result.getResult();
        }

        // Add some margin between results
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        mathView.setLayoutParams(params);
        return -1;
        // Add to results container
        //resultsContainer.addView(mathView, 0); // Add at top to show newest first
    }

    private void displayError(String message) {
        JLatexMathView errorView = new JLatexMathView(this);
        errorView.setLatex("\\textcolor{red}{\\text{" + message + "}}");
        //resultsContainer.addView(errorView, 0);
    }

    private void setNumericOnClickListener() {
        int[] numericButtonIds = new int[]{
                R.id.btn_0, R.id.btn_1, R.id.btn_2,
                R.id.btn_3, R.id.btn_4, R.id.btn_5,
                R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_decimal, R.id.x
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button)v;
                int cursorPosition = inputField.getSelectionStart();
                String existingText = inputField.getText().toString();
                String newText = btn.getText().toString();
                String updatedText = existingText.substring(0, cursorPosition) + newText + existingText.substring(cursorPosition);
                inputField.setText(updatedText);
                inputField.setSelection(cursorPosition+ newText.length());
            }
        };

        for (int id : numericButtonIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void setOperatorOnClickListener() {
        int[] operatorButtonIds = new int[]{
                R.id.btn_add, R.id.btn_subtract,
                R.id.btn_multiply, R.id.btn_divide,
                R.id.btn_open_parenthesis, R.id.btn_close_parenthesis,
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button)v;
                int cursorPosition = inputField.getSelectionStart();
                String existingText = inputField.getText().toString();
                String newText = btn.getText().toString();
                String updatedText = existingText.substring(0, cursorPosition) + newText + existingText.substring(cursorPosition);
                inputField.setText(updatedText);
                inputField.setSelection(cursorPosition+ newText.length());
            }
        };

        for (int id : operatorButtonIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void setAdvancedOperatorOnClickListener(){
        int[] operatorButtonIds = new int[]{
                R.id.sin, R.id.cos, R.id.tan, R.id.ln, R.id.sinh, R.id.cosh, R.id.tanh
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button)v;
                int cursorPosition = inputField.getSelectionStart();
                String existingText = inputField.getText().toString();
                String newText = btn.getText().toString()+"()";
                String updatedText = existingText.substring(0, cursorPosition) + newText + existingText.substring(cursorPosition);
                inputField.setText(updatedText);
                inputField.setSelection(cursorPosition+ newText.length()-1);
            }
        };

        for (int id : operatorButtonIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }
    private void setOtherOperatorOnClickListener(){
        int[] operatorButtonIds = new int[]{
                R.id.factorial, R.id.power,
                R.id.root, R.id.integral,
                R.id.derivative, R.id.euler,
                R.id.pi, R.id.log, R.id.btn_percentage
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button)v;
                int cursorPosition = inputField.getSelectionStart();
                String existingText = inputField.getText().toString();
                int btnId = btn.getId();
                int offset = 0;
                String newText = "";
                if (btnId == R.id.factorial) {
                    newText = "!";
                } else if (btnId == R.id.power) {
                    newText = "^";
                } else if (btnId == R.id.root) {
                    newText = "root(,)";
                    offset = 2;
                } else if (btnId == R.id.integral) {
                    newText = "integral(,,)";
                    offset = 3;
                } else if (btnId == R.id.derivative){
                    newText = "derivative(,)";
                    offset = 2;
                } else if (btnId == R.id.euler){
                    newText = "e";
                } else if (btnId == R.id.pi){
                    newText = "π";
                } else if (btnId == R.id.log){
                    newText = "log(,)";
                    offset = 2;
                } else if (btnId == R.id.btn_percentage) {
                    newText = "/100";
                }
                String updatedText = existingText.substring(0, cursorPosition) + newText + existingText.substring(cursorPosition);
                inputField.setText(updatedText);
                inputField.setSelection(cursorPosition+ newText.length()-offset);
            }
        };

        for (int id : operatorButtonIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void setMemoryOnClickListener(){

        int[] operatorButtonIds = new int[]{
                R.id.mplus, R.id.mminus, R.id.mrecall, R.id.mclear
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "memory: " + memory, Toast.LENGTH_SHORT).show();
                Button btn = (Button) v;
                int btnId = btn.getId();
                if (btnId == R.id.mrecall){
                    Toast.makeText(MainActivity.this, "m = " + memory, Toast.LENGTH_SHORT).show();
                    int cursorPosition = inputField.getSelectionStart();
                    String existingText = inputField.getText().toString();
                    String newText = "m";
                    String updatedText = existingText.substring(0, cursorPosition) + newText + existingText.substring(cursorPosition);
                    inputField.setText(updatedText);
                    inputField.setSelection(cursorPosition+ newText.length());
                    return;
                }
                // Evaluate the expression using our AdvancedCalculator
                if (btnId == R.id.mplus){
                    double result = calculateAndDisplayResult();
                    memory += result;
                    Toast.makeText(MainActivity.this, "m = " + memory, Toast.LENGTH_SHORT).show();
                }
                if (btnId == R.id.mminus){
                    double result = calculateAndDisplayResult();
                    memory -= result;
                    Toast.makeText(MainActivity.this, "m = " + memory, Toast.LENGTH_SHORT).show();
                }
                if (btnId == R.id.mclear){
                    memory = 0;
                    Toast.makeText(MainActivity.this, "m = " + memory, Toast.LENGTH_SHORT).show();
                }
            }
        };
        for (int id : operatorButtonIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

}