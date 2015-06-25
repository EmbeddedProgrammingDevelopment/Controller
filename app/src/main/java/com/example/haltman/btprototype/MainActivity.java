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

public class MainActivity extends Activity implements OnClickListener {
    private final static int DEVICES_DIALOG = 1;
    private final static int ERROR_DIALOG = 2;

    private BluetoothTask bluetoothTask = new BluetoothTask(this);

    private ProgressDialog waitDialog;
    private String errorMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        Button resetBtn = (Button) findViewById(R.id.button_reset);
        resetBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        // Bluetooth初期化
        bluetoothTask.init();
        // ペアリング済みデバイスの一覧を表示してユーザに選ばせる。
        showDialog(DEVICES_DIALOG);
    }

    @Override
    protected void onDestroy() {
        bluetoothTask.doClose();
        super.onDestroy();
    }

    public void doSetResultText(String text)
    {
        String[] texts = text.split(",");
        TextView t1 = (TextView)findViewById(R.id.text_temperature);
        t1.setText(texts[0]);
        TextView t2 = (TextView)findViewById(R.id.text_humidity);
        t2.setText(texts[1]);
    }

    protected void restart() {
        Intent intent = this.getIntent();
        this.finish();
        this.startActivity(intent);
    }

    //----------------------------------------------------------------
    // 以下、ダイアログ関連
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DEVICES_DIALOG) return createDevicesDialog();
        if (id == ERROR_DIALOG) return createErrorDialog();
        return null;
    }
    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == ERROR_DIALOG) {
            ((AlertDialog) dialog).setMessage(errorMessage);
        }
        super.onPrepareDialog(id, dialog);
    }

    public Dialog createDevicesDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Select device");

        // ペアリング済みデバイスをダイアログのリストに設定する。
        Set<BluetoothDevice> pairedDevices = bluetoothTask.getPairedDevices();
        final BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);
        String[] items = new String[devices.length];
        for (int i=0;i<devices.length;i++) {
            items[i] = devices[i].getName();
        }

        alertDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // 選択されたデバイスを通知する。そのまま接続開始。
                bluetoothTask.doConnect(devices[which]);
            }
        });
        alertDialogBuilder.setCancelable(false);
        return alertDialogBuilder.create();
    }

    @SuppressWarnings("deprecation")
    public void errorDialog(String msg) {
        if (this.isFinishing()) return;
        this.errorMessage = msg;
        this.showDialog(ERROR_DIALOG);
    }
    public Dialog createErrorDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error");
        alertDialogBuilder.setMessage("");
        alertDialogBuilder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        return alertDialogBuilder.create();
    }

    public void showWaitDialog(String msg) {
        if (waitDialog == null) {
            waitDialog = new ProgressDialog(this);
        }
        waitDialog.setMessage(msg);
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.show();
    }
    public void hideWaitDialog() {
        waitDialog.dismiss();
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.button_back)
        {
            bluetoothTask.doSend("b");
        }
        else if(v.getId()==R.id.button_forward)
        {
            bluetoothTask.doSend("f");
        }
        else if(v.getId()==R.id.button_left)
        {
            bluetoothTask.doSend("l");
        }
        else if(v.getId()==R.id.button_right)
        {
            bluetoothTask.doSend("r");
        }
        else if(v.getId()==R.id.button_stop)
        {
            bluetoothTask.doSend("s");
        }
        else if(v.getId()==R.id.button_get)
        {
            bluetoothTask.doReceive();
        }
        else if(v.getId()==R.id.button_reset)
        {
            restart();
        }
    }
}