       1,546ms
Error: Gradle task assembleDebug failed with exit code 1
(base) kalyan@kalyans-MacBook-Pro AI-keyboard % flutter run 
Launching lib/main.dart on 23090RA98I in debug mode...
Running Gradle task 'assembleDebug'...                           1,791ms
✓ Built build/app/outputs/flutter-apk/app-debug.apk
Installing build/app/outputs/flutter-apk/app-debug.apk...           6.7s
I/flutter (11210): [IMPORTANT:flutter/shell/platform/android/android_context_vk_impeller.cc(61)] Using the Impeller rendering backend (Vulkan).
I/flutter (11210): Firebase already initialized, continuing...
Syncing files to device 23090RA98I...                               36ms

Flutter run key commands.
r Hot reload. 🔥🔥🔥
R Hot restart.
h List all available interactive commands.
d Detach (terminate "flutter run" but leave application running).
c Clear the screen
q Quit (terminate the application on the device).

A Dart VM Service on 23090RA98I is available at: http://127.0.0.1:53885/AtPECYCj9H0=/
I/flutter (11210): 🔵 [AuthWrapper] Checking if this is first app launch...
I/flutter (11210): 🔵 [AuthWrapper] First launch: true
I/flutter (11210): 🔵 [AuthWrapper] Showing welcome screen for first launch
E/LB      (11210): fail to open file: No such file or directory
E/LB      (11210): fail to open node: No such file or directory
E/LB      (11210): fail to open node: No such file or directory
I/GrallocExtra(11210): gralloc_extra_query:is_SW3D 0
D/BLASTBufferQueue(11210): [SurfaceView[com.example.ai_keyboard/com.example.ai_keyboard.MainActivity]#1](f:0,a:1) acquireNextBufferLocked size=1220x2712 mFrameNumber=1 applyTransaction=true mTimestamp=940129742100035(auto) mPendingTransactions.size=0 graphicBufferId=48146583388170 transform=0
D/LibMBrainProxy(11210): Init Paramters
W/ServiceManagerCppClient(11210): Failed to get isDeclared for vendor.mediatek.hardware.mbrain.IMBrain/default: Status(-1, EX_SECURITY): 'SELinux denied.'
I/Choreographer(11210): Skipped 45 frames!  The application may be doing too much work on its main thread.
D/UserSceneDetector(11210): invoke error.
W/Looper  (11210): PerfMonitor doFrame : time=2ms vsyncFrame=48505800 latency=760ms procState=-1 historyMsgCount=1
I/flutter (11210): 🔵 [AuthWrapper] Marked first launch as complete
The Flutter DevTools debugger and profiler on 23090RA98I is available at: http://127.0.0.1:9100?uri=http://127.0.0.1:53885/AtPECYCj9H0=/
D/VRI[MainActivity](11210): vri.reportNextDraw android.view.ViewRootImpl.performTraversals:4985 android.view.ViewRootImpl.doTraversal:3572 android.view.ViewRootImpl$TraversalRunnable.run:11792 android.view.Choreographer$CallbackRecord.run:1821 android.view.Choreographer$CallbackRecord.run:1830 
D/VRI[MainActivity](11210): Setup new sync=wmsSync-VRI[MainActivity]#2
D/UserSceneDetector(11210): invoke error.
D/HWUI    (11210): makeCurrent grContext:0xb40000725bc13110 reset mTextureAvailable
D/ple.ai_keyboard(11210): MiuiProcessManagerServiceStub setSchedFifo
I/MiuiProcessManagerImpl(11210): setSchedFifo pid:11210, mode:3
D/BLASTBufferQueue(11210): [VRI[MainActivity]#0](f:0,a:1) acquireNextBufferLocked size=1220x2712 mFrameNumber=1 applyTransaction=true mTimestamp=940129959853881(auto) mPendingTransactions.size=0 graphicBufferId=48146583388178 transform=0
D/VRI[MainActivity](11210): vri.reportDrawFinished
I/NativeTurboSchedManager(11210): Load libmiui_runtime
E/NativeTurboSchedManagerJni(11210): open /dev/metis failed!
E/NativeTurboSchedManagerJni(11210): open /dev/metis failed!
V/MediaPlayer(11210): resetDrmState:  mDrmInfo=null mDrmProvisioningThread=null mPrepareDrmInProgress=false mActiveDrmScheme=false
V/MediaPlayer(11210): cleanDrmObj: mDrmObj=null mDrmSessionId=null
I/HandWritingStubImpl(11210): refreshLastKeyboardType: 1
I/HandWritingStubImpl(11210): getCurrentKeyboardType: 1
I/HandWritingStubImpl(11210): getCurrentKeyboardType: 1
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940132343, downTime=940132343, phoneEventTime=00:38:49.114 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940132404, downTime=940132343, phoneEventTime=00:38:49.175 } moveCount:0
I/flutter (11210): 🔵 [LoginIllustraionScreen] Initiating Google Sign-In...
I/flutter (11210): 🔵 [GoogleAuth] Starting Google Sign-In flow...
I/flutter (11210): 🔵 [GoogleAuth] Step 1: Triggering Google account selection...
W/google_sign_in(11210): clientId is not supported on Android and is interpreted as serverClientId. Use serverClientId instead to suppress this warning.
I/ActivityThread(11210): HardwareRenderer preload  done
D/SecurityManager(11210): checkAccessControl flag1
D/MiuiCameraCoveredManager(11210): java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String android.provider.MiuiSettings$SettingsCloudData$CloudData.getString(java.lang.String, java.lang.String)' on a null object reference
I/ForceDarkHelperStubImpl(11210): setViewRootImplForceDark: false for com.google.android.gms.auth.api.signin.internal.SignInHubActivity@dca318b, reason: DarkModeEnabled
D/VRI[SignInHubActivity](11210): hardware acceleration = true, forceHwAccelerated = false
D/InputEventReceiver(11210): Input log is disabled in InputEventReceiver.
D/BufferQueueConsumer(11210): [](id:2bca00000002,api:0,p:-1,c:11210) connect: controlledByApp=false
D/BLASTBufferQueue(11210): [VRI[SignInHubActivity]#2](f:0,a:0) constructor()
D/BLASTBufferQueue(11210): [VRI[SignInHubActivity]#2](f:0,a:0) update width=1220 height=2712 format=-2 mTransformHint=0
D/VRI[SignInHubActivity](11210): vri.reportNextDraw android.view.ViewRootImpl.performTraversals:4985 android.view.ViewRootImpl.doTraversal:3572 android.view.ViewRootImpl$TraversalRunnable.run:11792 android.view.Choreographer$CallbackRecord.run:1821 android.view.Choreographer$CallbackRecord.run:1830 
D/VRI[SignInHubActivity](11210): Setup new sync=wmsSync-VRI[SignInHubActivity]#4
D/HWUI    (11210): makeCurrent grContext:0xb40000725bc13110 reset mTextureAvailable
D/BLASTBufferQueue(11210): [VRI[SignInHubActivity]#2](f:0,a:1) acquireNextBufferLocked size=1220x2712 mFrameNumber=1 applyTransaction=true mTimestamp=940132561980651(auto) mPendingTransactions.size=0 graphicBufferId=48146583388179 transform=0
D/VRI[SignInHubActivity](11210): vri.reportDrawFinished
E/NativeTurboSchedManagerJni(11210): open /dev/metis failed!
E/NativeTurboSchedManagerJni(11210): open /dev/metis failed!
D/ProfileInstaller(11210): Installing profile for com.example.ai_keyboard
E/ple.ai_keyboard(11210): FrameInsert open fail: No such file or directory
D/SecurityManager(11210): checkAccessControl flag1
I/HandWritingStubImpl(11210): refreshLastKeyboardType: 1
I/HandWritingStubImpl(11210): getCurrentKeyboardType: 1
D/SecurityManager(11210): checkAccessControl flag1
I/flutter (11210): 🟢 [GoogleAuth] Step 1 Success: Google account selected
I/flutter (11210): 🔵 [GoogleAuth] Selected account: bedugamkalyan@gmail.com
I/flutter (11210): 🔵 [GoogleAuth] Account ID: 112895654270505831293
I/flutter (11210): 🔵 [GoogleAuth] Display Name: Kalyan Bedugam
I/flutter (11210): 🔵 [GoogleAuth] Step 2: Retrieving authentication tokens...
I/ple.ai_keyboard(11210): Background concurrent mark compact GC freed 5805KB AllocSpace bytes, 22(1512KB) LOS objects, 49% free, 4983KB/9966KB, paused 266us,11.642ms total 171.284ms
D/VRI[SignInHubActivity](11210): visibilityChanged oldVisibility=true newVisibility=false
I/HandWritingStubImpl(11210): refreshLastKeyboardType: 1
I/HandWritingStubImpl(11210): getCurrentKeyboardType: 1
D/BLASTBufferQueue(11210): [VRI[SignInHubActivity]#2](f:0,a:1) destructor()
D/BufferQueueConsumer(11210): [VRI[SignInHubActivity]#2(BLAST Consumer)2](id:2bca00000002,api:0,p:-1,c:11210) disconnect
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=android.app.Activity$$ExternalSyntheticLambda0@c776bb2
D/View    (11210): [Warning] assignParent to null: this = DecorView@6b1a268[SignInHubActivity]
D/ActivityThread(11210): Fail to check app heapsize due to java.lang.NoSuchMethodException: dalvik.system.VMRuntime.getBlockingGcCountForAlloc []
I/flutter (11210): 🟢 [GoogleAuth] Step 2 Success: Authentication tokens retrieved
I/flutter (11210): 🔵 [GoogleAuth] Validating authentication tokens...
I/flutter (11210): 🔵 [GoogleAuth] Access Token: Present (324 chars)
I/flutter (11210): 🔵 [GoogleAuth] ID Token: Present (1110 chars)
I/flutter (11210): 🔵 [GoogleAuth] Step 3: Creating Firebase credential...
I/flutter (11210): 🟢 [GoogleAuth] Step 3 Success: Firebase credential created
I/flutter (11210): 🔵 [GoogleAuth] Step 4: Signing in to Firebase...
W/System  (11210): Ignoring header X-Firebase-Locale because its value was null.
W/System  (11210): Ignoring header X-Firebase-Locale because its value was null.
D/FirebaseAuth(11210): Notifying id token listeners about user ( J0CoM6lVQXelmXySQzpBGzGaILs2 ).
D/FirebaseAuth(11210): Notifying auth state listeners about user ( J0CoM6lVQXelmXySQzpBGzGaILs2 ).
I/flutter (11210): 🔵 [AuthWrapper] User logged in (bedugamkalyan@gmail.com), starting cloud sync...
I/flutter (11210): KeyboardCloudSync: Starting cloud sync for user J0CoM6lVQXelmXySQzpBGzGaILs2
I/flutter (11210): 🟢 [GoogleAuth] Step 4 Success: Firebase sign-in completed
I/flutter (11210): 🟢 [GoogleAuth] Step 4 Success: Firebase User object validated
I/flutter (11210): 🔵 [GoogleAuth] Firebase User UID: J0CoM6lVQXelmXySQzpBGzGaILs2
I/flutter (11210): 🔵 [GoogleAuth] Firebase User Email: bedugamkalyan@gmail.com
I/flutter (11210): 🔵 [GoogleAuth] Firebase User DisplayName: Kalyan Bedugam
I/flutter (11210): 🔵 [GoogleAuth] Firebase User PhotoURL: https://lh3.googleusercontent.com/a/ACg8ocKc6AbxbqrrEEXroE_rc3DRcb4XFrb1_l_yGwR06sfKe7zzhg=s96-c
I/flutter (11210): 🔵 [GoogleAuth] Is New User: false
I/flutter (11210): 🔵 [GoogleAuth] Step 5: Saving user data to Firestore...
I/flutter (11210): 🔵 [GoogleAuth] Using display name: "Kalyan Bedugam"
I/flutter (11210): 🔵 [Firestore] Starting Firestore write operation...
I/flutter (11210): 🔵 [Firestore] User UID: J0CoM6lVQXelmXySQzpBGzGaILs2
I/flutter (11210): 🔵 [Firestore] Display Name: "Kalyan Bedugam"
I/flutter (11210): 🔵 [Firestore] Email: bedugamkalyan@gmail.com
I/flutter (11210): 🔵 [Firestore] Photo URL: https://lh3.googleusercontent.com/a/ACg8ocKc6AbxbqrrEEXroE_rc3DRcb4XFrb1_l_yGwR06sfKe7zzhg=s96-c
I/flutter (11210): 🔵 [Firestore] Is New User: false
I/flutter (11210): 🔵 [Firestore] Updating existing user profile...
I/flutter (11210): 🔵 [Firestore] Writing user update...
W/DynamiteModule(11210): Local module descriptor class for com.google.android.gms.providerinstaller.dynamite not found.
I/DynamiteModule(11210): Considering local module com.google.android.gms.providerinstaller.dynamite:0 and remote module com.google.android.gms.providerinstaller.dynamite:0
W/ProviderInstaller(11210): Failed to load providerinstaller module: No acceptable module com.google.android.gms.providerinstaller.dynamite found. Local version is 0 and remote version is 0.
D/ApplicationLoaders(11210): Returning zygote-cached class loader: /system/framework/org.apache.http.legacy.jar
D/nativeloader(11210): Configuring clns-8 for other apk /system/framework/com.android.media.remotedisplay.jar. target_sdk_version=36, uses_libraries=ALL, library_path=/data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/lib/arm64:/data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.google.android.gms
D/nativeloader(11210): Extending system_exposed_libraries: libapuwareapusys.mtk.so:libapuwareapusys_v2.mtk.so:libapuwarexrp.mtk.so:libapuwarexrp_v2.mtk.so:libapuwareutils.mtk.so:libapuwareutils_v2.mtk.so:libapuwarehmp.mtk.so:libneuron_graph_delegate.mtk.so:libneuronusdk_adapter.mtk.so:libtflite_mtk.mtk.so:libarmnn_ndk.mtk.so:libcmdl_ndk.mtk.so:libnir_neon_driver_ndk.mtk.so:libmvpu_runtime.mtk.so:libmvpu_runtime_pub.mtk.so:libmvpu_engine_pub.mtk.so:libmvpu_pattern_pub.mtk.so:libmvpuop_mtk_cv.mtk.so:libmvpuop_mtk_nn.mtk.so:libmvpu_runtime_25.mtk.so:libmvpu_runtime_25_pub.mtk.so:libmvpu_engine_25_pub.mtk.so:libmvpu_pattern_25_pub.mtk.so:libmvpuop25_mtk_cv.mtk.so:libmvpuop25_mtk_nn.mtk.so:libmvpu_config.mtk.so:libteeservice_client.trustonic.so:libmisys_jni.xiaomi.so:libpag.hyperos.so:libffavc.hyperos.so
W/ple.ai_keyboard(11210): Loading /data/misc/apexdata/com.android.art/dalvik-cache/arm64/system@framework@com.android.location.provider.jar@classes.odex non-executable as it requires an image which we failed to load
D/nativeloader(11210): Configuring clns-9 for other apk /system/framework/com.android.location.provider.jar. target_sdk_version=36, uses_libraries=ALL, library_path=/data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/lib/arm64:/data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.google.android.gms
D/nativeloader(11210): Extending system_exposed_libraries: libapuwareapusys.mtk.so:libapuwareapusys_v2.mtk.so:libapuwarexrp.mtk.so:libapuwarexrp_v2.mtk.so:libapuwareutils.mtk.so:libapuwareutils_v2.mtk.so:libapuwarehmp.mtk.so:libneuron_graph_delegate.mtk.so:libneuronusdk_adapter.mtk.so:libtflite_mtk.mtk.so:libarmnn_ndk.mtk.so:libcmdl_ndk.mtk.so:libnir_neon_driver_ndk.mtk.so:libmvpu_runtime.mtk.so:libmvpu_runtime_pub.mtk.so:libmvpu_engine_pub.mtk.so:libmvpu_pattern_pub.mtk.so:libmvpuop_mtk_cv.mtk.so:libmvpuop_mtk_nn.mtk.so:libmvpu_runtime_25.mtk.so:libmvpu_runtime_25_pub.mtk.so:libmvpu_engine_25_pub.mtk.so:libmvpu_pattern_25_pub.mtk.so:libmvpuop25_mtk_cv.mtk.so:libmvpuop25_mtk_nn.mtk.so:libmvpu_config.mtk.so:libteeservice_client.trustonic.so:libmisys_jni.xiaomi.so:libpag.hyperos.so:libffavc.hyperos.so
D/ApplicationLoaders(11210): Returning zygote-cached class loader: /system_ext/framework/androidx.window.extensions.jar
D/ApplicationLoaders(11210): Returning zygote-cached class loader: /system_ext/framework/androidx.window.sidecar.jar
W/ple.ai_keyboard(11210): Loading /data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/oat/arm64/base.odex non-executable as it requires an image which we failed to load
D/nativeloader(11210): Configuring clns-10 for other apk /data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/base.apk. target_sdk_version=36, uses_libraries=, library_path=/data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/lib/arm64:/data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/base.apk!/lib/arm64-v8a, permitted_path=/data:/mnt/expand:/data/user/0/com.google.android.gms
W/ProviderInstaller(11210): Failed to report request stats: com.google.android.gms.common.security.ProviderInstallerImpl.reportRequestStats [class android.content.Context, long, long]
I/ple.ai_keyboard(11210): hiddenapi: Accessing hidden method Ldalvik/system/VMStack;->getStackClass2()Ljava/lang/Class; (runtime_flags=0, domain=core-platform, api=unsupported) from Lgjre; (domain=app) using reflection: allowed
E/GoogleApiManager(11210): Failed to get service from broker. 
E/GoogleApiManager(11210): java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'.
E/GoogleApiManager(11210):      at android.os.Parcel.createExceptionOrNull(Parcel.java:3256)
E/GoogleApiManager(11210):      at android.os.Parcel.createException(Parcel.java:3240)
E/GoogleApiManager(11210):      at android.os.Parcel.readException(Parcel.java:3223)
E/GoogleApiManager(11210):      at android.os.Parcel.readException(Parcel.java:3165)
E/GoogleApiManager(11210):      at bbkc.a(:com.google.android.gms@253931035@25.39.31 (260400-813878953):36)
E/GoogleApiManager(11210):      at bbid.z(:com.google.android.gms@253931035@25.39.31 (260400-813878953):143)
E/GoogleApiManager(11210):      at baoz.run(:com.google.android.gms@253931035@25.39.31 (260400-813878953):42)
E/GoogleApiManager(11210):      at android.os.Handler.handleCallback(Handler.java:959)
E/GoogleApiManager(11210):      at android.os.Handler.dispatchMessage(Handler.java:100)
E/GoogleApiManager(11210):      at cmjd.mC(:com.google.android.gms@253931035@25.39.31 (260400-813878953):1)
E/GoogleApiManager(11210):      at cmjd.dispatchMessage(:com.google.android.gms@253931035@25.39.31 (260400-813878953):5)
E/GoogleApiManager(11210):      at android.os.Looper.loopOnce(Looper.java:249)
E/GoogleApiManager(11210):      at android.os.Looper.loop(Looper.java:337)
E/GoogleApiManager(11210):      at android.os.HandlerThread.run(HandlerThread.java:85)
W/FlagRegistrar(11210): Failed to register com.google.android.gms.providerinstaller#com.example.ai_keyboard
W/FlagRegistrar(11210): frum: 17: 17: API: Phenotype.API is not available on this device. Connection failed with: ConnectionResult{statusCode=DEVELOPER_ERROR, resolution=null, message=null, clientMethodKey=null}
W/FlagRegistrar(11210):         at fruo.a(:com.google.android.gms@253931035@25.39.31 (260400-813878953):13)
W/FlagRegistrar(11210):         at govh.d(:com.google.android.gms@253931035@25.39.31 (260400-813878953):3)
W/FlagRegistrar(11210):         at govj.run(:com.google.android.gms@253931035@25.39.31 (260400-813878953):130)
W/FlagRegistrar(11210):         at goxq.execute(:com.google.android.gms@253931035@25.39.31 (260400-813878953):1)
W/FlagRegistrar(11210):         at govr.f(:com.google.android.gms@253931035@25.39.31 (260400-813878953):1)
W/FlagRegistrar(11210):         at govr.m(:com.google.android.gms@253931035@25.39.31 (260400-813878953):99)
W/FlagRegistrar(11210):         at govr.r(:com.google.android.gms@253931035@25.39.31 (260400-813878953):17)
W/FlagRegistrar(11210):         at fjzu.hy(:com.google.android.gms@253931035@25.39.31 (260400-813878953):35)
W/FlagRegistrar(11210):         at exqb.run(:com.google.android.gms@253931035@25.39.31 (260400-813878953):12)
W/FlagRegistrar(11210):         at goxq.execute(:com.google.android.gms@253931035@25.39.31 (260400-813878953):1)
W/FlagRegistrar(11210):         at exqc.b(:com.google.android.gms@253931035@25.39.31 (260400-813878953):18)
W/FlagRegistrar(11210):         at exqr.b(:com.google.android.gms@253931035@25.39.31 (260400-813878953):36)
W/FlagRegistrar(11210):         at exqt.d(:com.google.android.gms@253931035@25.39.31 (260400-813878953):25)
W/FlagRegistrar(11210):         at bami.c(:com.google.android.gms@253931035@25.39.31 (260400-813878953):9)
W/FlagRegistrar(11210):         at baox.q(:com.google.android.gms@253931035@25.39.31 (260400-813878953):48)
W/FlagRegistrar(11210):         at baox.d(:com.google.android.gms@253931035@25.39.31 (260400-813878953):10)
W/FlagRegistrar(11210):         at baox.g(:com.google.android.gms@253931035@25.39.31 (260400-813878953):192)
W/FlagRegistrar(11210):         at baox.onConnectionFailed(:com.google.android.gms@253931035@25.39.31 (260400-813878953):2)
W/FlagRegistrar(11210):         at baoz.run(:com.google.android.gms@253931035@25.39.31 (260400-813878953):70)
W/FlagRegistrar(11210):         at android.os.Handler.handleCallback(Handler.java:959)
W/FlagRegistrar(11210):         at android.os.Handler.dispatchMessage(Handler.java:100)
W/FlagRegistrar(11210):         at cmjd.mC(:com.google.android.gms@253931035@25.39.31 (260400-813878953):1)
W/FlagRegistrar(11210):         at cmjd.dispatchMessage(:com.google.android.gms@253931035@25.39.31 (260400-813878953):5)
W/FlagRegistrar(11210):         at android.os.Looper.loopOnce(Looper.java:249)
W/FlagRegistrar(11210):         at android.os.Looper.loop(Looper.java:337)
W/FlagRegistrar(11210):         at android.os.HandlerThread.run(HandlerThread.java:85)
W/FlagRegistrar(11210): Caused by: bakp: 17: API: Phenotype.API is not available on this device. Connection failed with: ConnectionResult{statusCode=DEVELOPER_ERROR, resolution=null, message=null, clientMethodKey=null}
W/FlagRegistrar(11210):         at bbhp.a(:com.google.android.gms@253931035@25.39.31 (260400-813878953):15)
W/FlagRegistrar(11210):         at baml.a(:com.google.android.gms@253931035@25.39.31 (260400-813878953):1)
W/FlagRegistrar(11210):         at bami.c(:com.google.android.gms@253931035@25.39.31 (260400-813878953):5)
W/FlagRegistrar(11210):         ... 12 more
D/nativeloader(11210): Load /data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/base.apk!/lib/arm64-v8a/libconscrypt_gmscore_jni.so using class loader ns clns-10 (caller=/data/app/~~ZwYhXh4H1VB3sw0fO24Hwg==/com.google.android.gms-iCe98MjO9_VhUqCpIGyEug==/base.apk): ok
V/NativeCrypto(11210): Registering com/google/android/gms/org/conscrypt/NativeCrypto's 319 native methods...
I/ple.ai_keyboard(11210): hiddenapi: Accessing hidden method Ljava/security/spec/ECParameterSpec;->getCurveName()Ljava/lang/String; (runtime_flags=0, domain=core-platform, api=unsupported) from Lcom/google/android/gms/org/conscrypt/Platform; (domain=app) using reflection: allowed
I/ProviderInstaller(11210): Installed default security provider GmsCore_OpenSSL
I/ple.ai_keyboard(11210): hiddenapi: Accessing hidden field Ljava/net/Socket;->impl:Ljava/net/SocketImpl; (runtime_flags=0, domain=core-platform, api=unsupported) from Lcom/google/android/gms/org/conscrypt/Platform; (domain=app) using reflection: allowed
I/ple.ai_keyboard(11210): hiddenapi: Accessing hidden method Ljava/security/spec/ECParameterSpec;->setCurveName(Ljava/lang/String;)V (runtime_flags=0, domain=core-platform, api=unsupported) from Lcom/google/android/gms/org/conscrypt/Platform; (domain=app) using reflection: allowed
I/flutter (11210): KeyboardCloudSync: Remote settings received, applying locally...
I/flutter (11210): KeyboardCloudSync: ✓ Settings persisted to SharedPreferences
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/MainActivity(11210): ✓ Settings updated via MethodChannel
I/flutter (11210): KeyboardCloudSync: ✓ Native keyboard notified via MethodChannel
D/MainActivity(11210): sendBroadcast called with action: com.example.ai_keyboard.SETTINGS_CHANGED
D/MainActivity(11210): Broadcast sent: com.example.ai_keyboard.SETTINGS_CHANGED
I/flutter (11210): KeyboardCloudSync: ✓ Settings broadcast sent
I/flutter (11210): KeyboardCloudSync: ✓ Settings applied and keyboard notified
I/flutter (11210): ✅ [AuthWrapper] Cloud sync started successfully
I/flutter (11210): 🔵 [AuthWrapper] Showing welcome screen for first launch
I/flutter (11210): 🟢 [Firestore] Existing user sign-in updated successfully
I/flutter (11210): 🔵 [Firestore] Document path: users/J0CoM6lVQXelmXySQzpBGzGaILs2
I/flutter (11210): 🟢 [Firestore] Firestore write operation completed successfully
I/flutter (11210): 🟢 [GoogleAuth] Step 5 Success: User data saved to Firestore
I/flutter (11210): 🟢 [GoogleAuth] Google Sign-In flow completed successfully
I/flutter (11210): 🟢 [GoogleAuth] User bedugamkalyan@gmail.com is now authenticated
I/flutter (11210): 🟢 [LoginIllustraionScreen] Google Sign-In successful
I/flutter (11210): KeyboardCloudSync: Cloud sync stopped
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=io.flutter.embedding.android.FlutterActivity$1@5c66ad
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940142595, downTime=940142595, phoneEventTime=00:38:59.366 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940142628, downTime=940142595, phoneEventTime=00:38:59.399 } moveCount:0
I/flutter (11210): unhandled element <filter/>; Picture key: Svg loader
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=io.flutter.embedding.android.FlutterActivity$1@5c66ad
E/GoogleApiManager(11210): Failed to get service from broker. 
E/GoogleApiManager(11210): java.lang.SecurityException: Unknown calling package name 'com.google.android.gms'.
E/GoogleApiManager(11210):      at android.os.Parcel.createExceptionOrNull(Parcel.java:3256)
E/GoogleApiManager(11210):      at android.os.Parcel.createException(Parcel.java:3240)
E/GoogleApiManager(11210):      at android.os.Parcel.readException(Parcel.java:3223)
E/GoogleApiManager(11210):      at android.os.Parcel.readException(Parcel.java:3165)
E/GoogleApiManager(11210):      at bbkc.a(:com.google.android.gms@253931035@25.39.31 (260400-813878953):36)
E/GoogleApiManager(11210):      at bbid.z(:com.google.android.gms@253931035@25.39.31 (260400-813878953):143)
E/GoogleApiManager(11210):      at baoz.run(:com.google.android.gms@253931035@25.39.31 (260400-813878953):42)
E/GoogleApiManager(11210):      at android.os.Handler.handleCallback(Handler.java:959)
E/GoogleApiManager(11210):      at android.os.Handler.dispatchMessage(Handler.java:100)
E/GoogleApiManager(11210):      at cmjd.mC(:com.google.android.gms@253931035@25.39.31 (260400-813878953):1)
E/GoogleApiManager(11210):      at cmjd.dispatchMessage(:com.google.android.gms@253931035@25.39.31 (260400-813878953):5)
E/GoogleApiManager(11210):      at android.os.Looper.loopOnce(Looper.java:249)
E/GoogleApiManager(11210):      at android.os.Looper.loop(Looper.java:337)
E/GoogleApiManager(11210):      at android.os.HandlerThread.run(HandlerThread.java:85)

Performing hot restart...                                               
Restarted application in 1,379ms.
I/flutter (11210): Firebase already initialized, continuing...
V/MediaPlayer(11210): resetDrmState:  mDrmInfo=null mDrmProvisioningThread=null mPrepareDrmInProgress=false mActiveDrmScheme=false
V/MediaPlayer(11210): cleanDrmObj: mDrmObj=null mDrmSessionId=null
V/MediaPlayer(11210): resetDrmState:  mDrmInfo=null mDrmProvisioningThread=null mPrepareDrmInProgress=false mActiveDrmScheme=false
V/MediaPlayer(11210): cleanDrmObj: mDrmObj=null mDrmSessionId=null
D/MediaPlayer(11210): _release native called
D/MediaPlayer(11210): _release native finished
I/flutter (11210): 🔵 [AuthWrapper] Checking if this is first app launch...
I/flutter (11210): 🔵 [AuthWrapper] First launch: false
I/flutter (11210): 🔵 [AuthWrapper] User logged in (bedugamkalyan@gmail.com), starting cloud sync...
I/flutter (11210): KeyboardCloudSync: Starting cloud sync for user J0CoM6lVQXelmXySQzpBGzGaILs2
I/flutter (11210): 🔵 [AuthWrapper] Auth state changed - User: Not signed in
W/DynamiteModule(11210): Local module descriptor class for com.google.android.gms.providerinstaller.dynamite not found.
I/flutter (11210): 🔵 [AuthWrapper] Auth state changed - User: bedugamkalyan@gmail.com
I/flutter (11210): 🟢 [AuthWrapper] User authenticated, showing home screen
I/DynamiteModule(11210): Considering local module com.google.android.gms.providerinstaller.dynamite:0 and remote module com.google.android.gms.providerinstaller.dynamite:0
W/ProviderInstaller(11210): Failed to load providerinstaller module: No acceptable module com.google.android.gms.providerinstaller.dynamite found. Local version is 0 and remote version is 0.
W/ProviderInstaller(11210): Failed to report request stats: com.google.android.gms.common.security.ProviderInstallerImpl.reportRequestStats [class android.content.Context, long, long]
V/MediaPlayer(11210): resetDrmState:  mDrmInfo=null mDrmProvisioningThread=null mPrepareDrmInProgress=false mActiveDrmScheme=false
V/MediaPlayer(11210): cleanDrmObj: mDrmObj=null mDrmSessionId=null
I/flutter (11210): KeyboardCloudSync: Remote settings received, applying locally...
I/flutter (11210): KeyboardCloudSync: ✓ Settings persisted to SharedPreferences
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/MainActivity(11210): ✓ Settings updated via MethodChannel
I/flutter (11210): KeyboardCloudSync: ✓ Native keyboard notified via MethodChannel
D/MainActivity(11210): sendBroadcast called with action: com.example.ai_keyboard.SETTINGS_CHANGED
D/MainActivity(11210): Broadcast sent: com.example.ai_keyboard.SETTINGS_CHANGED
I/flutter (11210): KeyboardCloudSync: ✓ Settings broadcast sent
I/flutter (11210): KeyboardCloudSync: ✓ Settings applied and keyboard notified
I/flutter (11210): ✅ [AuthWrapper] Cloud sync started successfully
I/flutter (11210): 🔵 [AuthWrapper] Auth state changed - User: bedugamkalyan@gmail.com
I/flutter (11210): 🔵 [AuthWrapper] Auth state changed - User: bedugamkalyan@gmail.com
I/flutter (11210): 🟢 [AuthWrapper] User authenticated, showing home screen
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940172777, downTime=940172777, phoneEventTime=00:39:29.548 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940172838, downTime=940172777, phoneEventTime=00:39:29.609 } moveCount:0
D/VRI[MainActivity](11210): visibilityChanged oldVisibility=true newVisibility=false
W/ple.ai_keyboard(11210): mali: basep_raw_ioctl, 995: errno = 22.
W/ple.ai_keyboard(11210): mali: basep_raw_ioctl, 995: errno = 22.
W/ple.ai_keyboard(11210): mali: basep_raw_ioctl, 995: errno = 22.
W/ple.ai_keyboard(11210): mali: basep_raw_ioctl, 995: errno = 22.
W/ple.ai_keyboard(11210): mali: basep_raw_ioctl, 995: errno = 22.
W/ple.ai_keyboard(11210): mali: basep_raw_ioctl, 995: errno = 22.
W/ple.ai_keyboard(11210): mali: basep_raw_ioctl, 995: errno = 22.
W/ple.ai_keyboard(11210): mali: basep_raw_ioctl, 995: errno = 22.
D/BLASTBufferQueue(11210): [SurfaceView[com.example.ai_keyboard/com.example.ai_keyboard.MainActivity]#1](f:0,a:4) destructor()
D/BufferQueueConsumer(11210): [SurfaceView[com.example.ai_keyboard/com.example.ai_keyboard.MainActivity]#1(BLAST Consumer)1](id:2bca00000001,api:0,p:-1,c:11210) disconnect
D/BLASTBufferQueue(11210): [VRI[MainActivity]#0](f:0,a:2) destructor()
D/BufferQueueConsumer(11210): [VRI[MainActivity]#0(BLAST Consumer)0](id:2bca00000000,api:0,p:-1,c:11210) disconnect
D/ActivityThread(11210): Fail to check app heapsize due to java.lang.NoSuchMethodException: dalvik.system.VMRuntime.getBlockingGcCountForAlloc []
I/ForceDarkHelperStubImpl(11210): setViewRootImplForceDark: false for com.example.ai_keyboard.MainActivity@a051ff5, reason: DarkModeEnabled
D/AppScoutStateMachine(11210): 11210-ScoutStateMachinecreated
D/SecurityManager(11210): checkAccessControl flag1
D/BufferQueueConsumer(11210): [](id:2bca00000003,api:0,p:-1,c:11210) connect: controlledByApp=false
D/BLASTBufferQueue(11210): [VRI[MainActivity]#3](f:0,a:0) constructor()
D/BLASTBufferQueue(11210): [VRI[MainActivity]#3](f:0,a:0) update width=1220 height=2712 format=-3 mTransformHint=0
D/BufferQueueConsumer(11210): [](id:2bca00000004,api:0,p:-1,c:11210) connect: controlledByApp=false
D/BLASTBufferQueue(11210): [SurfaceView[com.example.ai_keyboard/com.example.ai_keyboard.MainActivity]#4](f:0,a:0) constructor()
D/BLASTBufferQueue(11210): [SurfaceView[com.example.ai_keyboard/com.example.ai_keyboard.MainActivity]#4](f:0,a:0) update width=1220 height=2712 format=4 mTransformHint=0
E/mali_gralloc(11210): ERROR: Format allocation info not found for format: 38
E/mali_gralloc(11210): ERROR: Format allocation info not found for format: 0
E/ple.ai_keyboard(11210): Invalid base format! req_base_format = 0x0, req_format = 0x38
E/mali_gralloc(11210): ERROR: Unrecognized and/or unsupported format 0x38 and usage 0xb00
E/mali_gralloc(11210): ERROR: Format allocation info not found for format: 3b
E/mali_gralloc(11210): ERROR: Format allocation info not found for format: 0
E/ple.ai_keyboard(11210): Invalid base format! req_base_format = 0x0, req_format = 0x3b
E/mali_gralloc(11210): ERROR: Unrecognized and/or unsupported format 0x3b and usage 0xb00
E/mali_gralloc(11210): ERROR: Format allocation info not found for format: 38
E/mali_gralloc(11210): ERROR: Format allocation info not found for format: 0
E/ple.ai_keyboard(11210): Invalid base format! req_base_format = 0x0, req_format = 0x38
E/mali_gralloc(11210): ERROR: Unrecognized and/or unsupported format 0x38 and usage 0xb00
E/mali_gralloc(11210): ERROR: Format allocation info not found for format: 3b
E/mali_gralloc(11210): ERROR: Format allocation info not found for format: 0
E/ple.ai_keyboard(11210): Invalid base format! req_base_format = 0x0, req_format = 0x3b
E/mali_gralloc(11210): ERROR: Unrecognized and/or unsupported format 0x3b and usage 0xb00
D/VRI[MainActivity](11210): vri.reportNextDraw android.view.ViewRootImpl.performTraversals:4985 android.view.ViewRootImpl.doTraversal:3572 android.view.ViewRootImpl$TraversalRunnable.run:11792 android.view.Choreographer$CallbackRecord.run:1821 android.view.Choreographer$CallbackRecord.run:1830 
D/SurfaceView(11210): UPDATE Surface(name=SurfaceView[com.example.ai_keyboard/com.example.ai_keyboard.MainActivity]#284761)/@0xdaf086d, mIsProjectionMode = false
D/VRI[MainActivity](11210): Setup new sync=wmsSync-VRI[MainActivity]#7
D/BLASTBufferQueue(11210): [VRI[MainActivity]#3](f:0,a:1) acquireNextBufferLocked size=1220x2712 mFrameNumber=1 applyTransaction=true mTimestamp=940179165170500(auto) mPendingTransactions.size=0 graphicBufferId=48146583388205 transform=0
D/VRI[MainActivity](11210): vri.reportDrawFinished
E/NativeTurboSchedManagerJni(11210): open /dev/metis failed!
E/NativeTurboSchedManagerJni(11210): open /dev/metis failed!
D/BLASTBufferQueue(11210): [SurfaceView[com.example.ai_keyboard/com.example.ai_keyboard.MainActivity]#4](f:0,a:1) acquireNextBufferLocked size=1220x2712 mFrameNumber=1 applyTransaction=true mTimestamp=940179176411807(auto) mPendingTransactions.size=0 graphicBufferId=48146583388197 transform=0
I/HandWritingStubImpl(11210): refreshLastKeyboardType: 1
I/HandWritingStubImpl(11210): getCurrentKeyboardType: 1
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940180129, downTime=940180129, phoneEventTime=00:39:36.900 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940180328, downTime=940180129, phoneEventTime=00:39:37.099 } moveCount:25
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940181379, downTime=940181379, phoneEventTime=00:39:38.150 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940181478, downTime=940181379, phoneEventTime=00:39:38.249 } moveCount:0
I/HandWritingStubImpl(11210): refreshLastKeyboardType: 1
I/HandWritingStubImpl(11210): getCurrentKeyboardType: 1
D/OpenAIConfig(11210): OpenAIConfig initializing...
D/OpenAIConfig(11210): hasApiKey: encrypted=false, direct=false, result=false
D/OpenAIConfig(11210): No API key found, setting new key...
D/OpenAIConfig(11210): API key stored securely
D/OpenAIConfig(11210): API key set via encrypted storage
D/OpenAIConfig(11210): Returning cached API key
D/OpenAIConfig(11210): Returning cached API key
D/OpenAIConfig(11210): Authorization header created successfully
D/OpenAIConfig(11210): OpenAIConfig initialization complete:
D/OpenAIConfig(11210):   - API key available: true
D/OpenAIConfig(11210):   - Auth header available: true
D/OpenAIConfig(11210):   - AI features enabled: true
D/AIKeyboardService(11210): OpenAI configuration initialized successfully
D/AIKeyboardService(11210): 🔧 Initializing core components...
D/AIKeyboardService(11210): ✅ UserDictionaryManager initialized
D/AIKeyboardService(11210): ✅ MultilingualDictionary initialized
W/TransliterationEngine(11210): Unsupported language: en
D/AIKeyboardService(11210): ✅ TransliterationEngine initialized
D/AIKeyboardService(11210): ✅ IndicScriptHelper initialized
D/AIKeyboardService(11210): ✅ UnifiedAutocorrectEngine initialized
D/MultilingualDict(11210): 📚 Starting lazy load for language: en
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for en
D/MultilingualDict(11210): 📚 Starting lazy load for language: hi
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for hi
D/MultilingualDict(11210): 📚 Starting lazy load for language: te
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for te
D/MultilingualDict(11210): 📚 Starting lazy load for language: ta
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for ta
D/AIKeyboardService(11210): ✅ UnifiedAutocorrectEngine preloaded with 4 languages
D/AIKeyboardService(11210): ✅ Core components initialization COMPLETE
W/MultilingualDict(11210): ⚠️ Could not load words for te: dictionaries/te_words.txt
W/MultilingualDict(11210): ⚠️ Could not load bigrams for te
W/MultilingualDict(11210): ⚠️ Could not load words for ta: dictionaries/ta_words.txt
I/AIKeyboardService(11210): Settings loaded
D/AIKeyboardService(11210): 🔵 [Dictionary] Starting async dictionary load...
W/MultilingualDict(11210): ⚠️ Could not load bigrams for ta
D/AIKeyboardService(11210): Custom prompts loaded - Grammar: 0, Tones: 0, Assistants: 0
D/LanguageManager(11210): Loaded preferences - Current: en, Enabled: [en]
D/LanguageManager(11210): Enabled multiple languages by default: [en, es, fr, de, hi]
D/AIKeyboardService(11210): Connected user dictionary manager to autocorrect engine
D/AIKeyboardService(11210): User dictionary integration complete
D/AIKeyboardService(11210): Enhanced prediction system initialized
D/SwipeAutocorrectEngine(11210): ✅ SwipeAutocorrectEngine integrated with UnifiedAutocorrectEngine
D/AIKeyboardService(11210): ✅ SwipeAutocorrectEngine integrated with UnifiedAutocorrectEngine
D/LanguageManager(11210): Added language change listener
D/AIKeyboardService(11210): Multilingual components initialized successfully
D/AIKeyboardService(11210): AI Bridge initialized successfully
D/AIResponseCache(11210): Loaded 0 cache entries into memory
D/AIKeyboardService(11210): Advanced AI Service initialized
D/AIKeyboardService(11210): 🟢 AI service marked as ready
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 1), retrying once more...
D/AIResponseCache(11210): Loaded 0 cache entries into memory
D/CleverTypeAIService(11210): CleverTypeAIService initializing...
D/OpenAIConfig(11210): hasApiKey: encrypted=true, direct=false, result=true
D/OpenAIConfig(11210): Returning cached API key
D/OpenAIConfig(11210): Returning cached API key
D/CleverTypeAIService(11210): API key exists: sk-proj-7G...
D/OpenAIConfig(11210): Returning cached API key
D/OpenAIConfig(11210): Authorization header created successfully
D/CleverTypeAIService(11210): Authorization header available: true
D/AIKeyboardService(11210): CleverType AI Service initialized successfully
D/AIKeyboardService(11210): Theme communication ready (broadcast-based)
D/MultilingualDict(11210): 📖 Loaded 171 words for hi
D/AIKeyboardService(11210): Enhanced Caps/Shift Manager initialized successfully
D/ClipboardHistoryManager(11210): Initializing ClipboardHistoryManager
D/MultilingualDict(11210): 📊 Loaded 4 bigrams for hi
D/MultilingualDict(11210): 📖 Loaded 172 words for en
D/ClipboardHistoryManager(11210): ClipboardHistoryManager initialized with 0 history items and 0 templates
D/MultilingualDict(11210): 📊 Loaded 10 bigrams for en
D/DictionaryManager(11210): Initializing DictionaryManager
D/DictionaryManager(11210): No dictionary entries in Flutter prefs
D/DictionaryManager(11210): Rebuilt shortcut map with 0 entries
D/DictionaryManager(11210): DictionaryManager initialized with 0 entries (enabled: true)
D/AIKeyboardService(11210): ✅ User dictionary sync initiated
D/AIKeyboardService(11210): User dictionary manager initialized
D/AIKeyboardService(11210): Periodic sync scheduled every 10 minutes
D/AIKeyboardService(11210): Broadcast receiver registered successfully
D/AIKeyboardService(11210): Settings polling disabled (using BroadcastReceiver as authoritative source)
D/AIKeyboardService(11210): ✅ Corrected current language to: en
D/AIKeyboardService(11210): ✅ Language prefs loaded: enabled=[en], current=en (idx=0), multi=false
D/AIKeyboardService(11210): ✅ Feature flags: translit=true, reverse=false
D/TypingSyncAudit(11210): ═══════════════════════════════════════════════════
D/TypingSyncAudit(11210): {
D/TypingSyncAudit(11210):   "analysis": "TypingSyncAudit - Feature Status Report",
D/TypingSyncAudit(11210):   "timestamp": 1759691380783,
D/TypingSyncAudit(11210):   "features": {
D/TypingSyncAudit(11210):     "suggestions": {
D/TypingSyncAudit(11210):       "updateAISuggestions": false,
D/TypingSyncAudit(11210):       "suggestionContainerInflation": true,
D/TypingSyncAudit(11210):       "nextWordModel": true
D/TypingSyncAudit(11210):     },
D/TypingSyncAudit(11210):     "content": {
D/TypingSyncAudit(11210):       "emojiPipeline": true,
D/TypingSyncAudit(11210):       "clipboardSuggester": true
D/TypingSyncAudit(11210):     },
D/TypingSyncAudit(11210):     "typing": {
D/TypingSyncAudit(11210):       "autocap": true,
D/TypingSyncAudit(11210):       "doubleSpacePeriod": true,
D/TypingSyncAudit(11210):       "autocorrectEngine": true
D/TypingSyncAudit(11210):     },
D/TypingSyncAudit(11210):     "ui": {
D/TypingSyncAudit(11210):       "popupPreviewSetting": true
D/TypingSyncAudit(11210):     },
D/TypingSyncAudit(11210):     "dictionary": {
D/TypingSyncAudit(11210):       "dictionaryManager": true,
D/TypingSyncAudit(11210):       "languageManager": true
D/TypingSyncAudit(11210):     }
D/TypingSyncAudit(11210):   },
D/TypingSyncAudit(11210):   "summary": {
D/TypingSyncAudit(11210):     "totalFeatures": 11,
D/TypingSyncAudit(11210):     "implementedFeatures": 10,
D/TypingSyncAudit(11210):     "implementationRate": "90%"
D/TypingSyncAudit(11210):   }
D/TypingSyncAudit(11210): }
D/TypingSyncAudit(11210): ═══════════════════════════════════════════════════
W/TypingSyncAudit(11210): ⚠ Feature gaps detected:
W/TypingSyncAudit(11210):   1. Unified updateAISuggestions method
D/AIKeyboardService(11210): ✅ AIKeyboardService onCreate completed successfully
D/MultilingualDict(11210): ✅ Loaded te: 0 words, 0 bigrams (5ms)
D/WordDatabase(11210): Populating initial word data...
D/MultilingualDict(11210): ✅ Loaded ta: 0 words, 0 bigrams (8ms)
D/MultilingualDict(11210): Language en already loaded or loading
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for en
D/MediaPlayer(11210): finalize() native_finalize called
D/MultilingualDict(11210): 📚 Starting lazy load for language: es
D/MediaPlayer(11210): finalize() native_finalize finished
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for es
D/MultilingualDict(11210): 📚 Starting lazy load for language: fr
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for fr
D/MultilingualDict(11210): 📚 Starting lazy load for language: de
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for de
D/MultilingualDict(11210): Language hi already loaded or loading
D/UnifiedAutocorrectEngine(11210): Preloaded dictionary for hi
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~QPRZ0qWkoUnU5Iy8m-jIAA==/com.google.android.marvin.talkback-NFF5xADhlWaSZ1r49c4G8g==/base.apk' with 1 weak references
D/MultilingualDict(11210): 📖 Loaded 119 words for es
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~QPRZ0qWkoUnU5Iy8m-jIAA==/com.google.android.marvin.talkback-NFF5xADhlWaSZ1r49c4G8g==/split_config.arm64_v8a.apk' with 1 weak references
D/MultilingualDict(11210): ✅ Loaded hi: 171 words, 4 bigrams (37ms)
D/MultilingualDict(11210): ✅ Loaded en: 172 words, 10 bigrams (42ms)
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~ArVEi8OC5YnNfb_c_NiCrw==/cn.wps.moffice_eng-dlqYrQCpxEXioyz5Ea0ouA==/base.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~ArVEi8OC5YnNfb_c_NiCrw==/cn.wps.moffice_eng-dlqYrQCpxEXioyz5Ea0ouA==/split_config.arm64_v8a.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~ArVEi8OC5YnNfb_c_NiCrw==/cn.wps.moffice_eng-dlqYrQCpxEXioyz5Ea0ouA==/split_config.en.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~ArVEi8OC5YnNfb_c_NiCrw==/cn.wps.moffice_eng-dlqYrQCpxEXioyz5Ea0ouA==/split_config.xxhdpi.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~ArVEi8OC5YnNfb_c_NiCrw==/cn.wps.moffice_eng-dlqYrQCpxEXioyz5Ea0ouA==/split_vassonic.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~ArVEi8OC5YnNfb_c_NiCrw==/cn.wps.moffice_eng-dlqYrQCpxEXioyz5Ea0ouA==/split_vassonic.config.xxhdpi.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~jNSAyFpZ6Km6VD2624pCzQ==/com.anthropic.claude-KNSH7_KD1XBcicSeJPlKbA==/base.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~jNSAyFpZ6Km6VD2624pCzQ==/com.anthropic.claude-KNSH7_KD1XBcicSeJPlKbA==/split_config.arm64_v8a.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~jNSAyFpZ6Km6VD2624pCzQ==/com.anthropic.claude-KNSH7_KD1XBcicSeJPlKbA==/split_config.en.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~jNSAyFpZ6Km6VD2624pCzQ==/com.anthropic.claude-KNSH7_KD1XBcicSeJPlKbA==/split_config.xxhdpi.apk' with 1 weak references
D/MultilingualDict(11210): 📖 Loaded 164 words for fr
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~duVmjGcLwlNQsU1OLwHbFQ==/com.truecaller-yWkg9bY7CyRSm5z2F7FcZQ==/base.apk' with 1 weak references
D/MultilingualDict(11210): 📊 Loaded 4 bigrams for es
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~duVmjGcLwlNQsU1OLwHbFQ==/com.truecaller-yWkg9bY7CyRSm5z2F7FcZQ==/split_config.arm64_v8a.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~duVmjGcLwlNQsU1OLwHbFQ==/com.truecaller-yWkg9bY7CyRSm5z2F7FcZQ==/split_config.xxhdpi.apk' with 1 weak references
D/AIKeyboardService(11210): Loaded 500 common words, 0 business words, 50 tech words, 419 corrections, 58 contractions
D/MiuiCameraCoveredManager(11210): java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String android.provider.MiuiSettings$SettingsCloudData$CloudData.getString(java.lang.String, java.lang.String)' on a null object reference
D/MultilingualDict(11210): 📊 Loaded 4 bigrams for fr
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~duVmjGcLwlNQsU1OLwHbFQ==/com.truecaller-yWkg9bY7CyRSm5z2F7FcZQ==/split_insights_category_model.apk' with 1 weak references
W/ple.ai_keyboard(11210): ApkAssets: Deleting an ApkAssets object '<empty> and /data/app/~~duVmjGcLwlNQsU1OLwHbFQ==/com.truecaller-yWkg9bY7CyRSm5z2F7FcZQ==/split_insights_category_model.config.arm64_v8a.apk' with 1 weak references
D/VRI[InputMethod](11210): hardware acceleration = true, forceHwAccelerated = false
D/MultilingualDict(11210): 📖 Loaded 172 words for de
D/MultilingualDict(11210): 📊 Loaded 4 bigrams for de
D/InputEventReceiver(11210): Input log is disabled in InputEventReceiver.
D/InputEventReceiver(11210): Input log is disabled in InputEventReceiver.
I/AIKeyboardService(11210): 🟢 [Dictionary] Async dictionary load completed
D/MultilingualDict(11210): ✅ Loaded es: 119 words, 4 bigrams (16ms)
D/MultilingualDict(11210): ✅ Loaded fr: 164 words, 4 bigrams (21ms)
I/WordDatabase(11210): ✅ Loaded 450 words from assets
D/MultilingualDict(11210): ✅ Loaded de: 172 words, 4 bigrams (29ms)
I/HandWritingStubImpl(11210): getCurrentKeyboardType: 1
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=false HintedRow=false HintedSymbols=true Borderless=false OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=true LP=200ms Bottom=3px Utility=emoji
D/CapsShiftManager(11210): Auto-capitalization applied
D/AIKeyboardService(11210): onStartInput - showing initial suggestions
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 2), retrying once more...
D/AIKeyboardService(11210): Custom prompts loaded - Grammar: 0, Tones: 0, Assistants: 0
I/WordDatabase(11210): ✅ Loaded correction mappings from assets
D/WordDatabase(11210): Initial data population completed
D/AIServiceBridge(11210): Enhanced AI engines initialized successfully
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 3), retrying once more...
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 4), retrying once more...
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 5), retrying once more...
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
E/AIKeyboardService(11210): Suggestion container still not ready after retries, aborting.
D/UserDictionaryManager(11210): 💾 Local user dictionary saved (0 entries).
I/UserDictionaryManager(11210): 🔄 Merged 0 cloud words into local cache.
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 1), retrying once more...
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 2), retrying once more...
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 3), retrying once more...
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 4), retrying once more...
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
W/AIKeyboardService(11210): Suggestion container not ready (attempt 5), retrying once more...
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
E/AIKeyboardService(11210): Suggestion container still not ready after retries, aborting.
D/AIKeyboardService(11210): Autocorrect Test: cacheSize=0
D/AIKeyboardService(11210): Autocorrect Test: loadedLanguages=[]
D/AIKeyboardService(11210): Autocorrect Test: totalWords=0
D/AIKeyboardService(11210): Autocorrect Test: userWords=0
D/SwipeAutocorrectEngine(11210): SwipeAutocorrectEngine initialized in 5ms
D/SwipeAutocorrectEngine(11210): Loaded 315 words, 0 user words, 11 bigrams
D/AIKeyboardService(11210): 📊 Firestore word frequencies loading for en
D/AIKeyboardService(11210): Swipe autocorrect engine and dictionary initialization completed
D/AIKeyboardService(11210): 🔵 [Dictionary] Starting async dictionary load...
D/AIKeyboardService(11210): Loaded 500 common words, 0 business words, 50 tech words, 419 corrections, 58 contractions
I/AIKeyboardService(11210): 🟢 [Dictionary] Async dictionary load completed
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940198349, downTime=940198349, phoneEventTime=00:39:55.120 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940198505, downTime=940198349, phoneEventTime=00:39:55.276 } moveCount:30
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940204028, downTime=940204028, phoneEventTime=00:40:00.799 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940204402, downTime=940204028, phoneEventTime=00:40:01.173 } moveCount:51
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940204870, downTime=940204870, phoneEventTime=00:40:01.642 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940204939, downTime=940204870, phoneEventTime=00:40:01.710 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940210910, downTime=940210910, phoneEventTime=00:40:07.681 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940210953, downTime=940210910, phoneEventTime=00:40:07.724 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940211695, downTime=940211695, phoneEventTime=00:40:08.466 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940211769, downTime=940211695, phoneEventTime=00:40:08.540 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940212380, downTime=940212380, phoneEventTime=00:40:09.151 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940212486, downTime=940212380, phoneEventTime=00:40:09.257 } moveCount:0
I/flutter (11210): ✅ Keyboard settings saved
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/MainActivity(11210): ✓ Settings updated via MethodChannel
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=false OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=true LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
D/MainActivity(11210): ✓ notifyConfigChange received
D/MainActivity(11210): Settings changed broadcast sent
I/flutter (11210): ✓ Notified keyboard
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=false OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=true LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940221768, downTime=940221768, phoneEventTime=00:40:18.539 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940222103, downTime=940221768, phoneEventTime=00:40:18.874 } moveCount:46
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940222746, downTime=940222746, phoneEventTime=00:40:19.517 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940222819, downTime=940222746, phoneEventTime=00:40:19.590 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940223887, downTime=940223887, phoneEventTime=00:40:20.658 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940223996, downTime=940223887, phoneEventTime=00:40:20.767 } moveCount:0
I/flutter (11210): ✅ Keyboard settings saved
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/MainActivity(11210): ✓ Settings updated via MethodChannel
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=true LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
D/MainActivity(11210): ✓ notifyConfigChange received
D/MainActivity(11210): Settings changed broadcast sent
I/flutter (11210): ✓ Notified keyboard
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=true LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940225495, downTime=940225495, phoneEventTime=00:40:22.266 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940225835, downTime=940225495, phoneEventTime=00:40:22.606 } moveCount:52
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940226147, downTime=940226147, phoneEventTime=00:40:22.918 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940226354, downTime=940226147, phoneEventTime=00:40:23.125 } moveCount:32
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940226855, downTime=940226855, phoneEventTime=00:40:23.626 } moveCount:0
I/MiInputConsumer(11210): optimized resample latency: 6051565 ns
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940227312, downTime=940226855, phoneEventTime=00:40:24.083 } moveCount:79
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940228206, downTime=940228206, phoneEventTime=00:40:24.977 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940228296, downTime=940228206, phoneEventTime=00:40:25.067 } moveCount:0
I/flutter (11210): ✅ Keyboard settings saved
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/MainActivity(11210): ✓ Settings updated via MethodChannel
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
D/MainActivity(11210): ✓ notifyConfigChange received
D/MainActivity(11210): Settings changed broadcast sent
I/flutter (11210): ✓ Notified keyboard
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940228864, downTime=940228864, phoneEventTime=00:40:25.635 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940229290, downTime=940228864, phoneEventTime=00:40:26.061 } moveCount:67
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940229647, downTime=940229647, phoneEventTime=00:40:26.418 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940230127, downTime=940229647, phoneEventTime=00:40:26.898 } moveCount:80
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940231146, downTime=940231146, phoneEventTime=00:40:27.918 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940231443, downTime=940231146, phoneEventTime=00:40:28.214 } moveCount:47
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940232499, downTime=940232499, phoneEventTime=00:40:29.270 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940233023, downTime=940232499, phoneEventTime=00:40:29.794 } moveCount:59
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940234956, downTime=940234956, phoneEventTime=00:40:31.727 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940235026, downTime=940234956, phoneEventTime=00:40:31.797 } moveCount:0
I/flutter (11210): ✅ Keyboard settings saved
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/MainActivity(11210): ✓ Settings updated via MethodChannel
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
D/MainActivity(11210): ✓ notifyConfigChange received
D/MainActivity(11210): Settings changed broadcast sent
I/flutter (11210): ✓ Notified keyboard
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940235510, downTime=940235510, phoneEventTime=00:40:32.281 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940235576, downTime=940235510, phoneEventTime=00:40:32.347 } moveCount:0
I/flutter (11210): ✅ Keyboard settings saved
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/MainActivity(11210): ✓ Settings updated via MethodChannel
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
D/MainActivity(11210): ✓ notifyConfigChange received
D/MainActivity(11210): Settings changed broadcast sent
I/flutter (11210): ✓ Notified keyboard
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940239657, downTime=940239657, phoneEventTime=00:40:36.428 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940240044, downTime=940239657, phoneEventTime=00:40:36.815 } moveCount:72
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940241713, downTime=940241713, phoneEventTime=00:40:38.484 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940242101, downTime=940241713, phoneEventTime=00:40:38.872 } moveCount:61
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940242669, downTime=940242669, phoneEventTime=00:40:39.440 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940242747, downTime=940242669, phoneEventTime=00:40:39.518 } moveCount:0
I/flutter (11210): ✅ Keyboard settings saved
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/MainActivity(11210): ✓ Settings updated via MethodChannel
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
D/MainActivity(11210): ✓ notifyConfigChange received
D/MainActivity(11210): Settings changed broadcast sent
I/flutter (11210): ✓ Notified keyboard
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940246551, downTime=940246551, phoneEventTime=00:40:43.322 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940246611, downTime=940246551, phoneEventTime=00:40:43.382 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940248430, downTime=940248430, phoneEventTime=00:40:45.201 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940248990, downTime=940248430, phoneEventTime=00:40:45.761 } moveCount:88
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940249483, downTime=940249483, phoneEventTime=00:40:46.254 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940249944, downTime=940249483, phoneEventTime=00:40:46.715 } moveCount:74
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940235510, downTime=940235510, phoneEventTime=00:40:32.281 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940235576, downTime=940235510, phoneEventTime=00:40:32.347 } moveCount:0
I/flutter (11210): ✅ Keyboard settings saved
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/MainActivity(11210): ✓ Settings updated via MethodChannel
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
D/MainActivity(11210): ✓ notifyConfigChange received
D/MainActivity(11210): Settings changed broadcast sent
I/flutter (11210): ✓ Notified keyboard
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940239657, downTime=940239657, phoneEventTime=00:40:36.428 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940240044, downTime=940239657, phoneEventTime=00:40:36.815 } moveCount:72
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940241713, downTime=940241713, phoneEventTime=00:40:38.484 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940242101, downTime=940241713, phoneEventTime=00:40:38.872 } moveCount:61
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940242669, downTime=940242669, phoneEventTime=00:40:39.440 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940242747, downTime=940242669, phoneEventTime=00:40:39.518 } moveCount:0
I/flutter (11210): ✅ Keyboard settings saved
D/MainActivity(11210): ✓ Settings V2 persisted to SharedPreferences
D/TypingSync(11210): Settings applied: popup=false, autocorrect=true, emoji=true, nextWord=true, clipboard=true
D/MainActivity(11210): Broadcast sent: SETTINGS_CHANGED
D/MainActivity(11210): ✓ Settings updated via MethodChannel
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
D/MainActivity(11210): ✓ notifyConfigChange received
D/MainActivity(11210): Settings changed broadcast sent
I/flutter (11210): ✓ Notified keyboard
D/AIKeyboardService(11210): SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): 📥 Loading settings from broadcast...
D/AIKeyboardService(11210): ✅ Config applied complete: NumRow=true HintedRow=false HintedSymbols=true Borderless=true OneHanded=false@right(87%) Scale=1.0x1.0 Spacing=5dp/2dp FontScale=1.0 Popup=false LP=200ms Bottom=3px Utility=emoji
D/AIKeyboardService(11210): ✅ Settings applied successfully
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=null
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): Keyboard layout reloaded - NumberRow: true, Language: en
W/AIKeyboardService(11210): Cannot recreate toolbar - main layout not found
D/AIKeyboardService(11210): Settings and theme applied successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940246551, downTime=940246551, phoneEventTime=00:40:43.322 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940246611, downTime=940246551, phoneEventTime=00:40:43.382 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940248430, downTime=940248430, phoneEventTime=00:40:45.201 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940248990, downTime=940248430, phoneEventTime=00:40:45.761 } moveCount:88
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940249483, downTime=940249483, phoneEventTime=00:40:46.254 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940249944, downTime=940249483, phoneEventTime=00:40:46.715 } moveCount:74
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=io.flutter.embedding.android.FlutterActivity$1@5c66ad
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940271627, downTime=940271627, phoneEventTime=00:41:08.398 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940271713, downTime=940271627, phoneEventTime=00:41:08.484 } moveCount:0

