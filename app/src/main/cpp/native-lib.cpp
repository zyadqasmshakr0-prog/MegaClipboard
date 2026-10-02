#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <chrono>

using namespace std;

// تحديث الهيكل ليشمل كل طلباتك
struct ClipboardItem {
    int type; // 0 = نص، 1 = صورة أو ملف
    string content; // النص أو رابط الصورة
    bool isPinned; // هل هو مثبت؟
    long long timestamp; // وقت النسخ للمؤقت
};

static vector<ClipboardItem> clipboardHistory;
static string filePath = ""; // مسار الملف المحفوظ في ذاكرة الهاتف

// 1. وظيفة الحفظ داخل الملفات (لحفظ البيانات للأبد)
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

// 2. وظيفة استرجاع البيانات من الملف عند تشغيل الهاتف
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

// تهيئة المحرك وإخباره بمكان حفظ الملفات
extern "C" JNIEXPORT void JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_initEngine(JNIEnv* env, jobject, jstring path) {
    const char* path_chars = env->GetStringUTFChars(path, nullptr);
    filePath = string(path_chars) + "/megaclipboard_data.bin"; // اسم الملف السري
    env->ReleaseStringUTFChars(path, path_chars);
    loadFromFile(); // جلب البيانات السابقة فوراً
}

// إضافة نص أو صورة للذاكرة والملف
extern "C" JNIEXPORT void JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_addToEngine(JNIEnv* env, jobject, jstring data, jint type) {
    const char* data_chars = env->GetStringUTFChars(data, nullptr);
    
    ClipboardItem item;
    item.type = type; 
    item.content = string(data_chars);
    item.isPinned = false; // التثبيت الافتراضي مغلق
    
    // تسجيل وقت النسخ من أجل المؤقت لاحقاً
    auto now = chrono::system_clock::now();
    item.timestamp = chrono::duration_cast<chrono::seconds>(now.time_since_epoch()).count();
    
    clipboardHistory.insert(clipboardHistory.begin(), item);
    env->ReleaseStringUTFChars(data, data_chars);
    
    saveToFile(); // الحفظ داخل الملف فوراً!
}

// جلب المحتوى (النص أو رابط الصورة)
extern "C" JNIEXPORT jstring JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_getDataFromEngine(JNIEnv* env, jobject, jint index) {
    if (index >= 0 && index < clipboardHistory.size()) {
        return env->NewStringUTF(clipboardHistory[index].content.c_str());
    }
    return env->NewStringUTF("");
}

// جلب نوع المحتوى لمعرفة هل نلصق نص أم صورة
extern "C" JNIEXPORT jint JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_getTypeFromEngine(JNIEnv* env, jobject, jint index) {
    if (index >= 0 && index < clipboardHistory.size()) {
        return clipboardHistory[index].type;
    }
    return 0;
}
