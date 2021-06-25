package kr.gachon.goopago;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public DBHelper (Context context) {
        super(context, "storedb", null, DATABASE_VERSION);          // 번역 전 문장과 번역 후 문장을 저장하기 위한 DB
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String storeSQL = "create table sentence(_id integer primary key autoincrement," +   // 테이블명은 sentence
                "beforeText," +                                                                       // 번역 전 문장
                "afterText," +                                                             // 번역 후 문장
                "whatapi)";                                                                // 어떤 api를 사용했는지
        db.execSQL(storeSQL);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(newVersion==DATABASE_VERSION){
            db.execSQL("drop table sentence");
            onCreate(db);
        }
    }
}
