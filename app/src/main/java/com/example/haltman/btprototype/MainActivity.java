package com.example.haltman.btprototype;

import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 *  メインアクティビティクラス（Y.K 担当分）
 */
public class MainActivity extends Activity implements OnClickListener {
    private final static int DEVICES_DIALOG = 1;
    private final static int ERROR_DIALOG = 2;

    //BluetoothのSPP通信用クラスのインスタンス
    private BluetoothTask bluetoothTask = new BluetoothTask(this);

    //通信確立中に表示するダイアログ
    private ProgressDialog waitDialog;

    //エラーメッセージ
    private String errorMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI上のすべてのボタンのリスナークラスを当該クラスに設定する。
        Button b1 = (Button)findViewById(R.id.button_back);
        Button b2 = (Button)findViewById(R.id.button_forward);
        Button b3 = (Button)findViewById(R.id.button_left);
        Button b4 = (Button)findViewById(R.id.button_right);
        Button b5 = (Button)findViewById(R.id.button_get);
        Button b6 = (Button)findViewById(R.id.button_stop);
        Button b7 = (Button)findViewById(R.id.button_reset);

        b1.setOnClickListener(this);
        b2.setOnClickListener(this);
        b3.setOnClickListener(this);
        b4.setOnClickListener(this);
        b5.setOnClickListener(this);
        b6.setOnClickListener(this);
        b7.setOnClickListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        // Bluetooth通信クラスを初期化する。
        bluetoothTask.init();
        // ペアリング済みデバイスの一覧を表示してユーザに選ばせる。
        showDialog(DEVICES_DIALOG);
    }

    @Override
    protected void onDestroy() {
        // Bluetooth通信クラスを終了する。（開放する。）
        bluetoothTask.doClose();
        super.onDestroy();
    }

    /**
     * Activityを再起動する。（Bluetooth通信も仕切りなおす）
     */
    protected void restart() {
        Intent intent = this.getIntent();
        this.finish();
        this.startActivity(intent);
    }

    //----------------------------------------------------------------
    // 以下、ダイアログ関連

    /**
     * ダイアログを作成しなければいけないタイミングで呼び出される関数。
     * @param id 作成するダイアログのID
     * @return 作成されたダイアログのインスタンス
     */
    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DEVICES_DIALOG) return createDevicesDialog();
        if (id == ERROR_DIALOG) return createErrorDialog();
        return null;
    }

    /**
     * ダイアログ表示前に必要な処理を記述する。
     * @param id 準備するダイアログID
     * @param dialog 準備するダイアログのインスタンス
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == ERROR_DIALOG) {
            ((AlertDialog) dialog).setMessage(errorMessage);
        }
        super.onPrepareDialog(id, dialog);
    }

    /**
     * 通信するBluetoothデバイスをペアリング済みのものから選択するためのダイアログを作成する。
     * @return 作成されたダイアログのインスタンス
     */
    public Dialog createDevicesDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        //タイトルの設定
        alertDialogBuilder.setTitle("Select device");

        // ペアリング済みのBluetoothデバイスをダイアログのリストにアイテムとして追加していく。
        Set<BluetoothDevice> pairedDevices = bluetoothTask.getPairedDevices();
        final BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);
        String[] items = new String[devices.length];
        for (int i=0;i<devices.length;i++) {
            items[i] = devices[i].getName();
        }

        //それぞれのBluetoothデバイスを示したアイテムがタップされた時に、タップされたデバイスを通信に用いるように設定するようリスナー関数を登録する。
        alertDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //ダイアログを破棄する。
                dialog.dismiss();
                // 選択されたデバイスを通知する。そのまま接続開始。
                bluetoothTask.doConnect(devices[which]);
            }
        });

        //ダイアログがキャンセル可能に設定する。
        alertDialogBuilder.setCancelable(false);

        //ダイアログインスタンスを実際に作成して、返す。
        return alertDialogBuilder.create();
    }

    /**
     * エラーメッセージをダイアログとして表示する関数。
     * （主にBluetooth通信用クラスから異常発生時に使用される。）
     * @param msg エラーメッセージの内容
     */
    @SuppressWarnings("deprecation")
    public void errorDialog(String msg) {
        if (this.isFinishing()) return;
        this.errorMessage = msg;

        //エラーダイアログを表示する。
        this.showDialog(ERROR_DIALOG);
    }


    /**
     * エラー表示用のダイアログを作成する。
     * @return エラー表示用のダイアログを表示する関数
     */
    public Dialog createErrorDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("");

        //Exitボタンをタップした時にダイアログを閉じるようにリスナーを設定する。
        alertDialogBuilder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        return alertDialogBuilder.create();
    }

    /**
     * 通信中であることを示すダイアログを表示する。
     * （主にBluetooth通信用クラスから通信確立中に使用される。）
     * @param msg 表示するメッセージ
     */
    public void showWaitDialog(String msg) {
        if (waitDialog == null) {
            waitDialog = new ProgressDialog(this);
        }
        waitDialog.setMessage(msg);
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.show();
    }

    /**
     * 通信中であることを示すダイアログを隠す。
     * （主にBluetooth通信用クラスから通信確立中に使用される。）
     */
    public void hideWaitDialog() {
        waitDialog.dismiss();
    }

    /**
     * UI内のボタンがタップされた時、そのボタンのIDによってボタンの種類を区別してBluetooth通信クラスにコマンドを送信する。
     * @param v クリックされたボタンのインスタンス
     */
    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.button_back) //後退(↓)がタップされたとき、コマンド文字列"b"を送信。
        {
            bluetoothTask.doSend("b");
        }
        else if(v.getId()==R.id.button_forward) //前進(↑)がタップされたとき、コマンド文字列"f"を送信。
        {
            bluetoothTask.doSend("f");
        }
        else if(v.getId()==R.id.button_left) //左旋回(←)がタップされたとき、コマンド文字列"l"を送信。
        {
            bluetoothTask.doSend("l");
        }
        else if(v.getId()==R.id.button_right) //右旋回(→)がタップされたとき、コマンド文字列"r"を送信。
        {
            bluetoothTask.doSend("r");
        }
        else if(v.getId()==R.id.button_stop) //停止(STOP)がタップされたとき、コマンド文字列"s"を送信。
        {
            bluetoothTask.doSend("s");
        }
        else if(v.getId()==R.id.button_get) //温湿度取得(GET TEMPERATURE AND HUMIDITY)がタップされたとき、温湿度取得コマンドを送信。
        {
            bluetoothTask.doReceive();
        }
        else if(v.getId()==R.id.button_reset)  //リセット(RESET)がタップされたとき、Bluetooth接続を再設定する。
        {
            restart();
        }
    }

    /**
     * UI上の温度と湿度の値の表示を上書きする。
     * @param text 温度と湿度の値をカンマ区切りで格納した文字列
     */
    public void doSetResultText(String text)
    {
        String[] texts = text.split(",");
        TextView t1 = (TextView)findViewById(R.id.text_temperature);
        t1.setText(texts[0]);
        TextView t2 = (TextView)findViewById(R.id.text_humidity);
        t2.setText(texts[1]);
    }
}