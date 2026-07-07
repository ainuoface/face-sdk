# Ainuo Face iOS SDK 接入与使用规范

版本：v0.1.0

本文档为 Ainuo Face iOS SDK 的官方标准接入规范，旨在为移动端研发人员提供清晰的 SDK 集成、初始化、人脸录入、识别比对及本地人脸库管理指导。

SDK 内部已深度集成并优化了人脸检测、特征提取、本地高性能向量检索、图片 1:1 比对、图片 1:N 搜索、相机实时识别及动作活体检测功能。接入侧无需关注底层模型结构、检测器配置或特征向量的降维与比对细节，直接使用预设的最佳实践配置即可满足绝大多数通用业务场景。

---

## 1. 快速接入

### 1.1 引入 SDK

**Swift Package 接入**

将 iOS SDK Swift Package 加入业务工程，并选择 product `AinuoFaceSDK`。

若业务工程同样基于 Swift Package 进行包管理，可在 `Package.swift` 中声明依赖：

```swift
dependencies: [
    .package(path: "../face-recognition-ios-sdk")
]
```

随后在业务 target 中引入 SDK 产物：

```swift
.product(name: "AinuoFaceSDK", package: "AinuoFaceIOSSDK")
```

若通过远程私有源或 `xcframework` 二进制库的形式接入，按标准方式完成链接即可。在需要调用能力的源码文件中统一导入：

```swift
import AinuoFaceSDK
```

### 1.2 声明相机权限

在涉及相机录入、相机流实时识别或动作活体检测的场景中，必须在 App 的 `Info.plist` 中声明相机权限：

```xml
<key>NSCameraUsageDescription</key>
<string>用于人脸录入、识别和活体检测</string>
```

SDK 提供的标准相机控制器会在需要时自动发起相机权限请求。业务方也可在进入人脸业务流前提前校验权限，若权限被拒则引导用户前往系统设置页面手动开启。

### 1.3 准备授权文本

iOS 端的离线授权机制与 App 的 Bundle Identifier 强绑定。需提前确认目标接入 App 的 Bundle Identifier，例如：

```text
com.example.customer.app
```

获取授权后会得到一段 license 文本。对外 Demo 不内置授权文件，业务侧应通过服务端下发、私有配置或构建参数拿到授权文本，并在初始化时直接传入 SDK。

### 1.4 初始化 SDK

建议在 App 启动生命周期（如 `AppDelegate` 或启动页）且在首次调用人脸相关业务前完成引擎初始化。

直接透传授权字符串：

```swift
let licenseText = "<your-license-text>"

let result = FaceSdk.initialize(
    config: FaceSdkConfig(),
    licenseText: licenseText
)
```

初始化成功后，方可调用底库管理、特征比对、实时检索等进阶功能。全局辅助方法如下：

```swift
let ready = FaceSdk.isInitialized
let sdkVersion = FaceSdk.getVersion()
let licenseInfo = FaceSdk.getLicenseInfo()
let validUntil = licenseInfo?.validUntilEpochSeconds

// 当应用进入后台或长期无需人脸业务时，可主动释放内存与引擎实例
FaceSdk.release()
```

---

## 2. 核心架构与 API 总览

### 2.1 SDK 生命周期管理

| API                                          | 核心职能                                     |
| :------------------------------------------- | :------------------------------------------- |
| `FaceSdk.initialize(config:licenseText:)`    | 基于内存中的授权文本初始化引擎。             |
| `FaceSdk.isInitialized`                      | 探针方法，校验当前环境引擎是否就绪。         |
| `FaceSdk.getVersion()`                       | 获取当前 SDK 框架的具体版本标识。            |
| `FaceSdk.getLicenseInfo()`                   | 获取 SDK 校验后的授权有效期。                |
| `FaceSdk.release()`                          | 销毁内部引擎实例及常驻内存资源。             |
| `FaceSdk.personManager()`                    | 暴露出底库持久化及特征管理器的单例。         |
| `FaceSdk.recognitionEngine()`                | 暴露出特征提取、向量召回与比对引擎的单例。   |

### 2.2 预置标准化视图控制器

为大幅缩减业务接入成本，SDK 内部封装了企业级标准的交互视图控制器。包含了硬件相机管控、人脸框渲染、状态防抖过滤及标准 UI 交互闭环。

