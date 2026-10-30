package jp.co.benesse.dcha.dchaservice;

interface IDchaService {
    void rebootPad(int rebootMode, String srcFile) = 2;
    void setDefaultPreferredHomeApp(String packageName) = 3;
    void clearDefaultPreferredApp(String packageName) = 4;
    boolean installApp(String path, int installFlag) = 7;
    boolean uninstallApp(String packageName, int uninstallFlag) = 8;
    void setSetupStatus(int status) = 10;
    void removeTask(String packageName) = 13;
    void hideNavigationBar(boolean hide) = 21;
}
