package com.hzh.dbsharding.simple;

import com.hzh.dbsharding.simple.dao.UserDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * @NAME: UserDaoTest
 * @USER: DaHuangGO
 * @DATE: 2022/11/17
 * @TIME: 14:52
 * @YEAR: 2022
 * @MONTH: 11
 * @DAY: 17
 */
@SpringBootTest(classes = {ShardingJdbcSimpleBootstrap.class})
@RunWith(SpringRunner.class)
public class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    public void testInsertUser(){
        for (int i = 0 ; i<10; i++){
            Long id = i + 1L;
            userDao.insertUser(id,"姓名"+ id );
        }
    }
    @Test
    public void testSelectUserbyIds(){
        List<Long> userIds = new ArrayList<>();
        userIds.add(1L);
        userIds.add(2L);
        List<Map> users = userDao.selectUserbyIds(userIds);
        System.out.println(users);
    }


    @Test
    public void testSelectUserInfobyIds(){
        List<Long> userIds = new ArrayList<>();
        userIds.add(1L);
        userIds.add(2L);
        List<Map> users = userDao.selectUserInfobyIds(userIds);
        JSONArray jsonUsers = new JSONArray(users);
        System.out.println(jsonUsers);
    }


}
