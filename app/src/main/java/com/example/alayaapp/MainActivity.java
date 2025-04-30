package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast; // For showing simple messages

import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2500; // 2.5 seconds


    private enum AppState {
        SPLASH, LOGIN, SIGNUP
    }
    private AppState currentState = AppState.SPLASH;


    ConstraintLayout mainContainer;
    ImageView splashLogo;
    ConstraintLayout formContainer;
    TextView formTitle;
    TextInputLayout emailLayout;
    TextInputLayout passwordLayout;
    TextInputLayout confirmPasswordLayout;
    Button actionButton;
    LinearLayout switchLayout;
    TextView switchPromptText;
    TextView switchLinkText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.Theme_AlayaApp_SplashImitation);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mainContainer = findViewById(R.id.main_container);
        splashLogo = findViewById(R.id.splash_logo);
        formContainer = findViewById(R.id.form_container);
        formTitle = findViewById(R.id.form_title);
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        actionButton = findViewById(R.id.action_button);
        switchLayout = findViewById(R.id.switch_layout);
        switchPromptText = findViewById(R.id.switch_prompt_text);
        switchLinkText = findViewById(R.id.switch_link_text);



        showSplash();


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showLogin();

            setTheme(R.style.Theme_AlayaApp);

            mainContainer.setBackgroundColor(getResources().getColor(R.color.white, getTheme()));
            formContainer.setBackgroundColor(getResources().getColor(R.color.white, getTheme()));

        }, SPLASH_DELAY);


        switchLayout.setOnClickListener(v -> {
            if (currentState == AppState.LOGIN) {
                showSignUp();
            } else if (currentState == AppState.SIGNUP) {
                showLogin();
            }
        });


        actionButton.setOnClickListener(v -> {
            if (currentState == AppState.LOGIN) {
                // TODO: Add actual login logic here
                Toast.makeText(MainActivity.this, "Sign In Clicked (No logic)", Toast.LENGTH_SHORT).show();
            } else if (currentState == AppState.SIGNUP) {
                // TODO: Add actual sign up logic here (validate passwords match etc.)
                Toast.makeText(MainActivity.this, "Sign Up Clicked (No logic)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSplash() {
        currentState = AppState.SPLASH;
        splashLogo.setVisibility(View.VISIBLE);
        formContainer.setVisibility(View.GONE);

        mainContainer.setBackgroundColor(getResources().getColor(R.color.colorPrimary, getTheme()));
    }

    private void showLogin() {
        currentState = AppState.LOGIN;
        splashLogo.setVisibility(View.GONE);
        formContainer.setVisibility(View.VISIBLE);


        formTitle.setText(R.string.login_title);
        confirmPasswordLayout.setVisibility(View.GONE);
        actionButton.setText(R.string.sign_in_button);
        switchPromptText.setText(R.string.prompt_no_account);
        switchLinkText.setText(R.string.link_sign_up);
    }

    private void showSignUp() {
        currentState = AppState.SIGNUP;
        splashLogo.setVisibility(View.GONE);
        formContainer.setVisibility(View.VISIBLE);


        formTitle.setText(R.string.signup_title);
        confirmPasswordLayout.setVisibility(View.VISIBLE);
        actionButton.setText(R.string.sign_up_button);
        switchPromptText.setText(R.string.prompt_have_account);
        switchLinkText.setText(R.string.link_sign_in);
    }
}