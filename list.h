#ifndef __LIST_H__
#define __LIST_H__

typedef struct listNode
{
    struct listNode *prev;
    struct listNode *next;
    void *value;
} listNode;

typedef struct listVTable
{
    //复制函数指针
    void *(*dup)(void *ptr);
    //释放函数指针
    void (*free)(void *ptr);
    //匹配函数指针
    int (*match)(void *ptr, void *key);
} listVTable;

typedef struct list
{
    listNode *head;
    listNode *tail;

    listVTable *vtable;
    unsigned long len;
} list;

#define listLength(l) ((l)->len)      //获取list长度
#define listFirst(l) ((l)->head)      //获取列表首部
#define listLast(l) ((l)->tail)       //获取列表尾部
#define listPrevNode(n) ((n)->prev)   //给定结点的上一结点
#define listNextNode(n) ((n)->next)   //给定结点的下一节点
#define listNodeValue(n) ((n)->value) //给点的结点的值，这个value不是一个数值类型，而是一个函数指针

#define listSetVTable(l, vtable) ((l)->vtable = (vtable))
#define listGetVTable(l) ((l)->vtable)

/* Prototypes */
/* 定义了方法的原型 */
list *listCreate(void);                                             //创建list列表
void listRelease(list *list);                                       //列表的释放
list *listAddNodeHead(list *list, void *value);                     //添加列表头结点
list *listAddNodeTail(list *list, void *value);                     //添加列表尾结点
list *listInsertNode(list *list, listNode *reference, void *value); //某位置上插入及结点
void listDelNode(list *list, listNode *node);                       //列表上删除给定的结点
listIter *listGetIterator(list *list);                              //获取列表给定方向上的迭代器
listIter *listGetReverseIterator(list *list);                       //获取列表给定方向上的迭代器
list *listDup(list *orig);                                          //列表的复制
listNode *listSearchKey(list *list, void *key);                     //关键字搜索具体结点
listNode *listIndex(list *list, long index);                        //下标索引具体的结点

typedef struct listIter
{
    listNode *current;
    void (*next)(listIter *it);
} listIter;

#endif