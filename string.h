#ifndef __STRING_H__
#define __STRING_H__

typedef struct string
{
    //字符长度
    unsigned int len;

    //具体存放字符的buf
    char buf[];
} string;

string string_new(const char *buffer);
unsigned int string_len(const string s); //获取sds的长度
string string_duplicate(string s);

#endif