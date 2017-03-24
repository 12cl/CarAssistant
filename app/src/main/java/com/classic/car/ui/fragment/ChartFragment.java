package com.classic.car.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.classic.car.R;
import com.classic.car.app.CarApplication;
import com.classic.car.consts.Consts;
import com.classic.car.db.dao.ConsumerDao;
import com.classic.car.entity.ConsumerDetail;
import com.classic.car.entity.FuelConsumption;
import com.classic.car.ui.base.AppBaseFragment;
import com.classic.car.utils.ChartUtil;
import com.classic.car.utils.DataUtil;
import com.classic.car.utils.DateUtil;
import com.classic.car.utils.MoneyUtil;
import com.classic.car.utils.RxUtil;
import com.classic.car.utils.ToastUtil;
import com.classic.car.utils.Util;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.PieData;
import com.jakewharton.rxbinding.view.RxView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class ChartFragment extends AppBaseFragment {
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

    private float                            mTotalMoney;
    private Map<Integer, Float>              mValuesMap;
    private Observable<List<ConsumerDetail>> mAllData;
    private FuelConsumption                  mMinFuelConsumption;
    private FuelConsumption                  mMaxFuelConsumption;
    private LayoutInflater                   mLayoutInflater;

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

        ChartUtil.initLineChart(mAppContext, mFuelLineChart);
        ChartUtil.initBarChart(mAppContext, mConsumerBarChart);
        ChartUtil.initPieChart(mAppContext, mPercentagePieChart);
        addSubscription(processAccidentalClick(mSaveConsumer, mConsumerBarChart));
        addSubscription(processAccidentalClick(mSaveFuel, mFuelLineChart));
        addSubscription(processAccidentalClick(mSavePercentage, mPercentagePieChart));
        // mAllData = mConsumerDao.queryByType(null);
        mAllData = mConsumerDao.queryBetween(DateUtil.getTime(2017), DateUtil.getTime(2018)-1);
        addSubscription(processLineChartData());
        addSubscription(processBarChartData());
        addSubscription(processPieChartData());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.chart_menu, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_date:

                return true;
            default:
                return false;
        }
    }

    @Override public void onFragmentShow() {
        super.onFragmentShow();
        setHasOptionsMenu(true);
        // mFuelLineChart.animateXY(ANIMATE_DURATION, ANIMATE_DURATION);
        // mConsumerBarChart.animateXY(ANIMATE_DURATION, ANIMATE_DURATION);
        // mPercentagePieChart.animateXY(ANIMATE_DURATION, ANIMATE_DURATION);
    }

    @Override public void onFragmentHide() {
        super.onFragmentHide();
        setHasOptionsMenu(false);
    }

    private Subscription processBarChartData() {
        return mAllData.compose(RxUtil.<List<ConsumerDetail>>applySchedulers(RxUtil.IO_ON_UI_TRANSFORMER))
                       .flatMap(new Func1<List<ConsumerDetail>, Observable<BarData>>() {
                           @Override public Observable<BarData> call(List<ConsumerDetail> list) {
                               return Observable.just(ChartUtil.convertBarData(mAppContext, list));
                           }
                       })
                       .subscribe(new Action1<BarData>() {
                           @Override public void call(BarData barData) {
                               if (null != barData) {
                                   mConsumerBarChart.setData(barData);
                                   mConsumerBarChart.animateXY(ANIMATE_DURATION, ANIMATE_DURATION);
                               }
                               mSaveConsumer.setVisibility(null != barData ? View.VISIBLE : View.GONE);
                           }
                       }, RxUtil.ERROR_ACTION);
    }

    private Subscription processPieChartData() {
        return mAllData.compose(RxUtil.<List<ConsumerDetail>>applySchedulers(RxUtil.IO_ON_UI_TRANSFORMER))
                       .flatMap(new Func1<List<ConsumerDetail>, Observable<Map<Integer, Float>>>() {
                           @Override public Observable<Map<Integer, Float>> call(List<ConsumerDetail> list) {
                               mValuesMap = new HashMap<>();
                               mTotalMoney = 0;
                               for (int i = 0; i < list.size(); i++) {
                                   final int type = list.get(i).getType();
                                   if (!mValuesMap.containsKey(type)) {
                                       mValuesMap.put(type, 0f);
                                   }
                                   final float money = list.get(i).getMoney();
                                   mTotalMoney = MoneyUtil.newInstance(mTotalMoney).add(money).create().floatValue();
                                   mValuesMap.put(type,
                                           MoneyUtil.newInstance(mValuesMap.get(type)).add(money).create().floatValue
                                                   ());
                               }
                               return Observable.just(mValuesMap);
                           }
                       })
                       .flatMap(new Func1<Map<Integer, Float>, Observable<PieData>>() {
                           @Override public Observable<PieData> call(Map<Integer, Float> map) {
                               return Observable.just(ChartUtil.convertPieData(mAppContext, mTotalMoney, map));
                           }
                       })
                       .subscribe(new Action1<PieData>() {
                           @Override public void call(PieData pieData) {
                               if (null != pieData) {
                                   mPercentagePieChart.setData(pieData);
                                   mPercentagePieChart.animateXY(ANIMATE_DURATION, ANIMATE_DURATION);
                               }
                               mSavePercentage.setVisibility(null != pieData ? View.VISIBLE : View.GONE);
                               processPercentageDetail();
                           }
                       }, RxUtil.ERROR_ACTION);
    }

    private void processPercentageDetail() {
        if(DataUtil.isEmpty(mValuesMap)) { return; }
        if (mPercentageDetail.getChildCount() > 0) {
            mPercentageDetail.removeAllViews();
        }
        if(null == mLayoutInflater){
            mLayoutInflater = LayoutInflater.from(mActivity);
        }
        int rows = 1;
        ArrayList<Map.Entry<Integer, Float>> list = new ArrayList<>(mValuesMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Float>>() {
            @Override public int compare(Map.Entry<Integer, Float> o1, Map.Entry<Integer, Float> o2) {
                // 按降序排列
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        for (Map.Entry<Integer, Float> item : list) {
            View itemView = mLayoutInflater.inflate(R.layout.item_table, null);
            ((TextView) itemView.findViewById(R.id.item_table_lable)).setText(Consts.TYPE_MENUS[item.getKey()]);
            ((TextView) itemView.findViewById(R.id.item_table_total_money)).setText(
                    Util.formatRMB(item.getValue()));
            ((TextView) itemView.findViewById(R.id.item_table_percentage)).setText(
                    Util.formatPercentage(item.getValue(), mTotalMoney));
            itemView.findViewById(R.id.item_table_bottom_divider)
                    .setVisibility(rows == mValuesMap.size() ? View.VISIBLE : View.GONE);
            mPercentageDetail.addView(itemView, LinearLayout.LayoutParams.MATCH_PARENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT);
            rows++;
        }
        // for (Integer key : mValuesMap.keySet()) {
        //     View itemView = mLayoutInflater.inflate(R.layout.item_table, null);
        //     ((TextView) itemView.findViewById(R.id.item_table_lable)).setText(Consts.TYPE_MENUS[key]);
        //     ((TextView) itemView.findViewById(R.id.item_table_total_money)).setText(
        //             Util.formatRMB(mValuesMap.get(key)));
        //     ((TextView) itemView.findViewById(R.id.item_table_percentage)).setText(
        //             Util.formatPercentage(mValuesMap.get(key), mTotalMoney));
        //     itemView.findViewById(R.id.item_table_bottom_divider)
        //             .setVisibility(rows == mValuesMap.size() ? View.VISIBLE : View.GONE);
        //     mPercentageDetail.addView(itemView, LinearLayout.LayoutParams.MATCH_PARENT,
        //             LinearLayout.LayoutParams.WRAP_CONTENT);
        //     rows++;
        // }
        View totalView = mLayoutInflater.inflate(R.layout.item_total_table, null);
        ((TextView) totalView.findViewById(R.id.item_total_table_value)).setText(Util.formatRMB(mTotalMoney));
        mPercentageDetail.addView(totalView, LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

    }

    private Subscription processLineChartData() {
        // return mConsumerDao.queryByType(Consts.TYPE_FUEL)
        return mConsumerDao.query(Consts.TYPE_FUEL, DateUtil.getTime(2017), DateUtil.getTime(2018)-1, false)
                           .compose(RxUtil.<List<ConsumerDetail>>applySchedulers(RxUtil.IO_ON_UI_TRANSFORMER))
                           .flatMap(new Func1<List<ConsumerDetail>, Observable<List<FuelConsumption>>>() {
                               @Override public Observable<List<FuelConsumption>> call(List<ConsumerDetail> list) {
                                   List<FuelConsumption> result = new ArrayList<>();
                                   mMinFuelConsumption = null;
                                   mMaxFuelConsumption = null;
                                   for (int i = 0; i < list.size() - 1; i++) {
                                       ConsumerDetail startItem = list.get(i);
                                       ConsumerDetail endItem = list.get(i + 1);
                                       final long mileage = endItem.getCurrentMileage() - startItem.getCurrentMileage();
                                       final float money = mileage == 0L
                                                           ? 0
                                                           : MoneyUtil.newInstance(startItem.getMoney())
                                                                      .multiply(100)
                                                                      .divide(mileage)
                                                                      .create()
                                                                      .floatValue();
                                       final float oilMass = startItem.getUnitPrice() == 0F
                                                             ? 0
                                                             : MoneyUtil.newInstance(money)
                                                                        .divide(startItem.getUnitPrice())
                                                                        .create()
                                                                        .floatValue();

                                       final FuelConsumption item = new FuelConsumption(mileage,
                                               Float.valueOf(MoneyUtil.replace(money)),
                                               Float.valueOf(MoneyUtil.replace(oilMass)));
                                       result.add(item);
                                       mMinFuelConsumption = null == mMinFuelConsumption
                                                             ? item
                                                             : (item.getMoney() < mMinFuelConsumption.getMoney()
                                                                ? item
                                                                : mMinFuelConsumption);
                                       mMaxFuelConsumption = null == mMaxFuelConsumption
                                                             ? item
                                                             : (item.getMoney() > mMaxFuelConsumption.getMoney()
                                                                ? item
                                                                : mMaxFuelConsumption);
                                   }
                                   return Observable.just(result);
                               }
                           })
                           .flatMap(new Func1<List<FuelConsumption>, Observable<LineData>>() {
                               @Override public Observable<LineData> call(List<FuelConsumption> list) {
                                   return Observable.just(ChartUtil.convertLineData(mAppContext, list));
                               }
                           })
                           .subscribe(new Action1<LineData>() {
                               @Override public void call(LineData lineData) {
                                   if (null != lineData) {
                                       mFuelLineChart.setData(lineData);
                                       mFuelLineChart.animateXY(ANIMATE_DURATION, ANIMATE_DURATION);
                                   }
                                   if (null != mMinFuelConsumption) {
                                       mMinMoney.setText(Util.formatRMB(mMinFuelConsumption.getMoney()));
                                       mMinOilMess.setText(Util.formatOilMess(mMinFuelConsumption.getOilMass()));
                                   }
                                   if (null != mMaxFuelConsumption) {
                                       mMaxMoney.setText(Util.formatRMB(mMaxFuelConsumption.getMoney()));
                                       mMaxOilMess.setText(Util.formatOilMess(mMaxFuelConsumption.getOilMass()));
                                   }
                                   mSaveFuel.setVisibility(null != lineData ? View.VISIBLE : View.GONE);
                               }
                           }, RxUtil.ERROR_ACTION);
    }

    private Subscription processAccidentalClick(TextView view, final Chart chart){
        return RxView.clicks(view)
                     .throttleFirst(Consts.SHIELD_TIME, TimeUnit.SECONDS)
                     .subscribe(new Action1<Void>() {
                         @Override public void call(Void aVoid) {
                             final String fileName = new StringBuilder("CarAssistant_").append(
                                     DateUtil.formatDate("yyyy-MM-dd_HH:mm:ss", System.currentTimeMillis()))
                                                                                       .append(".png")
                                                                                       .toString();

                             ToastUtil.showToast(mAppContext, chart.saveToGallery(fileName, 100)
                                                              ? R.string.chart_save_success
                                                              : R.string.chart_save_fail);
                         }
                     });
    }
}
