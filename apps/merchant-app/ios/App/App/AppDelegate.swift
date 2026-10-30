// 文件版本：V0.01（基于 Capacitor 8.5.3 模板）
import UIKit
import Capacitor
import WebKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }

    func application(_ application: UIApplication,
                     configurationForConnecting connectingSceneSession: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        let config = UISceneConfiguration(name: "Default Configuration",
                                          sessionRole: connectingSceneSession.role)
        config.delegateClass = SceneDelegate.self
        return config
    }
}

// 商户手机应用（第 25 条 25-R7）：后台页面的二次确认与理由输入用网页原生弹窗（window.alert／confirm／prompt），
// Capacitor 原生壳的弹窗按钮写死英文；根视图控制器换成本类（Main.storyboard、SceneDelegate.swift），
// 只把三种弹窗换成按钮为「确定」「取消」的系统弹窗（PGM「界面全中文（所有终端零英文）」），其余网页委托照旧交给 Capacitor。
class MerchantBridgeViewController: CAPBridgeViewController {
    // webView.uiDelegate 是弱引用，由本控制器持有。
    private var chineseDialogs: ChineseWebDialogs?

    override func capacitorDidLoad() {
        super.capacitorDidLoad()
        guard let webView = webView, let original = webView.uiDelegate as? WebViewDelegationHandler else { return }
        let dialogs = ChineseWebDialogs(original: original)
        chineseDialogs = dialogs
        webView.uiDelegate = dialogs
    }
}

/// 三种网页弹窗自己弹，其余 WKUIDelegate 方法（新窗口、媒体权限等）转给 Capacitor 原来的 WebViewDelegationHandler。
final class ChineseWebDialogs: NSObject, WKUIDelegate {
    private let original: WebViewDelegationHandler

    init(original: WebViewDelegationHandler) {
        self.original = original
        super.init()
    }

    override func responds(to aSelector: Selector!) -> Bool {
        return super.responds(to: aSelector) || original.responds(to: aSelector)
    }

    override func forwardingTarget(for aSelector: Selector!) -> Any? {
        return original.responds(to: aSelector) ? original : super.forwardingTarget(for: aSelector)
    }

    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        guard let presenter = topViewController(of: webView) else { return completionHandler() }
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确定", style: .default) { _ in completionHandler() })
        presenter.present(alert, animated: true)
    }

    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
        guard let presenter = topViewController(of: webView) else { return completionHandler(false) }
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "取消", style: .cancel) { _ in completionHandler(false) })
        alert.addAction(UIAlertAction(title: "确定", style: .default) { _ in completionHandler(true) })
        presenter.present(alert, animated: true)
    }

    func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String, defaultText: String?, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (String?) -> Void) {
        // 原生桥借 prompt 做同步查询（CapacitorHttp、CapacitorCookies，内容是带 type 的 JSON），交回 Capacitor 原处理，不弹窗。
        if let data = prompt.data(using: .utf8),
           let payload = try? JSONSerialization.jsonObject(with: data, options: .fragmentsAllowed) as? [String: Any],
           payload["type"] is String {
            return original.webView(webView, runJavaScriptTextInputPanelWithPrompt: prompt, defaultText: defaultText, initiatedByFrame: frame, completionHandler: completionHandler)
        }
        guard let presenter = topViewController(of: webView) else { return completionHandler(nil) }
        let alert = UIAlertController(title: nil, message: prompt, preferredStyle: .alert)
        alert.addTextField { field in field.text = defaultText }
        alert.addAction(UIAlertAction(title: "取消", style: .cancel) { _ in completionHandler(nil) })
        alert.addAction(UIAlertAction(title: "确定", style: .default) { [weak alert] _ in completionHandler(alert?.textFields?.first?.text ?? defaultText) })
        presenter.present(alert, animated: true)
    }

    private func topViewController(of webView: WKWebView) -> UIViewController? {
        var top = webView.window?.rootViewController
        while let presented = top?.presentedViewController, !presented.isBeingDismissed {
            top = presented
        }
        return top
    }
}
