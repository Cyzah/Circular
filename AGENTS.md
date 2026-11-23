# Circular Agent 工作约定

## 工作范围与文件权限

- 工程根目录统一记作 `Circular_Kotlin/`，文中的项目内路径均相对于该目录。开始操作先确认当前路径。
- 工程外的系统目录只允许读取，不得在那里创建、修改、移动或删除文件，也不能把下载、缓存、临时文件、日志或工具副本写入其中。已安装的工具可只读使用，运行时数据目录必须重定向到 `Circular_Kotlin/.local/`。
- 新文件和构建写入原则上只允许位于 `Circular_Kotlin/`；访问或写入工程外目录须有用户授权。
- 只完成用户授权的任务。未经授权不得额外新增脚本、文档、计划、报告、测试、示例配置、压缩包或自动化配置，也不能自动初始化 Git、提交、上传或发布。
- 用户授权构建或环境准备后，工具在下表约定位置产生的缓存和构建输出属于该操作的一部分。需要新建其他文件时，先说明路径和用途。
- 操作前检查已有文件，禁止静默覆盖。复制现有文件时保持内容和目录结构，不能顺带格式化或重写。
- 不修改、移动、清理工程以外的资料。只有用户明确要求移动时才能删除源文件；复制操作必须保留源文件。
- 完整步骤和环境配置见同目录的 构建指南.md。不要自行补充第二套构建流程。

## 缓存、临时文件和产物

以下位置均相对于工程根目录：

| 内容或环境变量 | 固定位置 |
| --- | --- |
| GRADLE_USER_HOME、Wrapper 下载 | .local/gradle-home/ |
| Gradle 工程缓存 | .local/gradle-project/ |
| Kotlin 工程持久数据 | .local/kotlin/ |
| ANDROID_USER_HOME、Debug keystore | .local/android/ |
| ANDROID_AVD_HOME | .local/android/avd/ |
| TEMP、TMP、java.io.tmpdir | .local/tmp/ |
| Java user.home | .local/runtime-home/ |
| 工具安装包 | .local/downloads/ |
| JDK 等工具链副本 | .local/toolchains/ |
| Android SDK | .local/android-sdk/ |
| 使用者的签名密钥 | .local/keystore/ |
| Debug APK | app/build/outputs/apk/<变体>/debug/，文件名见构建指南 |
| Release APK | app/build/outputs/apk/<变体>/release/，文件名见构建指南 |
| 单元测试结果 | app/build/test-results/testGithubDebugUnitTest/、testLspRepoDebugUnitTest/ |
| 测试、Lint 等构建报告 | app/build/reports/ |
| 根工程生成报告 | build/reports/ |
| 正式交付归档 | delivery/<版本>/ |

- 所有 Gradle 操作从根目录调用 tools/build.ps1，禁止绕过脚本裸调 Gradle。
- 显式传 --project-cache-dir 指向 .local/gradle-project/，以及 -Pkotlin.project.persistent.dir 指向 .local/kotlin/，并加 -Pkotlin.project.persistent.dir.gradle.disableWrite=true。
- SDK 管理器、keytool 或其他 Java 工具启动前，也要设置 TEMP、TMP、JAVA_TOOL_OPTIONS 中的 user.home 和 java.io.tmpdir，指向表中目录。不要仅依赖 Gradle 脚本处理其他工具。
- 新装 JDK 和 SDK 放到表中位置。已有工具只读使用时，实际 SDK 路径可以是用户在本机 local.properties 中指定的工程外位置。
- 不改写 HOME、USERPROFILE 等系统目录变量。Python/Node 确有必要时，为其缓存、字节码和临时文件显式指定 `.local/` 下的目录，不使用系统默认缓存。
- 不打印完整环境变量或可能包含口令的工具参数。
- 临时数据必须使用任务唯一路径和 try/finally 清理；失败、中断、重试也要清理，最后核对路径不存在。
- 禁止整体清空 .local/、密钥目录、其他任务状态或正式交付归档。仅删除本任务明确创建且已核实绝对路径的临时文件。

## 构建与签名边界

