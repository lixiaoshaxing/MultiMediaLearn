//
// Created by 李晓 on 17/7/28.
//

#include <malloc.h>
#include <pthread.h>
#include "queue.h"

//ffmpeg解析出AvPacket，作为生产者，放入audio，video播放播放需要队列
/**
 * 队列，这里主要用于存放AVPacket的指针
 * 这里，使用生产者消费模式来使用队列，至少需要2个队列实例，分别用来存储音频AVPacket和视频AVPacket
 *  1.生产者：read_stream线程负责不断的读取视频文件中AVPacket，分别放入两个队列中
	2.消费者：
	1）视频解码，从视频AVPacket Queue中获取元素，解码，绘制
	2）音频解码，从音频AVPacket Queue中获取元素，解码，播放
*/
struct _Queue {
    int size;
    //一个数组，用来存储AvPacket*数据，申请空间的时候要对这里另外处理
    void **tab;
    //写入的时候写在这个位置
    int next_to_write;
    //读取的时候从这个位置读，需要通过加锁对这里进行控制
    int next_to_read;
    int *ready;
};

//队列的初始化，空间的申请
Queue *queue_init(int size, queue_fill_func fill_func) {
    Queue *queue = (Queue *) malloc(sizeof(Queue));
    queue->size = size;
    queue->next_to_write = 0;
    queue->next_to_read = 0;
    //对数组申请空间
    queue->tab = malloc(size * sizeof(*queue->tab));
    int i;
    //对数组赋初值
    for (i = 0; i < size; i++) {
        queue->tab[i] = fill_func();
    }
    return queue;
}

//队列的释放
void queue_free(Queue *queue, queue_free_func free_func){
    //先销毁数组中的元素
    //销毁queue整个结构体
    int i ;
    for(i = 0; i < queue->size; i ++){
        free_func((void *)queue->tab[i]);
    }
    free(queue->tab);
    free(queue);
}

//获取下一个索引位置
int queue_get_next(Queue *queue, int current){
    return (current + 1) % queue->size;
}

//压入元素到队列,返回头部元素的指针，进行赋值，添加信号量，条件变量
void * queue_push(Queue *queue, pthread_mutex_t *mutex, pthread_cond_t *cond){
    int current = queue->next_to_write;
    int next_to_write;
    for (;;) {
        next_to_write = queue_get_next(queue, current);
        if(next_to_write != queue->next_to_read){ //没有填满
            break;
        }
        //队列填满了，阻塞这里等待消费
        pthread_cond_wait(cond, mutex);
    }
    queue->next_to_write = next_to_write; //设置新的入队列下一位置

    //通知消费者进行消费
    pthread_cond_broadcast(cond);
    return queue->tab[current];
}

/**
 * 出队列，拿到AvPacket的位置，并读取其中的音视频数据
 * @param queue
 * @return
 */
void * queue_pop(Queue *queue, pthread_mutex_t *mutex, pthread_cond_t *cond){
    int current = queue->next_to_read;
    for(;;){
        if(queue->next_to_read != queue->next_to_write){ //队列不为空，可以消费
            break;
        }
        pthread_cond_wait(cond, mutex);
    }
    queue->next_to_read = queue_get_next(queue, current);

    pthread_cond_broadcast(cond);
    return queue->tab[current];
}



