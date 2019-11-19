package com.walkonlight.sdk02.walkonlightstreet;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class login extends AppCompatActivity {

    EditText edt_id;
    EditText edt_pw;
    Button btn_login;
    Button btn_join;
    TextView text;
    Intent intent;
    Intent intent_join;
    Boolean CHECK;
    String ID;

    private static String TAG = "phptest_MainActivity";

    private static final String TAG_JSON="webnautes";
    private static final String TAG_ID = "id";
    private static final String TAG_PW = "pw";

    String mJsonString;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edt_id = (EditText) findViewById(R.id.edt_id);
        edt_pw = (EditText) findViewById(R.id.edt_pw);
        btn_login = (Button) findViewById(R.id.btn_login);
        btn_join = (Button) findViewById(R.id.btn_join);
        text = (TextView) findViewById(R.id.text);

        intent = new Intent(this, MainActivity.class);
        intent_join  = new Intent(this, join.class);

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetData task = new GetData();               // 데이터 불러오는 쓰레드 실행
                task.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectUser.php");      // 실행
            }
        });

        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent_join);
            }
        });
    }

    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;                          // 로딩중입니다 뜨게하는 표시
        String errorString = null;
        @Override
        protected void onPreExecute() {                          // 초기화
            super.onPreExecute();
            progressDialog = ProgressDialog.show(login.this, "로그인중입니다.", null, true, true);   //로딩중입니다를 please wait로 바꿔서 외침
        }

        @Override
        protected void onPostExecute(String result) {           // 일처리 끝나고
            super.onPostExecute(result);
            progressDialog.dismiss();                       // 로딩중입니다. 끝내기
            Log.d(TAG, "response  - " + result);

            if (result == null){                                // 결과값이 null이라면 에러 스트링 보내기
                Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
            }
            else {
                mJsonString = result;                              // 결과값을 mJsonString 스트링에 저장
                showResult();
                if(CHECK == true){
                    Toast.makeText(getApplicationContext(), "로그인을 환영합니다.", Toast.LENGTH_LONG).show();
                    intent.putExtra("id",ID);
                    startActivity(intent);
                    finish();
                }
                else if(CHECK == false){
                    Toast.makeText(getApplicationContext(), "id pw 다시확인해주세요", Toast.LENGTH_LONG).show();
                }
            }
        }


        @Override
        protected String doInBackground(String... params) {          // 쓰레드 돌기 param에 URL 볼러오기

            String serverURL = params[0];               //  param에 URL 저장된거 불러오기


            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();     // url 에 연결하기


                httpURLConnection.setReadTimeout(5000);                         // 5초동안 못읽으면 타임아웃
                httpURLConnection.setConnectTimeout(5000);                      // 5초동안 못연결시 타임아웃
                httpURLConnection.connect();                                    // 연결


                int responseStatusCode = httpURLConnection.getResponseCode();          // url 연결 잘되어있는 확인하는 플레그?
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {       // url연결이 잘되었다면
                    inputStream = httpURLConnection.getInputStream();           // url에서 stream 가져오기
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();           // url에서 에러 가져오기
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");      // 가져온 inputstream 을 UTF-8 형식으로 저장하기
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);          //  가져온것들 버퍼에 저장하고

                StringBuilder sb = new StringBuilder();         // 문자열수정이 용이한 StringBuilder를 선언
                String line;

                while((line = bufferedReader.readLine()) != null){      // 버퍼리더에서 한라인씩 읽어서 null이 될때까지
                    sb.append(line);                                    // StringBuilder에 저장하기
                }


                bufferedReader.close();                         // 버퍼리더를 모두 썻으니 끝내기


                return sb.toString().trim();                // Stringbuilder에 저장된것을 string으로 바꾸고 맨앞 맨끝 공백 없애서 반환하기  이것이 result가 됨


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);
                errorString = e.toString();                 // 에러가 나면 에러를 스트링으로 바꾸고 에러에 저장

                return null;
            }

        }
    }


    private void showResult(){
        CHECK = false;
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);        // mJsonString은 읽어온 결과값이 저장된곳임 그것은 jsonObject를 사용하여 읽기 편하기 만들기
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);       // jsonarray는 jsonobject가 들어가는 배열을 뜻함

            for(int i=0;i<jsonArray.length();i++){      // 제이슨배열 길이만큼 반복하기

                JSONObject item = jsonArray.getJSONObject(i);       // 제이슨배열에서 오브젝트를 item에 저장하고

                String id = item.getString(TAG_ID);     // 아이템에서 TAG_ID "id" 부분
                String pw = item.getString(TAG_PW);     // 아이템에서 TAG_ID "pw" 부분

                if(edt_id.getText().toString().equals(id.toString())){
                    if(edt_pw.getText().toString().equals(pw.toString())){
                        CHECK = true;
                        ID = id;
                        break;
                    }
                }

                HashMap<String,String> hashMap = new HashMap<>();      // 해쉬맵 생성
                hashMap.put(TAG_ID, id);      // 해쉬맵에 "id" 부분에 id를 넣고
                hashMap.put(TAG_PW, pw);      // 해쉬맵에 "pw" 부분에 pw를 넣고
            }


        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }

    }

}