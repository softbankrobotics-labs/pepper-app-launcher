package com.softbankrobotics.pepperapplauncher;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayPosition;
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy;
import com.aldebaran.qi.sdk.object.conversation.QiChatExecutor;
import com.softbankrobotics.pepperapplauncher.Utils.ChatData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    private final String TAG = "MSI_PepperAppLauncher";
    private LinearLayout verticalContainer, currentHorizontalContainer;
    private PackageManager pm;
    private Resources resources;
    private Configuration config;

    private final List<String> apkNames = new ArrayList<>();
    private Intent overlayPopupService,overlayBtnCloseService;
    private boolean settingsResult,settingsAccess=false;
    private String packageName;

    private final List<String> escape=Arrays.asList("\\(","\\)","\\[","\\]","\\{","\\}","~","$","%","^",":");

    private Future<Void> chatFuture;
    private final List<String> locales= Arrays.asList("fr","en"); //ADD YOUR LANGUAGE HERE
    private final HashMap<String,String> contentLocales=new HashMap<>();
    private final HashMap<String,ChatData> chatDataLocales=new HashMap<>();
    private String currentLanguage="";
    public Map<String, String> appLabelVsPackageName = new HashMap<>();

    //Access Manager related variables
    private final String ACCESS_MANAGER_PACKAGE = "com.softbankrobotics.accessmanager";
    private final String AUTHENTICATION_ACTIVITY_PACKAGE = "com.softbankrobotics.accessmanager.ui.authentication.AuthenticationActivity";
    private final int AUTHENTICATION_CODE = 47;

    private QiContext qiContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        QiSDK.register(this, this);

        pm = getPackageManager();
        overlayPopupService = new Intent(this, OverlayPopupService.class);
        overlayBtnCloseService = new Intent(this, OverlayBtnCloseService.class);

        getDrawOverlaysPermission();

        resources = getResources();
        config = resources.getConfiguration();

        getPepperAppList();

        findViewById(R.id.settings).setOnClickListener((v) -> startApplication("com.android.settings"));
        verticalContainer = findViewById(R.id.vertical_container);


    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,"onPause");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isMyServiceRunning(OverlayPopupService.class)) {
            overlayPopupService = new Intent(this, OverlayPopupService.class);
            stopService(overlayPopupService);
        }
        if (isMyServiceRunning(OverlayBtnCloseService.class)) {
            overlayBtnCloseService = new Intent(this, OverlayBtnCloseService.class);
            stopService(overlayBtnCloseService);
        }
        QiSDK.unregister(this,this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: started");
        if (!settingsResult) {
            if (isMyServiceRunning(OverlayPopupService.class)) {
                overlayPopupService = new Intent(this, OverlayPopupService.class);
                stopService(overlayPopupService);
            }
            if (isMyServiceRunning(OverlayBtnCloseService.class)) {
                overlayBtnCloseService = new Intent(this, OverlayBtnCloseService.class);
                stopService(overlayBtnCloseService);
            }
        }
        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.IMMERSIVE);
        setSpeechBarDisplayPosition(SpeechBarDisplayPosition.BOTTOM);
        settingsResult = false;

        Log.d(TAG, "onResume: finished");
    }

    private void getDrawOverlaysPermission() {
        Handler handler = new Handler();
        Runnable checkOverlaySetting = new Runnable() {
            @Override
            public void run() {
                if (Settings.canDrawOverlays(MainActivity.this)) {
                    finishActivity(0);
                }
                handler.postDelayed(this, 1000);
            }
        };

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
            handler.postDelayed(checkOverlaySetting, 1000);
        }
    }

    /**
     * Update the container with languages. Update the flag.
     * Set clickable the button to switch language
     */
    private void configureButtonLanguage() {
        runOnUiThread(() -> {
            ImageButton buttonLanguage = findViewById(R.id.button_language);
            ImageButton buttonLang1 = findViewById(R.id.button_lang_1);
            View languageContainer = findViewById(R.id.view_lang_container);
            languageContainer.setVisibility(View.INVISIBLE);
            languageContainer.setClickable(false);

            buttonLanguage.setOnClickListener(v -> {
                if (languageContainer.isClickable()) {
                    languageContainer.setVisibility(View.INVISIBLE);
                    languageContainer.setClickable(false);
                } else {
                    languageContainer.setVisibility(View.VISIBLE);
                    languageContainer.setClickable(true);
                }
            });

            if (currentLanguage.toLowerCase().equals("fr")) {
                buttonLanguage.setImageResource(R.drawable.lang_fr);
                buttonLang1.setImageResource(R.drawable.lang_uk);
                buttonLang1.setOnClickListener(v -> {
                    languageContainer.setVisibility(View.INVISIBLE);
                    setLocale("en");
                });
            } else {
                buttonLanguage.setImageResource(R.drawable.lang_uk);
                buttonLang1.setImageResource(R.drawable.lang_fr);
                buttonLang1.setOnClickListener(v -> {
                    languageContainer.setVisibility(View.INVISIBLE);
                    setLocale("fr");
                });
            }
        });
    }

    /**
     * Change the current language of the application. Modify the layout with this language.
     * Run the new chatData
     * @param lang the code of the given language
     */
    public void setLocale(String lang) {
        currentLanguage=lang;

        if(!lang.equals(config.locale.toString())){
            config.locale = new Locale(lang);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            resources = new Resources(getAssets(), metrics, config);
        }

        //Run the chat with the new locale
        runChat();

        Log.i(TAG, "setting Locale to : " + lang);

        //Configure the layout with the new locale
        configureButtonLanguage();
        createAppButton();
    }

    /**
     * Iterate through all applications to build the layout, checking if the app is installed
     */
    private void createAppButton() {
        //Clear the previous layout
        verticalContainer.removeAllViews();

        int nbButtons=0;
        //creating buttons
        for (String pckg : apkNames) {
            if (isPackageInstalled(pckg, pm)) {
                try {
                    buildImageButton(pckg,nbButtons);
                    nbButtons=nbButtons+1;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "could not find package : " + pckg);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Getting all sbr apps
     */
    private void getPepperAppList() {

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA | PackageManager.GET_SHARED_LIBRARY_FILES);
        for (ApplicationInfo app : apps) {
            try {
                ZipFile apk = new ZipFile(app.publicSourceDir);
                ZipEntry manifest = apk.getEntry("AndroidManifest.xml");
                if (manifest != null) {
                    InputStream stream = apk.getInputStream(manifest);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_16LE));
                    while (reader.ready()) {
                        String line = reader.readLine();
                        if (line.contains("com.softbank.hardware.pepper")) {
                            apkNames.add(app.packageName);
                            Log.d(TAG, "onCreate: app to display : " + app.packageName);
                        }
                    }
                    reader.close();
                    stream.close();
                }
                apk.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //any app that should not show up in the launcher
        apkNames.remove("com.softbankrobotics.pepperapplauncher");
        //apkNames.remove("com.softbankrobotics.kioskclient");

        /*
        ADD YOUR APPLICATIONS HERE USING THIS SYNTAX
        apkNames.add("packageName")
         */
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        boolean found = true;
        try {
            packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            found = false;
        }
        return found;
    }

    /**
     * Set the layout in a given language with every application
     * @param packageName package name of the application (the label will be used)
     * @param nbButtons iteration through applications to build various horizontal containers
     * @throws PackageManager.NameNotFoundException if the package does not exist
     */
    private void buildImageButton(String packageName, int nbButtons) throws PackageManager.NameNotFoundException {
        if (nbButtons % 3 == 0) {
            buildHorizontalContainer();
        }
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(380, 230);
        params.setMargins(10, 10, 10, 10);
        button.setLayoutParams(params);
        button.setPadding(20, 0, 20, 0);
        button.setBackground(ContextCompat.getDrawable(this,R.drawable.ui_white_r30));
        ApplicationInfo app = pm.getApplicationInfo(packageName, 0);
        Resources resources = pm.getResourcesForApplication(app);
        Drawable appIcon = resources.getDrawableForDensity(app.icon, DisplayMetrics.DENSITY_XXXHIGH, getTheme());
        Log.d(TAG, "buildImageButton: appName : "+packageName);

        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(appIcon.getIntrinsicWidth(), appIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);
            appIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            appIcon.draw(canvas);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }
        appIcon = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 180, 180, true));

        //Get app label in the correct locale
        final Resources appRes = pm.getResourcesForApplication(packageName);
        appRes.updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        final String appName = appRes.getString(app.labelRes);

        button.setCompoundDrawablesWithIntrinsicBounds(null, appIcon, null, null);
        Log.i(TAG,appName+" "+config.locale.toLanguageTag());
        button.setText(appName);
        button.setTextSize(25);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setOnClickListener((v) -> startApplication(packageName));
        runOnUiThread(() -> currentHorizontalContainer.addView(button));
    }

    private void buildHorizontalContainer() {
        LinearLayout horizontalContainer = new LinearLayout(this);
        horizontalContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        currentHorizontalContainer = horizontalContainer;
        runOnUiThread(() -> verticalContainer.addView(currentHorizontalContainer));
    }

    /**
     * Start the given application.
     * If the application is Settings, launch the AccessManager Authentication
     * @param packageName the package of the application
     */
    public void startApplication(String packageName) {
        this.packageName=packageName;
        //If settings, go to access manager. Launch the app otherwise
        if(packageName.equals("com.android.settings")){
            if (isAccessManagerInstalled(getApplicationContext())) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(ACCESS_MANAGER_PACKAGE, AUTHENTICATION_ACTIVITY_PACKAGE));
                startActivityForResult(intent, AUTHENTICATION_CODE);
            } else {
                Toast.makeText(this,"Access Manager is not installed",LENGTH_SHORT).show();
            }
        } else {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                overlayPopupService = new Intent(this, OverlayPopupService.class);
                overlayPopupService.putExtra("locale",currentLanguage);
                if(Settings.canDrawOverlays(MainActivity.this)){
                    startActivity(launchIntent);//null pointer check in case package name was not found
                    startService(overlayPopupService);
                } else {
                    getDrawOverlaysPermission();
                }
            }
        }
    }

    /**
     * If the right pin code is provided, launch settings
     * Go back to app launcher Otherwise
     * @param requestCode code depending on the intent
     * @param resultCode result from the intent. Handle every cases
     * @param data null
     */
    public void onActivityResult(int requestCode, int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Resources resources=newConfig(currentLanguage);
        if (requestCode == AUTHENTICATION_CODE) {
            //Right pin code, access granted
            int RESULT_CODE_RECOVERY_OK = 55;
            if(resultCode==RESULT_OK){
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    if(Settings.canDrawOverlays(MainActivity.this)){
                        settingsAccess=true;
                        settingsResult=true;
                        startActivity(launchIntent);
                        startService(overlayBtnCloseService);
                    } else getDrawOverlaysPermission();
                }
            //If wrong pin code, or cancelled
            } else if(resultCode==RESULT_FIRST_USER || resultCode== RESULT_CODE_RECOVERY_OK){
                Toast.makeText(this,resources.getString(R.string.retry),LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,resources.getString(R.string.denied),LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this,resources.getString(R.string.denied),LENGTH_SHORT).show();
        }
    }

    /**
     * Check if Access Manager is installed
     */
    public boolean isAccessManagerInstalled(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(ACCESS_MANAGER_PACKAGE, AUTHENTICATION_ACTIVITY_PACKAGE));
        @SuppressLint({"WrongConstant", "QueryPermissionsNeeded"})
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        return !list.isEmpty();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     *Build sayable buttons with topics and then set the layout and run chatData the first time.
     * Only set the layout and run chatData otherwise
     * @param qiContext the qiContext of this thread
     */
    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Log.d(TAG, "onRobotFocusGained: started");

        this.qiContext=qiContext;

        Log.i(TAG, String.valueOf(settingsResult));
        //If this is not the first focus gained
        if(!currentLanguage.equals("")){
            //If focus is gained after ActivityOnResult CODE OK, do not chat
            if(!settingsAccess){
                initChat();
                runOnUiThread(() ->  setLocale(currentLanguage));
            }
        } else {
            //Store locale (the locale will change during initContent)
            Locale tempLoc=config.locale;
            //Create content of topics for buttons
            initContent();
            //Create chatData with content
            initChat();
            //If the config is not supported by the app, set english as default language
            if(locales.contains(tempLoc.getLanguage())) config.locale=tempLoc;
            else config.locale=new Locale("en");
            runOnUiThread(() -> setLocale(config.locale.getLanguage()));
        }
        Log.d(TAG, "onRobotFocusGained: finished");
    }

    @Override
    public void onRobotFocusLost() {
        Log.i(TAG, "onRobotFocusLost: ");
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        Log.i(TAG,reason);
    }

    /**
     * Give the possibility to the user to open applications using his voice, no mater the language
     */
    private void initContent() {

        for(String locale : locales){

            //Set the locale for resources
            Resources resources=newConfig(locale);

            //Start Building the topic
            StringBuilder topicContent= new StringBuilder(resources.getString(R.string.topic));
            topicContent.append(resources.getString(R.string.concept));
            topicContent.append(resources.getString(R.string.hello));
            topicContent.append(resources.getString(R.string.apps));

            //Add every apk to the topic with the executor
            for (String packageName : apkNames) {
                if (isPackageInstalled(packageName, pm)) {
                    try {

                        //Get the application's label in the given language
                        ApplicationInfo app = pm.getApplicationInfo(packageName, 0);
                        final Resources locRes = pm.getResourcesForApplication(packageName);
                        Configuration configuration=locRes.getConfiguration();
                        locRes.updateConfiguration(configuration, getBaseContext().getResources().getDisplayMetrics());
                        String localizedLabel= locRes.getString(app.labelRes);

                        for(String character:escape) localizedLabel=localizedLabel.replaceAll(character,"");
                        appLabelVsPackageName.put(localizedLabel,packageName);
                        Log.i(TAG,"Adding "+localizedLabel);

                        //Set concept content with label
                        topicContent.append("\"").append(localizedLabel).append("\" ");
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "could not find package : " + packageName);
                        e.printStackTrace();
                    }
                }
            }
            //Close the apps concept
            topicContent.append("]\n");
            topicContent.append(resources.getString(R.string.start));
            contentLocales.put(locale,topicContent.toString());
        }

    }

    /**
     * Create a chatData based on the content of the topic, the locale.
     * Add the executor to start the application.
     */
    private void initChat(){

        Map<String, QiChatExecutor> executors = new HashMap<>();
        // Map the executor name from the topic to our qiChatExecutor
        executors.put("myExecutor", new MyQiChatExecutor(qiContext,this));

        //Build chatData for every locale
        for(String locale : locales){
            String content=contentLocales.get(locale);
            ChatData chatLocale=new ChatData(this,qiContext,new Locale(locale), Collections.singletonList(content),true);
            chatDataLocales.put(locale,chatLocale);
            if(chatLocale.languageIsInstalled){
                chatLocale.chat.addOnStartedListener(() -> chatLocale.goToBookmarkNewTopic("hi","topic"));
                chatLocale.setUpChatContent(executors,null,null);
            }
        }

    }

    /**
     * Run the chatData in the selected language
     */
    private void runChat(){
        Log.i(TAG,"runChat");
        ChatData currentChatData=chatDataLocales.get(currentLanguage);
        if(currentChatData!=null && currentChatData.languageIsInstalled) {
            if (chatFuture != null) {
                chatFuture.thenConsume(value -> chatFuture = currentChatData.chat.async().run());
                chatFuture.requestCancellation();
            } else{
                chatFuture = currentChatData.chat.async().run();

            }
        }
    }

    /**
     * Set a new config with a given locale
     * @param locale code of the locale used for the config
     * @return resources with the correct locale
     */
    private Resources newConfig(String locale){
        Configuration conf = getResources().getConfiguration();
        conf.locale = new Locale(locale);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return new Resources(getAssets(), metrics, conf);
    }
}