══╡ EXCEPTION CAUGHT BY RENDERING LIBRARY ╞═════════════════════════════════════════════════════════
The following assertion was thrown during layout:
A RenderFlex overflowed by 7.9 pixels on the right.

The relevant error-causing widget was:
  Row Row:file:///Users/kalyan/AI-keyboard/lib/theme/theme_editor_v2.dart:47:20

To inspect this widget in Flutter DevTools, visit:
http://127.0.0.1:9100/#/inspector?uri=http%3A%2F%2F127.0.0.1%3A53885%2FAtPECYCj9H0%3D%2F&inspectorRef=inspector-0

The overflowing RenderFlex has an orientation of Axis.horizontal.
The edge of the RenderFlex that is overflowing has been marked in the rendering with a yellow and
black striped pattern. This is usually caused by the contents being too big for the RenderFlex.
Consider applying a flex factor (e.g. using an Expanded widget) to force the children of the
RenderFlex to fit within the available space instead of being sized to their natural size.
This is considered an error condition because it indicates that there is content that cannot be
seen. If the content is legitimately bigger than the available space, consider clipping it with a
ClipRect widget before putting it in the flex, or using a scrollable container rather than a Flex,
like a ListView.
The specific RenderFlex in question is: RenderFlex#52394 relayoutBoundary=up4 OVERFLOWING:
  creator: Row ← Padding ← ConstrainedBox ← Container ← Column ← KeyedSubtree-[GlobalKey#7230c] ←
    _BodyBuilder ← MediaQuery ← LayoutId-[<_ScaffoldSlot.body>] ← CustomMultiChildLayout ←
    _ActionsScope ← Actions ← ⋯
  parentData: offset=Offset(16.0, 0.0) (can use size)
  constraints: BoxConstraints(0.0<=w<=374.7, h=50.0)
  size: Size(374.7, 50.0)
  direction: horizontal
  mainAxisAlignment: start
  mainAxisSize: max
  crossAxisAlignment: center
  textDirection: ltr
  verticalDirection: down
  spacing: 0.0
◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤◢◤
════════════════════════════════════════════════════════════════════════════════════════════════════

I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940279676, downTime=940279676, phoneEventTime=00:41:16.447 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940279730, downTime=940279676, phoneEventTime=00:41:16.501 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940280827, downTime=940280827, phoneEventTime=00:41:17.598 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940280883, downTime=940280827, phoneEventTime=00:41:17.654 } moveCount:0
D/AIKeyboardService(11210): 🎨 Theme changed: Red, applying to keyboard...
D/AIKeyboardService(11210): 🎨 Applying comprehensive theme: Red (animated)
D/AIKeyboardService(11210): ✅ Emoji panel (new) themed
D/AIKeyboardService(11210): 🎨 Complete theme application finished successfully
D/MainActivity(11210): 🎨 Theme V2 changed: Red (theme_red)
D/MainActivity(11210): Starting Theme V2 change notification: Red
D/MainActivity(11210): Theme V2 data - ID: theme_red, Data length: 1251, Settings changed: true
D/MainActivity(11210): 🎨 Theme V2 broadcast sent: Red
D/AIKeyboardService(11210): 🎨 THEME_CHANGED broadcast received! Theme: Red (theme_red), V2: true, Has data: true
D/AIKeyboardService(11210): Loaded theme after reload: Red (theme_red)
D/AIKeyboardService(11210): Keyboard view not ready, queuing V2 theme update for later
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=io.flutter.embedding.android.FlutterActivity$1@5c66ad