- 固定依赖及工具版本以 gradle/libs.versions.toml、gradle/wrapper/gradle-wrapper.properties 和 app/build.gradle.kts 为准。依赖更新优先选择兼容的稳定版，并通过上述配置统一管理。
- Android 配置是 compileSdk 37、compileSdkMinor 2，应用编译目标为 JVM 17，构建使用 JDK 21。JDK 和 SDK 需先准备，Wrapper 只负责 Gradle 下载。
- 首次构建联网解析依赖；只有缓存完整时使用 --offline。网络或依赖缺失不能通过切换到工程外或系统默认缓存解决。
- tools/build.ps1 的默认任务是 :app:assembleGithubRelease。Agent 每次调用都必须显式列出签名前任务，不能省略 -Tasks，也不要使用可能聚合 Release 的 build、assemble、check。
- Agent 可在用户授权范围内运行单元测试、Lint、Debug 构建和维护性检查。Debug 必须显式加 -PuseReleaseSigningForDebug=false。
- Agent 不执行任何变体的 assembleRelease、bundleRelease、签名 Release 任务或 -PuseReleaseSigningForDebug=true。
- Release 签名配置、密钥管理和签名构建由用户本人操作。Agent 不读取 local-signing.properties、密钥内容或真实签名环境变量，不代填口令，不把秘密写入输出。用户明确授权文件移动时，只能按文件整体操作并在内存中校验一致性，不执行密钥枚举、导出、口令尝试或签名。
- 允许说明参数名：storeFile、storePassword、keyAlias、keyPassword，以及对应的 CIRCULAR_KEYSTORE、CIRCULAR_KEYSTORE_PASSWORD、CIRCULAR_KEY_ALIAS、CIRCULAR_KEY_PASSWORD。
- 本机签名文件仅存放在 .local/keystore/，该目录必须被 Git 忽略。可跟踪文件不得记录实际签名文件名、来源路径、别名、口令、内容或哈希；本机 storeFile 的具体值由用户本人设置，不假定示例默认路径就是真实文件。
- signing.properties.example 是公开模板，不是真实配置。local.properties、local-signing.properties 和密钥是本机文件，不随源码分发。
- Gradle 配置阶段也可能加载本机签名设置，Debug 参数仅控制选用的签名方案；不能据此宣称 Debug 完全不加载这些设置。
- 用户本人签名后，Agent 可在授权范围内运行 tools/verify-apk.ps1 对产物作只读校验。校验脚本面向已签名的 Release，不用于宣称 Debug 等同于 Release 验证。

## 测试工具与用例

- 选择和维护测试前阅读同目录的 [测试指南.md](测试指南.md)，环境和构建仍以 [构建指南.md](构建指南.md) 为准。
- app/src/test/kotlin/、tools/test-*.ps1、tools/tests/ 及其夹具、测试辅助函数属于维护源码，必须随 Git 保留；测试输出、设备临时数据、性能摘要和验证日志按测试指南归档或清理，并由 .gitignore 排除。
- 工具自测可按任务需要执行；宿主 Hook/绘制回归先运行显式 Debug 单测任务生成编译产物。运行设备脚本仍须用户明确授权，不能用宿主或设置页通过替代真机 SystemUI Hook 验证。
- 按构建指南的统一入口执行检查，只报告本轮实际执行的检查及清理结果。

## 签名前验证入口

先按 构建指南.md 设置会话环境，并在根目录执行：

