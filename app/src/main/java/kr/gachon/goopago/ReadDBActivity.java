package kr.gachon.goopago;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;

public class ReadDBActivity extends AppCompatActivity {

    LinearLayout layout2;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_db);

        layout2 = (LinearLayout) findViewById(R.id.layout2);          // activity_read_db의  스크롤뷰 바로 밑 리니어 레이아웃 (여러 위젯 담기 위함)

        DBHelper helper = new DBHelper(this);
        final SQLiteDatabase db = helper.getWritableDatabase();
        final Cursor cursor = db.rawQuery("select * from sentence", null);
        while (cursor.moveToNext()){                                                             // 메인 화면에서 저장 버튼을 클릭 할 떄 마다
            LinearLayout.LayoutParams innerparams = new LinearLayout.LayoutParams(                // 저장할 문장과 그 번역문, 삭제 버튼을 한 리니어 레이아웃에 담는다.
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            innerparams.bottomMargin = 10;
            innerparams.topMargin = 10;
            final LinearLayout innerlayout = new LinearLayout(this);
            innerlayout.setPadding(30,30,30,30);
            innerlayout.setOrientation((LinearLayout.HORIZONTAL));                           // 번역 전,후 문장과 삭제 버튼 배치를 가로로 하기 위해 horizontal
            innerlayout.setBackgroundResource(R.drawable.layout_border);                    // 레이아웃 간 구분을 보기 쉽게 하기 위함
            innerlayout.setLayoutParams(innerparams);
            layout2.addView(innerlayout);

            LinearLayout.LayoutParams innerViewparams = new LinearLayout.LayoutParams(          // 번역 전,후 문장을 담는 레이아웃
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
            );
            innerViewparams.rightMargin = 5;
            LinearLayout innerViewlayout = new LinearLayout(this);
            innerViewlayout.setOrientation(LinearLayout.VERTICAL);
            innerViewlayout.setLayoutParams(innerViewparams);

            LinearLayout.LayoutParams innerDeleteparams = new LinearLayout.LayoutParams(            // 삭제 버튼을 담는 레이아웃
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 4
            );
            LinearLayout innerDeletelayout = new LinearLayout(this);
            innerDeletelayout.setOrientation(LinearLayout.HORIZONTAL);
            innerDeletelayout.setGravity(Gravity.CENTER_VERTICAL);
            innerDeletelayout.setLayoutParams(innerDeleteparams);

            innerlayout.addView(innerViewlayout);
            innerlayout.addView(innerDeletelayout);

            TextView whatapi = new TextView(this);
            TextView textview1 =  new TextView(this);                         // 번역 전 문장을 담아 보여줄 텍스트뷰
            final TextView textview2 = new TextView(this);                         // 번역 후 문장을 담아 보여줄 텍스트뷰
            Button deleteBtn = new Button(this);                             // 필요없는 저장 리스트 항목 삭제 버튼
            deleteBtn.setText("삭제");

            innerViewlayout.addView(whatapi);
            innerViewlayout.addView(textview1);
            innerViewlayout.addView(textview2);
            innerDeletelayout.addView(deleteBtn);

            if (cursor.getString(3).equals("papago")){                     // 파파고 or 구글, 어떤 api를 사용했는지
                whatapi.setText("[파파고]");
            }
            else
                whatapi.setText("[구글]");

            textview1.setText(cursor.getString(1));
            textview1.setTypeface(null, Typeface.BOLD);                                  // 번역 전 문장을 진하게 표시
            textview2.setText(cursor.getString(2));

            final String id = cursor.getString(0);
            deleteBtn.setOnClickListener(new View.OnClickListener() {             // 삭제 버튼을 누르면 선택한 데이터를 DB에서 삭제함
                @Override
                public void onClick(View v) {
                    db.execSQL("delete from sentence where _id =" + id);
                    //db.execSQL("delete from sentence where afterText =" + "'" + textview2.getText().toString() + "'");
                    innerlayout.setVisibility(View.GONE);
                }
            });
        }


        // Apache POI를 이용한 엑셀파일 만들기
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toast.makeText(ReadDBActivity.this, "(test) 엑셀 파일 생성중..", Toast.LENGTH_SHORT).show();
                boolean Check = isExternalStorageWritable();
                String canWritable = String.valueOf(Check);

                // 외부저장소(external storage)에 접근할 수 있어야함. 거기다 파일을 둘려구요.
                if (!Check) {
                    // 외부저장소(external storage)에 파일을 write할 수 없을경우 실행.
                    // (여기서 외부저장소란, sd카드같은 보조기억장치개념의 외부저장소가 아님.)
                    // (앱 시스템적인 구성요소 밖의 영역이 "외부저장소"임)
                    // (따라서 그냥 단말기 자체의 저장공간에 저장됨(사용자가 설정해두었다면, sd카드에 저장될 수도 있음))
                    Toast.makeText(getApplicationContext(),
                            "Writable한가요? : " + canWritable + "=====> external storage에 파일을 write할 수 없단다. permission을 열어라 ",
                            Toast.LENGTH_LONG).show();

                } else {
                    try {
                        // 예제) txt파일을 외부저장소에 저장하자.
                                        /* (((      txt파일을 /sdcard/Android/data/[패키지명]/files/[testDirectory(내가 만듬)]/[sample.txt(내가 만듬)] 에 저장.     )))

                                            -// [testDirectory] 디렉토리를 만듭니다.
                                                    File file = new File(getExternalFilesDir(null),"testDirectory");
                                                    if(!file.exists()) {
                                                        file.mkdir();
                                                    }


                                            // [sample.txt] 파일을 만들겠습니다.
                                                    File txtfile = new File(file, "sample.txt");
                                                    FileOutputStream fileOut = new FileOutputStream(txtfile);
                                                    fileOut.flush();
                                                    fileOut.close();

                                        txt파일을 외부저장소(sdcard)에 저장하는 예제 끝*/


                        // excel 파일 만든다
                        String sheetName = "Goopago_Saved_Translation";//name of sheet

                        XSSFWorkbook wb = new XSSFWorkbook();
                        XSSFSheet sheet = wb.createSheet(sheetName);

                        //안내문
                        XSSFRow row = sheet.createRow(0);
                        row.createCell(0).setCellValue("[파파고/구글]");
                        row.createCell(1).setCellValue("(원문)");
                        row.createCell(2).setCellValue("--->");
                        row.createCell(3).setCellValue("(결과문)");


                        Cursor cursor = db.rawQuery("select * from sentence", null);
                        // cursor.moveToFirst();
                        int i = 1;
                        while (cursor.moveToNext()) {
                            row = sheet.createRow(i++);
                            row.createCell(0).setCellValue("[" + cursor.getString(3) + "]");
                            row.createCell(1).setCellValue(cursor.getString(1));
                            row.createCell(2).setCellValue("--->");
                            row.createCell(3).setCellValue(cursor.getString(2));
                        }
                            // excel 만드는과정 끝(전송해야함)

                            final String strSDpath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download";
                            // 저장위치 명시(File) 및, 해당위치로의 "쓰기"동작 명시(FileOutputStream)
                        File xlsFile = new File(strSDpath+ "/GoopagoTranslation_stored_db.xlsx");            // getExternalFilesDir = /sdcard/Android/data/[패키지명]/files/
                            FileOutputStream fileOut = new FileOutputStream(xlsFile);

                            // 엑셀 워크북을 OutputStream으로, 저장하는 과정.
                            wb.write(fileOut);      // 엑셀워크북(시트가 모이면 워크북임)을 파일로 송출할 준비를 합니다.
                            fileOut.flush();
                            fileOut.close();

                            //excel파일 저장하는과정 끝. 저장위치 나타냄
                            Toast.makeText(getApplicationContext(),
                                    strSDpath + "에 저장되었습니다(안드로이드 explorer기준의 절대 경로입니다. 접근은 불가능하세요)",
                                    Toast.LENGTH_SHORT).show();

                            Toast.makeText(getApplicationContext(),
                                    "내장메모리/Android/data/[패키지명]/files/  에 저장된 파일(GoopagoTranslation_stored_db.xlsx)을 확인해보세요!! 즐거운 시간 되세요",
                                    Toast.LENGTH_LONG).show();

                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    //외부저장소에 쓸수있는 권한이 열려있어야함(최신 안드로이드는 기본이 열림).
    //만약에 안되면 manifest.xml에 외부저장소에 파일을 쓰는 권한(이하 다음과 같음)을 명시할 것.
    //          <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