D/MainActivity(11210): Starting Theme V2 change notification: Red
D/MainActivity(11210): Theme V2 data - ID: theme_red, Data length: 1251, Settings changed: true
D/MainActivity(11210): 🎨 Theme V2 broadcast sent: Red
D/AIKeyboardService(11210): 🎨 THEME_CHANGED broadcast received! Theme: Red (theme_red), V2: true, Has data: true
D/AIKeyboardService(11210): Loaded theme after reload: Red (theme_red)
D/AIKeyboardService(11210): Keyboard view not ready, queuing V2 theme update for later
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=io.flutter.embedding.android.FlutterActivity$1@5c66ad
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940302390, downTime=940302390, phoneEventTime=00:41:39.161 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940302496, downTime=940302390, phoneEventTime=00:41:39.267 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940303367, downTime=940303367, phoneEventTime=00:41:40.138 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940303450, downTime=940303367, phoneEventTime=00:41:40.221 } moveCount:0
D/AIKeyboardService(11210): Periodic user dictionary sync completed
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940304841, downTime=940304841, phoneEventTime=00:41:41.612 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940304869, downTime=940304841, phoneEventTime=00:41:41.640 } moveCount:0
I/UserDictionaryManager(11210): ☁️ Synced 0 user words to Firestore.
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940305437, downTime=940305437, phoneEventTime=00:41:42.208 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940305506, downTime=940305437, phoneEventTime=00:41:42.277 } moveCount:0
D/MainActivity(11210): Emoji settings updated: skinTone=, historyMaxSize=90
D/MainActivity(11210): Emoji settings broadcast sent
D/AIKeyboardService(11210): EMOJI_SETTINGS_CHANGED broadcast received!
I/flutter (11210): ✓ Skin tone sent to keyboard:  (index 0)
D/AIKeyboardService(11210): Reloading emoji settings from broadcast...
D/AIKeyboardService(11210): Emoji settings reloaded successfully
D/AIKeyboardService(11210): Emoji settings reloaded successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940306215, downTime=940306215, phoneEventTime=00:41:42.986 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940306343, downTime=940306215, phoneEventTime=00:41:43.114 } moveCount:15
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940307411, downTime=940307411, phoneEventTime=00:41:44.182 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940307459, downTime=940307411, phoneEventTime=00:41:44.230 } moveCount:0
D/MainActivity(11210): Emoji settings updated: skinTone=🏿, historyMaxSize=90
D/AIKeyboardService(11210): EMOJI_SETTINGS_CHANGED broadcast received!
D/AIKeyboardService(11210): Reloading emoji settings from broadcast...
D/AIKeyboardService(11210): Emoji settings reloaded successfully
D/AIKeyboardService(11210): Emoji settings reloaded successfully!
D/MainActivity(11210): Emoji settings broadcast sent
I/flutter (11210): ✓ Skin tone sent to keyboard: 🏿 (index 5)
led with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940366364, downTime=940366364, phoneEventTime=00:42:43.135 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940366435, downTime=940366364, phoneEventTime=00:42:43.206 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'pot'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'pot'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'pot', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='pot', prevToken='yo7', lang='en'
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940366526, downTime=940366526, phoneEventTime=00:42:43.297 } moveCount:0
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940366597, downTime=940366526, phoneEventTime=00:42:43.368 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'pott'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'pott'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'pott', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='pott', prevToken='yo7', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940366739, downTime=940366739, phoneEventTime=00:42:43.510 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940366800, downTime=940366739, phoneEventTime=00:42:43.571 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'potti'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'potti'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'potti', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='potti', prevToken='yo7', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940367720, downTime=940367720, phoneEventTime=00:42:44.491 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940367755, downTime=940367720, phoneEventTime=00:42:44.526 } moveCount:0
I/flutter (11210): Dictionary settings broadcast sent
I/ImeTracker(11210): com.example.ai_keyboard:7e02fc26: onRequestHide at ORIGIN_CLIENT reason HIDE_SOFT_INPUT fromUser false
D/MainActivity(11210): sendBroadcast called with action: com.example.ai_keyboard.DICTIONARY_CHANGED
V/InputMethodService(11210): CALL: hideWindow
D/MainActivity(11210): Broadcast sent: com.example.ai_keyboard.DICTIONARY_CHANGED
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=ImeCallback=ImeOnBackInvokedCallback@136143823 Callback=android.window.ImeOnBackInvokedDispatcher$ImeOnBackInvokedCallbackWrapper@6e93dcb
D/InsetsController(11210): hide(ime(), fromIme=true)
D/AIKeyboardService(11210): DICTIONARY_CHANGED broadcast received!
D/ViewRootImplStubImpl(11210): requestedTypes: 8
D/AIKeyboardService(11210): Reloading dictionary settings from broadcast...
D/AIKeyboardService(11210): Dictionary settings reloaded: enabled=true
D/DictionaryManager(11210): Initializing DictionaryManager
D/DictionaryManager(11210): Loaded 3 dictionary entries from preferences
D/DictionaryManager(11210): Rebuilt shortcut map with 4 entries
D/DictionaryManager(11210): Loaded 4 entries from Flutter prefs (skipped: 0)
D/DictionaryManager(11210): Synced 4 entries to Flutter SharedPreferences
D/AIKeyboardService(11210): Dictionary updated with 4 entries
D/DictionaryManager(11210): Rebuilt shortcut map with 4 entries
D/DictionaryManager(11210): DictionaryManager initialized with 4 entries (enabled: true)
D/AIKeyboardService(11210): Dictionary settings reloaded successfully!
.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940384687, downTime=940384064, phoneEventTime=00:43:01.458 } moveCount:84
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940385574, downTime=940385574, phoneEventTime=00:43:02.345 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940385647, downTime=940385574, phoneEventTime=00:43:02.418 } moveCount:0
I/flutter (11210): Clipboard settings broadcast sent
D/MainActivity(11210): sendBroadcast called with action: com.example.ai_keyboard.CLIPBOARD_CHANGED
D/MainActivity(11210): Broadcast sent: com.example.ai_keyboard.CLIPBOARD_CHANGED
D/AIKeyboardService(11210): CLIPBOARD_CHANGED broadcast received!
D/AIKeyboardService(11210): Reloading clipboard settings from broadcast...
D/ClipboardHistoryManager(11210): No clipboard items in Flutter prefs
D/ClipboardHistoryManager(11210): Updated settings: maxSize=20, autoExpiry=true, expiryMinutes=60
D/AIKeyboardService(11210): Clipboard settings reloaded: enabled=true, maxSize=20, autoExpiry=true
D/AIKeyboardService(11210): Clipboard settings reloaded successfully!
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940391064, downTime=940391064, phoneEventTime=00:43:07.835 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940391182, downTime=940391064, phoneEventTime=00:43:07.953 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940392893, downTime=940392893, phoneEventTime=00:43:09.664 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940392998, downTime=940392893, phoneEventTime=00:43:09.769 } moveCount:0
I/flutter (11210): Clipboard settings broadcast sent
D/MainActivity(11210): sendBroadcast called with action: com.example.ai_keyboard.CLIPBOARD_CHANGED
D/MainActivity(11210): Broadcast sent: com.example.ai_keyboard.CLIPBOARD_CHANGED
D/AIKeyboardService(11210): CLIPBOARD_CHANGED broadcast received!
D/AIKeyboardService(11210): Reloading clipboard settings from broadcast...
D/ClipboardHistoryManager(11210): No clipboard items in Flutter prefs
D/ClipboardHistoryManager(11210): Updated settings: maxSize=20, autoExpiry=true, expiryMinutes=60
D/AIKeyboardService(11210): Clipboard settings reloaded: enabled=true, maxSize=20, autoExpiry=true
D/AIKeyboardService(11210): Clipboard settings reloaded successfully!
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=io.flutter.embedding.android.FlutterActivity$1@5c66ad
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940400534, downTime=940400534, phoneEventTime=00:43:17.305 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940400898, downTime=940400534, phoneEventTime=00:43:17.669 } moveCount:68
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940401267, downTime=940401267, phoneEventTime=00:43:18.038 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940401336, downTime=940401267, phoneEventTime=00:43:18.107 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940402322, downTime=940402322, phoneEventTime=00:43:19.093 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940403347, downTime=940402322, phoneEventTime=00:43:20.118 } moveCount:138
I/ScrollIdentify(11210): on fling
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940404238, downTime=940404238, phoneEventTime=00:43:21.009 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'com.example.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940404320, downTime=940404238, phoneEventTime=00:43:21.091 } moveCount:0
D/MainActivity(11210): Setting multilingual mode: true
D/MainActivity(11210): ✅ Multilingual mode set to: true
D/AIKeyboardService(11210): LANGUAGE_CHANGED broadcast received! Language: null, Multi: true
D/AIKeyboardService(11210): ✅ Language prefs loaded: enabled=[en], current=en (idx=0), multi=true
D/AIKeyboardService(11210): ✅ Feature flags: translit=true, reverse=false
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=com.example.ai_keyboard.SwipeKeyboardView{8efdb33 V.ED..... ......ID 0,0-1220,774 #7f0800f7 app:id/keyboard_view}
E/ple.ai_keyboard(11210): Invalid resource ID 0x00000000.
E/ple.ai_keyboard(11210): Invalid resource ID 0x00000000.
E/ple.ai_keyboard(11210): Invalid resource ID 0x00000000.
D/AIKeyboardService(11210): Applied theme: Red (theme_red)
D/AIKeyboardService(11210): Theme debug info: V2 theme applied successfully
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): ✅ Keyboard layout reloaded after language change
D/AIKeyboardService(11210): ✅ Language settings reloaded! Current: en, Enabled: [en]
mple.ai_keyboard/com.example.ai_keyboard.MainActivity', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940420337, downTime=940420284, phoneEventTime=00:43:37.108 } moveCount:0
D/MainActivity(11210): Setting enabled languages: [en, hi], current: en
D/MainActivity(11210): ✅ Enabled languages saved: [en, hi]
D/AIKeyboardService(11210): LANGUAGE_CHANGED broadcast received! Language: en, Multi: false
D/MainActivity(11210): ✅ Language change broadcast sent: en
D/AIKeyboardService(11210): ✅ Language prefs loaded: enabled=[en, hi], current=en (idx=0), multi=true
D/AIKeyboardService(11210): ✅ Feature flags: translit=true, reverse=false
D/AIKeyboardService(11210): 🔄 Switching from LETTERS to LETTERS
D/AIKeyboardService(11210): ✅ Rebound OnKeyboardActionListener after layout change. View=com.example.ai_keyboard.SwipeKeyboardView{8efdb33 V.ED..... ......ID 0,0-1220,774 #7f0800f7 app:id/keyboard_view}
E/ple.ai_keyboard(11210): Invalid resource ID 0x00000000.
E/ple.ai_keyboard(11210): Invalid resource ID 0x00000000.
E/ple.ai_keyboard(11210): Invalid resource ID 0x00000000.
D/AIKeyboardService(11210): Applied theme: Red (theme_red)
D/AIKeyboardService(11210): Theme debug info: V2 theme applied successfully
D/AIKeyboardService(11210): ✅ Switched to LETTERS
D/AIKeyboardService(11210): ✅ Keyboard layout reloaded after language change
D/AIKeyboardService(11210): ✅ Language settings reloaded! Current: en, Enabled: [en, hi]
W/WindowOnBackDispatcher(11210): sendCancelIfRunning: isInProgress=false callback=io.flutter.embedding.android.FlutterActivity$1@5c66ad

D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940436154, downTime=940436154, phoneEventTime=00:43:52.925 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940436233, downTime=940436154, phoneEventTime=00:43:53.004 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'love'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'love'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'love', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='love', prevToken='I', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940436494, downTime=940436494, phoneEventTime=00:43:53.265 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940436563, downTime=940436494, phoneEventTime=00:43:53.334 } moveCount:0
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
D/AIKeyboardService(11210): Getting AI suggestions for word: '', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='', prevToken='I', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940437214, downTime=940437214, phoneEventTime=00:43:53.985 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940437304, downTime=940437214, phoneEventTime=00:43:54.075 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'y'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'y'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'y', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='y', prevToken='love', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940437499, downTime=940437499, phoneEventTime=00:43:54.270 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940437600, downTime=940437499, phoneEventTime=00:43:54.371 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'yo'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'yo'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'yo', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='yo', prevToken='love', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940437725, downTime=940437725, phoneEventTime=00:43:54.496 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940437778, downTime=940437725, phoneEventTime=00:43:54.549 } moveCount:0
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'yo'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'yo', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='yo', prevToken='love', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940438052, downTime=940438052, phoneEventTime=00:43:54.823 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940438122, downTime=940438052, phoneEventTime=00:43:54.893 } moveCount:0
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
D/AIKeyboardService(11210): Getting AI suggestions for word: '', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='', prevToken='love', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: ❤️, 💕
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: [❤️, 💕]
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Set suggestion 0: ❤️
D/AIKeyboardService(11210): Set suggestion 1: 💕
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940441185, downTime=940441185, phoneEventTime=00:43:57.956 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940441244, downTime=940441185, phoneEventTime=00:43:58.015 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'd'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'd'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'd', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='d', prevToken='yo7', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940441689, downTime=940441689, phoneEventTime=00:43:58.460 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940441752, downTime=940441689, phoneEventTime=00:43:58.523 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'do'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'do'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'do', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='do', prevToken='yo7', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940441958, downTime=940441958, phoneEventTime=00:43:58.729 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940442013, downTime=940441958, phoneEventTime=00:43:58.784 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'don'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'don'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'don', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='don', prevToken='yo7', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940442213, downTime=940442213, phoneEventTime=00:43:58.984 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940442277, downTime=940442213, phoneEventTime=00:43:59.048 } moveCount:0
D/AIKeyboardService(11210): Updated currentWord: 'donr'
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: 'donr'
D/AIKeyboardService(11210): Getting AI suggestions for word: 'donr', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='donr', prevToken='yo7', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_DOWN, id[0]=0, pointerCount=1, eventTime=940442660, downTime=940442660, phoneEventTime=00:43:59.431 } moveCount:0
I/MIUIInput(11210): [MotionEvent] ViewRootImpl windowName 'InputMethod', { action=ACTION_UP, id[0]=0, pointerCount=1, eventTime=940442710, downTime=940442660, phoneEventTime=00:43:59.481 } moveCount:0
D/AIKeyboardService(11210): updateAISuggestions called - aiSuggestionsEnabled: true, isAIReady: true, currentWord: ''
D/AIKeyboardService(11210): Getting AI suggestions for word: '', context: []
D/AIKeyboardService(11210): Enhanced suggestions: currentWord='', prevToken='yo7', lang='en'
D/AIKeyboardService(11210): Enhanced prediction results: 
W/AIKeyboardService(11210): AI suggestion error: AI services not initialized
D/AIKeyboardService(11210): updateSuggestionUI called with suggestions: []
D/AIKeyboardService(11210): suggestionContainer is null: false
D/AIKeyboardService(11210): Container child count: 3
D/AIKeyboardService(11210): Suggestion container made visible