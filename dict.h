#ifndef __DICT_H__
#define __DICT_H__

#include <stdint.h>

/* 字典结构体，保存K-V值的结构体 */
typedef struct dict_entry
{
    //字典key函数指针
    void *key;
    union {
        void *ptr;
        //无符号整型值
        uint64_t nat64;
        //有符号整型值
        int64_t int64;
        double real;
    } value;
    //下一字典结点
    struct dict_entry *next;
} dict_entry;

/* 字典类型 */
typedef struct dict_vtable
{
    //哈希计算方法，返回整形变量
    unsigned int (*hash)(const void *key);
    //复制key方法
    void *(*duplicate_key)(void *privdata, const void *key);
    //复制val方法
    void *(*duplicate_value)(void *privdata, const void *value);
    //key值比较方法
    int (*compare)(void *privdata, const void *left, const void *right);
    //key的析构函数
    void (*free_key)(void *privdata, void *key);
    //val的析构函数
    void (*free_value)(void *privdata, void *value);
} dict_vtable;

typedef struct dict_hashtable
{
    //字典实体
    dict_entry **table;
    //表格可容纳字典数量
    unsigned long size;
    unsigned long sizemask;
    //正在被使用的数量
    unsigned long used;
} dict_hashtable;

/* 字典主操作类 */
typedef struct dict
{
    //字典类型
    dict_vtable *vtable;
    //私有数据指针
    void *privdata;
    //字典哈希表，共2张，一张旧的，一张新的
    dict_hashtable ht[2];
    //重定位哈希时的下标
    long rehashidx; /* rehashing not in progress if rehashidx == -1 */
    //当前迭代器数量
    int iterators; /* number of iterators currently running */
} dict;

typedef struct dict_iterator
{
    //当前字典
    dict *d;
    //下标
    long index;
    //表格，和安全值的表格代表的是旧的表格，还是新的表格
    int table, safe;
    //字典实体
    dict_entry *entry, *next_entry;
    /* unsafe iterator fingerprint for misuse detection. */
    /* 指纹标记，避免不安全的迭代器滥用现象 */
    long long fingerprint;
} dict_iterator;

dict *dict_create(dict_vtable *type, void *privDataPtr);    //创建dict字典
int dict_add(dict *d, void *key, void *val);                //字典根据key, val添加一个字典集
int dict_delete(dict *d, const void *key);                  //根据key删除一个字典集
void dict_release(dict *d);                                 //释放整个dict
dict_entry *dict_find(dict *d, const void *key);            //根据key寻找字典集
unsigned int dictGenHashFunction(const void *key, int len); //输入的key值，目标长度，此方法帮你计算出索引值
void dictEmpty(dict *d, void(callback)(void *));            //清空字典

#endif