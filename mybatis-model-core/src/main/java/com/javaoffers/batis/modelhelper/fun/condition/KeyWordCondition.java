package com.javaoffers.batis.modelhelper.fun.condition;

import com.javaoffers.batis.modelhelper.fun.ConditionTag;
import com.javaoffers.batis.modelhelper.utils.TableHelper;

import java.util.Collections;
import java.util.Map;

/**
 * @Description: select statement: query fields
 * @Auther: create by cmj on 2022/5/2 16:31
 */
@Deprecated
public class KeyWordCondition extends SelectColumnCondition  {

    private String keyWord;
    private Class mClass;

    @Override
    public ConditionTag getConditionTag() {
        return ConditionTag.SELECT;
    }

    @Override
    public String getSql() {
        return keyWord+" ";
    }

    @Override
    public Map<String, Object> getParams() {
        return Collections.EMPTY_MAP;
    }

    public KeyWordCondition(String keyWord) {
        super(keyWord);
        this.keyWord = keyWord;
    }

    @Override
    public String toString() {
        return keyWord+" : "+getConditionTag().toString();
    }
}