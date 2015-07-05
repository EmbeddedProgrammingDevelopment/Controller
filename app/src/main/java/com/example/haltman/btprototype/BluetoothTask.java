package com.example.haltman.btprototype;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.util.Log;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 *  Bluetoothの通信用クラス（A.I担当分）
 *  引用元Webサイト : http://www.kotemaru.org/2013/10/30/android-bluetooth-sample.html
 */
public class BluetoothTask {
    //Logcat用のタグ
    private static final String TAG = "BluetoothTask";

    //BluetoothのSPP通信に使用するUUID。
    private static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //メインアクティビティへの参照。
    private MainActivity activity;

    //Bluetooth通信のアダプタ
    private BluetoothAdapter bluetoothAdapter;

    //通信するBluetoothデバイスを扱うオブジェクト
    private BluetoothDevice bluetoothDevice = null;

    //Bluetooth通信用のソケット
    private BluetoothSocket bluetoothSocket;

    //SPP通信の文字列受信用ストリーム
    private InputStream btIn;

    //SPP通信の文字列送信用ストリーム
    private OutputStream btOut;

    /**
     * コンストラクタ
     * @param activity メインアクティビティのインスタンス
     */
    public BluetoothTask(MainActivity activity) {
        this.activity = activity;
    }

    /**
     * Bluetoothの初期化。
     */
    public void init() {
        // Bluetoothアダプタを取得する。
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //取得に失敗した時は、AndroidがBluetoothを取得していないためエラーメッセージを出力して終了。
        if (bluetoothAdapter == null) {
            activity.errorDialog("This device is not implement Bluetooth.");
            return;
        }

        // Bluetoothがむこうの時はユーザに有効化の許可を求めた上で、有効化する。
        if (!bluetoothAdapter.isEnabled()) {
            // TODO: ユーザに許可を求める処理。
            activity.errorDialog("This device is disabled Bluetooth.");
            return;
        }
    }
    /**
     * @return ペアリング済みのデバイス一覧を返す。デバイス選択ダイアログ用。
     */
    public Set<BluetoothDevice> getPairedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }

    /**
     * 指定されたデバイスとの接続を非同期で開始する。
     * @param device 選択デバイス
     */
    public void doConnect(BluetoothDevice device) {
        bluetoothDevice = device;
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(APP_UUID);
            new ConnectTask().execute();
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
            activity.errorDialog(e.toString());
        }
    }

    /**
     * 非同期でBluetoothの接続を閉じる。
     * （CloseTaskを実行する。）
     */
    public void doClose() {
        new CloseTask().execute();
    }

    /**
     * 非同期でメッセージの送信を行う。（主にキャタピラ制御用）
     * @param msg 送信メッセージ。
     */
    public void doSend(String msg) {
        new SendTask().execute(msg);
    }

    /**
     * 非同期で、温度取得を行う。
     */
    public void doReceive(){ new ReceiveTask().execute();}

    /**
     * Bluetoothと接続を開始する非同期タスク。
     * - 時間がかかる場合があるのでProcessDialogを表示する。
     * - 双方向のストリームを開くところまで。
     */
    private class ConnectTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected void onPreExecute() {
            activity.showWaitDialog("Connect Bluetooth Device.");
        }

        @Override
        protected Object doInBackground(Void... params) {
            try {
                bluetoothSocket.connect();
                btIn = bluetoothSocket.getInputStream();
                btOut = bluetoothSocket.getOutputStream();
            } catch (Throwable t) {
                doClose();
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Throwable) {
                Log.e(TAG,result.toString(),(Throwable)result);
                activity.errorDialog(result.toString());
            } else {
                activity.hideWaitDialog();
            }
        }
    }

    /**
     * Bluetoothと接続を終了する非同期タスク。
     * - 不要かも知れないが念のため非同期にしている。
     */
    private class CloseTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected Object doInBackground(Void... params) {
            try {
                try{btOut.close();}catch(Throwable t){/*ignore*/}
                try{btIn.close();}catch(Throwable t){/*ignore*/}
                bluetoothSocket.close();
            } catch (Throwable t) {
                return t;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Throwable) {
                Log.e(TAG,result.toString(),(Throwable)result);
                activity.errorDialog(result.toString());
            }
        }
    }

    /**
     * メッセージを送信する非同期タスククラス。
     * （主にキャタピラ制御に用いられる。）
     */
    private class SendTask extends AsyncTask<String, Void, Object> {
        @Override
        protected Object doInBackground(String... params) {
            try {
                btOut.write(params[0].getBytes());
                btOut.flush();

                return new String("Connection.");
            } catch (Throwable t) {
                doClose();
                return t;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof Exception) {
                Log.e(TAG,result.toString(),(Throwable)result);
                activity.errorDialog(result.toString());
            } else {
                // 結果を画面に反映。
                //activity.doSetResultText(result.toString());
            }
        }
    }

    /**
     * メッセージを送信して、向こうからの応答メッセージを取得する非同期タスククラス。
     * （主に温度取得に用いられる。）
     */
    private class ReceiveTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                btOut.write('g');
                btOut.flush();

                return new String("connected.");
            } catch (Throwable t) {
                doClose();
                return t.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                // 結果を画面に反映。
                String res="";
                try {
                    while(true) {
                        byte[] buf = new byte[512];
                        int len = btIn.read(buf);
                        String r = new String(buf,0,len);
                        res += r;
                        if(r.contains("|"))
                        {
                            break;
                        }
                    }
                    Log.d(TAG,res);
                }catch(Exception e)
                {

                }
                activity.doSetResultText(res.substring(0,res.length()-1));
            }
        }
    }
}
