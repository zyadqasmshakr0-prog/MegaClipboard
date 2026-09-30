#include <jni.h>
#include <string>
#include <vector>

using namespace std;

// هيكل يمثل كل عنصر داخل الحافظة
struct ClipboardItem {
    string text;
    bool isPinned;
};

// الذاكرة الدائمة التي ستحفظ النصوص طوال فترة عمل لوحة المفاتيح
static vector<ClipboardItem> clipboardHistory;

// وظيفة لاستقبال النص المنسوخ من الأندرويد وحفظه في C++
extern "C" JNIEXPORT void JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_addTextToEngine(JNIEnv* env, jobject /* this */, jstring new_text) {
    const char* text_chars = env->GetStringUTFChars(new_text, nullptr);
    string text_str(text_chars);
    
    ClipboardItem item;
    item.text = text_str;
    item.isPinned = false;
    
    // إدراج النص الجديد في بداية القائمة (الأحدث أولاً)
    clipboardHistory.insert(clipboardHistory.begin(), item);
    
    env->ReleaseStringUTFChars(new_text, text_chars);
}

// وظيفة لإرسال النص من C++ إلى الأندرويد ليتم حقنه في الواتساب
extern "C" JNIEXPORT jstring JNICALL
Java_com_zayad_megaclipboard_ClipboardKeyboard_getTextFromEngine(JNIEnv* env, jobject /* this */, jint index) {
    if (index >= 0 && index < clipboardHistory.size()) {
        return env->NewStringUTF(clipboardHistory[index].text.c_str());
    }
    return env->NewStringUTF("");
}