| 控制器基类                   | 适用场景                            |
| :--------------------------- | :---------------------------------- |
| `FaceRegisterViewController` | 面向终端用户的单人人脸录入页面。    |
| `FaceSearchViewController`   | 面向实时通行或签到的 1:N 检索页面。 |
| `FaceLivenessViewController` | 强安全前置动作活体检测页面。        |
| `FaceActivityResult`         | 录入与识别流程的标准回调数据结构。  |
| `FaceLivenessActivityResult` | 活体检测流程的标准回调数据结构。    |

### 2.3 原子相机组件

针对高度定制化的 UI 需求，可直接集成底层的无 UI 纯净相机流处理器。

| 核心组件                 | 通用能力矩阵                                                              |
| :----------------------- | :------------------------------------------------------------------------ |
| `FaceRegisterCameraView` | `configure(_:)`、`setListener(_:)`、`start()`、`stop()`、`switchCamera()` |
| `FaceSearchCameraView`   | `configure(_:)`、`setListener(_:)`、`start()`、`stop()`、`switchCamera()` |

### 2.4 底库与向量持久化管理

由 `FaceSdk.personManager()` 统筹，支持高性能的本地 SQLite 特征持久化与高并发查询。

| API 方法                                       | 业务语义                                   |
| :--------------------------------------------- | :----------------------------------------- |
| `register(personId:name:image:extra:)`         | 解析单张静态图片并落库特征至全局池。       |
| `register(personId:name:image:groupId:extra:)` | 将人员特征落库至指定业务分组下。           |
| `get(personId:)`                               | 精确匹配并召回单个人员详情。               |
| `list()`                                       | 全表扫描导出（生产环境强烈建议配合分页）。 |
| `list(groupId:)`                               | 导出指定分组下的完整人员列表。             |
| `list(offset:limit:)`                          | 基于游标实现全局分页加载。                 |
| `list(groupId:offset:limit:)`                  | 基于游标实现指定分组下的分页加载。         |
| `listFaceFeatures(personId:)`                  | 查询指定人员的全部特征模板。               |
| `count()`                                      | 统计全局已注册人员规模。                   |
| `count(groupId:)`                              | 统计特定分组下的人员规模。                 |
| `delete(personId:)`                            | 物理删除指定人员的所有关联特征及记录。     |
| `clear()`                                      | 危险操作：清空本地底库中所有人员。         |
| `clear(groupId:)`                              | 物理清空指定分组，不波及其他业务分组。     |

### 2.5 视觉检测与检索核心

由 `FaceSdk.recognitionEngine()` 提供高层级的视觉算子调用。

| API 方法               | 业务语义                                          |
| :--------------------- | :------------------------------------------------ |
| `extractFeature(_:)`   | 图像预处理与张量计算，输出高维特征向量。          |
| `compare(_:_:)`        | 读入两帧图像执行 1:1 人脸验证任务。               |
| `compare(_:_:config:)` | 纯特征 1:1 验证任务（面向业务自维护底库场景）。   |
| `search(_:config:)`    | 单帧图像 1:N 并发向量召回（输出 TopK 候选队列）。 |

---

## 3. 标准化视图控制器集成

### 3.1 实时相机流人员录入

```swift
final class RegisterViewController: UIViewController, FaceRegisterViewControllerDelegate {
    func startFaceRegister() {
        let controller = FaceRegisterViewController(
            config: FaceRegisterViewControllerConfig(
                registerConfig: FaceRegisterCameraViewConfig(
                    personId: "user_10086",
                    name: "张三",
                    extra: #"{"department":"研发部"}"#,
                    groupId: "store_shanghai_001"
                ),
                title: "人脸录入",
                finishOnRegistered: true
            )
        )
        controller.delegate = self
        navigationController?.pushViewController(controller, animated: true)
    }

    func faceRegisterViewController(_ controller: FaceRegisterViewController, didFinish result: FaceActivityResult) {
        if result.success {
            print("注册流程完成，系统自动落库 ID: \(result.personId ?? "")")
        } else {
            print("流程终止或异常: \(result.code), \(result.message)")
        }
    }
}
```

触发 `success` 即代表底层视觉引擎已成功捕获有效帧、完成特征抽取并持久化至 SQLite，业务侧无需重复调用保存操作。

默认录入页为单人单特征模式，只采集一条正脸模板。需要提升儿童、角度变化或表情变化场景下的识别稳定性时，可以显式开启多模板采集：

