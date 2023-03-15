package com.china.observer.action;

import com.china.observer.util.AssertUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.popup.BalloonPopupBuilderImpl;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class ShowCodeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        dataContext = e.getDataContext();
        project = e.getProject();
        //获取项目
        Project project = e.getProject();
        AssertUtil.isNullNoException(project);
        //获取编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        AssertUtil.isNullNoException(editor);
        //通过项目的文档管理器，获取当前编辑器展示的文件
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        AssertUtil.isNullNoException(psiFile);
        //获取展示的文件中，光标所在的PsiElement
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        AssertUtil.isNullNoException(element);
        //必须是 PsiIdentifier 才处理
        if (! (element instanceof PsiIdentifier)) {
            //提示注意光标位置
            Messages.showMessageDialog("Plugin internal error.", "Notification", Messages.getInformationIcon());
            return;
        }
        //获取上一级，一般类型是 PsiReferenceExpression
        PsiElement pe = element.getParent();
        //悬浮在函数上 -> PsiReferenceExpression
        if (pe instanceof PsiReferenceExpression) {
            PsiReferenceExpression reference = (PsiReferenceExpression) pe;
            PsiElement resolve = reference.resolve();
            //方法 -> 需要判断接口还是实现类
            if (resolve instanceof PsiMethod) {
                executePsiMethod(resolve, project, editor);
            }
            //当前类字段
            else if (resolve instanceof PsiField) {

            }
            //局部变量
            else if (resolve instanceof PsiLocalVariable) {

            } else {
                //插件内部错误
                Messages.showMessageDialog("Plugin internal error", "Notification", Messages.getInformationIcon());
                return;
            }
        }
        //悬浮在类上 -> PsiJavaCodeReferenceElement
        else if (pe instanceof PsiJavaCodeReferenceElement) {



        }
        //悬浮在当前类的字段上 -> PsiField
        else if (pe instanceof PsiField) {



        }
        //悬浮在当前函数的参数上 -> PsiParameter
        else if (pe instanceof PsiParameter) {



        }
        //悬浮在函数的名字上 -> PsiMethod
        else if (pe instanceof PsiMethod) {
            executePsiMethod(pe, project, editor);
        } else {
            //插件内部错误
            Messages.showMessageDialog("Plugin internal error", "Notification", Messages.getInformationIcon());
            return;
        }

    }

    /**
     * 执行method判定
     * @param pe
     * @param project
     * @param editor
     */
    private void executePsiMethod(PsiElement pe, Project project, Editor editor) {
        //直接放到了函数名字上，需要判断是接口还是实现，接口的话可能有多个实现，实现的话直接展示
        PsiMethod pm = (PsiMethod) pe;
        //检查PsiMethod是本地代码还是第三方jar包的代码，通过文件来识别
        VirtualFile virtualFile = pm.getContainingFile().getVirtualFile();
        if (virtualFile != null) {
            //第三方代码文件路径
            String absolutePath = virtualFile.getPath();
            //本地项目路径
            String projectBasePath = project.getBasePath();
            // 第三方的代码 - 同样要检查接口还是实现类
            if (!absolutePath.startsWith(projectBasePath)) {


                System.out.println();

            }
        } else {
            Messages.showMessageDialog("No corresponding reality found", "Notification", Messages.getInformationIcon());
            return;
        }
        //处理本地项目代码
        //方法所在的类
        PsiClass containingClass = pm.getContainingClass();
        AssertUtil.isNullNoException(containingClass);
        //接口
        if (containingClass.isInterface()) {
            //在整个项目中，查找获取到的方法(所有，包括实现类的方法)
            Collection<PsiMethod> methods = OverridingMethodsSearch.search(pm, GlobalSearchScope.projectScope(project), true)
                    .findAll();
            //如果方法有多个实现，展示列表，则进行选择
            if (methods.size() > 1) {
                showImplList(methods, editor);
            } else if (methods.size() == 1) {
                //只有 1 个实现，直接展示
                showCode(methods.iterator().next(), editor);
            } else {
                //插件内部错误
                Messages.showMessageDialog("No corresponding reality found", "Notification", Messages.getInformationIcon());
                return;
            }
        }
        //实现类代码，直接展示
        else {
            showCode(pm, editor);
        }
    }

    private DataContext dataContext;
    private Project project;
    public static final DataKey<Editor> MY_EDITOR = DataKey.create("my.plugin.editor");
    /**
     * 展示代码
     * @param method
     * @param editor
     */
    private void showCode(PsiMethod psiMethod, Editor edi) {
//        VirtualFile virtualFile = psiMethod.getContainingFile().getVirtualFile();
//        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
//        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
//        Document document = EditorFactory.getInstance().createDocument(psiMethod.getText());
//        Editor ed = EditorFactory.getInstance().createViewer(document, project);
//        JBScrollPane scrollPane = new JBScrollPane(ed.getComponent());
//        // 创建一个JBPopup
//        JBPopup popup = JBPopupFactory.getInstance()
//                .createComponentPopupBuilder(scrollPane, editor.getComponent())
//                .setRequestFocus(true)
//                .setResizable(true)
//                .setMovable(true)
//                .setTitle("My Popup")
//                .createPopup();
//        JComponent jComponent = popup.getContent();
//        jComponent.addAncestorListener(new AncestorListener() {
//            //打开
//            @Override
//            public void ancestorAdded(AncestorEvent ancestorEvent) {
//            }
//            //关闭
//            @Override
//            public void ancestorRemoved(AncestorEvent ancestorEvent) {
//                System.out.println("关闭");
//                EditorFactory.getInstance().releaseEditor(ed);
//            }
//            //移动
//            @Override
//            public void ancestorMoved(AncestorEvent ancestorEvent) {
//            }
//        });
//        popup.showInBestPositionFor(editor);
    }

    /**
     * 展示实现类列表
     * @param methods
     * @param editor
     */
    private void showImplList(Collection<PsiMethod> methods, Editor editor) {
        List<PsiMethod> methodList = new ArrayList<>(methods);
        JBList<PsiMethod> list = new JBList<>(methodList);
        //默认选中第一个
        list.setSelectedIndex(0);
        //设置列表字体
        list.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
        //设置列表展示形式 - PsiClassListCellRenderer为展示class的样式
        list.setCellRenderer(new ListCellRenderer<PsiMethod>() {
            private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
            @Override
            public Component getListCellRendererComponent(JList<? extends PsiMethod> jList, PsiMethod psiMethod, int index, boolean isSelected, boolean cellHasFocus) {
                // 根据value中的信息，生成用于展示的Component对象
                JLabel label = (JLabel) defaultRenderer.getListCellRendererComponent(list, psiMethod.getName(), index, isSelected, cellHasFocus);
                //设置图标
                label.setIcon(psiMethod.getContainingClass().getIcon(Iconable.ICON_FLAG_READ_STATUS));
                // 添加返回类型和参数信息
                label.setText(psiMethod.getContainingClass().getName() + " (" + ((PsiJavaFileImpl) psiMethod.getContainingFile()).getPackageName() + ")");
                return label;
            }
        });
        //鼠标点击事件
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //点击 1 次
                if (e.getClickCount() == 1) {
                    //展示对应方法的实现代码
                    PsiMethod pm = list.getSelectedValue();
                    if (pm != null) {
                        showCode(pm, editor);
                    }
                }
            }
        });
        //鼠标悬停事件
        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                list.setSelectedIndex(index);
                list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
        JPanel content = new JPanel(new BorderLayout());
        content.add(list, BorderLayout.CENTER);
        JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(content, null)
                .setTitle("I Love You dong dong")
                .setResizable(true) // 设置弹窗可以调节大小
                .setMovable(true) // 设置弹窗可以拖动
                .setRequestFocus(true)
                .setFocusable(true)
                .createPopup();
        popup.showInBestPositionFor(editor);
    }

}
