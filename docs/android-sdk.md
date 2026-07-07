# Ainuo Face Android SDK 使用文档

版本：v0.1.0

本文面向 Android 业务接入方，目标是说明 SDK 怎么集成、怎么初始化、怎么录入、怎么识别、怎么管理本地人脸库。

SDK 已内置人脸检测、特征提取、本地人脸库、图片 1:1 比对、图片 1:N 搜索、相机实时 1:N 搜索和动作活体页面。接入方通常不需要关心模型、检测器、数据库、底层特征向量等内部配置，优先使用默认配置即可。

---

## 1. 快速接入

### 1.1 引入 SDK

**本地 AAR 接入**

将交付的 `ainuo-face-android-sdk.aar` 放到业务工程 `app/libs` 目录。使用本地 AAR 时，业务工程需要补齐 SDK 依赖：

```kotlin
dependencies {
    implementation(files("libs/ainuo-face-android-sdk.aar"))

    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.gms:play-services-tasks:18.2.0")
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")
    implementation("androidx.room:room-runtime:2.5.2")
}
```

**Maven 接入**

如果 SDK 发布到私有 Maven 仓库，可以使用：

```kotlin
dependencies {
    implementation("com.ainuo.face:face-android-sdk:0.1.0")
}
```

### 1.2 声明相机权限

使用内置相机页面、活体页面或自定义相机识别时，需要声明相机权限：

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

运行时也需要按 Android 权限规则申请 `CAMERA`。SDK 内置页面会返回 `CAMERA_PERMISSION_DENIED`，但建议业务侧在进入页面前先申请权限。

### 1.3 初始化 SDK

建议在 `Application#onCreate()`、启动页，或首次进入人脸业务前完成初始化。

对外 Demo 不内置授权文件。业务侧应通过服务端下发、私有配置或构建参数拿到授权文本，再以字符串形式传入 SDK：

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val licenseText = "<your-license-text>"

        val result = FaceSdk.initialize(
            context = this,
            config = FaceSdkConfig(),
            licenseKey = licenseText
        )

        if (!result.success) {
            Log.e("FaceSDK", "初始化失败: ${result.code}, ${result.message}")
        }
    }
}
```

初始化成功后再使用人脸库、图片比对、图片搜索、相机识别和活体页面。

```kotlin
val ready = FaceSdk.isInitialized()
val sdkVersion = FaceSdk.getVersion()
val licenseInfo = FaceSdk.getLicenseInfo()
val validUntil = licenseInfo?.validUntilEpochSeconds

