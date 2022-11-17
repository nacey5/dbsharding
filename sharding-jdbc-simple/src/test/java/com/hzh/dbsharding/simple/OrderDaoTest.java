package com.hzh.dbsharding.simple;

import com.hzh.dbsharding.simple.dao.OrderDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @version 1.0
 **/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ShardingJdbcSimpleBootstrap.class})
public class OrderDaoTest {

    @Autowired
    OrderDao orderDao;

    @Test
    public void testInsertOrder(){
        for(int i=1;i<20;i++){
            orderDao.insertOrder(new BigDecimal(i),Long.valueOf(i),"SUCCESS");
        }
    }

    @Test
    public void testSelectOrderbyIds(){
        List<Long> ids = new ArrayList<>();
//        ids.add(799685717457371136L);
        ids.add(799685717524480001L);

        List<Map> maps = orderDao.selectOrderbyIds(ids);
        System.out.println(maps);
    }
}
