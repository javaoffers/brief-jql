package com.javaoffers.base.modelhelper.sample.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaoffers.base.modelhelper.sample.spring.mapper.CrudUserMapper;
import com.javaoffers.base.modelhelper.sample.spring.model.User;
import com.javaoffers.base.modelhelper.sample.spring.model.UserOrder;
import com.javaoffers.batis.modelhelper.fun.GetterFun;
import com.javaoffers.batis.modelhelper.fun.crud.JoinFun;
import com.javaoffers.batis.modelhelper.fun.crud.OnFun;
import com.javaoffers.batis.modelhelper.fun.crud.SelectFun;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.io.Serializable;

@SpringBootApplication
@RequestMapping
@MapperScan("com.javaoffers.base.modelhelper.sample.spring.mapper")
public class SpringSuportCrudUserMapper implements InitializingBean {

    ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    CrudUserMapper crudUserMapper;

    public static void main(String[] args) {
        SpringApplication.run(SpringSuportCrudUserMapper.class, args);

    }

    @Override
    public void afterPropertiesSet() throws Exception {

        /**
         * 查询指定的字段
         */
        User exm = crudUserMapper.select().col(User::getId).where().ex();

        System.out.println("-------------------------------");

        /**
         * 带有条件的查询
         */
        exm = crudUserMapper
                .select()
                .col(User::getId)
                .col(User::getBirthday)
                .where()
                .ex();

        System.out.println("-------------------------------");

        /**
         * 查询所有字段
         */
        crudUserMapper.select().colAll().where().ex();

        System.out.println("-------------------------------");

        /**
         * left join 查询。 只支持 两张表 left join
         */
        User ex = crudUserMapper.select()
                .colAll()
                .leftJoin(UserOrder::new)
                .colAll()
                .on()
                .oeq(User::getId, UserOrder::getUserId)
                .eq(UserOrder::getIsDel, 1)
                .where()
                .eq(User::getId, 1)
                .or()
                .eq(User::getId,2)
                .ex();


        System.exit(0);
    }
}