FaceSdk.release()
```

---

## 2. 常用方法总览

### 2.1 SDK 生命周期

| 方法                                                       | 用途                           |
| :--------------------------------------------------------- | :----------------------------- |
| `FaceSdk.initialize(context, FaceSdkConfig(), licenseKey)` | 使用授权文本初始化 SDK。       |
| `FaceSdk.isInitialized()`                                  | 判断 SDK 是否已初始化。        |
| `FaceSdk.getVersion()`                                     | 获取 SDK 版本。                |
| `FaceSdk.getLicenseInfo()`                                 | 获取 SDK 校验后的授权有效期。  |
| `FaceSdk.release()`                                        | 释放 SDK 资源。                |
| `FaceSdk.personManager()`                                  | 获取本地人脸库管理对象。       |
| `FaceSdk.recognitionEngine()`                              | 获取图片检测、比对、搜索能力。 |

### 2.2 内置相机页面

推荐先使用 `FaceCameraKit` 跑通完整流程，SDK 会处理 CameraX、预览、人脸框、提示、注册、搜索和页面返回。

| 方法                                                   | 用途                                                        |
| :----------------------------------------------------- | :---------------------------------------------------------- |
| `FaceCameraKit.createRegisterIntent(context, config)`  | 创建内置相机录入页 Intent，可通过 ActivityResult 获取结果。 |
| `FaceCameraKit.createSearchIntent(context, config)`    | 创建内置相机识别页 Intent，可通过 ActivityResult 获取结果。 |
| `FaceCameraKit.createLivenessIntent(context, config)`  | 创建独立动作活体页 Intent。                                 |
| `FaceCameraKit.startRegisterActivity(context, config)` | 直接启动录入页，不关心 ActivityResult 时使用。              |
| `FaceCameraKit.startSearchActivity(context, config)`   | 直接启动识别页，不关心 ActivityResult 时使用。              |
| `FaceCameraKit.startLivenessActivity(context, config)` | 直接启动活体页，不关心 ActivityResult 时使用。              |
| `FaceActivityResult.fromIntent(data)`                  | 解析内置页面返回结果。                                      |

### 2.3 人脸库管理

通过 `FaceSdk.personManager()` 使用。

| 方法                                               | 用途                                       |
| :------------------------------------------------- | :----------------------------------------- |
| `register(personId, name, bitmap)`                 | 用一张图片注册人员。                       |
| `register(personId, name, bitmap, extra)`          | 注册人员，并保存业务扩展字段。             |
| `register(personId, name, bitmap, groupId, extra)` | 注册人员到指定分组，并保存业务扩展字段。   |
| `get(personId)`                                    | 查询单个人员。                             |
| `list()`                                           | 查询全部人员。人数较多时建议使用分页方法。 |
| `list(groupId)`                                    | 查询指定分组的人员。                       |
| `list(offset, limit)`                              | 分页查询人员。                             |
| `list(groupId, offset, limit)`                     | 分页查询指定分组的人员。                   |
| `listFaceFeatures(personId)`                       | 查询指定人员的全部特征模板。               |
| `count()`                                          | 查询当前人脸库人数。                       |
| `count(groupId)`                                   | 查询指定分组人数。                         |
| `delete(personId)`                                 | 删除指定人员。                             |
| `clear()`                                          | 清空人脸库，业务侧建议加二次确认。         |
| `clear(groupId)`                                   | 清空指定分组，其他分组不受影响。           |

### 2.4 图片识别

通过 `FaceSdk.recognitionEngine()` 使用。

| 方法                          | 用途                                               |
| :---------------------------- | :------------------------------------------------- |
| `detect(bitmap)`              | 检测图片中的人脸。                                 |
| `extractFeature(bitmap)`      | 从图片中提取人脸特征。一般业务不需要直接保存特征。 |
| `compare(bitmap1, bitmap2)`   | 图片 1:1 比对。                                    |
| `compare(feature1, feature2)` | 特征 1:1 比对。仅在业务已经自行管理特征时使用。    |
| `search(bitmap)`              | 图片 1:N 搜索本地人脸库。                          |
| `search(bitmap, config)`      | 带搜索配置的图片 1:N 搜索。                        |
| `searchTopK(bitmap, topK)`    | 返回最相似的 TopK 人员。                           |

### 2.5 自定义相机识别

如果内置识别页不满足 UI 需求，优先使用 `FaceSearchCameraView`。

| 方法                                           | 用途                                 |
| :--------------------------------------------- | :----------------------------------- |
| `bind(lifecycleOwner)`                         | 绑定生命周期。                       |
| `configure(config)`                            | 设置识别配置、前后摄像头和预览方式。 |
| `setFaceSearchCameraListener(listener)`        | 设置识别回调。                       |
| `start(lifecycleOwner, config, listener)`      | 一次性绑定、配置并启动识别。         |
| `start()`                                      | 按已绑定和已配置参数启动识别。       |
| `pauseSearch()` / `resumeSearch()`             | 暂停或恢复搜索。                     |
| `switchCamera()` / `setLensFacing(lensFacing)` | 切换前后摄像头。                     |
| `stop()`                                       | 停止相机预览和搜索。                 |
| `release()`                                    | 页面销毁时释放资源。                 |

---

## 3. 内置页面使用

### 3.1 相机录入人员

```kotlin
private val registerLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val faceResult = FaceActivityResult.fromIntent(result.data)
        if (faceResult.success) {
            Toast.makeText(this, "录入成功: ${faceResult.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "录入失败: ${faceResult.code}", Toast.LENGTH_SHORT).show()
        }
    }

fun startFaceRegister() {
    val intent = FaceCameraKit.createRegisterIntent(
        context = this,
        config = FaceRegisterUiConfig(
            personId = "user_10086",
            name = "张三",
            extraJson = """{"department":"研发部"}""",
            groupId = "store_shanghai_001",
            uiConfig = FaceUiConfig(title = "人脸录入")
        )
    )

    registerLauncher.launch(intent)
}
```

录入成功后，SDK 会把人员信息和人脸特征保存到本地人脸库。

默认录入页为单人单特征模式，只采集一条正脸模板。需要提升儿童、角度变化或表情变化场景下的识别稳定性时，可以显式开启多模板采集：

```kotlin
val intent = FaceCameraKit.createRegisterIntent(
    context = this,
    config = FaceRegisterUiConfig(
        personId = "user_10086",
        name = "张三",
        captureMode = FaceRegisterCaptureMode.MULTI_TEMPLATE,
        targetFeatureTemplateCount = 4
    )
)
```

多模板采集仍然只允许画面中有一个人；它表示“同一个人采多条特征模板”，不是多人同时录入。

### 3.2 相机识别人员

```kotlin
private val searchLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val faceResult = FaceActivityResult.fromIntent(result.data)
        if (faceResult.success && faceResult.matched) {
            Toast.makeText(this, "识别成功: ${faceResult.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "识别失败: ${faceResult.code}", Toast.LENGTH_SHORT).show()
        }
    }

