package sdk.sinocare.com.sinocaresdk.widget;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sinocare.protocols.ProtocolVersion;

import java.text.SimpleDateFormat;
import java.util.Date;

import sdk.sinocare.com.sinocaresdk.R;

/**
 * @author zhongzhigang
 * @Description:
 * @date 2018/1/30
 */
public class PopupWindowChooseType extends PopupWindow{
    private int screenWidth;
    private int screenHeight;
    // 用于保存PopupWindow的宽度
    private int width;
    // 用于保存PopupWindow的高度
    private int height;
    private BluetoothDevice device;

    View popupWindow_view;
    private OpClick opClick;

    public PopupWindowChooseType setOpClick(OpClick opClick) {
        this.opClick = opClick;
        return this;
    }

    public PopupWindowChooseType(Context context , BluetoothDevice device) {
        super(context);
        this.device = device;
        popupWindow_view = LayoutInflater.from(context).inflate(R.layout.choose_type_dialog,null,false);
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth =dm.widthPixels;
        screenHeight =dm.heightPixels;
        initView();
    }

    /**
     * 强制绘制popupWindowView，并且初始化popupWindowView的尺寸
     */
    private void mandatoryDraw() {
        this.popupWindow_view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        /**
         * 强制刷新后拿到PopupWindow的宽高
         */
        this.width = this.popupWindow_view.getMeasuredWidth();
        this.height = this.popupWindow_view.getMeasuredHeight();
    }

    private void initView() {
        this.setContentView(popupWindow_view);
        // 设置弹出窗体的宽
        this.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        // 设置弹出窗体的高
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        // 设置弹出窗体可点击
        this.setTouchable(true);
        this.setFocusable(true);
        // 设置点击是否消失
        this.setOutsideTouchable(true);
        //设置弹出窗体动画效果
        this.setAnimationStyle(android.R.style.Animation_Dialog);
        //实例化一个ColorDrawable颜色为半透明
        ColorDrawable background = new ColorDrawable(0x4f000000);
        //设置弹出窗体的背景
        this.setBackgroundDrawable(background);
        // 绘制
        this.mandatoryDraw();
        Button btnCommad_1 = (Button)popupWindow_view.findViewById(R.id.btn_commad_1);
        Button btnCommad_2 = (Button)popupWindow_view.findViewById(R.id.btn_commad_2);
        Button btnCommad_3 = (Button)popupWindow_view.findViewById(R.id.btn_commad_3);
        TextView macTv = (TextView) popupWindow_view.findViewById(R.id.mac_tv);
        macTv.setText("你当前选择的设备为：\nmac地址:" + device.getAddress());
        btnCommad_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (opClick != null) {
                    opClick.goTestActivity(ProtocolVersion.WL_1,device);
                }
                disMissPopup();
            }
        });
        btnCommad_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (opClick != null) {
                    opClick.goTestActivity(ProtocolVersion.WL_WEIXIN_AIR,device);
                }
                disMissPopup();
            }
        });
        btnCommad_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (opClick != null) {
                    opClick.goTestActivity(ProtocolVersion.TRUE_METRIX_AIR,device);
                }
                disMissPopup();
            }
        });
        View layMenu = popupWindow_view.findViewById(R.id.cmd_dialog);
        layMenu.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK)
                    disMissPopup();
                return false;
            }
        });
    }

    // date类型转换为String类型
    // formatType格式为yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
    // data Date类型的时间
    public static String dateToString(Date data, String formatType) throws  RuntimeException{
        return new SimpleDateFormat(formatType).format(data);
    }

    private void disMissPopup(){
       dismiss();
    }

    public interface OpClick {
        void goTestActivity(ProtocolVersion protocolVersion,BluetoothDevice device);
    }

}
