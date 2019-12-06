#ifndef __MINIDB_H__
#define __MINIDB_H__

typedef struct redisDb
{
    dict *dict;          /* The keyspace for this DB */
    dict *expires;       /* Timeout of keys with a timeout set */
    dict *blocking_keys; /* Keys with clients waiting for data (BLPOP) */
    dict *ready_keys;    /* Blocked keys that received a PUSH */
    dict *watched_keys;  /* WATCHED keys for MULTI/EXEC CAS */
    int id;
    long long avg_ttl; /* Average TTL, just for stats */
} minidb;

#endif