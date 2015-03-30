package com.asynctaskdownloader.alex.asynctaskdownloader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends ActionBarActivity {

    //[公用] 圖片資料夾位址
    static final File publicPicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    static final File customCreateFolder = new File(publicPicFolder, "MyPicFolder");

    Button btnDownload, btnReadPic, btnOpenExplorer;
    TextView txtNumOfDownload;
    ImageView imgView;
    ProgressBar progressBar;
    Spinner spPicList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupViewComponent();
        refreshPicList();
    }

    void setupViewComponent()
    {
        btnDownload = (Button)findViewById(R.id.download);
        imgView = (ImageView)findViewById(R.id.imgView);
        txtNumOfDownload = (TextView)findViewById(R.id.NumOfDownload);
        btnReadPic = (Button)findViewById(R.id.readpic);
        btnOpenExplorer = (Button)findViewById(R.id.openexplorer);
        spPicList = (Spinner)findViewById(R.id.PicList);
        progressBar = (ProgressBar)findViewById(R.id.progressbar);
        progressBar.setProgress(0); //初始化progressbar = 0

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //待下載的圖片URL
                String img_url1 = "http://crackberry.com/sites/crackberry.com/files/styles/large/public/topic_images/2013/ANDROID.png";
                String img_url2 = "http://d.blog.xuite.net/d/0/b/e/238561270/blog_3571718/txt/205236449/0.jpg";
                String img_url3 = "http://www.worldfortravel.com/wp-content/uploads/2015/02/Eiffel-Tower-France.jpg";

                /// /取得系統資訊的網路狀態
                ConnectivityManager mConnMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                if(mConnMgr.getActiveNetworkInfo() == null)
                {
                    Toast.makeText(MainActivity.this,"No network service...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    startActivity(intent);
                }else {
                    new DownloadTask().execute(img_url1, img_url2, img_url3);
                }
            }
        });

        btnReadPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(spPicList.getSelectedItem() == null) {
                    Toast.makeText(MainActivity.this, "請選擇圖片", Toast.LENGTH_SHORT).show();
                    return;
                }

                String strSelectedImg = spPicList.getSelectedItem().toString();
                Bitmap bmp = BitmapFactory.decodeFile(customCreateFolder.getPath()+"/"+strSelectedImg);
                imgView.setImageBitmap(bmp);
            }
        });

        //呼叫外部文件瀏覽app
        btnOpenExplorer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = getPackageManager().getLaunchIntentForPackage("com.rhmsoft.fm");
                startActivity(it);
            }
        });

    }

    //建立DownloadTask類別, 繼承自AsyncTask<Params, Progress, Result>, 可覆寫onPreExecute() onProgressUpdate()  doInBackground()  onPostExecute() 方法
    /*
　　1. Params，啟動任務執行的輸入參數
　　2. Progress，後台任務執行的百分比
　　3. Result，後台計算的結果類型

        onPreExecute -- AsyncTask 執行前的準備工作
        doInBackground -- 實際要執行的程式碼就是寫在這裡。
        onProgressUpdate -- 用來顯示目前的進度，
        onPostExecute -- 執行完的結果 - Result 會傳入這裡。
        除了 doInBackground，其他 3 個 method 都是在 UI thread 呼叫
     */
    public class DownloadTask extends AsyncTask<String,Integer,Void>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MainActivity.this, "Start to download...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(String... params) {
            int count;
            try {
                for(int i=0;i<params.length;i++) {
                    URL url = new URL(params[i]);
                    URLConnection conection = url.openConnection();
                    conection.connect();
                    int lenghtOfFile = conection.getContentLength();
                    InputStream input = new BufferedInputStream(url.openStream());

                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.TAIWAN).format(new Date());
                    OutputStream output = new FileOutputStream(customCreateFolder + "/" + timestamp + ".jpg");

                    byte data[] = new byte[1024];
                    long total = 0;
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        //可帶入多個參數, 下例：第一個參數為下載百分比, 第二，三參數為顯示下載進度(已下載數/總任務數)
                        publishProgress((int) ((total * 100) / lenghtOfFile), i+1, params.length);
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
            txtNumOfDownload.setText(values[1]+ "/" + values[2]);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            refreshPicList();
            Toast.makeText(MainActivity.this,"Download finished.", Toast.LENGTH_SHORT).show();
        }
    }

    void refreshPicList()
    {
        //如果資料夾不存在, 則建立
        if(!customCreateFolder.exists())
            customCreateFolder.mkdirs();

        File[] fileImgLoc = customCreateFolder.listFiles();
        String[] strImgName = new String[fileImgLoc.length];
        for(int i=0;i<fileImgLoc.length;i++)
        {
            strImgName[i] = fileImgLoc[i].getName();
        }

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, strImgName);
        spPicList.setAdapter(myAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPicList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