```swift
let controller = FaceRegisterViewController(
    config: FaceRegisterViewControllerConfig(
        registerConfig: FaceRegisterCameraViewConfig(
            personId: "user_10086",
            name: "张三",
            captureMode: .multiTemplate,
            targetFeatureTemplateCount: 4
        )
    )
)
```

多模板采集仍然只允许画面中有一个人；它表示“同一个人采多条特征模板”，不是多人同时录入。

### 3.2 实时相机流人脸检索

```swift
final class SearchViewController: UIViewController, FaceSearchViewControllerDelegate {
    func startFaceSearch() {
        let controller = FaceSearchViewController(
            config: FaceSearchViewControllerConfig(
                searchConfig: FaceSearchConfig(
                    threshold: 0.55,
                    topK: 3,
                    singleFaceOnly: false,
                    groupId: "store_shanghai_001"
                ),
                lensFacing: .front,
                title: "人脸识别",
                finishOnMatched: true
            )
        )
        controller.delegate = self
        navigationController?.pushViewController(controller, animated: true)
    }

    func faceSearchViewController(_ controller: FaceSearchViewController, didFinish result: FaceActivityResult) {
        if result.success && result.matched {
            print("系统校验命中人员: \(result.name ?? result.personId ?? "")")
        } else {
            print("检索未命中或发生阻断错误: \(result.code), \(result.message)")
        }
    }
}
```

**关键配置说明**：`threshold` 决定了特征空间向量距离判定的及格线。默认值 0.55 仅为调试基准，生产环境必须结合硬件设备 ISP 质量、现场光照条件及底库整体规模进行严格的重新标定。

### 3.3 动作交互式活体检测

```swift
final class LivenessViewController: UIViewController, FaceLivenessViewControllerDelegate {
    func startLiveness() {
        let controller = FaceLivenessViewController(
            config: FaceLivenessViewControllerConfig(
                title: "活体检测",
                motionConfig: FaceMotionLivenessConfig(
                    stepCount: 3,
                    timeoutSeconds: 10
                ),
                enableSound: true
            )
        )
        controller.delegate = self
        navigationController?.pushViewController(controller, animated: true)
    }

    func faceLivenessViewController(_ controller: FaceLivenessViewController, didFinish result: FaceLivenessActivityResult) {
        if result.success {
            print("交互式活体验证通过")
        } else {
            print("活体拦截或流程超时失败: \(result.code), \(result.message)")
        }
    }
}
```

针对强安全级别签到门禁场景，建议在核心业务流前序节点接入该动作活体检测。

---

## 4. 底库维护与分组隔离

### 4.1 基于静态图像注册

适用于业务后台系统下发照片落库，或设备端选图录入的场景：

```swift
guard let manager = FaceSdk.personManager() else { return }

do {
    let faceImage = try FaceImage(uiImage: image)
    let result = manager.register(
        personId: "user_10086",
        name: "张三",
        image: faceImage,
        groupId: "store_shanghai_001",
        extra: #"{"department":"研发部"}"#
    )

    if result.success {
        print("静态图像提特征落库完成: \(result.person?.personId ?? "")")
    } else {
        print("图像解析或提特征失败: \(result.code), \(result.message)")
    }
} catch {
    print("图像数据封装异常: \(error.localizedDescription)")
}
```

**数据强一致性约束**：`personId` 为核心业务主键，在全局必须保持绝对唯一。即使分配至不同的 `groupId`，也严禁发生 `personId` 碰撞。

### 4.2 数据的增删改查机制

```swift
guard let manager = FaceSdk.personManager() else { return }

let person = manager.get(personId: "user_10086")
let allPersons = manager.list(offset: 0, limit: 50)
let groupPersons = manager.list(groupId: "store_shanghai_001", offset: 0, limit: 50)
let groupCount = manager.count(groupId: "store_shanghai_001")

let deleteResult = manager.delete(personId: "user_10086")
let clearGroupResult = manager.clear(groupId: "store_shanghai_001")
```

`list()` / `list(offset:limit:)` 返回人员摘要列表。对于多模板人员，`FacePerson.featureCount` 表示当前模板数量，`feature` 仅代表主模板；如需读取正脸、左右转头、表情等全部模板，请使用 `listFaceFeatures(personId:)`。

```swift
let templates = manager.listFaceFeatures(personId: "user_10086")
if templates.success {
    templates.templates.forEach { template in
        print("\(template.slotType.rawValue) \(template.feature.embedding.count)维")
    }
}
```

