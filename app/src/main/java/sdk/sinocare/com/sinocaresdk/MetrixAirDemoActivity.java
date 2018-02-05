package sdk.sinocare.com.sinocaresdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.sinocare.Impl.SC_BatteryCallBack;
import com.sinocare.Impl.SC_BlueToothCallBack;
import com.sinocare.Impl.SC_CurrentDataCallBack;
import com.sinocare.domain.BloodSugarData;
import com.sinocare.handler.SN_MainHandler;
import com.sinocare.protocols.ProtocolVersion;
import com.sinocare.status.SC_DataStatusUpdate;
import com.sinocare.status.SC_ErrorStatus;
import com.sinocare.utils.LogUtil;

import java.util.ArrayList;
import java.util.Date;

import sdk.sinocare.com.sinocaresdk.MsgListAdapter.DeviceListItem;
import sdk.sinocare.com.sinocaresdk.widget.PopupWindowMetrixAir;

/**
 * @author zhongzhigang
 *         created at 2018/1/30
 * @file_name MetrixAirDemoActivity.java
 * @description: //真睿(TRUE METRIX AIR)血糖仪
 */
public class MetrixAirDemoActivity extends Activity implements PopupWindowMetrixAir.OpClick {

    private static final String TAG = MetrixAirDemoActivity.class.getSimpleName();
    public static final int REFRESH = 1001;

