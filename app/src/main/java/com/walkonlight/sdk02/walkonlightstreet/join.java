package com.walkonlight.sdk02.walkonlightstreet;


import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class join extends AppCompatActivity {

    private static String TAG = "phptest_MainActivity";
    TextView mTextViewResult;
    EditText user_phone;
    EditText user_name;
    EditText user_id;
    EditText user_pw;
    Button btn_join;
    Button btn_back;
    @Override

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        user_phone = (EditText)findViewById(R.id.user_phone);
        user_name = (EditText)findViewById(R.id.user_name);
        user_id = (EditText)findViewById(R.id.user_id);
        user_pw = (EditText)findViewById(R.id.user_pw);
        btn_join = (Button)findViewById(R.id.btn_join);
        btn_back = (Button)findViewById(R.id.btn_back);

        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = user_phone.getText().toString();
                String name = user_name.getText().toString();
                String id = user_id.getText().toString();
                String pw = user_pw.getText().toString();
                InsertData task = new InsertData();
                task.execute(phone,name,id,pw);
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(join.this, "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
            Log.d(TAG, "POST response - " + result);
        }

        @Override
        protected String doInBackground(String... params) {

            String phone = (String)params[0];     // phone, name, id, pw 받아오기
            String name = (String)params[1];
            String id = (String)params[2];
            String pw = (String)params[3];

            String serverURL = "https://sdk0213.000webhostapp.com/zyro/lightstreet/InsertUser.php";
            String postParameters = "phone=" + phone + "&name=" + name + "&id=" + id + "&pw=" + pw;     // 여기다가저장

            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                //httpURLConnection.setRequestProperty("content-type", "application/json");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));       // 여기서 쓰기
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString();


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }
}