`delete(personId:)`、`clear()` 和 `clear(groupId:)` 会同时删除人员记录和该人员关联的全部特征模板，不会只删除主脸。

**性能规约**：对于千人或万人级别的大底库设备，严禁调用全量 `list()` 接口。务必采用 `offset/limit` 的分页规范，以防止发生大对象内存溢出（OOM）。

### 4.3 隔离分组（GroupId）机制

`groupId` 机制用于解决单机支持多门店、多业务线或 SaaS 多租户的数据逻辑隔离问题。

```swift
let registerResult = manager.register(
    personId: "user_10086",
    name: "张三",
    image: faceImage,
    groupId: "store_shanghai_001", // 指定落入到该门店分组
    extra: nil
)

let searchResult = FaceSdk.recognitionEngine().search(
    faceImage,
    config: FaceSearchConfig(
        threshold: 0.55,
        topK: 3,
        groupId: "store_shanghai_001" // 将召回池限定在该门店内
    )
)
```

**边界处理机制**：

- `groupId == nil`：人员不具备分组属性；检索时置为空则代表全库扫描。
- 传入纯空白字符串（如 `""` 或 `" "`）属于非法参数输入，底层将拦截并抛出 `configInvalid` 错误代码。

---

## 5. 核心算子：比对与检索

### 5.1 图像 1:1 比对运算

```swift
do {
    let left = try FaceImage(uiImage: leftImage)
    let right = try FaceImage(uiImage: rightImage)

    let result = FaceSdk.recognitionEngine().compare(left, right)

    if result.success {
        print("相似度得分: \(result.similarity), 同人判定: \(result.matched)")
    } else {
        print("执行算子失败: \(result.code), \(result.message)")
    }
} catch {
    print("图像数据装载异常: \(error.localizedDescription)")
}
```

### 5.2 图像 1:N 向量召回

```swift
do {
    let faceImage = try FaceImage(uiImage: image)
    let result = FaceSdk.recognitionEngine().search(
        faceImage,
        config: FaceSearchConfig(
            threshold: 0.55,
            topK: 3,
            singleFaceOnly: true, // 静态场景建议开启严格的单人校验
            groupId: "store_shanghai_001"
        )
    )

    if result.success, let best = result.matches.first {
        print("召回最优匹配候选: \(best.person.name ?? best.person.personId)")
        print("该候选相似度: \(best.similarity)")
    } else {
        print("全局底库未召回达标人员: \(result.code), \(result.message)")
    }
} catch {
    print("图像装载异常: \(error.localizedDescription)")
}
```

针对包含复杂背景的合照场景，建议置 `singleFaceOnly = true` 强制排错。但在处理高频连续相机帧流时，需置为 `false`，SDK 内部的调度中心将自动计算并追踪画面中央的主体人脸执行后续 Pipeline。

---

## 6. 定制化原子相机集成

若需要对整体 UI 布局及动效进行细粒度的重构，可绕开标准控制器，直接将 `FaceSearchCameraView` 编排至自定义 `UIViewController` 内。

```swift
final class CustomSearchViewController: UIViewController, FaceSearchCameraViewListener {
    private let cameraView = FaceSearchCameraView()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.addSubview(cameraView)
        cameraView.frame = view.bounds
        cameraView.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        cameraView.configure(
            FaceSearchCameraViewConfig(
                searchConfig: FaceSearchConfig(
                    threshold: 0.55,
                    topK: 3,
                    singleFaceOnly: false,
                    livenessRequired: false,
                    groupId: "store_shanghai_001"
                ),
                lensFacing: .front,
                enableRealtimeFaceBox: true,
                mirrorPreview: true
            )
        )
        cameraView.setListener(self)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // 发起硬件流式捕获与推理调度
        cameraView.start()
    }

    override func viewWillDisappear(_ animated: Bool) {
        // 中断并释放硬件与算法引擎通道，防止常驻内存与发热
        cameraView.stop()
        super.viewWillDisappear(animated)
    }

    func onMatched(_ result: FaceSearchResult) {
        let best = result.matches.first
        print("检索通道命中人员: \(best?.person.name ?? best?.person.personId ?? "")")
    }

    func onMostSimilar(_ result: FaceSearchResult) {
        let best = result.matches.first
        print("底库返回最贴近候选 (未达阈值): \(best?.person.name ?? best?.person.personId ?? "")")
    }

    func onFailed(code: FaceStatusCode, message: String) {
        print("通道执行异常中断: \(code), \(message)")
    }

    func onTips(code: FaceStatusCode, message: String) {
        // 向应用层透传视觉质量纠正指令（偏头、遮挡、光照等）
        print("需要反馈至交互层的指令: \(message)")
    }
}
```

