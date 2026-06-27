# Ainuo Face SDK Demo

本仓库按 tag 发布 Ainuo Face SDK 的 Android/iOS 官方 Demo 与 SDK 二进制产物。当前版本：`v0.1.9`。

## 下载官方 Demo

```bash
git clone --branch v0.1.9 https://github.com/ainuoface/face-sdk.git
cd face-sdk
```

## Android 接入

使用 Android Studio 打开 `android/` 目录即可运行官方 Demo。

客户接入 Android 应用时，普通 Gradle 工程可以继续添加当前 tag 对应的 GitHub raw Maven 仓库和 SDK 依赖：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/ainuoface/face-sdk/v0.1.9/maven") }
    }
}

dependencies {
    implementation("com.ainuo:face-sdk:0.1.9")
}
```

如果接入环境只支持固定 Maven 仓库，例如 HBuilderX 云打包，可使用 JitPack。

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencies {
    implementation("com.github.ainuoface:face-sdk:v0.1.9")
}
```

## iOS 接入

使用 Xcode 打开 `ios/AinuoFaceDemo.xcodeproj` 即可运行官方 Demo。

客户接入 iOS 应用时，可通过 CocoaPods 引用当前 tag：

```ruby
pod 'AinuoFaceSDK', :git => 'https://github.com/ainuoface/face-sdk.git', :tag => 'v0.1.9'
```

```
