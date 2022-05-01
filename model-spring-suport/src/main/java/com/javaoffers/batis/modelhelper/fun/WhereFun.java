package com.javaoffers.batis.modelhelper.fun;

import jdk.nashorn.internal.objects.annotations.Where;

/**
 * @Description: sql where 语句
 * @Auther: create by cmj on 2022/5/2 00:08
 */
public interface WhereFun<M,C, V, R extends WhereFun<M,C,V,R>> {

    /**
     * 拼接Or, 否者默认都是 and
     * @return
     */
    public R or();

    /**
     * 添加等值关系 =
     * @param col
     * @param value
     * @return
     */
    public R eq(C col, V value);

    /**
     * 添加不等值关系 !=
     * @param col
     * @param value
     * @return
     */
    public R ueq(C col, V value);

    /**
     * 大于  >
     * @param col
     * @param value
     * @return
     */
    public R gt(C col, V value);

    /**
     * 小于 <
     * @param col
     * @param value
     * @return
     */
    public R lt(C col, V value);

    /**
     * 大于等于  >=
     * @param col
     * @param value
     * @return
     */
    public R gtEq(C col, V value);

    /**
     * 小于等于 <=
     * @param col
     * @param value
     * @return
     */
    public R ltEq(C col, V value);

    /**
     * sql 范围 : between
     * @param col
     * @param start
     * @Param end
     * @return
     */
    public R between(C col, V start,  V end);

    /**
     * sql 模糊： like
     * @param col
     * @param value
     * @return
     */
    public R like(C col, V value);

    /**
     * sql语句  in
     * @param col
     * @param values
     * @return
     */
    public R in(C col, V... values);





}
