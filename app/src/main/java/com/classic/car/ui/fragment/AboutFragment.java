package com.classic.car.ui.fragment;

import com.classic.car.R;
import com.classic.core.fragment.BaseFragment;

/**
 * 应用名称: CarAssistant
 * 包 名 称: com.classic.car.ui.fragment
 *
 * 文件描述：关于页面
 * 创 建 人：续写经典
 * 创建时间：16/5/29 下午2:21
 */
public class AboutFragment extends BaseFragment {

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }
    @Override public int getLayoutResId() {
        return R.layout.fragment_about;
    }


}
