#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <chrono>

using namespace std;

struct ClipboardItem {
    int type;
    string content;
    bool isPinned;
    long long timestamp;
};

static vector<ClipboardItem> clipboardHistory;
static string filePath = "";

void saveToFile() {
    if(filePath.empty()) return;
    ofstream file(filePath, ios::binary);
    int size = clipboardHistory.size();
    file.write((char*)&size, sizeof(size));
    for(auto& item : clipboardHistory) {
        file.write((char*)&item.type, sizeof(item.type));
        file.write((char*)&item.isPinned, sizeof(item.isPinned));
        file.write((char*)&item.timestamp, sizeof(item.timestamp));
        int len = item.content.size();
        file.write((char*)&len, sizeof(len));
        file.write(item.content.c_str(), len);
    }
    file.close();
}

void loadFromFile() {
    if(filePath.empty()) return;
    ifstream file(filePath, ios::binary);
    if(!file.is_open()) return;
    int size = 0;
    if(file.read((char*)&size, sizeof(size))) {
        clipboardHistory.clear();
        for(int i=0; i<size; ++i) {
            ClipboardItem item;
            int len = 0;
            file.read((char*)&item.type, sizeof(item.type));
            file.read((char*)&item.isPinned, sizeof(item.isPinned));
            file.read((char*)&item.timestamp, sizeof(item.timestamp));
            file.read((char*)&len, sizeof(len));
            item.content.resize(len);
            file.read(&item.content[0], len);
            clipboardHistory.push_back(item);
        }
    }
    file.close();
}

extern "C" JNIEXPORT void JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_initEngine(JNIEnv* env, jobject, jstring path) {
    const char* path_chars = env->GetStringUTFChars(path, nullptr);
    filePath = string(path_chars) + "/megaclipboard_data.bin";
    env->ReleaseStringUTFChars(path, path_chars);
    loadFromFile();
}

extern "C" JNIEXPORT void JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_cleanupEngine(JNIEnv* env, jobject) {
    auto now = chrono::duration_cast<chrono::seconds>(chrono::system_clock::now().time_since_epoch()).count();
    bool changed = false;
    auto it = clipboardHistory.begin();
    while (it != clipboardHistory.end()) {
        if (!it->isPinned && (now - it->timestamp) > 86400) {
            it = clipboardHistory.erase(it);
            changed = true;
        } else {
            ++it;
        }
    }
    if (changed) saveToFile(); 
}

extern "C" JNIEXPORT void JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_addToEngine(JNIEnv* env, jobject, jstring data, jint type) {
    const char* data_chars = env->GetStringUTFChars(data, nullptr);
    
    // منع تكرار نفس النص إذا كان هو الأخير
    if (!clipboardHistory.empty() && clipboardHistory[0].content == string(data_chars)) {
        env->ReleaseStringUTFChars(data, data_chars);
        return;
    }

    ClipboardItem item;
    item.type = type; 
    item.content = string(data_chars);
    item.isPinned = false;
    auto now = chrono::system_clock::now();
    item.timestamp = chrono::duration_cast<chrono::seconds>(now.time_since_epoch()).count();
    
    clipboardHistory.insert(clipboardHistory.begin(), item);
    env->ReleaseStringUTFChars(data, data_chars);
    saveToFile();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_getDataFromEngine(JNIEnv* env, jobject, jint index) {
    if (index >= 0 && index < clipboardHistory.size()) return env->NewStringUTF(clipboardHistory[index].content.c_str());
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_getTypeFromEngine(JNIEnv* env, jobject, jint index) {
    if (index >= 0 && index < clipboardHistory.size()) return clipboardHistory[index].type;
    return 0;
}

// دالة جديدة لجلب عدد النصوص لمعرفة كم زر سنرسم في القائمة
extern "C" JNIEXPORT jint JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_getCountFromEngine(JNIEnv* env, jobject) {
    return clipboardHistory.size();
}
