//
// Created by xx on 9/7/2025.
//

#include "anti_detect.h"
#include <jni.h>
#include <string>
#include <android/log.h>
#include <dlfcn.h>
#include "Dobby/include/dobby.h" // 引入 Dobby 头文件

#define TAG "AntiDetect"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

// 定义一个线程局部存储变量，用于标记当前线程打开的是否是 maps 文件
// 使用 thread_local 可以避免多线程环境下的干扰
static thread_local bool maps_file_opened = false;

// 定义我们想要过滤掉的特征字符串
// 注意：这里应该包含你为BlackBox重命名的所有新包名/路径特征
const char* forbidden_keywords[] = {
        "blackbox",
        "virtual",
        "Bcore", // BlackBox 核心库名
        // ... 添加更多你需要隐藏的库名或路径特征
};

// 用于保存原始函数指针的变量
static FILE* (*orig_fopen)(const char* path, const char* mode) = nullptr;
static char* (*orig_fgets)(char* s, int size, FILE* stream) = nullptr;
static int (*orig_clone)(int (*fn)(void *), void *child_stack, int flags, void *arg, ...) = nullptr;


// 我们伪造的 fopen 函数
FILE* new_fopen(const char* path, const char* mode) {
    // 检查路径是否是我们要拦截的目标
    if (path != nullptr && strcmp(path, "/proc/self/maps") == 0) {
        LOGD("Intercepted fopen for /proc/self/maps");
        // 设置标记
        maps_file_opened = true;
    }
    // 无论如何都调用原始的 fopen，我们不在这一步做手脚，只设置标记
    return orig_fopen(path, mode);
}

// 我们伪造的 fgets 函数
char* new_fgets(char* s, int size, FILE* stream) {
    // 检查标记，如果当前线程没有打开 maps 文件，则直接调用原始函数
    if (!maps_file_opened) {
        return orig_fgets(s, size, stream);
    }

    // 循环调用原始的 fgets，直到找到一个“干净”的行
    while(true) {
        // 调用原始函数获取真实的下一行内容
        char* line = orig_fgets(s, size, stream);

        // 如果 line 为 NULL，说明文件已经读完，直接返回
        if (line == nullptr) {
            // 文件读取完毕，重置标记
            maps_file_opened = false;
            return nullptr;
        }
        LOGD("Intercepted fgets for /proc/self/maps %s", line);

        // 检查这一行是否包含任何需要隐藏的关键词
        bool found = false;
        for (const char* keyword : forbidden_keywords) {
            if (strstr(line, keyword) != nullptr) {
                found = true;
                break;
            }
        }

        // 如果没有找到关键词，说明这一行是“干净”的，返回给调用者
        if (!found) {
            return line;
        } else {
            // 如果找到了关键词，就“跳过”这一行，继续下一次循环读取下一行
            LOGD("Filtered maps line: %s", line);
        }
    }
}

// 我们自己的 new_clone 函数
int new_clone(int (*fn)(void *), void *child_stack, int flags, void *arg, ...) {
    // 使用GCC/Clang的内建函数获取返回地址 (LR寄存器)
    void* return_address = __builtin_return_address(0);

    Dl_info info;
    if (dladdr(return_address, &info)) {
        // 计算偏移
        // (char*) 类型转换是为了进行指针的字节运算
        uintptr_t offset = (uintptr_t)return_address - (uintptr_t)info.dli_fbase;

        LOGD("clone() called from SO: %s (base: %p), offset: 0x%lx",
             info.dli_fname ? info.dli_fname : "unknown",
             info.dli_fbase,
             offset);
    } else {
        LOGD("clone() called, but dladdr failed for address %p", return_address);
    }

    // !! 必须调用原始的 clone 函数，否则系统将无法创建新线程或进程 !!
    // 注意：处理可变参数(...)需要更复杂的汇编或va_list操作，
    // 在这个示例中，我们假设最常见的参数情况，并且直接透传。
    // 在真实项目中，这部分需要根据具体架构和调用约定小心处理。
    // 为了简单起见，这里我们只传递前4个固定参数。
    // 实际的线程创建通常会传递更多的参数(ptid, tls, ctid)，
    // 但Dobby的Hook上下文处理机制通常能正确传递它们。
    return orig_clone(fn, child_stack, flags, arg);
}

// 初始化我们的反检测 Hook
void init_anti_detect() {
    LOGD("Initializing anti-detection hooks...");

    // 使用 Dobby 来 Hook fopen 函数
//    int ret_fopen = DobbyHook(
//            (void *) fopen, // 要 Hook 的函数地址
//            (void *) new_fopen, // 我们自己的替换函数
//            (void **) &orig_fopen // 用于保存原始函数地址的指针
//    );
//
//    // 使用 Dobby 来 Hook fgets 函数
//    int ret_fgets = DobbyHook(
//            (void *) fgets,
//            (void *) new_fgets,
//            (void **) &orig_fgets
//    );

    // 使用 Dobby 设置 Hook
//    DobbyHook(
//            (void *) dlsym(RTLD_DEFAULT, "clone"),
//            (void *) new_clone,
//            (void **) &orig_clone
//    );

//    if (ret_fopen == 0 && ret_fgets == 0) {
//        LOGD("fopen and fgets hooked successfully!");
//    } else {
//        LOGD("Hook failed! fopen_ret: %d, fgets_ret: %d", ret_fopen, ret_fgets);
//    }
}