fun startFaceSearch() {
    val intent = FaceCameraKit.createSearchIntent(
        context = this,
        config = FaceSearchUiConfig(
            searchConfig = FaceSearchConfig(
                threshold = 0.55f,
                topK = 3,
                singleFaceOnly = false,
                groupId = "store_shanghai_001"
            ),
            uiConfig = FaceUiConfig(title = "人脸识别"),
            finishOnMatched = true
        )
    )

    searchLauncher.launch(intent)
}
```

`threshold` 是命中阈值，`topK` 是返回候选数量。正式业务建议结合设备、光线、底库规模和误识/拒识要求重新标定阈值。

### 3.3 动作活体页面

如果业务需要“先活体、再识别”，可以先启动独立活体页；活体通过后，再启动普通识别页。

```kotlin
private val livenessLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val faceResult = FaceActivityResult.fromIntent(result.data)
        if (faceResult.success) {
            startFaceSearch()
        } else {
            Toast.makeText(this, "活体失败: ${faceResult.code}", Toast.LENGTH_SHORT).show()
        }
    }

fun startLiveness() {
    val intent = FaceCameraKit.createLivenessIntent(
        context = this,
        config = FaceLivenessUiConfig(
            uiConfig = FaceUiConfig(title = "活体检测", enableSound = true)
        )
    )

    livenessLauncher.launch(intent)
}
```

### 3.4 解析页面返回结果

内置录入页、识别页、活体页都可以用同一个方式解析：

```kotlin
val result = FaceActivityResult.fromIntent(data)
```

| 字段         | 说明                                               |
| :----------- | :------------------------------------------------- |
| `success`    | 页面流程是否成功。                                 |
| `code`       | 状态码。成功为 `OK`，用户关闭为 `USER_CANCELLED`。 |
| `message`    | 详情信息，可能为空。                               |
| `personId`   | 注册或识别到的人员 ID。                            |
| `name`       | 注册或识别到的人员名称。                           |
| `matched`    | 识别是否命中。录入和活体场景一般不看。             |
| `similarity` | 识别相似度。                                       |
| `threshold`  | 本次识别阈值。                                     |
| `imageUri`   | 内置页面返回的人脸图片 Uri，可能为空。             |

---

## 4. 人脸库使用

人脸库操作涉及数据库和特征提取，建议放到 IO 线程执行。

### 4.1 注册人员

```kotlin
val result = FaceSdk.personManager().register(
    personId = "user_10086",
    name = "张三",
    bitmap = faceBitmap,
    groupId = "store_shanghai_001",
    extra = """{"department":"研发部"}"""
)

if (result.success) {
    Log.i("FaceSDK", "注册成功: ${result.person?.personId}")
} else {
    Log.e("FaceSDK", "注册失败: ${result.code}, ${result.message}")
}
```

`personId` 由业务侧生成并保证唯一。图片里没有人脸、人脸太小、角度过大或质量不合格时，注册会失败。

### 4.2 查询、列表和数量

```kotlin
val one = FaceSdk.personManager().get("user_10086")
val page = FaceSdk.personManager().list(offset = 0, limit = 50)
val count = FaceSdk.personManager().count()
```

```kotlin
if (page.success) {
    page.persons.forEach { person ->
        Log.i("FaceSDK", "${person.personId} ${person.name}")
    }
}
```

`list()` / `list(offset, limit)` 返回人员摘要列表。对于多模板人员，`FacePerson.featureCount` 表示当前模板数量，`primaryFeature` / `feature` 仅代表主模板；如需读取正脸、左右转头、表情等全部模板，请使用 `listFaceFeatures(personId)`。

```kotlin
val templates = FaceSdk.personManager().listFaceFeatures("user_10086")
if (templates.success) {
    templates.templates.forEach { template ->
        Log.i("FaceSDK", "${template.slotType} ${template.feature.dimension}维")
    }
}
```

### 4.3 删除与清空

```kotlin
val deleteResult = FaceSdk.personManager().delete("user_10086")

val clearResult = FaceSdk.personManager().clear()
```

`delete(personId)`、`clear()` 和 `clear(groupId)` 会同时删除人员记录和该人员关联的全部特征模板，不会只删除主脸。`clear()` 会删除整个人脸库，建议业务侧加二次确认。

### 4.4 分组使用

如果业务有门店、部门、租户等隔离需求，可以传入 `groupId`。不传 `groupId` 时，列表、数量、清空和搜索仍按全库处理。

```kotlin
val groupId = "store_shanghai_001"

FaceSdk.personManager().register(
    personId = "user_10086",
    name = "张三",
    bitmap = faceBitmap,
    groupId = groupId,
    extra = """{"department":"研发部"}"""
)

