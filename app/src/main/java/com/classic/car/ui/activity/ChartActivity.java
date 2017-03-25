package com.classic.car.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.FrameLayout;

import com.classic.android.base.BaseActivity;
import com.classic.car.R;
import com.classic.car.consts.Consts;
import com.classic.car.entity.ChartType;
import com.classic.car.utils.ChartUtil;
import com.classic.car.utils.RxUtil;
import com.classic.car.utils.ToastUtil;
import com.classic.car.utils.Util;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * 应用名称: CarAssistant
 * 包 名 称: com.classic.car.ui.activity
 *
 * 文件描述: TODO
 * 创 建 人: 续写经典
 * 创建时间: 2017/3/25 10:53
 */
public class ChartActivity extends BaseActivity {
    private static final String PARAMS_CHART_TYPE = "chartType";
    private static final String PARAMS_START_TIME = "startTime";
    private static final String PARAMS_END_TIME   = "endTime";

    private Context mAppContext;
    private Integer mDataType;
    private int     mChartType;
    private long    mStartTime;
    private long    mEndTime;
    private Chart   mChart;
    private boolean isAsc;

    private CompositeSubscription mCompositeSubscription;

    @BindView(R.id.chart_layout) FrameLayout mChartLayout;

    public static void start(@NonNull Activity activity, @ChartType int chartType, long startTime,
                             long endTime) {
        Intent intent = new Intent(activity, ChartActivity.class);
        intent.putExtra(PARAMS_CHART_TYPE, chartType);
        intent.putExtra(PARAMS_START_TIME, startTime);
        intent.putExtra(PARAMS_END_TIME, endTime);
        activity.startActivity(intent);
    }

    @Override public int getLayoutResId() {
        return R.layout.activity_chart;
    }

    @Override public void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        ButterKnife.bind(this);
        mAppContext = getApplicationContext();
        mCompositeSubscription = new CompositeSubscription();
        getParams();
        createChart(mChartType);
    }

    @Override protected void onStop() {
        super.onStop();
        if (null != mCompositeSubscription) {
            mCompositeSubscription.clear();
        }
    }

    private void getParams() {
        if (!getIntent().hasExtra(PARAMS_CHART_TYPE)) {
            finish();
            return;
        }
        mChartType = getIntent().getIntExtra(PARAMS_CHART_TYPE, ChartType.BAR_CHART);
        mStartTime = getIntent().getLongExtra(PARAMS_START_TIME, 0);
        mEndTime = getIntent().getLongExtra(PARAMS_END_TIME, 0);
        if (mChartType == ChartType.LINE_CHART) {
            mDataType = Consts.TYPE_FUEL;
            isAsc = true;
        }
    }

    @SuppressWarnings("unused") @OnClick(R.id.chart_back) void onBack(View view) {
        finish();
    }

    @SuppressWarnings("unused") @OnClick(R.id.chart_download) void download(View view) {
        if (null == mChart) { return; }
        mCompositeSubscription.add(Observable.unsafeCreate(new Observable.OnSubscribe<Boolean>() {
                                       @Override public void call(Subscriber<? super Boolean> subscriber) {
                                           if (!subscriber.isUnsubscribed()) {
                                               subscriber.onNext(mChart.saveToGallery(Util.createImageName(), 100));
                                               subscriber.onCompleted();
                                           }
                                       }
                                   })
                                   .compose(RxUtil.<Boolean>applySchedulers(RxUtil.IO_ON_UI_TRANSFORMER))
                                   .subscribe(new Action1<Boolean>() {
                                       @Override public void call(Boolean result) {
                                           ToastUtil.showToast(mAppContext, result ?
                                                   R.string.chart_save_success : R.string.chart_save_fail);
                                       }
                                   }, new Action1<Throwable>() {
                                       @Override public void call(Throwable throwable) {
                                           ToastUtil.showToast(mAppContext, R.string.chart_save_fail);
                                       }
                                   }));
    }

    private void createChart(int chartType) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                                                       FrameLayout.LayoutParams.MATCH_PARENT);
        switch (chartType) {
            case ChartType.BAR_CHART:
                mChart = new BarChart(mChartLayout.getContext());
                mChart.setLayoutParams(params);
                ChartUtil.initBarChart(mAppContext, (BarChart)mChart);
                break;
            case ChartType.PIE_CHART:
                mChart = new PieChart(mChartLayout.getContext());
                mChart.setLayoutParams(params);
                ChartUtil.initPieChart(mAppContext, (PieChart)mChart);
                break;
            case ChartType.LINE_CHART:
                mChart = new LineChart(mChartLayout.getContext());
                mChart.setLayoutParams(params);
                ChartUtil.initLineChart(mAppContext, (LineChart)mChart);
                break;
        }
        if (null != mChart) {
            mChartLayout.addView(mChart, 0);
        }
    }
}
