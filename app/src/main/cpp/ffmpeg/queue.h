//
// Created by 李晓 on 17/7/28.
//

#ifndef FFMPEGTEST_QUEUE_H
#define FFMPEGTEST_QUEUE_H

#endif //FFMPEGTEST_QUEUE_H

typedef struct _Queue Queue;

//用来对队列中元素申请空间的回调函数，指针函数
typedef void *(*queue_fill_func)();

typedef void *(*queue_free_func)(void *elem);

Queue *queue_init(int size, queue_fill_func fill_func);

void queue_free(Queue *queue, queue_free_func free_func);

int queue_get_next(Queue *queue, int current);

void * queue_push(Queue *queue, pthread_mutex_t *mutex, pthread_cond_t *cond);

void * queue_pop(Queue *queue, pthread_mutex_t *mutex, pthread_cond_t *cond);