val groupPersons = FaceSdk.personManager().list(groupId)
val groupCount = FaceSdk.personManager().count(groupId)
```

指定分组搜索：

```kotlin
val result = FaceSdk.recognitionEngine().search(
    bitmap = queryBitmap,
    config = FaceSearchConfig(
        threshold = 0.55f,
        topK = 3,
        groupId = groupId
    )
)
```

清空指定分组：

```kotlin
FaceSdk.personManager().clear(groupId)
```

`personId` 在本地人脸库内保持唯一，不能在不同 `groupId` 下重复注册同一个 `personId`。`groupId` 不能是空字符串或纯空白字符串。

---

## 5. 图片能力使用

图片接口适合相册图片、人证比对、上传照片搜索等场景。

### 5.1 人脸检测

```kotlin
val result = FaceSdk.recognitionEngine().detect(bitmap)

if (result.success) {
    val firstFace = result.faces.first()
    Log.i("FaceSDK", "face=${firstFace.left},${firstFace.top},${firstFace.right},${firstFace.bottom}")
} else {
    Log.w("FaceSDK", "未检测到人脸: ${result.code}")
}
```

### 5.2 图片 1:1 比对

```kotlin
val result = FaceSdk.recognitionEngine().compare(bitmapA, bitmapB)

if (result.success) {
    Log.i("FaceSDK", "similarity=${result.similarity}, matched=${result.matched}")
} else {
    Log.w("FaceSDK", "比对失败: ${result.code}")
}
```

`success=true` 表示两张图片都成功提取到人脸并完成比对；是否同一个人看 `matched`。

### 5.3 图片 1:N 搜索

```kotlin
val result = FaceSdk.recognitionEngine().search(
    bitmap = queryBitmap,
    config = FaceSearchConfig(
        threshold = 0.55f,
        topK = 3,
        singleFaceOnly = true,
        groupId = "store_shanghai_001"
    )
)

if (result.success) {
    val top1 = result.matches.firstOrNull()
    Log.i("FaceSDK", "命中: ${top1?.person?.name}, similarity=${top1?.similarity}")
} else if (result.code == FaceStatusCode.NO_MATCHED) {
    Log.i("FaceSDK", "没有达到阈值的人员")
} else {
    Log.w("FaceSDK", "搜索失败: ${result.code}")
}
```

`matches` 按相似度从高到低排列。一般业务取 `matches.firstOrNull()` 作为最相似人员。

### 5.4 特征提取和特征比对

大多数业务不需要直接处理特征；如果你们已经有自己的特征存储或迁移流程，可以使用：

```kotlin
val featureResult = FaceSdk.recognitionEngine().extractFeature(bitmap)

if (featureResult.success) {
    val feature = featureResult.feature
}
```

```kotlin
val compareResult = FaceSdk.recognitionEngine().compare(featureA, featureB)
```

不同 SDK 版本或不同模型生成的特征不建议混用。正式接入如需长期保存特征，请先确认升级和重录策略。

---

## 6. 自定义相机识别

如果内置识别页不符合业务 UI，可以嵌入 `FaceSearchCameraView`。它已经封装 CameraX、预览、人脸框和流式搜索，业务只需要处理回调。

### 6.1 XML 布局

```xml
<com.ainuo.face.sdk.ui.FaceSearchCameraView
    android:id="@+id/faceSearchCameraView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 6.2 启动识别

```kotlin
val cameraView = findViewById<FaceSearchCameraView>(R.id.faceSearchCameraView)

cameraView.start(
    lifecycleOwner = this,
    config = FaceSearchCameraViewConfig(
        searchConfig = FaceSearchConfig(
            threshold = 0.55f,
            topK = 3,
            singleFaceOnly = false,
            groupId = "store_shanghai_001"
        ),
        lensFacing = CameraSelector.LENS_FACING_FRONT,
        enableRealtimeFaceBox = true
    ),
    listener = object : FaceSearchCameraViewListenerAdapter() {
        override fun onReady() {
            Log.i("FaceSDK", "相机识别已启动")
        }

        override fun onFaceDetected(face: FaceDetectResult) {
            Log.i("FaceSDK", "检测到人脸数: ${face.faces.size}")
        }

        override fun onMostSimilar(result: FaceSearchResult) {
            val top1 = result.matches.firstOrNull()
            Log.i("FaceSDK", "最相似: ${top1?.person?.name}, ${top1?.similarity}")
        }

        override fun onMatched(result: FaceSearchResult) {
            val top1 = result.matches.firstOrNull()
            Log.i("FaceSDK", "最终命中: ${top1?.person?.name}, ${top1?.similarity}")
        }

        override fun onFailed(code: FaceStatusCode, message: String) {
            Log.w("FaceSDK", "识别失败: $code, $message")
        }

        override fun onTips(code: FaceStatusCode, message: String) {
            Log.d("FaceSDK", "过程提示: $code, $message")
        }
    }
)
```

