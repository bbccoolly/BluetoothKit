package com.inuker.bluetooth;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.inuker.bluetooth.library.ConstantsClassic;
import com.inuker.bluetooth.library.beacon.BluetoothDataParserImpl;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.ClassicResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.utils.BluetoothLog;
import com.inuker.bluetooth.library.utils.ByteUtils;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;

public class ClassicStepActivity extends FragmentActivity implements View.OnClickListener {
    private final static String TAG = ClassicStepActivity.class.getName();

    private ListView mConversationView;

    private ArrayAdapter<String> mConversationArrayAdapter;
    BluetoothDataParserImpl mBluetoothDataParserImpl;
    private boolean mConnected;
    private SearchResult device;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classic_step);


        Intent intent = getIntent();
        device = intent.getParcelableExtra("SearchResult");

        mConversationView = (ListView) findViewById(R.id.in);
        Button btnFirstAuth = (Button) findViewById(R.id.btnFirstAuth);
        Button btnSecondAuth = (Button) findViewById(R.id.btnSecondAuth);
        Button btnChargeStart = (Button) findViewById(R.id.btnChargeStart);
        Button btnChargeStop = (Button) findViewById(R.id.btnChargeStop);
        Button btnOffChargeStart = (Button) findViewById(R.id.btnOffChargeStart);
        Button btnUnoffChargeStart = (Button) findViewById(R.id.btnUnoffChargeStart);
        Button btnClose = (Button) findViewById(R.id.btnClose);
        Button btnRecon = (Button) findViewById(R.id.btnRecon);
        btnFirstAuth.setOnClickListener(this);
        btnSecondAuth.setOnClickListener(this);
        btnChargeStart.setOnClickListener(this);
        btnChargeStop.setOnClickListener(this);
        btnOffChargeStart.setOnClickListener(this);
        btnUnoffChargeStart.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnRecon.setOnClickListener(this);
        mBluetoothDataParserImpl = new BluetoothDataParserImpl(null);


        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        ClientManager.getClient().readClassic(new ClassicResponse() {
            @Override
            public void onResponse(int code, Object data) {
                if (code == ConstantsClassic.MESSAGE_READ) {
                    Log.i(TAG, "readClassic data = " + data);
                    String s1 = new String((byte[]) data);
                    String s = ByteUtils.byteToString((byte[]) data);
                    mConversationArrayAdapter.add("Received:" + s1);
                    mConversationArrayAdapter.notifyDataSetChanged();

                }
            }
        });


        ClientManager.getClient().registerConnectStatusListener(device.getAddress(), mConnectStatusListener);

        connectDeviceIfNeeded();
    }


    private final BleConnectStatusListener mConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            BluetoothLog.v(String.format("DeviceDetailActivity onConnectStatusChanged %d in %s",
                    status, Thread.currentThread().getName()));
            mConnected = (status == STATUS_CONNECTED);
            connectDeviceIfNeeded();
        }
    };

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btnFirstAuth) {
            sendByteData((byte) 0x31, null, 0);
        } else if (i == R.id.btnSecondAuth) {
            sendByteData((byte) 0x32, null, 0);
        } else if (i == R.id.btnChargeStart) {
            byte[] parms = new byte[]{(byte) 0xA1};
            sendByteData((byte) 0x10, parms, 0);
        } else if (i == R.id.btnChargeStop) {
            byte[] parms = new byte[]{(byte) 0xA2};
            sendByteData((byte) 0x10, parms, 0);
        } else if (i == R.id.btnOffChargeStart) {
            byte[] parms = new byte[]{(byte) 0xA3};
            sendByteData((byte) 0x10, parms, 0);
        } else if (i == R.id.btnUnoffChargeStart) {
            byte[] parms = new byte[]{(byte) 0xA4};
            sendByteData((byte) 0x10, parms, 0);
        } else if (i == R.id.btnClose) {
            ClientManager.getClient().disconnectClassic();
        } else if (i == R.id.btnRecon) {
            connectDeviceIfNeeded();
        }

    }

    private void sendByteData(byte command, byte[] params, int serialNum) {
        byte[] bytes = mBluetoothDataParserImpl.toBytes(command, params, serialNum);
        ClientManager.getClient().writeClassic(bytes, new ClassicResponse() {
            @Override
            public void onResponse(int code, Object data) {
                if (code == ConstantsClassic.MESSAGE_WRITE) {
                    String s = ByteUtils.byteToString((byte[]) data);
                    mConversationArrayAdapter.add("Send:" + s);
                    mConversationArrayAdapter.notifyDataSetChanged();
                    Log.i(TAG, "writeClassic data = " + data);
                } else {
                    Toast.makeText(ClassicStepActivity.this, "蓝牙未连接，请连接蓝牙", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void connectDeviceIfNeeded() {
        if (!mConnected) {
            connectDevice();
        }
    }


    private void connectDevice() {
        showNormalDialog();
        ClientManager.getClient().connectClassic(device.getAddress(), new ClassicResponse() {
            @Override
            public void onResponse(int code, Object data) {
//                mTvTitle.setText(String.format("%s", device.getAddress()));
                if (null != alertDialog && alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
                if (code == ConstantsClassic.CLASSIC_CON_SECCESS) {
                    Toast.makeText(ClassicStepActivity.this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ClassicStepActivity.this, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
                    showErrorDialog();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        ClientManager.getClient().disconnectClassic();
        ClientManager.getClient().unregisterConnectStatusListener(device.getAddress(), mConnectStatusListener);
        super.onDestroy();
    }


    private void showNormalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("蓝牙连接")
                .setCancelable(false)
                .setMessage("蓝牙连接中，请稍等...");
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void showErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("蓝牙连接失败")
                .setCancelable(false)
                .setMessage("蓝牙连接失败，是否重新连接")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        connectDeviceIfNeeded();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        builder.create().show();
    }


}