**资源治理警告**：当承载视图脱离屏幕可见范围时，必须同步调用 `stop()` 中断调度中心的工作，避免出现高频空转引起的系统过载与 UIKit 状态崩溃。

---

## 7. 关键配置定义

### 7.1 搜索配置 `FaceSearchConfig`

`FaceSearchConfig` 用于图片 1:N 搜索、预置识别控制器和自定义相机识别 View。

```swift
FaceSearchConfig(
    threshold: 0.55,
    searchIntervalMs: 1200,
    topK: 3,
    singleFaceOnly: false,
    livenessRequired: false,
    groupId: "store_shanghai_001"
)
```

| 字段 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `threshold` | `0.55` | 命中阈值。Top1 相似度达到该值时，才可能认为匹配。 |
| `minThreshold` | `0.50` | 业务侧可调阈值下限，用于页面滑动条或配置校验。 |
| `maxThreshold` | `0.90` | 业务侧可调阈值上限，用于页面滑动条或配置校验。 |
| `searchIntervalMs` | `1200` | 相机流两次搜索之间的最短间隔。数值越小越实时，也越耗性能。 |
| `minSearchIntervalMs` | `0` | 业务侧可调搜索间隔下限。 |
| `maxSearchIntervalMs` | `9000` | 业务侧可调搜索间隔上限。 |
| `topK` | `3` | 返回最相似的前几个候选人。 |
| `singleFaceOnly` | `true` | 是否要求图中只有一张人脸。图片搜索建议 `true`，相机识别可用 `false`。 |
| `returnFaceImage` | `false` | 是否在搜索结果中返回裁剪人脸图。需要展示或调试时再开启。 |
| `returnFeature` | `false` | 是否在搜索结果中返回本次提取的特征。普通业务不建议开启。 |
| `livenessRequired` | `false` | 相机流无感自然度评分开关。开启后返回 LiveScore，但不作为强制活体门禁。 |
| `minTopKConfidenceGap` | `0.04` | Top1 与 Top2 的最小分差。分差太小代表识别结果不够确定。 |
| `confirmWindowMs` | `3000` | 多帧确认时间窗口。 |
| `confirmHitCount` | `2` | 时间窗口内同一个人需要命中的次数。 |
| `confirmMinBestScore` | `0.70` | 多帧确认时，单次最好分数至少要达到的值。 |
| `confirmMinAverageScore` | `0.55` | 多帧确认时，平均分至少要达到的值。 |
| `groupId` | `nil` | 指定只在某个分组内搜索。不传表示全库搜索。 |

常用调参只需要关注 `threshold`、`topK`、`singleFaceOnly`、`searchIntervalMs` 和 `groupId`。其他参数用于相机流稳定性和多帧确认，正式上线前应结合现场数据验证后再调整。

### 7.2 预置页面配置

`FaceRegisterCameraViewConfig`：

| 字段 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `personId` | `nil` | 业务人员 ID。不传时 SDK 自动生成。正式业务建议传业务自己的人员 ID。 |
| `name` | 无默认值 | 人员名称。 |
| `extra` | `nil` | 业务扩展字段，建议使用 JSON 字符串。 |
| `groupId` | `nil` | 人员所属分组。不传表示不指定分组。 |
| `lensFacing` | `.front` | 默认前置摄像头。 |
| `enableRealtimeFaceBox` | `true` | 是否启用实时人脸框。 |
| `mirrorPreview` | `true` | 前置摄像头预览是否镜像。 |
| `captureMode` | `.single` | 录入采集模式。`.single` 为单正脸模板，`.multiTemplate` 为多模板采集。 |
| `targetFeatureTemplateCount` | `1` | 目标模板数量。单模板模式固定按 1 处理；多模板模式建议 2 到 4。 |
| `autoCaptureTemplates` | `false` | 兼容字段。新接入建议使用 `captureMode` 表达是否多模板采集。 |