### 6.3 释放资源

```kotlin
override fun onDestroy() {
    cameraView.release()
    super.onDestroy()
}
```

### 6.4 常用回调

| 回调                      | 说明                                       |
| :------------------------ | :----------------------------------------- |
| `onReady()`               | 搜索流启动成功。                           |
| `onFaceDetected(face)`    | 检测到人脸框，可用于更新提示。             |
| `onMostSimilar(result)`   | 找到 TopK 候选，但还没完成最终确认。       |
| `onMatched(result)`       | 通过阈值和多帧确认，通常可认为识别命中。   |
| `onFailed(code, message)` | 搜索失败、未命中、空库、相机异常等。       |
| `onTips(code, message)`   | 过程提示，如无人脸、人脸太小、角度不合格。 |

---

## 7. 可选配置

大多数接入只需要 `FaceSdkConfig()` 默认值。业务常用的可选配置只有下面几类。

### 7.1 搜索配置 `FaceSearchConfig`

```kotlin
FaceSearchConfig(
    threshold = 0.55f,
    topK = 3,
    singleFaceOnly = false,
    searchIntervalMs = 2000,
    livenessRequired = false,
    groupId = "store_shanghai_001"
)
```

| 字段                     | 默认值  | 说明                                                                   |
| :----------------------- | :------ | :--------------------------------------------------------------------- |
| `threshold`              | `0.55f` | 命中阈值。Top1 相似度达到该值时，才可能认为匹配。                      |
| `minThreshold`           | `0.50f` | 业务侧可调阈值下限，用于页面滑动条或配置校验。                         |
| `maxThreshold`           | `0.90f` | 业务侧可调阈值上限，用于页面滑动条或配置校验。                         |
| `topK`                   | `3`     | 返回最相似的前几个候选人。                                             |
| `singleFaceOnly`         | `true`  | 是否要求图中只有一张人脸。图片搜索建议 `true`，相机识别可用 `false`。  |
| `searchIntervalMs`       | `800`   | 相机流两次搜索之间的最短间隔。数值越小越实时，也越耗性能。             |
| `minSearchIntervalMs`    | `0`     | 业务侧可调搜索间隔下限。                                               |
| `maxSearchIntervalMs`    | `9000`  | 业务侧可调搜索间隔上限。                                               |
| `returnFaceBitmap`       | `false` | 是否在搜索结果中返回裁剪人脸图。需要展示或调试时再开启。               |
| `returnFeature`          | `false` | 是否在搜索结果中返回本次提取的特征。普通业务不建议开启。               |
| `livenessRequired`       | `false` | 相机流无感自然度评分开关。开启后返回 LiveScore，但不作为强制活体门禁。 |
| `minTopKConfidenceGap`   | `0.04f` | Top1 与 Top2 的最小分差。分差太小代表识别结果不够确定。                |
| `confirmWindowMs`        | `3000`  | 多帧确认时间窗口。                                                     |
| `confirmHitCount`        | `2`     | 时间窗口内同一个人需要命中的次数。                                     |
| `confirmMinBestScore`    | `0.70f` | 多帧确认时，单次最好分数至少要达到的值。                               |
| `confirmMinAverageScore` | `0.55f` | 多帧确认时，平均分至少要达到的值。                                     |
| `groupId`                | `null`  | 指定只在某个分组内搜索。不传表示全库搜索。                             |

常用调参只需要关注 `threshold`、`topK`、`singleFaceOnly`、`searchIntervalMs` 和 `groupId`。其他参数用于相机流稳定性和多帧确认，正式上线前应结合现场数据验证后再调整。

### 7.2 UI 配置 `FaceUiConfig`

```kotlin
FaceUiConfig(
    title = "人脸识别",
    themeColor = 0xFF1677FF.toInt(),
    showFaceBox = true,
    showTips = true,
    showCloseButton = true,
    enableSound = false,
    autoFinishOnSuccess = true
)
```

| 字段                  | 说明                             |
| :-------------------- | :------------------------------- |
| `title`               | 页面标题。                       |
| `themeColor`          | 页面主题色。                     |
| `showFaceBox`         | 是否显示人脸框。                 |
| `showTips`            | 是否显示过程提示。               |
| `showCloseButton`     | 是否显示关闭按钮。               |
| `enableSound`         | 是否启用声音提示，活体页可使用。 |
| `autoFinishOnSuccess` | 成功后是否自动关闭页面。         |

### 7.3 录入页配置 `FaceRegisterUiConfig`

```kotlin
FaceRegisterUiConfig(
    personId = "user_10086",
    name = "张三",
    captureMode = FaceRegisterCaptureMode.SINGLE,
    targetFeatureTemplateCount = 1
)
```

