package com.classic.car.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.classic.car.R;
import com.classic.car.app.CarApplication;
import com.classic.car.consts.Consts;
import com.classic.car.db.dao.ConsumerDao;
import com.classic.car.entity.ChartType;
import com.classic.car.entity.ConsumerDetail;
import com.classic.car.ui.activity.ChartActivity;
import com.classic.car.ui.activity.MainActivity;
import com.classic.car.ui.base.AppBaseFragment;
import com.classic.car.ui.chart.BarChartDisplayImpl;
import com.classic.car.ui.chart.IChartDisplay;
import com.classic.car.ui.chart.LineChartDisplayImpl;
import com.classic.car.ui.chart.PieChartDisplayImpl;
import com.classic.car.ui.widget.RelativePopupWindow;
import com.classic.car.ui.widget.YearsPopup;
import com.classic.car.utils.DataUtil;
import com.classic.car.utils.DateUtil;
import com.classic.car.utils.RxUtil;
import com.classic.car.utils.ToastUtil;
import com.classic.car.utils.Util;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.jakewharton.rxbinding.view.RxView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * 应用名称: CarAssistant
 * 包 名 称: com.classic.car.ui.fragment
 *
 * 文件描述：图表统计页面
 * 创 建 人：续写经典
 * 创建时间：16/5/29 下午2:21
 */
@SuppressWarnings("unchecked") public class ChartFragment extends AppBaseFragment {
    private static final int ANIMATE_DURATION = 400;

    @BindView(R.id.chart_fuel_linechart)      LineChart    mFuelLineChart;
    @BindView(R.id.chart_consumer_barchart)   BarChart     mConsumerBarChart;
    @BindView(R.id.chart_percentage_piechart) PieChart     mPercentagePieChart;
    @BindView(R.id.chart_min_money)           TextView     mMinMoney;
    @BindView(R.id.chart_max_money)           TextView     mMaxMoney;
    @BindView(R.id.chart_min_oilmess)         TextView     mMinOilMess;
    @BindView(R.id.chart_max_oilmess)         TextView     mMaxOilMess;
    @BindView(R.id.chart_consumer_save)       TextView     mSaveConsumer;
    @BindView(R.id.chart_fuel_save)           TextView     mSaveFuel;
    @BindView(R.id.chart_percentage_save)     TextView     mSavePercentage;
    @BindView(R.id.chart_percentage_detail)   LinearLayout mPercentageDetail;
    @Inject                                   ConsumerDao  mConsumerDao;

    private LayoutInflater mLayoutInflater;
    private IChartDisplay  mBarChartDisplay;
    private IChartDisplay  mPieChartDisplay;
    private IChartDisplay  mLineChartDisplay;

    private long mStartTime;
    private long mEndTime;

    public static ChartFragment newInstance() {
        return new ChartFragment();
    }

    @Override public int getLayoutResId() {
        return R.layout.fragment_chart;
    }

