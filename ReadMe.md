# MiniDB

这个项目的主要意图是实现一个Java版的Redis从而深入认识Redis底层是怎么实现的, 
因为很多数据结构和接口Java都有自己的实现所以我们也就不重复造轮子了 
主要的目的还是为了了解Redis的应用需求和基本架构

在redis早期只有很少的几个文件 我们可以从一般来先了解下几个最重要的文件

实现列表类型, 底层数据结构是双向列表 对应Java里面的LinkedList, 具体的实现我们暂时可以不关心, 直接使用java的ConcurrentLinkedDeque就好

* adlist.c
* adlist.h

列表在Redis除了用于提供储存结构还被许多内部模块使用:

* 事务模块使用双端链表依序保存输入的命令
* 服务器模块使用双端链表来保存多个客户端
* 订阅/发送模块使用双端链表来保存订阅模式的多个客户端
* 事件模块使用双端链表来保存时间事件

字典的实现是使用哈希表跟Python的字典和Java的HashMap实现机制一样
Redis使用了两个哈希表, 
我估计平时只使用一个
另一个是为了rehash的时候逐渐将一个表里的数据迁移到另一个表里
所以在rehash的时候 两个表的数据加起来才是全部数据
正在rehash的时候取值会从两个表取, 加新的key-value会加到新表的
rehash结束的时候再把两张表对换一下

* dict.h
* dict.c

启动逻辑

1. 载入配置文件 - initServerConfig()
2. 初始化服务器全局状态例如对应的列表类型 - initServer()
3. 创建daemon进程 if (server.daemonize) daemonize() 后台运行调用setsid 脱离用户session
3. ...
4. 载入数据
5. 启动事件循环 - aeMain(server.el)

命令执行

readQueryFromClient -> processCommand -> lookupCommand -> cmdTable -> reply-handler

lookupCommand -> 根据字符来查找相应命令的实现函数

目前支持的命令有

* PING
* SELECT db(0-15)
* KEYS regex
* GET
* SET
* EXISTS
* DEL
* EXPIRE key milliseconds
* HKEYS
* HGET key field
* HSET key field value
* HEXISTS
* HDEL
* LEN
* FIRST
* LAST
* LPUSH
* LPOP
* RPUSH
* RPOP
* TYPE
* QUIT

## Reference

- [ ] [Redis源码分析](https://www.kancloud.cn/digest/redis-code/199030)
- [ ] [Redis-Code](https://github.com/linyiqun/Redis-Code)
- [ ] [Redis 源码解析](https://redissrc.readthedocs.io/en/latest/)
- [ ] [菜鸟从Redis源码学习C语言](http://www.shixinke.com/c/study-c-from-redis-source-code)
- [ ] [Redis 设计与实现](http://redisbook.com/)
- [x] [The Node.js Event Loop, Timers, and process.nextTick()](https://nodejs.org/en/docs/guides/event-loop-timers-and-nexttick/)

## Book

- [ ] Database System Concepts
- [ ] Inside SQLite
- [ ] [Architecture of a Database System](http://db.cs.berkeley.edu/papers/fntdb07-architecture.pdf)
- [ ] Database Systems: A Practical Approach to Design, Implementation, and Management
- [ ] Fundamentals of Database Systems
- [ ] [Database Management Systems](http://pages.cs.wisc.edu/~dbbook/)
- [ ] [Database Systems: The Complete Book](http://infolab.stanford.edu/~ullman/dscb.html)
- [ ] [Bigtable: A Distributed Storage System for Structured Data](https://static.googleusercontent.com/media/research.google.com/en//archive/bigtable-osdi06.pdf)

## Course

- [ ] [UC Berkeley CS 186: Introduction to Database Systems](https://cs186berkeley.net/)
- [ ] [MIT 6.830/6.814: Database Systems](https://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-830-database-systems-fall-2010)
- [ ] [Stanford CS 145: Data Management and Data Systems](https://cs145-fa19.github.io/)
- [ ] [Stanford CS 245: Principles of Data-Intensive Systems](http://web.stanford.edu/class/cs245/)
- [ ] [Stanford CS 346: Database System Implementation](https://web.stanford.edu/class/cs346)
- [ ] [CMU 15-445/645: DATABASE SYSTEMS](https://15445.courses.cs.cmu.edu)
- [ ] [CMU 15-721: Advanced Database Systems](https://15721.courses.cs.cmu.edu/)
- [ ] [Coursera: Database Management Essentials](https://www.coursera.org/learn/database-management)
- [ ] [Coursera: Database Systems Specialization](https://www.coursera.org/specializations/database-systems)

## Source Code

* RDBMS
  * [MySQL](https://github.com/mysql/mysql-server)
  * [Postgres](https://github.com/postgres/postgres)
  * [SQLite](https://sqlite.org/src/)
  * [MariaDB](https://github.com/MariaDB/server)
* Key-Value
  * [Redis](https://github.com/antirez/redis)
  * [KeyDB](https://github.com/JohnSully/KeyDB)
  * [Voldemort](https://github.com/voldemort/voldemort)
  * [Memcached](https://github.com/memcached/memcached)
  * [LevelDB](https://github.com/google/leveldb)
  * [RocksDB](https://github.com/facebook/rocksdb)
  * [etcd](https://github.com/etcd-io/etcd)
  * [BoltDB](https://github.com/boltdb/bolt)
* Document-Oriented
  * [MongoDB](https://github.com/mongodb/mongo)
  * [TinyDB](https://github.com/msiemens/tinydb)
* Graph
  * [Neo4j](https://github.com/neo4j/neo4j)
* Columnar
  * [Apache Cassandra](https://github.com/apache/cassandra)
  * [Apache HBase](https://github.com/apache/hbase)
  * [ClickHouse](https://github.com/ClickHouse/ClickHouse)
* NewSQL
  * [TiDB](https://github.com/pingcap/tidb)
  * [TDengine](https://github.com/taosdata/TDengine)
* Query Engine
  * [Presto](https://github.com/prestodb/presto)
  * [Apache Hive](https://github.com/apache/hive)
  * [Apache Spark](https://github.com/apache/spark)
