# 权限管理库使用说明

这是一个完善的 Android 运行时权限请求库，支持自动显示权限请求原因对话框。

## 主要特性

- 🎯 **声明式API**: 使用 Compose 风格的声明式权限管理
- 🔄 **状态管理**: 自动跟踪权限状态变化
- 💬 **智能对话框**: 自动显示权限请求原因说明
- 🎨 **Material Design 3**: 遵循最新设计规范
- 🔧 **灵活配置**: 支持必需/可选权限分类
- 🔄 **生命周期感知**: 应用从后台回到前台时自动刷新权限状态
- ⚡ **永久拒绝处理**: 智能处理永久拒绝的权限，引导用户到设置页面

## 基本使用

### 1. 单个权限

```kotlin
@Composable
fun CameraScreen() {
    val cameraPermission = rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        usage = { 
            Text(
                text = "需要相机权限来拍照和录制视频",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        required = true
    )
    
    PermissionManager(permissionState = cameraPermission) {
        PermissionCheck(
            permissionState = cameraPermission,
            onGranted = {
                // 权限已授权，显示相机界面
                CameraContent()
            },
            onDenied = { state ->
                // 权限被拒绝，显示自定义内容
                Button(onClick = { state.requestPermissions() }) {
                    Text("请求相机权限")
                }
            }
        )
    }
}
```

### 2. 多个权限

```kotlin
@Composable
fun MediaScreen() {
    val mediaPermissions = rememberPermissionState(
        permissions = setOf(
            PermissionInfo(
                permission = Manifest.permission.CAMERA,
                usage = { 
                    Text("需要相机权限来拍照")
                },
                required = true
            ),
            PermissionInfo(
                permission = Manifest.permission.RECORD_AUDIO,
                usage = { 
                    Text("需要录音权限来录制视频")
                },
                required = false
            ),
            PermissionInfo(
                permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                usage = { 
                    Text("需要存储权限来保存媒体文件")
                },
                required = true
            )
        )
    )
    
    PermissionManager(permissionState = mediaPermissions) {
        when {
            mediaPermissions.allRequiredPermissionsGranted -> {
                // 所有必需权限已授权
                MediaContent(hasAudioPermission = mediaPermissions.permissionStates[Manifest.permission.RECORD_AUDIO] == PermissionStatus.Granted)
            }
            else -> {
                // 显示权限请求界面
                Column {
                    Text("需要权限才能继续")
                    Button(onClick = { mediaPermissions.requestPermissions() }) {
                        Text("请求权限")
                    }
                }
            }
        }
    }
}
```

### 3. 权限状态检查

```kotlin
@Composable
fun PermissionStatusExample() {
    val permissionState = rememberPermissionState(...)
    
    // 检查所有权限
    if (permissionState.allPermissionsGranted) {
        Text("所有权限已授权")
    }
    
    // 检查必需权限
    if (permissionState.allRequiredPermissionsGranted) {
        Text("必需权限已授权")
    }
    
    // 检查单个权限
    when (permissionState.permissionStates[Manifest.permission.CAMERA]) {
        PermissionStatus.Granted -> Text("相机权限已授权")
        PermissionStatus.Denied -> Text("相机权限被拒绝")
        PermissionStatus.DeniedPermanently -> Text("相机权限被永久拒绝")
        PermissionStatus.NotRequested -> Text("相机权限未请求")
        null -> Text("权限状态未知")
    }
}
```

### 4. 手动权限管理

```kotlin
@Composable
fun ManualPermissionExample() {
    val permissionState = rememberPermissionState(...)
    
    Column {
        Button(onClick = { 
            // 请求所有权限
            permissionState.requestPermissions() 
        }) {
            Text("请求所有权限")
        }
        
        Button(onClick = { 
            // 请求特定权限
            permissionState.requestPermission(Manifest.permission.CAMERA) 
        }) {
            Text("请求相机权限")
        }
        
        Button(onClick = { 
            // 跳转到应用设置
            permissionState.openAppSettings() 
        }) {
            Text("前往设置")
        }
        
        Button(onClick = { 
            // 更新权限状态
            permissionState.updatePermissionStates() 
        }) {
            Text("刷新权限状态")
        }
    }
}
```

## API 参考

### PermissionInfo

```kotlin
data class PermissionInfo(
    val permission: String,        // Android权限字符串
    val usage: @Composable () -> Unit,  // 权限使用说明
    val required: Boolean = false  // 是否为必需权限
)
```

### PermissionState

主要属性：
- `permissionStates: Map<String, PermissionStatus>` - 权限状态映射
- `allPermissionsGranted: Boolean` - 是否所有权限都已授权
- `allRequiredPermissionsGranted: Boolean` - 是否所有必需权限都已授权
- `deniedPermissions: List<PermissionInfo>` - 被拒绝的权限列表

主要方法：
- `requestPermissions()` - 请求所有未授权权限
- `requestPermission(permission: String)` - 请求特定权限
- `updatePermissionStates()` - 更新权限状态
- `refreshPermissionStates()` - 强制刷新权限状态（用于生命周期变化时）
- `openAppSettings()` - 跳转到应用设置页面

### PermissionStatus

```kotlin
enum class PermissionStatus {
    NotRequested,      // 未请求
    Granted,          // 已授权
    Denied,           // 被拒绝但可以再次请求
    DeniedPermanently // 被拒绝且用户选择"不再询问"
}
```

## 注意事项

1. **Activity 要求**: `rememberPermissionState` 必须在 `ComponentActivity` 中使用
2. **权限声明**: 确保在 `AndroidManifest.xml` 中声明了所需权限
3. **生命周期感知**: 权限状态会在应用从后台回到前台时自动刷新
4. **对话框**: 权限说明对话框会在需要时自动显示
5. **设置跳转**: 对于永久拒绝的权限，会提供跳转到设置的选项
6. **状态同步**: 用户在设置中修改权限后，回到应用会立即更新状态

## 最佳实践

1. **按需请求**: 只在需要使用功能时请求权限
2. **清晰说明**: 在 `usage` 中清晰说明权限的用途
3. **分类管理**: 合理设置 `required` 标志区分必需和可选权限
4. **降级体验**: 为权限被拒绝的情况提供降级体验
5. **状态持久**: 权限状态会在配置变更时保持
