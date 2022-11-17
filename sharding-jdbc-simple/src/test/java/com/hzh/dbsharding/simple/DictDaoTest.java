package com.hzh.dbsharding.simple;

import com.hzh.dbsharding.simple.dao.DictDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @NAME: DictDaoTest
 * @USER: DaHuangGO
 * @DATE: 2022/11/17
 * @TIME: 15:02
 * @YEAR: 2022
 * @MONTH: 11
 * @DAY: 17
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ShardingJdbcSimpleBootstrap.class})
public class DictDaoTest {

    @Autowired
    private DictDao dictDao;

    @Test
    public void testInsertDict(){
        dictDao.insertDict(1L,"user_type","0","管理员");
        dictDao.insertDict(2L,"user_type","1","操作员");
    }
    @Test
    public void testDeleteDict(){
        dictDao.deleteDict(1L);
        dictDao.deleteDict(2L);
    }

}
