package cn.dreamn.qianji_auto.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.adapter.SmartViewHolder;

import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.ui.base.BaseAdapter;
import cn.dreamn.qianji_auto.utils.pictures.MyBitmapUtils;

public class DataListAdapter extends BaseAdapter {
    private Context mContext;
    public DataListAdapter(Context context) {

        super(R.layout.data_item);
        mContext=context;
    }

    @Override
    protected void onBindViewHolder(SmartViewHolder holder, Bundle item, int position) {

        ImageView icon_header = (ImageView) holder.findView(R.id.icon_header);

        TextView item_title = (TextView) holder.findView(R.id.item_title);

        item_title.setText(item.getString("name"));
        final Handler mHandler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                MyBitmapUtils.setImage(mContext,icon_header,(Bitmap) msg.obj);
            }
        };
        MyBitmapUtils myBitmapUtils=new MyBitmapUtils(mContext,mHandler);
        myBitmapUtils.disPlay(icon_header,item.getString("icon"));

    }
}
