package com.classic.car.ui.activity;

import android.Manifest;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.IdRes;
import android.view.KeyEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.classic.android.BasicProject;
import com.classic.android.permissions.AfterPermissionGranted;
import com.classic.android.permissions.AppSettingsDialog;
import com.classic.android.permissions.EasyPermissions;
import com.classic.android.utils.DoubleClickExitHelper;
import com.classic.car.BuildConfig;
import com.classic.car.R;
import com.classic.car.consts.Consts;
import com.classic.car.ui.base.AppBaseActivity;
import com.classic.car.ui.fragment.AboutFragment;
import com.classic.car.ui.fragment.ChartFragment;
import com.classic.car.ui.fragment.MainFragment;
import com.classic.car.ui.fragment.TimelineFragment;
import com.classic.car.utils.PgyUtil;
import com.elvishew.xlog.LogLevel;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import java.util.List;

import butterknife.BindView;

public class MainActivity extends AppBaseActivity {
    @BindView(R.id.main_bottombar) BottomBar mBottomBar;

    private MainFragment     mMainFragment;
    private ChartFragment    mChartFragment;
    private TimelineFragment mTimelineFragment;
    private AboutFragment    mAboutFragment;

    private DoubleClickExitHelper mDoubleClickExitHelper;

    @Override public int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override public void initView(Bundle savedInstanceState) {
        if(BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }
        super.initView(savedInstanceState);
        setTitle(R.string.app_name);
        mDoubleClickExitHelper = new DoubleClickExitHelper(mActivity);

        checkStoragePermissions();
        initBottomBar();
        PgyUtil.register(mAppContext);
        PgyUtil.checkUpdate(mActivity, false);
    }

    private static final int REQUEST_CODE_STORAGE  = 101;
    private static final int REQUEST_CODE_SETTINGS = 102;
    private static final String[] STORAGE_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @AfterPermissionGranted(REQUEST_CODE_STORAGE)
    private void checkStoragePermissions(){
        if (EasyPermissions.hasPermissions(this, STORAGE_PERMISSIONS)) {
            init();
        } else {
            EasyPermissions.requestPermissions(this, Consts.STORAGE_PERMISSIONS_DESCRIBE,
                                               REQUEST_CODE_STORAGE, STORAGE_PERMISSIONS);
        }
    }

    @Override public void onPermissionsGranted(int requestCode, List<String> perms) {
        super.onPermissionsGranted(requestCode, perms);
        if(requestCode == REQUEST_CODE_STORAGE){
            init();
        }
    }

    @Override public void onPermissionsDenied(int requestCode, List<String> perms) {
        super.onPermissionsDenied(requestCode, perms);
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, Consts.STORAGE_PERMISSIONS_DESCRIBE)
                    .setTitle("权限申请")
                    .setPositiveButton("设置")
                    .setNegativeButton("取消", null)
                    .setRequestCode(REQUEST_CODE_SETTINGS)
                    .build()
                    .show();
        }
    }

    private void init() {
        BasicProject.config(new BasicProject.Builder().setDebug(BuildConfig.DEBUG)
                                                      .setRootDirectoryName(Consts.DIR_NAME)
                                                      .setExceptionHandler(mAppContext)
                                                      .setLog(BuildConfig.DEBUG
                                                              ? LogLevel.ALL
                                                              : LogLevel.NONE));
    }

    private void initBottomBar(){
        mBottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.tab_main:
                        if (null == mMainFragment) {
                            mMainFragment = MainFragment.newInstance();
                        }
                        changeFragment(R.id.main_fragment_layout, mMainFragment);
                        break;
                    case R.id.tab_chart:
                        if (null == mChartFragment) {
                            mChartFragment = ChartFragment.newInstance();
                        }
                        changeFragment(R.id.main_fragment_layout, mChartFragment);
                        break;
                    case R.id.tab_timeline:
                        if (null == mTimelineFragment) {
                            mTimelineFragment = TimelineFragment.newInstance();
                        }
                        changeFragment(R.id.main_fragment_layout, mTimelineFragment);
                        break;
                    case R.id.tab_about:
                        if (null == mAboutFragment) {
                            mAboutFragment = AboutFragment.newInstance();
                        }
                        changeFragment(R.id.main_fragment_layout, mAboutFragment);
                        break;
                }
            }
        });
    }

    @Override public void unRegister() {
        super.unRegister();
        PgyUtil.destroy();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mDoubleClickExitHelper.onKeyDown(keyCode, event);
    }

    public void onHide() {
        getToolbar().animate().translationY(-getToolbar().getHeight()).setInterpolator(new AccelerateInterpolator(2));
        mBottomBar.animate().translationY(mBottomBar.getHeight()).setInterpolator(new AccelerateInterpolator(2));
    }

    public void onShow() {
        getToolbar().animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
        mBottomBar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
    }
}
