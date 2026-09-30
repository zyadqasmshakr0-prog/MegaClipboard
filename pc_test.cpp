#include <iostream>
#include <string>
#include <vector>

using namespace std;

// هيكل يمثل كل "عنصر" داخل الحافظة
struct ClipboardItem {
    string text;
    bool isPinned;
};

class MegaClipboardEngine {
private:
    vector<ClipboardItem> history; // الذاكرة التي ستحفظ كل النصوص

public:
    // وظيفة لإضافة نص جديد
    void addText(string newText) {
        ClipboardItem item;
        item.text = newText;
        item.isPinned = false; // افتراضيا النص غير مثبت
        history.push_back(item);
        
        cout << "[+] تم حفظ نص جديد! (الحجم: " << newText.length() << " بايت)" << endl;
    }

    // وظيفة لتثبيت نص معين (Pin)
    void pinText(int index) {
        if (index >= 0 && index < history.size()) {
            history[index].isPinned = true;
            cout << "[*] تم تثبيت النص رقم " << index << " بنجاح!" << endl;
        }
    }

    // وظيفة لعرض محتويات الحافظة
    void showHistory() {
        cout << "\n=== محتويات الحافظة الحالية ===" << endl;
        for (int i = 0; i < history.size(); i++) {
            cout << i << " | ";
            if (history[i].isPinned) cout << "[مُثبت] ";
            
            // نعرض أول 40 حرف فقط لكي لا تمتلئ الشاشة إذا كان النص طويلا
            cout << history[i].text.substr(0, 40) << "..." << endl;
        }
        cout << "===============================\n" << endl;
    }
};

int main() {
    cout << "--- تشغيل محرك MegaClipboard ---" << endl;
    MegaClipboardEngine engine;

    // 1. إضافة نصوص عادية
    engine.addText("السلام عليكم، هذا نص لتجربة الحافظة.");
    engine.addText("رقم حسابي البنكي هو 123456789.");
    
    // 2. محاكاة إضافة نص عملاق جدا (مليون حرف 'A') لاختبار قوة C++
    string massiveText(1000000, 'A'); 
    engine.addText(massiveText);

    // 3. تثبيت النص الخاص برقم الحساب
    engine.pinText(1);

    // 4. عرض النتيجة
    engine.showHistory();

    return 0;
}