| 字段                         | 默认值     | 说明                                                                 |
| :--------------------------- | :--------- | :------------------------------------------------------------------- |
| `personId`                   | 无默认值   | 业务人员 ID。                                                        |
| `name`                       | 无默认值   | 人员名称。                                                           |
| `extraJson`                  | `null`     | 业务扩展字段，建议使用 JSON 字符串。                                 |
| `groupId`                    | `null`     | 人员所属分组。不传表示不指定分组。                                   |
| `qualityConfig`              | 严格模式   | 录入时的人脸质量配置。                                               |
| `uiConfig`                   | 录入页标题 | 录入页 UI 配置。                                                     |
| `captureMode`                | `SINGLE`   | 录入采集模式。`SINGLE` 为单正脸模板，`MULTI_TEMPLATE` 为多模板采集。 |
| `targetFeatureTemplateCount` | `1`        | 目标模板数量。单模板模式固定按 1 处理；多模板模式建议 2 到 4。       |
| `autoCaptureTemplates`       | `false`    | 兼容字段。新接入建议使用 `captureMode` 表达是否多模板采集。          |

`SINGLE` 是默认推荐模式，体验最短；`MULTI_TEMPLATE` 会引导用户采集正脸、轻微左转、轻微右转和表情补充模板，用于提升复杂场景下的稳定性。多模板模式下如果不传 `targetFeatureTemplateCount`，SDK 默认按 4 个模板处理。

### 7.4 无感 LiveScore

`livenessRequired=true` 只用于相机流连续帧自然度评分，不是强活体门禁，也不会阻断识别结果。

```kotlin
FaceSearchConfig(
    livenessRequired = true,
    threshold = 0.55f,
    topK = 3
)
```

开启后，可以在 `FaceSearchResult` 读取：

| 字段                | 说明                                                 |
| :------------------ | :--------------------------------------------------- |
| `liveScore`         | `0.0~1.0` 的连续帧自然度参考分，样本不足时可能为空。 |
| `liveScoreReady`    | 是否已达到统计窗口要求。                             |
| `liveScoreWindowMs` | 本次评分使用的时间窗口。                             |

如果业务需要强制真人校验，建议先使用独立动作活体页，通过后再进行识别。

---

## 8. 返回结果说明

SDK 大部分结果都遵循 `success + code + message + 业务数据`。

### 8.1 常用方法返回值

| 方法                                              | 返回类型                | 说明                                                       |
| :------------------------------------------------ | :---------------------- | :--------------------------------------------------------- |
| `FaceSdk.initialize(...)`                         | `FaceSdkResult`         | 初始化是否成功。失败时看 `code` 和 `message`。             |
| `FaceSdk.getLicenseInfo()`                        | `FaceLicenseInfo?`      | 初始化成功后可读取授权有效期。未初始化或授权未加载时为空。 |
| `FaceSdk.personManager().register(...)`           | `FacePersonResult`      | 注册结果。成功时 `person` 为已保存人员。                   |
| `FaceSdk.personManager().get(...)`                | `FacePersonResult`      | 查询单人结果。未找到时返回 `PERSON_NOT_FOUND`。            |
| `FaceSdk.personManager().list(...)`               | `FacePersonListResult`  | 人员列表结果。成功时读取 `persons`。                       |
| `FaceSdk.personManager().listFaceFeatures(...)`   | `FaceFeatureTemplateListResult` | 指定人员的全部特征模板。                         |
| `FaceSdk.personManager().count(...)`              | `FacePersonCountResult` | 人数统计结果。成功时读取 `count`。                         |
| `FaceSdk.personManager().delete(...)`             | `FaceSdkResult`         | 删除是否成功。                                             |
| `FaceSdk.personManager().clear(...)`              | `FaceSdkResult`         | 清空是否成功。                                             |
| `FaceSdk.recognitionEngine().detect(...)`         | `FaceDetectResult`      | 人脸检测结果。成功时读取 `faces`。                         |
| `FaceSdk.recognitionEngine().extractFeature(...)` | `FaceFeatureResult`     | 特征提取结果。成功时读取 `feature`。                       |
| `FaceSdk.recognitionEngine().compare(...)`        | `FaceCompareResult`     | 1:1 比对结果。成功后看 `matched`。                         |
| `FaceSdk.recognitionEngine().search(...)`         | `FaceSearchResult`      | 1:N 搜索结果。成功时读取 `matches`。                       |
| `FaceActivityResult.fromIntent(data)`             | `FaceActivityResult`    | 内置录入、识别、活体页面返回结果。                         |

### 8.2 通用结果 `FaceSdkResult`

| 字段      | 说明                 |
| :-------- | :------------------- |
| `success` | 是否成功。           |
| `code`    | 状态码。             |
| `message` | 详情信息，可能为空。 |