`.single` 是默认推荐模式，体验最短；`.multiTemplate` 会引导用户采集正脸、轻微左转、轻微右转和表情补充模板，用于提升复杂场景下的稳定性。多模板模式下如果不传 `targetFeatureTemplateCount`，SDK 默认按 4 个模板处理。

`FaceSearchViewControllerConfig`：

| 字段 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `searchConfig` | `FaceSearchConfig(singleFaceOnly: false)` | 识别配置。相机识别通常允许多脸，由 SDK 选择主体人脸。 |
| `lensFacing` | `.front` | 默认前置摄像头。 |
| `title` | `"人脸识别"` | 页面标题。 |
| `finishOnMatched` | `true` | 命中后是否自动结束页面。 |

`FaceLivenessViewControllerConfig`：

| 字段 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `title` | `"活体检测"` | 页面标题。 |
| `motionConfig` | `FaceMotionLivenessConfig()` | 动作活体配置。 |
| `enableSound` | `true` | 是否启用声音提示。 |

`FaceMotionLivenessConfig`：

| 字段 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `stepCount` | `3` | 本轮需要完成的动作数量，范围为 1 到全部动作数。 |
| `timeoutSeconds` | `10` | 活体总超时时间，内部限制在 5 到 60 秒。 |
| `customSteps` | `nil` | 自定义动作序列。不传时 SDK 随机选择动作。 |

---

## 8. 返回结果规范语义

SDK 的接口统一采用结构化的 `success + code + message + Data` 规约进行数据分发。

### 8.1 常用方法返回值

| 方法 | 返回类型 | 说明 |
| :--- | :--- | :--- |
| `FaceSdk.initialize(...)` | `FaceSdkResult` | 初始化是否成功。失败时看 `code` 和 `message`。 |
| `FaceSdk.getLicenseInfo()` | `FaceLicenseInfo?` | 初始化成功后可读取授权有效期。未初始化或授权未加载时为空。 |
| `FaceSdk.personManager()?.register(...)` | `FacePersonResult` | 注册结果。成功时 `person` 为已保存人员。 |
| `FaceSdk.personManager()?.get(...)` | `FacePersonResult` | 查询单人结果。未找到时返回 `personNotFound`。 |
| `FaceSdk.personManager()?.list(...)` | `FacePersonListResult` | 人员列表结果。成功时读取 `persons`。 |
| `FaceSdk.personManager()?.listFaceFeatures(personId:)` | `FaceFeatureTemplateListResult` | 指定人员的全部特征模板。 |
| `FaceSdk.personManager()?.count(...)` | `FacePersonCountResult` | 人数统计结果。成功时读取 `count`。 |
| `FaceSdk.personManager()?.delete(...)` | `FaceSdkResult` | 删除是否成功。 |
| `FaceSdk.personManager()?.clear(...)` | `FaceSdkResult` | 清空是否成功。 |
| `FaceSdk.recognitionEngine().extractFeature(...)` | `FaceFeatureResult` | 特征提取结果。成功时读取 `feature`。 |
| `FaceSdk.recognitionEngine().compare(...)` | `FaceCompareResult` | 1:1 比对结果。成功后看 `matched`。 |
| `FaceSdk.recognitionEngine().search(...)` | `FaceSearchResult` | 1:N 搜索结果。成功时读取 `matches`。 |
| `FaceRegisterViewController` / `FaceSearchViewController` | `FaceActivityResult` | 预置录入和识别页面回调结果。 |
| `FaceLivenessViewController` | `FaceLivenessActivityResult` | 动作活体页面回调结果。 |

### 8.2 原子通用响应 `FaceSdkResult`

| 实体字段  | 语义规范                                   |
| :-------- | :----------------------------------------- |
| `success` | 标识业务请求的最终完成态（Boolean）。      |
| `code`    | 与该结果绑定的系统状态枚举或分类编码。     |
| `message` | 用于生产追溯及排障的描述性负载，允许为空。 |

包括初始化、持久化操作、清库等管理类动作默认抛出此结构。

### 8.3 授权信息 `FaceLicenseInfo`

| 实体字段 | 语义规范 |
| :--- | :--- |
| `validUntilEpochSeconds` | SDK 校验后的授权有效期，Unix 秒级时间戳。 |

该对象仅用于业务侧展示授权有效期或做运维提示，不需要也不建议业务侧自行解析 license 文本。

### 8.4 人员业务实体 `FacePerson`