    private ListView mListView;
    private Button commandButton, disconnectButton, clearButton;
    private PopupWindowMetrixAir popupWindow;
    private ArrayList<MsgListAdapter.DeviceListItem> list;
    private MsgListAdapter mAdapter;
    private SN_MainHandler snMainHandler = null;
    private BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hud_main);
        initActivity();
        registerReceiver(mBtReceiver, makeIntentFilter());
    }

    private void initActivity() {
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null)
                device = bundle.getParcelable("device");
            list = new ArrayList<>();
            mAdapter = new MsgListAdapter(this, list);
            mListView = (ListView) findViewById(R.id.list);
            mListView.setAdapter(mAdapter);
            mListView.setFastScrollEnabled(true);
            snMainHandler = SN_MainHandler.getBlueToothInstance(this);
            snMainHandler.connectBlueTooth(device, new SC_BlueToothCallBack() {
                @Override
                public void onConnectFeedBack(int result) {
                    if(result == 16){
                        LogUtil.log(TAG, "onConnectFeedBack-----------success");
                    }else {
                        LogUtil.log(TAG, "onConnectFeedBack-----------fail");
                    }
                }
            }, ProtocolVersion.TRUE_METRIX_AIR);
            snMainHandler.registerReceiveBloodSugarData(new SC_CurrentDataCallBack<BloodSugarData>() {

                @Override
                public void onStatusChange(int status) {
                    // TODO Auto-generated method stub
                    if (status == SC_DataStatusUpdate.SC_BLOOD_FFLASH)
                        list.add(new DeviceListItem("请插入试条测试！", false));
                    else if (status == SC_DataStatusUpdate.SC_MC_TESTING)
                        list.add(new DeviceListItem("正在测试，请稍后！", false));
                    else if (status == SC_DataStatusUpdate.SC_MC_SHUTTINGDOWN)
                        list.add(new DeviceListItem("正在关机！", false));
                    else if (status == SC_DataStatusUpdate.SC_MC_SHUTDOWN)
                        list.add(new DeviceListItem("已关机！", false));

                    loadHandler.sendEmptyMessage(REFRESH);
                }

                @Override
                public void onReceiveSyncData(BloodSugarData datas) {
                    float v = datas.getBloodSugarValue();
                    Date date = datas.getCreatTime();
                    list.add(new DeviceListItem("同步历史测试结果：" + v + "mmol/l," + "时间："
                            + date.toLocaleString(), false));
                    loadHandler.sendEmptyMessage(REFRESH);
                }

                @Override
                public void onReceiveSucess(BloodSugarData datas) {
                    // TODO Auto-generated method stub
                    float v = datas.getBloodSugarValue();
                    Date date = datas.getCreatTime();
                    float t = datas.getTemperature();
                    list.add(new DeviceListItem("测试结果：" + v + "mmol/l," + "时间："
                            + date.toLocaleString() + "当前温度：" + t + "°", false));
                    loadHandler.sendEmptyMessage(REFRESH);
                }
            });

            commandButton = (Button) findViewById(R.id.bt_command);
            commandButton.setOnClickListener(commandButtonClickListener);
            disconnectButton = (Button) findViewById(R.id.bt_disconnect);
            disconnectButton.setOnClickListener(disconnectButtonClickListener);
            clearButton = (Button) findViewById(R.id.bt_clear);
            clearButton.setOnClickListener(clearButtonClickListener);

            if (!snMainHandler.isConnected()) {
                setActivityInIdleState();
            } else {
                setActivityInConnectedState();
            }
        }
    }

    //未连接状态界面刷新
    private void setActivityInIdleState() {
        commandButton.setVisibility(View.GONE);
        disconnectButton.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);
    }

    //连接状态界面刷新
    private void setActivityInConnectedState() {
        //searchButton.setVisibility(View.GONE);
        commandButton.setVisibility(View.VISIBLE);
        disconnectButton.setVisibility(View.VISIBLE);
        clearButton.setVisibility(View.VISIBLE);
    }

    private OnClickListener disconnectButtonClickListener = new OnClickListener() {
        @SuppressWarnings("static-access")
        @Override
        public void onClick(View arg0) {
            snMainHandler.disconnectDevice();
            finish();
        }
    };

    private OnClickListener clearButtonClickListener = new OnClickListener() {
        @SuppressWarnings("static-access")
        @Override
        public void onClick(View arg0) {
            list.clear();
            loadHandler.sendEmptyMessage(REFRESH);
        }
    };

    private OnClickListener commandButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (!snMainHandler.isConnected()) {
                //Hud_Display.toast("设备未连接，请先建立连接再发送命令！");
            } else {
                getPopupWindow();
                popupWindow.showAtLocation(commandButton, Gravity.CENTER, 0, 0);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        if (!snMainHandler.isBlueToothEnable()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 3);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        snMainHandler.disconnectDevice();
        unregisterReceiver(mBtReceiver);
    }

    //创建PopupWindow
    @SuppressLint("InflateParams")
    @SuppressWarnings("deprecation")
    protected void initPopuptWindow() {
        popupWindow = new PopupWindowMetrixAir(this);
        popupWindow.setOpClick(this);
    }

    //获取PopupWindow实例
    private void getPopupWindow() {
        if (null != popupWindow) {
            popupWindow.dismiss();
            return;
        } else {
            initPopuptWindow();
        }
    }

    @Override
    public void firstData() {
        list.add(new DeviceListItem("命令:获取最近一条测量数据！", true));
        loadHandler.sendEmptyMessage(REFRESH);
        snMainHandler.requestFirstRecord();
    }

    @Override
    public void historyData() {
        //获取当前所有历史数据
        list.add(new DeviceListItem("命令:获取所有血糖测量数据！", true));
        loadHandler.sendEmptyMessage(REFRESH);
        snMainHandler.requestAllRecord();
    }

    @Override
    public void getBattery() {
        //设置设备时间
        list.add(new DeviceListItem("命令:获取设备的剩余电量", true));
        loadHandler.sendEmptyMessage(REFRESH);
        snMainHandler.requestBattery(new SC_BatteryCallBack() {
            @Override
            public void onBatteryCallBack(int percent) {
                list.add(new DeviceListItem("获取到设备的剩余电量为" + percent + "%", false));
                loadHandler.sendEmptyMessage(REFRESH);
            }
        });
    }

    //广播监听SDK ACTION
    private final BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SN_MainHandler.ACTION_SN_CONNECTION_STATE_CHANGED.equals(action)) {
                if (snMainHandler.isUnSupport()) {
                    list.add(new MsgListAdapter.DeviceListItem("手机设备不支持低功耗蓝牙，无法连接血糖仪", false));
                    loadHandler.sendEmptyMessage(REFRESH);
                } else if (snMainHandler.isConnected()) {
                    setActivityInConnectedState();
                } else if (snMainHandler.isIdleState() || snMainHandler.isDisconnecting()) {
                    setActivityInIdleState();
                }
            } else if (SN_MainHandler.ACTION_SN_ERROR_STATE.equals(action)) {
                Bundle bundle = intent.getExtras();
                int errorStatus = bundle.getInt(SN_MainHandler.EXTRA_ERROR_STATUS);
                if (errorStatus == SC_ErrorStatus.SC_OVER_RANGED_TEMPERATURE)
                    list.add(new DeviceListItem("错误码：E-2", false));
                else if (errorStatus == SC_ErrorStatus.SC_AUTH_ERROR)
                    list.add(new DeviceListItem("错误：认证失败！", false));
                else if (errorStatus == SC_ErrorStatus.SC_ERROR_OPERATE)
                    list.add(new DeviceListItem("错误码：E-3！", false));
                else if (errorStatus == SC_ErrorStatus.SC_ERROR_FACTORY)
                    list.add(new DeviceListItem("错误码：E-6！", false));
                else if (errorStatus == SC_ErrorStatus.SC_ABLOVE_MAX_VALUE)
                    list.add(new DeviceListItem("错误码：HI", false));
                else if (errorStatus == SC_ErrorStatus.SC_BELOW_LEAST_VALUE)
                    list.add(new DeviceListItem("错误码：LO", false));
                else if (errorStatus == SC_ErrorStatus.SC_LOW_POWER)
                    list.add(new DeviceListItem("错误码：E-1！", false));
                else if (errorStatus == SC_ErrorStatus.SC_UNDEFINED_ERROR)
                    list.add(new DeviceListItem("未知错误！", false));
                else if (errorStatus == 6)
                    list.add(new DeviceListItem("E-6", false));
                loadHandler.sendEmptyMessage(REFRESH);
            } else if (SN_MainHandler.ACTION_SN_MC_STATE.equals(action)) {
                Bundle bundle = intent.getExtras();
                int MCStatus = bundle.getInt(SN_MainHandler.EXTRA_MC_STATUS);
            }
        }
    };

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SN_MainHandler.ACTION_SN_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(SN_MainHandler.ACTION_SN_ERROR_STATE);
        intentFilter.addAction(SN_MainHandler.ACTION_SN_MC_STATE);
        return intentFilter;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (snMainHandler.isConnected())
            snMainHandler.disconnectDevice();
    }

    //主线程中的handler
    private Handler loadHandler = new Handler() {
        /**
         * 接受子线程传递的消息机制
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            switch (what) {
                case REFRESH: {
                    mAdapter.notifyDataSetChanged();
                    mListView.setSelection(list.size());
                    break;
                }
            }
        }
    };

}
