package sdk.sinocare.com.sinocaresdk;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.sinocare.Impl.SC_BlueToothSearchCallBack;
import com.sinocare.domain.BlueToothInfo;
import com.sinocare.handler.SN_MainHandler;
import com.sinocare.protocols.ProtocolVersion;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;

import java.util.ArrayList;
import java.util.List;

import sdk.sinocare.com.sinocaresdk.widget.PopupWindowChooseType;

/**
 * 主界面,搜索设备
 */
public class MainActivity extends Activity implements PopupWindowChooseType.OpClick {

    private ListView mListView;
    private ArrayList<SiriListItem> list;
    private DevicesListAdapter mAdapter;
    private Context mContext = null;
    private SN_MainHandler snMainHandler = null;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    /**
     * 容器
     */
    private void initData() {
        list = new ArrayList<>();
        mAdapter = new DevicesListAdapter(this, list);
    }

    /**
     * 添加事件
     */
    private void initEvent() {
        searchButton.setOnClickListener(searchButtonClickListener);
        mListView.setOnItemClickListener(mDeviceClickListener);
    }

    /**
     * 初始化界面UI
     */
    private void initView() {
        searchButton = (Button) findViewById(R.id.bt_search);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setFastScrollEnabled(true);
        setActivityInIdleState();
    }

    /**
     * 初始化
     */
    private void init() {
        mContext = this;
        initData();
        initView();
        initEvent();
        initSnHandler();
    }

    /**
     * 未连接状态界面刷新
     */
    private void setActivityInIdleState() {
        if (snMainHandler == null) {
            return;
        }
        searchButton.setVisibility(View.VISIBLE);
        if (snMainHandler.isSearching()) {
            searchButton.setText("停止搜索");
        } else {
            searchButton.setText("搜索/连接");
        }
    }

    private OnClickListener searchButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (snMainHandler == null) {
                initSnHandler();
                return;
            }
            if (snMainHandler.isConnecting()) {
                Toast.makeText(mContext, "正在断开，请稍等", Toast.LENGTH_SHORT).show();
                snMainHandler.disconnectDevice();
                return;
            }
            list.clear();
            mAdapter.notifyDataSetChanged();
            if (!snMainHandler.isBlueToothEnable()) {
                Intent enableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, 3);
            }
            if (snMainHandler.isConnected()) {
                snMainHandler.disconnectDevice();
                searchButton.setText("搜索/连接");
            } else if (snMainHandler.isSearching()) {
                snMainHandler.cancelSearch();
                searchButton.setText("停止搜索");
            } else {
                searchButton.setText("搜索/连接");
                snMainHandler.searchBlueToothDevice(new SC_BlueToothSearchCallBack<BlueToothInfo>() {
                    @Override
                    public void onBlueToothSeaching(BlueToothInfo newDevice) {

                        SiriListItem sir = new SiriListItem(newDevice.getName() + "\n"
                                + newDevice.getDevice().getAddress(), false, newDevice);

                        //过滤掉已添加在设备列表中设备
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).info.getDevice().getAddress().equals(newDevice.getDevice().getAddress())) {
                                if (newDevice.getName().equals(list.get(i).info.getName()))
                                    return;
                            }
                        }
                        //添加list
                        list.add(sir);
                        mAdapter.notifyDataSetChanged();
                        mListView.setSelection(list.size() - 1);
                    }
                });
            }
        }
    };

    /**
     * 一定要先获取权限
     */
    private void initSnHandler() {
        AndPermission.with(MainActivity.this)
                .permission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        snMainHandler = SN_MainHandler.getBlueToothInstance(MainActivity.this);
                    }
                }).onDenied(new Action() {
            @Override
            public void onAction(List<String> permissions) {
                Toast.makeText(mContext, "请先允许用户权限", Toast.LENGTH_SHORT).show();
            }
        }).start();
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            final SiriListItem item = list.get(arg2);
            PopupWindowChooseType popupWindowChooseType = new PopupWindowChooseType(MainActivity.this, item.info.getDevice());
            popupWindowChooseType.setOpClick(MainActivity.this);
            popupWindowChooseType.showAtLocation(searchButton, Gravity.CENTER, 0, 0);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (snMainHandler != null) {
            snMainHandler.close();
        }
    }

    @Override
    public void goTestActivity(ProtocolVersion protocolVersion, BluetoothDevice device) {
        if (snMainHandler == null) {
            return;
        }
        snMainHandler.cancelSearch();
        Intent intent = new Intent();
        switch (protocolVersion) {
            case WL_1:
                intent.setClass(MainActivity.this, WLOneDemoActivity.class);
                break;
            case TRUE_METRIX_AIR:
                intent.setClass(MainActivity.this, MetrixAirDemoActivity.class);
                break;
            case WL_WEIXIN_AIR:
                intent.setClass(MainActivity.this, WLAirDemoActivity.class);
                break;
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable("device", device);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public class SiriListItem {
        String message;
        boolean isSiri;
        BlueToothInfo info;

        public SiriListItem(String msg, boolean siri, BlueToothInfo infos) {
            message = msg;
            isSiri = siri;
            info = infos;
        }
    }
}