| 实体字段                      | 语义规范                                     |
| :---------------------------- | :------------------------------------------- |
| `personId`                    | 接入方注册的主键 ID。                        |
| `name`                        | 用于呈现的人员别名。                         |
| `feature`                     | 主特征模板。                                 |
| `groupId`                     | 支持数据隔离的租户标识。                     |
| `extra`                       | 支持强定制扩展的透传报文（约定为 JSON 串）。 |
| `avatarBytes`                 | 人员头像缩略图字节，可能为空。               |
| `featureCount`                | 该人员当前特征模板数量。                     |
| `createdAtMs` / `updatedAtMs` | 数据库层级自动映射的时间戳标记。             |

### 8.5 人脸库结果

`FacePersonResult`：

| 实体字段 | 语义规范 |
| :--- | :--- |
| `success` | 人员操作是否成功。 |
| `code` | 状态码。 |
| `message` | 详情信息。 |
| `person` | 单个人员结果。注册或查询成功时返回。 |

`FacePersonListResult`：

| 实体字段 | 语义规范 |
| :--- | :--- |
| `success` | 列表查询是否成功。 |
| `code` | 状态码。 |
| `message` | 详情信息。 |
| `persons` | 人员列表。 |

`FacePersonCountResult`：

| 实体字段 | 语义规范 |
| :--- | :--- |
| `success` | 统计是否成功。 |
| `code` | 状态码。 |
| `message` | 详情信息。 |
| `count` | 人员数量。 |

`FaceFeatureTemplateListResult`：

| 实体字段 | 语义规范 |
| :--- | :--- |
| `success` | 特征模板列表查询是否成功。 |
| `code` | 状态码。人员不存在时返回 `personNotFound`。 |
| `message` | 详情信息。 |
| `templates` | 指定人员的全部特征模板，通常按 `slotIndex` 和创建时间稳定排序。 |

`FaceFeatureTemplate`：

| 实体字段 | 语义规范 |
| :--- | :--- |
| `featureId` | 特征模板 ID，可用于定位单个模板。 |
| `personId` | 所属人员 ID。 |
| `slotType` | 模板类型：`FRONT`、`YAW_LEFT`、`YAW_RIGHT`、`EXPRESSION`、`CUSTOM`。 |
| `slotIndex` | 模板顺序索引，主脸通常为 `0`。 |
| `feature` | 该模板对应的人脸特征。 |
| `qualityScore` | 采集质量分，可能为空。 |
| `avatarBytes` | 该模板头像缩略图字节，可能为空。 |
| `createdAtMs` | 模板创建时间。 |

### 8.5 控制器总线回调 `FaceActivityResult`

| 实体字段            | 语义规范                                   |
| :------------------ | :----------------------------------------- |
| `success`           | 标识预置控制器的整个生命周期是否异常终止。 |
| `code`              | 控制器结束时的主导状态码。                 |
| `personId` / `name` | 绑定落库主体或命中的主体信息。             |
| `matched`           | 布尔值标识，判定本次流程是否达标。         |
| `similarity`        | 引擎输出的标准欧氏转余弦相似度指标。       |
| `threshold`         | 触发当前分类结果的核心比较基准。           |

### 8.6 活体页面回调 `FaceLivenessActivityResult`

| 实体字段 | 语义规范 |
| :--- | :--- |
| `success` | 活体流程是否通过。 |
| `code` | 状态码。 |
| `message` | 详情信息。 |
| `imageFileURL` | 活体流程保存的现场图片路径，可能为空。 |

### 8.7 特征提取回调 `FaceFeatureResult`

| 实体字段 | 语义规范 |
| :--- | :--- |
| `success` | 是否成功提取特征。 |
| `code` | 状态码。 |
| `message` | 详情信息。 |
| `feature` | 人脸特征。普通业务不需要直接保存。 |

### 8.8 比对算法回调 `FaceCompareResult`

| 实体字段     | 语义规范                                        |
| :----------- | :---------------------------------------------- |
| `success`    | 标识底层的检测与解码通道没有报错。              |
| `code`       | 系统层级状态码。                                |
| `similarity` | 标定相似度得分。                                |
| `threshold`  | 标定阈值。                                      |
| `matched`    | 是否满足 `similarity >= threshold` 的公式判定。 |

### 8.9 召回引擎回调 `FaceSearchResult`

