package com.example.appfernandezbank.Autenticacao;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appfernandezbank.Helper.FirebaseHelper;
import com.example.appfernandezbank.app.MainActivity;
import com.fernandezbank.R;

import java.util.Objects;


public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail;
    private EditText edtSenha;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        iniciaComponentes();

    }

    public void validaDados(View view) {
        String email = edtEmail.getText().toString();
        String senha = edtSenha.getText().toString();

        if (!email.isEmpty()) {
            if (!senha.isEmpty()) {

                ocultarTeclado();

                progressBar.setVisibility(View.VISIBLE);

                logar(email, senha);

            } else {
                edtSenha.requestFocus();
                edtSenha.setError("Informe sua senha.");
            }
        } else {
            edtEmail.requestFocus();
            edtEmail.setError("Informe seu email.");
        }

    }



    public void Esqueceusuasenha(View view) {
        startActivity(new Intent(this, RecuperarContaActivity.class));
    }

    private void logar(String email, String senha) {
        FirebaseHelper.getAuth().signInWithEmailAndPassword(
                email, senha
        ).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                finish();
                startActivity(new Intent(this, MainActivity.class));
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Log.i("INFOTESTE", "logar: " + e.getMessage()));
    }

    private void iniciaComponentes() {
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        progressBar = findViewById(R.id.progressBar);
    }

    // Oculta o teclado do dispositivo
    private void ocultarTeclado() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(edtEmail.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

}