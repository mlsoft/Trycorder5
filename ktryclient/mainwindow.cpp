#include <QApplication>
#include <QAction>
#include <QSaveFile>
#include <QFileDialog>
#include <QTextStream>
#include <QByteArray>

#include <KTextEdit>
#include <KLocalizedString>
#include <KActionCollection>
#include <KStandardAction>
#include <KMessageBox>
#include <KIO/Job>

#include "mainwindow.h"

MainWindow::MainWindow(QWidget *parent) : KXmlGuiWindow(parent), fileName(QString())
{
  textArea = new KTextEdit();
  setCentralWidget(textArea);
  
  setupActions();
}

void MainWindow::setupActions()
{
    QAction* clearAction = new QAction(this);
    clearAction->setText(i18n("&Clear"));
    clearAction->setIcon(QIcon::fromTheme("document-new"));
    actionCollection()->setDefaultShortcut(clearAction, Qt::CTRL + Qt::Key_W);
    actionCollection()->addAction("clear", clearAction);
    connect(clearAction, SIGNAL(triggered(bool)), textArea, SLOT(clear()));
    
    KStandardAction::quit(qApp, SLOT(quit()), actionCollection());
    
    KStandardAction::open(this, SLOT(openFile()), actionCollection());
 
    KStandardAction::save(this, SLOT(saveFile()), actionCollection());
 
    KStandardAction::saveAs(this, SLOT(saveFileAs()), actionCollection());
 
    KStandardAction::openNew(this, SLOT(newFile()), actionCollection());
    
    setupGUI(Default, "ktryclientui.rc");
}

void MainWindow::newFile()
{
    fileName.clear();
    textArea->clear();
}

void MainWindow::saveFileAs(const QString &outputFileName)
{
    if (!outputFileName.isNull())
    {
        QSaveFile file(outputFileName);
        file.open(QIODevice::WriteOnly);
        
        QByteArray outputByteArray;
        outputByteArray.append(textArea->toPlainText().toUtf8());
        file.write(outputByteArray);
        file.commit();

        fileName = outputFileName;
    }
}

void MainWindow::saveFileAs()
{
    saveFileAs(QFileDialog::getSaveFileName(this, i18n("Save File As")));
}

void MainWindow::saveFile()
{
    if (!fileName.isEmpty())
    {
        saveFileAs(fileName);
    }
    else
    {
        saveFileAs();
    }
}


void MainWindow::openFile()
{
    openFile(QFileDialog::getOpenFileUrl(this, i18n("Open File")));
}
    
void MainWindow::openFile(const QUrl &inputFileName)
{
    if (!inputFileName.isEmpty())
    {
        KIO::Job* job = KIO::storedGet(inputFileName);
        fileName = inputFileName.toLocalFile();

        connect(job, SIGNAL(result(KJob*)), this, SLOT(downloadFinished(KJob*)));
        
        job->exec();
    }
}

void MainWindow::downloadFinished(KJob* job)
{
    if (job->error())
    {
        KMessageBox::error(this, job->errorString());
        fileName.clear();
        return;
    }
    
    KIO::StoredTransferJob* storedJob = (KIO::StoredTransferJob*)job;
    textArea->setPlainText(QTextStream(storedJob->data(), QIODevice::ReadOnly).readAll());
}
