// 文件版本：V0.01
// 商户手机应用（第 25 条 25-R7）：后台页面的二次确认与理由输入用网页原生弹窗（window.alert／confirm／prompt），
// Capacitor 原生壳的弹窗按钮写死英文；这里只把三种弹窗的按钮换成「确定」「取消」（PGM「界面全中文（所有终端零英文）」），
// 文件选择、媒体权限等其余行为沿用 Capacitor 的 BridgeWebChromeClient。
package com.complex.merchant.dev;

import android.app.AlertDialog;
import android.os.Bundle;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebView;
import android.widget.EditText;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebChromeClient;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设备没有可用的 WebView 时 BridgeActivity 只显示提示页、不建 bridge。
        if (bridge == null) return;
        bridge.getWebView().setWebChromeClient(new BridgeWebChromeClient(bridge) {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                if (isFinishing()) return true;
                new AlertDialog.Builder(view.getContext())
                    .setMessage(message)
                    .setPositiveButton("确定", (dialog, which) -> result.confirm())
                    .setOnCancelListener((dialog) -> result.cancel())
                    .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                if (isFinishing()) return true;
                new AlertDialog.Builder(view.getContext())
                    .setMessage(message)
                    .setPositiveButton("确定", (dialog, which) -> result.confirm())
                    .setNegativeButton("取消", (dialog, which) -> result.cancel())
                    .setOnCancelListener((dialog) -> result.cancel())
                    .show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                if (isFinishing()) return true;
                EditText input = new EditText(view.getContext());
                if (defaultValue != null) input.setText(defaultValue);
                new AlertDialog.Builder(view.getContext())
                    .setMessage(message)
                    .setView(input)
                    .setPositiveButton("确定", (dialog, which) -> result.confirm(input.getText().toString().trim()))
                    .setNegativeButton("取消", (dialog, which) -> result.cancel())
                    .setOnCancelListener((dialog) -> result.cancel())
                    .show();
                return true;
            }
        });
    }
}
