package com.example.refresh_selection;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;

public class ModelClient extends AppCompatActivity {

    private AlarmManager alarmManager;
    private GregorianCalendar mCalender;

    private NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    private TextView predict_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_test);
//        setAlarm(5);

        //        private void createNotificationChannel() {
//            // Create the NotificationChannel, but only on API 26+ because
//            // the NotificationChannel class is new and not in the support library
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                CharSequence name = getString(R.string.channel_name);
//                String description = getString(R.string.channel_description);
//                int importance = NotificationManager.IMPORTANCE_DEFAULT;
//                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
//                channel.setDescription(description);
//                // Register the channel with the system; you can't change the importance
//                // or other notification behaviors after this
//                NotificationManager notificationManager = getSystemService(NotificationManager.class);
//                notificationManager.createNotificationChannel(channel);
//            }
//        }
//
//        // push 알림
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.dm)
//                .setContentTitle("Refresh")
//                .setContentText("오늘은 날씨가 좋네요:) 같이 걸을까요?")
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
//


        findViewById(R.id.test_model_btn).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                Interpreter tflite = getTfliteInterpreter("converted_model.tflite");
                try {
                    AssetManager manager = getAssets();
                    InputStream in = manager.open("test.csv");

                    ArrayList<Data> cooked = parse(in);
                    String all_steps = "";
                    for(Data piece : cooked) {
                        all_steps += piece.steps+" ";
                    }
//                    System.out.println(all_steps);
                    String[] all_steps_strArr = all_steps.split(" ");
                    int[] all_steps_intArr = Arrays.stream(all_steps_strArr).mapToInt(Integer::parseInt).toArray();
//                    System.out.println(all_steps_intArr);
                    int[][] all_steps_inArr_2d = new int[all_steps_strArr.length/24][24];
                    int index = 0;
                    for(int j = 0; j<all_steps_inArr_2d.length; j++){
                        for(int k = 0; k< all_steps_inArr_2d[0].length; k++){
                                all_steps_inArr_2d[j][k] = all_steps_intArr[index++];
                        }
                    }
//                    System.out.println(all_steps_inArr_2d);
                    int test_day_index = 0;//여길 바꾸면 다른날짜값 데이터 가져온다.
                    int max = max(all_steps_inArr_2d[test_day_index]);
                    //하루의 0시부터 23시까지의 걸음수 가 있다. 최대걸음수를 저장한다.
                    int min = min(all_steps_inArr_2d[test_day_index]);
                    //하루중 걸음수 최소값을 저장한다.
                    float[][][] train_data = new float[1][24][1];
                    //모델에 들어갈 데이터 모양 만들기
                    for(int i = 0; i< train_data[0].length;i++){
                        train_data[0][i][0] = (all_steps_inArr_2d[test_day_index][i]-min)/((float)(max-min));
                    }
//minmax scaling
                    float[][][] output = new float[1][24][1];
                    tflite.run(train_data, output);//output 모양 지정해주고 결과물이 저장된다.
                    System.out.println(max_index(output)+"");// 예측값으로 소수점들이 나온다 그중에
                    predict_time= findViewById(R.id.predict_time);
                    predict_time.append(max_index(output)+"");
                    // 가장 높은값이 예측한 시간의 인덱스를 가져옴
                    //일단은 임시로 csv데이터로 모델을 예측함
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(ModelClient.this, modelPath));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /** Load TF Lite model from assets. */
    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    private static final int COL_DATE = 0;
    private static final int COL_TIME = 1;
    private static final int COL_STEPS = 2;

    private ArrayList<Data> parse(InputStream in) throws IOException {
        ArrayList<Data> results = new ArrayList<Data>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String nextLine = reader.readLine();
        while ((nextLine = reader.readLine()) != null) {
            String[] tokens = nextLine.split(",");
            if (tokens.length != 3) {
                Log.w("CSVParser", "Skipping Bad CSV Row");
                continue;
            }
            //Add new parsed result
            Data current = new Data();
            current.date = tokens[COL_DATE];
            current.time = tokens[COL_TIME];
            current.steps = tokens[COL_STEPS];

            results.add(current);
        }
        in.close();
        return results;
    }

    public int max(int[] arr){
        int max_num = 0;
        for(int i = 0; i<arr.length; i++){
            max_num = max_num>arr[i] ? max_num : arr[i];
        }
        return max_num;
    }

    public int min(int[] arr){
        int min_num = 10000;
        for(int i = 0; i<arr.length; i++){
            min_num = min_num<arr[i] ? min_num : arr[i];
        }
        return min_num;
    }

    public int max_index(float[][][] arr){
        int index = 0;
        float max_num = 0;
        for(int i = 0; i< arr[0].length;i++){
            if(max_num<arr[0][i][0]){
                max_num = arr[0][i][0];
                index = i;
            }
        }
        return index;
    }

//    private void setAlarm(int time) {
//        alarmManager = (AlarmManager) getSystemService((Context.ALARM_SERVICE));
//        //AlarmReceiver에 값 전달
//        Intent receiverIntent = new Intent(this, AlarmRecevier.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, receiverIntent, 0);
//
//        String from = "2021-05-28 02:07:00"; //임의로 날짜와 시간을 지정
//
//        //날짜 포맷을 바꿔주는 소스코드
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Date datetime = null;
//        try {
//            datetime = dateFormat.parse(from);
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(datetime);
//
//        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
////        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),  AlarmManager.INTERVAL_DAY, pendingIntent);
//
//
//    }

//    public void NotificationSomethings() {
//        final String NOTIFICATION_CHANNEL_ID = "10001";
//
//        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
//
//        Intent notificationIntent = new Intent(this, this.getClass()); // 두번째 파라미터가 ResultActivity
////        notificationIntent.putExtra("notificationId", count); //전달할 값
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) ;
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,  PendingIntent.FLAG_UPDATE_CURRENT);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground)) //BitMap 이미지 요구
//                .setContentTitle("Refresh")
//                .setContentText("오늘은 날씨가 좋네요:) 같이 걸을까요?")
//
//                // 더 많은 내용이라서 일부만 보여줘야 하는 경우 아래 주석을 제거하면 setContentText에 있는 문자열 대신 아래 문자열을 보여줌
//                //.setStyle(new NotificationCompat.BigTextStyle().bigText("더 많은 내용을 보여줘야 하는 경우..."))
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent) // 사용자가 노티피케이션을 탭시 ResultActivity로 이동하도록 설정
//                .setAutoCancel(true);
//
//        //OREO API 26 이상에서는 채널 필요
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//
//            builder.setSmallIcon(R.drawable.ic_launcher_foreground); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남
//            CharSequence channelName  = "노티페케이션 채널";
//            String description = "오레오 이상을 위한 것임";
//            int importance = NotificationManager.IMPORTANCE_HIGH;
//
//            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName , importance);
//            channel.setDescription(description);
//
//            // 노티피케이션 채널을 시스템에 등록
//            assert notificationManager != null;
//            notificationManager.createNotificationChannel(channel);
//
//        }else builder.setSmallIcon(R.mipmap.ic_launcher); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남
//
//        assert notificationManager != null;
//        notificationManager.notify(1234, builder.build()); // 고유숫자로 노티피케이션 동작시킴
//
//    }


}

class Data {
    public String date;
    public String time;
    public String steps;

    public Data() { }
}