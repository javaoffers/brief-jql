package com.javaoffers.batis.modelhelper.core;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MoreSQLInfo extends SQLInfo {

    private List<SQLInfo> sqlInfos = new LinkedList<>();

    public MoreSQLInfo() {
        super();
    }

    public void addSqlInfo(SQLInfo sqlInfo){
        if(sqlInfo!=null && sqlInfo.isStatus()){
            sqlInfos.add(sqlInfo);
        }
    }

    public void addAllSqlInfo(Collection<SQLInfo> sqlInfos){
        if(!CollectionUtils.isEmpty(sqlInfos)){
            this.sqlInfos.addAll(sqlInfos);
        }
    }

    public List<SQLInfo> getSqlInfos(){
        return sqlInfos;
    }

}