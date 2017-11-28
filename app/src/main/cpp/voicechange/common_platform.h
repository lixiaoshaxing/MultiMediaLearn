#include <pthread.h>
#include <assert.h>
bool Common_SuspendState();

typedef pthread_mutex_t Common_Mutex;

inline void Common_Mutex_Create(Common_Mutex *mutex)
{
	int err;
	pthread_mutexattr_t mutexattr;
    pthread_mutexattr_init(&mutexattr);

    err = pthread_mutexattr_settype(&mutexattr, PTHREAD_MUTEX_RECURSIVE_NP);
    assert(err == 0);

    err = pthread_mutex_init(mutex, &mutexattr);
    assert(err == 0);
}

inline void Common_Mutex_Destroy(Common_Mutex *mutex)
{
    pthread_mutex_destroy(mutex);
}

inline void Common_Mutex_Enter(Common_Mutex *mutex)
{
    int err = pthread_mutex_lock(mutex);
    assert(err == 0);
}

inline void Common_Mutex_Leave(Common_Mutex *mutex)
{
    int err = pthread_mutex_unlock(mutex);
    assert(err == 0);
}
