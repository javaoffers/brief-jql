package com.javaoffers.brief.modelhelper.fun.general.impl;

import com.javaoffers.brief.modelhelper.anno.derive.flag.*;
import com.javaoffers.brief.modelhelper.core.ConvertRegisterSelectorDelegate;
import com.javaoffers.brief.modelhelper.core.Id;
import com.javaoffers.brief.modelhelper.exception.*;
import com.javaoffers.brief.modelhelper.fun.GetterFun;
import com.javaoffers.brief.modelhelper.fun.crud.WhereFun;
import com.javaoffers.brief.modelhelper.fun.crud.WhereModifyFun;
import com.javaoffers.brief.modelhelper.fun.crud.WhereSelectFun;
import com.javaoffers.brief.modelhelper.fun.crud.delete.DeleteWhereFun;
import com.javaoffers.brief.modelhelper.fun.crud.impl.SelectFunImpl;
import com.javaoffers.brief.modelhelper.fun.crud.impl.delete.DeleteFunImpl;
import com.javaoffers.brief.modelhelper.fun.crud.impl.insert.InsertFunImpl;
import com.javaoffers.brief.modelhelper.fun.crud.impl.insert.MoreInsertFunImpl;
import com.javaoffers.brief.modelhelper.fun.crud.impl.update.UpdateFunImpl;
import com.javaoffers.brief.modelhelper.fun.crud.insert.MoreInsertFun;
import com.javaoffers.brief.modelhelper.fun.crud.update.SmartUpdateFun;
import com.javaoffers.brief.modelhelper.fun.general.GeneralFun;
import com.javaoffers.brief.modelhelper.utils.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import com.javaoffers.brief.modelhelper.utils.Assert;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Commonly used function implementation
 *
 * @param <T>
 * @author create by mingjie
 */
public class GeneralFunImpl<T, C extends GetterFun<T, Object>, V> implements GeneralFun<T, C, V> {

    static ConvertRegisterSelectorDelegate convert = ConvertRegisterSelectorDelegate.convert;

    private AtomicInteger ato = new AtomicInteger(0);

    private Class<T> mClass;

    private SelectFunImpl<T> selectFun;

    private InsertFunImpl<T> insertFun;

    private UpdateFunImpl<T, C, V> updateFun;

    private DeleteFunImpl<T> deleteFun;

    private NativeFunImpl nativeFun;

    private String tableName;

    private String primaryColNmae;

    private Field numOrStringField;

    private Field primaryField;

    private TableInfo tableInfo;

    public GeneralFunImpl(Class<T> mClass, SelectFunImpl<T> selectFun,
                          InsertFunImpl<T> insertFun, UpdateFunImpl<T, C, V> updateFun,
                          DeleteFunImpl<T> deleteFun, NativeFunImpl nativeFun) {
        this.mClass = mClass;
        this.selectFun = selectFun;
        this.insertFun = insertFun;
        this.updateFun = updateFun;
        this.deleteFun = deleteFun;
        this.tableName = TableHelper.getTableName(mClass);
        this.tableInfo = TableHelper.getTableInfo(mClass);
        this.nativeFun = nativeFun;
        Collection<Field> fields = this.tableInfo.getFieldNameAndField().values();
        for(Field field : fields) {
            Class<?> type = Utils.baseClassUpgrade(field.getType());
            if(Number.class.isAssignableFrom(type) || String.class.isAssignableFrom(type)){
                this.numOrStringField = field;
                break;
            }
        }
        Map<String, ColumnInfo> primaryColNames = this.tableInfo.getPrimaryColNames();
        if(MapUtils.isEmpty(primaryColNames)){
            return;
        }
        this.primaryColNmae = primaryColNames.keySet().iterator().next();
        this.primaryField = this.tableInfo.getColNameAndFieldOfModel().get(this.primaryColNmae).get(0);
    }

    @Override
    public Id save(T model) {
        if (model == null) {
            return Id.EMPTY_ID;
        }
        Id ex = insertFun.colAll(model).ex();
        if (ex != null) {
            return ex;
        }
        return Id.EMPTY_ID;
    }

    @Override
    public void saveOrModify(T model) {
        if (model == null) {
            return;
        }
        ArrayList<T> models = new ArrayList<>();
        models.add(model);
        saveOrModify(models);
    }

    @Override
    public void saveOrReplace(T model) {
        if (model == null) {
            return;
        }
        this.saveOrReplace(Lists.newArrayList(model));
    }

