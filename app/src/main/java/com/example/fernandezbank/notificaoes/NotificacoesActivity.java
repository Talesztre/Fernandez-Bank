package com.example.fernandezbank.notificaoes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fernandezbank.Helper.FirebaseHelper;
import com.example.fernandezbank.Model.Notificacao;

import com.example.fernandezbank.adapter.NotificacaoAdapter;
import com.example.fernandezbank.cobrar.PagarCobrancaActivity;
import com.example.fernandezbank.transferencia.TransferenciaReciboActivity;
import com.fernandezbank.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.tsuryo.swipeablerv.SwipeLeftRightCallback;
import com.tsuryo.swipeablerv.SwipeableRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificacoesActivity extends AppCompatActivity implements NotificacaoAdapter.OnClick {

    private NotificacaoAdapter notificacaoAdapter;
    private final List<Notificacao> notificacaoList = new ArrayList<>();

    private SwipeableRecyclerView rvNotificacoes;
    private ProgressBar progressBar;
    private TextView textInfo;

    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificacoes);

        configToolbar();

        iniciaComponentes();

        configRv();

        recuperaNotificacoes();

    }

    private void configRv(){
        rvNotificacoes.setLayoutManager(new LinearLayoutManager(this));
        rvNotificacoes.setHasFixedSize(true);
        notificacaoAdapter = new NotificacaoAdapter(notificacaoList, getBaseContext(), this);
        rvNotificacoes.setAdapter(notificacaoAdapter);

        rvNotificacoes.setListener(new SwipeLeftRightCallback.Listener() {
            @Override
            public void onSwipedLeft(int position) {
                showDialogRemover(notificacaoList.get(position));
            }

            @Override
            public void onSwipedRight(int position) {
                showDialogStatusNotificacao(notificacaoList.get(position));
            }
        });
    }

    private void showDialogStatusNotificacao(Notificacao notificacao){
        AlertDialog.Builder builder = new AlertDialog.Builder(
                this, R.style.CustomAlertDialog
        );

        View view = getLayoutInflater().inflate(R.layout.layout_dialog, null);

        TextView textTitulo = view.findViewById(R.id.textTitulo);
        TextView textMensagem = view.findViewById(R.id.textMensagem);

        if(notificacao.isLida()){
            textTitulo.setText("Deseja marcar esta notifica????o como N??o lida ?");
            textMensagem.setText("Aperte em sim para marcar esta notifica????o como N??o lida ou aperte em fechar para cancelar.");
        }else {
            textTitulo.setText("Deseja marcar esta notifica????o como Lida ?");
            textMensagem.setText("Aperte em sim para marcar esta notifica????o como Lida ou aperte em fechar para cancelar.");
        }

        view.findViewById(R.id.btnOK).setOnClickListener(v -> {

            notificacao.salvar();

            dialog.dismiss();
        });

        view.findViewById(R.id.btnFechar).setOnClickListener(v -> {
            dialog.dismiss();
            notificacaoAdapter.notifyDataSetChanged();
        });

        builder.setView(view);

        dialog = builder.create();
        dialog.show();
    }

    private void showDialogRemover(Notificacao notificacao){
        AlertDialog.Builder builder = new AlertDialog.Builder(
                this, R.style.CustomAlertDialog
        );

        View view = getLayoutInflater().inflate(R.layout.layout_dialog, null);

        TextView textTitulo = view.findViewById(R.id.textTitulo);
        TextView textMensagem = view.findViewById(R.id.textMensagem);

        textTitulo.setText("Deseja remover a notifica????o ?");
        textMensagem.setText("Aperte em sim para remover esta notifica????o ou aperte em fechar para cancelar.");

        view.findViewById(R.id.btnOK).setOnClickListener(v -> {
            removerNotificacoes(notificacao);
            dialog.dismiss();
        });

        view.findViewById(R.id.btnFechar).setOnClickListener(v -> {
            dialog.dismiss();
            notificacaoAdapter.notifyDataSetChanged();
        });

        builder.setView(view);

        dialog = builder.create();
        dialog.show();
    }

    private void removerNotificacoes(Notificacao notificacao){
        DatabaseReference notificacaoRef = FirebaseHelper.getDatabaseReference()
                .child("notificacoes")
                .child(FirebaseHelper.getIdFirebase())
                .child(notificacao.getId());
        notificacaoRef.removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()){

                notificacaoList.remove(notificacao);

                if(notificacaoList.isEmpty()){
                    textInfo.setText("Nenhuma notifica????o recebida.");
                }else {
                    textInfo.setText("");
                }

                Toast.makeText(this, "Notifica????o removida com sucesso!", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "Erro ao remover a Notifica????o!", Toast.LENGTH_SHORT).show();
            }
            notificacaoAdapter.notifyDataSetChanged();
        });

    }

    private void recuperaNotificacoes(){
        DatabaseReference notificacaoRef = FirebaseHelper.getDatabaseReference()
                .child("notificacoes")
                .child(FirebaseHelper.getIdFirebase());
        notificacaoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    notificacaoList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()){
                        Notificacao notificacao = ds.getValue(Notificacao.class);
                        notificacaoList.add(notificacao);
                    }
                    textInfo.setText("");
                }else {
                    textInfo.setText("Voc?? n??o tem nenhuma notifica????o.");
                }

                Collections.reverse(notificacaoList);
                progressBar.setVisibility(View.GONE);
                notificacaoAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void configToolbar(){
        TextView textTitulo = findViewById(R.id.textTitulo);
        textTitulo.setText("Notifica????es");

        findViewById(R.id.ibVoltar).setOnClickListener(v -> finish());
    }

    private void iniciaComponentes() {
        progressBar = findViewById(R.id.progressBar);
        textInfo = findViewById(R.id.textInfo);
        rvNotificacoes = findViewById(R.id.rvNotificacoes);
    }

    @Override
    public void OnClickListener(Notificacao notificacao) {
        if(notificacao.getOperacao().equals("COBRANCA")){
            Intent intent = new Intent(this, PagarCobrancaActivity.class);
            intent.putExtra("notificacao", notificacao);
            startActivity(intent);
        }else if(notificacao.getOperacao().equals("TRANSFERENCIA")){
            Intent intent = new Intent(this, TransferenciaReciboActivity.class);
            intent.putExtra("idTransferencia", notificacao.getIdOperacao());
            startActivity(intent);
        }else {

        }
    }
}