| 实体字段 | 语义规范 |
| :--- | :--- |
| `success` | 标识是否成功从持久化池中寻获达标目标。 |
| `code` | 系统级状态码。未找到符合预期目标时抛出 `noMatched`。 |
| `message` | 详情信息。 |
| `matches` | 经过引擎过滤并按降序排序返回的 TopK 队列。 |
| `liveScore` | 开启无感 LiveScore 时返回的自然度参考分，可能为空。 |
| `liveScoreReady` | 是否已达到统计窗口要求。 |
| `liveScoreWindowMs` | 本次评分使用的时间窗口。 |

`FaceSearchMatch` 切片详情：

| 实体字段     | 语义规范                       |
| :----------- | :----------------------------- |
| `person`     | 反序列化后的被匹配人员。       |
| `similarity` | 综合比对打分。                 |
| `threshold`  | 本次搜索阈值。                 |
| `rank`       | 在同批次排序管道中的绝对位次。 |
| `matched`    | 指示单体是否击穿业务阈值线。   |

---

## 9. 规范化异常处理体系

所有报错与异常中断，均有明确的归因与应对方案指导：

| 分类     | 状态码枚举                                                                         | 工程治理策略                                                                                                 |
| :------- | :--------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------- |
| 生命管控 | `sdkNotInitialized`                                                                | 前置拦截，确保业务启动初期成功挂载 `FaceSdk.initialize()`。                                                  |
| 鉴权验证 | `licenseNotFound`、`licenseInvalid`、`licenseExpired`、`licensePackageMismatch`    | 此类属于高频交付缺陷。必须通过 CI/CD 或人工检查防错，确认打包证书对应正确的 Bundle Identifier 与未过期文件。 |
| 视觉质量 | `faceNotFound`、`multipleFaces`、`faceTooSmall`、`faceTooLarge`、`faceNotCentered` | 属于常规的人机交互边界提示。需在业务层解耦映射为“靠近屏幕”、“请勿多人入镜”等自然语言指导。                   |
| 成像评估 | `faceAngleTooLarge`、`landmarkMissing`、`qualityNotPass`                           | 通常因暗光、硬阴影、强逆光、或严重五官遮挡引发底层特征瘫痪。需引导用户改善物理环境。                         |
| 数据调度 | `faceLibraryEmpty`、`personNotFound`、`personAlreadyExists`、`noMatched`           | 数据逻辑状态。需通过对账系统的机制检查注册分发链路。                                                         |
| 动作活体 | `livenessWaiting`、`livenessTimeout`、`livenessFailed`、`livenessPassed`           | 内部流转状态，将事件映射交由相关业务状态机接管。                                                             |
| 硬件驱动 | `cameraPermissionDenied`、`unsupportedCamera`、`cameraError`                       | 触及底层驱动限制或系统强管控拦截，需提供明确降级体验或弹窗引流至隐私设置页。                                 |
| 交互中断 | `userCancelled`                                                                    | 正常的界面退出与手势干预事件，业务逻辑通常直接静默放行。                                                     |
| 系统崩溃 | `inputImageError`、`configInvalid`、`operationTimeout`、`internalError`            | 指向显式的入参污染或系统层级（如资源干涸、模型腐烂）崩溃，需即刻转储堆栈并反馈跟进。                         |

---

## 10. 生产环境部署须知

1. **降低心智负担**：优先依赖高内聚的组件（`FaceRegisterViewController`、`FaceSearchViewController`）能够显著缩短业务上线周期并回避多线程管理雷区。
2. **严防主线程阻塞**：`personManager()` 的绝大部分写操作（注册更新）涉及繁重的图像张量转换及 SQLite I/O。批量作业必须显式派发至异步串行队列进行，绝对禁止在主线程中引发阻塞及丢帧。
3. **把控录入基准**：前端图像的采集质量将呈指数级影响特征向量的后续判定表现。务必在落库侧执行最严苛的光照、清晰度与角度准入标准，杜绝劣质特征污染公共检索池。
4. **合理规划活体防线**：内置视频流无感活体（`livenessRequired`）的拦截率天然受到算力与光照条件的限制，仅能作为辅助降级手段。在核心签到、支付门禁业务中，必须前置接驳具备独立防伪算法的交互式动作活体检测。
5. **参数收敛原则**：SDK 已在海量测试数据的印证下锁定了所有核心算法超参数的最佳实践阈值。非特例研发攻坚，严禁业务系统私自篡改配置默认参数映射，以免诱发雪崩级识别故障。
