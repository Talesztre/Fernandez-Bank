package com.example.fernandezbank.cobrar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fernandezbank.Helper.FirebaseHelper;
import com.example.fernandezbank.Helper.GetMask;
import com.example.fernandezbank.Model.Cobranca;
import com.example.fernandezbank.Model.Extrato;
import com.example.fernandezbank.Model.Notificacao;
import com.example.fernandezbank.Model.Pagamento;
import com.example.fernandezbank.Model.Usuario;

import com.fernandezbank.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class PagarCobrancaActivity extends AppCompatActivity {

    private TextView textValor;
    private TextView textData;
    private TextView textUsuario;
    private ImageView imagemUsuario;
    private ProgressBar progressBar;

    private AlertDialog dialog;

    private Cobranca cobranca;
    private Notificacao notificacao;
    private Usuario usuarioDestino;
    private Usuario usuarioOrigem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagar_cobranca);

        configToolbar();

        iniciaComponentes();

        recuperaUsuarioOrigem();

        getExtra();

    }

    public void confirmarPagamento(View view){
        if(cobranca != null){
            if(!cobranca.isPaga()){
                if(usuarioDestino != null && usuarioOrigem != null){
                    if(usuarioOrigem.getSaldo() >= cobranca.getValor()){

                        usuarioOrigem.setSaldo(usuarioOrigem.getSaldo() - cobranca.getValor());
                        usuarioOrigem.atualizarSaldo();

                        usuarioDestino.setSaldo(usuarioDestino.getSaldo() + cobranca.getValor());
                        usuarioDestino.atualizarSaldo();

                        atualizarStatusCobranca();

                        // Salva no Extrato do usu??rio que enviou o pagamento
                        salvarExtrato(usuarioOrigem, "SAIDA");

                        // Salva no Extrato do usu??rio que recebeu o pagamento
                        salvarExtrato(usuarioDestino, "ENTRADA");

                    }else {
                        showDialog("Saldo insuficiente.");
                    }
                }else {
                    Toast.makeText(this, "Ainda estamos recuperando as informa????es.", Toast.LENGTH_SHORT).show();
                }
            }else {
                showDialog("O pagamento j?? foi realizado para esta cobran??a.");
            }
        }else {
            Toast.makeText(this, "Ainda estamos recuperando as informa????es.", Toast.LENGTH_SHORT).show();
        }
    }

    private void salvarExtrato(Usuario usuario, String tipo){

        Extrato extrato = new Extrato();
        extrato.setOperacao("PAGAMENTO");
        extrato.setValor(cobranca.getValor());
        extrato.setTipo(tipo);

        DatabaseReference extratoRef = FirebaseHelper.getDatabaseReference()
                .child("extratos")
                .child(usuario.getId())
                .child(extrato.getId());
        extratoRef.setValue(extrato).addOnCompleteListener(task -> {
            if(task.isSuccessful()){

                DatabaseReference updateExtrato = extratoRef
                        .child("data");
                updateExtrato.setValue(ServerValue.TIMESTAMP);

                salvarPagamento(extrato);

            }else {
                showDialog("N??o foi poss??vel efetuar o pagamento, tente mais tarde.");
            }
        });

    }

    private void salvarPagamento(Extrato extrato) {

        Pagamento pagamento = new Pagamento();
        pagamento.setId(extrato.getId());
        pagamento.setIdCobranca(cobranca.getId());
        pagamento.setValor(cobranca.getValor());
        pagamento.setIdUserDestino(usuarioDestino.getId());
        pagamento.setIdUserOrigem(usuarioOrigem.getId());

        DatabaseReference pagamentoRef = FirebaseHelper.getDatabaseReference()
                .child("pagamentos")
                .child(extrato.getId());
        pagamentoRef.setValue(pagamento).addOnSuccessListener(aVoid -> {

            DatabaseReference update = pagamentoRef.child("data");
            update.setValue(ServerValue.TIMESTAMP);

        });

        if(extrato.getTipo().equals("ENTRADA")){
            configNotificacao(extrato.getId());
        }else {
            Intent intent = new Intent(this, ReciboPagamentoActivity.class);
            intent.putExtra("idPagamento", pagamento.getId());
            startActivity(intent);
        }

    }

    private void atualizarStatusCobranca(){
        DatabaseReference cobrancaRef = FirebaseHelper.getDatabaseReference()
                .child("cobrancas")
                .child(FirebaseHelper.getIdFirebase())
                .child(notificacao.getIdOperacao())
                .child("paga");
        cobrancaRef.setValue(true);
    }

    private void getExtra(){
        notificacao = (Notificacao) getIntent().getSerializableExtra("notificacao");

        recuperaCobranca();
    }

    private void recuperaCobranca(){
        DatabaseReference cobrancaRef = FirebaseHelper.getDatabaseReference()
                .child("cobrancas")
                .child(FirebaseHelper.getIdFirebase())
                .child(notificacao.getIdOperacao());
        cobrancaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cobranca = snapshot.getValue(Cobranca.class);

                recuperaUsuarioDestino();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Configura a notifica????o
    private void configNotificacao(String idOperacao){
        Notificacao notificacao = new Notificacao();
        notificacao.setIdOperacao(idOperacao);
        notificacao.setIdDestinario(usuarioDestino.getId());
        notificacao.setIdEmitente(usuarioOrigem.getId());
        notificacao.setOperacao("PAGAMENTO");

        // Envia a notifica????o para o usu??rio que ir?? receber a cobran??a
        enviarNotificacao(notificacao);

    }

    // Envia a notifica????o para o usu??rio que ir?? receber o pagamento
    private void enviarNotificacao(Notificacao notificacao){
        DatabaseReference noficacaoRef = FirebaseHelper.getDatabaseReference()
                .child("notificacoes")
                .child(notificacao.getIdDestinario())
                .child(notificacao.getId());
        noficacaoRef.setValue(notificacao).addOnCompleteListener(task -> {
            if(task.isSuccessful()){

                DatabaseReference updateRef = noficacaoRef
                        .child("data");
                updateRef.setValue(ServerValue.TIMESTAMP);

            }else {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void showDialog(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(
                this, R.style.CustomAlertDialog
        );

        View view = getLayoutInflater().inflate(R.layout.layout_dialog_info, null);

        TextView textTitulo = view.findViewById(R.id.textTitulo);
        textTitulo.setText("Aten????o");

        TextView mensagem = view.findViewById(R.id.textMensagem);
        mensagem.setText(msg);

        Button btnOK = view.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(v -> dialog.dismiss());

        builder.setView(view);

        dialog = builder.create();
        dialog.show();

    }

    private void recuperaUsuarioDestino() {
        DatabaseReference usuarioRef = FirebaseHelper.getDatabaseReference()
                .child("usuarios")
                .child(cobranca.getIdEmitente());
        usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usuarioDestino = snapshot.getValue(Usuario.class);
                configDados();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void recuperaUsuarioOrigem() {
        DatabaseReference usuarioRef = FirebaseHelper.getDatabaseReference()
                .child("usuarios")
                .child(FirebaseHelper.getIdFirebase());
        usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usuarioOrigem = snapshot.getValue(Usuario.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void configDados(){
        textUsuario.setText(usuarioDestino.getNome());
        if(usuarioDestino.getUrlImagem() != null){
            Picasso.get().load(usuarioDestino.getUrlImagem())
                    .placeholder(R.drawable.loading)
                    .into(imagemUsuario);
        }

        textData.setText(GetMask.getDate(cobranca.getData(), 3));
        textValor.setText(getString(R.string.text_valor, GetMask.getValor(cobranca.getValor())));

    }

    private void configToolbar(){
        TextView textTitulo = findViewById(R.id.textTitulo);
        textTitulo.setText("Confirme os dados");

        findViewById(R.id.ibVoltar).setOnClickListener(v -> finish());
    }

    private void iniciaComponentes(){
        textValor = findViewById(R.id.textValor);
        textData = findViewById(R.id.textData);
        textUsuario = findViewById(R.id.textUsuario);
        imagemUsuario = findViewById(R.id.imagemUsuario);
        progressBar = findViewById(R.id.progressBar);
    }

}