    @Override
    public List<Id> saveBatch(Collection<T> models) {
        if (CollectionUtils.isEmpty(models)) {
            return Collections.EMPTY_LIST;
        }
        List<Id> exs = insertFun.colAll(models).exs();
        if (exs != null) {
            return exs;
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void saveOrModify(Collection<T> models) {
        if (CollectionUtils.isEmpty(models)) {
            return;
        }
        //mysql and h2 for on duc
        if(tableInfo.getDbType().isSupportDuplicateModify()){
            MoreInsertFun<T, GetterFun<T, Object>, Object> moreInserFun = insertFun.colAll(models);
            ((MoreInsertFunImpl) moreInserFun).dupUpdate().exs();
        }else {
            List<T> updateList = saveWithFails(models);
            this.modifyBatchById(updateList);
        }

    }

    @Override
    public void saveOrReplace(Collection<T> models) {
        List<T> updateList = saveWithFails(models);
        this.replaceBatchById(updateList);
    }

    @Override
    public int remove(T model) {
        if (model == null) {
            return 0;
        }
        DeleteWhereFun<T, GetterFun<T, Object>, Object> where = deleteFun.where();
        AtomicBoolean status = parseWhere(model, where);
        if (status.get()) {
            return where.ex();
        }
        return 0;
    }

    @Override
    public int removeById(Serializable id) {
        return removeByIds(id);
    }

    @Override
    @SafeVarargs
    public final int removeByIds(Serializable... ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        Set<Serializable> idSet = Arrays.stream(ids).collect(Collectors.toSet());
        return removeByIds(idSet);
    }

    @Override
    public int logicRemove(T model) {
        if (model == null) {
            return 0;
        }

        T newModel = getLogicRemoveModel();
        WhereModifyFun<T, V> where = this.updateFun.npdateNull().colAll(newModel).where();
        if (parseWhere(model, where).get()) {
            return where.ex();
        }
        return 0;
    }

    @Override
    public int logicRemoveById(Serializable id) {
        HashSet<Serializable> ids = new HashSet<>();
        ids.add(id);
        return logicRemoveByIds(ids);
    }

    @Override
    @SafeVarargs
    public final int logicRemoveByIds(Serializable... ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return 0;
        }
        Set<Serializable> idsSet = Arrays.stream(ids).filter(Objects::nonNull).collect(Collectors.toSet());
        return logicRemoveByIds(idsSet);
    }

    @Override
    public <ID extends Serializable> int logicRemoveByIds(Collection<ID> ids) {
        assertPrimary();
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        T logicRemoveModel = getLogicRemoveModel();
        WhereModifyFun<T, V> where = this.updateFun.npdateNull().colAll(logicRemoveModel).where();
        Map<String, Object> param = new HashMap<>();
        String newColNameTag = getNewColNameTag();
        param.putIfAbsent(newColNameTag, ids);
        where.condSQL(tableName + "." + this.primaryColNmae + " in (#{" + newColNameTag + "})", param);
        return where.ex();
    }

    @Override
    public int removeByIds(Collection ids) {
        assertPrimary();
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        DeleteWhereFun<T, GetterFun<T, Object>, Object> where = deleteFun.where();
        Map<String, Object> param = new HashMap<>();
        String newColNameTag = getNewColNameTag();
        param.putIfAbsent(newColNameTag, ids);
        where.condSQL(this.primaryColNmae + " in ( #{" + newColNameTag + "} ) ", param);

        return where.ex();
    }

    @Override
    public int modifyById(T model) {
        if (model != null) {
            return modifyBatchById(Lists.newArrayList(model));
        }
        return 0;
    }

    @Override
    public int replaceById(T model) {
        if (model == null) {
            return 0;
        }
        return renovateBatchById(Lists.newArrayList(model), true);
    }

    @Override
    public int modifyBatchById(Collection<T> models) {
        return renovateBatchById(models, false);
    }

    @Override
    public int replaceBatchById(Collection<T> models) {
        return renovateBatchById(models, true);
    }

    @Override
    public int vsModifyById(T model) {
        if (model == null) {
            return 0;
        }
        return renovateByIdWithVersion(Lists.newArrayList(model), false);
    }

    @Override
    public int vsReplaceById(T model) {
        if (model == null) {
            return 0;
        }
        return renovateByIdWithVersion(Lists.newArrayList(model), true);
    }

    @Override
    public int vsModifyByIds(Collection<T> models) {
        if (CollectionUtils.isEmpty(models)) {
            return 0;
        }
        return renovateByIdWithVersion(models, false);
    }

    @Override
    public int vsReplaceByIds(Collection<T> models) {
        if (CollectionUtils.isEmpty(models)) {
            return 0;
        }
        return renovateByIdWithVersion(models, true);
    }

    @Override
    public List<T> query(T model) {
        if (model == null) {
            return Collections.EMPTY_LIST;
        }
        Pair<Boolean, WhereSelectFun<T, Object>> whereInfo = parseQueryWhere(model);
        //If there is no condition to return directly, the full table will not be queried. Avoid full table scan
        if (!whereInfo.getLeft()) {
            return Collections.EMPTY_LIST;
        }
        WhereSelectFun<T, Object> where = whereInfo.getRight();
        return where.exs();
    }

    @Override
    public T queryOne(T model) {
        List<T> list = query(model);
        if(CollectionUtils.isEmpty(list)){
            return null;
        }
        return list.get(0);
    }

    @Override
    public T queryOnlyOne(T model) {
        List<T> list = query(model);
        if(CollectionUtils.isEmpty(list)){
           return null;
        }else if(list.size() != 1){
            throw new QueryDataException("There are multiple query data");
        }
        return list.get(0);
    }

    @Override
    public List<T> query(T model, int pageNum, int pageSize) {
        if (model == null) {
            return Collections.EMPTY_LIST;
        }
        Pair<Boolean, WhereSelectFun<T, Object>> whereInfo = parseQueryWhere(model);
        //If there is no condition to return directly, the full table will not be queried. Avoid full table scan
        if (!whereInfo.getLeft()) {
            return Collections.EMPTY_LIST;
        }
        WhereSelectFun<T, Object> where = whereInfo.getRight();
        where.limitPage(pageNum, pageSize);
        return where.exs();
    }

    @Override
    public List<T> query(int pageNum, int pageSize) {
        if (pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        List<T> exs = this.selectFun.colAll().where().limitPage(pageNum, pageSize).exs();
        if (exs == null) {
            exs = Collections.EMPTY_LIST;
        }
        return exs;
    }

    @Override
    public T queryById(Serializable id) {
        if (id == null) {
            return null;
        }
        List<T> ts = queryByIds(id);
        if (ts == null || ts.size() == 0) {
            return null;
        }
        return ts.get(0);
    }

    @Override
    @SafeVarargs
    public final List<T> queryByIds(Serializable... ids) {
        if (ids == null || ids.length == 0) {
            return Collections.EMPTY_LIST;
        }
        Set idSet = Arrays.stream(ids).collect(Collectors.toSet());
        return queryByIds(idSet);
    }

    @Override
    public List<T> queryByIds(Collection ids) {
        assertPrimary();
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.EMPTY_LIST;
        }
        WhereSelectFun<T, Object> where = this.selectFun.colAll().where();
        HashMap<String, Object> param = new HashMap<>();
        String newColNameTag = getNewColNameTag();
        param.put(newColNameTag, ids);
        where.condSQL(this.primaryColNmae + " in ( #{" + newColNameTag + "} ) ", param);
        List<T> exs = where.exs();
        if (exs != null && exs.size() > 0) {
            return exs;
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<T> queryByIds(List ids) {
        return queryByIds((Collection) ids);
    }

    @Override
    public List<T> queryByIds(Set ids) {
        return queryByIds((Collection) ids);
    }

    @Override
    public List<T> queryByParam(Map<String, Object> param) {
        //avoid all table scan
        if (MapUtils.isEmpty(param)) {
            return Collections.EMPTY_LIST;
        }
        return queryByParam(param, -1, -1, false);
    }

    @Override
    public List<T> queryByParam(Map<String, Object> param, int pageNum, int pageSize) {
        return queryByParam(param, pageNum, pageSize, true);
    }

    @Override
    public Number count() {
        T ex = selectFun.col("count(1) as " + this.numOrStringField.getName()).where().ex();
        return getNumber(numOrStringField, ex);
    }

    @Override
    public Number count(C c) {
        Assert.isTrue(c != null, " count is null .");
        Pair<String, String> colNameAndAliasName = TableHelper.getColNameAndAliasName(c);
        T ex = selectFun.col("count(" + colNameAndAliasName.getLeft() + ") as " + numOrStringField.getName()).where().ex();
        return getNumber(numOrStringField, ex);
    }

    @Override
    public Number countDistinct(C c) {
        Assert.isTrue(c != null, " count is null .");
        Pair<String, String> colNameAndAliasName = TableHelper.getColNameAndAliasName(c);
        T ex = selectFun.col("count(Distinct(" + colNameAndAliasName.getLeft() + ")) as " + numOrStringField.getName()).where().ex();
        return getNumber(numOrStringField, ex);
    }

    @Override
    public Number count(T model) {
        WhereSelectFun<T, Object> where = selectFun.col("count(1) as " + numOrStringField.getName()).where();
        if (parseWhere(model, where).get()) {
            T ex = where.ex();
            return getNumber(numOrStringField, ex);
        }
        return 0L;
    }

    @Override
    public Number count(C c, T model) {
        Assert.isTrue(c != null, " count is null .");
        Pair<String, String> colNameAndAliasName = TableHelper.getColNameAndAliasName(c);
        WhereSelectFun<T, Object> where = selectFun.col("count(" + colNameAndAliasName.getLeft() + ") as " + numOrStringField.getName()).where();
        if (parseWhere(model, where).get()) {
            T ex = where.ex();
            return getNumber(numOrStringField, ex);
        }
        return 0L;
    }

    @Override
    public Number countDistinct(C c, T model) {
        Assert.isTrue(c != null, " count is null .");
        Pair<String, String> colNameAndAliasName = TableHelper.getColNameAndAliasName(c);
        WhereSelectFun<T, Object> where = selectFun.col("count(Distinct(" + colNameAndAliasName.getLeft() + ")) as " + numOrStringField.getName()).where();
        if (parseWhere(model, where).get()) {
            T ex = where.ex();
            return getNumber(numOrStringField, ex);
        }
        return 0L;
    }

    @Override
    public String ddlSQL(String sql) {
        return this.nativeFun.setSqlText(sql, SQLType.DDL).ex();
    }

    @Override
    public String ddlSQL(String sql, Map<String, Object> param) {
        return this.nativeFun.setSqlText(sql, SQLType.DDL).setParamMap(param).ex();
    }

    private Pair<Boolean, WhereSelectFun<T, Object>> parseQueryWhere(T model) {
        WhereSelectFun<T, Object> where = this.selectFun.colAll().where();
        AtomicBoolean isExecute = parseWhere(model, where);
        return Pair.of(isExecute.get(), where);
    }

    private List<T> queryByParam(Map<String, Object> param, int pageNum, int pageSize, boolean isPage) {
        if (param != null && param.size() > 0) {
            WhereSelectFun<T, Object> where = selectFun.colAll().where();
            AtomicBoolean status = new AtomicBoolean(false);
            param.forEach((colName, value) -> {
                Map<String, Object> param_ = new HashMap<>();
                String newColNameTag = getNewColNameTag();
                param_.put(newColNameTag, value);
                where.condSQL(colName + " in ( #{" + newColNameTag + "} ) ", param_);
                status.set(true);
            });
            if (status.get()) {
                if (isPage) {
                    where.limitPage(pageNum, pageSize);
                }
                List<T> exs = where.exs();
                if (exs != null) {
                    return exs;
                }
            }
        }
        return Collections.EMPTY_LIST;
    }

    private String getNewColNameTag() {
        return ato.getAndIncrement() + "_G_";
    }

    private TableInfo getTableInfo(Class modelClass) {
        TableInfo tableInfo = TableHelper.getTableInfo(modelClass);
        Assert.isTrue(tableInfo != null, "The table information corresponding to this class cannot be queried");
        return tableInfo;
    }

    private AtomicBoolean parseWhere(T model, WhereFun where) {
        AtomicBoolean status = new AtomicBoolean(false);
        Map<String, Object> colNameAndColValue = ColNameAndColValueUtils.parseColNameAndColValue(model, this.mClass);
        if(MapUtils.isNotEmpty(colNameAndColValue)){
            status.set(true);
            colNameAndColValue.forEach((colName, colValue)->{
                HashMap<String, Object> param = new HashMap<>();
                String newColNameTag = getNewColNameTag();
                param.putIfAbsent(newColNameTag, colValue);
                where.condSQL(colName + " in ( #{" + newColNameTag + "} ) ", param);
            });
        }
        return status;
    }

    private void parseWhereById(WhereModifyFun<T, V> where, AtomicBoolean status, T model) {
        Map<String, Object> coNameAndColValue = ColNameAndColValueUtils.parseUniqueCoNameAndUniqueColValue(model, mClass);
        if(MapUtils.isNotEmpty(coNameAndColValue)){
            status.set(true);
            coNameAndColValue.forEach((uniqueColName, uniqueColValue)->{
                Map<String, Object> param = new HashMap<>();
                String newColNameTag = getNewColNameTag();
                param.put(newColNameTag, uniqueColValue);
                where.condSQL(uniqueColName + " in ( #{" + newColNameTag + "} ) ", param);
            });
        }
    }

    private long getNumber(Field field, T ex) {
        if (ex == null) {
            return 0;
        }
        Object o = null;
        try {
            o = field.get(ex);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        } else {
            return Long.parseLong(o.toString());
        }
    }

    private int renovateBatchById(Collection<T> models, boolean updateNull) {
        if (models == null || models.size() == 0) {
            return 0;
        }
        models = models.stream().filter(Objects::nonNull).collect(Collectors.toList());
        WhereModifyFun<T, V> where = null;
        AtomicBoolean status = new AtomicBoolean(false);
        SmartUpdateFun<T, C, V> npdateNull = updateNull ? this.updateFun.updateNull() : this.updateFun.npdateNull();
        int i = 0;
        for (T model : models) {
            if (i == 0) {
                where = npdateNull.colAll(model).where();
            } else {
                where.addBatch().colAll(model).where();
                //Where participation should be consistent otherwise it will affect batch processing
                ato = new AtomicInteger(0);
            }
            i++;
            AtomicBoolean status_ = new AtomicBoolean(false);
            parseWhereById(where, status_, model);
            if (!status_.get()) {
                //This statement should be ignored and should not execute successfully
                where.condSQL(" 1 = 2 ");
            } else {
                status.set(true);
            }
        }
        if (status.get()) {
            return where.ex().intValue();
        }
        return 0;
    }

    //Is not  batch
    private int renovateByIdWithVersion(Collection<T> models, boolean updateNull) {
        if (models == null || models.size() == 0) {
            return 0;
        }

        DeriveInfo deriveColName = tableInfo.getDeriveColName(DeriveFlag.VERSION);
        if (deriveColName == null) {
            throw new ParseParamException(this.mClass.getName() + " no version field or col name");
        }
        Field versionField = deriveColName.getField();
        String versionColName = deriveColName.getColName();
        int modifyCount = 0;

        for (T model : models) {
            if (model == null) {
                continue;
            }
            SmartUpdateFun<T, C, V> npdateNull = updateNull ? this.updateFun.updateNull() : this.updateFun.npdateNull();
            boolean versionIsNull = false;
            Version version = null;
            try {
                version = (Version) versionField.get(model);
                // If the version does not exist that version of the current version is 0,
                // the update is successful after the latest version of 1
                if(version == null){
                    version = new Version(0);
                    versionField.set(model, version);
                    versionIsNull = true;
                }
            } catch (Exception e) {
                throw new ParseParamException("Parsing the version information failure");
            }


            WhereModifyFun<T, V> where = npdateNull.colAll(model).where();
            AtomicBoolean status_ = new AtomicBoolean(false);
            parseWhereById(where, status_, model);
            if (status_.get()) {
                if (versionIsNull) {
                    where.condSQL(tableName + "." + versionColName + " is null");
                } else {
                    where.condSQL(tableName + "." + versionColName + " in (" + version.get() + ")");
                }
                //After the success of the updated version plus one automatically.
                version.incrementAndGet();
                int i1 = where.ex().intValue();
                if (i1 == 0) {
                    //Update failed restore version number.
                    version.decrementAndGet();
                } else {
                    modifyCount = modifyCount + i1;
                }


            }
        }

        return modifyCount;
    }

    private T getLogicRemoveModel() {
        try {
            T newModel = (T) mClass.newInstance();
            TableInfo tableInfo = TableHelper.getTableInfo(mClass);
            DeriveInfo deriveColName = tableInfo.getDeriveColName(DeriveFlag.IS_DEL);
            Field logicRemoveField = null;
            Assert.isTrue(deriveColName != null && (logicRemoveField = deriveColName.getField()) != null,
                    "no  logic del col, please use IsDel to declaration");
            if (deriveColName.isDelField()) {
                logicRemoveField.set(newModel, IsDel.YES);
            } else {
                logicRemoveField.set(newModel, RowStatus.ABSENT);
            }
            return newModel;
        } catch (Exception e) {
            throw new UpdateFieldsException(e.getMessage());
        }
    }

    /**
     * Primary key must exist.
     */
    private void assertPrimary(){
        if(this.primaryColNmae == null || this.primaryField == null){
            throw new PrimaryKeyNotFoundException(this.tableName + " not primary key ");
        }
    }

    private List<T> saveWithFails(Collection<T> models) {
        List<T> updateList = Lists.newArrayList();
        for(T model : models){
            try {
                this.save(model);
            }catch (Exception e){
                if(e.getMessage()!= null && e.getMessage().toLowerCase().contains("duplicate")){
                    updateList.add(model);
                }else{
                    throw e;
                }
            }
        }
        return updateList;
    }

}
