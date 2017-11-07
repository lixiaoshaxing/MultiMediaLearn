package com.lx.multimedialearn.main;

/**
 * 对应task的model
 *
 * @author lixiao
 * @since 2017-09-05 17:31
 */
public class TabModel {
    /**
     * task对应的题目
     */
    String title;
    /**
     * tab对应描述
     */
    String des;

    /**
     * 需要跳转的activity对应class
     */
    Class aimActivity;

    public TabModel(String title, String des, Class aimActivity) {
        this.title = title;
        this.des = des;
        this.aimActivity = aimActivity;
    }
}
