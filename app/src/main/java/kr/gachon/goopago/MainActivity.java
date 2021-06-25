package kr.gachon.goopago;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipSession;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Target;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;

    private EditText translationText;
    private Button translationButton;
    private TextView resultText;
    private TextView resultText2;
    private String result;
    private ImageButton micButton;

    private Button papagoKeep;
    private Button googleKeep;
    private Button keepList;

    private Button papagoCopy;
    private Button googleCopy;
    private Button btnPapagoShare;
    private Button btnGoogleShare;

    DBHelper helper = new DBHelper(this);
    private Spinner source_spinner;
    private Spinner target_spinner;

    private String real_s_lang;                   // 어떤 언어로 작성한 문장을
    private String real_t_lang;                   // 어떤 언어로 번역이 되게 할지... 담는 언어 코드(ex.ko, ja, en) 변수들

    private Button btnPapagoTTS;
    private Button btnGoogleTTS;
    private TextView country_unvisible;          // getAddress 메서드로 얻은 국가 코드(2자리)를 담아놓기 위해 만듬; 실제 화면에 보일 일이 없음

    private int ttsLanguage;

    Intent i;
    SpeechRecognizer mRecognizer;
    final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    // 백 그라운드에서 파파고 API와 연결하여 번역 결과를 가져옵니다.
    class BackgroundTask extends AsyncTask<Integer, Integer, Integer> {
        protected void onPreExecute() {
        }

        @Override
        protected Integer doInBackground(Integer... arg0) {
            StringBuilder output = new StringBuilder();
            String clientId = "vuJK_b3Guf_elis1O5vj";
            String clientSecret = "qZG7FNy2lF";
            try {
                // 번역문을 UTF-8으로 인코딩합니다.
                String text = URLEncoder.encode(translationText.getText().toString(), "UTF-8");
                String apiURL = "https://openapi.naver.com/v1/papago/n2mt";

                // 파파고 API와 연결을 수행합니다.
                URL url = new URL(apiURL);  //URL객체 생성. URL객체의 매개변수가 "http://"포함하면, http연결을 위한 객체를 만듬.
                HttpURLConnection con = (HttpURLConnection) url.openConnection();   //URL객체의 openConnection메소드는 URLConnection객체를 반환함. HTTpURLConeection으로 형변환하자.
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id", clientId);  //이 메소드는, 브라우저->서버 전달되는 헤더에 들어가는 필드값.
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);

                // 번역할 문장을 파라미터로 전송합니다.
                String postParams = "source=" + real_s_lang + "&target=" + real_t_lang + "&text=" + text;              // source_language 와 target_language를 번역이 필요한 text와 같이 보냄
                // String postParams = "source=ko&target=en&text=" + text;  //queryString형식으로 보낸다.
                con.setDoOutput(true);   //이 객체(con)의 출력이 가능하게 함.
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());  //con은 서버가서, 데이터 get해올거임.
                wr.writeBytes(postParams);  //보낼 데이터
                wr.flush();
                wr.close();

                // 번역 결과를 받아옵니다.
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) { // 정상 호출
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {  // 에러 발생
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    output.append(inputLine);       //받아온 데이터를 output에 탈탈 넣어둠.
                }
                br.close();
            } catch (Exception ex) {
                Log.e("SampleHTTP", "Exception in processing response.", ex);
                ex.printStackTrace();
            }
            result = output.toString();     //받아온 데이터를 string형으로 전환까지 완료. 이제 Json 파싱하자.
            return null;
        }

        protected void onPostExecute(Integer a) {
            JsonParser parser = new JsonParser();       // JSON 문자열을 객체로 바꿔주는 Gson라이브러리에 있는
            JsonElement element = parser.parse(result);     //JsonParser, JsonElement란 도구를 이용해서 데이터를 가져오자
            if (element.getAsJsonObject().get("errormessage") != null) {         // getAsJsonObject 로 원하는 타입의 데이터 가져오자.
                Log.e("번역 오류", "번역 오류가 발생했습니다. " +                      // getAsJsonObject().get("name") 이면, 키가 "name"인 데이터 가져오는거임.
                        "[오류코드 : " + element.getAsJsonObject().get("errorCode").getAsString() + "]");
            } else if (element.getAsJsonObject().get("message") != null) {
                // 번역 결과 출력
                resultText.setText(element.getAsJsonObject().get("message").getAsJsonObject().get("result")
                        .getAsJsonObject().get("translatedText").getAsString());
            }
        }
    }


    class GoogleBackgroundTask extends AsyncTask<Integer, Integer, Integer> {
        private final static String GoogleApiURL = "https://translation.googleapis.com/language/translate/v2?key=";
        private final static String KEY = "AIzaSyAntVsP46GIoX_EIHeU6CYAqgVHmOgH8fM";
        private String TARGET = "&target=" + real_t_lang;                      // 문장이 real_t_lang 에 들어있는 값으로 번역이 됨
        private String SOURCE = "&source=" + real_s_lang;                     // 문장이 real_s_lang 에 들어있는 값으로 작성되었음
        private final static String QUERY = "&q=";


        private String targettext;
        String texting;

        @Override
        protected Integer doInBackground(Integer... arg0) {
            StringBuilder output = new StringBuilder();
            String KEY = "AIzaSyAntVsP46GIoX_EIHeU6CYAqgVHmOgH8fM";
            try {
                // 번역문을 UTF-8으로 인코딩합니다.
                String sourcetext = URLEncoder.encode(translationText.getText().toString(), "UTF-8");
                String GoogleApiURL = "https://translation.googleapis.com/language/translate/v2?key=";


                // 파파고 API와 연결을 수행합니다.
                URL googleurl = new URL(GoogleApiURL + KEY + SOURCE + TARGET + QUERY + sourcetext);  //URL객체 생성. URL객체의 매개변수가 "http://"포함하면, http연결을 위한 객체를 만듬.
                HttpURLConnection conn = (HttpURLConnection) googleurl.openConnection();   //URL객체의 openConnection메소드는 URLConnection객체를 반환함. HTTpURLConeection으로 형변환하자.

                BufferedReader bffr;
                if (conn.getResponseCode() == 200) { // 정상 호출
                    bffr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {  // 에러 발생
                    bffr = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                String line;
                while ((line = bffr.readLine()) != null) {
                    output.append(line);       //받아온 데이터를 output에 탈탈 넣어둠.
                }
                bffr.close();
            } catch (IOException ex) {
                Log.e("GoogleTranslatorError", ex.getMessage());
            }
            texting = output.toString();


            return null;
        }


        protected void onPostExecute(Integer a) {
            JsonParser googleparser = new JsonParser();
            JsonElement googleelement = googleparser.parse(texting);
            if (googleelement.isJsonObject()) {
                JsonObject obj = googleelement.getAsJsonObject();
                if (obj.get("error") == null) {
                    targettext = obj.get("data").getAsJsonObject().get("translations").getAsJsonArray().get(0).getAsJsonObject().get("translatedText").getAsString();

                    //JSON은 html상에 있었으므로, 특수문자(')등이 있으면, html에서 특수문자(')값은 &#39;로 매칭되서 표시된다.
                    // HTML코드를 디코딩 해줘야함.
                    // [org.apache.commons.lang3.StringEscapeUtils]  api가 그런 디코딩 변환과정을 지원해줌.
                    String HtmlEscaped_targettext = StringEscapeUtils.unescapeHtml4(targettext);

                    resultText2.setText(HtmlEscaped_targettext);


                }
            }
        }

    }

    public String getAddress(Context ctx, double latitude, double longitude) {       // 위도와 경도를 파라미터로 입력 받으면 해당 위치의
        String region_code = null;                                                     // 국가 코드 (2자리, KR, CN, JP, CA.. 등등) 얻어오는 메서드
        try {
            Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);


                region_code = address.getCountryCode();


            }
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }

        return region_code;
    }

    LocationManager manager;
    List<String> enabledProviders;
    float bestAccuracy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translationText = (EditText) findViewById(R.id.translationText);
        translationButton = (Button) findViewById(R.id.translationButton);
        resultText = (TextView) findViewById(R.id.resultText);
        resultText2 = (TextView) findViewById(R.id.resultText2);
        micButton = (ImageButton) findViewById(R.id.micButton);

        papagoKeep = (Button) findViewById(R.id.papagoKeep);
        googleKeep = (Button) findViewById(R.id.googleKeep);
        keepList = (Button) findViewById(R.id.keepList);
        btnPapagoShare = (Button) findViewById(R.id.btnPapagoShare);
        btnGoogleShare = (Button) findViewById(R.id.btnGoogleShare);

        papagoCopy = (Button) findViewById(R.id.papagoCopy);
        googleCopy = (Button) findViewById(R.id.googleCopy);

        source_spinner = (Spinner) findViewById(R.id.source_spinner);
        target_spinner = (Spinner) findViewById(R.id.target_spinner);

        country_unvisible = (TextView) findViewById(R.id.country_unvisible);

        btnPapagoTTS = (Button) findViewById(R.id.btnPapagoTTS);
        btnGoogleTTS = (Button) findViewById(R.id.btnGoogleTTS);
        tts = new TextToSpeech(this, this);

        //Locale systemLocale = getApplicationContext().getResources().getConfiguration().locale;
        //final String strCountry = systemLocale.getCountry();

        manager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {  // 권한 체크
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            getProviders();
            getLocation();
        }

        startActivity(new Intent(this, LoadingActivity.class)); // 앱실행 로딩화면

        String[] s_lang = {"한국어", "영어", "일본어", "중국어(간체)", "중국어(번체)", "베트남어", "태국어", "독일어", "스페인어", "이탈리아어", "프랑스어"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, s_lang);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        source_spinner.setAdapter(adapter);

        source_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] t_lang;

                if (parent.getItemAtPosition(position).equals("한국어")) {                                                                                   // 첫 번쨰 스피너에서 한국어가 선택되면
                    real_s_lang = "ko";                                                                                                                   // 작성될 문장은 한국어(ko) 이고
                    t_lang = new String[]{"영어", "일본어", "중국어(간체)", "중국어(번체)", "베트남어", "태국어", "독일어", "스페인어", "이탈리아어", "프랑스어"};    // 두 번째 스피너에 한국어에서 번역이 지원되는 언어(en, ja, zh-CH 등)만 뜬다.

                    //ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, t_lang);
                    //adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    //target_spinner.setAdapter(adapter2);
                    makeTarget_spinner(t_lang);

                    switch (country_unvisible.getText().toString()) {                  // GPS를 이용하여 해당 위치에 있는 나라를 알아내면 그 국가에 맞는 언어 코드를
                        // target_spinner의 디폴트 값으로 설정
                        case "KR":                                       // 우리나라에 있을 경우는  한국어 -> 영어로 설정
                        case "NZ":
                        case "GB":
                        case "AU":
                        case "CA":
                        case "PH":
                        case "HK":           // 뉴질랜드, 영국, 오스트레일리아, 캐나다, 필리핀, 홍콩은 영어(en)를 기본 언어로 설정
                            target_spinner.setSelection(0);
                            break;
                        case "JP":
                            target_spinner.setSelection(1);
                            break;
                        case "CN":                                             // 중국은 중국어(간체)
                            target_spinner.setSelection(2);
                            break;
                        case "TW":                                              // 대만은 중국어(번체)
                            target_spinner.setSelection(3);
                            break;
                        case "VN":
                            target_spinner.setSelection(4);
                            break;
                        case "TH":
                            target_spinner.setSelection(5);
                            break;
                        case "DE":
                        case "AT":
                        case "CH":
                        case "LI":
                        case "LU":        // 독일, 오스트리아, 스위스, 리히텐슈타인, 룩셈부르크는 독일어
                            target_spinner.setSelection(6);
                            break;
                        case "ES":
                        case "AR":
                        case "CO":
                        case "CL":
                        case "PY":
                        case "CR":
                        case "PA":
                        case "UY":
                            target_spinner.setSelection(7);
                            break;
                        case "IT":
                        case "VA":
                        case "SM":
                            target_spinner.setSelection(8);
                            break;
                        case "FR":
                        case "MC":
                        case "BE":
                            target_spinner.setSelection(9);
                            break;
                        default:
                            target_spinner.setSelection(0);              // gps 이용안할 시 디폴트는 영어(en)
                    }
                } else if (parent.getItemAtPosition(position).equals("영어")) {
                    real_s_lang = "en";
                    t_lang = new String[]{"한국어", "일본어", "중국어(간체)", "중국어(번체)"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("일본어")) {
                    real_s_lang = "ja";
                    t_lang = new String[]{"한국어", "영어", "중국어(간체)", "중국어(번체)"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("중국어(간체)")) {
                    real_s_lang = "zh-CN";
                    t_lang = new String[]{"한국어", "영어", "일본어", "중국어(번체)"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("중국어(번체)")) {
                    real_s_lang = "zh-TW";
                    t_lang = new String[]{"한국어", "영어", "일본어", "중국어(간체)"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("베트남어")) {
                    real_s_lang = "vi";
                    t_lang = new String[]{"한국어"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("태국어")) {
                    real_s_lang = "th";
                    t_lang = new String[]{"한국어"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("독일어")) {
                    real_s_lang = "de";
                    t_lang = new String[]{"한국어"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("스페인어")) {
                    real_s_lang = "es";
                    t_lang = new String[]{"한국어"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("이탈리아어")) {
                    real_s_lang = "it";
                    t_lang = new String[]{"한국어"};
                    makeTarget_spinner(t_lang);
                } else if (parent.getItemAtPosition(position).equals("프랑스어")) {
                    real_s_lang = "fr";
                    t_lang = new String[]{"한국어"};
                    makeTarget_spinner(t_lang);
                } else {
                    t_lang = new String[]{"한국어"};
                    makeTarget_spinner(t_lang);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        target_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position).equals("한국어")) {
                    real_t_lang = "ko";
                    ttsLanguage = tts.setLanguage(Locale.KOREA);
                } else if (parent.getItemAtPosition(position).equals("영어")) {
                    real_t_lang = "en";
                    ttsLanguage = tts.setLanguage(Locale.US);
                } else if (parent.getItemAtPosition(position).equals("일본어")) {
                    real_t_lang = "ja";
                    ttsLanguage = tts.setLanguage(Locale.JAPAN);
                } else if (parent.getItemAtPosition(position).equals("중국어(간체)")) {
                    real_t_lang = "zh-CN";
                    ttsLanguage = tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
                } else if (parent.getItemAtPosition(position).equals("중국어(번체)")) {
                    real_t_lang = "zh-TW";
                    ttsLanguage = tts.setLanguage(Locale.TRADITIONAL_CHINESE);
                } else if (parent.getItemAtPosition(position).equals("베트남어")) {
                    real_t_lang = "vi";
                    ttsLanguage = tts.setLanguage(new Locale("vi"));
                } else if (parent.getItemAtPosition(position).equals("태국어")) {
                    real_t_lang = "th";
                    ttsLanguage = tts.setLanguage(new Locale("th"));
                } else if (parent.getItemAtPosition(position).equals("독일어")) {
                    real_t_lang = "de";
                    ttsLanguage = tts.setLanguage(Locale.GERMANY);
                } else if (parent.getItemAtPosition(position).equals("스페인어")) {
                    real_t_lang = "es";
                    ttsLanguage = tts.setLanguage(new Locale("es"));
                } else if (parent.getItemAtPosition(position).equals("이탈리아어")) {
                    real_t_lang = "it";
                    ttsLanguage = tts.setLanguage(Locale.ITALY);
                } else if (parent.getItemAtPosition(position).equals("프랑스어")) {
                    real_t_lang = "fr";
                    ttsLanguage = tts.setLanguage(Locale.FRANCE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        translationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BackgroundTask().execute();
                new GoogleBackgroundTask().execute();

            }
        });

        //파파고 번역문 저장 버튼
        papagoKeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (resultText.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "저장할 문장이 없습니다", Toast.LENGTH_SHORT).show();
                } else {
                    String beforeText = translationText.getText().toString();           // 번역 전 문장을 받아옴
                    String afterText = resultText.getText().toString();                  // 파파고의 번역 후 문장을 받아옴
                    String whatapi = "papago";

                    SQLiteDatabase db = helper.getWritableDatabase();
                    db.execSQL("insert into sentence (beforeText, afterText, whatapi) values (?,?,?);",       // DB의 sentence 테이블에 데이터 입력
                            new String[]{beforeText, afterText, whatapi});
                    db.close();

                    Toast.makeText(MainActivity.this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //구글 번역문 저장 버튼
        googleKeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (resultText2.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "저장할 문장이 없습니다", Toast.LENGTH_SHORT).show();
                } else {
                    String beforeText = translationText.getText().toString();          // 번역 전 문장을 받아옴
                    String afterText = resultText2.getText().toString();               // 구글의 번역 후 ?문장을 받아옴
                    String whatapi = "google";

                    SQLiteDatabase db = helper.getWritableDatabase();
                    db.execSQL("insert into sentence (beforeText, afterText, whatapi) values (?,?,?);",       // DB의 sentence 테이블에 데이터 입력
                            new String[]{beforeText, afterText, whatapi});
                    db.close();

                    Toast.makeText(MainActivity.this, "저장되었습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //저장된 문장 리스트 확인 가능한 버튼
        keepList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ReadDBActivity.class);
                startActivity(intent);
            }
        });

        // 구글 번역 결과 클립보드에 복사
        papagoCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("label", resultText.getText().toString());
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(getApplication(), "클립보드에 복사되었습니다.", Toast.LENGTH_LONG).show();
            }
        });

        // 파파고 번역 결과 클립보드에 복사
        googleCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("label", resultText2.getText().toString());
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(getApplication(), "클립보드에 복사되었습니다.", Toast.LENGTH_LONG).show();
            }
        });

        btnPapagoShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent msg = new Intent(Intent.ACTION_SEND);

                msg.addCategory(Intent.CATEGORY_DEFAULT);
                msg.putExtra(Intent.EXTRA_SUBJECT, "Goopago");
                msg.putExtra(Intent.EXTRA_TEXT, "\n[번역 요청 문구]\n" + translationText.getText().toString() + "\n" + "[Papago 번역 결과]\n" + resultText2.getText().toString());
                msg.putExtra(Intent.EXTRA_TITLE, "[Papago 번역 공유]");
                msg.setType("text/plain");

                startActivity(Intent.createChooser(msg, "공유"));
            }
        });

        btnGoogleShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent msg = new Intent(Intent.ACTION_SEND);

                msg.addCategory(Intent.CATEGORY_DEFAULT);
                msg.putExtra(Intent.EXTRA_SUBJECT, "Goopago");
                msg.putExtra(Intent.EXTRA_TEXT, "\n[번역 요청 문구]\n" + translationText.getText().toString() + "\n" + "[Google 번역 결과]\n" + resultText.getText().toString());
                msg.putExtra(Intent.EXTRA_TITLE, "[Google 번역 공유]");
                msg.setType("text/plain");

                startActivity(Intent.createChooser(msg, "공유"));
            }
        });

        btnPapagoTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakOutPapago();
            }
        });
        btnGoogleTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakOutGoogle();
            }
        });

        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        // i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,country_unvisible.getText().toString());
        mRecognizer=SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        mRecognizer.setRecognitionListener(listener);

        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
                } else {
                    // 권한을 허용한 경우
                    try {
                        mRecognizer.startListening(i);
                    } catch(SecurityException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    //앱 종료시 tts를 같이 종료
    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            if (ttsLanguage == TextToSpeech.LANG_MISSING_DATA || ttsLanguage == TextToSpeech.LANG_NOT_SUPPORTED) {
                /*
                AlertDialog alertDialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("설정 알림");
                builder.setMessage("[Google TTS 엔진]을 설정해야 듣기 기능이 정상 작동합니다.");
                builder.setPositiveButton("확인", dialogListener);
                alertDialog=builder.create();
                alertDialog.show();
                 */

                Toast.makeText(this, "[Google TTS 엔진]을 설정해야 듣기 기능이 정상 작동합니다.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivityForResult(intent, 0);
                /*
                Intent install = new Intent();
                install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(install);
                */
            } else {
                btnPapagoTTS.setEnabled(true);
                btnGoogleTTS.setEnabled(true);
            }
        } else {
            Toast.makeText(this, "TTS 실패!", Toast.LENGTH_SHORT).show();
        }
    }

/*
    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivityForResult(intent, 0);
        }
    };
 */

    private void speakOutPapago() {
        String text = resultText.getText().toString();
        //tts.setPitch((float) 0.1); //음량
        //tts.setSpeechRate((float) 0.5); //재생속도
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void speakOutGoogle() {
        String text = resultText2.getText().toString();
        //tts.setPitch((float) 0.1); //음량
        //tts.setSpeechRate((float) 0.5); //재생속도
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void showToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void makeTarget_spinner(String[] lang) {
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, lang);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        target_spinner.setAdapter(adapter2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getProviders();
                getLocation();
            } else {
                showToast("no permission...");
            }
        }
    }

    private void getProviders() {

        String result = "All Providers :";
        List<String> providers = manager.getAllProviders();
        for (String provider : providers) {
            result += provider + ",";
        }

        result = "Enabled Providers : ";
        enabledProviders = manager.getProviders(true);
        for (String provider : enabledProviders) {
            result += provider + ",";
        }

    }

    private void getLocation() {

        for (String provider : enabledProviders) {
            Location location = null;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                location = manager.getLastKnownLocation(provider);
            } else {
                showToast("no permission");
            }
            if (location != null) {
                float accuracy = location.getAccuracy();
                if (bestAccuracy == 0) {
                    bestAccuracy = accuracy;
                    setLocationInfo(provider, location);
                } else {
                    if (accuracy < bestAccuracy) {
                        bestAccuracy = accuracy;
                        setLocationInfo(provider, location);
                    }
                }
            }
        }

    }

    private void setLocationInfo(String provider, Location location) {
        if (location != null) {
            String country = getAddress(this, location.getLatitude(), location.getLongitude());    // 위도, 경도를 이용하여 국가 코드(2자리)를 getAddress 메서드로 얻어와서
            country_unvisible.setText(country);                                                       // country 변수에 저장하고 이를 country_unvisible 텍스트뷰에 저장하면
            // 이를 이후 oncreate 클래스에서 getText().toString()하여 사용...
        } else {
            showToast("location null");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch(requestCode) {
            case 0 :
                //ActivityCompat.finishAffinity(this);
                finish();
                startActivity(new Intent(this, MainActivity.class));
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(),"음성인식을 시작합니다.",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }

            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message,Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줍니다.
            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for(int i = 0; i < matches.size() ; i++){
                translationText.setText(matches.get(i));
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };
}