    @Override public void initView(View parentView, Bundle savedInstanceState) {
        ((CarApplication) mActivity.getApplicationContext()).getAppComponent().inject(this);
        super.initView(parentView, savedInstanceState);
        setHasOptionsMenu(true);
        mCurrentYear = Calendar.getInstance().get(Calendar.YEAR);
        initChart();
        loadData(mCurrentYear);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.chart_menu, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_date) {
            showYears();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private YearsPopup mYearsPopup;
    private int mCurrentYear;
    private void showYears() {
        if (null == mYearsPopup) {
            mYearsPopup = new YearsPopup.Builder()
                    .context(mActivity)
                    .years(Consts.YEARS)
                    .fitInScreen(true)
                    .horizontalPosition(RelativePopupWindow.HorizontalPosition.RIGHT)
                    .verticalPosition(RelativePopupWindow.VerticalPosition.BELOW)
                    .listener(new YearsPopup.Listener() {
                        @Override public void onYearSelected(int year) {
                            ToastUtil.showToast(mAppContext, String.valueOf(year));
                            mCurrentYear = year;
                            loadData(mCurrentYear);
                        }
                    })
                    .build();
        }
        if (mYearsPopup.isShowing()) {
            mYearsPopup.dismiss();
        } else {
            mYearsPopup.show(((MainActivity)mActivity).getToolbar());
        }
    }

    @Override public void onFragmentShow() {
        super.onFragmentShow();
        setHasOptionsMenu(true);
        mActivity.setTitle(mCurrentYear+"年份消费统计图");
    }

    @Override public void onFragmentHide() {
        super.onFragmentHide();
        setHasOptionsMenu(false);
        mActivity.setTitle(R.string.app_name);
    }

    @Override public void onStop() {
        super.onStop();
        unRegister();
    }

    @SuppressWarnings("unchecked") private void initChart() {
        mBarChartDisplay = new BarChartDisplayImpl();
        mPieChartDisplay = new PieChartDisplayImpl();
        mLineChartDisplay = new LineChartDisplayImpl();
        mBarChartDisplay.init(mConsumerBarChart, true);
        mPieChartDisplay.init(mPercentagePieChart, true);
        mLineChartDisplay.init(mFuelLineChart, true);
        addSubscription(processAccidentalClick(mSaveConsumer, mConsumerBarChart));
        addSubscription(processAccidentalClick(mSaveFuel, mFuelLineChart));
        addSubscription(processAccidentalClick(mSavePercentage, mPercentagePieChart));

        RxView.touches(mConsumerBarChart)
              .throttleFirst(Consts.SHIELD_TIME, TimeUnit.SECONDS)
              .subscribe(new Action1<MotionEvent>() {
                  @Override public void call(MotionEvent motionEvent) {
                      startChartActivity(ChartType.BAR_CHART);
                  }
              });
        RxView.touches(mPercentagePieChart)
              .throttleFirst(Consts.SHIELD_TIME, TimeUnit.SECONDS)
              .subscribe(new Action1<MotionEvent>() {
                  @Override public void call(MotionEvent motionEvent) {
                      startChartActivity(ChartType.PIE_CHART);
                  }
              });
        RxView.touches(mFuelLineChart)
              .throttleFirst(Consts.SHIELD_TIME, TimeUnit.SECONDS)
              .subscribe(new Action1<MotionEvent>() {
                  @Override public void call(MotionEvent motionEvent) {
                      startChartActivity(ChartType.LINE_CHART);
                  }
              });
    }

    private void startChartActivity(@ChartType int type) {
        ChartActivity.start(mActivity, type, mStartTime, mEndTime);
    }

    private void loadData(int year) {
        mActivity.setTitle(mCurrentYear+"年份消费统计图");
        mStartTime = DateUtil.getTime(year);
        mEndTime = DateUtil.getTime(year + 1) - 1;
        addSubscription(loadConsumerDetail(mStartTime, mEndTime));
        addSubscription(loadFuelConsumption(mStartTime, mEndTime));
    }

    /** 加载消费信息 */
    private Subscription loadConsumerDetail(long startTime, long endTime) {
        return mConsumerDao.queryBetween(startTime, endTime)
                           .compose(RxUtil.<List<ConsumerDetail>>applySchedulers(RxUtil.IO_ON_UI_TRANSFORMER))
                           .flatMap(new Func1<List<ConsumerDetail>, Observable<Object>>() {
                               @Override public Observable<Object> call(List<ConsumerDetail> consumerDetails) {
                                   if (DataUtil.isEmpty(consumerDetails)) { return null; }
                                   return Observable.just(mBarChartDisplay.convert(consumerDetails),
                                                          mPieChartDisplay.convert(consumerDetails));
                               }
                           })
                           .subscribe(new Action1<Object>() {
                               @Override public void call(Object data) {
                                   if (null == data) { return; }
                                   if (null != mBarChartDisplay && data instanceof BarData) {
                                       mBarChartDisplay.animationDisplay(mConsumerBarChart, data, ANIMATE_DURATION);
                                       mSaveConsumer.setVisibility(View.VISIBLE);
                                   } else if (null != mPieChartDisplay &&
                                              data instanceof PieChartDisplayImpl.PieChartData) {
                                       PieChartDisplayImpl.PieChartData pieChartData =
                                               (PieChartDisplayImpl.PieChartData)data;
                                       mPieChartDisplay.animationDisplay(mPercentagePieChart, pieChartData,
                                                                         ANIMATE_DURATION);
                                       addPercentageDetailView(pieChartData);
                                       mSavePercentage.setVisibility(
                                               null != pieChartData.pieData ? View.VISIBLE : View.GONE);
                                   }
                               }
                           });
    }

    /** 加载油耗信息 */
    private Subscription loadFuelConsumption(long startTime, long endTime) {
        return mConsumerDao.query(Consts.TYPE_FUEL, startTime, endTime, false, true)
                           .compose(RxUtil.<List<ConsumerDetail>>applySchedulers(RxUtil.IO_ON_UI_TRANSFORMER))
                           .map(new Func1<List<ConsumerDetail>, LineChartDisplayImpl.LineChartData>() {
                               @Override public LineChartDisplayImpl.LineChartData call(List<ConsumerDetail> list) {
                                   return (LineChartDisplayImpl.LineChartData)mLineChartDisplay.convert(list);
                               }
                           })
                           .subscribe(new Action1<LineChartDisplayImpl.LineChartData>() {
                               @Override public void call(LineChartDisplayImpl.LineChartData lineChartData) {
                                   if (null != mLineChartDisplay) {
                                       mLineChartDisplay.animationDisplay(mFuelLineChart, lineChartData,
                                                                          ANIMATE_DURATION);
                                   }
                                   if (null != lineChartData.minFuelConsumption) {
                                       mMinMoney.setText(Util.formatRMB(lineChartData.minFuelConsumption.getMoney()));
                                       mMinOilMess.setText(Util.formatOilMess(lineChartData.minFuelConsumption.getOilMass()));
                                   }
                                   if (null != lineChartData.maxFuelConsumption) {
                                       mMaxMoney.setText(Util.formatRMB(lineChartData.maxFuelConsumption.getMoney()));
                                       mMaxOilMess.setText(Util.formatOilMess(lineChartData.maxFuelConsumption.getOilMass()));
                                   }
                                   mSaveFuel.setVisibility(null != lineChartData.lineData ? View.VISIBLE : View.GONE);
                               }
                           }, RxUtil.ERROR_ACTION);
    }

    private Subscription processAccidentalClick(TextView view, final Chart chart){
        return RxView.clicks(view)
                     .throttleFirst(Consts.SHIELD_TIME, TimeUnit.SECONDS)
                     .subscribe(new Action1<Void>() {
                         @Override public void call(Void aVoid) {
                             ToastUtil.showToast(mAppContext, chart.saveToGallery(Util.createImageName(), 100)
                                                              ? R.string.chart_save_success
                                                              : R.string.chart_save_fail);
                         }
                     });
    }

    /**
     * 添加消费百分比详细信息
     */
    private void addPercentageDetailView(@NonNull PieChartDisplayImpl.PieChartData pieChartData) {
        if (mPercentageDetail.getChildCount() > 0) {
            mPercentageDetail.removeAllViews();
        }
        if(null == mLayoutInflater){
            mLayoutInflater = LayoutInflater.from(mActivity);
        }

        List<Float> values = new ArrayList<>();
        for(int i =0; i < pieChartData.groupMoney.size();i++){
            values.add(pieChartData.groupMoney.valueAt(i));
        }
        // 顺序
        // Collections.sort(values);
        // 倒序
        Collections.sort(values, new Comparator<Float>() {
            @Override public int compare(Float o1, Float o2) {
                return o2.compareTo(o1);
            }
        });
        int rows = 1;
        for (Float item : values) {
            int key = pieChartData.groupMoney.keyAt(pieChartData.groupMoney.indexOfValue(item));
            @SuppressLint("InflateParams")
            View itemView = mLayoutInflater.inflate(R.layout.item_table, null);
            ((TextView) itemView.findViewById(R.id.item_table_lable)).setText(Consts.TYPE_MENUS[key]);
            ((TextView) itemView.findViewById(R.id.item_table_total_money)).setText(
                    Util.formatRMB(item));
            ((TextView) itemView.findViewById(R.id.item_table_percentage)).setText(
                    Util.formatPercentage(item, pieChartData.totalMoney));
            itemView.findViewById(R.id.item_table_bottom_divider)
                    .setVisibility(rows == values.size() ? View.VISIBLE : View.GONE);
            mPercentageDetail.addView(itemView, LinearLayout.LayoutParams.MATCH_PARENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT);
            rows++;
        }
        @SuppressLint("InflateParams")
        View totalView = mLayoutInflater.inflate(R.layout.item_total_table, null);
        ((TextView) totalView.findViewById(R.id.item_total_table_value)).setText(
                Util.formatRMB(pieChartData.totalMoney));
        mPercentageDetail.addView(totalView, LinearLayout.LayoutParams.MATCH_PARENT,
                                  LinearLayout.LayoutParams.WRAP_CONTENT);

    }
}