初始化、删除人员、清空人脸库等方法会返回 `FaceSdkResult`。

### 8.3 授权信息 `FaceLicenseInfo`

| 字段                     | 说明                                      |
| :----------------------- | :---------------------------------------- |
| `validUntilEpochSeconds` | SDK 校验后的授权有效期，Unix 秒级时间戳。 |

该对象仅用于业务侧展示授权有效期或做运维提示，不需要也不建议业务侧自行解析 license 文本。

### 8.4 人员对象 `FacePerson`

| 字段                          | 说明                                 |
| :---------------------------- | :----------------------------------- |
| `personId`                    | 业务人员 ID。                        |
| `name`                        | 人员名称。                           |
| `groupId`                     | 人员所属分组，可能为空。             |
| `extra`                       | 业务扩展字段，建议使用 JSON 字符串。 |
| `primaryFeature`              | 主特征模板。                         |
| `feature`                     | 兼容字段，等同于主特征模板。         |
| `faceBitmap`                  | 人员头像缩略图，可能为空。           |
| `featureCount`                | 该人员当前特征模板数量。             |
| `createdAtMs` / `updatedAtMs` | 创建和更新时间。                     |

### 8.5 人脸库结果

`FacePersonResult`：

| 字段      | 说明                                                          |
| :-------- | :------------------------------------------------------------ |
| `success` | 人员操作是否成功。                                            |
| `code`    | 状态码。                                                      |
| `message` | 详情信息，可能为空。                                          |
| `person`  | 单个人员结果。注册或查询成功时返回。                          |
| `persons` | 兼容字段，一般使用列表接口的 `FacePersonListResult.persons`。 |

`FacePersonListResult`：

| 字段      | 说明                 |
| :-------- | :------------------- |
| `success` | 列表查询是否成功。   |
| `code`    | 状态码。             |
| `message` | 详情信息，可能为空。 |
| `persons` | 人员列表。           |

`FacePersonCountResult`：

| 字段      | 说明                 |
| :-------- | :------------------- |
| `success` | 统计是否成功。       |
| `code`    | 状态码。             |
| `message` | 详情信息，可能为空。 |
| `count`   | 人员数量。           |

`FaceFeatureTemplateListResult`：

| 字段        | 说明                                                          |
| :---------- | :------------------------------------------------------------ |
| `success`   | 特征模板列表查询是否成功。                                    |
| `code`      | 状态码。人员不存在时返回 `PERSON_NOT_FOUND`。                 |
| `message`   | 详情信息，可能为空。                                          |
| `templates` | 指定人员的全部特征模板，通常按 `slotIndex` 和创建时间稳定排序。 |

`FaceFeatureTemplate`：

| 字段           | 说明                                                            |
| :------------- | :-------------------------------------------------------------- |
| `featureId`    | 特征模板 ID，可用于定位单个模板。                               |
| `personId`     | 所属人员 ID。                                                   |
| `slotType`     | 模板类型：`FRONT`、`YAW_LEFT`、`YAW_RIGHT`、`EXPRESSION`、`CUSTOM`。 |
| `slotIndex`    | 模板顺序索引，主脸通常为 `0`。                                  |
| `feature`      | 该模板对应的人脸特征。                                          |
| `qualityScore` | 采集质量分，可能为空。                                          |
| `faceBitmap`   | 该模板头像缩略图，可能为空。                                    |
| `createdAtMs`  | 模板创建时间。                                                  |

### 8.5 人脸检测与特征结果

`FaceDetectResult`：

| 字段      | 说明                 |
| :-------- | :------------------- |
| `success` | 是否检测到可用人脸。 |
| `code`    | 状态码。             |
| `message` | 详情信息，可能为空。 |
| `faces`   | 检测到的人脸框列表。 |

`FaceFeatureResult`：

| 字段         | 说明                                       |
| :----------- | :----------------------------------------- |
| `success`    | 是否成功提取特征。                         |
| `code`       | 状态码。                                   |
| `message`    | 详情信息，可能为空。                       |
| `feature`    | 人脸特征。普通业务不需要直接保存。         |
| `faceBox`    | 本次使用的人脸框，可能为空。               |
| `quality`    | 本次人脸质量结果，可能为空。               |
| `faceBitmap` | 裁剪人脸图，只有开启返回图片时才可能有值。 |

### 8.6 图片比对结果 `FaceCompareResult`

| 字段         | 说明                                         |
| :----------- | :------------------------------------------- |
| `success`    | 比对流程是否成功。成功不代表一定是同一个人。 |
| `code`       | 状态码。                                     |
| `similarity` | 两张人脸的相似度。                           |
| `threshold`  | 本次判定阈值。                               |
| `matched`    | 是否达到阈值。                               |
| `metric`     | 相似度算法，默认 `COSINE`。                  |

