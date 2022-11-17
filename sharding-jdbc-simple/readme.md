# 分库分表

![image.png](https://note.youdao.com/yws/res/2022/WEBRESOURCE7c03d26462125c94a82659799a15b9d7)

## 垂直分表

![image.png](https://note.youdao.com/yws/res/2028/WEBRESOURCE950bc34ff34b91f749050bb9ff87b18e)\
![image.png](https://note.youdao.com/yws/res/2031/WEBRESOURCEa5dff1c30a45c7b89e6d6e9afc3a36c7)

## 垂直分库

![image.png](https://note.youdao.com/yws/res/2034/WEBRESOURCEeab2176a24cf5916700226c6d5c050d0)

## 水平分库

![image.png](https://note.youdao.com/yws/res/2039/WEBRESOURCE41b52548dbecb681900d744941372e70)
![image.png](https://note.youdao.com/yws/res/2041/WEBRESOURCE7b160b58bb4cec84e11f921df3ca6cf6)

## 水平分表

![image.png](https://note.youdao.com/yws/res/2048/WEBRESOURCEecaf4cd9f5e7fa0e45058c7dbd143488)

## 小结

本章介绍了分库分表的各种方式，它们分别是垂直分表、垂直分库、水平分库和水平分表：
垂直分表：可以把一个宽表的字段按访问频次、是否是大字段的原则拆分为多个表，这样既能使业务清晰，还能提
升部分性能。拆分后，尽量从业务角度避免联查，否则性能方面将得不偿失。
垂直分库：可以把多个表按业务耦合松紧归类，分别存放在不同的库，这些库可以分布在不同服务器，从而使访问
压力被多服务器负载，大大提升性能，同时能提高整体架构的业务清晰度，不同的业务库可根据自身情况定制优化
方案。但是它需要解决跨库带来的所有复杂问题。
水平分库：可以把一个表的数据(按数据行)分到多个不同的库，每个库只有这个表的部分数据，这些库可以分布在
不同服务器，从而使访问压力被多服务器负载，大大提升性能。它不仅需要解决跨库带来的所有复杂问题，还要解
决数据路由的问题(数据路由问题后边介绍)。
水平分表：可以把一个表的数据(按数据行)分到多个同一个数据库的多张表中，每个表只有这个表的部分数据，这
样做能小幅提升性能，它仅仅作为水平分库的一个补充优化。
一般来说，在系统设计阶段就应该根据业务耦合松紧来确定垂直分库，垂直分表方案，在数据量及访问压力不是特
别大的情况，首先考虑缓存、读写分离、索引技术等方案。若数据量极大，且持续增长，再考虑水平分库水平分表
方案。

## 分库分表带来的问题

分库分表能有效的缓解了单机和单库带来的性能瓶颈和压力，突破网络IO、硬件资源、连接数的瓶颈，同时也带来了一些问题。

### 一致性问题

由于分库分表把数据分布在不同库甚至不同服务器，不可避免会带来分布式事务问题。

### 跨节点关联查询

![image.png](https://note.youdao.com/yws/res/2060/WEBRESOURCE1ac952bbdf8611759cb50cde8d278c7a)

### 跨节点分页，排序函数

跨节点多库进行查询时，limit分页、order by排序等问题，就变得比较复杂了。需要先在不同的分片节点中将数据
进行排序并返回，然后将不同分片返回的结果集进行汇总和再次排序。
如，进行水平分库后的商品库，按ID倒序排序分页，取第一页：
![image.png](https://note.youdao.com/yws/res/2069/WEBRESOURCEec9c2abee4f39fb64a6c3fc560a6967c)
以上流程是取第一页的数据，性能影响不大，但由于商品信息的分布在各数据库的数据可能是随机的，如果是取第
N页，需要将所有节点前N页数据都取出来合并，再进行整体的排序，操作效率可想而知。所以请求页数越大，系
统的性能也会越差。
在使用Max、Min、Sum、Count之类的函数进行计算的时候，与排序分页同理，也需要先在每个分片上执行相应
的函数，然后将各个分片的结果集进行汇总和再次计算，最终将结果返回。

### 主键避重

在分库分表环境中，由于表中数据同时存在不同数据库中，主键值平时使用的自增长将无用武之地，某个分区数据
库生成的ID无法保证全局唯一。因此需要单独设计全局主键，以避免跨库主键重复问题。
![image.png](https://note.youdao.com/yws/res/2074/WEBRESOURCEe5c962155c00706131b4ebcf9a92f33d)

### 公共表

实际的应用场景中，参数表、数据字典表等都是数据量较小，变动少，而且属于高频联合查询的依赖表。例子中地理区域表也属于此类型。
可以将这类表在每个数据库都保存一份，所有对公共表的更新操作都同时发送到所有分库执行。
由于分库分表之后，数据被分散在不同的数据库、服务器。因此，对数据的操作也就无法通过常规方式完成，并且它还带来了一系列的问题。好在，这些问题不是所有都需要我们在应用层面上解决，市面上有很多中间件可供我们选择，其中Sharding-JDBC使用流行度较高，我们来了解一下它。

# Sharding-JDBC

## 介绍

Sharding-JDBC是当当网研发的开源分布式数据库中间件，从 3.0 开始Sharding-JDBC被包含在Sharding-Sphere中，之后该项目进入进入Apache孵化器，4.0版本之后的版本为Apache版本。
ShardingSphere是一套开源的分布式数据库中间件解决方案组成的生态圈，它由Sharding-JDBC、ShardingProxy和Sharding-Sidecar（计划中）这3款相互独立的产品组成。 他们均提供标准化的数据分片、分布式事务和
数据库治理功能，可适用于如Java同构、异构语言、容器、云原生等各种多样化的应用场。
官方地址：<https://shardingsphere.apache.org/document/current/cn/overview/>
咱们目前只需关注Sharding-JDBC，它定位为轻量级Java框架，在Java的JDBC层提供的额外服务。 它使用客户端直连数据库，以jar包形式提供服务，无需额外部署和依赖，可理解为增强版的JDBC驱动，完全兼容JDBC和各种
ORM框架。
Sharding-JDBC的核心功能为数据分片和读写分离，通过Sharding-JDBC，应用可以透明的使用jdbc访问已经分库分表、读写分离的多个数据源，而不用关心数据源的数量以及数据如何分布。

*   适用于任何基于Java的ORM框架，如： Hibernate, Mybatis, Spring JDBC Template或直接使用JDBC。
*   基于任何第三方的数据库连接池，如：DBCP, C3P0, BoneCP, Druid, HikariCP等。
*   支持任意实现JDBC规范的数据库。目前支持MySQL，Oracle，SQLServer和PostgreSQL。
    ![image.png](https://note.youdao.com/yws/res/2092/WEBRESOURCE16418f21bfddc646ddf01e5e15ba7907)\
    上图展示了Sharding-Jdbc的工作方式，使用Sharding-Jdbc前需要人工对数据库进行分库分表，在应用程序中加入
    Sharding-Jdbc的Jar包，应用程序通过Sharding-Jdbc操作分库分表后的数据库和数据表，由于Sharding-Jdbc是对
    Jdbc驱动的增强，使用Sharding-Jdbc就像使用Jdbc驱动一样，在应用程序中是无需指定具体要操作的分库和分表
    的。

## 与jdbc性能对比

1.  性能损耗测试：服务器资源充足、并发数相同，比较JDBC和Sharding-JDBC性能损耗，Sharding-JDBC相对JDBC损耗不超过7%。
    ![image.png](https://note.youdao.com/yws/res/2102/WEBRESOURCE02e1d797734f4fe69fb2833b398f6013)
2.  性能对比测试：服务器资源使用到极限，相同的场景JDBC与Sharding-JDBC的吞吐量相。
3.  性能对比测试：服务器资源使用到极限，Sharding-JDBC采用分库分表后，Sharding-JDBC吞吐量较JDBC不分表有接近2倍的提升。
    ![image.png](https://note.youdao.com/yws/res/2107/WEBRESOURCEfcc249b56cf0b4e9badecf64520bc50e)

## shardingjdbc使用

### 分片规则

```properties
server.port=56081
spring.application.name = sharding‐jdbc‐simple‐demo
server.servlet.context‐path = /sharding‐jdbc‐simple‐demo
spring.http.encoding.enabled = true
spring.http.encoding.charset = UTF‐8
spring.http.encoding.force = true
spring.main.allow‐bean‐definition‐overriding = true
mybatis.configuration.map‐underscore‐to‐camel‐case = true
# 以下是分片规则配置
# 定义数据源
spring.shardingsphere.datasource.names = m1
spring.shardingsphere.datasource.m1.type = com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.m1.driver‐class‐name = com.mysql.jdbc.Driver
spring.shardingsphere.datasource.m1.url = jdbc:mysql://localhost:3306/order_db?useUnicode=true
spring.shardingsphere.datasource.m1.username = root
spring.shardingsphere.datasource.m1.password = root
# 指定t_order表的数据分布情况，配置数据节点
spring.shardingsphere.sharding.tables.t_order.actual‐data‐nodes = m1.t_order_$‐>{1..2}
# 指定t_order表的主键生成策略为SNOWFLAKE
spring.shardingsphere.sharding.tables.t_order.key‐generator.column=order_id
spring.shardingsphere.sharding.tables.t_order.key‐generator.type=SNOWFLAKE
# 指定t_order表的分片策略，分片策略包括分片键和分片算法
spring.shardingsphere.sharding.tables.t_order.table‐strategy.inline.sharding‐column = order_id
spring.shardingsphere.sharding.tables.t_order.table‐strategy.inline.algorithm‐expression =
t_order_$‐>{order_id % 2 + 1}
# 打开sql输出日志
spring.shardingsphere.props.sql.show = true
swagger.enable = true
logging.level.root = info
logging.level.org.springframework.web = info
logging.level.com.itheima.dbsharding = debug
logging.level.druid.sql = debug
```

1.首先定义数据源m1，并对m1进行实际的参数配置。
2.指定t\_order表的数据分布情况，他分布在m1.t\_order\_1，m1.t\_order\_2
3.指定t\_order表的主键生成策略为SNOWFLAKE，SNOWFLAKE是一种分布式自增算法，保证id全局唯一
4.定义t\_order分片策略，order\_id为偶数的数据落在t\_order\_1，为奇数的落在t\_order\_2，分表策略的表达式为
t\_order\_\$->{order\_id % 2 + 1}

## 执行流程

通过日志分析，Sharding-JDBC在拿到用户要执行的sql之后干了哪些事儿：
（1）解析sql，获取片键值，在本例中是order\_id
（2）Sharding-JDBC通过规则配置 t\_order\_\$->{order\_id % 2 + 1}，知道了当order\_id为偶数时，应该往
t\_order\_1表插数据，为奇数时，往t\_order\_2插数据。
（3）于是Sharding-JDBC根据order\_id的值改写sql语句，改写后的SQL语句是真实所要执行的SQL语句。
（4）执行改写后的真实sql语句
（5）将所有真正执行sql的结果进行汇总合并，返回。

## Sharding JDBC 执行原理

### 基本概念

在了解Sharding-JDBC的执行原理前，需要了解以下概念：

### 逻辑表

水平拆分的数据表的总称。例：订单数据表根据主键尾数拆分为10张表，分别是 t\_order\_0 、 t\_order\_1 到t\_order\_9 ，他们的逻辑表名为 t\_order。

### 真实表

在分片的数据库中真实存在的物理表。即上个示例中的 t\_order\_0 到t\_order\_9 。

### 数据节点

数据分片的最小物理单元。由数据源名称和数据表组成，例： ds\_0.t\_order\_0 。

### 绑定表

指分片规则一致的主表和子表。例如： t\_order 表和 t\_order\_item表，均按照 order\_id 分片,绑定表之间的分区键完全相同，则此两张表互为绑定表关系。绑定表之间的多表关联查询不会出现笛卡尔积关联，关联查询效率将大大提升。举例说明，如果SQL为：

```sql
SELECT i.* FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id WHERE o.order_id in (10,11);
```

在不配置绑定表关系时，假设分片键 order\_id将数值10路由至第0片，将数值11路由至第1片，那么路由后的SQL应该为4条，它们呈现为笛卡尔积：

```sql
SELECT i.* FROM t_order_0 o JOIN t_order_item_0 i ON o.order_id=i.order_id WHERE o.order_id in
(10, 11);
SELECT i.* FROM t_order_0 o JOIN t_order_item_1 i ON o.order_id=i.order_id WHERE o.order_id in
(10, 11);
SELECT i.* FROM t_order_1 o JOIN t_order_item_0 i ON o.order_id=i.order_id WHERE o.order_id in
(10, 11);
SELECT i.* FROM t_order_1 o JOIN t_order_item_1 i ON o.order_id=i.order_id WHERE o.order_id in
(10, 11);
```

在配置绑定表关系后，路由的SQL应该为2条：

```sql
SELECT i.* FROM t_order_0 o JOIN t_order_item_0 i ON o.order_id=i.order_id WHERE o.order_id in
(10, 11);
SELECT i.* FROM t_order_1 o JOIN t_order_item_1 i ON o.order_id=i.order_id WHERE o.order_id in
(10, 11);
```

### 广播表

指所有的分片数据源中都存在的表，表结构和表中的数据在每个数据库中均完全一致。适用于数据量不大且需要与海量数据的表进行关联查询的场景，例如：字典表。

### 分片键

用于分片的数据库字段，是将数据库(表)水平拆分的关键字段。例：将订单表中的订单主键的尾数取模分片，则订单主键为分片字段。SQL中如果无分片字段，将执行全路由，性能较差。除了对单分片字段的支持，ShardingJdbc也支持根据多个字段进行分片。

### 分片算法

通过分片算法将数据分片，支持通过 = 、 BETWEEN 和 IN分片。分片算法需要应用方开发者自行实现，可实现的灵活度非常高。包括：精确分片算法 、范围分片算法 ，复合分片算法 等。例如：where order\_id = ? 将采用精确分片算法，where order\_id in (?,?,?)将采用精确分片算法，where order\_id BETWEEN ? and ? 将采用范围分片算法，复合分片算法用于分片键有多个复杂情况。

### 分片策略

包含分片键和分片算法，由于分片算法的独立性，将其独立抽离。真正可用于分片操作的是分片键 + 分片算法，也
就是分片策略。内置的分片策略大致可分为尾数取模、哈希、范围、标签、时间等。由用户方配置的分片策略则更
加灵活，常用的使用行表达式配置分片策略，它采用Groovy表达式表示，如: t\_user\_\$->{u\_id % 8} 表示t\_user表根据u\_id模8，而分成8张表，表名称为 t\_user\_0 到 t\_user\_7 。

### 自增主键生成策略

通过在客户端生成自增主键替换以数据库原生自增主键的方式，做到分布式主键无重复。

## sql解析

![image.png](https://note.youdao.com/yws/res/2170/WEBRESOURCEc242fa2c7d3bbca6749a5961e40b15ec)
![image.png](https://note.youdao.com/yws/res/2172/WEBRESOURCE3c0faf038e4a604104cc719d78c599c7)
![image.png](https://note.youdao.com/yws/res/2174/WEBRESOURCE656f15e5d9399db410a7a91497d8d8d3)
为了便于理解，抽象语法树中的关键字的Token用绿色表示，变量的Token用红色表示，灰色表示需要进一步拆分。
最后，通过对抽象语法树的遍历去提炼分片所需的上下文，并标记有可能需要SQL改写(后边介绍)的位置。 供分片使用的解析上下文包含查询选择项（Select Items）、表信息（Table）、分片条件（Sharding Condition）、自增
主键信息（Auto increment Primary Key）、排序信息（Order By）、分组信息（Group By）以及分页信息（Limit、Rownum、Top）。

## sql路由

SQL路由就是把针对逻辑表的数据操作映射到对数据结点操作的过程。
根据解析上下文匹配数据库和表的分片策略，并生成路由路径。 对于携带分片键的SQL，根据分片键操作符不同可以划分为单片路由(分片键的操作符是等号)、多片路由(分片键的操作符是IN)和范围路由(分片键的操作符是BETWEEN)，不携带分片键的SQL则采用广播路由。根据分片键进行路由的场景可分为直接路由、标准路由、笛卡尔路由等。

### 标准路由

标准路由是Sharding-Jdbc最为推荐使用的分片方式，它的适用范围是不包含关联查询或仅包含绑定表之间关联查
询的SQL。 当分片运算符是等于号时，路由结果将落入单库（表），当分片运算符是BETWEEN或IN时，则路由结
果不一定落入唯一的库（表），因此一条逻辑SQL最终可能被拆分为多条用于执行的真实SQL。 举例说明，如果按
照 order\_id 的奇数和偶数进行数据分片，一个单表查询的SQL如下：

```sql
SELECT * FROM t_order WHERE order_id IN (1, 2);
```

那么路由的结果应为：

```sql
SELECT * FROM t_order_0 WHERE order_id IN (1, 2);
SELECT * FROM t_order_1 WHERE order_id IN (1, 2);
```

绑定表的关联查询与单表查询复杂度和性能相当。举例说明，如果一个包含绑定表的关联查询的SQL如下：

```sql
SELECT * FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id WHERE order_id IN (1, 2);
```

那么路由的结果应为：

```sql
SELECT * FROM t_order_0 o JOIN t_order_item_0 i ON o.order_id=i.order_id WHERE order_id IN (1,
2);
SELECT * FROM t_order_1 o JOIN t_order_item_1 i ON o.order_id=i.order_id WHERE order_id IN (1,
2);
```

可以看到，SQL拆分的数目与单表是一致的。

### 笛卡尔路由

笛卡尔路由是最复杂的情况，它无法根据绑定表的关系定位分片规则，因此非绑定表之间的关联查询需要拆解为笛
卡尔积组合执行。 如果上个示例中的SQL并未配置绑定表关系，那么路由的结果应为：

```sql
SELECT * FROM t_order_0 o JOIN t_order_item_0 i ON o.order_id=i.order_id WHERE order_id IN (1,
2);
SELECT * FROM t_order_0 o JOIN t_order_item_1 i ON o.order_id=i.order_id WHERE order_id IN (1,
2);
SELECT * FROM t_order_1 o JOIN t_order_item_0 i ON o.order_id=i.order_id WHERE order_id IN (1,
2);
SELECT * FROM t_order_1 o JOIN t_order_item_1 i ON o.order_id=i.order_id WHERE order_id IN (1,
2);
```

笛卡尔路由查询性能较低，需谨慎使用。

### 全库表路由

对于不携带分片键的SQL，则采取广播路由的方式。根据SQL类型又可以划分为全库表路由、全库路由、全实例路
由、单播路由和阻断路由这5种类型。其中全库表路由用于处理对数据库中与其逻辑表相关的所有真实表的操作，
主要包括不带分片键的DQL(数据查询)和DML（数据操纵），以及DDL（数据定义）等。例如：

```sql
SELECT * FROM t_order WHERE good_prority IN (1, 10);
```

则会遍历所有数据库中的所有表，逐一匹配逻辑表和真实表名，能够匹配得上则执行。路由后成为

```sql
SELECT * FROM t_order_0 WHERE good_prority IN (1, 10);
SELECT * FROM t_order_1 WHERE good_prority IN (1, 10);
SELECT * FROM t_order_2 WHERE good_prority IN (1, 10);
SELECT * FROM t_order_3 WHERE good_prority IN (1, 10);
```

## sql改写

工程师面向逻辑表书写的SQL，并不能够直接在真实的数据库中执行，SQL改写用于将逻辑SQL改写为在真实数据库中可以正确执行的SQL。
如一个简单的例子，若逻辑SQL为：

```sql
SELECT order_id FROM t_order WHERE order_id=1;
```

假设该SQL配置分片键order\_id，并且order\_id=1的情况，将路由至分片表1。那么改写之后的SQL应该为：

```sql
SELECT order_id FROM t_order_1 WHERE order_id=1;
```

再比如，Sharding-JDBC需要在结果归并时获取相应数据，但该数据并未能通过查询的SQL返回。 这种情况主要是
针对GROUP BY和ORDER BY。结果归并时，需要根据 GROUP BY 和 ORDER BY 的字段项进行分组和排序，但如果原
始SQL的选择项中若并未包含分组项或排序项，则需要对原始SQL进行改写。 先看一下原始SQL中带有结果归并所
需信息的场景：

```sql
SELECT order_id, user_id FROM t_order ORDER BY user_id;
```

由于使用user\_id进行排序，在结果归并中需要能够获取到user\_id的数据，而上面的SQL是能够获取到user\_id数据
的，因此无需补列。
如果选择项中不包含结果归并时所需的列，则需要进行补列，如以下SQL：

```sql
SELECT order_id FROM t_order ORDER BY user_id;
```

由于原始SQL中并不包含需要在结果归并中需要获取的user\_id，因此需要对SQL进行补列改写。补列之后的SQL
是：

```sql
SELECT order_id, user_id AS ORDER_BY_DERIVED_0 FROM t_order ORDER BY user_id;
```

## sql执行

Sharding-JDBC采用一套自动化的执行引擎，负责将路由和改写完成之后的真实SQL安全且高效发送到底层数据源
执行。 它不是简单地将SQL通过JDBC直接发送至数据源执行；也并非直接将执行请求放入线程池去并发执行。它
更关注平衡数据源连接创建以及内存占用所产生的消耗，以及最大限度地合理利用并发等问题。 执行引擎的目标是
自动化的平衡资源控制与执行效率，他能在以下两种模式自适应切换：

### 内存限制模式

使用此模式的前提是，Sharding-JDBC对一次操作所耗费的数据库连接数量不做限制。 如果实际执行的SQL需要对
某数据库实例中的200张表做操作，则对每张表创建一个新的数据库连接，并通过多线程的方式并发处理，以达成
执行效率最大化。

### 连接限制模式

使用此模式的前提是，Sharding-JDBC严格控制对一次操作所耗费的数据库连接数量。 如果实际执行的SQL需要对
某数据库实例中的200张表做操作，那么只会创建唯一的数据库连接，并对其200张表串行处理。 如果一次操作中
的分片散落在不同的数据库，仍然采用多线程处理对不同库的操作，但每个库的每次操作仍然只创建一个唯一的数
据库连接。
内存限制模式适用于OLAP操作，可以通过放宽对数据库连接的限制提升系统吞吐量； 连接限制模式适用于OLTP操
作，OLTP通常带有分片键，会路由到单一的分片，因此严格控制数据库连接，以保证在线系统数据库资源能够被
更多的应用所使用，是明智的选择。

## 结果将从各个数据节点获取的多数据结果集，组合成为一个结果集并正确的返回至请求客户端，称为结果归并。
Sharding-JDBC支持的结果归并从功能上可分为遍历、排序、分组、分页和聚合5种类型，它们是组合而非互斥的
关系。
归并引擎的整体结构划分如下图。

结果归并从结构划分可分为流式归并、内存归并和装饰者归并。流式归并和内存归并是互斥的，装饰者归并可以在
流式归并和内存归并之上做进一步的处理。
内存归并很容易理解，他是将所有分片结果集的数据都遍历并存储在内存中，再通过统一的分组、排序以及聚合等
计算之后，再将其封装成为逐条访问的数据结果集返回。
流式归并是指每一次从数据库结果集中获取到的数据，都能够通过游标逐条获取的方式返回正确的单条数据，它与
数据库原生的返回结果集的方式最为契合。
下边举例说明排序归并的过程，如下图是一个通过分数进行排序的示例图，它采用流式归并方式。 图中展示了3张
表返回的数据结果集，每个数据结果集已经根据分数排序完毕，但是3个数据结果集之间是无序的。 将3个数据结
果集的当前游标指向的数据值进行排序，并放入优先级队列，t_score_0的第一个数据值最大，t_score_2的第一个
数据值次之，t_score_1的第一个数据值最小，因此优先级队列根据t_score_0，t_score_2和t_score_1的方式排序
队列。

下图则展现了进行next调用的时候，排序归并是如何进行的。 通过图中我们可以看到，当进行第一次next调用
时，排在队列首位的t_score_0将会被弹出队列，并且将当前游标指向的数据值（也就是100）返回至查询客户端，
并且将游标下移一位之后，重新放入优先级队列。 而优先级队列也会根据t_score_0的当前数据结果集指向游标的
数据值（这里是90）进行排序，根据当前数值，t_score_0排列在队列的最后一位。 之前队列中排名第二的
t_score_2的数据结果集则自动排在了队列首位。
在进行第二次next时，只需要将目前排列在队列首位的t_score_2弹出队列，并且将其数据结果集游标指向的值返
回至客户端，并下移游标，继续加入队列排队，以此类推。 当一个结果集中已经没有数据了，则无需再次加入队
列。

可以看到，对于每个数据结果集中的数据有序，而多数据结果集整体无序的情况下，Sharding-JDBC无需将所有的
数据都加载至内存即可排序。 它使用的是流式归并的方式，每次next仅获取唯一正确的一条数据，极大的节省了
内存的消耗。
装饰者归并是对所有的结果集归并进行统一的功能增强，比如归并时需要聚合SUM前，在进行聚合计算前，都会通
过内存归并或流式归并查询出结果集。因此，聚合归并是在之前介绍的归并类型之上追加的归并能力，即装饰者模
式。
## 归并
将从各个数据节点获取的多数据结果集，组合成为一个结果集并正确的返回至请求客户端，称为结果归并。
Sharding-JDBC支持的结果归并从功能上可分为遍历、排序、分组、分页和聚合5种类型，它们是组合而非互斥的
关系。
归并引擎的整体结构划分如下图。
![image.png](https://note.youdao.com/yws/res/2235/WEBRESOURCE0618dee8e2fb397329c938282a9c8db1)
结果归并从结构划分可分为流式归并、内存归并和装饰者归并。流式归并和内存归并是互斥的，装饰者归并可以在
流式归并和内存归并之上做进一步的处理。
内存归并很容易理解，他是将所有分片结果集的数据都遍历并存储在内存中，再通过统一的分组、排序以及聚合等
计算之后，再将其封装成为逐条访问的数据结果集返回。
流式归并是指每一次从数据库结果集中获取到的数据，都能够通过游标逐条获取的方式返回正确的单条数据，它与
数据库原生的返回结果集的方式最为契合。
下边举例说明排序归并的过程，如下图是一个通过分数进行排序的示例图，它采用流式归并方式。 图中展示了3张
表返回的数据结果集，每个数据结果集已经根据分数排序完毕，但是3个数据结果集之间是无序的。 将3个数据结
果集的当前游标指向的数据值进行排序，并放入优先级队列，t_score_0的第一个数据值最大，t_score_2的第一个
数据值次之，t_score_1的第一个数据值最小，因此优先级队列根据t_score_0，t_score_2和t_score_1的方式排序
队列。
![image.png](https://note.youdao.com/yws/res/2241/WEBRESOURCEf62042cbb59ea59c441339f4500b81b7)
下图则展现了进行next调用的时候，排序归并是如何进行的。 通过图中我们可以看到，当进行第一次next调用
时，排在队列首位的t_score_0将会被弹出队列，并且将当前游标指向的数据值（也就是100）返回至查询客户端，
并且将游标下移一位之后，重新放入优先级队列。 而优先级队列也会根据t_score_0的当前数据结果集指向游标的
数据值（这里是90）进行排序，根据当前数值，t_score_0排列在队列的最后一位。 之前队列中排名第二的
t_score_2的数据结果集则自动排在了队列首位。
在进行第二次next时，只需要将目前排列在队列首位的t_score_2弹出队列，并且将其数据结果集游标指向的值返
回至客户端，并下移游标，继续加入队列排队，以此类推。 当一个结果集中已经没有数据了，则无需再次加入队
列。
![image.png](https://note.youdao.com/yws/res/2244/WEBRESOURCEe4043254619e70583528db617a185e64)
可以看到，对于每个数据结果集中的数据有序，而多数据结果集整体无序的情况下，Sharding-JDBC无需将所有的
数据都加载至内存即可排序。 它使用的是流式归并的方式，每次next仅获取唯一正确的一条数据，极大的节省了
内存的消耗。
装饰者归并是对所有的结果集归并进行统一的功能增强，比如归并时需要聚合SUM前，在进行聚合计算前，都会通
过内存归并或流式归并查询出结果集。因此，聚合归并是在之前介绍的归并类型之上追加的归并能力，即装饰者模
式。
## 总结
通过以上内容介绍，相信大家已经了解到Sharding-JDBC基础概念、核心功能以及执行原理。
基础概念：`逻辑表，真实表，数据节点，绑定表，广播表，分片键，分片算法，分片策略，主键生成策略`
核心功能：`数据分片，读写分离`
执行流程： `SQL解析 => 查询优化 => SQL路由 => SQL改写 => SQL执行 => 结果归并`
## 水平分表
前面已经介绍过，水平分表是在同一个数据库内，把同一个表的数据按一定规则拆到多个表中。在快速入门里，我们已经对水平分库进行实现，这里不再重复介绍。
## 水平分库
前面已经介绍过，水平分库是把同一个表的数据按一定规则拆到不同的数据库中，每个库可以放在不同的服务器
上。接下来看一下如何使用Sharding-JDBC实现水平分库，咱们继续对快速入门中的例子进行完善。
(1)**将原有order_db库拆分为order_db_1、order_db_2**
![image.png](https://note.youdao.com/yws/res/2257/WEBRESOURCE452877c986384fc1b46a50b0c2912a75)
(2)**分片规则修改**
由于数据库拆分了两个，这里需要配置两个数据源。
分库需要配置分库的策略，和分表策略的意义类似，通过分库策略实现数据操作针对分库的数据库进行操作。
~~~properties
# 定义多个数据源
spring.shardingsphere.datasource.names = m1,m2
spring.shardingsphere.datasource.m1.type = com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.m1.driver‐class‐name = com.mysql.jdbc.Driver
spring.shardingsphere.datasource.m1.url = jdbc:mysql://localhost:3306/order_db_1?useUnicode=true
spring.shardingsphere.datasource.m1.username = root
spring.shardingsphere.datasource.m1.password = root
spring.shardingsphere.datasource.m2.type = com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.m2.driver‐class‐name = com.mysql.jdbc.Driver
spring.shardingsphere.datasource.m2.url = jdbc:mysql://localhost:3306/order_db_2?useUnicode=true
spring.shardingsphere.datasource.m2.username = root
spring.shardingsphere.datasource.m2.password = root
...
# 分库策略，以user_id为分片键，分片策略为user_id % 2 + 1，user_id为偶数操作m1数据源，否则操作m2。
spring.shardingsphere.sharding.tables.t_order.database‐strategy.inline.sharding‐column = user_id
spring.shardingsphere.sharding.tables.t_order.database‐strategy.inline.algorithm‐expression =m$‐>{user_id % 2 + 1}
~~~
**分库策略定义方式如下**:
~~~properties
#分库策略，如何将一个逻辑表映射到多个数据源
spring.shardingsphere.sharding.tables.<逻辑表名称>.database‐strategy.<分片策略>.<分片策略属性名>= #
分片策略属性值
#分表策略，如何将一个逻辑表映射为多个实际表
spring.shardingsphere.sharding.tables.<逻辑表名称>.table‐strategy.<分片策略>.<分片策略属性名>= #分
片策略属性值
~~~

Sharding-JDBC支持以下几种分片策略：
不管理分库还是分表，策略基本一样。
- standard：标准分片策略，对应StandardShardingStrategy。提供对SQL语句中的=, IN和BETWEEN AND的
分片操作支持。StandardShardingStrategy只支持单分片键，提供PreciseShardingAlgorithm和
RangeShardingAlgorithm两个分片算法。PreciseShardingAlgorithm是必选的，用于处理=和IN的分片。
RangeShardingAlgorithm是可选的，用于处理BETWEEN AND分片，如果不配置
RangeShardingAlgorithm，SQL中的BETWEEN AND将按照全库路由处理。
- complex：符合分片策略，对应ComplexShardingStrategy。复合分片策略。提供对SQL语句中的=, IN和
BETWEEN AND的分片操作支持。ComplexShardingStrategy支持多分片键，由于多分片键之间的关系复
杂，因此并未进行过多的封装，而是直接将分片键值组合以及分片操作符透传至分片算法，完全由应用开发
者实现，提供最大的灵活度。
- inline：行表达式分片策略，对应InlineShardingStrategy。使用Groovy的表达式，提供对SQL语句中的=和
IN的分片操作支持，只支持单分片键。对于简单的分片算法，可以通过简单的配置使用，从而避免繁琐的Java
代码开发，如: t_user_$->{u_id % 8} 表示t_user表根据u_id模8，而分成8张表，表名称为 t_user_0 到
t_user_7 。
- hint：Hint分片策略，对应HintShardingStrategy。通过Hint而非SQL解析的方式分片的策略。对于分片字段
非SQL决定，而由其他外置条件决定的场景，可使用SQL Hint灵活的注入分片字段。例：内部系统，按照员工
登录主键分库，而数据库中并无此字段。SQL Hint支持通过Java API和SQL注释(待实现)两种方式使用。
- none：不分片策略，对应NoneShardingStrategy。不分片的策略。
目前例子中都使用inline分片策略，若对其他分片策略细节若感兴趣，请查阅官方文档：
https://shardingsphere.apache.org
**(3)插入测试**
修改testInsertOrder方法，插入数据中包含不同的user_id
~~~java
@Test
public void testInsertOrder(){
    for (int i = 0 ; i<10; i++){
        orderDao.insertOrder(new BigDecimal((i+1)*5),1L,"WAIT_PAY");
    }
    for (int i = 0 ; i<10; i++){
        orderDao.insertOrder(new BigDecimal((i+1)*10),2L,"WAIT_PAY");
    }
}

~~~
执行testInsertOrder:
![image.png](https://note.youdao.com/yws/res/2290/WEBRESOURCE2cfe0e4af67a3172bccddddf670221a3)
通过日志可以看出，根据user_id的奇偶不同，数据分别落在了不同数据源，达到目标。
**（4）查询测试**
调用快速入门的查询接口进行测试：
~~~java
List<Map> selectOrderbyIds(@Param("orderIds")List<Long> orderIds)
~~~
通过日志发现，sharding-jdbc将sql路由到m1和m2：
![image.png](https://note.youdao.com/yws/res/2300/WEBRESOURCE55989345d881844309741a23fae580ef)
问题分析：
由于查询语句中并没有使用分片键user_id，所以sharding-jdbc将广播路由到每个数据结点。
下边我们在sql中添加分片键进行查询。
在OrderDao中定义接口：
~~~java
@Select({"<script>",
" select",
" * ",
" from t_order t ",
"where t.order_id in",
"<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>",
"#{id}",
"</foreach>",
" and t.user_id = #{userId} ",
"</script>"
})
List<Map> selectOrderbyUserAndIds(@Param("userId") Integer userId,@Param("orderIds")List<Long>
orderIds);
~~~
编写测试方法：
~~~java
@Test
public void testSelectOrderbyUserAndIds(){
List<Long> orderIds = new ArrayList<>();
orderIds.add(373422416644276224L);
orderIds.add(373422415830581248L);
//查询条件中包括分库的键user_id
int user_id = 1;
List<Map> orders = orderDao.selectOrderbyUserAndIds(user_id,orderIds);
JSONArray jsonOrders = new JSONArray(orders);
System.out.println(jsonOrders);
}
~~~
![image.png](https://note.youdao.com/yws/res/2308/WEBRESOURCE491bbbe86eb09e47315f10f82daea1e4)
## 垂直分库
前面已经介绍过，垂直分库是指按照业务将表进行分类，分布到不同的数据库上面，每个库可以放在不同的服务器
上，它的核心理念是专库专用。接下来看一下如何使用Sharding-JDBC实现垂直分库。
(1)创建数据库
创建数据库user_db
~~~mysql
CREATE DATABASE `user_db` CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
~~~
在user_db中创建t_user表
~~~mysql
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
`user_id` bigint(20) NOT NULL COMMENT '用户id',
`fullname` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户姓名',
`user_type` char(1) DEFAULT NULL COMMENT '用户类型',
PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
~~~
(2)在Sharding-JDBC规则中修改
~~~properties
# 新增m0数据源，对应user_db
spring.shardingsphere.datasource.names = m0,m1,m2
...
spring.shardingsphere.datasource.m0.type = com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.m0.driver‐class‐name = com.mysql.jdbc.Driver
spring.shardingsphere.datasource.m0.url = jdbc:mysql://localhost:3306/user_db?useUnicode=true
spring.shardingsphere.datasource.m0.username = root
spring.shardingsphere.datasource.m0.password = root
....
# t_user分表策略，固定分配至m0的t_user真实表
spring.shardingsphere.sharding.tables.t_user.actual‐data‐nodes = m$‐>{0}.t_user
spring.shardingsphere.sharding.tables.t_user.table‐strategy.inline.sharding‐column = user_id
spring.shardingsphere.sharding.tables.t_user.table‐strategy.inline.algorithm‐expression = t_user
~~~

## 公共表
公共表属于系统中数据量较小，变动少，而且属于高频联合查询的依赖表。参数表、数据字典表等属于此类型。可
以将这类表在每个数据库都保存一份，所有更新操作都同时发送到所有分库执行。接下来看一下如何使用
Sharding-JDBC实现公共表。
(1)创建数据库
分别在user_db、order_db_1、order_db_2中创建t_dict表：
~~~mysql
CREATE TABLE `t_dict` (
`dict_id` bigint(20) NOT NULL COMMENT '字典id',
`type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典类型',
`code` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典编码',
`value` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典值',
PRIMARY KEY (`dict_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;
~~~
(2)在Sharding-JDBC规则中修改
~~~properties
# 指定t_dict为公共表
spring.shardingsphere.sharding.broadcast‐tables=t_dict
~~~

（5）字典关联查询测试
字典表已在各各分库存在，各业务表即可和字典表关联查询。
定义用户关联查询dao：
在UserDao中定义：
~~~java
/**
* 根据id列表查询多个用户，关联查询字典表
* @param userIds 用户id列表
* @return
*/
@Select({"<script>",
" select",
" * ",
" from t_user t ,t_dict b",
" where t.user_type = b.code and t.user_id in",
"<foreach collection='userIds' item='id' open='(' separator=',' close=')'>",
"#{id}",
"</foreach>",
"</script>"
})
List<Map> selectUserInfobyIds(@Param("userIds")List<Long> userIds);

~~~

## 读写分离
面对日益增加的系统访问量，数据库的吞吐量面临着巨大瓶颈。 对于同一时刻有大量并发读操作和较少写操作类
型的应用系统来说，将数据库拆分为主库和从库，主库负责处理事务性的增删改操作，从库负责处理查询操作，能
够有效的避免由数据更新导致的行锁，使得整个系统的查询性能得到极大的改善。
![image.png](https://note.youdao.com/yws/res/2339/WEBRESOURCEa38ab34e0e6e456e52f1709948c5803e)
通过一主多从的配置方式，可以将查询请求均匀的分散到多个数据副本，能够进一步的提升系统的处理能力。 使用
多主多从的方式，不但能够提升系统的吞吐量，还能够提升系统的可用性，可以达到在任何一个数据库宕机，甚至
磁盘物理损坏的情况下仍然不影响系统的正常运行。
![image.png](https://note.youdao.com/yws/res/2341/WEBRESOURCE65d2900a32e492d9a84ff76a8950c5d7)
读写分离的数据节点中的数据内容是一致的，而水平分片的每个数据节点的数据内容却并不相同。将水平分片和读
写分离联合使用，能够更加有效的提升系统的性能。
**Sharding-JDBC读写分离则是根据SQL语义的分析，将读操作和写操作分别路由至主库与从库。**它提供透明化读写
分离，让使用方尽量像使用一个数据库一样使用主从数据库集群。
![image.png](https://note.youdao.com/yws/res/2346/WEBRESOURCE0f629a547de06022d5c157c99f34af81)
Sharding-JDBC提供一主多从的读写分离配置，可独立使用，也可配合分库分表使用，同一线程且同一数据库连接
内，如有写入操作，以后的读操作均从主库读取，用于保证数据一致性。**Sharding-JDBC不提供主从数据库的数据
同步功能，需要采用其他机制支持。**
![image.png](https://note.youdao.com/yws/res/2350/WEBRESOURCE07e8cba1a795f9276d8b6dac0f5f081e)
### mysql主从同步
一，新增mysql实例
复制原有mysql如：D:\mysql-5.7.25(作为主库) -> D:\mysql-5.7.25-s1(作为从库)，并修改以下从库的my.ini：
~~~ properties
[mysqld]
#设置3307端口
port = 3307
# 设置mysql的安装目录
basedir=D:\mysql‐5.7.25‐s1
# 设置mysql数据库的数据的存放目录
datadir=D:\mysql‐5.7.25‐s1\data
~~~
然后将从库安装为windows服务，注意配置文件位置：
`D:\mysql‐5.7.25‐s1\bin>mysqld install mysqls1 ‐‐defaults‐file="D:\mysql‐5.7.25‐s1\my.ini`
由于从库是从主库复制过来的，因此里面的数据完全一致，可使用原来的账号、密码登录。
二，修改主、从库的配置文件(my.ini)，新增内容如下：
主库：
~~~ properties
[mysqld]
#开启日志
log‐bin = mysql‐bin
#设置服务id，主从不能一致
server‐id = 1
#设置需要同步的数据库
binlog‐do‐db=user_db
#屏蔽系统库同步
binlog‐ignore‐db=mysql
binlog‐ignore‐db=information_schema
binlog‐ignore‐db=performance_schema
~~~
从库：
~~~ properties
[mysqld]
#开启日志
log‐bin = mysql‐bin
#设置服务id，主从不能一致
server‐id = 2
#设置需要同步的数据库
replicate_wild_do_table=user_db.%
#屏蔽系统库同步
replicate_wild_ignore_table=mysql.%
replicate_wild_ignore_table=information_schema.%
replicate_wild_ignore_table=performance_schema.%
~~~
重启主库和从库：
~~~shell
net start [主库服务名]
net start [从库服务名mysqls1]
~~~
![image.png](https://note.youdao.com/yws/res/2367/WEBRESOURCE5b8ef8312bbdad4bd2251de93477307c)
三，授权主从复制专用账号
~~~shell
#切换至主库bin目录，登录主库
mysql ‐h localhost ‐uroot ‐p
#授权主备复制专用账号
GRANT REPLICATION SLAVE ON *.* TO 'db_sync'@'%' IDENTIFIED BY 'db_sync';
#刷新权限
FLUSH PRIVILEGES;
#确认位点 记录下文件名以及位点
show master status;
~~~
![image.png](https://note.youdao.com/yws/res/2374/WEBRESOURCEbcdd8de82c6491fd028b1e490c8dae20)
'四，设置从库向主库同步数据、并检查链路'
~~~sh
#切换至从库bin目录，登录从库
mysql ‐h localhost ‐P3307 ‐uroot ‐p
#先停止同步
STOP SLAVE;
#修改从库指向到主库，使用上一步记录的文件名以及位点
CHANGE MASTER TO
master_host = 'localhost',
master_user = 'db_sync',
master_password = 'db_sync',
master_log_file = 'mysql‐bin.000002',
master_log_pos = 154;
#启动同步
START SLAVE;
#查看从库状态Slave_IO_Runing和Slave_SQL_Runing都为Yes说明同步成功，如果不为Yes，请检查error_log，然后
排查相关异常。
show slave status\G
#注意 如果之前此备库已有主库指向 需要先执行以下命令清空
STOP SLAVE IO_THREAD FOR CHANNEL '';
reset slave all;
~~~
最后测试在主库修改数据库，看从库是否能够同步成功。
### 实现shardingJDBC读写分离
(1)在Sharding-JDBC规则中修改
~~~properties
# 增加数据源s0，使用上面主从同步配置的从库。
spring.shardingsphere.datasource.names = m0,m1,m2,s0
...
spring.shardingsphere.datasource.s0.type = com.alibaba.druid.pool.DruidDataSource
spring.shardingsphere.datasource.s0.driver‐class‐name = com.mysql.jdbc.Driver
spring.shardingsphere.datasource.s0.url = jdbc:mysql://localhost:3307/user_db?useUnicode=true
spring.shardingsphere.datasource.s0.username = root
spring.shardingsphere.datasource.s0.password = root
....
# 主库从库逻辑数据源定义 ds0为user_db
spring.shardingsphere.sharding.master‐slave‐rules.ds0.master‐data‐source‐name=m0
spring.shardingsphere.sharding.master‐slave‐rules.ds0.slave‐data‐source‐names=s0
# t_user分表策略，固定分配至ds0的t_user真实表
spring.shardingsphere.sharding.tables.t_user.actual‐data‐nodes = ds0.t_user
....
~~~