~~~powershell
$projectRoot = (Get-Location).Path
if ([IO.Path]::GetFileName($projectRoot.TrimEnd('\')) -ne 'Circular_Kotlin') { throw '请从 Circular_Kotlin 工程根目录执行。' }

$cacheArguments = @(
    '--project-cache-dir', (Join-Path $projectRoot '.local/gradle-project'),
    ('-Pkotlin.project.persistent.dir=' + (Join-Path $projectRoot '.local/kotlin')),
    '-Pkotlin.project.persistent.dir.gradle.disableWrite=true'
)
& ./tools/build.ps1 -Tasks ($cacheArguments + @(
    '-PuseReleaseSigningForDebug=false',
    ':app:testGithubDebugUnitTest', ':app:lintGithubDebug', ':app:assembleGithubDebug',
    ':app:testLspRepoDebugUnitTest', ':app:lintLspRepoDebug', ':app:assembleLspRepoDebug'
))
if ($LASTEXITCODE -ne 0) { throw '检查失败，不能报告为通过。' }

& ./tools/check-maintainability.ps1
if ($LASTEXITCODE -ne 0) { throw '维护性检查失败。' }
~~~

只有实际执行并通过的检查才能报告为通过。现有日志、文件复制成功或静态审查不能替代本次构建，也不能替代真机 Hook 验证。

## 版本与交付

- version.properties 是唯一版本来源。版本格式为 major.minor.patch，不增加独立 Build 编号。
- 只有用户明确开始新的正式发布轮次时，才调用一次 tools/bump-version.ps1。Debug、验证、重试和同轮重编译均不 bump。
- patch、minor 按 26 进位，versionCode 必须为正数且每次正式交付递增。
- 签名后的原始 APK 和归档副本都要校验；校验对照当前 version.properties，因此不能提前开启下一轮版本递增。
- 经授权归档时，在 delivery/<版本>/ 存放对应变体的 APK、SHA256SUMS.txt 和 RELEASE.md。正式版本永久保留。发布说明只写实际结果，不记录口令、密钥内容或本机绝对路径。
- 原始 Gradle APK 保留在 app/build/outputs/apk/。未经授权不复制发布包、生成 ZIP、创建 Git 标签或上传。
- 不默认产生独立验证记录文件。用户需要留档时，先确定具体文件与位置，再创建。

## 源码与模块约束

- 显示名 Circular。普通版 github 的安装包 ID 为 com.cyzah.circular，LSPosed 仓库版 lspRepo 的安装包 ID 为 io.github.cyzah.circular。源码 namespace 和 Hook 包为 com.cyzah.circular，配置 Provider 使用各自的 applicationId。未经明确要求不更改安装标识、入口、Provider 或协议。
- 应用源码和单元测试使用 Kotlin。共用内容在 app/src/main/kotlin/ 和 app/src/test/kotlin/，变体专属内容在 app/src/github/、app/src/lspRepo/ 和对应测试源集。保留作者署名、许可证、Hook 行为、配置协议和九种字体。
- app/src/main/resources/META-INF/xposed/ 的三份元数据和 app/src/main/resources/fonts/ 的九种字体必须完整保留。
- UI 固定 OS4 Glass：一个根 Scaffold、独立内容/页面 backdrop 和根弹层宿主。界面主题设置与 SystemUI 图标配置分开保存。
- 设置卡片四边留白 20dp，滑条轨道 23dp，同一边距只应用一次。
- 不引入与当前任务无关的重构、依赖替换或文件拆分。需要调整现有行为时先理解本工程实现，不能仅凭外部示例替换。

## Git 与隐私边界

- 仓库只管理源码、测试、构建配置、Wrapper、工具脚本、公开模板和文档；安装的工具链、SDK、下载的依赖、本机环境、缓存、构建输出和发布归档必须忽略。
- 初始化本地仓库不表示授权首次提交、配置远程或上传。未获明确授权，不设置作者身份、复制全局凭据、添加远程、执行提交/推送/发布。
- 不复制其他仓库的 .git、历史、remote、hooks、令牌或身份配置。初始化时不引入外部 Git 模板或钩子。
- 保留 gradle/wrapper/gradle-wrapper.jar、gradle/libs.versions.toml、Gradle 构建配置及 tools/ 的脚本；它们是源码的一部分。不能整体忽略 tools/、gradle/、所有 JAR 或所有 properties 文件。
- .local/、本机配置、签名容器、私钥、真实环境变量文件、下载目录和生成物禁止暂存。不要使用 git add .、git add -A 或 git add -f；先按允许清单检查，再仅暂存用户要求的具体文件。
- 提交前检查 git status、git diff --cached --name-only 和忽略规则；真实私有文件不得进入 Git 索引或对象库，亦不得写入提交说明、文档或日志。
- .gitignore 只是保护文件不被普通暂存，不是加密或权限系统；本机私有目录仍需按用户要求保管。未来若需将某项固定依赖纳入源码，先获得用户批准并重新审查规则，不直接强制加入。

## 设备操作

- 只有用户明确要求设备验证时才操作设备，先核对设备身份，每个 ADB 命令都指定 -s <serial>。
- 临时推送使用任务唯一目录；所有临时文件、辅助包及改动过的系统设置必须在 try/finally 中清理或恢复，并验证结果。
- 不清理其他应用数据或共享目录；实际交付的应用可保留。
- 分开报告设置页、模拟器和真机 SystemUI Hook 的验证结果。
