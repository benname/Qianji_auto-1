/*
 * Copyright (C) 2021 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cn.dreamn.qianji_auto.ui.fragment.data.regulars;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.LayoutMode;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.bottomsheets.BottomSheet;
import com.afollestad.materialdialogs.files.DialogFileChooserExtKt;
import com.afollestad.materialdialogs.list.DialogListExtKt;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.shehuan.statusview.StatusView;
import com.xuexiang.xpage.annotation.Page;
import com.xuexiang.xpage.enums.CoreAnim;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;
import com.yanzhenjie.recyclerview.touch.OnItemMoveListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import cn.dreamn.qianji_auto.R;
import cn.dreamn.qianji_auto.database.Helper.Category;
import cn.dreamn.qianji_auto.database.Helper.identifyRegulars;
import cn.dreamn.qianji_auto.permission.PermissionUtils;
import cn.dreamn.qianji_auto.ui.adapter.CateItemListAdapter;
import cn.dreamn.qianji_auto.ui.base.BaseFragment;
import cn.dreamn.qianji_auto.ui.fragment.web.WebViewFragment;
import cn.dreamn.qianji_auto.ui.utils.AutoBillWeb;
import cn.dreamn.qianji_auto.ui.views.LoadingDialog;
import cn.dreamn.qianji_auto.utils.files.FileUtils;
import cn.dreamn.qianji_auto.utils.runUtils.Log;
import cn.dreamn.qianji_auto.utils.runUtils.Task;
import cn.dreamn.qianji_auto.utils.runUtils.Tool;
import es.dmoral.toasty.Toasty;


@Page(name = "本地识别规则", anim = CoreAnim.slide)
public class localFragment extends BaseFragment {

    private static final int HANDLE_ERR = 0;
    private static final int HANDLE_OK = 1;
    private static final int HANDLE_REFRESH = 2;
    private static final int HANDLE_OUT = 3;
    private final String type;
    @BindView(R.id.status)
    StatusView statusView;
    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.recycler_view)
    SwipeRecyclerView recyclerView;
    @BindView(R.id.multiple_actions_down)
    FloatingActionsMenu floatingActionButton;
    @BindView(R.id.action_cate)
    FloatingActionButton action_cate;
    @BindView(R.id.action_import)
    FloatingActionButton action_import;
    @BindView(R.id.action_export)
    FloatingActionButton action_export;
    @BindView(R.id.action_delAll)
    FloatingActionButton action_delAll;
    LoadingDialog loadDialog;
    private CateItemListAdapter mAdapter;
    private List<Bundle> list;
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_ERR:
                    if (statusView != null) statusView.showEmptyView();
                    break;
                case HANDLE_OK:
                    mAdapter.refresh(list);
                    Task.onMain(1000, () -> statusView.showContentView());
                    break;
                case HANDLE_REFRESH:
                    String d = (String) msg.obj;
                    if (loadDialog != null)
                        loadDialog.close();
                    if ((d != null && !d.equals("")))
                        Toasty.success(getContext(), d, Toast.LENGTH_LONG).show();
                    loadFromData(refreshLayout);
                    break;
                case HANDLE_OUT:
                    if (loadDialog != null)
                        loadDialog.close();
                    String d2 = (String) msg.obj;
                    if ((d2 != null && !d2.equals("")))
                        Toasty.success(getContext(), d2, Toast.LENGTH_LONG).show();

                    break;
            }
            floatingActionButton.setVisibility(View.VISIBLE);
        }
    };

    public localFragment(String type) {
        this.type = type;
    }

    private String getName() {
        switch (type) {
            case "sms":
                return "短信";
            case "notice":
                return "通知";
            case "app":
                return "app";
        }
        return "";
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_main_data_sort_manager;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.sendEmptyMessage(HANDLE_REFRESH);
    }

    @Override
    protected void initViews() {
        statusView.setEmptyView(R.layout.empty_view);
        statusView.setLoadingView(R.layout.loading_view);

        statusView.setOnEmptyViewConvertListener(viewHolder -> {
            viewHolder.setText(R.id.empty_info, "你还没有任何" + getName() + "规则哦！\n");
        });
        statusView.setOnLoadingViewConvertListener(viewHolder -> {
            viewHolder.setText(R.id.load_info, "正在加载" + getName() + "规则...");
        });
        floatingActionButton.setVisibility(View.GONE);
        statusView.showLoadingView();
        initLayout();
    }

    @SuppressLint("CheckResult")
    @Override
    protected void initListeners() {

        refreshLayout.setOnRefreshListener(refreshlayout -> {
            refreshlayout.finishRefresh(2000/*,false*/);//传入false表示刷新失败
        });
        action_cate.setOnClickListener(v -> {


            WebViewFragment.openUrl(this, "file:///android_asset/html/Regulars/index.html?type=" + this.type);


        });
        action_import.setOnClickListener(v -> {
            PermissionUtils permissionUtils = new PermissionUtils(getContext());
            permissionUtils.grant(PermissionUtils.Storage);
            try {
                // 导入
                BottomSheet bottomSheet = new BottomSheet(LayoutMode.WRAP_CONTENT);
                MaterialDialog dialog = new MaterialDialog(getContext(), bottomSheet);
                dialog.title(null, "请选择自动记账" + getName() + "识别规则配置文件");

                DialogFileChooserExtKt.fileChooser(dialog, getContext(), Environment.getExternalStorageDirectory(), file -> file.isDirectory() || (file.isFile() && file.getName().endsWith("ankio." + this.type + ".backup")),
                        true, R.string.files_default_empty_text, false, null,
                        (materialDialog, file) -> {
                            //Log.d(file.getAbsolutePath());
                            String data = FileUtils.get(file.getAbsolutePath());
                            JSONObject jsonObject = JSONObject.parseObject(data);
                            String from = jsonObject.getString("from");

                            if (!from.equals(this.type)) {
                                Toasty.error(getContext(), "该文件不是有效的" + getName() + "配置数据文件").show();
                                return null;
                            }
                            JSONArray jsonArray = jsonObject.getJSONArray("data");

                            BottomSheet bottomSheet2 = new BottomSheet(LayoutMode.WRAP_CONTENT);
                            MaterialDialog dialog2 = new MaterialDialog(getContext(), bottomSheet2);
                            dialog2.cornerRadius(15f, null);
                            dialog2.title(null, "恢复提醒");
                            dialog2.message(null, "是否覆盖原有数据（清空不保留）？", null);
                            dialog2.negativeButton(null, "不清空", (a) -> null);
                            dialog2.positiveButton(null, "清空", (a) -> {
                                Category.clear();
                                return null;
                            });

                            dialog2.setOnDismissListener(dialog1 -> {
                                loadDialog = new LoadingDialog(getContext(), "数据导入中...");
                                loadDialog.show();
                                Task.onThread(() -> {
                                    for (int i = 0; i < jsonArray.size(); i++) {
                                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                                        identifyRegulars.add(
                                                new String(Base64.decode(jsonObject1.getString("regular"), Base64.NO_WRAP)),
                                                jsonObject1.getString("name"),
                                                jsonObject1.getString("text"),
                                                jsonObject1.getString("tableList"),
                                                jsonObject1.getString("identify"),
                                                jsonObject1.getString("fromApp"),
                                                jsonObject1.getString("des"),
                                                new identifyRegulars.Finish() {
                                                    @Override
                                                    public void onFinish() {
                                                        Log.d("finish data" + jsonObject1.toString());
                                                    }
                                                });
                                    }
                                    Message message = new Message();
                                    message.what = HANDLE_REFRESH;
                                    message.obj = "恢复成功！";
                                    mHandler.sendMessage(message);
                                });


                            });
                            dialog2.show();
                            return null;
                        });

                dialog.cornerRadius(15f, null);
                dialog.show();
            } catch (Exception | Error e) {
                e.printStackTrace();
                Log.i("出错了，可能是权限未给全！" + e.toString());
            }
        });
        action_export.setOnClickListener(v -> {
            BottomSheet bottomSheet = new BottomSheet(LayoutMode.WRAP_CONTENT);
            MaterialDialog dialog1 = new MaterialDialog(getContext(), bottomSheet);
            dialog1.cornerRadius(15f, null);
            dialog1.title(null, "请选择导出方案");
            DialogListExtKt.listItems(dialog1, null, Arrays.asList("导出至下载文件夹", "分享"), null, true, (materialDialog, index, text) -> {
                loadDialog = new LoadingDialog(getContext(), "数据导出中...");
                loadDialog.show();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("from", this.type);
                identifyRegulars.getAll(this.type, null, bundle -> {
                    JSONArray jsonArray = new JSONArray();
                    for (Bundle regular : bundle) {
                        //        Category.addCategory(new String(Base64.decode(jsonObject1.getString("regular"), Base64.NO_WRAP)), jsonObject1.getString("name"), jsonObject1.getString("tableList"), jsonObject1.getString("des")
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("name", regular.getString("name"));
                        jsonObject1.put("regular", Base64.encode(regular.getString("regular").getBytes(), Base64.NO_WRAP));
                        jsonObject1.put("tableList", regular.getString("tableList"));
                        jsonObject1.put("des", regular.getString("des"));
                        jsonObject1.put("fromApp", regular.getString("fromApp"));
                        jsonObject1.put("identify", regular.getString("identify"));
                        jsonObject1.put("text", regular.getString("text"));
                        jsonArray.add(jsonObject1);
                    }
                    jsonObject.put("data", jsonArray);
                    String fileName = Tool.getTime("yyyyMMddHHmmss") + ".ankio." + this.type + ".backup";
                    Tool.writeToCache(getContext(), fileName, jsonObject.toJSONString());
                    switch (index) {
                        case 0:
                            String newFileName = Environment.getExternalStorageDirectory().getPath() + "/Download/QianJiAuto/" + fileName;
                            FileUtils.makeRootDirectory(Environment.getExternalStorageDirectory().getPath() + "/Download/QianJiAuto/");
                            FileUtils.copyFile(getContext().getExternalCacheDir().getPath() + "/" + fileName, newFileName);
                            Log.m(fileName);
                            FileUtils.del(fileName);
                            break;
                        case 1:

                            Tool.shareFile(getContext(), getContext().getExternalCacheDir().getPath() + "/" + fileName);
                            FileUtils.del(fileName);
                            break;

                    }
                    Message message = new Message();
                    message.what = HANDLE_OUT;
                    message.obj = "数据导出成功";
                    mHandler.sendMessage(message);


                });

                return null;
            });
            dialog1.show();


        });
        action_delAll.setOnClickListener(v -> {
            BottomSheet bottomSheet2 = new BottomSheet(LayoutMode.WRAP_CONTENT);
            MaterialDialog dialog2 = new MaterialDialog(getContext(), bottomSheet2);
            dialog2.cornerRadius(15f, null);
            dialog2.title(null, "删除提醒");
            dialog2.message(null, "是否清空所有" + getName() + "规则数据？", null);
            dialog2.negativeButton(null, "不清空", (a) -> null);
            dialog2.positiveButton(null, "清空", (a) -> {
                Category.clear(() -> {
                    Message message = new Message();
                    message.what = HANDLE_REFRESH;
                    message.obj = "清除成功";
                    mHandler.sendMessage(message);
                });

                return null;
            });
            dialog2.show();
        });
    }

    private void initLayout() {
        mAdapter = new CateItemListAdapter(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(this::OnItemClickListen);
        mAdapter.setOnMoreClick(item -> {
            BottomSheet bottomSheet = new BottomSheet(LayoutMode.WRAP_CONTENT);
            MaterialDialog dialog = new MaterialDialog(getContext(), bottomSheet);
            dialog.cornerRadius(15f, null);
            dialog.title(null, item.getString("name"));
            dialog.message(null, item.getString("des"), null);
            dialog.show();
        });
        recyclerView.setLongPressDragEnabled(true);
        recyclerView.setOnItemMoveListener(new OnItemMoveListener() {
            @Override
            public boolean onItemMove(RecyclerView.ViewHolder srcHolder, RecyclerView.ViewHolder targetHolder) {
                // 此方法在Item拖拽交换位置时被调用。
                // 第一个参数是要交换为之的Item，第二个是目标位置的Item。

                // 交换数据，并更新adapter。
                int fromPosition = srcHolder.getAdapterPosition();
                int toPosition = targetHolder.getAdapterPosition();
                Collections.swap(list, fromPosition, toPosition);
                mAdapter.notifyItemMoved(fromPosition, toPosition);
                identifyRegulars.setSort(list.get(fromPosition).getInt("id"), fromPosition);
                identifyRegulars.setSort(list.get(toPosition).getInt("id"), toPosition);

                // 返回true，表示数据交换成功，ItemView可以交换位置。
                return true;
            }

            @Override
            public void onItemDismiss(RecyclerView.ViewHolder viewHolder) {

            }

        });// 监听拖拽，更新UI。
        refreshLayout.setOnRefreshListener(this::loadFromData);
        refreshLayout.setEnableRefresh(true);
        loadFromData(refreshLayout);
    }

    @SuppressLint("CheckResult")
    private void OnItemClickListen(View view, int position) {
        if (list == null || position >= list.size()) return;

        Bundle cate = list.get(position);

        BottomSheet bottomSheet = new BottomSheet(LayoutMode.WRAP_CONTENT);
        MaterialDialog dialog = new MaterialDialog(getContext(), bottomSheet);
        dialog.cornerRadius(15f, null);
        String disable = "禁用";
        if (cate.getInt("use") != 1) {
            disable = "启用";
        }
        dialog.title(null, "请选择操作(" + cate.getString("name") + ")");
        DialogListExtKt.listItems(dialog, null, Arrays.asList("删除", "可视化编辑", "上传到云端", disable), null, true, (materialDialog, index, text) -> {
            switch (index) {
                case 0:
                    Category.del(cate.getInt("id"), () -> {
                        Message message = new Message();
                        message.obj = "删除成功";
                        message.what = HANDLE_REFRESH;
                        mHandler.sendMessage(message);
                    });
                    break;
                case 1:

                    WebViewFragment.openUrl(this, "file:///android_asset/html/Category/index.html?id=" + cate.getInt("id") + "&data=" + Base64.encodeToString(cate.getString("tableList").getBytes(), Base64.NO_WRAP));
                    break;
                case 2:
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", cate.getString("name"));
                    jsonObject.put("text", cate.getString("text"));
                    jsonObject.put("data", cate.getString("regular"));
                    jsonObject.put("tableList", cate.getString("tableList"));
                    jsonObject.put("identify", cate.getString("identify"));
                    jsonObject.put("fromApp", cate.getString("fromApp"));
                    jsonObject.put("isCate", "0");
                    jsonObject.put("description", cate.getString("des"));
                    String result = Base64.encodeToString(jsonObject.toString().getBytes(), Base64.NO_WRAP);
                    AutoBillWeb.httpSend(getContext(), this, "send", result);
                    break;
                case 3:
                    if (text == "禁用") {
                        Category.deny(cate.getInt("id"), () -> {
                            Message message = new Message();
                            message.obj = "禁用成功";
                            message.what = HANDLE_REFRESH;
                            mHandler.sendMessage(message);
                        });
                    } else {
                        Category.enable(cate.getInt("id"), () -> {
                            Message message = new Message();
                            message.obj = "启用成功";
                            message.what = HANDLE_REFRESH;
                            mHandler.sendMessage(message);
                        });
                    }
            }
            return null;
        });
        dialog.show();

    }


    public void loadFromData(RefreshLayout refreshLayout) {

        Task.onMain(1000, () -> {
            identifyRegulars.getAll(type, null, regulars -> {
                if (regulars == null || regulars.length == 0) {
                    mHandler.sendEmptyMessage(HANDLE_ERR);
                } else {
                    list = Arrays.asList(regulars);
                    mHandler.sendEmptyMessage(HANDLE_OK);
                }
            });
        });
    }


}