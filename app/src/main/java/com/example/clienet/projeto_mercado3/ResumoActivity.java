package com.example.clienet.projeto_mercado3;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class ResumoActivity extends AppCompatActivity {

    Cliente cliente;
    LocalEntrega localEntrega = new LocalEntrega();
    Carrinho carrinho = new Carrinho();
    Mercado mercado = new Mercado();

    SQLiteDatabase mDb;
    SQLiteDatabaseDao dao;

    ProgressDialog mProgressDialog;
    ProgressDialog mProgressDialog2;

    Intent intent;

    TextView endereco;
    TextView bairro;
    TextView cidade;
    TextView destinatario;
    TextView valor_total;

    Button finalizar;

    ScrollView sv;

    View header;

    ListView list;

    ArrayList<HashMap<String, Object>> listData;

    ArrayList<HashMap<String, Object>> listdatasqlite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resumo);

        intent = getIntent();
        Bundle bundle = intent.getExtras();
        cliente = (Cliente)bundle.getSerializable("cliente");

        endereco = (TextView) findViewById(R.id.endereco);

        bairro = (TextView) findViewById(R.id.bairro);

        cidade = (TextView) findViewById(R.id.cidade);

        destinatario = (TextView) findViewById(R.id.destinatario);

        valor_total = findViewById(R.id.valor_total);

        list = (ListView) findViewById(R.id.list_items);

        sv = (ScrollView) findViewById(R.id.scrollView);

        finalizar = findViewById(R.id.finalizar);

        finalizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ResumoActivity.this, FormaPagamentoActivity.class);

                intent.putExtra("cliente", cliente);

                startActivity(intent);

            }
        });

        getSupportActionBar().setSubtitle("Login: "+cliente.getEmail());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dao = new SQLiteDatabaseDao();

        new FetchSQL2().execute();

    }

    @Override
    public void onBackPressed()
    {

        Intent intent = new Intent(ResumoActivity.this, LocalEntregaActivity.class);

        intent.putExtra("cliente", cliente);

        startActivity(intent);

    }

    @Override
    public boolean onSupportNavigateUp(){
        Intent intent = new Intent(ResumoActivity.this, LocalEntregaActivity.class);

        intent.putExtra("cliente", cliente);

        startActivity(intent);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.logout:

                this.deleteDatabase("/data/data/com.example.clienet.projeto_mercado3/databases/Carrinhos.db");

                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();

                editor.clear();
                editor.commit();

                Intent intent = new Intent(ResumoActivity.this, MainActivity.class);

                finishAffinity();

                startActivity(intent);

                return true;

            case R.id.seus_pedidos:

                Intent intent_pedidos = new Intent(ResumoActivity.this, PedidosActivity.class);

                Bundle bundle = new Bundle();

                bundle.putSerializable("cliente", cliente);

                intent_pedidos.putExtras(bundle);

                startActivity(intent_pedidos);

                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class FetchSQL extends AsyncTask<Void,Void,String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            mProgressDialog = new ProgressDialog(ResumoActivity.this);

            mProgressDialog.setTitle("Por Favor Aguarde");
            // Set progressdialog message
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            // Show progressdialog
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            String senha = "";

            listData = new ArrayList<HashMap<String, Object>>();

            ArrayList idprodutos = new ArrayList();
            ArrayList quantidades = new ArrayList();
            float subtotal_produtos = 0;
            int quantidade_total = 0;

            for(int i=0; i<listdatasqlite.size(); i++){
                idprodutos.add(listdatasqlite.get(i).get("idproduto"));
                quantidades.add(listdatasqlite.get(i).get("quantidade"));
            }

            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(getString(R.string.ipnovo)+"carrinho");

                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setReadTimeout(30000 /* milliseconds */);
                urlConnection.setConnectTimeout(30000 /* milliseconds */);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty ("Authorization", "Bearer "+cliente.getToken());
                urlConnection.setRequestProperty ("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty ("Accept", "application/json");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("idprodutos", idprodutos.toString().replace("[", "").replace("]", ""))
                        .appendQueryParameter("idmercado", String.valueOf(cliente.getIdmercado()));
                String query = builder.build().getEncodedQuery();

                // Open connection for sending data
                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
                urlConnection.connect();

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return "exception";
            }

            try {

                int response_code = urlConnection.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    InputStream in = urlConnection.getInputStream();

                    InputStreamReader isw = new InputStreamReader(in);

                    StringBuilder sb = new StringBuilder();

                    String line;

                    BufferedReader br = new BufferedReader(isw);

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    Log.e("aaaaaaaaaaaaaaaaaaaaaa",""+sb);

                    JSONObject json = new JSONObject(sb.toString());

                    JSONArray jsonArray = new JSONArray(json.getString("rows"));

                    for(int i=0; i<jsonArray.length(); i++){
                        JSONObject json_data = jsonArray.getJSONObject(i);

                        HashMap<String, Object> meMap = new HashMap<String, Object>();
                        meMap.put("idproduto", json_data.getInt("idproduto"));
                        meMap.put("nome", json_data.getString("nome")+" "+json_data.getString("marca")+" " +json_data.getString("quantidade_unidade")+" "+json_data.getString("unidade"));
                        meMap.put("foto", getString(R.string.ip)+getString(R.string.endereco_imagem)+json_data.getString("foto"));
                        meMap.put("preco", json_data.getString("preco"));
                        meMap.put("estoque", json_data.getInt("estoque"));
                        meMap.put("quantidade", quantidades.get(i));
                        float qtd = Float.parseFloat(String.valueOf(quantidades.get(i)));
                        float preco = Float.parseFloat(json_data.getString("preco"));
                        subtotal_produtos = subtotal_produtos + (qtd*preco);
                        meMap.put("subtotal", String.valueOf(new DecimalFormat("######0.00").format(preco * qtd)));
                        listData.add(meMap);
                        quantidade_total = quantidade_total + (int) qtd;

                    }

                    carrinho.setSubtotal(subtotal_produtos);
                    carrinho.setQuantidade_total(quantidade_total);

                    jsonArray = new JSONArray(json.getString("rows2"));

                    for(int i=0; i<jsonArray.length(); i++) {
                        JSONObject json_data = jsonArray.getJSONObject(i);
                        mercado.setNome_mercado(json_data.getString("nome"));
                        mercado.setFoto_mercado(json_data.getString("foto"));
                        mercado.setBairro_mercado(json_data.getString("bairro"));
                        carrinho.setPreco_entrega(Float.parseFloat(json_data.getString("preco_entrega")));
                        cliente.setIdcidade_carrinho(json_data.getInt("cidades_idcidade"));
                    }

                }else{

                    return("unsuccessful");
                }

            }
            catch(Exception e){
                Log.e("JSON", String.valueOf("aaaaaaaaaaaaaaaa"));
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return senha;
        }
        @Override
        protected void onPostExecute(String value) {
            //

            Log.e("JSON", String.valueOf("aaaaaaaaaaaaaaaa"+mercado.getNome_mercado()));

            header = getLayoutInflater().inflate(R.layout.listview_header2,null);

            ((TextView) header.findViewById(R.id.preco_entrega)).setText("Preço da Entrega: R$ "+carrinho.getPreco_entrega());
            ((TextView) header.findViewById(R.id.mercado)).setText(mercado.getNome_mercado());
            ((TextView) header.findViewById(R.id.bairro)).setText(mercado.getBairro_mercado());
            ((TextView) header.findViewById(R.id.numero_de_itens)).setText(Integer.toString(carrinho.getQuantidade_total())+" itens");

            carrinho.setTotal(carrinho.getSubtotal()+carrinho.getPreco_entrega());

            ((TextView) header.findViewById(R.id.total)).setText("Total: R$ "+String.valueOf(new DecimalFormat("######0.00").format(carrinho.getTotal())));

            valor_total.setText("Valor Total: R$ "+String.valueOf(new DecimalFormat("######0.00").format(carrinho.getTotal())));

            list.addHeaderView(header);


            ListAdapter adapter =
                    new MyAdapter5(
                            ResumoActivity.this,
                            listData,
                            R.layout.carrinho2,
                            new String[]{"nome", "preco", "subtotal"},
                            new int[]{R.id.nome, R.id.valor_unitario, R.id.subtotal},
                            intent);
            list.setAdapter(adapter);

            //adapter.notifyAll();

            mProgressDialog.dismiss();

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            sv.smoothScrollTo(0, 0);

        }
    }

    private class FetchSQL2 extends AsyncTask<Void,Void,String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            mProgressDialog2 = new ProgressDialog(ResumoActivity.this);

            mProgressDialog2.setTitle("Por Favor Aguarde");
            // Set progressdialog message
            mProgressDialog2.setMessage("Loading...");
            mProgressDialog2.setIndeterminate(false);
            // Show progressdialog
            mProgressDialog2.show();
        }

        @Override
        protected String doInBackground(Void... params) {

            URL url;
            HttpURLConnection urlConnection = null;
            String result = null;

            try {

                url = new URL(getString(R.string.ipnovo)+"endereco");

                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setReadTimeout(30000 /* milliseconds */);
                urlConnection.setConnectTimeout(30000 /* milliseconds */);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty ("Authorization", "Bearer "+cliente.getToken());
                urlConnection.setRequestProperty ("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty ("Accept", "application/json");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("idendereco", String.valueOf(cliente.getIdendereco()));
                String query = builder.build().getEncodedQuery();

                // Open connection for sending data
                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
                urlConnection.connect();

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return "exception";
            }

            try {

                int response_code = urlConnection.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    InputStream in = urlConnection.getInputStream();

                    InputStreamReader isw = new InputStreamReader(in);

                    StringBuilder sb = new StringBuilder();

                    String line;

                    BufferedReader br = new BufferedReader(isw);

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    Log.e("add", "rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr"+sb.toString());

                    JSONObject json = new JSONObject(sb.toString());

                    JSONArray jsonArray = new JSONArray(json.getString("rows"));

                    JSONObject json_data = jsonArray.getJSONObject(0);

                    localEntrega.setEndereco(json_data.getString("endereco"));
                    localEntrega.setNumero(json_data.getString("numero"));
                    localEntrega.setComplemento(json_data.getString("complemento"));
                    localEntrega.setBairro(json_data.getString("bairro"));
                    localEntrega.setDestinatario(json_data.getString("nome_destinatario"));

                    jsonArray = new JSONArray(json.getString("rows2"));

                    json_data = jsonArray.getJSONObject(0);

                    localEntrega.setIdcidade(json_data.getInt("idcidade"));
                    //cliente.setIdcidade_carrinho(json_data.getInt("idcidade"));
                    localEntrega.setNome_cidade(json_data.getString("nome"));

                }else{
                    return("unsuccessful");
                }

            }
            catch(Exception e){
                Log.e("JSON", String.valueOf("aaaaaaaaaaaaaaaa"));
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return result;
        }
        @Override
        protected void onPostExecute(String value) {
            //

            endereco.setText(localEntrega.getEndereco()+", "+localEntrega.getNumero()+", "+localEntrega.getComplemento());
            bairro.setText("Bairro: "+localEntrega.getBairro());
            cidade.setText("Cidade: "+localEntrega.getNome_cidade());
            destinatario.setText("Destinatario: "+localEntrega.getDestinatario());

            mProgressDialog2.dismiss();

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        }
    }

    class SQLiteDatabaseDao {

        @SuppressLint("WrongConstant")
        public SQLiteDatabaseDao() {
            mDb = openOrCreateDatabase("Carrinhos.db",
                    SQLiteDatabase.CREATE_IF_NECESSARY, null);

            getAllData("produto");

            new ResumoActivity.FetchSQL().execute();

        }

        public void getAllData(String table) {
            Cursor c = mDb.rawQuery("select * from " + table + " where mercado_idmercado='"+ cliente.getIdmercado() +"'", null);
            //int columnsSize = c.getColumnCount();
            listdatasqlite = new ArrayList<HashMap<String, Object>>();
            // 获取表的内容
            while (c.moveToNext()) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                //for (int i = 0; i < columnsSize; i++) {
                map.put("id", c.getInt(0));
                map.put("idproduto", c.getInt(1));
                map.put("quantidade", c.getInt(2));
                //}
                listdatasqlite.add(map);
            }
        }
    }
}
