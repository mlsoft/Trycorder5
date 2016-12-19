#ifndef MAINWINDOW_H
#define MAINWINDOW_H
 
#include <KXmlGuiWindow>

class KTextEdit;
class KJob;
 
class MainWindow : public KXmlGuiWindow
{
    Q_OBJECT
    
  public:
    MainWindow(QWidget *parent=0);
    void openFile(const QUrl &inputFileName);
 
  private:
    KTextEdit* textArea;
    void setupActions();
    
    QString fileName;
 
  private slots:
    void newFile();
    void openFile();
    void saveFile();
    void saveFileAs();
    void saveFileAs(const QString &outputFileName);
    
    void downloadFinished(KJob* job);
};
 
#endif