### 8.7 图片和相机搜索结果 `FaceSearchResult`

| 字段                           | 说明                                                   |
| :----------------------------- | :----------------------------------------------------- |
| `success`                      | 是否命中。                                             |
| `code`                         | 状态码。未命中常见为 `NO_MATCHED`。                    |
| `message`                      | 详情信息，可能为空。                                   |
| `matches`                      | TopK 候选列表，按相似度从高到低排列。                  |
| `feature`                      | 本次查询图的人脸特征，只有开启返回特征时才可能有值。   |
| `faceBitmap`                   | 本次查询图的人脸裁剪图，只有开启返回图片时才可能有值。 |
| `liveScore` / `liveScoreReady` | 开启无感 LiveScore 时返回。                            |

`FaceSearchMatch` 常用字段：

| 字段         | 说明                 |
| :----------- | :------------------- |
| `person`     | 候选人员。           |
| `similarity` | 相似度。             |
| `rank`       | 排名。               |
| `threshold`  | 本次搜索阈值。       |
| `matched`    | 该候选是否达到阈值。 |

---

## 9. 常见状态码

| 类型       | 状态码                                                                                                         | 建议处理                                   |
| :--------- | :------------------------------------------------------------------------------------------------------------- | :----------------------------------------- |
| 生命周期   | `SDK_NOT_INITIALIZED`                                                                                          | 先调用 `FaceSdk.initialize()`。            |
| 授权       | `LICENSE_NOT_FOUND`、`LICENSE_INVALID`、`LICENSE_EXPIRED`、`LICENSE_PACKAGE_MISMATCH`、`LICENSE_CERT_MISMATCH` | 检查授权文本、包名、签名证书和有效期。     |
| 人脸检测   | `FACE_NOT_FOUND`、`MULTIPLE_FACES`、`FACE_TOO_SMALL`、`FACE_TOO_LARGE`、`FACE_NOT_CENTERED`                    | 引导用户正对镜头，保持合适距离，单人入镜。 |
| 质量       | `FACE_ANGLE_TOO_LARGE`、`LANDMARK_MISSING`、`QUALITY_NOT_PASS`                                                 | 调整光线、角度和距离。                     |
| 人脸库     | `FACE_LIBRARY_EMPTY`、`PERSON_NOT_FOUND`、`PERSON_ALREADY_EXISTS`、`NO_MATCHED`                                | 检查是否已录入人员，或提示未命中。         |
| 活体       | `LIVENESS_WAITING`、`LIVENESS_TIMEOUT`、`LIVENESS_FAILED`、`LIVENESS_PASSED`                                   | 用于独立动作活体页。                       |
| 相机       | `CAMERA_PERMISSION_DENIED`、`UNSUPPORTED_CAMERA`、`CAMERA_ERROR`                                               | 检查相机权限和设备相机。                   |
| 用户行为   | `USER_CANCELLED`                                                                                               | 用户主动关闭页面，通常不算系统错误。       |
| 输入和系统 | `INPUT_IMAGE_ERROR`、`CONFIG_INVALID`、`OPERATION_TIMEOUT`、`INTERNAL_ERROR`                                   | 记录日志，检查输入图片、配置和运行环境。   |

---

## 10. 接入注意事项

1. 建议优先使用 `FaceCameraKit` 完成录入、识别和活体检测的基础接入。若业务需要自定义页面，再使用 `FaceSearchCameraView` 承载识别能力。
2. 人脸库相关接口涉及本地数据库访问，注册接口还会执行人脸检测和特征提取。请在后台线程调用注册、列表、删除、清空等方法，避免阻塞主线程。
3. 录入质量会直接影响后续识别效果。建议在录入阶段对光线、距离、角度和单人入镜做更严格的控制，避免低质量人脸进入底库。
4. 图片搜索通常用于单人照片，建议使用 `singleFaceOnly=true`；相机识别面向实时画面，可使用 `singleFaceOnly=false`，由 SDK 选择合适的人脸进行识别。
5. `threshold` 为识别命中阈值。默认值可用于快速验证流程，正式上线前应结合实际设备、现场光线、底库规模和业务误识/拒识要求完成阈值标定。
6. `livenessRequired` 用于相机流无感自然度评分，不作为强制活体拦截条件。若业务要求“活体通过后再识别”，应先调用独立活体页面，通过后再进入识别流程。
7. 有门店、部门、租户等业务范围时，建议使用 `groupId` 分组注册和搜索。`groupId` 不传表示全库，传入后只影响该分组。
8. SDK 已内置默认模型、检测器和本地数据库配置。普通接入场景无需修改底层配置；如需定制，请与 SDK 交付方确认兼容性和升级策略。
